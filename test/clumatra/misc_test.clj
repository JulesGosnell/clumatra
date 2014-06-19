(ns clumatra.misc-test
  (:import  [java.lang.reflect Method]
            [clumatra.core ObjectArrayToObjectArrayKernel])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
            [clumatra.util :refer :all]
            [clumatra.core :refer :all]
            [clumatra.test-util :refer :all])
  (:gen-class))

;;------------------------------------------------------------------------------

(definterface BooleanKernel (^void invoke [^booleans in ^booleans out ^int gid]))

(deftest boolean-copy-test
  (testing "copy elements of a boolean[]"
    (let [kernel (reify BooleanKernel
                   (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Boolean/TYPE even?]] Boolean/TYPE)]
      (is (apply = results)))))

(deftest boolean-if-test
  (testing "flip elements of a boolean[]"
    (let [kernel (reify BooleanKernel
                   (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
                     (aset out gid (if (aget in gid) false true))))
          results (test-kernel kernel inc [[Boolean/TYPE even?]] Boolean/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface ByteKernel (^void invoke [^bytes in ^bytes out ^int gid]))

(deftest byte-copy-test
  (testing "copy elements of a byte[]"
    (let [kernel (reify ByteKernel
                   (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Byte/TYPE identity]] Byte/TYPE)]
      (is (apply = results)))))

(deftest byte-inc-test
  (testing "increment elements of a byte[] via application of a java static method"
    (let [kernel (reify ByteKernel
                   (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
                     (aset out gid (byte (inc (aget in gid))))))
          results (test-kernel kernel inc [[Byte/TYPE identity]] Byte/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface CharKernel (^void invoke [^chars in ^chars out ^int gid]))

(deftest char-copy-test
  (testing "copy elements of a char[]"
    (let [kernel (reify CharKernel
                   (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Character/TYPE (fn [n] (+ 65 (mod n 26)))]] Character/TYPE)]
      (is (apply = results)))))

(deftest char-toLowercase-test
  (testing "downcase elements of an char[] via application of a java static method"
    (let [kernel (reify CharKernel
                   (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
                     (aset out gid (java.lang.Character/toLowerCase (aget in gid)))))
          results (test-kernel kernel inc [[Character/TYPE (fn [n] (+ 65 (mod n 26)))]] Character/TYPE)]
      (is (apply = results)))))

;; ;;------------------------------------------------------------------------------

(definterface ShortKernel (^void invoke [^shorts in ^shorts out ^int gid]))

(deftest short-copy-test
  (testing "copy elements of a short[]"
    (let [kernel (reify ShortKernel
                   (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Short/TYPE identity]] Short/TYPE)]
      (is (apply = results)))))

(deftest short-inc-test
  (testing "increment elements of a short[] via application of a java static method"
    (let [kernel (reify ShortKernel
                   (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
                     (aset out gid (short (inc (aget in gid))))))
          results (test-kernel kernel inc [[Short/TYPE identity]] Short/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface IntKernel (^void invoke [^ints in ^ints out ^int gid]))

(deftest int-copy-test
  (testing "copy elements of an int[]"
    (let [kernel (reify IntKernel
                   (^void invoke [^IntKernel self ^ints in ^ints out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Integer/TYPE identity]] Integer/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface LongKernel (^void invoke [^longs in ^longs out ^int gid]))

(deftest long-copy-test
  (testing "copy elements of a long[]"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(deftest long-inc-test
  (testing "increment elements of a long[] via the application of a builtin function"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (inc (aget in gid)))))
          results (test-kernel kernel inc [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(defn ^long my-inc [^long l] (inc l))

(deftest long-my-inc-test
  (testing "increment elements of a long[] via the application of a named clojure function"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (long (my-inc (aget in gid))))))
          results (test-kernel kernel inc [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(defn ^:static ^long my-static-inc [^long l] (inc l)) ;I don't think this is static..

(deftest long-my-static-inc-test
  (testing "increment elements of a long[] via the application of a named static clojure function"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (long (my-static-inc (aget in gid))))))
          results (test-kernel kernel inc [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(deftest long-anonymous-inc-test
  (testing "increment elements of a long[] via the application of an anonymous clojure function"
    (let [my-inc (fn [^long l] (inc l))
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (long (my-inc (aget in gid))))))
          results (test-kernel kernel inc [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface FloatKernel (^void invoke [^floats in ^floats out ^int gid]))

(deftest float-copy-test
  (testing "copy elements of a float[]"
    (let [kernel (reify FloatKernel
                   (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Float/TYPE identity]] Float/TYPE)]
      (is (apply = results)))))

(deftest float-inc-test
  (testing "increment elements of a float[] via application of a java static method"
    (let [kernel (reify FloatKernel
                   (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
                     (aset out gid (float (inc (aget in gid))))))
          results (test-kernel kernel inc [[Float/TYPE identity]] Float/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface DoubleKernel (^void invoke [^doubles in ^doubles out ^int gid]))

(deftest double-copy-test
  (testing "copy elements of a double[]"
    (let [kernel (reify DoubleKernel
                   (^void invoke [^CharKernel self ^doubles in ^doubles out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel inc [[Double/TYPE identity]] Double/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(deftest object-copy-test
  (testing "copy elements of an object[]"
    (let [kernel (reify ObjectArrayToObjectArrayKernel
                   (^void invoke [^ObjectArrayToObjectArrayKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
                     (aset out i (aget in i))))
          results (test-kernel kernel inc [[Object identity]] Object)]
      (is (apply = results)))))

;; TODO:
;; try running the same kernel many times in parallel ?

;; parallel object copy test...
;; compare and contrast running many size 32/64 kernels

;; copying a vector into a large input array, running a single kernel,
;; copying results out into another vector...
;; latter would work for Vec32

(deftest multiplication-test
  (testing "square ?Long? elements of an Object[]"
    (let [kernel (reify ObjectArrayToObjectArrayKernel
                   (^void invoke [^ObjectArrayToObjectArrayKernel self ^objects in ^objects out ^int gid]
                     (aset out gid (* (aget in gid) (aget in gid)))))
          results (test-kernel kernel inc [[Object identity]] Object)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface StringIntKernel (^void invoke [^"[Ljava.lang.String;" in ^ints out ^int gid]))

(deftest string-length-test
  (testing "find lengths of an array of Strings via application of a java virtual method"
    (let [kernel (reify StringIntKernel
                   (^void invoke [^StringIntKernel self ^"[Ljava.lang.String;" in ^ints out ^int gid]
                     (aset out gid (.length ^String (aget in gid)))))
          results (test-kernel kernel inc [[String (fn [^Long i] (.toString i))]] Integer/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.misc-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
