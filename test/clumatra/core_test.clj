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
   (Double/TYPE)    (double 0)
   
   Object           1
   })

;;------------------------------------------------------------------------------
;; another go at macro-ising this all up...

(defn type->array-type-2 [^Class t] (class (make-array t 0)))
(def type->array-type (memoize type->array-type-2))

(defn make-param [n t]
  (with-meta (gensym n) {:tag t}))

(defn make-array-param [n t]
  (make-param n (type->array-type t)))

(defn method-symbol [^Method method]
  (symbol (.replace (.toString method) " " "_")))

(def input-fns
  {
   (.getDeclaredMethod clojure.lang.RT "box" (into-array Class [Boolean/TYPE])) [even?]

   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Boolean/TYPE)])) [(fn [i](boolean-array [(even? i)]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Byte/TYPE)])) [(fn [i](byte-array [i]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Character/TYPE)])) [(fn [i](char-array [(char (+ 65 i))]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Double/TYPE)])) [(fn [i](double-array [(double i)]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Float/TYPE)])) [(fn [i](float-array [(float i)]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Integer/TYPE)])) [(fn [i](int-array [(int i)]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Long/TYPE)])) [(fn [i](long-array [i]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Object)])) [(fn [i](into-array Object [i]))]
   (.getDeclaredMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Short/TYPE)])) [(fn [i](short-array [(short i)]))]

    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Boolean/TYPE) Integer/TYPE])) [(fn [i](boolean-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Byte/TYPE) Integer/TYPE])) [(fn [i](byte-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Character/TYPE) Integer/TYPE])) [(fn [i](char-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Double/TYPE) Integer/TYPE])) [(fn [i](double-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Float/TYPE) Integer/TYPE])) [(fn [i](float-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Integer/TYPE) Integer/TYPE])) [(fn [i](int-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Long/TYPE) Integer/TYPE])) [(fn [i](long-array (inc i)))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Object) Integer/TYPE])) [(fn [i](into-array Object (range (inc i))))]
    (.getDeclaredMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Short/TYPE) Integer/TYPE])) [(fn [i](short-array (inc i)))]

   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Boolean/TYPE)])) [(fn [i](boolean-array [(even? i)]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Byte/TYPE)])) [(fn [i](byte-array [i]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Character/TYPE)])) [(fn [i](char-array [(char (+ 65 i))]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Double/TYPE)])) [(fn [i](double-array [(double i)]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Float/TYPE)])) [(fn [i](float-array [(float i)]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Integer/TYPE)])) [(fn [i](int-array [(int i)]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Long/TYPE)])) [(fn [i](long-array [i]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Object)])) [(fn [i](into-array Object [i]))]
   (.getDeclaredMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Short/TYPE)])) [(fn [i](short-array [(short i)]))]

    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Boolean/TYPE) Integer/TYPE  Boolean/TYPE])) [(fn [i](boolean-array (inc i))) identity even?]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Byte/TYPE) Integer/TYPE Byte/TYPE])) [(fn [i](byte-array (inc i))) identity byte]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Character/TYPE) Integer/TYPE Character/TYPE])) [(fn [i](char-array (inc i))) identity (fn [i] (char (+ 65  i)))]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Double/TYPE) Integer/TYPE Double/TYPE])) [(fn [i](double-array (inc i))) identity double]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Float/TYPE) Integer/TYPE Float/TYPE])) [(fn [i](float-array (inc i))) identity float]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Integer/TYPE) Integer/TYPE Integer/TYPE])) [(fn [i](int-array (inc i))) identity int]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Long/TYPE) Integer/TYPE Long/TYPE])) [(fn [i](long-array (inc i))) identity long]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Object) Integer/TYPE Object])) [(fn [i](into-array Object (range (inc i)))) identity identity]
    (.getDeclaredMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Short/TYPE) Integer/TYPE Short/TYPE])) [(fn [i](short-array (inc i))) identity short]

   })

