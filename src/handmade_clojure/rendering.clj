(ns handmade-clojure.rendering
  (:require [handmade-clojure.resource :refer [dispose]]
            [handmade-clojure.util :refer [int-array? float-array?]]
            [clojure.core.matrix :as m]
            [org.suskeyhose.imports :refer [import-static-all]]))

(import-static-all org.lwjgl.opengl.GL
                   org.lwjgl.opengl.GL11
                   org.lwjgl.opengl.GL15
                   org.lwjgl.opengl.GL20
                   org.lwjgl.opengl.GL30)

(m/set-current-implementation :vectorz)

(defn init-opengl
  [red green blue alpha]
  (createCapabilities)
  (glClearColor red green blue alpha)
  (glEnable GL_DEPTH_TEST)
  (glEnable GL_CULL_FACE)
  (glCullFace GL_BACK)
  (glFrontFace GL_CCW))

(defn set-clear-color
  [red green blue alpha]
  (glClearColor red green blue alpha))

(defn clear-screen
  []
  (glClear (bit-or GL_COLOR_BUFFER_BIT GL_DEPTH_BUFFER_BIT)))

(defn create-shader
  [shader-type ^String shader-source]
  (let [shader (glCreateShader (case shader-type
                                 :vertex-shader GL_VERTEX_SHADER
                                 :fragment-shader GL_FRAGMENT_SHADER
                                 shader-type))]
    (when (= shader 0)
      (throw (RuntimeException. (str "Unable to create shader of type: " shader-type))))
    (glShaderSource shader shader-source)
    (glCompileShader shader)
    (when (= (glGetShaderi shader GL_COMPILE_STATUS) 0)
      (throw (RuntimeException. (str "Unable to compile shader of type: " shader-type "\n"
                                     (glGetShaderInfoLog shader)))))
    shader))

(defmethod dispose :shader
  [_ [program shader]]
  (when-not (= shader 0)
    (glDetachShader program shader)))

(defn create-shader-program
  [shaders]
  (let [program (glCreateProgram)]
    (doseq [shader shaders]
      (glAttachShader program shader))
    (glLinkProgram program)
    (when (= (glGetProgrami program GL_LINK_STATUS) 0)
      (throw (RuntimeException. (str "Unable to link the shader program.\n"
                                     (glGetProgramInfoLog program)))))
    (doseq [shader shaders]
      (dispose :shader [program shader]))
    (glValidateProgram program)
    (when (= (glGetProgrami program GL_VALIDATE_STATUS) 0)
      (binding [*out* *err*]
        (println (str "Unable to validate shader program.\n"
                      (glGetProgramInfoLog program)))))
    program))

(defn bind-program
  [program]
  (glUseProgram (if program program 0)))

(defmethod dispose :shader-program
  [_ program]
  (glDeleteProgram program))

(defn get-uniform
  [^long program ^String name]
  (let [uniform (glGetUniformLocation program name)]
    #_(when (< uniform 0)
      (throw (RuntimeException. (str "Unable to find uniform: " name))))
    uniform))

(defn set-matrix-uniform
  [^long uniform mat]
  (let [^floats mat-array (into-array Float/TYPE (m/to-vector mat))]
   (glUniformMatrix4fv uniform false mat-array)))

(defn set-int-uniform
  [^long uniform i]
  (glUniform1i uniform (int i)))

(defn set-float-uniform
  [^long uniform f]
  (glUniform1f uniform (float f)))

(defn set-vector-3-uniform
  [^long uniform vec]
  (let [^floats vec-array (if (float-array? vec)
                            vec
                            (into-array Float/TYPE vec))]
    (glUniform3fv uniform vec-array)))
