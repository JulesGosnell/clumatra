(ns clumatra.hash-set-test
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    ;;;(.getMethod clojure.lang.AFn "run" (into-array Class []))
    ;;; NYI
    ;; (.getMethod clojure.lang.PersistentHashSet "kvreduce" (into-array Class [clojure.lang.IFn Object]))
    (.getMethod clojure.lang.PersistentHashSet "withMeta" (into-array Class [clojure.lang.IPersistentMap]))
    (.getMethod clojure.lang.PersistentHashSet "meta" (into-array Class []))
    ;; (.getMethod clojure.lang.PersistentHashSet "nth" (into-array Class [Integer/TYPE Object]))
    (.getMethod clojure.lang.APersistentSet "invoke" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentHashSet "create" (into-array Class [java.util.List]))
    (.getMethod clojure.lang.PersistentHashSet "create" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentHashSet "create" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentHashSet "createWithCheck" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentHashSet "createWithCheck" (into-array Class [java.util.List]))
    (.getMethod clojure.lang.PersistentHashSet "createWithCheck" (into-array Class [(type->array-type Object)]))
    ;; (.getMethod clojure.lang.PersistentHashSet "assocN" (into-array Class [Integer/TYPE Object]))
    (.getMethod clojure.lang.APersistentSet "toArray" (into-array Class [(type->array-type Object)]))
    ;; (.getMethod clojure.lang.APersistentSet "valAt" (into-array Class [Object]))
    ;; (.getMethod clojure.lang.PersistentHashSet "arrayFor" (into-array Class [Integer/TYPE]))
    ;; (.getMethod clojure.lang.PersistentHashSet "nth" (into-array Class [Integer/TYPE]))
    (.getMethod clojure.lang.PersistentHashSet "clear" (into-array Class []))
    (.getMethod clojure.lang.PersistentHashSet "removeAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.PersistentHashSet "retainAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.PersistentHashSet "addAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.PersistentHashSet "asTransient" (into-array Class []))
    (.getMethod clojure.lang.APersistentSet "iterator" (into-array Class []))
    (.getMethod clojure.lang.APersistentSet "remove" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentSet "add" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentSet "get" (into-array Class [Object]))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods clojure.lang.PersistentHashSet)))
    })

(def input-fns
   {
;;    (.getMethod clojure.lang.APersistentSet "assoc" (into-array Class [Object Object])) [identity (fn [_] 0) (fn [_] nil)]
    }
  )

(deftest-kernels
  (filter
   (fn [^java.lang.reflect.Method m]
     (not
      (contains?
       #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.Set java.util.Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? clojure.lang.PersistentHashSet excluded-methods))
  (fn [i] (into #{} (map (fn [j] (* i j)) (range 1 (inc *wavefront-size*)))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.vector-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
