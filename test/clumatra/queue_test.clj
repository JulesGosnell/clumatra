(ns clumatra.queue-test
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (.getMethod clojure.lang.PersistentQueue "clear" (into-array Class []))

    (.getMethod clojure.lang.PersistentQueue "retainAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.PersistentQueue "remove" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentQueue "removeAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.PersistentQueue "add" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentQueue "addAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.PersistentQueue "toArray" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentQueue "iterator" (into-array Class []))

    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods clojure.lang.PersistentQueue)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.Obj))) (.getMethods clojure.lang.PersistentQueue)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.PersistentQueue))) (.getMethods clojure.lang.PersistentQueue)))
    })

(def input-fns
  {}
  )

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn clojure.lang.ASeq clojure.lang.Obj java.lang.Iterable java.util.List java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentQueue excluded-methods))
  (fn [i] (into clojure.lang.PersistentQueue/EMPTY (map (fn [j] (* i j)) (range *wavefront-size*))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.queue-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
