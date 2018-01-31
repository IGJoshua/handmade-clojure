(ns handmade-clojure.rendering.mesh
  (:require [handmade-clojure.resource :refer [dispose with-dispose]]
            [clojure.spec.alpha :as s])
  (:import [org.lwjgl.opengl GL GL30 GL20 GL15 GL11]
           [java.nio FloatBuffer]
           [org.lwjgl.system MemoryUtil]
           [org.lwjgl BufferUtils]))

(s/def ::vao int?)
(s/def ::vbo int?)
(s/def ::index-vbo int?)
(s/def ::color-vbo int?)
(s/def ::vertex-count pos-int?)

(def ^:private int-array-type (Class/forName "[I"))
(defn ^:private int-array?
  [a]
  (instance? int-array-type a))

(s/def ::index-array int-array?)
(s/def ::mesh (s/keys :req-un [::vbo ::index-vbo ::vao ::vertex-count ::index-count
                               ::color-vbo]))

(s/def ::vertex (s/coll-of float?))
(s/def ::vertices (s/coll-of ::vertex))

(def ^:private float-array-type (Class/forName "[F"))
(defn ^:private float-array?
  [a]
  (instance? float-array-type a))

(defmethod dispose :memory
  [_ m]
  (when-not (nil? m)
    (MemoryUtil/memFree m)))

(defn create-mesh
  [vertices indices colors]
  (let [vert-buffer (MemoryUtil/memAllocFloat (count vertices))
        vert-count (/ (count vertices) 3)
        vert-array (if (float-array? vertices)
                     vertices
                     (into-array Float/TYPE vertices))
        index-buffer (MemoryUtil/memAllocInt (count indices))
        index-array (if (int-array? indices)
                      indices
                      (into-array Integer/TYPE indices))
        color-buffer (MemoryUtil/memAllocFloat (count colors))
        color-array (if (float-array? colors)
                      colors
                      (into-array Float/TYPE colors))
        vao (GL30/glGenVertexArrays)
        vbo (GL15/glGenBuffers)
        index-vbo (GL15/glGenBuffers)
        color-vbo (GL15/glGenBuffers)]
    (with-dispose :memory vert-buffer
      (with-dispose :memory index-buffer
        (with-dispose :memory color-buffer
          (.. vert-buffer (put vert-array) (flip))
          (.. index-buffer (put index-array) (flip))
          (.. color-buffer (put color-array) (flip))

          (GL30/glBindVertexArray vao)

          (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER vbo)
          (GL15/glBufferData GL15/GL_ARRAY_BUFFER vert-buffer GL15/GL_STATIC_DRAW)
          (GL20/glVertexAttribPointer 0 3 GL11/GL_FLOAT false 0 0)

          (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER color-vbo)
          (GL15/glBufferData GL15/GL_ARRAY_BUFFER color-buffer GL15/GL_STATIC_DRAW)
          (GL20/glVertexAttribPointer 1 3 GL11/GL_FLOAT false 0 0)

          (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER index-vbo)
          (GL15/glBufferData GL15/GL_ELEMENT_ARRAY_BUFFER index-buffer GL15/GL_STATIC_DRAW)

          (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER 0)
          (GL30/glBindVertexArray 0))))
    {:vbo vbo :vao vao :vertex-count vert-count :index-vbo index-vbo :index-count (count indices)
     :color-vbo color-vbo}))
(s/fdef create-mesh
        :args (s/cat :vertices ::vertices)
        :ret ::mesh)

(defn dispose-mesh
  [mesh]
  (GL20/glDisableVertexAttribArray 0)
  (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER 0)
  (GL15/glDeleteBuffers (:color-vbo mesh))
  (GL15/glDeleteBuffers (:vbo mesh))
  (GL15/glDeleteBuffers (:index-vbo mesh))
  (GL30/glBindVertexArray 0)
  (GL30/glDeleteVertexArrays (:vao mesh))
  nil)
(s/fdef dispose-mesh
        :args (s/cat :mesh ::mesh)
        :ret nil?)

(defmethod dispose :mesh
  [_ mesh]
  (dispose-mesh mesh))

(defn render-mesh
  [mesh]
  (GL30/glBindVertexArray (:vao mesh))
  (GL20/glEnableVertexAttribArray 0)
  (GL20/glEnableVertexAttribArray 1)
  (GL11/glDrawElements GL11/GL_TRIANGLES (:index-count mesh) GL11/GL_UNSIGNED_INT 0)
  (GL20/glDisableVertexAttribArray 0)
  (GL20/glDisableVertexAttribArray 1)
  (GL30/glBindVertexArray 0))
