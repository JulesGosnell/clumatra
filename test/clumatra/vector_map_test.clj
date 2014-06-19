(ns clumatra.vector-map-test
  (:import
   [java.util Collection Map]
   [clojure.lang
    IObj AFn ISeq IPersistentVector APersistentVector PersistentVector PersistentVector$Node
    Associative PersistentVector$TransientVector ITransientCollection Numbers]
   [clumatra.core ObjectArrayToObjectArrayKernel]
   [clumatra.vector VectorToObjectArrayKernel VectorToVectorKernel VectorNodeToVectorNodeKernel])
  (:require
   [clojure [pprint :as p]]
   [clojure.core
    [reducers :as r]
    [rrb-vector :as v]])
  (:use
   [clojure test]
   [clumatra util core vector test-util]))

;;------------------------------------------------------------------------------
;; gvmap2 - pass entire vector as input to kernel and copy directly
;; into empty output vector. gpu must execute branches in
;; vector-array. loops are unrolled to predermined sizes to avoid
;; further branches and recursion. trie and tail consumed by same
;; vector. currently flakey.

(defn foo [i l ^"[Ljava.lang.Object;" a]
  (.array ^PersistentVector$Node (aget a (bit-and (Numbers/unsignedShiftRight i l) 0x1f))))

(let [^objects lookup 
      (object-array
       [(fn [i l a] (foo i 5 a)) 
        (fn [i l a] (->> a (foo i 5)))
        (fn [i l a] (->> a (foo i 10) (foo i 5)))
        (fn [i l a] (->> a (foo i 15) (foo i 10) (foo i 5)))
        (fn [i l a] (->> a (foo i 20) (foo i 15) (foo i 10) (foo i 5)))
        (fn [i l a] (->> a (foo i 25) (foo i 20) (foo i 15) (foo i 10) (foo i 5)))
        (fn [i l a] (->> a (foo i 30) (foo i 25) (foo i 20) (foo i 15) (foo i 10) (foo i 5)))])]

  (defn vector-trie-array [i l a]
    ((aget lookup (/ l 5)) i l a)))

(defn vector-trie-count [^clojure.lang.PersistentVector v]
  (let [l (.length v)]
    (Numbers/shiftLeft (Numbers/unsignedShiftRight (dec l) 5) 5)))

(defn vector-trie-count [^clojure.lang.PersistentVector v]
  (- (.length v) (count (.tail v))))
  
(defn ^"[Ljava.lang.Object;" vector-array [^clojure.lang.PersistentVector v i]
  (if (>= i (vector-trie-count v))
    (.tail v)
    (vector-trie-array i (.shift v) (.array (.root v)))))

(defn vector-get [v i] (aget (vector-array v i) (bit-and i 0x1f)))
(defn vector-set [v i val] (aset (vector-array v i) (bit-and i 0x1f) val))

