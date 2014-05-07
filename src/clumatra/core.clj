(ns clumatra.core
  (:import [java.lang.reflect Method])
  (:require [clojure.test :refer :all] 
            [clumatra core [util :as u]])
  )
            
(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------

(defn local-kernel-compile [kernel ^Method method n]
  (fn [& args]
    (doseq [^Long i (range n)]
      (.invoke method kernel (into-array Object (concat args (list (int i))))))
    (last args)))

(println)
(u/compile-if
 (Class/forName "com.amd.okra.OkraContext")
 (do
   ;; looks like okra is available :-)
   (println "*** LOADING WITH OKRA ***")
   (use '(clumatra okra))
   (def okra-kernel-compile kernel-compile2)
   )
 (do
   ;; we must be on my i386 laptop :-(
   (println "*** LOADING WITHOUT OKRA ***")
    
   (defn find-method [object ^String name]
     (first (filter (fn [^Method method] (= (.getName method) "invoke")) (.getMethods (class object)))))
    
   (def okra-kernel-compile local-kernel-compile)
   ))
(println)
