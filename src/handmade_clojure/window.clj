(ns handmade-clojure.window
  (:require [handmade-clojure.resource :refer [dispose]]
            [org.suskeyhose.imports :refer [import-static-all]])
  (:import [org.lwjgl.glfw GLFWErrorCallback GLFWKeyCallback GLFWMouseButtonCallback
            GLFWCursorPosCallback GLFWScrollCallback GLFWCursorEnterCallback
            GLFWFramebufferSizeCallback]))

(import-static-all org.lwjgl.glfw.GLFW
                   org.lwjgl.glfw.GLFWErrorCallback
                   org.lwjgl.glfw.Callbacks
                   org.lwjgl.system.MemoryUtil)

(defmacro with-valid-window
  [w s & forms]
  `(if-not (or (= ~w NULL)
               (nil? ~w))
     (do ~@forms)
     (throw (IllegalStateException. (str "Cannot " ~s " without a window.")))))

(defn set-callback
  [window-id kw f]
  (case kw
    :error-callback (glfwSetErrorCallback
                     (proxy [GLFWErrorCallback] []
                       (invoke [error description] (f error description))))
    :key-callback (with-valid-window window-id "set key callback"
                    (glfwSetKeyCallback
                     window-id
                     (proxy [GLFWKeyCallback] []
                       (invoke [window key scancode action mods]
                         (f window key scancode action mods)))))
    :mouse-button-callback (with-valid-window window-id "set mouse button callback"
                             (glfwSetMouseButtonCallback
                              window-id
                              (proxy [GLFWMouseButtonCallback] []
                                (invoke [window button action mods]
                                  (f window button action mods)))))
    :cursor-pos-callback (with-valid-window window-id "set cursor pos callback"
                           (glfwSetCursorPosCallback
                            window-id
                            (proxy [GLFWCursorPosCallback] []
                              (invoke [window x y]
                                (f window x y)))))
    :scroll-callback (with-valid-window window-id "set scroll callback"
                       (glfwSetScrollCallback
                        window-id
                        (proxy [GLFWScrollCallback] []
                          (invoke [window x y]
                            (f window x y)))))
    :cursor-enter-callback (with-valid-window window-id "set cursor enter callback"
                             (glfwSetCursorEnterCallback
                              window-id
                              (proxy [GLFWCursorEnterCallback] []
                                (invoke [window entered]
                                  (f window entered)))))
    :framebuffer-size-callback (with-valid-window window-id "set framebuffer size callback"
                                 (glfwSetFramebufferSizeCallback
                                  window-id
                                  (proxy [GLFWFramebufferSizeCallback] []
                                    (invoke [window width height]
                                      (f window width height)))))
    (throw (IllegalArgumentException. "Invalid callback passed to GLFW."))))

(defn init-window
  [width height title]
  (glfwSetErrorCallback (createPrint System/err))

  (if-not (glfwInit)
    (throw (IllegalStateException. "Unable to initialize GLFW")))

  (glfwDefaultWindowHints)
  (glfwWindowHint GLFW_VISIBLE GLFW_FALSE)
  (glfwWindowHint GLFW_RESIZABLE GLFW_TRUE)
  (glfwWindowHint GLFW_CONTEXT_VERSION_MAJOR 3)
  (glfwWindowHint GLFW_CONTEXT_VERSION_MINOR 2)
  (glfwWindowHint GLFW_OPENGL_PROFILE GLFW_OPENGL_CORE_PROFILE)
  (glfwWindowHint GLFW_OPENGL_FORWARD_COMPAT GLFW_TRUE)

  (let [window-id (glfwCreateWindow width height title NULL NULL)]
    (if (= window-id NULL)
      (throw (RuntimeException. "Failed to create GLFW window (graphics card may not support OpenGL 3.2)")))

    (let [vidmode (glfwGetVideoMode (glfwGetPrimaryMonitor))]
      (glfwSetWindowPos
       window-id
       (-> (.width vidmode) (- width) (/ 2))
       (-> (.height vidmode) (- height) (/ 2))))

    (glfwMakeContextCurrent window-id)
    (glfwSwapInterval 1)
    (glfwSetWindowSizeLimits window-id 400 300 GLFW_DONT_CARE GLFW_DONT_CARE)
    (glfwShowWindow window-id)

    window-id))

(defmethod dispose :window
  [_ window-id]
  (try (with-valid-window window-id "free callbacks and destroy window"
         (glfwFreeCallbacks window-id)
         (glfwDestroyWindow window-id))
       (catch Exception e nil))
  (glfwTerminate)
  (.free (glfwSetErrorCallback nil)))

(defn swap-window
  [window-id]
  (glfwSwapBuffers window-id))

(defn poll-events
  []
  (glfwPollEvents))

(defn should-close?
  [window-id]
  (glfwWindowShouldClose window-id))
