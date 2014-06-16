(ns clumatra.vector-test
  (:import
   [java.util Collection Map]
   [clojure.lang
    IObj AFn ISeq IPersistentVector APersistentVector PersistentVector PersistentVector$Node
    Associative PersistentVector$TransientVector ITransientCollection])
  (:require
   [clojure [pprint :as p]]
   [clojure.core
    [reducers :as r]
    [rrb-vector :as v]])
  (:use
   [clojure test]
   [clumatra util core vector test-util]))

;;------------------------------------------------------------------------------
;; TODO:
;; look at core reducer
;; reuse fork-join for two backends
;; consider reworking what we have to integrate better
;; write some tests that make use of Map splicing
;; consider moving reduce/combine to seqspert, with an optional clumatra binding

;; (def a (into [] (range 10000000)))
;; (time (do (into [] a) nil)) ;; 124
;; (time (do (mapv identity a) nil)) ;; 220
;; (time (do (vmap identity a) nil)) ;; 140
;; (time (do (mapv inc a) nil)) ;; 280
;; (time (do (vmap inc a) nil)) ;; 240

;; ;; vmap should win when used in parallel mode because of the zero cost
;; ;; of concatenation. - but current reduction code not suitable

;; (def n (/ (count a) (.availableProcessors (Runtime/getRuntime)))) ;; = 625000

;; (do (time (r/fold (r/monoid into vector) conj a)) nil) ;; 380 ms
;; (do (time (r/fold (r/monoid v/catvec v/vector) conj a)) nil) ;; 380 ms

;; (do (time (r/fold (r/monoid into vector) conj (r/map inc a))) nil) ;; 620 ms
;; (do (time (r/fold (r/monoid v/catvec v/vector) conj (r/map inc a))) nil) ;; 680 ms

;; (do (time (r/fold n (r/monoid into vector) conj (r/map inc a))) nil) ;; 590 ms
;; (do (time (r/fold n (r/monoid v/catvec v/vector) conj (r/map inc a))) nil) ;; 320 - 520 - erratic !

;; (time (count (vmap inc a))) ;; 230 ms
;; (time (count (fjvmap inc a)))
;; (= (time (r/fold n (r/monoid v/catvec v/vector) conj (r/map inc a))) (fjvmap inc a))

(deftest vector-map-test
  (testing "mapping across vector"
    (let [data (vec (range 100))
          f inc]
      (is (= (map f data) (vmap f data) (fjvmap f data) (gvmap f data))))))

(deftest gvmap-test
  (testing "can we map the identity fn across a large vector using the gpu ?"
    (let [in (vec (range 100))
          out (gvmap identity in)]
      (is (= out in)))))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    (fetch-method clojure.lang.AFn "run" [])
    (fetch-method java.lang.Object "wait" [Long/TYPE Integer/TYPE])
    (fetch-method java.lang.Object "wait" [Long/TYPE])
    (fetch-method java.lang.Object "wait" [])
    (fetch-method java.lang.Object "notify" [])
    (fetch-method java.lang.Object "notifyAll" [])
    (fetch-method java.lang.Iterable "forEach" [java.util.function.Consumer])
    (fetch-method java.util.List "replaceAll" [java.util.function.UnaryOperator])
    (fetch-method java.util.List "sort" [java.util.Comparator])
    ;; vector - unsupported 
    (fetch-method APersistentVector "removeAll" [java.util.Collection])
    (fetch-method APersistentVector "retainAll" [java.util.Collection])
    (fetch-method APersistentVector "clear" [])
    (fetch-method APersistentVector "addAll" [java.util.Collection])
    (fetch-method APersistentVector "remove" [Integer/TYPE])
    (fetch-method APersistentVector "set" [Integer/TYPE Object])
    (fetch-method APersistentVector "add" [Integer/TYPE Object])
    (fetch-method APersistentVector "addAll" [Integer/TYPE java.util.Collection])
    (fetch-method APersistentVector "add" [Object])
    (fetch-method APersistentVector "remove" [Object])

    ;; how can I avoid this
    (fetch-method PersistentVector "assocN" IPersistentVector [Integer/TYPE Object])
    (fetch-method APersistentVector "assoc" Associative [Object Object])
    (fetch-method PersistentVector "withMeta" PersistentVector [clojure.lang.IPersistentMap])
    (fetch-method PersistentVector "withMeta" IObj [clojure.lang.IPersistentMap])
    (fetch-method PersistentVector "asTransient" ITransientCollection [])
    (fetch-method PersistentVector "asTransient"  [])
    
    ;; looks like iterators do not compare well
    (fetch-method PersistentVector "iterator" [])
    (fetch-method APersistentVector "listIterator" [Integer/TYPE])
    (fetch-method APersistentVector "listIterator" [])

    ;;; NYI
    (fetch-method PersistentVector "kvreduce" [clojure.lang.IFn Object])
    (fetch-method PersistentVector "nth" [Integer/TYPE Object])
    (fetch-method APersistentVector "invoke" [Object])
    (fetch-method PersistentVector "create" [(type->array-type Object)])
    (fetch-method PersistentVector "create" [clojure.lang.ISeq])
    (fetch-method PersistentVector "assocN" [Integer/TYPE Object])
    (fetch-method APersistentVector "toArray" [(type->array-type Object)])
    (fetch-method APersistentVector "valAt" [Object])
    (fetch-method PersistentVector "arrayFor" [Integer/TYPE])
    (fetch-method PersistentVector "nth" [Integer/TYPE])
    })

