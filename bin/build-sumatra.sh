#!/bin/bash -x

## distilled from: https://wiki.openjdk.java.net/display/Sumatra/Sumatra+JDK+build+instructions

export LANG=C && \
export PATH=/usr/lib/jvm/java-openjdk/bin:$PATH && \
rm -rf sumatra-dev sumatra && \
hg clone http://hg.openjdk.java.net/sumatra/sumatra-dev && \
cd sumatra-dev && \
bash ./get_source.sh  && \
bash ./configure && \
make all && \
cd .. && \
mv sumatra-dev/build/linux-x86_64-normal-server-release/images/j2sdk-image sumatra
