;; top-down approach to enabling GPU to be used to map a function across a vector

(ns clumatra.vector
  ;;;(:use [clojure.tools.cli :only [cli]])
  (:import [java.util
            Map TreeMap]
           [java.util.concurrent.atomic
            AtomicReference]
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
;; A change of tack -

;; Copy vector contents into a single array, dispatch kernel on this, then copy
;; results back out into a vector...
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

(defn vector-node-to-array [offset shift ^PersistentVector$Node src ^objects tgt]
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

(defn vector-to-array [^PersistentVector src]
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

(let [^TreeMap powers-of-32
      (reduce
       (fn [^Map m [k v]] (.put m k v) m)
       (doto (TreeMap.) (.put 0 5))
       (map (fn [p] (let [b (* p 5)] [(inc (bit-shift-left 1 b)) b])) (range 2 10)))]
  
  (defn find-shift [n] (.getValue (.floorEntry powers-of-32 n)))
  )

(defn down-shift [n] (if (= n 5) 5 (- n 5)))
  
(defn round-up [n] (int (Math/ceil (double n))))

(defn ^PersistentVector$Node array-to-vector-node [^AtomicReference atom ^objects src src-start width shift]
  (let [tgt (object-array 32)]
    (if (= shift 5)
      (let [rem (- (count src) src-start)]
        (if (> rem 0)
          (System/arraycopy src src-start tgt 0 (min rem 32))))
      (let [new-shift (down-shift shift)
            new-width (bit-shift-left 1 new-shift)]
        (dotimes [n (round-up (/ width new-width))]
          (let [new-start (+ src-start (* n new-width))]
            (aset tgt n (array-to-vector-node atom src new-start new-width new-shift))))))
    (PersistentVector$Node. atom tgt)))

(defn array-to-vector [^objects src-array]
  (let [length (alength src-array)
        rem (mod length 32)
        tail-length (if (and (not (zero? length)) (= rem 0)) 32 rem)
        root-length (- length tail-length)
        shift (find-shift root-length)
        width (bit-shift-left 1 shift)
        atom (java.util.concurrent.atomic.AtomicReference. nil)
        root-array (object-array 32)
        nodes-needed (round-up (/ root-length (bit-shift-left 1 shift)))]
    (doall
     ;; TODO: use futures explicitly so that we can deal with tail on
     ;; foreground whilst branches are done in background...
     (pmap
      (fn [i]
        (let [start (* i width)
              end (min (- root-length start) width)]
        (aset root-array i (array-to-vector-node atom src-array start end shift))))
      (range nodes-needed)))
    (let [tail (object-array tail-length)]
      (System/arraycopy src-array (- length tail-length) tail 0 tail-length)
      (construct-vector length shift (PersistentVector$Node. atom root-array) tail))))

;;------------------------------------------------------------------------------
;; finally - this should be quite fast - when run on HSA h/w :-)

;; (def v (vec (range (* 32 32 32 32))))
;; (= (gvmap3 identity v) v) - true

;; target time is:
;; (dotimes [n 100] (time (do (mapv identity v) nil)))
;; on same hardware as:
;; (dotimes [n 100] (time (do (gvmap3 identity v) nil)))
;;; :-)

(defn gvmap [f ^PersistentVector v]
  (let [width (.count v)
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

;; consider migrating vector<->array fns to seqspert and providing
;; better access to vec's ctor...

;; experiment with adding items to a set via gpu...
;; kernel input: (object-array (mapv (fn [i] (object-array (range i (+ i 10)))) (range 10)))
;; kernel output: #{}[n]
;; kernel code (aset out n (into #{} (aget in n)))
