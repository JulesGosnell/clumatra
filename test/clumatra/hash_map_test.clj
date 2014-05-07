(ns clumatra.hash-map-test
  (:import  [java.lang.reflect Method]
            [java.util Collection Map]
            [clojure.lang IObj AFn IFn ISeq IPersistentMap ITransientCollection APersistentMap PersistentHashMap Associative PersistentHashMap$TransientHashMap])
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
    (fetch-method APersistentMap "clear"   [])
    (fetch-method APersistentMap "count"   [])
    (fetch-method APersistentMap "empty"   [])
    (fetch-method APersistentMap "get"     [Object])
    (fetch-method APersistentMap "invoke"  [Object])
    (fetch-method APersistentMap "put"     [Object Object])
    (fetch-method APersistentMap "putAll"  [Map])
    (fetch-method APersistentMap "remove"  [Object])
    (fetch-method APersistentMap "seq"     [])
    (fetch-method APersistentMap "values"  [])

    (fetch-method PersistentHashMap "assocEx"         [Object Object])
    (fetch-method PersistentHashMap "containsKey"     [Object])
    (fetch-method PersistentHashMap "count"           [])
    (fetch-method PersistentHashMap "create"          [(type->array-type Object)])
    (fetch-method PersistentHashMap "create"          [IPersistentMap (type->array-type Object)])
    (fetch-method PersistentHashMap "create"          [ISeq])
    (fetch-method PersistentHashMap "createWithCheck" [(type->array-type Object)])
    (fetch-method PersistentHashMap "createWithCheck" [ISeq])
    (fetch-method PersistentHashMap "empty"           [])
    (fetch-method PersistentHashMap "entryAt"         [Object])
    (fetch-method PersistentHashMap "fold"            [Long/TYPE IFn IFn IFn IFn IFn IFn])
    (fetch-method PersistentHashMap "iterator"        [])
    (fetch-method PersistentHashMap "kvreduce"        [IFn Object])
    (fetch-method PersistentHashMap "meta"            [])
    (fetch-method PersistentHashMap "seq"             [])
    (fetch-method PersistentHashMap "without"         [Object])

    (fetch-method APersistentMap "valAt" Object [Object Object])
    (fetch-method APersistentMap "valAt" Object [Object])
    (fetch-method PersistentHashMap "asTransient" ITransientCollection [])
    (fetch-method PersistentHashMap "asTransient" PersistentHashMap$TransientHashMap [])
    (fetch-method PersistentHashMap "assoc" Associative [Object Object])
    (fetch-method PersistentHashMap "assoc" IPersistentMap [Object Object])
    (fetch-method PersistentHashMap "valAt" Object [Object Object])
    (fetch-method PersistentHashMap "valAt" Object [Object])
    (fetch-method PersistentHashMap "withMeta" IObj [IPersistentMap])
    (fetch-method PersistentHashMap "withMeta" PersistentHashMap [IPersistentMap])
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^Method m]
     (not
      (contains?
       #{Object AFn Iterable Map Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentHashMap excluded-methods))
  (fn [i] (reduce (fn [r j] (conj r [(* i j) j])) {} (range 1 (inc *wavefront-size*))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.hash-map-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
