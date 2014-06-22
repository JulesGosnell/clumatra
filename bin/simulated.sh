#!/bin/sh -x

if [ ! -d jdk1.8.0-graal ]
then
    wget -nv -O- http://ouroboros.dyndns-free.com/clumatra/jdk1.8.0-graal.tgz | tar zxf -
fi

exec mvn -t conf/toolchains.xml -P simulated-acceleration clean compile clojure:test $@
