#!/bin/sh -x

rm -fr jdk1.8.0-graal lib

case "$1" in
    no-acceleration)
	;;
    simulated-acceleration)
	wget -nv -O- http://ouroboros.dyndns-free.com/clumatra/jdk1.8.0-graal.tgz | tar zxf -
	;;
    hardware-acceleration)
	wget -nv -O- http://ouroboros.dyndns-free.com/clumatra/jdk1.8.0-graal.tgz | tar zxf -
	mkdir lib
	cd lib
	wget -nv https://raw.githubusercontent.com/HSAFoundation/Okra-Interface-to-HSA-Device/master/okra/dist/bin/libamdhsacl64.so
	wget -nv https://raw.githubusercontent.com/HSAFoundation/Okra-Interface-to-HSA-Device/master/okra/dist/bin/libnewhsacore64.so
	wget -nv https://raw.githubusercontent.com/HSAFoundation/Okra-Interface-to-HSA-Device/master/okra/dist/bin/libokra_x86_64.so
	cd ..
	;;
esac


