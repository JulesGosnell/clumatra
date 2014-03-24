(ns clumatra.vector
  (:import [java.util.concurrent.atomic AtomicReference]
           [clojure.lang
            PersistentVector PersistentVector$Node
            PersistentHashMap PersistentHashMap$INode
            ])
  ;;(:require [clumatra [core :as gpu]])
  )

(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------

(defmulti kernel-compile-leaf (fn [backend function] backend))
(defmulti kernel-compile-branch (fn [backend function] backend))

(defmethod kernel-compile-leaf :sequential [_ f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
    (loop [i 0]
      (if (< i 32)
        (do
          (aset out i (f (aget in i)))
          (recur (unchecked-inc i)))
        out))))

(defmethod kernel-compile-branch :sequential [_ f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out & args]
    (loop [i 0]
      (if (< i 32)
        (do
          (aset out i (apply f (aget in i) args))
          (recur (unchecked-inc i)))
        out))))

(defmethod kernel-compile-branch :threads-parallel [f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out & args]
    (pmap (fn [i] (aset out i (apply f (aget in i) args))) (range 32))
    out
    ))

;; :forkjoin-parallel - TODO

;;(defmethod compile-leaf :okra-parallel [_ f] (gpu/kernel-compile f 32))

;;------------------------------------------------------------------------------

;; map a kernel across an input array returning the output array...
(defn vmap-leaf-array [^"[Ljava.lang.Object;" in k]
  (k in (make-array Object 32)))

(defn vmap-branch-array [^"[Ljava.lang.Object;" in level k]
  (k in (make-array Object 32) level k))

;; recurse down a node mapping the branch and leaf kernels across the
;; appropriate arrays, returning a new Node...
(defn vmap-node [^PersistentVector$Node n level bk lk]
  (PersistentVector$Node.
   (AtomicReference.)
   (let [a (.array n)]
     (if (zero? level)
       (vmap-leaf-array a lk)
       (vmap-branch-array a (dec level) bk)
       ))))

(defn ^java.lang.reflect.Constructor unlock-constructor [^Class class param-types]
  (doto (.getDeclaredConstructor class param-types) (.setAccessible true)))

(defn process-tail [f ^"[Ljava.lang.Object;" in]
  (let [n (count in)
        ^"[Ljava.lang.Object;" out (make-array Object n)]
    (loop [i 0]
      (if (< i n)
        (do
          (aset out i (f (aget in i)))
          (recur (unchecked-inc i)))
        out))))

(let [ObjectArray (type (into-array Object []))
      ctor (unlock-constructor
            PersistentVector
            (into-array
             Class
             [(Integer/TYPE) (Integer/TYPE) PersistentVector$Node ObjectArray]))]

  (defn vmap 
    ([f ^PersistentVector v]
       (vmap :sequential :sequential f v))
    ([branch-backend leaf-backend f ^PersistentVector v]
       (let [lk (kernel-compile-leaf :sequential f)
             bk (kernel-compile-branch :sequential (fn [n l bk] (if n (vmap-node n l bk lk))))
             shift (.shift v)]
         (.newInstance
          ctor
          (into-array
           Object
           [(count v)
            shift
            (vmap-node (.root v) (/ shift 5) bk lk)
            (process-tail f (.tail v))]))))))

;;------------------------------------------------------------------------------
;; TODO: fjvmap


