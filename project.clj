(defproject clumatra "0.1.0-SNAPSHOT"
  
  :description "Clumatra: Clojure for the GPU via Sumatra / Graal / Okra"
  
  :url "http://git@github.com:JulesGosnell/clumatra"
  
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [
                 [org.clojure/clojure "1.7.0-alpha2"]
                 [org.clojure/core.rrb-vector "0.0.11"]
                 [com.amd/okra "1.9"]
                 ]
  
  :plugins [[lein-nodisassemble "0.1.3"]]
  
  :repositories [["ouroboros" "http://ouroboros.dyndns-free.com/artifactory/repo"]]
  
  :global-vars {*warn-on-reflection* true
                *assert* false
                *unchecked-math* true}
  
  :jvm-opts [
             "-server"
             "-Xms1g"
             "-Xmx8g"
             ;;"-Dclumatra.verbose=true"
             "-G:Log=CodeGen"
             "-XX:-UseHSAILDeoptimization"
             "-XX:-UseHSAILSafepoints"
             "-XX:+GPUOffload"
             "-XX:+TraceGPUInteraction"
             "-XX:-UseGraalClassLoader"
             ]
  )
