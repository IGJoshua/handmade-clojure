(ns handmade-clojure.core
  (:require [handmade-clojure.window :refer [set-callback init-window swap-window
                                             poll-events should-close?]]
            [handmade-clojure.rendering :refer [init-opengl set-clear-color clear-screen
                                                create-shader create-shader-program
                                                bind-program get-uniform set-matrix-uniform
                                                set-int-uniform]]
            [handmade-clojure.rendering.mesh :refer [create-mesh render-mesh]]
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
                          nil)
        game-state (atom {:position [0 0 0]})
        vertex-shader-source (slurp (io/resource "shader/vertex.glsl"))
        fragment-shader-source (slurp (io/resource "shader/fragment.glsl"))]
    ;; Initialization
    (with-dispose :window @window
      ;; Create window and init OpenGL
      (reset! window (init-window @width @height title))
      (init-opengl 0 0 0 1)

      (glEnable GL_DEPTH_TEST)
      (glEnable GL_CULL_FACE)
      (glCullFace GL_BACK)
      (glFrontFace GL_CCW)

      (set-callback @window :framebuffer-size-callback resize-callback)
      (set-callback @window :key-callback (fn [window key scancode action mods]
                                            (if (or (= action GLFW_PRESS) (= action GLFW_REPEAT))
                                              (case-cond
                                               key
                                               GLFW_KEY_ESCAPE (glfwSetWindowShouldClose window true)
                                               GLFW_KEY_LEFT (s/transform [s/ATOM :position 0] #(- % 0.1)
                                                                          game-state)
                                               GLFW_KEY_RIGHT (s/transform [s/ATOM :position 0] #(+ 0.1 %)
                                                                           game-state)
                                               GLFW_KEY_UP (s/transform [s/ATOM :position 1] #(+ 0.1 %)
                                                                        game-state)
                                               GLFW_KEY_DOWN (s/transform [s/ATOM :position 1] #(- % 0.1)
                                                                          game-state)))))
      ;; Init the shaders and stuff
      (let [vert-shader (create-shader :vertex-shader vertex-shader-source)
            fragment-shader (create-shader :fragment-shader fragment-shader-source)
            shader-program (create-shader-program [vert-shader fragment-shader])]
        (with-dispose :shader-program shader-program
          ;; Define the geometry
          (let [cube (create-cube)
                aspect (atom (/ @width @height))
                proj-uniform (get-uniform shader-program "projectionMatrix")
                view-uniform (get-uniform shader-program "viewMatrix")
                world-uniform (get-uniform shader-program "worldMatrix")
                texture-uniform (get-uniform shader-program "texture_sampler")
                texture-id (glGenTextures)]
            (with-dispose :texture texture-id
              (if-let [[^java.nio.DirectByteBuffer texture-bytes ^long width ^long height] (load-texture "resources/texture/dirt.png")]
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
                  (when @resized?
                    (glViewport 0 0 @width @height)
                    (reset! aspect (/ @width @height))
                    (reset! resized? false))
                  ;; Clear screen
                  (clear-screen)

                  (let [proj-mat (projection-matrix 90 @aspect 0.10 1000)
                        camera-mat (translation-matrix 0 0 2)
                        world-mat (apply translation-matrix (s/select-first [s/ATOM :position] game-state))]
                    ;; Draw items to the screen
                    (bind-program shader-program)

                    (set-matrix-uniform proj-uniform proj-mat)
                    (set-matrix-uniform view-uniform (inverse camera-mat))
                    (set-matrix-uniform world-uniform world-mat)
                    (set-int-uniform texture-uniform 0)

                    (render-mesh cube texture-id)

                    (bind-program nil)

                    ;; Flip buffer
                    (swap-window @window)
                    (poll-events)))))))))))
