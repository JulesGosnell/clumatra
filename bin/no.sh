#!/bin/sh -x

testgoal=test

for arg in "$@"
do
    case "$arg" in
    --junit)
	    testgoal=test-with-junit
            ;;
    *)
	    flags="$flags $arg"
            ;;
    esac
done

exec mvn -t conf/toolchains.xml -P no-acceleration clean compile clojure:$testgoal $flags
