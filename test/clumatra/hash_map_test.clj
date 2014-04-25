(ns clumatra.hash-map-test
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
    (.getMethod clojure.lang.APersistentMap "put" (into-array Class [Object Object]))
    (.getMethod clojure.lang.APersistentMap "putAll" (into-array Class [java.util.Map]))

    (.getMethod clojure.lang.APersistentMap "count" (into-array Class []))
    (.getMethod clojure.lang.APersistentMap "empty" (into-array Class []))
    (.getMethod clojure.lang.APersistentMap "get" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "invoke" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "remove" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "seq" (into-array Class []))
    (.getMethod clojure.lang.APersistentMap "valAt" (into-array Class [Object Object]))
    (.getMethod clojure.lang.APersistentMap "valAt" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentMap "values" (into-array Class []))

    (.getMethod clojure.lang.PersistentHashMap "asTransient" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashMap "assoc" (into-array Class [Object Object]))
    (.getMethod clojure.lang.PersistentHashMap "assocEx" (into-array Class [Object Object]))
    (.getMethod clojure.lang.PersistentHashMap "containsKey" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentHashMap "count" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashMap "create" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentHashMap "create" (into-array Class [clojure.lang.IPersistentMap (type->array-type Object)]))
    (.getMethod clojure.lang.PersistentHashMap "create" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentHashMap "createWithCheck" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentHashMap "createWithCheck" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentHashMap "empty" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashMap "entryAt" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentHashMap "fold" (into-array Class [Long/TYPE clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn]))
    (.getMethod clojure.lang.PersistentHashMap "iterator" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashMap "kvreduce" (into-array Class [clojure.lang.IFn Object]))
    (.getMethod clojure.lang.PersistentHashMap "meta" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashMap "seq" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashMap "withMeta" (into-array Class [clojure.lang.IPersistentMap]))
    (.getMethod clojure.lang.PersistentHashMap "without" (into-array Class [Object]))

    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "asTransient") (= (.getReturnType m) clojure.lang.ITransientCollection))) (.getMethods clojure.lang.PersistentHashMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) clojure.lang.Associative))) (.getMethods clojure.lang.PersistentHashMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentHashMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object))) (.getMethods clojure.lang.PersistentHashMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods clojure.lang.PersistentHashMap)))
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Map java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentHashMap excluded-methods))
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
