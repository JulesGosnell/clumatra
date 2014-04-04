(ns clumatra.core-test
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clumatra [util :as u]])
  (:gen-class))

(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.core-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))

;;------------------------------------------------------------------------------

(defn local-kernel-compile [kernel ^Method method n]
  (fn [in out]
    (doseq [^Long i (range n)]
      (.invoke method kernel (into-array Object [in out (int i)])))
    out))

(println)
(u/compile-if
 (Class/forName "com.amd.okra.OkraContext")
 (do
   ;; looks like okra is available :-)
   (println "*** TESTING WITH OKRA ***")
   (use '(clumatra core))
   (def okra-kernel-compile kernel-compile2)
   )
 (do
   ;; we must be on my i386 laptop :-(
   (println "*** TESTING WITHOUT OKRA ***")
    
   (defn find-method [object ^String name]
     (first (filter (fn [^Method method] (= (.getName method) "invoke")) (.getDeclaredMethods (class object)))))
    
   (def okra-kernel-compile local-kernel-compile)
   ))
(println)


(defn test-kernel [kernel method n & runtime-args]
  (let [compiled (okra-kernel-compile kernel method n) ;compile once
        [in out] runtime-args
        out  (apply compiled runtime-args)]
    (println (seq in) " -> " (seq out))
    (= (seq out)              
       (seq (apply compiled runtime-args)) ;run twice
       (seq (apply (local-kernel-compile kernel method n) runtime-args))))) ;compare against control

;;------------------------------------------------------------------------------

(definterface BooleanKernel (^void invoke [^booleans in ^booleans out ^int gid]))

(deftest boolean-test
  (println "boolean-test")
  (testing "copy elements of a boolean[]"
    (let [n 64
          kernel (reify BooleanKernel
                   (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (boolean-array (map even? (range n))) (boolean-array n))))))

(deftest boolean-flip-test
  (println "boolean-flip-test")
  (testing "flip elements of a boolean[]"
    (let [n 64
          kernel (reify BooleanKernel
                   (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
                     (aset out gid (if (aget in gid) false true))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (boolean-array (map even? (range n))) (boolean-array n))))))

;;------------------------------------------------------------------------------

(definterface ByteKernel (^void invoke [^bytes in ^bytes out ^int gid]))

(deftest byte-test
  (println "byte-test")
  (testing "copy elements of a byte[]"
    (let [n 64
          kernel (reify ByteKernel
                   (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (byte-array (range n)) (byte-array n))))))

;;  SIGSEGV (0xb) at pc=0x00007f7c015d1623, pid=5069, tid=140170587764480

;; (deftest inc-byte-test
;;   (println "inc-byte-test")
;;   (testing "increment elements of a byte[] via application of a java static method"
;;     (let [n 64
;;           kernel (reify ByteKernel
;;                    (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
;;                      (aset out gid (byte (inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (byte-array (range n)) (byte-array n))))))

;; ;;------------------------------------------------------------------------------

;; (definterface CharKernel (^void invoke [^chars in ^chars out ^int gid]))

;; ;; looks like it upsets Jenkins JUnit test result parser

;; ;; (deftest char-test
;; (println "char-test")
;; ;;   (testing "copy elements of a char[]"
;; ;;     (let [n 26
;; ;;           kernel (reify CharKernel
;; ;;                    (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
;; ;;                      (aset out gid (aget in gid))))]
;; ;;       (is (test-kernel
;; ;;            kernel (find-method kernel "invoke") n
;; ;;            (char-array (map char (range 65 (+ 65 n)))) (char-array n))))))

;; ;; wierd - I would expect this one to work - investigate...
;; ;; com.oracle.graal.graph.GraalInternalError: unimplemented

;; ;; (deftest toLowercase-char-test
;; ;; (println "toLowercase-char-test")
;; ;;   (testing "downcase elements of an char[] via application of a java static method"
;; ;;     (let [n 26
;; ;;           kernel (reify CharKernel
;; ;;                    (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
;; ;;                      (aset out gid (java.lang.Character/toLowerCase (aget in gid)))))]
;; ;;       (is (test-kernel
;; ;;            kernel (find-method kernel "invoke") n
;; ;;            (char-array (map char (range 65 (+ 65 n)))) (char-array n))))))

;;------------------------------------------------------------------------------

(definterface ShortKernel (^void invoke [^shorts in ^shorts out ^int gid]))

(deftest short-test
  (println "short-test")
  (testing "copy elements of a short[]"
    (let [n 64
          kernel (reify ShortKernel
                   (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (short-array (range n)) (short-array n))))))

;; seems to have killed jvm

;; (deftest inc-short-test
;;   (println "inc-short-test")
;;   (testing "increment elements of a short[] via application of a java static method"
;;     (let [n 64
;;           kernel (reify ShortKernel
;;                    (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
;;                      (aset out gid (short (inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (short-array (range n)) (short-array n))))))

;;------------------------------------------------------------------------------

(definterface IntKernel (^void invoke [^ints in ^ints out ^int gid]))

(deftest int-test
(println "int-test")
  (testing "copy elements of an int[]"
    (let [n 64
          kernel (reify IntKernel
                   (^void invoke [^IntKernel self ^ints in ^ints out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (int-array (range n)) (int-array n))))))

;;------------------------------------------------------------------------------

(definterface LongKernel (^void invoke [^longs in ^longs out ^int gid]))

(deftest long-test
(println "long-test")
  (testing "copy elements of a long[]"
    (let [n 64
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (long-array (range n)) (long-array n))))))

(deftest unchecked-inc-long-test
  (println "unchecked-inc-long-test")
  (testing "increment elements of a long[] via the application of a java static method"
    (let [n 64
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (clojure.lang.Numbers/unchecked-inc (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (long-array (range n)) (long-array n))))))

(deftest inc-long-test
  (println "inc-long-test")
  (testing "increment elements of a long[] via the application of a builtin function"
    (let [n 64
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (inc (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (long-array (range n)) (long-array n))))))

;; *** Error in `/home/jules/workspace/clumatra/jdk1.8.0-graal/bin/java': double free or corruption (out): 0x00007f64b88c4d10 ***
;; *** Error in `/home/jules/workspace/clumatra/jdk1.8.0-graal/bin/java': malloc(): memory corruption: 0x00007f64b88c4d90 ***

;; (defn ^long my-inc [^long l] (inc l))

;; (deftest my-inc-long-test
;;   (println "my-inc-long-test")
;;   (testing "increment elements of a long[] via the application of a named clojure function"
;;     (let [n 64
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (long (my-inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (long-array (range n))
;;            (long-array n))))))

;; *** Error in `/home/jules/workspace/clumatra/jdk1.8.0-graal/bin/java': corrupted double-linked list: 0x00007f30248bff00 ***

;; (defn ^:static ^long my-static-inc [^long l] (inc l)) ;I don't think this is static..

;; (deftest my-static-inc-long-test
;;   (println "my-static-inc-long-test")
;;   (testing "increment elements of a long[] via the application of a named static clojure function"
;;     (let [n 64
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (long (my-static-inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (long-array (range n))
;;            (long-array n))))))

;; fails comparison

;; (deftest anonymous-inc-long-test
;;   (println "anonymous-inc-long-test")
;;   (testing "increment elements of a long[] via the application of an anonymous clojure function"
;;     (let [my-inc (fn [^long l] (inc l))
;;           n 64
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (long (my-inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (long-array (range n))
;;            (long-array n))))))

;; ;;------------------------------------------------------------------------------

(definterface FloatKernel (^void invoke [^floats in ^floats out ^int gid]))

(deftest float-test
  (println "float-test")
  (testing "copy elements of a float[]"
    (let [n 64
          kernel (reify FloatKernel
                   (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (float-array (range n)) (float-array n))))))

;; com.oracle.graal.graph.GraalInternalError: Node implementing Lowerable not handled in HSAIL Backend: 110|NewArray

;; (deftest inc-float-test
;;   (println "inc-float-test")
;;   (testing "increment elements of a float[] via application of a java static method"
;;     (let [n 64
;;           kernel (reify FloatKernel
;;                    (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
;;                      (aset out gid (float (inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (float-array (range n)) (float-array n))))))

;;------------------------------------------------------------------------------

(definterface DoubleKernel (^void invoke [^doubles in ^doubles out ^int gid]))

(deftest double-test
  (println "double-test")
  (testing "copy elements of a double[]"
    (let [n 64
          kernel (reify DoubleKernel
                   (^void invoke [^CharKernel self ^doubles in ^doubles out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (double-array (range n)) (double-array n))))))

(deftest multiplyP-double-test
  (println "multiplyP-double-test")
  (testing "double elements of a double[] via application of a java static method"
    (let [n 64
          kernel (reify DoubleKernel
                   (^void invoke [^CharKernel self ^doubles in ^doubles out ^int gid]
                     (aset out gid (clojure.lang.Numbers/multiplyP (aget in gid) (double 2.0)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (double-array (range n)) (double-array n))))))

(deftest quotient-double-test
  (println "quotient-double-test")
  (testing "quotient elements of a double[] via application of a java static method"
    (let [n 64
          kernel (reify DoubleKernel
                   (^void invoke [^CharKernel self ^doubles in ^doubles out ^int gid]
                     (aset out gid (clojure.lang.Numbers/quotient (aget in gid) (double 2.0)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (double-array (range n)) (double-array n))))))

;;------------------------------------------------------------------------------

(definterface ObjectKernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

(deftest object-test
  (println "object-test")
  (testing "copy elements of an object[]"
    (let [n 64
          kernel (reify ObjectKernel
                   (^void invoke [^ObjectKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
                     (aset out i (aget in i))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (into-array Object (range n)) (make-array Object n))))))

;;------------------------------------------------------------------------------

(deftest Long-test
  (println "Long-test")
  (testing "copy Long elements of an Object[]"
    (let [n 64
          kernel (reify ObjectKernel
                   (^void invoke [^ObjectKernel self ^objects in ^objects out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (into-array Object (range n)) (make-array Object n))))))

;; com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest Long-multiplication-test
;;   (println "Long-test")
;;   (testing "copy Long elements of an Object[]"
;;     (let [n 64
;;           kernel (reify ObjectKernel
;;                    (^void invoke [^ObjectKernel self ^objects in ^objects out ^int gid]
;;                      (aset out gid (* (aget in gid) (aget in gid)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (into-array Object (range n)) (make-array Object n))))))

;;------------------------------------------------------------------------------

;; (definterface ObjectBooleanKernel (^void invoke [^"[Ljava.lang.Object;" in ^booleans out ^int gid]))

;; fails under jenkins !!

;; (deftest isZero-test
;;   (println "isZero-test")
;;   (testing "apply static java function to elements of Object[]"
;;     (let [n 64
;;           kernel (reify ObjectBooleanKernel
;;                    (invoke [self in out gid]
;;                      (aset out gid (clojure.lang.Numbers/isZero (aget in gid)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (into-array ^Object (range 64))
;;            (boolean-array n))))))

;; #  SIGSEGV (0xb) at pc=0x00007fec8635b91c, pid=10777, tid=140653852776192

;; (deftest isPos-test
;;   (println "isPos-test")
;;   (testing "apply static java function to elements of Object[]"
;;     (let [n 64
;;           kernel (reify ObjectBooleanKernel
;;                    (invoke [self in out gid]
;;                      (aset out gid (clojure.lang.Numbers/isPos (aget in gid)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (into-array ^Object (range -16 16))
;;            (boolean-array n))))))

;; com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest isNeg-test
;;   (println "isNeg-test")
;;   (testing "apply static java function to elements of Object[]"
;;     (let [n 64
;;           kernel (reify ObjectBooleanKernel
;;                    (invoke [self in out gid]
;;                      (aset out gid (clojure.lang.Numbers/isNeg (aget in gid)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (into-array ^Object (range -16 16))
;;            (boolean-array n))))))

;;------------------------------------------------------------------------------

(definterface StringIntKernel (^void invoke [^"[Ljava.lang.String;" in ^ints out ^int gid]))

(deftest string-length-test
  (println "string-length-test")
  (testing "find lengths of an array of Strings via application of a java virtual method"
    (let [n 64
          kernel (reify StringIntKernel
                   (^void invoke [^StringIntKernel self ^"[Ljava.lang.String;" in ^ints out ^int gid]
                     (aset out gid (.length ^String (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (into-array ^String (map (fn [^Long i] (.toString i)) (range n)))
           (int-array n))))))

;;------------------------------------------------------------------------------

(definterface ListLongKernel (^void invoke [^"[Lclojure.lang.PersistentList;" in ^longs out ^int i]))

;; com.oracle.graal.graph.GraalInternalError: Node implementing Lowerable not handled in HSAIL Backend: 358|NewInstance

;; (deftest list-peek-test
;;   (println "list-peek-test")
;;   (testing "map 'peek' across an array of lists - call a method on a Clojure list"
;;     (let [n 64
;;           kernel (reify ListLongKernel
;;                    (^void invoke [^ListLongKernel self ^"[Lclojure.lang.PersistentList;" in ^longs out ^int i]
;;                      (aset out i (.peek ^clojure.lang.PersistentList (aget in i)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (into-array clojure.lang.PersistentList (map list (range n))) (long-array n))))))

;;------------------------------------------------------------------------------
