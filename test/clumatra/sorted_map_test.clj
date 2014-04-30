(ns clumatra.sorted-map-test
  (:import  [java.util Collection Map Iterator]
            [clojure.lang Associative AFn ISeq IPersistentMap IMapEntry APersistentMap PersistentTreeMap PersistentTreeMap$NodeIterator])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method APersistentMap "clear" [])
    (fetch-method APersistentMap "putAll" [java.util.Map])
    (fetch-method APersistentMap "put" [Object Object])

    (fetch-method APersistentMap "count" [])
    (fetch-method APersistentMap "empty" [])
    (fetch-method APersistentMap "get" [Object])
    (fetch-method APersistentMap "invoke" [Object])
    (fetch-method APersistentMap "invoke" [Object Object])
    (fetch-method APersistentMap "remove" [Object])
    (fetch-method APersistentMap "seq" [])
    (fetch-method PersistentTreeMap "seq" [Boolean/TYPE])
    (fetch-method APersistentMap "valAt" [Object Object])
    (fetch-method APersistentMap "valAt" [Object])
    (fetch-method APersistentMap "entryAt" [Object])
    (fetch-method APersistentMap "values" [])
    (fetch-method PersistentTreeMap "keys" [])
    (fetch-method PersistentTreeMap "keys" [PersistentTreeMap$NodeIterator])
    (fetch-method PersistentTreeMap "vals" [])
    (fetch-method PersistentTreeMap "vals" [PersistentTreeMap$NodeIterator])

    (fetch-method PersistentTreeMap "assoc" [Object Object])
    (fetch-method PersistentTreeMap "assocEx" [Object Object])
    (fetch-method PersistentTreeMap "containsKey" [Object])
    (fetch-method PersistentTreeMap "create" [clojure.lang.ISeq])
    (fetch-method PersistentTreeMap "create" [java.util.Comparator clojure.lang.ISeq])
    (fetch-method PersistentTreeMap "doCompare" [Object Object])
    (fetch-method PersistentTreeMap "entryAt" [Object])
    (fetch-method PersistentTreeMap "entryKey" [Object])
    (fetch-method PersistentTreeMap "iterator" [])
    (fetch-method PersistentTreeMap "kvreduce" [clojure.lang.IFn Object])
    (fetch-method PersistentTreeMap "meta" [])
    (fetch-method PersistentTreeMap "reverseIterator" [])
    (fetch-method PersistentTreeMap "seqFrom" [Object Boolean/TYPE])

    (fetch-method PersistentTreeMap "assoc" Associative [Object Object])
    (fetch-method PersistentTreeMap "assoc" IPersistentMap [Object Object])
    (fetch-method PersistentTreeMap "assocEx" IPersistentMap [Object Object])
    (fetch-method PersistentTreeMap "entryAt" IMapEntry [Object])
    (fetch-method PersistentTreeMap "iterator" Iterator [])
    (fetch-method PersistentTreeMap "valAt" Object [Object Object])
    (fetch-method PersistentTreeMap "valAt" Object [Object])
;;    (fetch-method PersistentTreeMap "without" IPersistentMap [Object Object])
    (fetch-method PersistentTreeMap "without" IPersistentMap [Object])
    (fetch-method PersistentTreeMap "without" PersistentTreeMap [Object])
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Map java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentTreeMap excluded-methods))
  (fn [i] (sorted-map :input i))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.sorted-map-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
