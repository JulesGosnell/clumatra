#!/bin/sh -x

exec mvn -t conf/toolchains.xml -P no-acceleration clean compile clojure:test $@
