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
;;; wierd - a different one from the one in PersistentVector - I cannot find an impl
    (first (filter (fn [^java.lang.reflect.Method m] (and (= (.getName m) "assoc") (= (.getReturnType m) clojure.lang.Associative))) (.getMethods clojure.lang.APersistentVector))) 
    ;; looks like iterators do not compare well
    (.getMethod clojure.lang.PersistentVector "iterator" (into-array Class []))
    (.getMethod clojure.lang.APersistentVector "listIterator" (into-array Class [Integer/TYPE]))
    (.getMethod clojure.lang.APersistentVector "listIterator" (into-array Class []))
;;;(.getMethod clojure.lang.APersistentVector "rangedIterator" (into-array Class [Integer/TYPE Integer/TYPE]))
    (.getMethod clojure.lang.PersistentVector "asTransient" (into-array Class []))
    })

(def input-fns
  {
   (.getMethod clojure.lang.APersistentVector "assoc" (into-array Class [Object Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "compareTo" (into-array Class [Object])) [(fn [i] [i])(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "contains" (into-array Class [Object])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "containsAll" (into-array Class [java.util.Collection])) [(fn [i] [i])(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "containsKey" (into-array Class [Object])) [(fn [i] (vec (range (inc i))))]
;;;   (.getMethod clojure.lang.APersistentVector "doEquals" (into-array Class [clojure.lang.IPersistentVector Object])) [(fn [i] [i])]
;;;   (.getMethod clojure.lang.APersistentVector "doEquiv" (into-array Class [clojure.lang.IPersistentVector Object])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "entryAt" (into-array Class [Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "equals" (into-array Class [Object])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "equiv" (into-array Class [Object])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "get" (into-array Class [Integer/TYPE])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "hashCode" (into-array Class [])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "hasheq" (into-array Class [])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "indexOf" (into-array Class [Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "invoke" (into-array Class [Object])) [(fn [i] (vec (range (inc i)))) int]
   (.getMethod clojure.lang.APersistentVector "isEmpty" (into-array Class [])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "lastIndexOf" (into-array Class [Object])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "length" (into-array Class [])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "peek" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "rseq" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "size" (into-array Class [])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "subList" (into-array Class [Integer/TYPE Integer/TYPE])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "toArray" (into-array Class [(type->array-type Object)])) [(fn [i] (vec (range (inc i)))) (fn [i](make-array Object (inc i)))]
   (.getMethod clojure.lang.APersistentVector "toArray" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "toString" (into-array Class [])) [(fn [i] [i])]
   (.getMethod clojure.lang.APersistentVector "valAt" (into-array Class [Object Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.APersistentVector "valAt" (into-array Class [Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "nth" (into-array Class [Integer/TYPE Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "seq" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "pop" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "count" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "meta" (into-array Class [])) [(fn [i] (with-meta (vec (range i)) {:arg i}))]
   (.getMethod clojure.lang.PersistentVector "withMeta" (into-array Class [clojure.lang.IPersistentMap])) [(fn [i] (vec (range i))) (fn  [i] {:arg i})]
   (.getMethod clojure.lang.PersistentVector "cons" (into-array Class [Object])) [(fn [i] (vec (range (inc i))))]
;;   (.getMethod clojure.lang.PersistentVector "nth" (into-array Class [Integer/TYPE])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "arrayFor" (into-array Class [Integer/TYPE])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "chunkedSeq" (into-array Class [])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "assocN" (into-array Class [Integer/TYPE Object])) [(fn [i] (vec (range (inc i))))]
   (.getMethod clojure.lang.PersistentVector "kvreduce" (into-array Class [clojure.lang.IFn Object])) [(fn [i] (vec (range (inc i)))) (fn [r [k v]] (+ r v))]
   (.getMethod clojure.lang.PersistentVector "empty" (into-array Class [])) [(fn [i] (vec (range (inc i))))]

   })

(defn non-static? [m] (fn [m] (not (static? m))))

;;(deftest-kernels (extract-methods non-static? clojure.lang.PersistentVector excluded-methods) input-fns) 

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.vector-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
