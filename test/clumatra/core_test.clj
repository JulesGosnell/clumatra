(ns clumatra.core-test
  (:require [clojure.test :refer :all]
            [clojure.core [reducers :as r]]
            [clumatra.core :refer :all]))

(set! *warn-on-reflection* true)

(deftest a-test
  (testing "copy object array"
    (let [n 32
          kernel-fn (do (println "compiling kernel...")
                        (kernel-compile identity n))
          in (into-array Object (range n))]
      (let [out (make-array Object n)]
        (is (not (= (seq in) (seq out))))
        (do (println "running kernel...")
            (kernel-fn in out)
            (println "kernel run"))
        (is (= (seq in) (seq out))))  
      (let [out (make-array Object n)]
        (is (not (= (seq in) (seq out))))
        (do (println "running kernel...")
            (kernel-fn in out)
            (println "kernel run"))
        (is (= (seq in) (seq out))))
      )))
