(ns handmade-clojure.rendering.mesh
  (:require [handmade-clojure.resource :refer [dispose with-dispose]]
            [handmade-clojure.util :refer [int-array? float-array?]]
            [clojure.spec.alpha :as s]
            [org.suskeyhose.imports :refer [import-static-all]]
            [clojure.java.io :as io]
            [clojure.string :as str])
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
(s/def ::normal-vbo int?)
(s/def ::vertex-count pos-int?)

(s/def ::index-array int-array?)
(s/def ::mesh (s/keys :req-un [::vbo ::index-vbo ::vao ::vertex-count ::index-count ::uv-vbo ::normal-vbo]))

(s/def ::vertex (s/coll-of float?))
(s/def ::vertices (s/coll-of ::vertex))

(defmethod dispose :memory
  [_ ^java.nio.Buffer m]
  (when-not (nil? m)
    (memFree m)))

(defn create-mesh
  [vertices indices uvs normals]
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
        normal-buffer (memAllocFloat (count normals))
        ^floats normal-array (if (float-array? normals)
                               normals
                               (into-array Float/TYPE normals))
        vao (glGenVertexArrays)
        vbo (glGenBuffers)
        index-vbo (glGenBuffers)
        uv-vbo (glGenBuffers)
        normal-vbo (glGenBuffers)]
    (with-dispose :memory [vert-buffer index-buffer uv-buffer normal-buffer]
      (.. vert-buffer (put vert-array) (flip))
      (.. index-buffer (put index-array) (flip))
      (.. uv-buffer (put uv-array) (flip))
      (.. normal-buffer (put normal-array) (flip))

      (glBindVertexArray vao)

      (glBindBuffer GL_ARRAY_BUFFER vbo)
      (glBufferData GL_ARRAY_BUFFER vert-buffer GL_STATIC_DRAW)
      (glVertexAttribPointer 0 3 GL_FLOAT false 0 0)

      (glBindBuffer GL_ARRAY_BUFFER uv-vbo)
      (glBufferData GL_ARRAY_BUFFER uv-buffer GL_STATIC_DRAW)
      (glVertexAttribPointer 1 2 GL_FLOAT false 0 0)

      (glBindBuffer GL_ARRAY_BUFFER normal-vbo)
      (glBufferData GL_ARRAY_BUFFER normal-buffer GL_STATIC_DRAW)
      (glVertexAttribPointer 2 3 GL_FLOAT false 0 0)

      (glBindBuffer GL_ELEMENT_ARRAY_BUFFER index-vbo)
      (glBufferData GL_ELEMENT_ARRAY_BUFFER index-buffer GL_STATIC_DRAW)

      (glBindBuffer GL_ARRAY_BUFFER 0)
      (glBindVertexArray 0))
    {:vbo vbo :vao vao :vertex-count vert-count :index-vbo index-vbo
     :index-count (count indices) :uv-vbo uv-vbo :normal-vbo normal-vbo}))
(s/fdef create-mesh
        :args (s/cat :vertices ::vertices)
        :ret ::mesh)

(defmethod dispose :mesh
  [_ mesh]
  (glDisableVertexAttribArray 0)
  (glBindBuffer GL_ARRAY_BUFFER 0)
  (glDeleteBuffers ^long (:vbo mesh))
  (glDeleteBuffers ^long (:uv-vbo mesh))
  (glDeleteBuffers ^long (:index-vbo mesh))
  (glBindVertexArray 0)
  (glDeleteVertexArrays ^long (:vao mesh)))

(defn render-mesh
  [mesh texture-id]
  (glActiveTexture GL_TEXTURE0)
  (glBindTexture GL_TEXTURE_2D texture-id)
  (glBindVertexArray (:vao mesh))
  (glEnableVertexAttribArray 0)
  (glEnableVertexAttribArray 1)
  (glEnableVertexAttribArray 2)
  (glDrawElements GL_TRIANGLES (:index-count mesh) GL_UNSIGNED_INT 0)
  (glDisableVertexAttribArray 0)
  (glDisableVertexAttribArray 1)
  (glDisableVertexAttribArray 2)
  (glBindTexture GL_TEXTURE_2D NULL)
  (glBindVertexArray NULL))

(defmulti load-mesh (fn [^String path] (.substring path (inc (str/last-index-of path ".")))))

(defmethod load-mesh :default
  [path]
  (throw (Exception. (str "Invalid mesh type: " path))))

(defn ^:private parse-face
  [s]
  )

(defmethod load-mesh "obj"
  [path]
  (let [lines (str/split-lines (slurp (io/resource path)))
        verts (atom [])
        uvs (atom [])
        normals (atom [])
        faces (atom [])]
    (loop [line (first lines)
           lines (rest lines)]
      (let [tokens (str/split line #"\s")
            t (first tokens)
            tokens (rest tokens)]
        (case t
          "v" (swap! verts
                     #(apply conj %
                             (for [i (range 0 3)]
                               (Float/parseFloat (nth tokens i)))))
          "vt" (swap! uvs
                      #(apply conj %
                              (for [i (range 0 2)]
                                (Float/parseFloat (nth tokens i)))))
          "vn" (swap! normals
                      #(apply conj %
                              (for [i (range 0 3)]
                                (Float/parseFloat (nth tokens i)))))
          "f" (swap! faces
                     #(apply conj %
                             (for [i (range 0 3)]
                               (parse-face (nth tokens i)))))
          nil))
      (when (seq lines)
        (recur (first lines) (rest lines))))
    ))
