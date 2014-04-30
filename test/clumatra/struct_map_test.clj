(ns clumatra.struct-map-test
  (:import  [java.util Collection Map]
            [clojure.lang AFn IPersistentMap APersistentMap PersistentStructMap PersistentStructMap$Def ISeq])
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
     (fetch-method APersistentMap "values" [])

     (fetch-method PersistentStructMap "assocEx"       [Object Object])
     (fetch-method PersistentStructMap "construct"     [PersistentStructMap$Def ISeq])
     (fetch-method PersistentStructMap "containsKey"   [Object])
     (fetch-method PersistentStructMap "count"         [])
     (fetch-method PersistentStructMap "create"        [PersistentStructMap$Def ISeq])
     (fetch-method PersistentStructMap "createSlotMap" [ISeq])
     (fetch-method PersistentStructMap "empty"         [])
     (fetch-method PersistentStructMap "entryAt"       [Object])
     (fetch-method PersistentStructMap "getAccessor"   [PersistentStructMap$Def Object])
     (fetch-method PersistentStructMap "iterator"      [])
     (fetch-method PersistentStructMap "meta"          [])
     (fetch-method PersistentStructMap "seq"           [])
     (fetch-method PersistentStructMap "withMeta"      [IPersistentMap])
     (fetch-method PersistentStructMap "without"       [Object])

     (fetch-method PersistentStructMap "valAt" Object [Object])
     (fetch-method PersistentStructMap "valAt" Object [Object Object])
    })

(def input-fns {})

(defstruct s :input)

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{Object Iterable Map Collection
          AFn}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentStructMap excluded-methods))
  (fn [i] (struct-map s :input i))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.struct-map-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
