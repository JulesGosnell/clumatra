#!/bin/bash
export JAVA_HOME=`pwd`/jdk1.8.0-graal/
export JAVACMD=$JAVA_HOME/bin/java
export PATH=$JAVA_HOME/bin:$PATH
export MAVEN_OPTS="-Xms1g -Xmx1g -server -XX:+GPUOffload -XX:+TraceGPUInteraction"
$@
