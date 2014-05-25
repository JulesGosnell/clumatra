#!/bin/bash -x

## distilled from: https://wiki.openjdk.java.net/display/Sumatra/Sumatra+JDK+build+instructions

export LANG=C &&\
export PATH=/usr/lib/jvm/java-openjdk/bin:$PATH &&\
mkdir graal &&\
cd graal &&\
hg clone http://hg.openjdk.java.net/sumatra/sumatra-dev &&\
cd sumatra-dev &&\
bash ./get_source.sh  &&\
bash ./configure &&\
make all &&\
cd .. &&\
export JAVA_HOME=`pwd`/sumatra-dev/build/linux-x86_64-normal-server-release/images/j2sdk-image/ &&\
export EXTRA_JAVA_HOMES=/etc/alternatives/java_sdk_1.7.0 &&\
hg clone http://hg.openjdk.java.net/graal/graal &&\
cd graal &&\
./mx.sh --vmbuild product --vm server build &&\
./mx.sh --vm server unittest -XX:+TraceGPUInteraction -XX:+GPUOffload -G:Log=CodeGen hsail.test.IntAddTest
