(ns clumatra.core-test
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core [reducers :as r]]
            [clojure.core [rrb-vector :as v]]
            ;;[clumatra.core :refer :all]
            ;;[no [disassemble :as d]]
            )
  (:gen-class))

(set! *warn-on-reflection* true)
;;(set! *assert* true)
(set! *unchecked-math* true)

(defn -main [& args]
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

;; pinched from core reducers...
(defmacro ^:private compile-if [exp then else]
  (if (try (eval exp) (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

    (println)

(compile-if
 (= (System/getProperty "os.arch") "amd64")
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

(definterface CharKernel (^void invoke [^chars in ^chars out ^int gid]))

(deftest char-test
  (testing "copy elements of an char[]"
    (let [n 26
          kernel (reify CharKernel
                   (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
                     (aset out gid (aget in gid))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (char-array (map char (range 65 (+ 65 n)))) (char-array n))))))

;; wierd - I would expect this one to work - investigate...
;; com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest char-downcase-test
;;   (testing "downcase elements of an char[] via application of a java static method"
;;     (let [n 26
;;           kernel (reify CharKernel
;;                    (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
;;                      (aset out gid (java.lang.Character/toLowerCase (aget in gid)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (char-array (map char (range 65 (+ 65 n)))) (char-array n))))))

;;------------------------------------------------------------------------------

(definterface DoubleKernel (^void invoke [^doubles in ^doubles out ^int gid]))

(deftest double-test
  (testing "double elements of a double[] via application of a java static method"
    (let [n 32
          kernel (reify DoubleKernel
                   (^void invoke [^CharKernel self ^doubles in ^doubles out ^int gid]
                     (aset out gid (clojure.lang.Numbers/multiplyP (aget in gid) (double 2.0)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (double-array (range n)) (double-array n))))))

;;------------------------------------------------------------------------------

(definterface IntKernel (^void invoke [^ints in ^ints out ^int gid]))

(deftest int-test
  (testing "increment elements of an int[] via application of a java static method"
    (let [n 32
          kernel (reify IntKernel
                   (^void invoke [^IntKernel self ^ints in ^ints out ^int gid]
                     (aset out gid (clojure.lang.Numbers/unchecked-inc (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (int-array (range n)) (int-array n))))))

;;------------------------------------------------------------------------------

(definterface LongKernel (^void invoke [^longs in ^longs out ^int gid]))

(deftest long-static-method-test
  (testing "increment elemnts of a long[] via the application of a java static method"
    (let [n 32
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (clojure.lang.Numbers/unchecked-inc (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (long-array (range n)) (long-array n))))))

(deftest long-builtin-function-test
  (testing "increment elements of a long[] via the application of a builtin function"
    (let [n 32
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (inc (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (long-array (range n)) (long-array n))))))

(defn ^long my-inc [^long l] (inc l))

;; com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest long-clojure-function-test
;;   (testing "increment elements of a long[] via the application of a named clojure function"
;;     (let [n 32
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (long (my-inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (long-array (range n))
;;            (long-array n))))))

(defn ^:static ^long my-static-inc [^long l] (inc l)) ;I don't think this is static..

;; com.oracle.graal.graph.GraalInternalError.unimplemented (GraalInternalError.java:38)

;; (deftest long-static-clojure-function-test
;;   (testing "increment elements of a long[] via the application of a named static clojure function"
;;     (let [n 32
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (long (my-static-inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (long-array (range n))
;;            (long-array n))))))

;;com.oracle.graal.graph.GraalInternalError: java.lang.ClassCastException:
;;   com.oracle.graal.hotspot.hsail.HSAILHotSpotLIRGenerator cannot be cast to com.oracle.graal.hotspot.HotSpotLIRGenerator

;; (deftest long-function-test
;;   (testing "increment elements of a long[] via the application of an anonymous clojure function"
;;     (let [my-inc (fn [^long l] (inc l))
;;           n 32
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (long (my-inc (aget in gid))))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (long-array (range n))
;;            (long-array n))))))

;;------------------------------------------------------------------------------

(definterface StringIntKernel (^void invoke [^"[Ljava.lang.String;" in ^ints out ^int gid]))

(deftest string-int-test
  (testing "find lengths of an array of Strings via application of a java virtual method"
    (let [n 32
          kernel (reify StringIntKernel
                   (^void invoke [^StringIntKernel self ^"[Ljava.lang.String;" in ^ints out ^int gid]
                     (aset out gid (.length ^String (aget in gid)))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (into-array ^String (map (fn [^Long i] (.toString i)) (range n)))
           (int-array n))))))

;;------------------------------------------------------------------------------

(definterface ObjectKernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

;; we can copy Object efs from one array to another, but cannot yet create new Objects
;; I guess we could try looking up Objects that had already been created
(deftest object-test
  (testing "copy object array - we cannot create new objects yet"
    (let [n 32
          kernel (reify ObjectKernel
                   (^void invoke [^ObjectKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
                     (aset out i (aget in i))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (into-array Object (range n)) (make-array Object n))))))

;;------------------------------------------------------------------------------

(definterface ListLongKernel (^void invoke [^"[Lclojure.lang.PersistentList;" in ^longs out ^int i]))

;; a bit optimistic :-) - doesn't do any allocation, but does use more of Clojure's runtime...
;; com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest list-test
;;   (testing "map 'peek' across an array of lists - call a method on a Clojure list"
;;     (let [n 32
;;           kernel (reify ListLongKernel
;;                    (^void invoke [^ListLongKernel self ^"[Lclojure.lang.PersistentList;" in ^longs out ^int i]
;;                      (aset out i (.peek ^clojure.lang.PersistentList (aget in i)))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (into-array clojure.lang.PersistentList (map list (range n))) (long-array n))))))

;;------------------------------------------------------------------------------
;; IDEAS:
;;------------------------------------------------------------------------------
;; Graal config option: warn-on-Box
;; unimplemented error - what is unimplemented ?
;; Sumatra feature completion page - which bytecodes are implemented
;; Test should kick out kernel bytecode on failure
;; should be easy to switch test from local / emulator / gpu
;; how can we package these tests as junit so they can be run from command line ?
;; can we derive interface and reification from looking at signature of function or type of e.g. rrb-vector
;; what primitive types will be available on GPU ?
;;------------------------------------------------------------------------------

;;; TODO: boolean, short, float, byte
;;------------------------------------------------------------------------------

(definterface BooleanKernel (^void invoke [^booleans in ^booleans out ^int gid]))

;;com.oracle.graal.graph.GraalInternalError: should not reach here

;; (deftest boolean-test
;;   (testing "increment elements of an boolean[] via application of a java static method"
;;     (let [n 32
;;           kernel (reify BooleanKernel
;;                    (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
;;                      (aset out gid (if (aget in gid) false  true))))]
;;       (is (test-kernel
;;            kernel (find-method kernel "invoke") n
;;            (boolean-array (map even? (range n))) (boolean-array n))))))

;;------------------------------------------------------------------------------

(definterface ByteKernel (^void invoke [^bytes in ^bytes out ^int gid]))

(deftest byte-test
  (testing "increment elements of an byte[] via application of a java static method"
    (let [n 32
          kernel (reify ByteKernel
                   (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
                     (aset out gid (byte (inc (aget in gid))))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (byte-array (range n)) (byte-array n))))))

;;------------------------------------------------------------------------------

(definterface ShortKernel (^void invoke [^shorts in ^shorts out ^int gid]))

(deftest short-test
  (testing "increment elements of an short[] via application of a java static method"
    (let [n 32
          kernel (reify ShortKernel
                   (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
                     (aset out gid (short (inc (aget in gid))))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (short-array (range n)) (short-array n))))))

;;------------------------------------------------------------------------------

(definterface FloatKernel (^void invoke [^floats in ^floats out ^int gid]))

(deftest float-test
  (testing "increment elements of an float[] via application of a java static method"
    (let [n 32
          kernel (reify FloatKernel
                   (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
                     (aset out gid (float (inc (aget in gid))))))]
      (is (test-kernel
           kernel (find-method kernel "invoke") n
           (float-array (range n)) (float-array n))))))

;;------------------------------------------------------------------------------

;; (defmacro defkernel [^String name ^Class class]
;;   `(let [t# ~class
;;          ts# (class (make-array t# 0))]
;;      (definterface ~(symbol (str name "Kernel")) (^void invoke [^{:tag ts#} in ^{:tag ts#} out ^int gid]))))
