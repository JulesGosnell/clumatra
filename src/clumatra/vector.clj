;; top-down approach to enabling GPU to be used to map a function across a vector

(ns clumatra.vector
  ;;;(:use [clojure.tools.cli :only [cli]])
  (:import [java.util.concurrent.atomic AtomicReference]
           [clojure.lang
            PersistentVector PersistentVector$Node
            PersistentHashMap PersistentHashMap$INode
            IFn
            ])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clumatra
             [util :as u]
             [core :as c]]
            ;;[no [disassemble :as d]]
            )
  )

(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------
;; infrastructure

;; recurse down a node mapping the branch and leaf kernels across the
;; appropriate arrays, returning a new Node...
(defn vmap-node [^PersistentVector$Node n level bk lk]
  (PersistentVector$Node.
   (AtomicReference.)
   (let [a (.array n)]
     (if (zero? level)
       (lk a (object-array 32))
       (bk a (object-array 32) (dec level) bk)
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
         (let [t (.tail v)
               n (count t)] 
           (tk n t (object-array n)))])))))

;;------------------------------------------------------------------------------
;; local impl

;; wavefront size provided at construction time, no runtime args
(defn kernel-compile-leaf [f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
    (dotimes [i 32] (aset out i (f (aget in i))))
    ;;(clojure.lang.RT/amap f in out)
    out))

;; wavefront size provided at construction time, supports runtime args
(defn kernel-compile-branch [f]
  (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out & args]
    (dotimes [i 32] (aset out i (apply f (aget in i) args))) out))

;; expects call-time wavefront size - for handling variable size arrays
(defn kernel-compile-tail [f]
  (fn [n ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
    (dotimes [i n] (aset out i (f (aget in i))))
    out))

(defn vmap [f ^PersistentVector v]
  (let [lk (kernel-compile-leaf f)
        bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))
        tk (kernel-compile-tail f)]
    (vmap-2 f v bk lk tk)))

;;------------------------------------------------------------------------------
;; fork/join impl

(let [[fjpool fjtask fjinvoke fjfork fjjoin]
      (u/with-ns 'clojure.core.reducers [pool fjtask fjinvoke fjfork fjjoin])]

  (defn fjkernel-compile-branch [f]
    (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out & args]
      (fjinvoke
       (fn []
         (doseq [task 
                 (let [tasks (object-array 32)]
                   (dotimes [i 32] (aset tasks i (fjfork (fjtask #(aset out i (apply f (aget in i) args))))))
                   tasks)]
           (fjjoin task))))
      out))
  
  ;; (defn fjprocess-tail [f t]
  ;;   (fjjoin (fjinvoke (fn [] (fjfork (fjtask #(process-tail f t)))))))

  )

(defn fjvmap [f ^PersistentVector v]
  (let [lk (kernel-compile-leaf f)
        bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))
        pbk (fjkernel-compile-branch (fn [n l pbk] (if n (vmap-node n l bk lk))))
        tk (kernel-compile-tail f)]
    (vmap-2 f v pbk lk tk)))     ;TODO run tail on another thread ?

;;------------------------------------------------------------------------------
;; Okra impl

;; return a kernel fn that does not need the call-time provision of
;; wavefront size
(definterface
  OkraBranchKernel
  (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i ^long level ^Object bar]))

(defn okra-branch-kernel-compile [foo]
  (let [kernel
        (reify
          OkraBranchKernel
          (^void invoke [^Kernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i ^long level ^Object bar]
            (aset out i (foo (aget in i) level bar))))]
    (c/okra-kernel-compile kernel (u/fetch-method (class kernel) "invoke") 1 1)))

(defn kl32 [k] (fn [in out & args] (apply k 32 in out args) out))

;; we still need to define a new vector type with a branching factor
;; of 64, to take full advantage of compute units...
(defn gvmap [f ^PersistentVector v]
  (let [k (c/simple-kernel-compile f) ; use same okra kernel for leaf and tail
        lk (kl32 k)
        ;; use an fj kernel for branches - nodes and arrays will be built in parallel
        bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))
        pbk (fjkernel-compile-branch (fn [n l pbk] (if n (vmap-node n l bk lk))))
 
        ;; if okra supported allocation etc we might be able to do
        ;; this - but it might be too slow...

;        bk (kl32 (okra-branch-kernel-compile (fn [n l bk] (if n (vmap-node n l bk lk)))))

        ;; if kernels were concurrently shareable, we could reuse innards of lk here...
        ;; lets assume that they are...
        tk k]
    ;; N.B. root and tail are being run in serial NOT parallel - consider...
    (vmap-2 f v bk lk tk)))

;;------------------------------------------------------------------------------
;;; lets try reducing

;; (defn kernel-compile-reduce-leaf [f]
;;   (fn [init ^"[Ljava.lang.Object;" a]
;;     (clojure.lang.RT/areduce f init a)
;;     ;; (let [l (alength a)]
;;     ;;   (loop  [i 0 r init]
;;     ;;     (if (< i l)
;;     ;;       (recur (unchecked-inc i) (f r (aget a i)))
;;     ;;       r)))
;;     ))

;; (defn kernel-compile-reduce-branch [f]
;;   (fn [init ^"[Ljava.lang.Object;" a]
;;     (clojure.lang.RT/areduce f init a)
;;     ;; (let [l (alength a)]
;;     ;;   (loop  [i 0 r init]
;;     ;;     (if (< i l)
;;     ;;       (recur (unchecked-inc i) (f r (aget a i)))
;;     ;;       r)))
;;     ))

;; (defn vreduce-node [^PersistentVector$Node n level bk lk]
;;   (let [a (.array n)]
;;     (if (= 1 level)
;;       (lk a (make-array Object 32))
;;       (bk a (make-array Object 32) (dec level) bk)
;;       )))

;; (defn vreduce-2 [f ^PersistentVector v bk lk tk]
;;   (let [shift (.shift v)]
;;     (apply
;;      f
;;      (vreduce-node (.root v) (/ shift 5) bk lk)
;;      (tk f (.tail v)))))

;; (defn vreduce [bf lf v]
;;   (let [lk (kernel-compile-leaf lf)
;;         bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))]
;;     (vreduce-2 f v bk lk process-tail)))

;; (defn array-reduce [f init ^"[Ljava.lang.Object;" a]
;;   (let [l (alength a)]
;;     (loop  [i 0 r init]
;;       (if (< i l)
;;         (recur (unchecked-inc i) (f r (aget a i)))
;;          r))))

;;------------------------------------------------------------------------------

;; TODO:
;;  add a gvmap that uses gpu
;;  define vreduce, fjvreduce and gvreduce
;;  timings and tests
;;  fixed and variable sized kernel compilation
;;  load-time kernel compilation of fn -> [bk, lk, tk] and map fns that accept this in place of fn
;;  as above for reductions
;;  we need versions of these maps that zip - more playing with macros...

;; equals is a zip and map of equality operator over 
