;; top-down approach to enabling GPU to be used to map a function across a vector

(ns clumatra.vector
  ;;;(:use [clojure.tools.cli :only [cli]])
  (:import [java.util.concurrent.atomic AtomicReference]
           [clojure.lang
            PersistentVector PersistentVector$Node
            PersistentHashMap PersistentHashMap$INode
            ])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clumatra
             [util :as u]])
  )

(set! *warn-on-reflection* true)

(defn process-tail [f ^"[Ljava.lang.Object;" in]
  (let [n (count in)
        ^"[Ljava.lang.Object;" out (make-array Object n)]
    (loop [i 0]
      (if (< i n)
        (do
          (aset out i (f (aget in i)))
          (recur (unchecked-inc i)))
        out))))

(defn kernel-compile-leaf [f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
    (loop [i 0]
      (if (< i 32)
        (do
          (aset out i (f (aget in i)))
          (recur (unchecked-inc i)))
        out))))

(defn kernel-compile-branch [f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out & args]
    (loop [i 0]
      (if (< i 32)
        (do
          (aset out i (apply f (aget in i) args))
          (recur (unchecked-inc i)))
        out))))

;; recurse down a node mapping the branch and leaf kernels across the
;; appropriate arrays, returning a new Node...
(defn vmap-node [^PersistentVector$Node n level bk lk]
  (PersistentVector$Node.
   (AtomicReference.)
   (let [a (.array n)]
     (if (zero? level)
       (lk a (make-array Object 32))
       (bk a (make-array Object 32) (dec level) bk)
       ))))

(let [ObjectArray (type (into-array Object []))
      ctor (u/unlock-constructor
            PersistentVector
            (into-array
             Class
             [(Integer/TYPE) (Integer/TYPE) PersistentVector$Node ObjectArray]))]

  (defn vmap-2 [f ^PersistentVector v bk lk tk]
    (let [shift (.shift v)]
      (.newInstance
       ctor
       (into-array
        Object
        [(count v)
         shift
         (vmap-node (.root v) (/ shift 5) bk lk)
         (tk f (.tail v))])))))


(let [[fjpool fjtask fjinvoke fjfork fjjoin]
      (u/with-ns 'clojure.core.reducers [pool fjtask fjinvoke fjfork fjjoin])]

  (defn fjkernel-compile-branch [f]
    (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out & args]
      (fjinvoke
       ;; can this be simplified ?
       (fn []
         (doseq [task (mapv 
                       (fn [i] (fjfork (fjtask #(aset out i (apply f (aget in i) args)))))
                       [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31])]
           (fjjoin task))))
      out))

  (defn fjprocess-tail [f t]
    ;;can this be simplified ?
    (fjjoin (fjinvoke (fn [] (fjfork (fjtask #(process-tail f t)))))))

  )

(defn vmap [f ^PersistentVector v]
  (let [lk (kernel-compile-leaf f)
        bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))]
    (vmap-2 f v bk lk process-tail)))

(defn fjvmap [f ^PersistentVector v]
  (let [lk (kernel-compile-leaf f)
        bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))
        pbk (fjkernel-compile-branch (fn [n l pbk] (if n (vmap-node n l bk lk))))]
    (vmap-2 f v pbk lk fjprocess-tail)))

;;------------------------------------------------------------------------------

;; TODO:
;;  add a gvmap that uses gpu
;;  define vreduce, fjvreduce and gvreduce
;;  timings and tests

