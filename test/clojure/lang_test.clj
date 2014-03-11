(ns clojure.lang-test
  (:require [clojure.test :refer :all]
            [clojure.lang :refer :all]))

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
      (= b c))))


(defn test-foo [n foo] (let [r (into [] (range n))] (= (vmap foo r) (map foo r))))

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
