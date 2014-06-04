(ns clumatra.vector-test
  (:import  [java.util Collection Map]
            [clojure.lang IObj AFn ISeq IPersistentVector APersistentVector PersistentVector Associative PersistentVector$TransientVector ITransientCollection])
  (:require [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure.test :refer :all]
            [clumatra.util :refer :all]
            [clumatra.core :refer :all]
            [clumatra.vector :refer :all]
            [clumatra.test-util :refer :all]))

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

(defn reduction-kernel-compile [f]
  (let [kernel
        (reify ReductionKernel
          (^void invoke
            [^ReductionKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
            (aset
             out
             i
             (let [^"[Ljava.lang.Object;" a (aget in i)]
               (->>
                0
                (clojure.lang.Numbers/unchecked_add (long (aget a 0)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 1)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 2)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 3)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 4)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 5)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 6)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 7)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 8)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 9)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 10)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 11)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 12)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 13)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 14)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 15)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 16)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 17)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 18)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 19)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 20)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 21)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 22)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 23)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 24)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 25)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 26)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 27)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 28)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 29)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 30)))
                (clojure.lang.Numbers/unchecked_add (long (aget a 31)))
                )
               ;; (clojure.lang.Numbers/unchecked_add
               ;;  (long (aget a 0))
               ;;  (clojure.lang.Numbers/unchecked_add
               ;;   (long (aget a 1))
               ;;   (clojure.lang.Numbers/unchecked_add
               ;;    (long (aget a 2))
               ;;    (clojure.lang.Numbers/unchecked_add
               ;;     (long (aget a 3))
               ;;     (clojure.lang.Numbers/unchecked_add
               ;;      (long (aget a 4))
               ;;      (clojure.lang.Numbers/unchecked_add
               ;;       (long (aget a 5))
               ;;       (clojure.lang.Numbers/unchecked_add
               ;;        (long (aget a 6))
               ;;        (clojure.lang.Numbers/unchecked_add
               ;;         (long (aget a 7))
               ;;         (clojure.lang.Numbers/unchecked_add
               ;;          (long (aget a 8))
               ;;          (clojure.lang.Numbers/unchecked_add
               ;;           (long (aget a 9))
               ;;           (clojure.lang.Numbers/unchecked_add
               ;;            (long (aget a 10))
               ;;            (clojure.lang.Numbers/unchecked_add
               ;;             (long (aget a 11))
               ;;             (clojure.lang.Numbers/unchecked_add
               ;;              (long (aget a 12))
               ;;              (clojure.lang.Numbers/unchecked_add
               ;;               (long (aget a 13))
               ;;               (clojure.lang.Numbers/unchecked_add
               ;;                (long (aget a 14))
               ;;                (clojure.lang.Numbers/unchecked_add
               ;;                 (long (aget a 15))
               ;;                 (clojure.lang.Numbers/unchecked_add
               ;;                  (long (aget a 16))
               ;;                  (clojure.lang.Numbers/unchecked_add
               ;;                   (long (aget a 17))
               ;;                   (clojure.lang.Numbers/unchecked_add
               ;;                    (long (aget a 18))
               ;;                    (clojure.lang.Numbers/unchecked_add
               ;;                     (long (aget a 19))
               ;;                     (clojure.lang.Numbers/unchecked_add
               ;;                      (long (aget a 20))
               ;;                      (clojure.lang.Numbers/unchecked_add
               ;;                       (long (aget a 21))
               ;;                       (clojure.lang.Numbers/unchecked_add
               ;;                        (long (aget a 22))
               ;;                        (clojure.lang.Numbers/unchecked_add
               ;;                         (long (aget a 23))
               ;;                         (clojure.lang.Numbers/unchecked_add
               ;;                          (long (aget a 24))
               ;;                          (clojure.lang.Numbers/unchecked_add
               ;;                           (long (aget a 25))
               ;;                           (clojure.lang.Numbers/unchecked_add
               ;;                            (long (aget a 26))
               ;;                            (clojure.lang.Numbers/unchecked_add
               ;;                             (long (aget a 27))
               ;;                             (clojure.lang.Numbers/unchecked_add
               ;;                              (long (aget a 28))
               ;;                              (clojure.lang.Numbers/unchecked_add
               ;;                               (long (aget a 29))
               ;;                               (clojure.lang.Numbers/unchecked_add
               ;;                                (long (aget a 30))
               ;;                                (long (aget a 31)))))))))))))))))))))))))))))))))
               ))))
        ]
    (okra-kernel-compile kernel (fetch-method (class kernel) "invoke") 1 1)))

;; if I can get this to work, then I have the beginnings of vector
;; reduction...

;; I would have to copy the vector leaf array pointers into an input
;; array for this kernel..

(deftest proto-reduction-test
  (testing "can we reduce an Object[][32] into an Object[] using a hardwired function"
    (let [width 64
          in (object-array (repeat width (object-array (range 32))))
          out (object-array (object-array width))
          kernel (reduction-kernel-compile +)] ;+ is not used here...
      (is (= (* width 496) (apply + (kernel width in out)))))))

;; if this works then I have reduced the leaf nodes and therefore the
;; work to be done by a factor of 32...
;; how should I reduce the rest of the data... ?
