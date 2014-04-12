(ns clumatra.core-test
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
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
  (fn [& args]
    (doseq [^Long i (range n)]
      (.invoke method kernel (into-array Object (concat args (list (int i))))))
    (last args)))

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

;;------------------------------------------------------------------------------

(def ^:dynamic *wavefront-size* 4)

(def type->default
  {(Boolean/TYPE)   false
   (Character/TYPE) \#
   (Byte/TYPE)      (byte 0)
   (Short/TYPE)     (short 0)
   (Integer/TYPE)   (int 0)
   (Long/TYPE)      0
   (Float/TYPE)     (float 0)
   (Double/TYPE)    (double 0)})

;;------------------------------------------------------------------------------
;; another go at macro-ising this all up...

(def type->array-type 
  {(Boolean/TYPE)   (class (boolean-array 0))
   (Character/TYPE) (class (char-array 0))
   (Byte/TYPE)      (class (byte-array 0))
   (Short/TYPE)     (class (short-array 0))
   (Integer/TYPE)   (class (int-array 0))
   (Long/TYPE)      (class (long-array 0))
   (Float/TYPE)     (class (float-array 0))
   (Double/TYPE)    (class (double-array 0))})

(defn make-param [n t]
  (with-meta (gensym n) {:tag t}))

(defn make-array-param [n t]
  (make-param n (type->array-type t)))

(def method->input-fns
  {
   (.getDeclaredMethod clojure.lang.Numbers "quotient" (into-array Class [Double/TYPE Double/TYPE])) inc
   (.getDeclaredMethod clojure.lang.Numbers "quotient" (into-array Class [Long/TYPE Long/TYPE])) inc
   (.getDeclaredMethod clojure.lang.Numbers "quotient" (into-array Class [Long/TYPE Double/TYPE])) inc
   (.getDeclaredMethod clojure.lang.Numbers "quotient" (into-array Class [Double/TYPE Long/TYPE])) inc
   })

(defn method-symbol [^Method method]
  (symbol (.replace (.toString method) " " "_")))

