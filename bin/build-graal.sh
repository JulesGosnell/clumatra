#!/bin/bash -x

## distilled from: https://wiki.openjdk.java.net/display/Sumatra/Sumatra+JDK+build+instructions

## as yourself:

mkdir graal
cd graal

## build sumatra/jdk8
export LANG=C
export PATH=/usr/lib/jvm/java-openjdk/bin:$PATH
hg clone http://hg.openjdk.java.net/sumatra/sumatra-dev
cd sumatra-dev
bash ./get_source.sh 
bash ./configure
make all
cd ..

## build graal and install in sumatra-dev tree
export JAVA_HOME=`pwd`/sumatra-dev/build/linux-x86_64-normal-server-release/images/j2sdk-image/
hg clone http://hg.openjdk.java.net/graal/graal
cd graal
./mx.sh --vmbuild product --vm server build

## test
./mx.sh --vm server unittest -XX:+TraceGPUInteraction -XX:+GPUOffload -G:Log=CodeGen hsail.test.IntAddTest

## Note you must use the extra option -XX:+GPUOffload to enable offloading and
## use -XX:+TraceGPUInteraction to see extra messages about GPU initialization etc.

cd ../..


