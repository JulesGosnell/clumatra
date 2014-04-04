#!/bin/sh -x

exec mvn -f ci-pom.xml -t conf/toolchains.xml -P no-acceleration clean compile clojure:test $@
