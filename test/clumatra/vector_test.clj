(ns clumatra.vector-test
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
    (.getMethod clojure.lang.AFn "run" (into-array Class []))
    (.getMethod java.lang.Object "wait" (into-array Class [Long/TYPE Integer/TYPE]))
    (.getMethod java.lang.Object "wait" (into-array Class [Long/TYPE]))
    (.getMethod java.lang.Object "wait" (into-array Class []))
    (.getMethod java.lang.Object "notify" (into-array Class []))
    (.getMethod java.lang.Object "notifyAll" (into-array Class []))
    (.getMethod java.lang.Iterable "forEach" (into-array Class [java.util.function.Consumer]))
    (.getMethod java.util.List "replaceAll" (into-array Class [java.util.function.UnaryOperator]))
    (.getMethod java.util.List "sort" (into-array Class [java.util.Comparator]))
    ;; vector - unsupported 
    (.getMethod clojure.lang.APersistentVector "removeAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.APersistentVector "retainAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.APersistentVector "clear" (into-array Class []))
    (.getMethod clojure.lang.APersistentVector "addAll" (into-array Class [java.util.Collection]))
    (.getMethod clojure.lang.APersistentVector "remove" (into-array Class [Integer/TYPE]))
    (.getMethod clojure.lang.APersistentVector "set" (into-array Class [Integer/TYPE Object]))
    (.getMethod clojure.lang.APersistentVector "add" (into-array Class [Integer/TYPE Object]))
    (.getMethod clojure.lang.APersistentVector "addAll" (into-array Class [Integer/TYPE java.util.Collection]))
    (.getMethod clojure.lang.APersistentVector "add" (into-array Class [Object]))
    (.getMethod clojure.lang.APersistentVector "remove" (into-array Class [Object]))
    ;; how can I avoid this
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) clojure.lang.Associative))) (.getMethods clojure.lang.APersistentVector))) 
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assocN") (= (.getReturnType m) clojure.lang.IPersistentVector))) (.getMethods clojure.lang.PersistentVector))) 
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "withMeta") (= (.getReturnType m) clojure.lang.IObj))) (.getMethods clojure.lang.PersistentVector))) 
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "asTransient") (= (.getReturnType m) clojure.lang.ITransientCollection))) (.getMethods clojure.lang.PersistentVector))) 
    ;; looks like iterators do not compare well
    (.getMethod clojure.lang.PersistentVector "iterator" (into-array Class []))
    (.getMethod clojure.lang.APersistentVector "listIterator" (into-array Class [Integer/TYPE]))
    (.getMethod clojure.lang.APersistentVector "listIterator" (into-array Class []))
    (.getMethod clojure.lang.PersistentVector "asTransient" (into-array Class []))

    ;;; NYI
    (.getMethod clojure.lang.PersistentVector "kvreduce" (into-array Class [clojure.lang.IFn Object]))
    (.getMethod clojure.lang.PersistentVector "withMeta" (into-array Class [clojure.lang.IPersistentMap]))
    (.getMethod clojure.lang.PersistentVector "nth" (into-array Class [Integer/TYPE Object]))
    (.getMethod clojure.lang.APersistentVector "invoke" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentVector "create" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.PersistentVector "create" (into-array Class [clojure.lang.ISeq]))
    (.getMethod clojure.lang.PersistentVector "assocN" (into-array Class [Integer/TYPE Object]))
    (.getMethod clojure.lang.APersistentVector "toArray" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.APersistentVector "valAt" (into-array Class [Object]))
    (.getMethod clojure.lang.PersistentVector "arrayFor" (into-array Class [Integer/TYPE]))
    (.getMethod clojure.lang.PersistentVector "nth" (into-array Class [Integer/TYPE]))
    })

(def input-fns
   {
    (.getMethod clojure.lang.APersistentVector "assoc" (into-array Class [Object Object])) [identity (fn [_] 0) (fn [_] nil)]
    (.getMethod clojure.lang.APersistentVector "compareTo" (into-array Class [Object])) []
    (.getMethod clojure.lang.APersistentVector "contains" (into-array Class [Object])) [identity (fn [[h]] h)]
    (.getMethod clojure.lang.APersistentVector "containsAll" (into-array Class [java.util.Collection])) []
    (.getMethod clojure.lang.APersistentVector "containsKey" (into-array Class [Object])) [identity (fn [i] 0)]
    (.getMethod clojure.lang.APersistentVector "entryAt" (into-array Class [Object])) [identity (fn [_] 0)]
    (.getMethod clojure.lang.APersistentVector "get" (into-array Class [Integer/TYPE])) [identity (fn [_] 0)]
    (.getMethod clojure.lang.APersistentVector "subList" (into-array Class [Integer/TYPE Integer/TYPE])) [identity (fn [_] 0) (fn [_] 1)]
    (.getMethod clojure.lang.PersistentVector "meta" (into-array Class [])) [(fn [v] (with-meta v {:arg "meta"}))]
    }
  )

(deftest-kernels
  (filter (fn [^java.lang.reflect.Method m] (not (contains? #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.List java.util.Collection} (.getDeclaringClass m))))
          (extract-methods non-static? clojure.lang.PersistentVector excluded-methods))
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
