(defproject seqspert "0.1.0-SNAPSHOT"
  :description "Seqspert: Understand the internals of Clojure sequence implementations"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.6.0-beta2"]
                 ;;[org.clojure/clojure "1.6.0-master-SNAPSHOT"]
                 [org.clojure/core.rrb-vector "0.0.10"]
                 [com.amd/okra-with-sim "1.8"]
                 ]
  :jvm-opts ["-Xms1g" "-Xmx1g" "-server" "-XX:+GPUOffload" "-XX:+TraceGPUInteraction"]
  )
