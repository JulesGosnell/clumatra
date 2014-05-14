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

(defn gvmap [f ^PersistentVector v]
  (let [lk (kl32 (c/simple-kernel-compile f))
        bk (kl32 (okra-branch-kernel-compile (fn [n l bk] (if n (vmap-node n l bk lk)))))
        ;; if kernels were concurrently shareable, we could reuse innards of lk here...
        tk (c/simple-kernel-compile f)]
    (vmap-2 f v bk lk tk)))

;;------------------------------------------------------------------------------
;; Okra impl - another idea - package pairs of Object[32] up to look like Object[64]
;; to my GPU, which has  anative wavefront of this size...

;; further thinking.

;; apparently, compute-units are at their most efficient when
;; execution of all their cores is "locked" - i.e. all cores are
;; executing the same code in sync with each other.  I fear that the
;; necessary 'if' in pset/pget would mean that this is not the case if
;; I introduce this pattern into my kernel execution.

;; but if this is the case then most of the code that you might wish
;; to execute on gpu will have the same problem - ask someone who
;; knows...

;; if sumatra/graal/okra is targetting the java stream api, then, at
;; some level, they will have to address the fact that work does not
;; always arrive in chunks of the same width as the available
;; compute-unit. If this is done at the graal/okra level then I
;; may/should be able to reuse it. I think it is more likely that they
;; would implement the breaking down of larger kernel widths to fit
;; compute-units than their aggregation, which is what I would like,
;; but you never know.

(defprotocol Pair
  (left [_])
  (right [_])
  (pget [_ i])
  (pset [_ i v])
  )

(deftype PairImpl [^objects l ^objects r]
  Pair
  (left [_] l)
  (right [_] r)

  ;;(pget [_ i] (if (> i 31) (aget r (- i 32)) (aget l i)))
  ;;(pset [_ i v] (if (> i 31) (aset r (- i 32) v) (aset l i v)))

  ;; I think this will spend less time SPMD and more SIMD - more ifs
  ;; but they are shorter...
  (pget [_ i]   (let [j (rem i 32)] (aget (if (= i j) l r) j)))
  (pset [_ i v] (let [j (rem i 32)] (aset (if (= i j) l r) j v)))
  )

;; we could switch between arrays depending on whether i was odd or
;; even - not so intuititive but could it be quicker?
;; e.g. (clojure.lang.Numbers/isZero (clojure.lang.Numbers/and
;; (Integer. 2) 1)) - we still have to do the subtraction of 32 from i
;; so probably no quicker.

;;; lets hope graal can run identical kernels side-by-side on the same
;;; compute-unit - then they could handle index translation in a way
;;; that did not "unlock" execution.

;; could I use and address-translation table -
;; i.e. 0->0,1->1,...33->1,34->2,... ? needs to also produce correct
;; output array without using an if.

;; now we have to work out:
;; whether we can call kernels on Pairs instead of Object[]s
;; how to reduce mapping f over v into a series of applications of kernel to PairImpls

;; recurse to bottom-1 tier of tree
;; should be holding Object[32][32]
;; reduce it into 16xPairImpls, run them on gpu and unpack back into Object[32][32]
;; return up stack - except that on way down, same transformation should occur at each level


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

;; notes:
;; [] -> root = Object[32]{nil,...}, tail=Object[0], shift=5
;; [0] -> root = Object[32]{nil,...}, tail=Object[1]{0}, shift=5
;; (vec (range 32)) -> root=Object[32]{nil,...}, tail=Object[32]{0,1,2,...,31}, shift=5
;; (vec (range 33)) -> root=Object[32]{old-tail, nil,...}, tail=Object[1]{32}, shift=5
;; (vec (range 65)) -> root=Object[32]{older-tail, old-tail,nil,...}, tail=Object[1]{64}, shift=5
;; (vec (range (+ (* 32 32) 1))) -> root=Object[32][32]{older-tail, old-tail,...}, tail=Object[1]{1024}, shift=5
;; (vec (range (+ (* 32 32) 32 1))) -> root=Object[32][32][32]{recent-tail, nil,...}, tail=Object[1]{1056}, shift=10

;; etc

;; so
;; tail is always full
;; arrays are always filled from [0]
;; only leaf arrays carry data, branch arrays only carry structure

;; possible approaches:

;; 1 thread recurses to bottom of input vec building complete empty
;; output vec and launching single kernel at bottom for entire
;; iteration. Arrays would be allocated on way down and wrapped in
;; Nodes on way up whilst kernel was running. We need some efficient
;; index maths so that tail can be tacked on end of kernel...

;; going forward, we could step down a level in the vec and launch
;; ncores threads to each do as above on a subvec - would need a
;; little thought re how we partition vec and tail...

;; lets just focus on first step

;; (defn bar [c s ^PersistentVector$Node n ^"[Ljava.lang.Object;" t]
;;   (let [a (.array n)]
;;     (if (zero? s)
;;       ;; we are a leaf
;;       (println (seq a))
;;       ;; we are a branch
;;       (dotimes [i 32] (bar c (- s 5) n (aget a i)))
;;     )))

;; (defn foo [^PersistentVector v]
;;   (let [c (.count v)
;;         s (.shift v)
;;         ^PersistentVector$Node r (.root v)
;;         ^"[Ljava.lang.Object;" t (.tail v)]
;;     ;; new Vector(c s 
;;     (bar c s r t)))
