(ns clumatra.stream
  (:import
   [com.oracle.graal.hsail HSAIL]
   [com.oracle.graal.phases GraalOptions]
   [com.oracle.graal.options OptionValue]
   [com.oracle.graal.hotspot HotSpotGraalRuntime]
   [com.oracle.graal.hotspot.hsail HSAILHotSpotBackend]
   [com.amd.okra OkraContext OkraKernel]
   ))

;; java8 stuff:

;;; create a functional interface
(definterface ^{java.lang.FunctionalInterface true} I (^long foo [^long bar]))
(.isAnnotationPresent I java.lang.FunctionalInterface)
(.getDeclaredAnnotations I)

;; instantiate and instance of it...
(reify I (^long foo [self ^long bar] bar))

;; create a 'stream' and apply a function IN PARALLEL to it...
(into
 []
 (.toArray
  (.sequential
   (.mapToLong
    (.parallel (.stream (java.util.ArrayList. [1 2 3 4 5])))
    (reify java.util.function.ToLongFunction (applyAsLong [self l] (* 2 l)))))))

;; now, what about Sumatra, Graal, HSAIL etc...
;;http://developer.amd.com/community/blog/2013/06/17/hsail-based-gpu-offload-the-quest-for-java-performance-begins/


;; https://wiki.openjdk.java.net/display/Sumatra/The+HSAIL+Simulator


;; is it worth building and playing with Graal/Sumatra ?

;; https://wiki.openjdk.java.net/display/Sumatra/AMD+Sumatra+prototype%3A+APU+meets+Stream+API

;; What h/w do I need to use it ?

;; go back to just seeing how fast we can get entries into a map...

(def data (into [] (range 1000000)))

(time (def result (reduce (fn [r x] (assoc r x x)) {} data)))
(time (def result (persistent! (reduce (fn [r x] (assoc! r x x)) (transient {}) data))))

;; can we go faster than the above...

