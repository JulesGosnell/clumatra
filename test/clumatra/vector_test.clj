(ns clumatra.vector-test
  (:require [clojure.test :refer :all]
            [clumatra.vector :refer :all]))

(set! *warn-on-reflection* true)

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

(deftest exercise-backends
  (testing "backends"
    (is (test-foo2 :sequential        :sequential 33 identity))
    (is (test-foo2 :threads-parallel  :sequential 33 identity))
    ))

;; TODO:
;; look at core reducer
;; reuse fork-join for two backends
;; consider reworking what we have to integrate better
;; write some tests that make use of Map splicing
;; consider moving reduce/combine to seqspert, with an optional clumatra binding

