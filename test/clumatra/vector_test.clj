(ns clumatra.vector-test
  (:import
   [java.util Collection Map]
   [clojure.lang
    IObj AFn ISeq IPersistentVector APersistentVector PersistentVector PersistentVector$Node
    Associative PersistentVector$TransientVector ITransientCollection Numbers])
  (:require
   [clojure [pprint :as p]]
   [clojure.core
    [reducers :as r]
    [rrb-vector :as v]])
  (:use
   [clojure test]
   [clumatra util core vector test-util]))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method clojure.lang.AFn "run" [])
    (fetch-method java.lang.Object "wait" [Long/TYPE Integer/TYPE])
    (fetch-method java.lang.Object "wait" [Long/TYPE])
    (fetch-method java.lang.Object "wait" [])
    (fetch-method java.lang.Object "notify" [])
    (fetch-method java.lang.Object "notifyAll" [])
    (fetch-method java.lang.Iterable "forEach" [java.util.function.Consumer])
    (fetch-method java.util.List "replaceAll" [java.util.function.UnaryOperator])
    (fetch-method java.util.List "sort" [java.util.Comparator])
    ;; vector - unsupported 
    (fetch-method APersistentVector "removeAll" [java.util.Collection])
    (fetch-method APersistentVector "retainAll" [java.util.Collection])
    (fetch-method APersistentVector "clear" [])
    (fetch-method APersistentVector "addAll" [java.util.Collection])
    (fetch-method APersistentVector "remove" [Integer/TYPE])
    (fetch-method APersistentVector "set" [Integer/TYPE Object])
    (fetch-method APersistentVector "add" [Integer/TYPE Object])
    (fetch-method APersistentVector "addAll" [Integer/TYPE java.util.Collection])
    (fetch-method APersistentVector "add" [Object])
    (fetch-method APersistentVector "remove" [Object])

    ;; how can I avoid this
    (fetch-method PersistentVector "cons" [Object]) ;crashes simulator
    (fetch-method PersistentVector "assocN" IPersistentVector [Integer/TYPE Object])
    (fetch-method APersistentVector "assoc" Associative [Object Object])
    (fetch-method PersistentVector "withMeta" PersistentVector [clojure.lang.IPersistentMap])
    (fetch-method PersistentVector "withMeta" IObj [clojure.lang.IPersistentMap])
    (fetch-method PersistentVector "asTransient" ITransientCollection [])
    (fetch-method PersistentVector "asTransient"  [])
    
    ;; looks like iterators do not compare well
    (fetch-method PersistentVector "iterator" [])
    (fetch-method APersistentVector "listIterator" [Integer/TYPE])
    (fetch-method APersistentVector "listIterator" [])

    ;;; NYI
    (fetch-method PersistentVector "kvreduce" [clojure.lang.IFn Object])
    (fetch-method PersistentVector "nth" [Integer/TYPE Object])
    (fetch-method APersistentVector "invoke" [Object])
    (fetch-method PersistentVector "create" [(type->array-type Object)])
    (fetch-method PersistentVector "create" [clojure.lang.ISeq])
    (fetch-method PersistentVector "assocN" [Integer/TYPE Object])
    (fetch-method APersistentVector "toArray" [(type->array-type Object)])
    (fetch-method APersistentVector "valAt" [Object])
    (fetch-method PersistentVector "arrayFor" [Integer/TYPE])
    (fetch-method PersistentVector "nth" [Integer/TYPE])
    })

(def input-fns
   {
    (fetch-method APersistentVector "assoc" [Object Object]) [identity (fn [_] 0) (fn [_] nil)]
    (fetch-method APersistentVector "compareTo" [Object]) []
    (fetch-method APersistentVector "contains" [Object]) [identity (fn [[h]] h)]
    (fetch-method APersistentVector "containsAll" [java.util.Collection]) []
    (fetch-method APersistentVector "containsKey" [Object]) [identity (fn [i] 0)]
    (fetch-method APersistentVector "entryAt" [Object]) [identity (fn [_] 0)]
    (fetch-method APersistentVector "get" [Integer/TYPE]) [identity (fn [_] 0)]
    (fetch-method APersistentVector "subList" [Integer/TYPE Integer/TYPE]) [identity (fn [_] 0) (fn [_] 1)]
    (fetch-method PersistentVector "meta" []) [(fn [v] (with-meta v {:arg "meta"}))]
    }
  )

(deftest-kernels
  (filter (fn [^java.lang.reflect.Method m] (not (contains? #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.List java.util.Collection} (.getDeclaringClass m))))
          (extract-methods non-static? PersistentVector excluded-methods))
  (fn [i] (mapv (fn [j] (* i j)) (range 1 (inc *wavefront-size*))))
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

