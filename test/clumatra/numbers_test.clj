(ns clumatra.numbers-test
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
            [clumatra [util :as u]]
            [clumatra.test-util :refer :all])
  (:gen-class))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    ;; these seem to crash simulated build
    (.getMethod clojure.lang.Numbers "max" (into-array Class [Double/TYPE Long/TYPE]))
    (.getMethod clojure.lang.Numbers "max" (into-array Class [Long/TYPE Double/TYPE]))
    (.getMethod clojure.lang.Numbers "min" (into-array Class [Double/TYPE Long/TYPE]))
    (.getMethod clojure.lang.Numbers "min" (into-array Class [Long/TYPE Double/TYPE]))
    (.getMethod clojure.lang.Numbers "num" (into-array Class [Double/TYPE]))
    (.getMethod clojure.lang.Numbers "num" (into-array Class [Float/TYPE]))
    })

(def input-fns
  {
   ;; arrays
   (.getMethod clojure.lang.Numbers "boolean_array" (into-array Class [Integer/TYPE Object]))[identity even?]
   (.getMethod clojure.lang.Numbers "byte_array" (into-array Class [Integer/TYPE Object]))[identity byte]
   (.getMethod clojure.lang.Numbers "char_array" (into-array Class [Integer/TYPE Object]))[identity (fn [i] (char (+ i 65)))]
   (.getMethod clojure.lang.Numbers "char_array" (into-array Class [Object]))[(fn [i] (list (char (+ i 65))))]
   (.getMethod clojure.lang.Numbers "double_array" (into-array Class [Object]))[(fn [i] (list (double i)))]
   (.getMethod clojure.lang.Numbers "float_array" (into-array Class [Object]))[(fn [i] (list (float i)))]
   (.getMethod clojure.lang.Numbers "short_array" (into-array Class [Integer/TYPE Object]))[identity short]

   ;; cast array
   (.getMethod clojure.lang.Numbers "booleans" (into-array Class [Object])) [(fn [i] (boolean-array (map even? (range i))))]
   (.getMethod clojure.lang.Numbers "bytes" (into-array Class [Object])) [(fn [i] (byte-array (map byte (range i))))]
   (.getMethod clojure.lang.Numbers "chars" (into-array Class [Object])) [(fn [i] (char-array (map (fn [i2] (char (+ 65 i2))) (range i))))]
   (.getMethod clojure.lang.Numbers "doubles" (into-array Class [Object])) [(fn [i] (double-array (map double (range i))))]
   (.getMethod clojure.lang.Numbers "floats" (into-array Class [Object])) [(fn [i] (float-array (map float (range i))))]
   (.getMethod clojure.lang.Numbers "ints" (into-array Class [Object])) [(fn [i] (int-array (map int (range i))))]
   (.getMethod clojure.lang.Numbers "longs" (into-array Class [Object])) [(fn [i] (long-array (range i)))]
   (.getMethod clojure.lang.Numbers "shorts" (into-array Class [Object])) [(fn [i] (short-array (map short (range i))))]

   ;; misc
   (.getMethod clojure.lang.Numbers "divide" (into-array Class [java.math.BigInteger,java.math.BigInteger])) [(fn [i](java.math.BigInteger. (str i)))(fn [i](java.math.BigInteger. (str i)))]
   (.getMethod clojure.lang.Numbers "reduceBigInt" (into-array Class [clojure.lang.BigInt])) [bigint]

   })

(deftest-kernels (extract-methods static? clojure.lang.Numbers excluded-methods) inc input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.numbers-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