(def input-fns
   {
    (fetch-method APersistentVector "assoc" [Object Object]) [identity (fn [_] 0) (fn [_] nil)]
    (fetch-method APersistentVector "compareTo" [Object]) []
    (fetch-method APersistentVector "contains" [Object]) [identity (fn [[h]] h)]
    (fetch-method APersistentVector "containsAll" [java.util.Collection]) []
    (fetch-method APersistentVector "containsKey" [Object]) [identity (fn [i] 0)]
    (fetch-method APersistentVector "entryAt" [Object]) [identity (fn [_] 0)]
    (fetch-method APersistentVector "get" [Integer/TYPE]) [identity (fn [_] 0)]
    (fetch-method APersistentVector "subList" [Integer/TYPE Integer/TYPE]) [identity (fn [_] 0) (fn [_] 1)]
    (fetch-method PersistentVector "meta" []) [(fn [v] (with-meta v {:arg "meta"}))]
    }
  )

(deftest-kernels
  (filter (fn [^java.lang.reflect.Method m] (not (contains? #{java.lang.Object clojure.lang.AFn java.lang.Iterable java.util.List java.util.Collection} (.getDeclaringClass m))))
          (extract-methods non-static? PersistentVector excluded-methods))
  (fn [i] (mapv (fn [j] (* i j)) (range 1 (inc *wavefront-size*))))
  input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.vector-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))

;; try reduction using clojure.lang.Numbers/unchecked_add(long, long)

;;------------------------------------------------------------------------------

(definterface ReductionKernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

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
        (reify ReductionKernel
          (^void invoke
            [^ReductionKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
            (aset out i (inline-areduce (aget in i) 32 f))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

;; e.g.
;;(def ^"[Ljava.lang.Object;" in (object-array (range 32)))
;;(p/pprint (macroexpand-1 '(inline-areduce in 32 +)))


;; if I can get this to work, then I have the beginnings of vector
;; reduction...

;; I would have to copy the vector leaf array pointers into an input
;; array for this kernel..

;; (deftest proto-reduction-test
;;   (testing "can we reduce an Object[][32] into an Object[] using a hardwired function"
;;     (let [width 64
;;           in (object-array (repeat width (object-array (range 32))))
;;           out (object-array (object-array width))
;;           kernel (reduction-kernel-compile +)] ;+ is not used here...
;;       (is (= (* width 496) (apply + (kernel width in out)))))))

;; (deftest reduction-kernel-test
;;   (testing "can we reduce an Object[][32] into an Object[] using a hardwired function"
;;     (let [width 64
;;           in (object-array (repeat width (object-array (range 32))))
;;           out (object-array (object-array width))
;;           kernel (reduction-kernel-compile +)]
;;       (is (= (* width 496) (apply + (kernel width in out)))))))

;; if this works then I have reduced the leaf nodes and therefore the
;; work to be done by a factor of 32...
;; how should I reduce the rest of the data... ?

;; two types of reduction ?
;; homogeneous: leaves and branches are reduced by same fn - e.g. +-ing numbers together
;; heterogeneous: leaves are reduced with F1 and branches with F2
;; also consider seed values for Fs when a single node needs reduction...

;; how about :
;; (gvreduce f1 seed vec)
;; (gvreduce f1 seed f2 seed vec)
;; e.g.
;; (gvreduce + 0 [0 1 2 3 ... 1000000])
;; (gvreduce conj #{} map-merge #{} [0 1 2 3 ... 1000000])
;; or we could think in terms of monoids - consider ...

;; plan A:
;; -- use multiple threads to walk down to one level above bottom of trie and copy leaf array addresses into single large array for gpu to reduce into similar sized Object[]
;; -- threads coould wait for this, then pick out results for their leaves and recurse back up combining results until there is only one.
;; issues:
;;  no cpu threads working whilst gpu is working 
;;  no combination can start until all leaves are reduces

;; plan B:
;; -- fork a task for each of the 1-32 subtries - each on should prepare and execute a kernel to reduce the entire subtrie.
;; -- we want to combine results as soon as they become available - do we put them on a queue or can we somehow use a lazy seq
;; issues:
;; final 32 values recombined in serial - TX32 - if we did the recombination in parallel it would take Tx5
;;; we could recurse down 5 levels off fork/join tasks, having each
;;; one spawn and wait for two children then recombine their
;;; results... - would that be too much overhead ? no for heavy fns - yes for light ones...
;; can we just divide tree by number of available processors  and only do reduction at this granularity ?
;; on a 16 core box we would then be left with a recombination cost of 16T instead of 5T...(probably 2*5T as with only 16 threads each one would have to do 2 of the 32 pieces of work),

;; would recombination always occur in the same order - I think so -
;; if every thread collapsed their rhs into their lhs then this would
;; be the same as a sequential collapse from left to right ?

;;; what about tail and the non-width-32 scraggy ends of vectors ?

;; serial reduction of vec[1024] into a set would be 1024*t where t is the time for 1 insertion
;; parallel reduction of same vec could be e.g. 32t 

;; split vec[1024] into 32 * array[32] and reduce in parallel on gpu - 32t - yielding set[32]*32
;; split these onto 16 threads where each one does set[32]+set[32] - 32t - yielding set[64]*16
;; split these onto 8 threads where each one does set[64]+set[64] - 64t -  yielding set[128]*8
;; set[128]+set[128] - 128t - yielding set[256]*4
;; set[256]+set[256] - 256t - yielding set[512]*2
;; set[512]+set[512] - 512t - yielding set[1024]
;; total time = 32t + 32t + 64t + 128t + 256t + 512t = 

;; try writing and testing a kernel to take e.g. vector|array[n] and reduce it to array[n/2]

;; need VectorReduction and ArrayReduction Kernel types
;; VectorReduction = (aset out i (f (get in (* 2 i)) (get in (inc (* 2 i)))))
;; ArrayReduction = (aset out i (f (aget in (* 2 i)) (aget in (inc (* 2 i)))))
;; arity of tree to be reduced should be weighed against weight of function to be applied: heavy fn = low arity (i.e. 2), light function (e.g. +) -> higher arity (e.g. 32 - use inline areduce)

;;; we could either use subvecs or pass in an offset value, allowing
;;; us to reduce one piece of vec on each cpu core...

;;------------------------------------------------------------------------------

(definterface ArrayReductionKernel
  (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

(definterface VectorReductionKernel
  (^void invoke [^clojure.lang.PersistentVector in ^"[Ljava.lang.Object;" out ^int i]))

;;------------------------------------------------------------------------------

(defn array-copy-kernel-compile [f]
  (let [kernel
        (reify ArrayReductionKernel
          (^void invoke
            [^ArrayReductionKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
            (aset out i (aget in i))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

(defn vector-copy-kernel-compile [f]
  (let [kernel
        (reify VectorReductionKernel
          (^void invoke
            [^VectorReductionKernel self ^clojure.lang.PersistentVector in ^"[Ljava.lang.Object;" out ^int i]
            (aset out i (.nth in i))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

(deftest vector-copy-test
  (testing "can we copy the contents of a Clojure vector of size>=32 directly into an Object[] on GPU"
    (let [w 1024
          in (vec (range w))
          kernel (vector-copy-kernel-compile nil)]
      (is (= (seq (kernel w in (object-array w))) (range w))))))

(deftest array-copy-test
  (testing "can we copy the contents of an Object[] into an Object[] on GPU"
    (let [w 1024
          in (object-array (range w))
          kernel (array-copy-kernel-compile nil)]
      (is (= (seq (kernel w in (object-array w))) (range w))))))

;------------------------------------------------------------------------------    
;; I think that I am going to have to write some Java:
;; 1. to expose private vector ctor
;; 2. to expose enough of PHM to allow splicing
;; 3. to refactor PV.nth() to a point whereby it will run on GPU

;; post about and consider use of "heterogenous queueing" i.e. cpu and
;; gpu can dispatch work on themselves and each other with similar
;; effort.

;; try translating PersistentVector.nth() into Clojure:

;; (* 32 32 32 32 32 32) -> 1073741824 > 1 Billion

;; so lets use a jump table of size 6 and some loop unrolling to avoid
;; any branching at all in the lookup of an element in a vector. this
;; will be much better for simd - might even be faster than the
;; existing clojure impl :-)


(defn foo [i l ^"[Ljava.lang.Object;" a]
  (.array ^PersistentVector$Node (aget a (bit-and (clojure.lang.Numbers/unsignedShiftRight i l) 16r01f))))

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
    (clojure.lang.Numbers/shiftLeft (clojure.lang.Numbers/unsignedShiftRight (dec l) 5) 5)))
  
(defn ^"[Ljava.lang.Object;" vector-array [^clojure.lang.PersistentVector v i]
  (if (>= i (vector-trie-count v))
    (.tail v)
    (vector-trie-array i (.shift v) (.array (.root v)))))

(defn vector-get [v i] (aget (vector-array v i) (bit-and i 0x1f)))
(defn vector-set [v i val] (aset (vector-array v i) (bit-and i 0x1f) val))

;;------------------------------------------------------------------------------

(definterface VectorToVectorKernel
  (^void invoke [^clojure.lang.PersistentVector in ^clojure.lang.PersistentVector out ^int i]))

;; inline everything to see if we can get it to work on gpu...
;; only works for vectors of shift = 10
(defn vector-to-vector-copy-kernel-compile [_]
  (let [kernel
        (reify VectorToVectorKernel
          (^void invoke
            [^VectorToVectorKernel self ^clojure.lang.PersistentVector in ^clojure.lang.PersistentVector out ^int i]
            (let [l (count in)
                  n (clojure.lang.Numbers/shiftLeft (clojure.lang.Numbers/unsignedShiftRight (dec l) 5) 5) ;trie count
                  j (bit-and i 0x1f)
                  q (bit-and (clojure.lang.Numbers/unsignedShiftRight i 5) 0x1f)
                  r (bit-and (clojure.lang.Numbers/unsignedShiftRight i 10) 0x1f)]
              (aset
               (if (>= i n)
                 (.tail out)
                 (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array (.root out)) r)) q)))
               j
               (aget
                 (if (>= i n)
                   (.tail in)
                   (.array ^PersistentVector$Node (aget (.array ^PersistentVector$Node (aget (.array (.root in)) r)) q)))
                 j)))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

(deftest vector-to-vector-copy-test
  (testing "can we perform a direct vector-to-vector (map f v) on the GPU ?"
    (let [w 10000
          in (vec (range w))
          out (empty-vector w)
          kernel (vector-to-vector-copy-kernel-compile inc)]
      (is (= (kernel w in out)
             (vec (range w)))))))

(defn vector-to-vector-mapping-kernel-compile [f]
  (let [kernel
        (reify VectorToVectorKernel
          (^void invoke
            [^VectorToVectorKernel self ^clojure.lang.PersistentVector in ^clojure.lang.PersistentVector out ^int i]
            (vector-set out i (inc (vector-get in i)))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

(defn gvmap2 [f ^PersistentVector in]
  (let [width (.count in)
        out (empty-vector width)
        kernel (vector-to-vector-mapping-kernel-compile f)]
    (kernel width in out)))

(deftest vector-to-vector-mapping-test
  (testing "can we perform a direct vector-to-vector (map f v) on the GPU ?"
    (let [w 10000
          in (vec (range w))
          out (empty-vector w)
          kernel (vector-to-vector-mapping-kernel-compile inc)]
      (is (= (kernel w in out)
             (mapv inc (range w)))))))

;;------------------------------------------------------------------------------
;; further reduction work...

(defn vector-to-array-reduction-kernel-compile [f]
  (let [kernel
        (reify VectorReductionKernel
          (^void invoke
            [^VectorReductionKernel self ^clojure.lang.PersistentVector in ^"[Ljava.lang.Object;" out ^int i]
            (let [n (* 2 i)]
              (aset out i (+ (.nth in n)(.nth in (inc n)))))))]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

;; only compare first 32 elements of result
(deftest vector-to-array-reduction-1-test
  (testing "can we perform the first stage of a reduction by '+' from a vector[n] to an Object[n/2] on GPU?"
    (let [w 1024
          half (/ w 2)
          in (vec (range w))
          kernel (vector-to-array-reduction-kernel-compile nil)]
      ;; only seems to work for first 64 elts
      (is (= (take 32 (seq (kernel half in (object-array half))))
             (take 32 (apply map + (vals (group-by even? (range w))))))))))

(deftest vector-to-array-reduction-2-test
  (testing "can we perform the first stage of a reduction by '+' from a vector[n] to an Object[n/2] on GPU?"
    (let [w 1024
          half (/ w 2)
          in (vec (range w))
          kernel (vector-to-array-reduction-kernel-compile nil)]
      (is (= (seq (kernel half in (object-array half)))
             (apply map + (vals (group-by even? (range w)))))))))

;;------------------------------------------------------------------------------

;; tidy up vector-get and provide test - time against nth
;; write vector-set and provide test

;; MapVectorToVectorKernel - for all depths
;; MapVectorToArrayKernel - for all depths
;; MapArrayToArrayKernel
;; MapArrayToVectorKernel - for all depths

;; ReduceVectorToArrayKernel - for all depths
;; ReduceArrayToArrayKernel
