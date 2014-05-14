(ns clumatra.test-util
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
            [clumatra.util :refer :all]
            [clumatra.core :refer :all])
  (:gen-class))

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

(defn public? [^Method m]
  (let [modifiers (.getModifiers m)]
    (java.lang.reflect.Modifier/isPublic modifiers)))

(defn static? [^Method m]
  (let [modifiers (.getModifiers m)]
    (java.lang.reflect.Modifier/isStatic modifiers)))

(defn non-static? [m] (fn [m] (not (static? m))))

(defn public-static? [^Method m]
  (let [modifiers (.getModifiers m)]
    (and (java.lang.reflect.Modifier/isPublic modifiers)
         (java.lang.reflect.Modifier/isStatic modifiers))))

(defmacro deftest-kernel [method default-input-fn input-fns]
  (let [^Method method# (eval method)
        default-input-fn# (eval default-input-fn)
        input-fns# input-fns
        dummy (println "GENERATING:" (.toString method#))
        static# (static? method#)
        method-name# (str (.getName (.getDeclaringClass method#)) "/" (.getName method#))
        input-types# (concat (if static# nil [(.getDeclaringClass method#)]) (.getParameterTypes method#))
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
                         (~(symbol (if static# method-name# (str "." (.getName method#))))
                          ~@(map (fn [e#] (list 'aget e# wid#)) input-params#)))))
               results#
               (test-kernel
                ~kernel#
                ~default-input-fn#
                ~(mapv vector input-types# (concat (input-fns# method#) (repeat identity)))
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

(defn test-kernel [kernel default-input-fn in-types-and-fns out-type]
  (let [method (fetch-method (class kernel) "invoke")
        out-element (type->default out-type)
        out-fn (if out-element
                 (fn [] (into-array out-type (repeat *wavefront-size* out-element)))
                 (fn [] (make-array out-type *wavefront-size*)))
        in-arrays (mapv (fn [[t f]] (into-array t (map f (map default-input-fn (range *wavefront-size*))))) in-types-and-fns)
        ;;;in-arrays (mapv (fn [t] (into-array t (map identity (range 1 (inc *wavefront-size*))))) in-types)
        compiled (okra-kernel-compile kernel method (count in-arrays) 1)] ;compile once
    [(r-seq (apply compiled  *wavefront-size* (conj in-arrays (out-fn))))              
     (r-seq (apply compiled  *wavefront-size* (conj in-arrays (out-fn)))) ;run twice
     (r-seq (apply (local-kernel-compile kernel method (count in-arrays) 1)  *wavefront-size* (conj in-arrays (out-fn))))])) ;compare against control

;;------------------------------------------------------------------------------

(def primitive-types
  #{Boolean/TYPE Character/TYPE Byte/TYPE Short/TYPE Integer/TYPE Long/TYPE Float/TYPE Double/TYPE})

(defn primitive? [^Class t]
  (contains? primitive-types t))
  
(defn takes-only-primitives? [^Method m]
  (every? primitive? (.getParameterTypes m)))

(defn returns-primitive? [^Method m]
  (primitive? (.getReturnType m)))

(defmacro deftest-kernels [methods default-input-fn input-fns]
  (let [default-input-fn# (eval default-input-fn)
        input-fns# (eval input-fns)]
    (conj (map (fn [method#] `(deftest-kernel ~method# ~default-input-fn# ~input-fns#)) (eval methods)) 'do)))

;;------------------------------------------------------------------------------

(defn extract-methods [modifier ^Class class excluded-methods]
  (filter
   (fn [m] (not (contains? excluded-methods m)))
   (filter
    modifier
    (filter
     public?
     (.getMethods class)))))

;;------------------------------------------------------------------------------

;; N.B.

;; I think that we are going to have trouble dealing with lazy
;; sequences gpu-side. Easiest thing to do is to disallow them.

;;------------------------------------------------------------------------------



