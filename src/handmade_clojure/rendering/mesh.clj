(ns handmade-clojure.rendering.mesh
  (:require [handmade-clojure.resource :refer [dispose with-dispose]]
            [clojure.spec.alpha :as s]
            [org.suskeyhose.imports :refer [import-static-all]])
  (:import [org.lwjgl.opengl GL GL30 GL20 GL15 GL11]
           [java.nio FloatBuffer]
           [org.lwjgl.system MemoryUtil]
           [org.lwjgl BufferUtils]))

(import-static-all org.lwjgl.opengl.GL
                   org.lwjgl.opengl.GL11
                   org.lwjgl.opengl.GL13
                   org.lwjgl.opengl.GL15
                   org.lwjgl.opengl.GL20
                   org.lwjgl.opengl.GL30
                   org.lwjgl.system.MemoryUtil
                   org.lwjgl.BufferUtils)

(s/def ::vao int?)
(s/def ::vbo int?)
(s/def ::index-vbo int?)
(s/def ::uv-vbo int?)
(s/def ::vertex-count pos-int?)

(def ^:private int-array-type (Class/forName "[I"))
(defn ^:private int-array?
  [a]
  (instance? int-array-type a))

(s/def ::index-array int-array?)
(s/def ::mesh (s/keys :req-un [::vbo ::index-vbo ::vao ::vertex-count ::index-count ::uv-vbo]))

(s/def ::vertex (s/coll-of float?))
(s/def ::vertices (s/coll-of ::vertex))

(def ^:private float-array-type (Class/forName "[F"))
(defn ^:private float-array?
  [a]
  (instance? float-array-type a))

(defmethod dispose :memory
  [_ ^java.nio.Buffer m]
  (when-not (nil? m)
    (memFree m)))

(defn create-mesh
  [vertices indices uvs]
  (let [vert-buffer (memAllocFloat (count vertices))
        vert-count (/ (count vertices) 3)
        ^floats vert-array (if (float-array? vertices)
                     vertices
                     (into-array Float/TYPE vertices))
        index-buffer (memAllocInt (count indices))
        ^ints index-array (if (int-array? indices)
                      indices
                      (into-array Integer/TYPE indices))
        uv-buffer (memAllocFloat (count uvs))
        ^floats uv-array (if (float-array? uvs)
                   uvs
                   (into-array Float/TYPE uvs))
        vao (glGenVertexArrays)
        vbo (glGenBuffers)
        index-vbo (glGenBuffers)
        uv-vbo (glGenBuffers)]
    (with-dispose :memory vert-buffer
      (with-dispose :memory index-buffer
        (with-dispose :memory uv-buffer
          (.. vert-buffer (put vert-array) (flip))
          (.. index-buffer (put index-array) (flip))
          (.. uv-buffer (put uv-array) (flip))

          (glBindVertexArray vao)

          (glBindBuffer GL_ARRAY_BUFFER vbo)
          (glBufferData GL_ARRAY_BUFFER vert-buffer GL_STATIC_DRAW)
          (glVertexAttribPointer 0 3 GL_FLOAT false 0 0)

          (glBindBuffer GL_ARRAY_BUFFER uv-vbo)
          (glBufferData GL_ARRAY_BUFFER uv-buffer GL_STATIC_DRAW)
          (glVertexAttribPointer 1 2 GL_FLOAT false 0 0)

          (glBindBuffer GL_ELEMENT_ARRAY_BUFFER index-vbo)
          (glBufferData GL_ELEMENT_ARRAY_BUFFER index-buffer GL_STATIC_DRAW)

          (glBindBuffer GL_ARRAY_BUFFER 0)
          (glBindVertexArray 0))))
    {:vbo vbo :vao vao :vertex-count vert-count :index-vbo index-vbo
     :index-count (count indices) :uv-vbo uv-vbo}))
(s/fdef create-mesh
        :args (s/cat :vertices ::vertices)
        :ret ::mesh)

(defn dispose-mesh
  [mesh]
  (glDisableVertexAttribArray 0)
  (glBindBuffer GL_ARRAY_BUFFER 0)
  (glDeleteBuffers ^long (:vbo mesh))
  (glDeleteBuffers ^long (:uv-vbo mesh))
  (glDeleteBuffers ^long (:index-vbo mesh))
  (glBindVertexArray 0)
  (glDeleteVertexArrays ^long (:vao mesh))
  nil)
(s/fdef dispose-mesh
        :args (s/cat :mesh ::mesh)
        :ret nil?)

(defmethod dispose :mesh
  [_ mesh]
  (dispose-mesh mesh))

(defn render-mesh
  [mesh texture-id]
  (glActiveTexture GL_TEXTURE0)
  (glBindTexture GL_TEXTURE_2D texture-id)
  (glBindVertexArray (:vao mesh))
  (glEnableVertexAttribArray 0)
  (glEnableVertexAttribArray 1)
  (glDrawElements GL_TRIANGLES (:index-count mesh) GL_UNSIGNED_INT 0)
  (glDisableVertexAttribArray 0)
  (glDisableVertexAttribArray 1)
  (glBindVertexArray 0))
