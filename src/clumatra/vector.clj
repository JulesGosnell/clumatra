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
;; (clojure.pprint/pprint (macroexpand-1 '(map-array (inc 2) (into-array [1 2 3]) inc)))
;; (seq (map-array (inc 2) (into-array [1 2 3]) inc))

;; using this seems to be slightly slower - probably set up costs with let

;; (defmacro map-array [size i o function]
;;   (let [in  (with-meta (gensym) {:tag "[Ljava.lang.Object;"})
;;         out (with-meta (gensym) {:tag "[Ljava.lang.Object;"})
;;         f (gensym)
;;         s# (eval size)]
;;     `(let [~in  ~i
;;            ~out ~o
;;            ~f ~function]
;;        (do
;;          ~@(doall
;;             (map
;;              (fn [i] `(aset ^"[Ljava.lang.Object;" ~out ~i (~f (aget ~in ~i))))
;;              (range s#))))
;;        ~out)))
 
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
;; TODO:
;; - integrate with core
;; - unroll array loops with macro
;; - unroll level loop to/from given depth with macro - then we won't need to pass param to branch-kernel
;; - implement reduction into hashset/map in similar way
;;------------------------------------------------------------------------------


;;------------------------------------------------------------------------------
;; not sure that this is actually productive
;; - on the input side it WOULD save a little in terms of iterator churn
;; - on the output side, the cost of producing tuples of [size, node] and [key, value] might be more expensive than just putting a PersistentMap around the node and calling (assoc) on it. Perhaps all HashMap nodes could be HashMaps in their own right and carry size and an assoc/conj method.

;; (defn ^PersistentHashMap$INode reduce-vector-array-into-hashmap-node [f l ^"[Ljava.lang.Object;" in]
;; )

;; (let [ctor (unlock-constructor
;;             PersistentHashMap
;;             (into-array
;;              Class
;;              [(Integer/TYPE) clojure.lang.PersistentHashMap$INode (Boolean/TYPE) Object]))]
;;   (defn reduce-vector-into-hashmap [f ^PersistentVector v]
;;     (let [levels (/ (.shift v) 5)
;;           [root-count root-node] (reduce-vector-array-into-hashmap-node f levels (.root v))
;;           [tail-count tail-node] (reduce-vector-array-into-hashmap-node f levels (.tail v))
;;           [new-count new-node] [0 nil] somehow merge root and tail
;;           ]
;;     (.newInstance ctor (into-array Object [(int new-count) new-node false nil])))))

;; (reduce-vector-into-hashmap inc [0 1 2 3 4 5])
