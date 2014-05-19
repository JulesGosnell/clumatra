(ns clumatra.numbers-test
  (:import  [java.math BigInteger]
            [java.lang.reflect Method]
            [clojure.lang Numbers BigInt])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
            [clumatra.util :refer :all]
            [clumatra.test-util :refer :all])
  (:gen-class))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    ;; these seem to crash simulated build
    (fetch-method Numbers "max" [Double/TYPE Long/TYPE])
    (fetch-method Numbers "max" [Long/TYPE Double/TYPE])
    (fetch-method Numbers "min" [Double/TYPE Long/TYPE])
    (fetch-method Numbers "min" [Long/TYPE Double/TYPE])
    (fetch-method Numbers "num" [Double/TYPE])
    (fetch-method Numbers "num" [Float/TYPE])

    (fetch-method Numbers "divide" [Long/TYPE Long/TYPE])

    })

(def input-fns
  {
   ;; arrays
   (fetch-method Numbers "boolean_array" [Integer/TYPE Object])[identity even?]
   (fetch-method Numbers "byte_array"    [Integer/TYPE Object])[identity byte]
   (fetch-method Numbers "char_array"    [Integer/TYPE Object])[identity (fn [i] (char (+ i 65)))]
   (fetch-method Numbers "char_array"    [Object])[(fn [i] (list (char (+ i 65))))]
   (fetch-method Numbers "double_array"  [Object])[(fn [i] (list (double i)))]
   (fetch-method Numbers "float_array"   [Object])[(fn [i] (list (float i)))]
   (fetch-method Numbers "short_array"   [Integer/TYPE Object])[identity short]

   ;; cast array
   (fetch-method Numbers "booleans" [Object]) [(fn [i] (boolean-array (map even? (range i))))]
   (fetch-method Numbers "bytes"    [Object]) [(fn [i] (byte-array (map byte (range i))))]
   (fetch-method Numbers "chars"    [Object]) [(fn [i] (char-array (map (fn [i2] (char (+ 65 i2))) (range i))))]
   (fetch-method Numbers "doubles"  [Object]) [(fn [i] (double-array (map double (range i))))]
   (fetch-method Numbers "floats"   [Object]) [(fn [i] (float-array (map float (range i))))]
   (fetch-method Numbers "ints"     [Object]) [(fn [i] (int-array (map int (range i))))]
   (fetch-method Numbers "longs"    [Object]) [(fn [i] (long-array (range i)))]
   (fetch-method Numbers "shorts"   [Object]) [(fn [i] (short-array (map short (range i))))]

   ;; misc
   (fetch-method Numbers "divide"       [BigInteger BigInteger]) [(fn [i](BigInteger. (str i)))(fn [i](BigInteger. (str i)))]
   (fetch-method Numbers "reduceBigInt" [BigInt]) [bigint]

   })

(deftest-kernels (extract-methods static? Numbers excluded-methods) inc input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.numbers-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
