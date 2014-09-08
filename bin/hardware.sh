#!/bin/sh -x

testgoal=test
flags=

for arg in "$@"
do
    case "$arg" in
    --junit)
	    testgoal=test-with-junit
            ;;
    --refresh)
	    rm -fr jdk1.8.0-graal lib
            ;;
    *)
	    flags="$flags $arg"
            ;;
    esac
done

if [ ! -d jdk1.8.0-graal ]
then
    wget -nv -O- http://ouroboros.dyndns-free.com/clumatra/jdk1.8.0-graal.tgz | tar zxf -
fi

if [ ! -d lib ]
then
    mkdir lib
    cd lib
    wget -nv https://raw.githubusercontent.com/HSAFoundation/Okra-Interface-to-HSA-Device/master/okra/dist/bin/libokra_x86_64.so
    wget -nv https://raw.githubusercontent.com/HSAFoundation/HSA-Runtime-AMD/master/lib/x86_64/libhsa-runtime64.so.1
    wget -nv https://raw.githubusercontent.com/HSAFoundation/HSA-Drivers-Linux-AMD/master/kfd-0.8/libhsakmt/lnx64a/libhsakmt.so.1
    cd ..
fi

export CLUMATRA_HOME=`pwd`
export PATH=$CLUMATRA_HOME/lib:$PATH
export LD_LIBRARY_PATH=$CLUMATRA_HOME/lib

exec mvn -t conf/toolchains.xml -P hardware-acceleration clean compile clojure:$testgoal $flags
