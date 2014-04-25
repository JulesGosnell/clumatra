(ns clumatra.sorted-set-test
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (.getMethod clojure.lang.APersistentSet "add" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentSet "addAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.APersistentSet "get" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentSet "invoke" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentSet "iterator" (into-array Class []))
    (.getMethod clojure.lang.APersistentSet "remove" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentSet "removeAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.APersistentSet "retainAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.APersistentSet "toArray" (into-array Class [(type->array-type Object)]))

    (.getMethod clojure.lang.PersistentTreeSet "clear" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeSet "comparator" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeSet "cons" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentTreeSet "create" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentTreeSet "create" (into-array Class [java.util.Comparator clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentTreeSet "disjoin" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentTreeSet "empty" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeSet "entryKey" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentTreeSet "meta" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeSet "rseq" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeSet "seq" (into-array Class [Boolean/TYPE]))
    (.getMethod clojure.lang.PersistentTreeSet "seqFrom" (into-array Class [Object Boolean/TYPE]))
    (.getMethod clojure.lang.PersistentTreeSet "withMeta" (into-array Class [clojure.lang.IPersistentMap]))

    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "cons") (= (.getReturnType m) clojure.lang.IPersistentCollection))) (.getMethods clojure.lang.PersistentTreeSet)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods clojure.lang.PersistentTreeSet)))
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Set java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentTreeSet excluded-methods))
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
