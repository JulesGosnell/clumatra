(ns clumatra.array-map-test
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

    (.getMethod clojure.lang.PersistentArrayMap "asTransient" (into-array Class []))
    (.getMethod clojure.lang.PersistentArrayMap "createAsIfByAssoc" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentArrayMap "createWithCheck" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentArrayMap "entryAt" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentArrayMap "iterator" (into-array Class []))
    (.getMethod clojure.lang.PersistentArrayMap "kvreduce" (into-array Class [clojure.lang.IFn Object]))
    (.getMethod clojure.lang.PersistentArrayMap "meta" (into-array Class []))

    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "asTransient") (= (.getReturnType m) clojure.lang.ITransientCollection))) (.getMethods clojure.lang.PersistentArrayMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentArrayMap)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object) (= (seq (.getParameterTypes m)) [Object]))) (.getMethods clojure.lang.PersistentArrayMap)))
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Map java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentArrayMap excluded-methods))
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
