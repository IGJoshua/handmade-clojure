(ns handmade-clojure.rendering.primitives
  (:require [handmade-clojure.rendering.mesh :refer [create-mesh]]))

(defn create-cube []
  (let [positions (float-array [-0.5  0.5  0.5
                               -0.5 -0.5  0.5
                               0.5 -0.5  0.5
                               0.5  0.5  0.5 ; front face
                               -0.5  0.5 -0.5
                               0.5  0.5 -0.5
                               -0.5 -0.5 -0.5
                               0.5 -0.5 -0.5 ; back face
                               -0.5  0.5  0.5
                               -0.5 -0.5  0.5
                               -0.5 -0.5 -0.5
                               -0.5  0.5 -0.5 ; left face
                               0.5  0.5  0.5
                               0.5 -0.5  0.5
                               0.5 -0.5 -0.5
                               0.5  0.5 -0.5 ; right face
                               -0.5  0.5  0.5
                               -0.5  0.5 -0.5
                               0.5  0.5  0.5
                               0.5  0.5 -0.5 ; top face
                               -0.5 -0.5  0.5
                               -0.5 -0.5 -0.5
                               0.5 -0.5  0.5
                               0.5 -0.5 -0.5])  ; bottom face
        indices (int-array [ 0  1  3  3  1  2   ; front face
                            7  6  4  7  4  5    ; back face
                            10  9  8 11 10  8   ; left face
                            12 13 14 12 14 15   ; right face
                            16 18 17 17 18 19   ; top face
                            20 21 22 22 21 23]) ; bottom face
        uvs (float-array [0.0 0.0
                          0.0 1.0
                          1.0 1.0
                          1.0 0.0       ; front face
                          1.0 0.0
                          0.0 0.0
                          1.0 1.0
                          0.0 1.0       ; back face
                          1.0 0.0
                          1.0 1.0
                          0.0 1.0
                          0.0 0.0       ; left face
                          0.0 0.0
                          0.0 1.0
                          1.0 1.0
                          1.0 0.0       ; right face
                          0.0 1.0
                          0.0 0.0
                          1.0 1.0
                          1.0 0.0       ; top face
                          0.0 0.0
                          0.0 1.0
                          1.0 0.0
                          1.0 1.0])     ; bottom face
        normals (float-array [0 0 1
                              0 0 1
                              0 0 1
                              0 0 1
                              0 0 -1
                              0 0 -1
                              0 0 -1
                              0 0 -1
                              -1 0 0
                              -1 0 0
                              -1 0 0
                              -1 0 0
                              1 0 0
                              1 0 0
                              1 0 0
                              1 0 0
                              0 1 0
                              0 1 0
                              0 1 0
                              0 1 0
                              0 -1 0
                              0 -1 0
                              0 -1 0
                              0 -1 0])]
    (create-mesh positions uvs normals indices)))
