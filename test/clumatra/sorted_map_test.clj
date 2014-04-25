(ns clumatra.sorted-map-test
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (.getMethod clojure.lang.APersistentMap "clear" (into-array Class []))
    (.getMethod clojure.lang.APersistentMap "putAll" (into-array Class [java.util.Map]))
    (.getMethod clojure.lang.APersistentMap "put" (into-array Class [Object Object]))

    (.getMethod clojure.lang.APersistentMap "count" (into-array Class []))
    (.getMethod clojure.lang.APersistentMap "empty" (into-array Class []))
    (.getMethod clojure.lang.APersistentMap "get" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "invoke" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "invoke" (into-array Class [Object Object]))
    (.getMethod clojure.lang.APersistentMap "remove" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "seq" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "seq" (into-array Class [Boolean/TYPE]))
    (.getMethod clojure.lang.APersistentMap "valAt" (into-array Class [Object Object]))
    (.getMethod clojure.lang.APersistentMap "valAt" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "entryAt" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "values" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "keys" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "keys" (into-array Class [clojure.lang.PersistentTreeMap$NodeIterator]))
    (.getMethod clojure.lang.PersistentTreeMap "vals" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "vals" (into-array Class [clojure.lang.PersistentTreeMap$NodeIterator]))

    (.getMethod clojure.lang.PersistentTreeMap "assoc" (into-array Class [Object Object]))
    (.getMethod clojure.lang.PersistentTreeMap "assocEx" (into-array Class [Object Object]))
    (.getMethod clojure.lang.PersistentTreeMap "containsKey" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentTreeMap "create" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentTreeMap "create" (into-array Class [java.util.Comparator clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentTreeMap "doCompare" (into-array Class [Object Object]))
    (.getMethod clojure.lang.PersistentTreeMap "entryAt" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentTreeMap "entryKey" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentTreeMap "iterator" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "kvreduce" (into-array Class [clojure.lang.IFn Object]))
    (.getMethod clojure.lang.PersistentTreeMap "meta" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "reverseIterator" (into-array Class []))
    (.getMethod clojure.lang.PersistentTreeMap "seqFrom" (into-array Class [Object Boolean/TYPE]))
    (.getMethod clojure.lang.PersistentTreeMap "without" (into-array Class [Object]))

    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) clojure.lang.Associative) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) clojure.lang.IPersistentMap) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assocEx") (= (.getReturnType m) clojure.lang.IPersistentMap) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "entryAt") (= (.getReturnType m) clojure.lang.IMapEntry) (= (seq (.getParameterTypes m)) [Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "iterator") (= (.getReturnType m) java.util.Iterator))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object) (= (seq (.getParameterTypes m)) [Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "without") (= (.getReturnType m) clojure.lang.IPersistentMap) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "without") (= (.getReturnType m) clojure.lang.IPersistentMap) (= (seq (.getParameterTypes m)) [Object]))) (.getMethods clojure.lang.PersistentTreeMap)))
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Map java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentTreeMap excluded-methods))
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
