#!/bin/bash -x

## distilled from https://wiki.openjdk.java.net/display/Graal/Instructions

for arg in "$@"
do
    case "$arg" in
        --refresh)
            rm -fr sumatra
            wget -nv -O- http://ouroboros.dyndns-free.com/clumatra/sumatra.tgz | tar zxf -
            ;;
    esac
done

rm -rf graal jdk1.8.0-graal && \
hg clone http://hg.openjdk.java.net/graal/graal && \
export JAVA_HOME=`pwd`/sumatra && \
export EXTRA_JAVA_HOMES=/usr/local/java/jdk1.7.0_75 && \
cd graal && \
./mx.sh --vmbuild product --vm server build && \
cd .. && \
mv graal/jdk1.8.0-internal/product jdk1.8.0-graal
