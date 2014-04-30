(ns clumatra.vector-test
  (:import  [java.util Collection Map]
            [clojure.lang AFn ISeq APersistentVector PersistentVector Associative])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

;;------------------------------------------------------------------------------

;; (deftest test-map-array
;;   (testing "map an array to an array using an unrolled loop"
;;     (is (= (map inc (range 32)) (seq (map-array 32 (into-array Object (range 32)) inc))))))

;;------------------------------------------------------------------------------

(def foo identity)                      ; okra will not yet let us call a fn

(deftest a-test
  (testing "map vector-to-vector"
    (let [a (into [] (range 100))
          b (time (mapv foo a))
          c (time (vmap foo a))]
      (is (= b c)))))

(defn test-foo [n foo] (let [r (into [] (range n))] (= (vmap foo r) (map foo r))))
(defn test-foo2 [branch leaf n foo] (let [r (into [] (range n))] (= (vmap branch leaf foo r) (map foo r))))

(deftest another-test
  (testing "map vector-to-vector"
    (is (test-foo 0 foo))
    (is (test-foo 32 foo))
    (is (test-foo 33 foo))
    (is (test-foo (+ 32 (* 32 32)) foo))
    (is (test-foo (+ 33 (* 32 32)) foo))
    (is (test-foo (+ 33 (* 32 32 32)) foo))
    (is (test-foo (+ 33 (* 32 32 32 32)) foo))
    ))

;; (deftest exercise-backends
;;   (testing "backends"
;;     (is (test-foo2 :sequential        :sequential 33 identity))
;;     (is (test-foo2 :threads-parallel  :sequential 33 identity))
;;     ))

;; TODO:
;; look at core reducer
;; reuse fork-join for two backends
;; consider reworking what we have to integrate better
;; write some tests that make use of Map splicing
;; consider moving reduce/combine to seqspert, with an optional clumatra binding

;; (def a (into [] (range 10000000)))
;; (time (do (into [] a) nil)) ;; 124
;; (time (do (mapv identity a) nil)) ;; 220
;; (time (do (vmap identity a) nil)) ;; 140
;; (time (do (mapv inc a) nil)) ;; 280
;; (time (do (vmap inc a) nil)) ;; 240

;; ;; vmap should win when used in parallel mode because of the zero cost
;; ;; of concatenation. - but current reduction code not suitable

;; (def n (/ (count a) (.availableProcessors (Runtime/getRuntime)))) ;; = 625000

;; (do (time (r/fold (r/monoid into vector) conj a)) nil) ;; 380 ms
;; (do (time (r/fold (r/monoid v/catvec v/vector) conj a)) nil) ;; 380 ms

;; (do (time (r/fold (r/monoid into vector) conj (r/map inc a))) nil) ;; 620 ms
;; (do (time (r/fold (r/monoid v/catvec v/vector) conj (r/map inc a))) nil) ;; 680 ms

;; (do (time (r/fold n (r/monoid into vector) conj (r/map inc a))) nil) ;; 590 ms
;; (do (time (r/fold n (r/monoid v/catvec v/vector) conj (r/map inc a))) nil) ;; 320 - 520 - erratic !

;; (time (count (vmap inc a))) ;; 230 ms
;; (time (count (fjvmap inc a)))
;; (= (time (r/fold n (r/monoid v/catvec v/vector) conj (r/map inc a))) (fjvmap inc a))


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
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) Associative))) (.getMethods APersistentVector))) 
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assocN") (= (.getReturnType m) clojure.lang.IPersistentVector))) (.getMethods PersistentVector))) 
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods PersistentVector))) 
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "asTransient") (= (.getReturnType m) clojure.lang.ITransientCollection))) (.getMethods PersistentVector))) 
    ;; looks like iterators do not compare well
    (fetch-method PersistentVector "iterator" [])
    (fetch-method APersistentVector "listIterator" [Integer/TYPE])
    (fetch-method APersistentVector "listIterator" [])
    (fetch-method PersistentVector "asTransient" [])

    ;;; NYI
    (fetch-method PersistentVector "kvreduce" [clojure.lang.IFn Object])
    (fetch-method PersistentVector "withMeta" [clojure.lang.IPersistentMap])
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
