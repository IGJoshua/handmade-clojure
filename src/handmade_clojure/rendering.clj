(ns handmade-clojure.rendering
  (:require [handmade-clojure.resource :refer [dispose]]
            [clojure.core.matrix :as m])
  (:import [org.lwjgl.opengl GL GL11 GL15 GL20 GL30]
           [org.lwjgl.system MemoryStack MemoryUtil]
           [org.lwjgl BufferUtils]))

(m/set-current-implementation :vectorz)

(defn init-opengl
  [red green blue alpha]
  (GL/createCapabilities)
  (GL11/glClearColor red green blue alpha))

(defn set-clear-color
  [red green blue alpha]
  (GL11/glClearColor red green blue alpha))

(defn clear-screen
  []
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)))

(defn create-shader
  [shader-type shader-source]
  (let [shader (GL20/glCreateShader (case shader-type
                                      :vertex-shader GL20/GL_VERTEX_SHADER
                                      :fragment-shader GL20/GL_FRAGMENT_SHADER
                                      shader-type))]
    (when (= shader 0)
      (throw (RuntimeException. (str "Unable to create shader of type: " shader-type))))
    (GL20/glShaderSource shader shader-source)
    (GL20/glCompileShader shader)
    (when (= (GL20/glGetShaderi shader GL20/GL_COMPILE_STATUS) 0)
      (throw (RuntimeException. (str "Unable to compile shader of type: " shader-type "\n"
                                     (GL20/glGetShaderInfoLog shader)))))
    shader))

(defn dispose-shader
  [program shader]
  (when-not (= shader 0)
    (GL20/glDetachShader program shader)))

(defmethod dispose :shader
  [_ [program shader]]
  (dispose-shader program shader))

(defn create-shader-program
  [shaders]
  (let [program (GL20/glCreateProgram)]
    (doseq [shader shaders]
      (GL20/glAttachShader program shader))
    (GL20/glLinkProgram program)
    (when (= (GL20/glGetProgrami program GL20/GL_LINK_STATUS) 0)
      (throw (RuntimeException. (str "Unable to link the shader program.\n"
                                     (GL20/glGetProgramInfoLog program)))))
    (doseq [shader shaders]
      (dispose-shader program shader))
    (GL20/glValidateProgram program)
    (when (= (GL20/glGetProgrami program GL20/GL_VALIDATE_STATUS) 0)
      (binding [*out* *err*]
        (println (str "Unable to validate shader program.\n"
                      (GL20/glGetProgramInfoLog program)))))
    program))

(defn bind-program
  [program]
  (GL20/glUseProgram (if program program 0)))

(defn dispose-program
  [program]
  (GL20/glDeleteProgram program))

(defmethod dispose :shader-program
  [_ program]
  (dispose-program program))

(defn get-uniform
  [program name]
  (let [uniform (GL20/glGetUniformLocation program name)]
    (when (< uniform 0)
      (throw (RuntimeException. (str "Unable to find uniform: " name))))
    uniform))

(defn set-matrix-uniform
  [uniform mat]
  (let [mat-array (into-array Float/TYPE (m/to-vector mat))]
   (GL20/glUniformMatrix4fv uniform false mat-array)))
