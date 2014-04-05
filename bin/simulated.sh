#!/bin/sh -x

exec mvn -t conf/toolchains.xml -P simulated-acceleration clean compile clojure:test $@
