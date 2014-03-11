(ns clumatra.core
  (:import
   [com.oracle.graal.hsail HSAIL]
   [com.oracle.graal.phases GraalOptions]
   [com.oracle.graal.options OptionValue]
   [com.oracle.graal.hotspot HotSpotGraalRuntime]
   [com.oracle.graal.hotspot.hsail HSAILHotSpotBackend]
   [com.amd.okra OkraContext OkraKernel]
   )
  ;;;(:use [clumatra.dummy OptionValue GraalOptions])
  )

;;------------------------------------------------------------------------------

(System/getProperty "java.version")

(set! *warn-on-reflection* true)

;;------------------------------------------------------------------------------
;; we should close the OkraContext - but we can't do that until we
;; have finished using the kernel...
;;------------------------------------------------------------------------------

(definterface Kernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i])) ; grid must be last param

(def Objects (class (into-array Object [])))
(def kernel-method-param-types (into-array ^Class [Objects Objects (Integer/TYPE)]))

;;------------------------------------------------------------------------------

(defn kernel-compile [foo n]
  (with-open [_ (OptionValue/override (GraalOptions/InlineEverything) true)
              _ (OptionValue/override (GraalOptions/PrintCompilation) true)
              _ (OptionValue/override (GraalOptions/PrintProfilingInformation) false)
              ;; _ (OptionValue/override (GraalOptions/RemoveNeverExecutedCode) true)
              ]
    (let [^HSAILHotSpotBackend backend (.getBackend (HotSpotGraalRuntime/runtime) HSAIL)
          kernel (reify Kernel
                   (^void invoke [^Kernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
                     (aset out i
                           ;;(foo
                           (aget in i)
                           ;;)
                           )))
          okra (OkraKernel.
                (doto (OkraContext.) (.setVerbose false))
                (.getCodeString
                 (.compileKernel
                  backend
                  (.lookupJavaMethod 
                   (.getMetaAccess (.getProviders backend))
                   (.getDeclaredMethod (class kernel) "invoke" kernel-method-param-types))
                  false))
                "&run")]
      (fn [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out]
        (.setLaunchAttributes okra n)
        (.dispatchWithArgs okra (into-array Object [kernel in out]))
        out))))

;;------------------------------------------------------------------------------
