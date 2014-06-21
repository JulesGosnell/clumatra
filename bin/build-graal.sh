#!/bin/bash -x

## distilled from: https://wiki.openjdk.java.net/display/Sumatra/Sumatra+JDK+build+instructions

export LANG=C && \
export PATH=/usr/lib/jvm/java-openjdk/bin:$PATH &&\
export JAVA_HOME=`pwd`/sumatra && \
export EXTRA_JAVA_HOMES=/etc/alternatives/java_sdk_1.7.0 && \
rm -rf graal jdk1.8.0-graal && \
hg clone http://hg.openjdk.java.net/graal/graal && \
cd graal && \
./mx.sh --vmbuild product --vm server build && \
./mx.sh --vm server unittest -XX:+TraceGPUInteraction -XX:+GPUOffload -G:Log=CodeGen hsail.test.IntAddTest && \
mv graal/jdk1.8.0-internal/product jdk1.8.0-graal
