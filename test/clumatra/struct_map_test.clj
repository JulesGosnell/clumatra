(ns clumatra.struct-map-test
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
     (.getMethod clojure.lang.APersistentMap "values" (into-array Class []))

     (.getMethod clojure.lang.PersistentStructMap "assocEx" (into-array Class [Object Object]))
     (.getMethod clojure.lang.PersistentStructMap "construct" (into-array Class [clojure.lang.PersistentStructMap$Def clojure.lang.ISeq]))
     (.getMethod clojure.lang.PersistentStructMap "containsKey" (into-array Class [Object]))
     (.getMethod clojure.lang.PersistentStructMap "count" (into-array Class []))
     (.getMethod clojure.lang.PersistentStructMap "create" (into-array Class [clojure.lang.PersistentStructMap$Def clojure.lang.ISeq]))
     (.getMethod clojure.lang.PersistentStructMap "createSlotMap" (into-array Class [clojure.lang.ISeq]))
     (.getMethod clojure.lang.PersistentStructMap "empty" (into-array Class []))
     (.getMethod clojure.lang.PersistentStructMap "entryAt" (into-array Class [Object]))
     (.getMethod clojure.lang.PersistentStructMap "getAccessor" (into-array Class [clojure.lang.PersistentStructMap$Def Object]))
     (.getMethod clojure.lang.PersistentStructMap "iterator" (into-array Class []))
     (.getMethod clojure.lang.PersistentStructMap "meta" (into-array Class []))
     (.getMethod clojure.lang.PersistentStructMap "seq" (into-array Class []))
     (.getMethod clojure.lang.PersistentStructMap "withMeta" (into-array Class [clojure.lang.IPersistentMap]))
     (.getMethod clojure.lang.PersistentStructMap "without" (into-array Class [Object]))

     (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) clojure.lang.Associative))) (.getMethods clojure.lang.PersistentStructMap)))
     (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object) (= (seq (.getParameterTypes m)) [Object Object]))) (.getMethods clojure.lang.PersistentStructMap)))
     (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "valAt") (= (.getReturnType m) Object))) (.getMethods clojure.lang.PersistentStructMap)))
    })

(def input-fns {})

(defstruct s :input)

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Map java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentStructMap excluded-methods))
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
