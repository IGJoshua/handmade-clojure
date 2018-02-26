(ns handmade-clojure.core
  (:require [handmade-clojure.window :refer [set-callback init-window swap-window
                                             poll-events should-close? show-window]]
            [handmade-clojure.rendering :refer [init-opengl set-clear-color clear-screen
                                                create-shader create-shader-program
                                                bind-program get-uniform set-matrix-uniform
                                                set-int-uniform set-vector-3-uniform set-float-uniform]]
            [handmade-clojure.rendering.mesh :refer [create-mesh render-mesh load-mesh]]
            [handmade-clojure.texture :refer [load-texture]]
            [handmade-clojure.resource :refer [with-dispose]]
            [handmade-clojure.matrix :refer [projection-matrix identity-matrix translation-matrix
                                             x-rotation-matrix y-rotation-matrix z-rotation-matrix]]
            [handmade-clojure.rendering.primitives :refer [create-cube]]
            [handmade-clojure.util :refer [once-only case-cond]]
            [clojure.core.matrix :refer [mmul inverse]]
            [clojure.java.io :as io]
            [clojure.core.async :as a :refer [go]]
            [org.suskeyhose.imports :refer [import-static-all]]
            [com.rpl.specter :as s])
  (:gen-class))

(import-static-all org.lwjgl.glfw.GLFW
                   org.lwjgl.opengl.GL11
                   org.lwjgl.opengl.GL15
                   org.lwjgl.opengl.GL30)

;; Basic structure of the game:
;; Init
;; Game Loop
  ;; Input
  ;; Update 0+ times, based on fixed timestep
  ;; Render
;; Cleanup

