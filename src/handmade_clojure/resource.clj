(ns handmade-clojure.resource)

(defmulti dispose (fn [type val] type))

(defmethod dispose :default
  [t _]
  (throw (RuntimeException. (str "No implementation of dispose on type " t))))

(defmacro with-dispose [t v & forms]
  `(try
     ~@forms
     (finally
       (dispose ~t ~v))))
