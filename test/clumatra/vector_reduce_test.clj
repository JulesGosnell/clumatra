(ns clumatra.vector-reduce-test
  (:import
   [java.util Collection Map]
   [clojure.lang
    IObj AFn ISeq IPersistentVector APersistentVector PersistentVector PersistentVector$Node
    Associative PersistentVector$TransientVector ITransientCollection Numbers]
   [clumatra.core ObjectArrayToObjectArrayKernel]
   [clumatra.vector VectorToObjectArrayKernel]
   )
  (:require
   [clojure [pprint :as p]]
   [clojure.core
    [reducers :as r]
    [rrb-vector :as v]])
  (:use
   [clojure test]
   [clumatra util core vector test-util]))

;;------------------------------------------------------------------------------
;; f() = the reducing function
;; m() = its monoid (optional)

;; stage 1: out[n] = f( m( in[n*2]), in[(n*2) + 1])
;; stage 2...n: out[n] = f(in[n*2], in[(n*2)+1])

;; naively:

;; we could run each stage in a fresh kernel dispatch using the output
;; of the first as the input of the next...

;; or:

;; can we produce some sort of macro that can generate inlined code to
;; do all of this in a single kernel down to a wavefront size of
;; e.g. 64 or 256 ? then finish off on the cpu ?

;; it probably becomes the threaded application of areduce ?

;; it would have to contains a few branches to deal with the case
;; where subarrays were not full - would this reduce the efficacy of
;; this idea ? think about it...

;; ------------------------------------------------------------------------------
;; perhaps we could reuse the following in some way ? - consider...
;;------------------------------------------------------------------------------
;; consider using a macro to produce kernel code for single and
;; multiple level reductions - unroll its loop to reduce branching on
;; SIMD:

;; This relies on f being both monoid and taking >0 args - e.g. +

;; I could provide a macro to handle non-monoids, but this could
;; complicate the whole Kernel i/f - so lets see how things go...
(defmacro inline-areduce
  [a n f]
  (let [A (gensym "a")
        F (gensym "f")]
    `(let [~A ~a ~F ~f]
       (->>
      ~@(map (fn [i#] `(~F (aget ^"[Ljava.lang.Object;" ~A ~i#))) (range n))
      ))
    ))

(defn reduction-kernel-compile [f]
  (let [kernel
        (reify ObjectArrayToObjectArrayKernel
          (^void invoke
            [^ObjectArrayToObjectArrayKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
            (aset out i (inline-areduce (aget in i) 32 f))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

;; e.g.
;;(def ^"[Ljava.lang.Object;" in (object-array (range 32)))
;;(p/pprint (macroexpand-1 '(inline-areduce in 32 +)))

;;;------------------------------------------------------------------------------

(defn vector-to-array-reduction-kernel-compile [f]
  (let [kernel
        (reify VectorToObjectArrayKernel
          (^void invoke
            [^VectorToObjectArrayKernel self ^clojure.lang.PersistentVector in ^"[Ljava.lang.Object;" out ^int i]
            (let [n (* 2 i)]
              (aset out i (+ (.nth in n)(.nth in (inc n)))))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

;; only compare first 32 elements of result
;; (deftest vector-to-array-reduction-1-test
;;   (testing "can we perform the first stage of a reduction by '+' from a vector[n] to an Object[n/2] on GPU?"
;;     (let [w 1024
;;           half (/ w 2)
;;           in (vec (range w))
;;           kernel (vector-to-array-reduction-kernel-compile nil)]
;;       ;; only seems to work for first 64 elts
;;       (is (= (take 32 (seq (kernel half in (object-array half))))
;;              (take 32 (apply map + (vals (group-by even? (range w))))))))))

;; (deftest vector-to-array-reduction-2-test
;;   (testing "can we perform the first stage of a reduction by '+' from a vector[n] to an Object[n/2] on GPU?"
;;     (let [w 1024
;;           half (/ w 2)
;;           in (vec (range w))
;;           kernel (vector-to-array-reduction-kernel-compile nil)]
;;       (is (= (seq (kernel half in (object-array half)))
;;              (apply map + (vals (group-by even? (range w)))))))))

