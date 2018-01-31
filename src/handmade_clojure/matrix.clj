(ns handmade-clojure.matrix
  (:require [clojure.core.matrix :as m]))

(m/set-current-implementation :vectorz)

(def identity-matrix
  (m/array [[1 0 0 0]
            [0 1 0 0]
            [0 0 1 0]
            [0 0 0 1]]))

(defn projection-matrix
  [fov aspect znear zfar]
  (let [scale (* (m/tan (/ (* fov 0.5 3.1415926535897) 180)) znear)
        width (* aspect scale 2)]
    (m/transpose
     (m/array [[(/ (* 2 znear) width) 0 0 0]
               [0 (/ (* 2 znear) (* 2 scale)) 0 0]
               [0 0 (- 0 (/ (+ zfar znear) (- zfar znear))) (/ (* -2 zfar znear) (- zfar znear))]
               [0 0 -1 0]]))))

(defn translation-matrix
  [x y z]
  (m/transpose
   (m/array [[1 0 0 x]
             [0 1 0 y]
             [0 0 1 z]
             [0 0 0 1]])))

(defn x-rotation-matrix
  [degrees]
  (let [rads (m/to-radians degrees)
        s (m/sin rads)
        c (m/cos rads)]
    (m/transpose
     (m/array [[1 0 0 0]
               [0 c (- 0 s) 0]
               [0 s c 0]
               [0 0 0 1]]))))

(defn y-rotation-matrix
  [degrees]
  (let [rads (m/to-radians degrees)
        s (m/sin rads)
        c (m/cos rads)]
    (m/transpose
     (m/array [[c 0 s 0]
               [0 1 0 0]
               [(- 0 s) 0 c 0]
               [0 0 0 1]]))))

(defn z-rotation-matrix
  [degrees]
  (let [rads (m/to-radians degrees)
        s (m/sin rads)
        c (m/cos rads)]
    (m/transpose
     (m/array [[c (- 0 s) 0 0]
               [s c 0 0]
               [0 0 1 0]
               [0 0 0 1]]))))
