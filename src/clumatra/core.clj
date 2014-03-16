(ns clumatra.core
  (:import
   [java.lang.reflect Method]
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

(defn kernel-compile2 [kernel ^Method method n]
  (with-open [_ (OptionValue/override (GraalOptions/InlineEverything) true)
              _ (OptionValue/override (GraalOptions/PrintCompilation) true)
              _ (OptionValue/override (GraalOptions/PrintProfilingInformation) false)
              ;; _ (OptionValue/override (GraalOptions/RemoveNeverExecutedCode) true)
              ]
    (let [^HSAILHotSpotBackend backend (.getBackend (HotSpotGraalRuntime/runtime) HSAIL)
          okra (OkraKernel.
                (doto (OkraContext.) (.setVerbose false))
                (.getCodeString
                 (.compileKernel
                  backend
                  (.lookupJavaMethod 
                   (.getMetaAccess (.getProviders backend))
                   method)
                  false))
                "&run")]
      (fn [in out]
        (.setLaunchAttributes okra n)
        (.dispatchWithArgs okra (into-array Object [kernel in out]))
        out))))

;;------------------------------------------------------------------------------

(definterface Kernel (^void invoke [^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i])) ; grid must be last param

(def Objects (class (into-array Object [])))
(def kernel-method-param-types (into-array ^Class [Objects Objects (Integer/TYPE)]))

(defn kernel-compile [function n]
  (let [kernel (reify Kernel
     (^void invoke [^Kernel self ^"[Ljava.lang.Object;" in ^"[Ljava.lang.Object;" out ^int i]
       (aset out i
             ;;(foo
             (aget in i)
             ;;)
             )))]
    (kernel-compile2 kernel (.getDeclaredMethod (class kernel) "invoke" kernel-method-param-types) n)))
  

;;------------------------------------------------------------------------------
