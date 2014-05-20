(defproject clumatra "0.1.0-SNAPSHOT"
  
  :description "Clumatra: Clojure for the GPU via Sumatra / Graal / Okra"
  
  :url "http://git@github.com:JulesGosnell/clumatra"
  
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 ;;[org.clojure/clojure "1.6.0-master-SNAPSHOT"]
                 [org.clojure/core.rrb-vector "0.0.11"]
                 [com.amd/okra "1.8"]
                 ]
  
  :plugins [[lein-nodisassemble "0.1.3"]]
  
  :repositories [["ouroboros" "http://ouroboros.dyndns-free.com/artifactory/repo"]]
  
  :global-vars {*warn-on-reflection* true
                *assert* false
                *unchecked-math* true}
  
  :jvm-opts ["-Xms1g" "-Xmx8g" "-server"
             ;;"-Dclumatra.verbose=true"
             "-Djava.library.path=/home/jules/workspace/clumatra/lib"
             "-XX:-UseHSAILDeoptimization" "-XX:-UseHSAILSafepoints" "-G:Log=CodeGen" "-XX:+GPUOffload" "-XX:+TraceGPUInteraction"
             ]
  )
