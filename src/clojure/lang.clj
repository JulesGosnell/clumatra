(ns clojure.lang
  (:import [java.util.concurrent.atomic AtomicReference]
           [clojure.lang PersistentVector PersistentVector$Node]))

(set! *warn-on-reflection* true)


;; java8 vmap-amap - parallel
(defn vmap-amap [in out l kernel]
  (.forEach
   (.parallel (java.util.stream.IntStream/range 0 l))
   (reify java.util.function.IntConsumer (accept [self i] (kernel in out i)))))

;; java8 vmap-amap - sequential
(defn vmap-amap [in out l kernel]
  (.forEach
   (java.util.stream.IntStream/range 0 l)
   (reify java.util.function.IntConsumer (accept [self i] (kernel in out i)))))

;; simple vmap-amap
(defn vmap-amap [^objects in ^objects out l kernel]
  (loop [i 0]
    (if (< i l)
      (do
        (kernel in out i)
        (recur (unchecked-inc i)))
      out)))

(defn kernelise [f]
  (fn [^objects in ^objects out i] (aset out i (f (aget in i)))))

;; TODO: how do we compile two interdependent functions ?

(defn vmap-node [n k] (println "SHOULD NOT BE CALLED"))

(defn vmap-array [k ^objects a]
  (let [func (if (= (type (nth a 0)) PersistentVector$Node)
               (kernelise (fn [v] (if v (vmap-node k v)))) ;TODO: churn
               k)
        l (alength a)]
    (vmap-amap a (make-array Object l) l func)

    ;(amap a i _ (func (aget a i)))

    ))

(defn vmap-node [k ^PersistentVector$Node n]
  (PersistentVector$Node. (AtomicReference.) (vmap-array k (.array n))))

(let [param-types (into-array 
                   Class
                   [(Integer/TYPE) (Integer/TYPE) PersistentVector$Node (type (into-array Object []))])
      ^java.lang.reflect.Constructor c (unlock-ctor PersistentVector param-types)]
  (defn vmap [f ^PersistentVector v]
    (let [vk (kernelise f)
          nk (kernelise (fn [v] (if v (vmap-node vk v))))]
      (.newInstance
       c
       (into-array
        Object
        [(count v)
         (.shift v)
         (vmap-node vk (.root v))
         (vmap-array vk (.tail v))])))))
      
;;(defmethod vmap PersistentVector [k ^PersistentVector v]
;;;  (PersistentVector. (vector-cnt v) (.shift v) (vmap k (.root v)) (vmap k (.tail v))))
      
(def a (into [] (range 1000000)))
(time (def b (mapv inc a)))
(time (def c (vmap inc a)))
(= b c)

;; try this with java 8


