(ns handmade-clojure.core
  (:require [handmade-clojure.window :refer [set-callback init-window swap-window
                                             poll-events should-close?]]
            [handmade-clojure.rendering :refer [init-opengl set-clear-color clear-screen
                                                create-shader create-shader-program
                                                bind-program get-uniform set-matrix-uniform]]
            [handmade-clojure.rendering.mesh :refer [create-mesh render-mesh]]
            [handmade-clojure.resource :refer [with-dispose]]
            [handmade-clojure.matrix :refer [projection-matrix identity-matrix translation-matrix
                                             x-rotation-matrix y-rotation-matrix z-rotation-matrix]]
            [clojure.core.matrix :refer [mmul inverse]]
            [clojure.java.io :as io]
            [clojure.core.async :as a :refer [go]]
            [org.suskeyhose.imports :refer [import-static-all]])
  (:gen-class))

(import-static-all org.lwjgl.glfw.GLFW
                   org.lwjgl.opengl.GL11)

;; Basic structure of the game:
;; Init
;; Game Loop
  ;; Input
  ;; Update 0+ times, based on fixed timestep
  ;; Render
;; Cleanup

(def vertex-shader-source (slurp (io/resource "shaders/vertex.glsl")))
(def fragment-shader-source (slurp (io/resource "shaders/fragment.glsl")))

(defn -main
  []
  (let [width (atom 640)
        height (atom 480)
        resized? (atom false)
        title "Hello World!"
        window (atom nil)
        resize-callback (fn [window w h]
                          (reset! width w)
                          (reset! height h)
                          (reset! resized? true)
                          nil)]
    ;; Initialization
    (with-dispose :window @window
      ;; Create window and init OpenGL
      (reset! window (init-window @width @height title))
      (init-opengl 0 0 0 1)

      (set-callback @window :framebuffer-size-callback resize-callback)
      (set-callback @window :key-callback (fn [window key scancode action mods]
                                            (when (= key GLFW_KEY_ESCAPE)
                                              (glfwSetWindowShouldClose window true))
                                            (println (char key))
                                            (println (cond (= action GLFW_PRESS) :key-down
                                                           (= action GLFW_REPEAT) :key-repeat
                                                           (= action GLFW_RELEASE) :key-up
                                                           :default :key-error))))

      ;; Init the shaders and stuff
      (let [vert-shader (create-shader :vertex-shader vertex-shader-source)
            fragment-shader (create-shader :fragment-shader fragment-shader-source)
            shader-program (create-shader-program [vert-shader fragment-shader])]
        (with-dispose :shader-program shader-program
          ;; Define the geometry
          (let [vertices (float-array [-0.5  0.5 0.0
                                       -0.5 -0.5 0.0
                                        0.5 -0.5 0.0
                                        0.5  0.5 0.0])
                indices (int-array [0 1 3 3 1 2])
                color (float-array [0.5 0 0
                                    0 0.5 0
                                    0 0 0.5
                                    0 0.5 0.5])
                mesh (create-mesh vertices indices color)
                aspect (atom (/ @width @height))
                proj-mat (atom (projection-matrix 90 @aspect 0.10 1000))
                proj-uniform (get-uniform shader-program "projectionMatrix")
                camera-mat (atom (mmul (translation-matrix 0 0 2) (x-rotation-matrix -18)))
                view-uniform (get-uniform shader-program "viewMatrix")
                world-mat (mmul (y-rotation-matrix -45) (translation-matrix 0 0 0))
                world-uniform (get-uniform shader-program "worldMatrix")]
            (with-dispose :mesh mesh
              ;; Main loop
              (while (not (should-close? @window))
                (when @resized?
                  (glViewport 0 0 @width @height)
                  (reset! aspect (/ @width @height))
                  (reset! proj-mat (projection-matrix 90 @aspect 0.01 1000))
                  (reset! resized? false))
                ;; Clear screen
                (clear-screen)

                ;; Draw items to the screen
                (bind-program shader-program)
                (set-matrix-uniform proj-uniform @proj-mat)
                (set-matrix-uniform view-uniform (inverse @camera-mat))
                (set-matrix-uniform world-uniform world-mat)

                (render-mesh mesh)

                (bind-program nil)

                ;; Flip buffer
                (swap-window @window)
                (poll-events)))))))))
