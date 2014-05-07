(ns clumatra.hash-set-test
  (:import  [java.lang.reflect Method]
            [java.util Collection List Set]
            [clojure.lang IObj AFn ISeq IPersistentMap APersistentSet PersistentHashSet])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.util :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    ;;; NYI
    (fetch-method APersistentSet "add"      [Object])
    (fetch-method APersistentSet "get"      [Object])
    (fetch-method APersistentSet "invoke"   [Object])
    (fetch-method APersistentSet "iterator" [])
    (fetch-method APersistentSet "remove"   [Object])
    (fetch-method APersistentSet "toArray"  [(type->array-type Object)])

    (fetch-method PersistentHashSet "addAll"          [Collection])
    (fetch-method PersistentHashSet "asTransient"     [])
    (fetch-method PersistentHashSet "clear"           [])
    (fetch-method PersistentHashSet "create"          [(type->array-type Object)])
    (fetch-method PersistentHashSet "create"          [ISeq])
    (fetch-method PersistentHashSet "create"          [List])
    (fetch-method PersistentHashSet "createWithCheck" [(type->array-type Object)])
    (fetch-method PersistentHashSet "createWithCheck" [ISeq])
    (fetch-method PersistentHashSet "createWithCheck" [List])
    (fetch-method PersistentHashSet "meta"            [])
    (fetch-method PersistentHashSet "removeAll"       [Collection])
    (fetch-method PersistentHashSet "retainAll"       [Collection])

    (fetch-method PersistentHashSet "withMeta" PersistentHashSet [IPersistentMap])
    (fetch-method PersistentHashSet "withMeta" IObj              [IPersistentMap])
    })

(def input-fns {})

(deftest-kernels
  (filter
   (fn [^Method m]
     (not
      (contains?
       #{Object AFn Iterable Set Collection}
       (.getDeclaringClass m))))
   (extract-methods non-static? PersistentHashSet excluded-methods))
  (fn [i] (into #{} (map (fn [j] (* i j)) (range 1 (inc *wavefront-size*)))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.hash-set-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
