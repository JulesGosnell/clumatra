;; bottom-up approach to enabling GPU to be used to map a function across a seqence

(ns clumatra.core
  (:import
   [java.lang.reflect Method]
   [com.oracle.graal.hsail HSAIL]
   [com.oracle.graal.phases GraalOptions]
   [com.oracle.graal.options OptionValue]
   [com.oracle.graal.hotspot HotSpotGraalRuntime]
   [com.oracle.graal.hotspot.hsail HSAILHotSpotBackend]
   [com.amd.okra OkraContext OkraKernel])
  (:require
   [no [disassemble :as d]])
  )
            
;;------------------------------------------------------------------------------
;; we should close the OkraContext - but we can't do that until we
;; have finished using the kernel...
;;------------------------------------------------------------------------------

(defn kernel-compile2 [kernel ^Method method n]
  (let [verbose (if (System/getProperty "clumatra.verbose") true false)
        okra-context (doto (OkraContext.) (.setVerbose (true? verbose)))]
    (with-open [a (OptionValue/override (GraalOptions/InlineEverything) true)
                b (OptionValue/override (GraalOptions/PrintCompilation) verbose)
                c (OptionValue/override (GraalOptions/PrintProfilingInformation) verbose)
                ;; d (OptionValue/override (GraalOptions/RemoveNeverExecutedCode) true)
                ]
      (if verbose (println "OKRA:" (if (OkraContext/isSimulator) "SIMULATED" "NATIVE")))
      (if verbose (println (d/disassemble kernel)))
      (let [^HSAILHotSpotBackend backend (.getBackend (HotSpotGraalRuntime/runtime) HSAIL)
            okra-kernel (OkraKernel.
                         okra-context
                         (.getCodeString
                          (.compileKernel
                           backend
                           (.lookupJavaMethod (.getMetaAccess (.getProviders backend)) method)
                           false))
                         "&run")]
      (fn [& args]
        (.setLaunchAttributes okra-kernel n)
        (.dispatchWithArgs okra-kernel (into-array Object (conj args kernel)))
        (last args))))))

;;------------------------------------------------------------------------------

(defn find-method [object ^String name]
  (first (filter (fn [^Method method] (= (.getName method) "invoke")) (.getDeclaredMethods (class object)))))

;; consider using gen-interface and pushing this code into kernel-compile

(definterface Kernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]))

(defn kernel-compile [function n]
  (let [kernel (reify Kernel
                 (^void invoke [^Kernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
                   (aset out i
                         ;;(foo
                         (aget in i)
                         ;;)
                         )))]
    (kernel-compile2 kernel (find-method kernel "invoke") n)))
  

;;------------------------------------------------------------------------------
