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

  ;; we could expose this better via java ?
  ;; args should be -  [count shift root tail]
  (defn construct-vector [count shift root tail] (.newInstance ctor (object-array [(int count) (int shift) root tail]))))

(defn vmap-2 [f ^PersistentVector v bk lk tk]
  (let [shift (.shift v)]
    (construct-vector
     (count v)
     shift
     (vmap-node (.root v) (/ shift 5) bk lk)
     (let [t (.tail v)
           n (count t)] 
       (tk n t (object-array n))))))

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
  (def fjpool fjpool)
  (def fjtask fjtask)
  (def fjinvoke fjinvoke)
  (def fjfork fjfork)
  (def fjjoin fjjoin))

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
; a more naive approach - using the single dispatch of a single kernel

(defn gvmap2 [f ^PersistentVector v]
  (let [width (count v)
        in (object-array v)
        out (object-array width)
        kernel (c/simple-kernel-compile f)]
    (kernel width in out)
    (into [] out)))

;; consider how we might split up the copying into and out of arrays
;; and achieve in parallel...

;; consider "fan-out" depth for such an approach...

;; we need an e.g. (fj-object-array vector) and (fjinto [] array)
;; ------------------------------------------------------------------------------



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

;;------------------------------------------------------------------------------
;; fast vector -> array - working

;; (def v (vec (range (* 32 32 32 32))))
;; (dotimes [n 100](time (object-array v)))
;; ...
;; "Elapsed time: 144.589973 msecs"
;; (dotimes [n 100](time (vector-to-array v)))
;; ...
;; "Elapsed time: 33.427521 msecs"

;; 4x faster on a 2 core laptop

;; (= (seq (object-array v)) (seq (vector-to-array v))) -> true

(defn vector-node-to-array [offset shift ^clojure.lang.PersistentVector$Node src ^objects tgt]
  (let [array (.array src)]
    (if (= shift 0)
      ;; copy leaf
      (System/arraycopy array 0 tgt offset 32)
      ;; copy child branches
      (let [m (clojure.lang.Numbers/shiftLeftInt 1 shift)
            new-shift (- shift 5)]
        (dotimes [n 32]
          (if-let [src (aget array n)]
            (vector-node-to-array (+ offset (* m n)) new-shift src tgt)))))))

(def thirty-two (vec (range 32)))

(defn vector-to-array [^clojure.lang.PersistentVector src]
  (let [length (.count src)
        tgt (object-array length)
        tail (.tail src)
        tail-length (alength tail)
        shift (.shift src)]
    (if (> shift 5)
      ;; parallel copy 
      (let [m (clojure.lang.Numbers/shiftLeftInt 1 shift)
            new-shift (- shift 5)
            root-array (.array (.root src))
            branches (reduce
                      (fn [r i]
                        (if-let [node (aget root-array i)]
                          (conj r (future (vector-node-to-array (* i m) new-shift node tgt)))
                          r))
                      []
                      thirty-two)]
        ;; copy tail whilst giving branches some time to run...
        (System/arraycopy tail 0 tgt (- length tail-length) tail-length)          
        ;; wait for all branches to finish
        (doseq [branch branches] (deref branch)))
      ;; sequential copy
      (do
        (vector-node-to-array 0 shift (.root src) tgt)
        (System/arraycopy tail 0 tgt (- length tail-length) tail-length)))
    tgt))

;;------------------------------------------------------------------------------
;; fast array -> vector - nearly working...

;; (def a (object-array (range (* 32 32 32 32))))
;; (dotimes [n 100](time (do (into [] a) nil)))
;; ...
;; "Elapsed time: 255.490017 msecs"
;; (dotimes [n 100](time (do (array-to-vector a) nil)))
;; ...
;; "Elapsed time: 20.570319 msecs"

;; 12x faster on a 2 core laptop

;; (= (into [] a) (array-to-vector a)) -> true

;; there is a faster way but...
(defn find-shift [n]
  (let [i (take-while (fn [n] (> n 0)) (iterate (fn [n] (bit-shift-right n 5)) n))
        shift (* 5 (dec (count i)))
        consumed (bit-shift-left 1 shift)
        remainder (- n consumed)]
    (+ shift (if (> remainder 0) 5 0))))
  
(defn array-to-vector-node [^objects src-array src-array-index width shift]
  ;;(println "array-to-vector-node" [^objects src-array src-array-index width shift])
  (let [array (object-array 32)
        atom nil                        ;TODO
        node (clojure.lang.PersistentVector$Node. atom array)]
    (if (= shift 5)
      (System/arraycopy src-array src-array-index array 0 32)
      (let [new-shift (- shift 5)
            new-width (bit-shift-left 1 new-shift)]
        (dotimes [n 32]
          (aset array n (array-to-vector-node src-array (+ src-array-index (* n new-width)) new-width new-shift)))))
    node))

(defn array-to-vector [^objects src-array]
  (let [length (alength src-array)
        shift (find-shift (- length 32))
        atom (java.util.concurrent.atomic.AtomicReference. nil)
        root-array (object-array 32)
        root (clojure.lang.PersistentVector$Node. atom root-array)]
    (doall
     (pmap
      (fn [i]
        (let [new-shift (- shift 5)
              new-width (bit-shift-left 1 new-shift)]
          (aset root-array i (array-to-vector-node src-array (* i new-width) new-width new-shift))))
      (range 32)))
    (let [rem (mod length 32)
          tail-length (if (= rem 0) 32 rem)
          tail (object-array tail-length)]
      (System/arraycopy src-array (- length tail-length) tail 0 tail-length)
      (construct-vector length (- shift 5) root tail))))

;;------------------------------------------------------------------------------
;; finally - this should be quite fast - when run on HSA h/w :-)

;; (def v (vec (range (* 32 32 32 32))))
;; (= (gvmap3 identity v) v) - true

;; target time is:
;; (dotimes [n 100] (time (do (mapv identity v) nil)))
;; on same hardware as:
;; (dotimes [n 100] (time (do (gvmap3 identity v) nil)))
;;; :-)

(defn gvmap3 [f ^PersistentVector v]
  (let [width (count v)
        in (vector-to-array v)
        out (object-array width)
        kernel (c/simple-kernel-compile f)]
    (kernel width in out)
    (array-to-vector out)))

;;------------------------------------------------------------------------------

;; try different flag combinations on h/w and s/w build
;; try to get repl working
;; time gvmap3 vs pmap or equivalent core/reduction
;; is it even better with 10,000,000 entries ?
;; could I do some simple instruction like inc or even a loop on the gpu ?
