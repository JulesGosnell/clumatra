(ns clumatra.core-test
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core [reducers :as r]]
            [clumatra.core :refer :all]))

(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------

(deftest object-test
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

;;------------------------------------------------------------------------------

(defn find-method [object ^String name]
  (first (filter (fn [^Method method] (= (.getName method) "invoke")) (.getDeclaredMethods (class object)))))

;;------------------------------------------------------------------------------

(definterface IntKernel (^void invoke [^ints in ^ints out ^int gid]))

(deftest int-test
  (testing "increment an int array"
    (let [n 32
          kernel (reify IntKernel
                   (^void invoke [^IntKernel self ^ints in ^ints out ^int gid]
                     (aset out gid (clojure.lang.Numbers/unchecked-inc (aget in gid)))))]
      (is (=
           (seq
            ((kernel-compile2 kernel (find-method kernel "invoke") n)
             (int-array (range n))
             (int-array n)))
           (range 1 (inc n)))))))

;;------------------------------------------------------------------------------

(definterface LongKernel (^void invoke [^longs in ^longs out ^int gid]))

(deftest long-test
  (testing "increment a long array via the application of a static method"
    (let [n 32
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (clojure.lang.Numbers/unchecked-inc (aget in gid)))))]
      (is (=
           (seq
            ((kernel-compile2 kernel (find-method kernel "invoke") n)
             (long-array (range n))
             (long-array n)))
           (range 1 (inc n)))))))

;;------------------------------------------------------------------------------

(deftest long-function-test
  (testing "increment a long array via the application of a function"
    (let [n 32
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (inc (aget in gid)))))]
      (is (=
           (seq
            ((kernel-compile2 kernel (find-method kernel "invoke") n)
             (long-array (range n))
             (long-array n)))
           (range 1 (inc n)))))))


;; gives  com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest long-function-test
;;   (testing "increment a long array via the application of a function containing a function call"
;;     (let [my-inc (fn [^long l] (inc l))
;;           n 32
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (my-inc (aget in gid)))))]
;;       (is (=
;;            (seq
;;             ((kernel-compile2 kernel (find-method kernel "invoke") n)
;;              (long-array (range n))
;;              (long-array n)))
;;            (range 1 (inc n)))))))

;; gives  com.oracle.graal.graph.GraalInternalError: unimplemented

;; (deftest long-function-test
;;   (testing "increment a long array via the application of a function containing a static method call"
;;     (let [my-inc (fn [^long l] (clojure.lang.Numbers/unchecked-inc l))
;;           n 32
;;           kernel (reify LongKernel
;;                    (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
;;                      (aset out gid (my-inc (aget in gid)))))]
;;       (is (=
;;            (seq
;;             ((kernel-compile2 kernel (find-method kernel "invoke") n)
;;              (long-array (range n))
;;              (long-array n)))
;;            (range 1 (inc n)))))))

;;------------------------------------------------------------------------------

(definterface StringIntKernel (^void invoke [^"[Ljava.lang.String;" in ^ints out ^int gid]))

(deftest string-int-test
  (testing "find length of an array of Strings"
    (let [n 32
          kernel (reify StringIntKernel
                   (^void invoke [^StringIntKernel self ^"[Ljava.lang.String;" in ^ints out ^int gid]
                     (aset out gid (.length ^String (aget in gid)))))]
      (is (= (seq ((kernel-compile2 kernel (find-method kernel "invoke") n)
                   (into-array ^String (map (fn [^Long i] (.toString i)) (range n)))
                   (int-array n)))
             (map (fn [^Long i] (.length (.toString i))) (range n)))))))