(defn vector-to-vector-mapping-kernel-compile [f]
  (let [kernel
        (reify VectorToVectorKernel
          (^void invoke
            [^VectorToVectorKernel self ^clojure.lang.PersistentVector in ^clojure.lang.PersistentVector out ^int i]
            (vector-set out i (inc (vector-get in i)))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

(deftest vector-to-vector-mapping-test
  (testing "can we perform a direct vector-to-vector (map f v) on the GPU ?"
    (let [w 10000
          in (vec (range w))
          out (empty-vector w)
          kernel (vector-to-vector-mapping-kernel-compile inc)]
      (is (= (kernel w in out)
             (mapv inc (range w)))))))

(defn gvmap2 [f ^PersistentVector in]
  (let [width (.count in)
        out (empty-vector width)
        kernel (vector-to-vector-mapping-kernel-compile f)]
    (kernel width in out)))

;;------------------------------------------------------------------------------
;; gvmap3 - no branching kernel - separate kernels used for trie and
;; tail. kernel rather than algorithm looked up in jump table.

(let [^"[Ljava.lang.Object;" kernel-fns
      (into-array
       Object
       [
        ;; shift = 0
        nil nil nil nil nil
        ;; shift = 5
        (fn [f]
          (let [kernel
                (reify VectorNodeToVectorNodeKernel
                  (^void invoke
                    [^VectorNodeToVectorNodeKernel self ^clojure.lang.PersistentVector$Node in ^clojure.lang.PersistentVector$Node out ^int i]
                    (let [p1 (Numbers/and (Numbers/unsignedShiftRight i 5) 0x1f)
                          p2 (Numbers/and i 0x1f)]
                      (aset
                       (.array ^PersistentVector$Node (aget (.array out) p1))
                       p2
                       (aget
                        (.array ^PersistentVector$Node (aget (.array in) p1))
                        p2)))))]
            (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))
        nil nil nil nil
        ;; shift = 10
        (fn [f]
          (let [kernel
                (reify VectorNodeToVectorNodeKernel
                  (^void invoke
                    [^VectorNodeToVectorNodeKernel self ^clojure.lang.PersistentVector$Node in ^clojure.lang.PersistentVector$Node out ^int i]
                    (let [p1 (Numbers/and (Numbers/unsignedShiftRight i 10) 0x1f)
                          p2 (Numbers/and (Numbers/unsignedShiftRight i 5) 0x1f)
                          p3 (Numbers/and i 0x1f)]
                      (aset
                       (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array out) p1)) p2))
                       p3
                       (aget
                        (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array in) p1)) p2))
                        p3)))))]
            (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))
        nil nil nil nil
        ;; shift = 15
        (fn [f]
          (let [kernel
                (reify VectorNodeToVectorNodeKernel
                  (^void invoke
                    [^VectorNodeToVectorNodeKernel self ^clojure.lang.PersistentVector$Node in ^clojure.lang.PersistentVector$Node out ^int i]
                    (let [p1 (Numbers/and (Numbers/unsignedShiftRight i 15) 0x1f)
                          p2 (Numbers/and (Numbers/unsignedShiftRight i 10) 0x1f)
                          p3 (Numbers/and (Numbers/unsignedShiftRight i 5) 0x1f)
                          p4 (Numbers/and i 0x1f)]
                      (aset
                       (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array out) p1)) p2)) p3))
                       p4
                       (aget
                        (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array in) p1)) p2)) p3))
                        p4)))))]
            (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))
        nil nil nil nil
        ;; shift = 20
        (fn [f]
          (let [kernel
                (reify VectorNodeToVectorNodeKernel
                  (^void invoke
                    [^VectorNodeToVectorNodeKernel self ^clojure.lang.PersistentVector$Node in ^clojure.lang.PersistentVector$Node out ^int i]
                    (let [p1 (Numbers/and (Numbers/unsignedShiftRight i 20) 0x1f)
                          p2 (Numbers/and (Numbers/unsignedShiftRight i 15) 0x1f)
                          p3 (Numbers/and (Numbers/unsignedShiftRight i 10) 0x1f)
                          p4 (Numbers/and (Numbers/unsignedShiftRight i 5) 0x1f)
                          p5 (Numbers/and i 0x1f)]
                      (aset
                       (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array out) p1)) p2)) p3)) p4))
                       p5
                       (aget
                        (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array in) p1)) p2)) p3)) p4))
                        p5)))))]
            (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))
        nil nil nil nil
        ;; shift = 25
        (fn [f]
          (let [kernel
                (reify VectorNodeToVectorNodeKernel
                  (^void invoke
                    [^VectorNodeToVectorNodeKernel self ^clojure.lang.PersistentVector$Node in ^clojure.lang.PersistentVector$Node out ^int i]
                    (let [p1 (Numbers/and (Numbers/unsignedShiftRight i 25) 0x1f)
                          p2 (Numbers/and (Numbers/unsignedShiftRight i 20) 0x1f)
                          p3 (Numbers/and (Numbers/unsignedShiftRight i 15) 0x1f)
                          p4 (Numbers/and (Numbers/unsignedShiftRight i 10) 0x1f)
                          p5 (Numbers/and (Numbers/unsignedShiftRight i 5) 0x1f)
                          p6 (Numbers/and i 0x1f)]
                      (aset
                       (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array out) p1)) p2)) p3)) p4)) p5))
                       p6
                       (aget
                        (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array in) p1)) p2)) p3)) p4)) p5))
                        p6)))))]
            (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))
        nil nil nil nil
        ;; shift = 30
        (fn [f]
          (let [kernel
                (reify VectorNodeToVectorNodeKernel
                  (^void invoke
                    [^VectorNodeToVectorNodeKernel self ^clojure.lang.PersistentVector$Node in ^clojure.lang.PersistentVector$Node out ^int i]
                    (let [j (Numbers/and i 0x1f)
                          p1 (Numbers/and (Numbers/unsignedShiftRight i 30) 0x1f)
                          p2 (Numbers/and (Numbers/unsignedShiftRight i 25) 0x1f)
                          p3 (Numbers/and (Numbers/unsignedShiftRight i 20) 0x1f)
                          p4 (Numbers/and (Numbers/unsignedShiftRight i 15) 0x1f)
                          p5 (Numbers/and (Numbers/unsignedShiftRight i 10) 0x1f)
                          p6 (Numbers/and (Numbers/unsignedShiftRight i 5) 0x1f)]
                      (aset
                       (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array out) p1)) p2)) p3)) p4)) p5)) p6))
                       j
                       (aget
                        (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array in) p1)) p2)) p3)) p4)) p5)) p6))
                        j)))))]
            (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))
        nil nil nil nil
        ;; shift=...
        ])]
  
  (defn select-vector-mapping-kernel-fn [^PersistentVector v] (aget kernel-fns (.shift v))))

