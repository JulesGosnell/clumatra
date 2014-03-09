(ns clumatra.core-test
  (:require [clojure.test :refer :all]
            [clojure.core [reducers :as r]]
            [clumatra.core :refer :all]))

(set! *warn-on-reflection* true)

(deftest a-test
  (testing "copy object array"
    (let [n 32
          kernel-fn (kernel-compile identity 32)
          in (into-array Object (range n))]
      (let [out (make-array Object n)]
        (is (not (= (seq in) (seq out))))
        (kernel-fn in out)
        (is (= (seq in) (seq out))))  
      (let [out (make-array Object n)]
        (is (not (= (seq in) (seq out))))
        (kernel-fn in out)
        (is (= (seq in) (seq out))))
      )))
