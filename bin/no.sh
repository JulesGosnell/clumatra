#!/bin/sh -x

testgoal=test

for arg in "$@"
do
    case "$arg" in
    --junit)
	    testgoal=test-with-junit
            ;;
    --refresh)
	    rm -fr jdk1.8.0-graal
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

exec mvn -t conf/toolchains.xml -P no-acceleration clean compile clojure:$testgoal $flags
