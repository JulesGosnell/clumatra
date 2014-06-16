#!/bin/bash
export JAVA_HOME=`pwd`/jdk1.8.0-graal
export LD_LIBRARY_PATH=`pwd`/lib:$LD_LIBRARY_PATH
export JAVACMD=$JAVA_HOME/bin/java
export PATH=$JAVA_HOME/bin:`pwd`/lib:$PATH
export MAVEN_OPTS="-Xms1g -Xmx1g -server -XX:+GPUOffload -XX:+TraceGPUInteraction"
$@
