(ns clumatra.list-test
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (.getMethod clojure.lang.PersistentList "reduce" (into-array Class [clojure.lang.IFn Object]))
    (.getMethod clojure.lang.PersistentList "reduce" (into-array Class [clojure.lang.IFn]))
    (.getMethod clojure.lang.PersistentList "withMeta" (into-array Class [clojure.lang.IPersistentMap]))

    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods clojure.lang.PersistentList)))
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.Obj))) (.getMethods clojure.lang.PersistentList)))
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
   (extract-methods non-static? clojure.lang.PersistentList excluded-methods))
  (fn [i] (into '() (map (fn [j] (* i j)) (range *wavefront-size*))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.list-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