(defonce game-state (atom {:position [0 0 0]
                           :camera {:position [0 0 2]
                                    :look [0 0]
                                    :projection {:fov 90
                                                 :near 0.1
                                                 :far 10000}}
                           :input {:movement [0 0]
                                   :look [0 0]
                                   :actions #{}}}))

(defn init-game-state
  []
  (reset! game-state {:position [0 0 0]
                      :camera {:position [0 0 2]
                               :look [0 0]
                               :projection {:fov 90
                                            :near 0.1
                                            :far 10000}}
                      :input {:movement [0 0]
                              :look [0 0]
                              :actions #{}}}))

(defmacro press-release [action on-press on-release]
  `(if (= ~action GLFW_PRESS)
     ~on-press
     (if (= ~action GLFW_RELEASE)
       ~on-release
       identity)))

(defmacro input-add []
  '(press-release
    action
    #(+ % 1)
    #(- % 1)))

(defmacro input-sub []
  '(press-release
    action
    #(- % 1)
    #(+ % 1)))

(defn key-callback
  [window key scancode action mods]
  (case-cond
   key
   GLFW_KEY_ESCAPE (glfwSetWindowShouldClose window true)
   GLFW_KEY_A (s/transform [s/ATOM :input :movement 0] (input-sub) game-state)
   GLFW_KEY_D (s/transform [s/ATOM :input :movement 0] (input-add) game-state)
   GLFW_KEY_W (s/transform [s/ATOM :input :movement 1] (input-sub) game-state)
   GLFW_KEY_S (s/transform [s/ATOM :input :movement 1] (input-add) game-state)
   GLFW_KEY_SPACE (s/transform [s/ATOM :input :actions] (press-release
                                                         action
                                                         #(conj % :jump)
                                                         #(disj % :jump))
                               game-state)
   GLFW_KEY_LEFT_SHIFT (s/transform [s/ATOM :input :actions] (press-release
                                                              action
                                                              #(conj % :crouch)
                                                              #(disj % :crouch))
                                    game-state)
   GLFW_KEY_LEFT (s/transform [s/ATOM :input :look 1] (input-add) game-state)
   GLFW_KEY_RIGHT (s/transform [s/ATOM :input :look 1] (input-sub) game-state)
   GLFW_KEY_UP (s/transform [s/ATOM :input :look 0] (input-add) game-state)
   GLFW_KEY_DOWN (s/transform [s/ATOM :input :look 0] (input-sub) game-state))
  #_(if (#{GLFW_PRESS GLFW_RELEASE} action) (println (:input @game-state))))

(def width (atom 640))
(def height (atom 480))
(def resized? (atom false))

(defn resize-callback
  [window w h]
  (reset! width w)
  (reset! height h)
  (reset! resized? true)
  nil)

(defn move-camera
  [input yaw pos]
  (let [x (first (:movement input))
        z (last (:movement input))]
    [(+ (* 0.1 x (Math/cos yaw))
        (* 0.1 z (Math/sin yaw))
        (first pos))
     (+ (* 0.1
           (+ (if (contains? (:actions input) :jump) 1 0)
              (if (contains? (:actions input) :crouch) -1 0)))
        (nth pos 1))
     (+ (* 0.1 z (Math/cos yaw))
        (* -0.1 x (Math/sin yaw))
        (last pos))]))

(defn change-look-dir
  [input look]
  [(+ (* 0.05
         (first (:look input)))
      (first look))
   (+ (* 0.05
         (last (:look input)))
      (last look))])

(defn -main
  []
  (go
    (let [title "Hello World!"
          window (atom nil)
          vertex-shader-source (slurp (io/resource "shader/vertex.glsl"))
          fragment-shader-source (slurp (io/resource "shader/fragment.glsl"))]
      ;; Initialization
      (with-dispose :window @window
        ;; Create window and init OpenGL
        (reset! window (init-window @width @height title))
        (show-window @window)
        (init-opengl 0 0 0 1)

        (set-callback @window :framebuffer-size-callback (fn [& args] (eval `(resize-callback ~@args))))
        (set-callback @window :key-callback (fn [& args] (eval `(key-callback ~@args))))
        ;; Init the shaders and stuff
        (let [vert-shader (create-shader :vertex-shader vertex-shader-source)
              fragment-shader (create-shader :fragment-shader fragment-shader-source)
              shader-program (create-shader-program [vert-shader fragment-shader])]
          (with-dispose :shader-program shader-program
            ;; Define the geometry
            (let [cube (load-mesh "mesh/cube.obj")
                  aspect (atom (/ @width @height))
                  proj-uniform (get-uniform shader-program "projectionMatrix")
                  view-uniform (get-uniform shader-program "viewMatrix")
                  world-uniform (get-uniform shader-program "worldMatrix")
                  texture-uniform (get-uniform shader-program "textureSampler")
                  camera-position-uniform (get-uniform shader-program "cameraPosition")
                  ambient-light-color-uniform (get-uniform shader-program "ambientLight.color")
                  ambient-light-intensity-uniform (get-uniform shader-program "ambientLight.intensity")
                  directional-light-color-uniform (get-uniform shader-program "directionalLight.color")
                  directional-light-intensity-uniform (get-uniform shader-program "directionalLight.intensity")
                  directional-light-direction-uniform (get-uniform shader-program "directionalLight.direction")
                  texture-id (glGenTextures)]
              ;; Set up constants for this
              (set-vector-3-uniform ambient-light-color-uniform [1.0 1.0 1.0])
              (set-float-uniform ambient-light-intensity-uniform 2)
              (set-vector-3-uniform directional-light-color-uniform [1.0 1.0 1.0])
              (set-float-uniform directional-light-intensity-uniform 1.0)
              (set-vector-3-uniform directional-light-direction-uniform [1.0 1.0 1.0])
              (with-dispose :texture texture-id
                (if-let [[^java.nio.DirectByteBuffer texture-bytes ^long width ^long height]
                         (load-texture "texture/dirt.png")]
                  (do (glBindTexture GL_TEXTURE_2D texture-id)
                      (glPixelStorei GL_UNPACK_ALIGNMENT 1)
                      #_(glTexParameteri GL_TEXTURE_2D GL_TEXTURE_MIN_FILTER GL_NEAREST)
                      #_(glTexParameteri GL_TEXTURE_2D GL_TEXTURE_MAG_FILTER GL_NEAREST)
                      (glTexImage2D GL_TEXTURE_2D
                                    0 GL_RGBA width height
                                    0 GL_RGBA GL_UNSIGNED_BYTE texture-bytes)
                      (glGenerateMipmap GL_TEXTURE_2D))
                  (throw (Exception. "Unable to get texture")))
                (with-dispose :mesh cube
                  ;; Main loop
                  (while (not (should-close? @window))
                    ;; Handle input
                    (when @resized?
                      (glViewport 0 0 @width @height)
                      (reset! aspect (/ @width @height))
                      (reset! resized? false))

                    (s/transform [s/ATOM (s/collect-one [:input]) :camera
                                  (s/collect-one [:look s/LAST]) :position]
                                 move-camera
                                 game-state)

                    (s/transform [s/ATOM (s/collect-one [:input]) :camera :look]
                                 change-look-dir
                                 game-state)

                    ;; Clear screen
                    (clear-screen)

                    (let [proj-mat (projection-matrix
                                    (s/select-first [s/ATOM :camera :projection :fov] game-state)
                                    @aspect
                                    (s/select-first [s/ATOM :camera :projection :near] game-state)
                                    (s/select-first [s/ATOM :camera :projection :far] game-state))
                          camera-mat (mmul
                                      (mmul
                                       (x-rotation-matrix
                                        (s/select-first [s/ATOM :camera :look s/FIRST] game-state))
                                       (y-rotation-matrix
                                        (s/select-first [s/ATOM :camera :look s/LAST] game-state)))
                                      (apply translation-matrix
                                             (s/select-first [s/ATOM :camera :position] game-state)))
                          world-mat (apply translation-matrix
                                           (s/select-first [s/ATOM :position] game-state))]
                      ;; Draw items to the screen
                      (bind-program shader-program)

                      (set-matrix-uniform proj-uniform proj-mat)
                      (set-matrix-uniform view-uniform (inverse camera-mat))
                      (set-matrix-uniform world-uniform world-mat)
                      (set-int-uniform texture-uniform 0)
                      (set-vector-3-uniform camera-position-uniform
                                            (s/select-first [s/ATOM :camera :position] game-state))

                      (render-mesh cube texture-id)

                      (bind-program nil)

                      ;; Flip buffer
                      (swap-window @window)
                      (poll-events)))))))))))
  nil)