(defmacro deftest-kernel [method]
  (let [^Method method# (eval method)
        input-fns# (method->input-fns method#)
        method-name# (str (.getName (.getDeclaringClass method#)) "/" (.getName method#))
        input-types# (.getParameterTypes method#)
        input-params# (mapv (fn [t] (make-array-param "in_" t)) input-types#)
        output-type# (.getReturnType method#)
        output-param# (make-array-param "out_" output-type#)
        wid-param# (make-param "wid_" Integer/TYPE)
        wid# (with-meta wid-param# nil)
        Kernel# (gensym "Kernel_")
        kernel# (gensym "kernel_")
        interface-params# (into [] (concat input-params# (list output-param#) (list wid-param#)))
        implementation-params# (into [] (concat (list (gensym "self_")) interface-params#))
        invoke# (with-meta (symbol "invoke") {:tag Void/TYPE})]
    `(do
       (definterface ~Kernel# ~(list invoke# interface-params#))
       (deftest ~(method-symbol method#)
         (println "Testing:" ~(.toString method#))
         (let [~(with-meta kernel# {:tag Kernel#})
               (reify
                 ~Kernel#
                 (~invoke# ~implementation-params#
                   (aset ~output-param# ~wid#
                         (~(symbol method-name#)
                          ~@(map (fn [e#] (list 'aget e# wid#)) input-params#)))))
               results#
               (test-kernel
                ~kernel#
                ~(mapv vector input-types#
                      ;;(concat ~input-fns# 
                      (repeat `identity)
                      ;;)
                      )
                ~output-type#)]
           (is (apply = results#)))))))

;; handle method invocations as well as static functions
;; reuse kernel interfaces where appropriate
;; write another macro to generate tests using this one
;; write some tests for this macro

;; 
;; (p/pprint (macroexpand-1 '(deftest-kernel (first primitive-number-methods))))
;; (eval (macroexpand-1 '(deftest-kernel (first primitive-number-methods))))

;;------------------------------------------------------------------------------

(defn test-kernel [kernel in-types-and-fns out-type]
  (let [method (find-method kernel "invoke")
        out-element (type->default out-type)
        in-arrays (mapv (fn [[t f]] (into-array t (map f (range 1 (inc *wavefront-size*))))) in-types-and-fns)
        compiled (okra-kernel-compile kernel method *wavefront-size*)] ;compile once
    [(seq (apply compiled (conj in-arrays (into-array out-type (repeat *wavefront-size* out-element)))))              
     (seq (apply compiled (conj in-arrays (into-array out-type (repeat *wavefront-size* out-element))))) ;run twice
     (seq (apply (local-kernel-compile kernel method *wavefront-size*) (conj in-arrays (into-array out-type (repeat *wavefront-size* out-element)))))])) ;compare against control

;;------------------------------------------------------------------------------

(definterface BooleanKernel (^void invoke [^booleans in ^booleans out ^int gid]))


(deftest boolean-copy-test
  (testing "copy elements of a boolean[]"
    (let [kernel (reify BooleanKernel
                   (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Boolean/TYPE even?]] Boolean/TYPE)]
      (is (apply = results)))))

(deftest boolean-if-test
  (testing "flip elements of a boolean[]"
    (let [kernel (reify BooleanKernel
                   (^void invoke [^BooleanKernel self ^booleans in ^booleans out ^int gid]
                     (aset out gid (if (aget in gid) false true))))
          results (test-kernel kernel [[Boolean/TYPE even?]] Boolean/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface ByteKernel (^void invoke [^bytes in ^bytes out ^int gid]))

(deftest byte-copy-test
  (testing "copy elements of a byte[]"
    (let [kernel (reify ByteKernel
                   (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Byte/TYPE identity]] Byte/TYPE)]
      (is (apply = results)))))

(deftest byte-inc-test
  (testing "increment elements of a byte[] via application of a java static method"
    (let [kernel (reify ByteKernel
                   (^void invoke [^ByteKernel self ^bytes in ^bytes out ^int gid]
                     (aset out gid (byte (inc (aget in gid))))))
          results (test-kernel kernel [[Byte/TYPE identity]] Byte/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface CharKernel (^void invoke [^chars in ^chars out ^int gid]))

(deftest char-copy-test
  (testing "copy elements of a char[]"
    (let [kernel (reify CharKernel
                   (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Character/TYPE (fn [n] (+ 65 (mod n 26)))]] Character/TYPE)]
      (is (apply = results)))))

(deftest char-toLowercase-test
  (testing "downcase elements of an char[] via application of a java static method"
    (let [kernel (reify CharKernel
                   (^void invoke [^CharKernel self ^chars in ^chars out ^int gid]
                     (aset out gid (java.lang.Character/toLowerCase (aget in gid)))))
          results (test-kernel kernel [[Character/TYPE (fn [n] (+ 65 (mod n 26)))]] Character/TYPE)]
      (is (apply = results)))))

;; ;;------------------------------------------------------------------------------

(definterface ShortKernel (^void invoke [^shorts in ^shorts out ^int gid]))

(deftest short-copy-test
  (testing "copy elements of a short[]"
    (let [kernel (reify ShortKernel
                   (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Short/TYPE identity]] Short/TYPE)]
      (is (apply = results)))))

(deftest short-inc-test
  (testing "increment elements of a short[] via application of a java static method"
    (let [kernel (reify ShortKernel
                   (^void invoke [^ShortKernel self ^shorts in ^shorts out ^int gid]
                     (aset out gid (short (inc (aget in gid))))))
          results (test-kernel kernel [[Short/TYPE identity]] Short/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface IntKernel (^void invoke [^ints in ^ints out ^int gid]))

(deftest int-copy-test
  (testing "copy elements of an int[]"
    (let [kernel (reify IntKernel
                   (^void invoke [^IntKernel self ^ints in ^ints out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Integer/TYPE identity]] Integer/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface LongKernel (^void invoke [^longs in ^longs out ^int gid]))

(deftest long-copy-test
  (testing "copy elements of a long[]"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(deftest long-inc-test
  (testing "increment elements of a long[] via the application of a builtin function"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (inc (aget in gid)))))
          results (test-kernel kernel [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(defn ^long my-inc [^long l] (inc l))

(deftest long-my-inc-test
  (testing "increment elements of a long[] via the application of a named clojure function"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (long (my-inc (aget in gid))))))
          results (test-kernel kernel [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(defn ^:static ^long my-static-inc [^long l] (inc l)) ;I don't think this is static..

(deftest long-my-static-inc-test
  (testing "increment elements of a long[] via the application of a named static clojure function"
    (let [kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (long (my-static-inc (aget in gid))))))
          results (test-kernel kernel [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

(deftest long-anonymous-inc-test
  (testing "increment elements of a long[] via the application of an anonymous clojure function"
    (let [my-inc (fn [^long l] (inc l))
          kernel (reify LongKernel
                   (^void invoke [^LongKernel self ^longs in ^longs out ^int gid]
                     (aset out gid (long (my-inc (aget in gid))))))
          results (test-kernel kernel [[Long/TYPE identity]] Long/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface FloatKernel (^void invoke [^floats in ^floats out ^int gid]))

(deftest float-copy-test
  (testing "copy elements of a float[]"
    (let [kernel (reify FloatKernel
                   (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Float/TYPE identity]] Float/TYPE)]
      (is (apply = results)))))

(deftest float-inc-test
  (testing "increment elements of a float[] via application of a java static method"
    (let [kernel (reify FloatKernel
                   (^void invoke [^FloatKernel self ^floats in ^floats out ^int gid]
                     (aset out gid (float (inc (aget in gid))))))
          results (test-kernel kernel [[Float/TYPE identity]] Float/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface DoubleKernel (^void invoke [^doubles in ^doubles out ^int gid]))

(deftest double-copy-test
  (testing "copy elements of a double[]"
    (let [kernel (reify DoubleKernel
                   (^void invoke [^CharKernel self ^doubles in ^doubles out ^int gid]
                     (aset out gid (aget in gid))))
          results (test-kernel kernel [[Double/TYPE identity]] Double/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface ObjectKernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

(deftest object-copy-test
  (testing "copy elements of an object[]"
    (let [kernel (reify ObjectKernel
                   (^void invoke [^ObjectKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
                     (aset out i (aget in i))))
          results (test-kernel kernel [[Object identity]] Object)]
      (is (apply = results)))))

(deftest multiplication-test
  (testing "square ?Long? elements of an Object[]"
    (let [kernel (reify ObjectKernel
                   (^void invoke [^ObjectKernel self ^objects in ^objects out ^int gid]
                     (aset out gid (* (aget in gid) (aget in gid)))))
          results (test-kernel kernel [[Object identity]] Object)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(definterface StringIntKernel (^void invoke [^"[Ljava.lang.String;" in ^ints out ^int gid]))

(deftest string-length-test
  (testing "find lengths of an array of Strings via application of a java virtual method"
    (let [kernel (reify StringIntKernel
                   (^void invoke [^StringIntKernel self ^"[Ljava.lang.String;" in ^ints out ^int gid]
                     (aset out gid (.length ^String (aget in gid)))))
          results (test-kernel kernel [[String (fn [^Long i] (.toString i))]] Integer/TYPE)]
      (is (apply = results)))))

;;------------------------------------------------------------------------------

(defn public-static? [^Method m]
  (let [modifiers (.getModifiers m)]
    (and (java.lang.reflect.Modifier/isPublic modifiers)
         (java.lang.reflect.Modifier/isStatic modifiers))))

(def primitive-types (into #{} (keys type->array-type)))

(defn primitive? [^Class t]
  (contains? primitive-types t))
  
(defn takes-only-primitives? [^Method m]
  (every? primitive? (.getParameterTypes m)))

(defn returns-primitive? [^Method m]
  (primitive? (.getReturnType m)))


(defmacro deftest-kernels [methods]
  (conj (map (fn [method#] `(deftest-kernel ~method#)) (eval methods)) 'do))

;;------------------------------------------------------------------------------

(def excluded-methods #{
                       (.getDeclaredMethod clojure.lang.RT "nextID" nil)
                       (.getDeclaredMethod clojure.lang.RT "booleanCast" (into-array Class [Boolean/TYPE]))
                       })

(defn extract-methods [^Class class]
  (filter
   (fn [m] (not (contains? excluded-methods m)))
   (filter
    returns-primitive?
    (filter
     takes-only-primitives?
     (filter
      public-static?
      (.getDeclaredMethods class))))))

;;------------------------------------------------------------------------------

(deftest-kernels (extract-methods clojure.lang.RT))
(deftest-kernels (extract-methods clojure.lang.Numbers))

