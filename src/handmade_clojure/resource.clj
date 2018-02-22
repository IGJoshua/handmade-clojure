(ns handmade-clojure.resource)

(defmulti dispose (fn [type val] type))

(defmethod dispose :default
  [t _]
  (throw (RuntimeException. (str "No implementation of dispose on type " t))))

(defmacro with-dispose [t v & forms]
  (let [v (if (vector? v)
            v
            (vector v))]
    `(try
       ~@forms
       (finally
         ~@(map (fn [item] `(dispose ~t ~item)) v)))))
