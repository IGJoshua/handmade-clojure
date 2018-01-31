(ns handmade-clojure.window
  (:require [handmade-clojure.resource :refer [dispose]])
  (:import [org.lwjgl.glfw GLFW GLFWErrorCallback GLFWKeyCallback GLFWMouseButtonCallback
            GLFWCursorPosCallback GLFWScrollCallback GLFWCursorEnterCallback
            GLFWFramebufferSizeCallback Callbacks]
           [org.lwjgl.system MemoryUtil]))

(defmacro with-valid-window
  [w s & forms]
  `(if-not (or (= ~w MemoryUtil/NULL)
               (nil? ~w))
     (do ~@forms)
     (throw (IllegalStateException. (str "Cannot " ~s " without a window.")))))

(defn set-callback
  [window-id kw f]
  (case kw
    :error-callback (GLFW/glfwSetErrorCallback
                     (proxy [GLFWErrorCallback] []
                       (invoke [error description] (f error description))))
    :key-callback (with-valid-window window-id "set key callback"
                    (GLFW/glfwSetKeyCallback
                     window-id
                     (proxy [GLFWKeyCallback] []
                       (invoke [window key scancode action mods]
                         (f window key scancode action mods)))))
    :mouse-button-callback (with-valid-window window-id "set mouse button callback"
                             (GLFW/glfwSetMouseButtonCallback
                              window-id
                              (proxy [GLFWMouseButtonCallback] []
                                (invoke [window button action mods]
                                  (f window button action mods)))))
    :cursor-pos-callback (with-valid-window window-id "set cursor pos callback"
                           (GLFW/glfwSetCursorPosCallback
                            window-id
                            (proxy [GLFWCursorPosCallback] []
                              (invoke [window x y]
                                (f window x y)))))
    :scroll-callback (with-valid-window window-id "set scroll callback"
                       (GLFW/glfwSetScrollCallback
                        window-id
                        (proxy [GLFWScrollCallback] []
                          (invoke [window x y]
                            (f window x y)))))
    :cursor-enter-callback (with-valid-window window-id "set cursor enter callback"
                             (GLFW/glfwSetCursorEnterCallback
                              window-id
                              (proxy [GLFWCursorEnterCallback] []
                                (invoke [window entered]
                                  (f window entered)))))
    :framebuffer-size-callback (with-valid-window window-id "set framebuffer size callback"
                                 (GLFW/glfwSetFramebufferSizeCallback
                                  window-id
                                  (proxy [GLFWFramebufferSizeCallback] []
                                    (invoke [window width height]
                                      (f window width height)))))
    (throw (IllegalArgumentException. "Invalid callback passed to GLFW."))))

(defn init-window
  [width height title]
  (GLFW/glfwSetErrorCallback (GLFWErrorCallback/createPrint System/err))

  (if-not (GLFW/glfwInit)
    (throw (IllegalStateException. "Unable to initialize GLFW")))

  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 3)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 2)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GLFW/GLFW_TRUE)

  (let [window-id (GLFW/glfwCreateWindow width height title MemoryUtil/NULL MemoryUtil/NULL)]
    (if (= window-id MemoryUtil/NULL)
      (throw (RuntimeException. "Failed to create GLFW window (graphics card may not support OpenGL 3.2)")))

    (let [vidmode (GLFW/glfwGetVideoMode (GLFW/glfwGetPrimaryMonitor))]
      (GLFW/glfwSetWindowPos
       window-id
       (-> (.width vidmode) (- width) (/ 2))
       (-> (.height vidmode) (- height) (/ 2))))

    (GLFW/glfwMakeContextCurrent window-id)
    (GLFW/glfwSwapInterval 1)
    (GLFW/glfwShowWindow window-id)

    window-id))

(defn dispose-window
  [window-id]
  (try (with-valid-window window-id "free callbacks and destroy window"
         (Callbacks/glfwFreeCallbacks window-id)
         (GLFW/glfwDestroyWindow window-id))
       (catch Exception e nil))
  (GLFW/glfwTerminate)
  (.free (GLFW/glfwSetErrorCallback nil)))

(defmethod dispose :window
  [_ window-id]
  (dispose-window window-id))

(defn swap-window
  [window-id]
  (GLFW/glfwSwapBuffers window-id))

(defn poll-events
  []
  (GLFW/glfwPollEvents))

(defn should-close?
  [window-id]
  (GLFW/glfwWindowShouldClose window-id))