(defmacro deftest-kernel [method]
  (let [^Method method# (eval method)
        dummy (println "GENERATING:" (.toString method#))
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
                ~(mapv vector input-types# (concat (input-fns method#) (repeat identity)))
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

(defn array? [^Object a] (.isArray (.getClass a)))

(defn r-seq [a]
  (if (array? a) [:array (map r-seq a)] a))

(defn test-kernel [kernel in-types-and-fns out-type]
  (let [method (find-method kernel "invoke")
        out-element (type->default out-type)
        out-fn (if out-element
                 (fn [] (into-array out-type (repeat *wavefront-size* out-element)))
                 (fn [] (make-array out-type *wavefront-size*)))
        in-arrays (mapv (fn [[t f]] (into-array t (map f (range 1 (inc *wavefront-size*))))) in-types-and-fns)
        ;;;in-arrays (mapv (fn [t] (into-array t (map identity (range 1 (inc *wavefront-size*))))) in-types)
        compiled (okra-kernel-compile kernel method *wavefront-size*)] ;compile once
    [(r-seq (apply compiled (conj in-arrays (out-fn))))              
     (r-seq (apply compiled (conj in-arrays (out-fn)))) ;run twice
     (r-seq (apply (local-kernel-compile kernel method *wavefront-size*) (conj in-arrays (out-fn))))])) ;compare against control

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

(def primitive-types
  #{Boolean/TYPE Character/TYPE Byte/TYPE Short/TYPE Integer/TYPE Long/TYPE Float/TYPE Double/TYPE})

(defn primitive? [^Class t]
  (contains? primitive-types t))
  
(defn takes-only-primitives? [^Method m]
  (every? primitive? (.getParameterTypes m)))

(defn returns-primitive? [^Method m]
  (primitive? (.getReturnType m)))


(defmacro deftest-kernels [methods]
  (conj (map (fn [method#] `(deftest-kernel ~method#)) (eval methods)) 'do))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (.getDeclaredMethod clojure.lang.RT "booleanCast" (into-array Class [Boolean/TYPE]))
    (.getDeclaredMethod clojure.lang.Numbers "divide" (into-array Class [java.math.BigInteger,java.math.BigInteger]))
    (.getDeclaredMethod clojure.lang.Numbers "reduceBigInt" (into-array Class [clojure.lang.BigInt]))

    ;; these are not suitable for testing
    (.getDeclaredMethod clojure.lang.RT "nextID" nil) ;; impure
    (.getDeclaredMethod clojure.lang.RT "makeClassLoader" nil)
    (.getDeclaredMethod clojure.lang.RT "baseLoader" nil)
    (.getDeclaredMethod clojure.lang.RT "errPrintWriter" nil)
    (.getDeclaredMethod clojure.lang.RT "init" nil)

    ;; these need more work on overriding input types/values
    (.getDeclaredMethod clojure.lang.Numbers "ints" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "booleans" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "bytes" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "shorts" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "doubles" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "chars" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "floats" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "longs" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "char_array" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "double_array" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.Numbers "float_array" (into-array Class [Object]))

    (.getDeclaredMethod clojure.lang.Numbers "boolean_array" (into-array Class [Integer/TYPE Object]))
    (.getDeclaredMethod clojure.lang.Numbers "char_array" (into-array Class [Integer/TYPE Object]))
    (.getDeclaredMethod clojure.lang.Numbers "byte_array" (into-array Class [Integer/TYPE Object]))
    (.getDeclaredMethod clojure.lang.Numbers "short_array" (into-array Class [Integer/TYPE Object]))

    ;; these crash simulated build
    (.getDeclaredMethod clojure.lang.Numbers "min" (into-array Class [Long/TYPE Double/TYPE]))
    (.getDeclaredMethod clojure.lang.Numbers "min" (into-array Class [Double/TYPE Long/TYPE]))
    (.getDeclaredMethod clojure.lang.Numbers "max" (into-array Class [Double/TYPE Long/TYPE]))
    (.getDeclaredMethod clojure.lang.Numbers "max" (into-array Class [Long/TYPE Double/TYPE]))
    (.getDeclaredMethod clojure.lang.Numbers "num" (into-array Class [Float/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "list" (into-array Class []))

    ;; failing 
    (.getDeclaredMethod clojure.lang.RT "keyword" (into-array Class [String String]))
    (.getDeclaredMethod clojure.lang.RT "processCommandLine" (into-array Class [(type->array-type String)]))
    (.getDeclaredMethod clojure.lang.RT "var" (into-array Class [String String]))
    (.getDeclaredMethod clojure.lang.RT "var" (into-array Class [String String Object]))
    (.getDeclaredMethod clojure.lang.RT "seq" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "meta" (into-array Class [Object]))

    (.getDeclaredMethod clojure.lang.RT "loadResourceScript" (into-array Class [Class String]))
    (.getDeclaredMethod clojure.lang.RT "loadResourceScript" (into-array Class [String Boolean/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "loadResourceScript" (into-array Class [Class String Boolean/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "loadResourceScript" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "maybeLoadResourceScript" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "classForName" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "loadClassForName" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "resourceAsStream" (into-array Class [ClassLoader String]))
    (.getDeclaredMethod clojure.lang.RT "getResource" (into-array Class [ClassLoader String]))
    (.getDeclaredMethod clojure.lang.RT "load" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "load" (into-array Class [String Boolean/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "loadLibrary" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "getColumnNumber" (into-array Class [java.io.Reader]))
    (.getDeclaredMethod clojure.lang.RT "getLineNumberingReader" (into-array Class [java.io.Reader]))
    (.getDeclaredMethod clojure.lang.RT "isLineNumberingReader" (into-array Class [java.io.Reader]))
    (.getDeclaredMethod clojure.lang.RT "resolveClassNameInContext" (into-array Class [java.lang.String]))
    (.getDeclaredMethod clojure.lang.RT "printString" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "readString" (into-array Class [String]))
    (.getDeclaredMethod clojure.lang.RT "lastModified" (into-array Class [java.net.URL String]))
    (.getDeclaredMethod clojure.lang.RT "readChar" (into-array Class [java.io.Reader]))
    (.getDeclaredMethod clojure.lang.RT "peekChar" (into-array Class [java.io.Reader]))
    (.getDeclaredMethod clojure.lang.RT "addURL" (into-array Class [java.lang.Object]))
    (.getDeclaredMethod clojure.lang.RT "getLineNumber" (into-array Class [java.io.Reader]))

    (.getDeclaredMethod clojure.lang.RT "isReduced" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "seqToTypedArray" (into-array Class [Class clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "seqToTypedArray" (into-array Class [clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "seqToPassedArray" (into-array Class [clojure.lang.ISeq (type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "object_array" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "arrayToList" (into-array Class [(type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "listStar" (into-array Class [Object Object clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "listStar" (into-array Class [Object Object Object Object Object clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "listStar" (into-array Class [Object Object Object clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "listStar" (into-array Class [Object Object Object Object clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "listStar" (into-array Class [Object clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "subvec" (into-array Class [clojure.lang.IPersistentVector Integer/TYPE Integer/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "vector" (into-array Class [(type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "mapUniqueKeys" (into-array Class [(type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedDoubleCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedFloatCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedLongCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedIntCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedCharCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedShortCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "uncheckedByteCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "doubleCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "floatCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "longCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "intCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "shortCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "byteCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "booleanCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "charCast" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "assocN" (into-array Class [Integer/TYPE Object Object]))
    (.getDeclaredMethod clojure.lang.RT "nth" (into-array Class [Object Integer/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "nth" (into-array Class [Object Integer/TYPE Object]))
    (.getDeclaredMethod clojure.lang.RT "dissoc" (into-array Class [Object Object]))
    (.getDeclaredMethod clojure.lang.RT "findKey" (into-array Class [clojure.lang.Keyword clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "assoc" (into-array Class [Object Object Object]))
    (.getDeclaredMethod clojure.lang.RT "more" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "fourth" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "third" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "second" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "conj" (into-array Class [clojure.lang.IPersistentCollection Object]) )
    (.getDeclaredMethod clojure.lang.RT "vals" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "seqOrElse" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "map" (into-array Class [(type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "peek" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "formatAesthetic" (into-array Class [java.io.Writer Object]))
    (.getDeclaredMethod clojure.lang.RT "formatStandard" (into-array Class [java.io.Writer Object]))
    (.getDeclaredMethod clojure.lang.RT "print" (into-array Class [Object java.io.Writer]))

    ;; doesn't crash my stuff, but does fail test...
 
    (.getDeclaredMethod clojure.lang.RT "boundedLength" (into-array Class [clojure.lang.ISeq Integer/TYPE]))
    (.getDeclaredMethod clojure.lang.RT "box" (into-array Class [Boolean]))
    (.getDeclaredMethod clojure.lang.RT "cons" (into-array Class [Object Object]))
    (.getDeclaredMethod clojure.lang.RT "contains" (into-array Class [Object Object]))
    (.getDeclaredMethod clojure.lang.RT "count" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "doFormat" (into-array Class [java.io.Writer String clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "find" (into-array Class [Object Object]))
    (.getDeclaredMethod clojure.lang.RT "first" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "format" (into-array Class [Object String (type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "get" (into-array Class [Object Object]))
    (.getDeclaredMethod clojure.lang.RT "keys" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "length" (into-array Class [clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "next" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "pop" (into-array Class [Object]))
    (.getDeclaredMethod clojure.lang.RT "seqToArray" (into-array Class [clojure.lang.ISeq]))
    (.getDeclaredMethod clojure.lang.RT "set" (into-array Class [(type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "setValues" (into-array Class [(type->array-type Object)]))
    (.getDeclaredMethod clojure.lang.RT "toArray" (into-array Class [Object]))
    })

(defn extract-methods [^Class class]
  (filter
   (fn [m] (not (contains? excluded-methods m)))
     (filter
      public-static?
      (.getDeclaredMethods class))))

;;------------------------------------------------------------------------------

(deftest-kernels (extract-methods clojure.lang.RT))
(deftest-kernels(extract-methods clojure.lang.Numbers))

