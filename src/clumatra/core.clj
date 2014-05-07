(ns clumatra.core
  (:import [java.lang.reflect Method])
  (:require [clojure.test :refer :all] 
            [clumatra [util :as u]])
  )
            
(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------

;; WARNING: this is invoked via introspection and will therefore be SLOOOOW...
(defn local-kernel-compile [kernel ^Method method n]
  (fn [& args]
    (dotimes [i n]
      (.invoke method kernel (object-array (concat args (list (int i))))))
    (last args)))

(println)
(u/compile-if
 (Class/forName "com.amd.okra.OkraContext")
 (do
   ;; looks like okra is available :-)
   (println "*** LOADING WITH OKRA ***")
   (use '(clumatra okra))
   )
 (do
   ;; we must be on my i386 laptop :-(
   (println "*** LOADING WITHOUT OKRA ***")
   (def okra-kernel-compile local-kernel-compile)
   ))
(println)
