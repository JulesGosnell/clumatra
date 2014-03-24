(ns clumatra.hash-map
  (:import [java.util.concurrent.atomic AtomicReference]
           [clojure.lang
            PersistentVector PersistentVector$Node
            PersistentHashMap PersistentHashMap$INode
            ])
  ;;(:require [clumatra [core :as gpu]])
  )

(set! *warn-on-reflection* true)

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
