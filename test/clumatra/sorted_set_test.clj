(ns clumatra.sorted-set-test
  (:import  [java.util Collection Map]
            [clojure.lang AFn IObj ISeq IPersistentSet IPersistentCollection PersistentTreeSet APersistentSet])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method APersistentSet "add"       [Object])
    (fetch-method APersistentSet "addAll"    [Collection])
    (fetch-method APersistentSet "get"       [Object])
    (fetch-method APersistentSet "invoke"    [Object])
    (fetch-method APersistentSet "iterator"  [])
    (fetch-method APersistentSet "remove"    [Object])
    (fetch-method APersistentSet "removeAll" [Collection])
    (fetch-method APersistentSet "retainAll" [Collection])
    (fetch-method APersistentSet "toArray"   [(type->array-type Object)])

    (fetch-method PersistentTreeSet "clear"      [])
    (fetch-method PersistentTreeSet "comparator" [])
    (fetch-method PersistentTreeSet "create"     [clojure.lang.ISeq])
    (fetch-method PersistentTreeSet "create"     [java.util.Comparator clojure.lang.ISeq])
    (fetch-method PersistentTreeSet "disjoin"    [Object])
    (fetch-method PersistentTreeSet "empty"      [])
    (fetch-method PersistentTreeSet "entryKey"   [Object])
    (fetch-method PersistentTreeSet "meta"       [])
    (fetch-method PersistentTreeSet "rseq"       [])
    (fetch-method PersistentTreeSet "seq"        [Boolean/TYPE])
    (fetch-method PersistentTreeSet "seqFrom"    [Object Boolean/TYPE])

    (fetch-method PersistentTreeSet "cons" IPersistentCollection [Object])
    (fetch-method PersistentTreeSet "cons" IPersistentSet [Object])
    (fetch-method PersistentTreeSet "withMeta" PersistentTreeSet [clojure.lang.IPersistentMap])
    (fetch-method PersistentTreeSet "withMeta" IObj [clojure.lang.IPersistentMap])
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Set Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentTreeSet excluded-methods))
  (fn [i] (into #{} (map (fn [j] (* i j)) (range 1 (inc *wavefront-size*)))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.sorted-set-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
