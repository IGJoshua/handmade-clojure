(ns handmade-clojure.texture
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.suskeyhose.imports :refer [import-static-all import-static]]
            [handmade-clojure.resource :refer [dispose]])
  (:import [de.matthiasmann.twl.utils PNGDecoder PNGDecoder$Format]))

(import-static-all org.lwjgl.BufferUtils)

(import-static org.lwjgl.opengl.GL11 glDeleteTextures)

(defmulti load-texture (fn [^String path] (.substring path (inc (str/last-index-of path ".")))))

(defmethod load-texture "png"
  [path]
  (let [in (io/input-stream path)
        result (atom nil)]
    (try
      (let [decoder (PNGDecoder. in)
            width (.getWidth decoder)
            height (.getHeight decoder)
            buf (createByteBuffer (* 4 width height))]
        (.decode decoder buf (* 4 width) PNGDecoder$Format/RGBA)
        (.flip buf)
        (reset! result [buf width height]))
      (finally
        (.close in)))
    @result))

(defmethod dispose :texture
  [_ ^long val]
  (glDeleteTextures val))

#_(defn- read-chars
    ([file index count]
     (let [file-start-position (.position file)
           arr (char-array count)
           decoder (.newDecoder (Charset/forName "UTF-8"))
           out-buffer (BufferUtils/createCharBuffer count)]
       (.position file index)
       (.decode decoder file out-buffer true)
       (.position file file-start-position)
       (.position out-buffer 0)
       (.get out-buffer arr)
       (String. arr)))
    ([file count]
     (let [result (read-chars file (.position file) count)]
       (.position file (+ (.position file) count))
       result)))

#_(defmethod load-texture "png"
  [path]
  ;; Load a png file into a java.nio intbuffer
  (try
    (let [raf (RandomAccessFile. path "r")
          f (.map (.getChannel raf)
                  FileChannel$MapMode/READ_ONLY 0 (.length raf))]
      (when-not (and (= (.get f 0) -119)
                     (= (read-chars f 1 3) "PNG")
                     (= (.get f 4) 13)
                     (= (.get f 5) 10)
                     (= (.get f 6) 26)
                     (= (.get f 7) 10))
        (throw (IOException. "Invalid png, initial bytes malformed.")))
      (.position f 8)
      (let [eof (atom false)
            width (atom 0)
            height (atom 0)
            bit-depth (atom 0)
            color-type (atom nil)
            compression-method (atom nil)
            filter-method (atom nil)
            interlace-method (atom nil)
            color-buffers (atom [])]
        (let [header-length (.getInt f)
              header-type (read-chars f 4)]
          (when-not (= header-type "IHDR")
            (throw (IOException. "Invalid png, header missing.")))
          ;; FIXME: Make this support other things
          (reset! width (.getInt f))
          (reset! height (.getInt f))
          (reset! bit-depth (.get f))
          (reset! color-type (.get f))
          (.position f (+ (.position f) header-length 4)))
        (loop [length (.getInt f)
               chunk-type (read-chars f 4)]
          (when (> length (.remaining f))
            (throw (IOException. "Parse error, chunk length cannot be greater than remaining size")))
          (cond
            (= "PLTE" chunk-type)
            (do (println "PLTE found"))
            (= "IDAT" chunk-type)
            (let [color-buffer (BufferUtils/createFloatBuffer (/ (* length 8) @bit-depth))]
              (case @color-type
                                        ;0
                                        ;2
                                        ;3
                                        ;4
                                        ;6
                (throw (IOException. "Unsupported color type.")))
              (swap! color-buffers conj color-buffer))
            (= "IEND" chunk-type)
            (do (println "IEND found"))
            (Character/isUpperCase (.charAt chunk-type 0))
            (throw (IOException. "Invalid critical chunk identified."))
            true
            nil)
          (.position f (+ (.position f) length 4))
          (when-not (.hasRemaining f)
            (reset! eof true))
          (when-not @eof
            (recur (.getInt f) (read-chars f 4))))))
    (catch IOException e
      (binding [*out* *err*]
        (println (str "Error encountered loading file " path "\n" e))))))

