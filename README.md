# Clumatra

Experiments with Clojure / Sumatra / Graal and, ultimately, Clojure compiled onto GPGPU...

For Fedora 20 / x86_64 - see https://wiki.openjdk.java.net/display/Sumatra/Sumatra+JDK+build+instructions for more detail / alternative architectures

sudo yum install java-1.7.0-openjdk mercurial freetype-devel cups-devel gcc gcc-c++ p7zip p7zip-plugins ccache libstdc++-static maven
sudo yum-builddep java-1.7.0-openjdk

Build yourself a graal-enabled jdk8 on which to run Clumatra

This may take 30 mins or so and will create a large subdir called 'graal'

<pre>
./bin/build-graal.sh
</pre>

Install the okra jar in your local maven / lein repository

<pre>
mvn install:install-file -DgroupId=com.amd -DartifactId=okra -Dversion=1.8 -Dpackaging=jar -Dfile=./graal/graal/lib/okra-1.8.jar
mvn install:install-file -DgroupId=com.amd -DartifactId=okra-with-sim -Dversion=1.8 -Dpackaging=jar -Dfile=./graal/graal/lib/okra-1.8-with-sim.jar
</pre>

You'll need lein - build and run Clumatra tests on graal-enabled jdk8

<pre>
./bin/env.sh lein test
</pre>

The output should look something like this:

<pre>
[HSAIL] library is libokra_x86_64.so
[HSAIL] using _OKRA_SIM_LIB_PATH_=/tmp/okraresource.dir_4110488871006839608/libokra_x86_64.so
[GPU] registered initialization of Okra (total initialized: 1)
Profiling info for clumatra.core$kernel_compile$reify__361.invoke(Object[], Object[], int)
  canBeStaticallyBound: false
Profiling info for clojure.lang.RT.intCast(int)
  canBeStaticallyBound: true
Profiling info for clojure.lang.RT.intCast(int)
  canBeStaticallyBound: true
Profiling info for clojure.lang.RT.aget(Object[], int)
  canBeStaticallyBound: true
Profiling info for clojure.lang.RT.aset(Object[], int, Object)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.hotspot.replacements.CheckCastDynamicSnippets.checkcastDynamic(Word, Object)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.replacements.SnippetCounter.inc()
  canBeStaticallyBound: false
Profiling info for com.oracle.graal.hotspot.replacements.TypeCheckSnippetUtils.checkUnknownSubType(Word, Word)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.hotspot.replacements.TypeCheckSnippetUtils.checkSelfAndSupers(Word, Word)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.hotspot.replacements.TypeCheckSnippetUtils.loadSecondarySupersElement(Word, int)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.verifyOop(Object)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.nodes.type.StampFactory.forNodeIntrinsic()
  canBeStaticallyBound: true
