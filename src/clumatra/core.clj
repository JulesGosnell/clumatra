(ns clumatra.core
  (:import [java.lang.reflect Method])
  (:require [clojure.test :refer :all] 
            [clumatra [util :as u]])
  )
            
(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------

;; WARNING: this is invoked via introspection and will therefore be SLOOOOW...
(defn local-kernel-compile [kernel ^Method method num-inputs num-outputs]
    (fn [n & args]
      (let [[h t] (split-at (+ num-inputs num-outputs) args)]
        ;; (println "NUM INPUTS:" num-inputs)
        ;; (println "ARGS:" args)
        ;; (println "H|T:" h t)
        (dotimes [i n]
          (let [params (concat h [(Integer. i)] t)]
            ;(println method "\n" (map type params) "\n" params)
            (.invoke method kernel (object-array params))))
      ;; out
      (nth args num-inputs))))

(println)
(u/compile-if
 (Class/forName "com.amd.okra.OkraContext")
 (do
   ;; looks like okra is available :-)
   (println "*** LOADING WITH OKRA ***")
   (require '(clumatra okra))
   (def okra-kernel-compile clumatra.okra/okra-kernel-compile)
   )
 (do
   ;; we must be on my i386 laptop :-(
   (println "*** LOADING WITHOUT OKRA ***")
   (def okra-kernel-compile local-kernel-compile)
   ))
(println)

;;------------------------------------------------------------------------------
;; general kernel for mapping a fn over a single input array...

(definterface ObjectArrayToObjectArrayKernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

(defn simple-kernel-compile [f]
  (let [kernel
        (if (= f identity)
          ;;; tmp hack to (maybe) allow testing of kernels on gpu before funcalls are working...
          (reify ObjectArrayToObjectArrayKernel
            (^void invoke [^ObjectArrayToObjectArrayKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
              (aset out i (aget in i))))
          (reify ObjectArrayToObjectArrayKernel
            (^void invoke [^ObjectArrayToObjectArrayKernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
              (aset out i (f (aget in i))))))
        ]
    (okra-kernel-compile kernel (u/fetch-method (class kernel) "invoke") 1 1)))
