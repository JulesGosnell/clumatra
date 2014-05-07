(ns clumatra.sorted-map-test
  (:import  [java.lang.reflect Method]
            [java.util Collection Map Iterator Comparator]
            [clojure.lang Associative IFn AFn ISeq IPersistentMap IMapEntry APersistentMap PersistentTreeMap PersistentTreeMap$NodeIterator PersistentTreeMap$Node])
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
    (fetch-method APersistentMap "clear" [])
    (fetch-method APersistentMap "putAll" [Map])
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

    (fetch-method PersistentTreeMap "containsKey" [Object])
    (fetch-method PersistentTreeMap "create" [ISeq])
    (fetch-method PersistentTreeMap "create" [Comparator ISeq])
    (fetch-method PersistentTreeMap "doCompare" [Object Object])
    (fetch-method PersistentTreeMap "entryKey" [Object])
    (fetch-method PersistentTreeMap "kvreduce" [IFn Object])
    (fetch-method PersistentTreeMap "meta" [])
    (fetch-method PersistentTreeMap "reverseIterator" [])
    (fetch-method PersistentTreeMap "seqFrom" [Object Boolean/TYPE])

    (fetch-method PersistentTreeMap "assoc" PersistentTreeMap [Object Object])
    (fetch-method PersistentTreeMap "assoc" Associative [Object Object])
    (fetch-method PersistentTreeMap "assoc" IPersistentMap [Object Object])
    (fetch-method PersistentTreeMap "assocEx" PersistentTreeMap [Object Object])
    (fetch-method PersistentTreeMap "assocEx" IPersistentMap [Object Object])
    (fetch-method PersistentTreeMap "entryAt" PersistentTreeMap$Node [Object])
    (fetch-method PersistentTreeMap "entryAt" IMapEntry [Object])
    (fetch-method PersistentTreeMap "iterator" PersistentTreeMap$NodeIterator [])
    (fetch-method PersistentTreeMap "iterator" Iterator [])
    (fetch-method PersistentTreeMap "valAt" Object [Object Object])
    (fetch-method PersistentTreeMap "valAt" Object [Object])
    (fetch-method PersistentTreeMap "without" IPersistentMap [Object])
    (fetch-method PersistentTreeMap "without" PersistentTreeMap [Object])
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^Method m]
     (not
      (contains?
       #{Object AFn Iterable Map Collection}
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
