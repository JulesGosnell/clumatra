(ns clumatra.list-test
  (:import  [java.lang.reflect Method]
            [java.util Collection Map List Set]
            [clojure.lang Obj IObj AFn IFn ISeq ASeq IPersistentMap ITransientCollection PersistentList])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method PersistentList "reduce"   [IFn Object])
    (fetch-method PersistentList "reduce"   [IFn])

    (fetch-method PersistentList "withMeta" PersistentList [IPersistentMap])
    (fetch-method PersistentList "withMeta" IObj [IPersistentMap])
    (fetch-method PersistentList "withMeta" Obj [IPersistentMap])
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
   (extract-methods non-static? PersistentList excluded-methods))
  (fn [i] (into '() (map (fn [j] (* i j)) (range *wavefront-size*))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.list-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