Fixed Hsail is
==============
version 0:95: $full : $large;	
// instance method HotSpotMethod<core$kernel_compile$reify__361.invoke(Object[], Object[], int)>	
kernel &run (	
	align 8 kernarg_u64 %_this,
	align 8 kernarg_u64 %_arg1,
	align 8 kernarg_u64 %_arg2
	) {
	ld_kernarg_u64  $d0, [%_this];
	ld_kernarg_u64  $d1, [%_arg1];
	ld_kernarg_u64  $d2, [%_arg2];
	workitemabsid_u32 $s0, 0;
	                                   
@L0:
	cmp_eq_b1_u64 $c0, $d2, 0; // null test 
	cbr $c0, @L1;
@L2:
	ld_global_s32 $s1, [$d2 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L27;
@L3:
	ld_global_s32 $s1, [$d2 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L26;
@L4:
	cmp_eq_b1_u64 $c0, $d1, 0; // null test 
	cbr $c0, @L5;
@L6:
	ld_global_s32 $s1, [$d1 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L25;
@L7:
	ld_global_s32 $s1, [$d1 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L24;
@L8:
	cvt_s64_s32 $d0, $s0;
	mul_s64 $d0, $d0, 4;
	add_u64 $d1, $d1, $d0;
	ld_global_u32 $d0, [$d1 + 16];
	cmp_eq_b1_u64 $c0, $d0, 0; // null test 
	cbr $c0, @L19;
@L10:
	ld_global_u32 $d1, [$d0 + 8];
	shl_u64 $d1, $d1, 3;
	ld_global_u32 $d3, [$d2 + 8];
	shl_u64 $d3, $d3, 3;
	ld_global_s64 $d3, [$d3 + 224];
	ld_global_s32 $s1, [$d3 + 12];
	cvt_s64_s32 $d4, $s1;
	cvt_s64_s32 $d4, $d4;
	add_s64 $d5, $d1, $d4;
	ld_global_s64 $d4, [$d5 + 0];
	cmp_eq_b1_s64 $c0, $d4, $d3;
	cbr $c0, @L19;
@L12:
	cmp_ne_b1_s32 $c0, $s1, 24;
	cbr $c0, @L23;
@L13:
	cmp_eq_b1_s64 $c0, $d1, $d3;
	cbr $c0, @L19;
@L15:
	ld_global_s64 $d4, [$d1 + 32];
	ld_global_s32 $s1, [$d4 + 0];
	mov_b32 $s2, 0;
	brn @L16;
@L17:
	shl_s32 $s3, $s2, 3;
	add_s32 $s3, $s3, 8;
	cvt_s64_s32 $d5, $s3;
	cvt_s64_s32 $d5, $d5;
	add_s64 $d6, $d4, $d5;
	ld_global_s64 $d5, [$d6 + 0];
	cmp_eq_b1_s64 $c0, $d3, $d5;
	cbr $c0, @L18;
@L20:
	add_s32 $s2, $s2, 1;
@L16:
	cmp_lt_b1_s32 $c0, $s2, $s1;
	cbr $c0, @L17;
	brn @L22;
@L19:
	cvt_s64_s32 $d1, $s0;
	mul_s64 $d1, $d1, 4;
	add_u64 $d2, $d2, $d1;
	mov_b64 $d1, $d0;
	st_global_u32 $d1, [$d2 + 16];
	ret;
@L18:
	st_global_s64 $d3, [$d1 + 24];
	brn @L19;
@L26:
	mov_b32 $s0, -92;
@L28:
	ret;
@L22:
	mov_b32 $s0, -35;
	brn @L28;
@L23:
	mov_b32 $s0, -35;
	brn @L28;
@L1:
	mov_b32 $s0, -11;
	brn @L28;
@L5:
	mov_b32 $s0, -11;
	brn @L28;
@L24:
	mov_b32 $s0, -92;
	brn @L28;
@L25:
	mov_b32 $s0, -27;
	brn @L28;
@L27:
	mov_b32 $s0, -27;
	brn @L28;
};	

spawning Program: hsailasm temp_hsa.hsail -g -o temp_hsa.o
hsailasm succeeded
createProgram succeeded
createKernel succeeded
INPUT:  [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31]
BEFORE: [nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil]
level 0, grid=32, group=1
pushPointerArg, addr=0
pushPointerArg, addr=0
pushPointerArg, addr=0
setPointerArg, addr=0xf0e33e88
setPointerArg, addr=0xf12b9b58
setPointerArg, addr=0xf12ba2b0
AFTER:  [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31]

lein test clumatra.core-test
Profiling info for clumatra.core$kernel_compile$reify__361.invoke(Object[], Object[], int)
  canBeStaticallyBound: false
Profiling info for clojure.lang.RT.intCast(int)
  canBeStaticallyBound: true
Profiling info for clojure.lang.RT.intCast(int)
  canBeStaticallyBound: true
Profiling info for clojure.lang.RT.aget(Object[], int)
  canBeStaticallyBound: true
Profiling info for clojure.lang.RT.aset(Object[], int, Object)
  canBeStaticallyBound: true
Fixed Hsail is
==============
version 0:95: $full : $large;	
// instance method HotSpotMethod<core$kernel_compile$reify__361.invoke(Object[], Object[], int)>	
kernel &run (	
	align 8 kernarg_u64 %_this,
	align 8 kernarg_u64 %_arg1,
	align 8 kernarg_u64 %_arg2
	) {
	ld_kernarg_u64  $d0, [%_this];
	ld_kernarg_u64  $d1, [%_arg1];
	ld_kernarg_u64  $d2, [%_arg2];
	workitemabsid_u32 $s0, 0;
	                                   
@L0:
	cmp_eq_b1_u64 $c0, $d2, 0; // null test 
	cbr $c0, @L1;
@L2:
	ld_global_s32 $s1, [$d2 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L27;
@L3:
	ld_global_s32 $s1, [$d2 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L26;
@L4:
	cmp_eq_b1_u64 $c0, $d1, 0; // null test 
	cbr $c0, @L5;
@L6:
	ld_global_s32 $s1, [$d1 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L25;
@L7:
	ld_global_s32 $s1, [$d1 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L24;
@L8:
	cvt_s64_s32 $d0, $s0;
	mul_s64 $d0, $d0, 4;
	add_u64 $d1, $d1, $d0;
	ld_global_u32 $d0, [$d1 + 16];
	cmp_eq_b1_u64 $c0, $d0, 0; // null test 
	cbr $c0, @L19;
@L10:
	ld_global_u32 $d1, [$d0 + 8];
	shl_u64 $d1, $d1, 3;
	ld_global_u32 $d3, [$d2 + 8];
	shl_u64 $d3, $d3, 3;
	ld_global_s64 $d3, [$d3 + 224];
	ld_global_s32 $s1, [$d3 + 12];
	cvt_s64_s32 $d4, $s1;
	cvt_s64_s32 $d4, $d4;
	add_s64 $d5, $d1, $d4;
	ld_global_s64 $d4, [$d5 + 0];
	cmp_eq_b1_s64 $c0, $d4, $d3;
	cbr $c0, @L19;
@L12:
	cmp_ne_b1_s32 $c0, $s1, 24;
	cbr $c0, @L23;
@L13:
	cmp_eq_b1_s64 $c0, $d1, $d3;
	cbr $c0, @L19;
@L15:
	ld_global_s64 $d4, [$d1 + 32];
	ld_global_s32 $s1, [$d4 + 0];
	mov_b32 $s2, 0;
	brn @L16;
@L17:
	shl_s32 $s3, $s2, 3;
	add_s32 $s3, $s3, 8;
	cvt_s64_s32 $d5, $s3;
	cvt_s64_s32 $d5, $d5;
	add_s64 $d6, $d4, $d5;
	ld_global_s64 $d5, [$d6 + 0];
	cmp_eq_b1_s64 $c0, $d3, $d5;
	cbr $c0, @L18;
@L20:
	add_s32 $s2, $s2, 1;
@L16:
	cmp_lt_b1_s32 $c0, $s2, $s1;
	cbr $c0, @L17;
	brn @L22;
@L19:
	cvt_s64_s32 $d1, $s0;
	mul_s64 $d1, $d1, 4;
	add_u64 $d2, $d2, $d1;
	mov_b64 $d1, $d0;
	st_global_u32 $d1, [$d2 + 16];
	ret;
@L18:
	st_global_s64 $d3, [$d1 + 24];
	brn @L19;
@L26:
	mov_b32 $s0, -92;
@L28:
	ret;
@L22:
	mov_b32 $s0, -35;
	brn @L28;
@L23:
	mov_b32 $s0, -35;
	brn @L28;
@L1:
	mov_b32 $s0, -11;
	brn @L28;
@L5:
	mov_b32 $s0, -11;
	brn @L28;
@L24:
	mov_b32 $s0, -92;
	brn @L28;
@L25:
	mov_b32 $s0, -27;
	brn @L28;
@L27:
	mov_b32 $s0, -27;
	brn @L28;
};	

spawning Program: hsailasm temp_hsa.hsail -g -o temp_hsa.o
hsailasm succeeded
createProgram succeeded
createKernel succeeded
level 0, grid=32, group=1
pushPointerArg, addr=0
pushPointerArg, addr=0
pushPointerArg, addr=0
setPointerArg, addr=0xf1684ee0
setPointerArg, addr=0xf17a0188
setPointerArg, addr=0xf17a08e0
level 0, grid=32, group=1
pushPointerArg, addr=0
pushPointerArg, addr=0
pushPointerArg, addr=0
setPointerArg, addr=0xf1684ee0
setPointerArg, addr=0xf17a0188
setPointerArg, addr=0xf17a4f08

Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
</pre>

If you want tun run up a dev environment, start it via the env.sh script - e.g.

<pre>
./bin/env.sh emacs
</pre>

to get JAVA_HOME etc set up

That's it for the moment - more as and when.

## License

Copyright © 2014 Julian Gosnell

Distributed under the Eclipse Public License, the same as Clojure.
