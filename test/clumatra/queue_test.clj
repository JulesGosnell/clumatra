(ns clumatra.queue-test
  (:import  [java.lang.reflect Method]
            [java.util Collection List]
            [clojure.lang Obj IObj AFn ASeq IPersistentMap PersistentQueue])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.util :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method PersistentQueue "clear"     [])
    (fetch-method PersistentQueue "retainAll" [Collection])
    (fetch-method PersistentQueue "remove"    [Object])
    (fetch-method PersistentQueue "removeAll" [Collection])
    (fetch-method PersistentQueue "add"       [Object])
    (fetch-method PersistentQueue "addAll"    [Collection])
    (fetch-method PersistentQueue "toArray"   [(type->array-type Object)])
    (fetch-method PersistentQueue "iterator"  [])

    (fetch-method PersistentQueue "withMeta" IObj [IPersistentMap])
    (fetch-method PersistentQueue "withMeta" Obj [IPersistentMap])
    (fetch-method PersistentQueue "withMeta" PersistentQueue [IPersistentMap])
    })

(def input-fns
  {}
  )

(deftest-kernels
  (filter
   (fn [^Method m]
     (not
      (contains?
       #{Object AFn ASeq Obj Iterable List Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentQueue excluded-methods))
  (fn [i] (into PersistentQueue/EMPTY (map (fn [j] (* i j)) (range *wavefront-size*))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.queue-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
