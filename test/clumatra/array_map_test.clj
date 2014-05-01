(ns clumatra.array-map-test
  (:import  [java.lang.reflect Method]
            [java.util Collection Map]
            [clojure.lang AFn IFn APersistentMap PersistentArrayMap ITransientCollection ITransientMap])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method APersistentMap "clear"  [])
    (fetch-method APersistentMap "put"    [Object Object])
    (fetch-method APersistentMap "putAll" [Map])

    (fetch-method APersistentMap "count"  [])
    (fetch-method APersistentMap "empty"  [])
    (fetch-method APersistentMap "get"    [Object])
    (fetch-method APersistentMap "invoke" [Object])
    (fetch-method APersistentMap "remove" [Object])
    (fetch-method APersistentMap "seq"    [])
    (fetch-method APersistentMap "valAt"  [Object Object])
    (fetch-method APersistentMap "valAt"  [Object])
    (fetch-method APersistentMap "values" [])

    (fetch-method PersistentArrayMap "createAsIfByAssoc" [(type->array-type Object)])
    (fetch-method PersistentArrayMap "createWithCheck"   [(type->array-type Object)])
    (fetch-method PersistentArrayMap "entryAt"           [Object])
    (fetch-method PersistentArrayMap "iterator"          [])
    (fetch-method PersistentArrayMap "kvreduce"          [IFn Object])
    (fetch-method PersistentArrayMap "meta"              [])

    (fetch-method PersistentArrayMap "asTransient" ITransientMap [])
    (fetch-method PersistentArrayMap "asTransient" ITransientCollection [])
    (fetch-method PersistentArrayMap "valAt"       Object [Object Object])
    (fetch-method PersistentArrayMap "valAt"       Object [Object])
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^Method m]
     (not
      (contains?
       #{Object AFn Iterable Map Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentArrayMap excluded-methods))
  (fn [i] (array-map :input i))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.array-map-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
