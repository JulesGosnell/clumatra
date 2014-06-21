(ns clumatra.okra
  (:import
   [java.lang.reflect Method]
   [com.oracle.graal.hsail HSAIL]
   [com.oracle.graal.compiler.common GraalOptions]
   [com.oracle.graal.options OptionValue]
   [com.oracle.graal.hotspot HotSpotGraalRuntime]
   [com.oracle.graal.hotspot.hsail HSAILHotSpotBackend]
   [com.amd.okra OkraContext OkraKernel])
  (:require
;;   [no [disassemble :as d]]
   [clumatra [util]]))

;; bottom-up approach to enabling GPU to be used to map a function across a seqence

;;------------------------------------------------------------------------------
;; TODO:
;; can we reuse the OkraContext - investigate
;; can we reuse a OkraKernel - investigate
;; can we use the same OkraKernel concurrently on multiple GPU cores ? - investigate
;;------------------------------------------------------------------------------

(defn okra-kernel-compile-orig [kernel ^Method method num-inputs num-outputs]
  (let [verbose (if (System/getProperty "clumatra.verbose") true false)
        okra-context (doto (OkraContext.) (.setVerbose (true? verbose)))]
    (with-open [a (OptionValue/override (GraalOptions/InlineEverything) true)
                b (OptionValue/override (GraalOptions/PrintCompilation) verbose)
                c (OptionValue/override (GraalOptions/PrintProfilingInformation) verbose)
                ;; d (OptionValue/override (GraalOptions/RemoveNeverExecutedCode) true)
                ]
      (if verbose (println "OKRA:" (if (OkraContext/isSimulator) "SIMULATED" "NATIVE")))
      ;;(if verbose (println (d/disassemble kernel)))
      (let [^HSAILHotSpotBackend backend (.getBackend (HotSpotGraalRuntime/runtime) HSAIL)
            code-string (.getCodeString
                         (.compileKernel
                          backend
                          (.lookupJavaMethod (.getMetaAccess (.getProviders backend)) method)
                          false))]
        (fn [n & args]
          ;; TODO: I am assuming that we need a fresh Kernel for each
          ;; concurrent GPU core...
          (doto (OkraKernel. okra-context code-string "&run")
            (.setLaunchAttributes n)
            (.dispatchWithArgs (object-array (cons kernel args))))
          ;;(.dispose okra-context)
          (last args))))))

(def okra-kernel-compile (memoize okra-kernel-compile-orig))

;;------------------------------------------------------------------------------
