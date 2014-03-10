(ns clojure.lang
  (:import [java.util.concurrent.atomic AtomicReference]
           [clojure.lang PersistentVector PersistentVector$Node]))

(set! *warn-on-reflection* true)

;; java7 - serial
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

;; ;; java8 - serial
;; (defn kernel-compile [f]
;;   (let [kernel (reify java.util.function.IntConsumer (accept [self i] (kernel in out i)))]
;;     (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
;;       (.forEach (java.util.stream.IntStream/range 0 (min (count in) (count out))) kernel))))

;; ;; java8 - parallel
;; (defn kernel-compile [f]
;;   (let [kernel (reify java.util.function.IntConsumer (accept [self i] (kernel in out i)))]
;;   (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
;;     (.forEach (.parallel (java.util.stream.IntStream/range 0 (min (count in) (count out))) kernel)))))

;; ;; see core for graal/java8

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

  (defn vmap [f ^PersistentVector v]
    (let [lk (kernel-compile-leaf f)
          bk (kernel-compile-branch (fn [n l bk] (if n (vmap-node n l bk lk))))
          shift (.shift v)]
      (.newInstance
       ctor
       (into-array
        Object
        [(count v)
         shift
         (vmap-node (.root v) (/ shift 5) bk lk)
         (process-tail f (.tail v))])))))

;;------------------------------------------------------------------------------
;; TODO:
;; - integrate with core
;; - unroll array loops with macro
;; - unroll level loop to/from given depth with macro - then we won't need to pass param to branch-kernel
;; - implement reduction into hashset/map in similar way

