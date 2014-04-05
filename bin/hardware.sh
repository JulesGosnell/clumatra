#!/bin/sh -x

export CLUMATRA_HOME=`pwd`
export PATH=$CLUMATRA_HOME/lib:$PATH
export LD_LIBRARY_PATH=$CLUMATRA_HOME/lib

exec mvn -t conf/toolchains.xml -P hardware-acceleration clean compile clojure:test $@