(defn tail-mapping-kernel [f]
  (let [kernel
        (reify ObjectArrayToObjectArrayKernel
          (^void invoke
            [^ObjectArrayToObjectArrayKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
            (aset out i (aget in i))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

(defn gvmap3 [f ^PersistentVector in]
  (let [;;; hopefully tail-kernel will be executed first - we know it
        ;;; will not consume a whole compute-unit - maybe it will
        ;;; finish before this is needed...
        tail-count (count (.tail in))
        out-tail-array (object-array tail-count)
        tail-kernel (tail-mapping-kernel f) ;; memo ?
        tail-future (future (tail-kernel tail-count (.tail in) out-tail-array))
        width (.count in)
        ^PersistentVector out (empty-vector width out-tail-array)
        trie-count (- width tail-count)
        trie-kernel ((select-vector-mapping-kernel-fn in) f)] ; memo?
    ;; TODO: we could run tail on cpu whilst trie is running on gpu ?
    ;; TODO: should we run small tails/tries on cpu ?
    ;; do some timings first and see how it goes...
    (trie-kernel trie-count (.root in) (.root out))
    @tail-future
    out))

(deftest gvmap3-test
  (testing "can we map the identity fn across a large vector using the gpu ?"
    (let [in (vec (range 100))
          out (gvmap identity in)]
      (is (= out in))))
  (testing "can we map the identity fn across a large vector using the gpu ?"
    (let [in (vec (range 1000))
          out (gvmap identity in)]
      (is (= out in))))
  (testing "can we map the identity fn across a large vector using the gpu ?"
    (let [in (vec (range 10000))
          out (gvmap identity in)]
      (is (= out in))))
  (testing "can we map the identity fn across a large vector using the gpu ?"
    (let [in (vec (range 100000))
          out (gvmap identity in)]
      (is (= out in))))
  (testing "can we map the identity fn across a large vector using the gpu ?"
    (let [in (vec (range 1000000))
          out (gvmap identity in)]
      (is (= out in)))))

;;------------------------------------------------------------------------------

;; TODO: get this all working in repl and time gvmap, gvmap2 and
;; gvmap3 with different sizes of vector...

(println "TESTING gvmapN")

(def data
  (mapv
   (fn [i] (vec (range i)))
   [
    ;;0 
    ;;1 31
    ;;;32
    33 (+ 32 31)
    1024
    1025 (+ 1025 31)
;;    (* 32 32 32) (+ (* 32 32 32) 31)
;;    (* 32 32 32 32) (+ (* 32 32 32 32) 31)
    ]))

(defn average-time [iters foo]
  (let [start (System/currentTimeMillis)]
    (dotimes [_ iters] (foo))
    (- (System/currentTimeMillis) start)))

(doseq [datum data]
  (println "mapv  :" (count datum) "items -" (average-time 100 #(mapv  identity datum)) "ms")
  (println "gvmap :" (count datum) "items -" (average-time 100 #(gvmap  identity datum)) "ms")
;  (println "gvmap2:" (count datum) "items -" (average-time 100 #(gvmap2 identity datum)) "ms")
  (println "gvmap3:" (count datum) "items -" (average-time 100 #(gvmap3 identity datum)) "ms")
  )

;; TODO: select one gvmap and migrate to vector.clj ?
