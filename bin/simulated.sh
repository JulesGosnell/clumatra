#!/bin/sh -x

exec mvn -f ci-pom.xml -t conf/toolchains.xml -P simulated-acceleration clean compile clojure:test $@
