(ns handmade-clojure.util)

(defmacro once-only [[& names] & body]
  (let [gensyms (repeatedly (count names) gensym)]
    `(let [~@(mapcat (fn [g] `(~g (gensym))) gensyms)]
       `(let ~(into [] (apply concat ~(mapv (fn [g n] ``(~~g ~~n)) gensyms names)))
          ~(let [~@(mapcat (fn [n g] `(~n ~g)) names gensyms)]
             ~@body)))))

(defn ^:private create-cond-clause
  ([e default]
   `(true ~default))
  ([e test-case result]
   `((= ~test-case ~e) ~result)))

(defmacro case-cond
  [e & clauses]
  (once-only
   [e]
   (let [clauses (partition-all 2 clauses)]
     `(cond ~@(mapcat (partial apply create-cond-clause e) clauses)))))

(def ^:const int-array-type (Class/forName "[I"))
(defn int-array?
  [a]
  (instance? int-array-type a))

(def ^:const float-array-type (Class/forName "[F"))
(defn float-array?
  [a]
  (instance? float-array-type a))
