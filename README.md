# Clumatra

Experiments with Clojure / Sumatra / Graal and, ultimately, Clojure compiled onto GPGPU...

For Fedora 20 / x86_64 - see https://wiki.openjdk.java.net/display/Sumatra/Sumatra+JDK+build+instructions for more detail / alternative architectures

You'll need to ensure that a few packages are installed - do the following as root:

<pre>
yum install java-1.7.0-openjdk mercurial freetype-devel cups-devel gcc gcc-c++ p7zip p7zip-plugins ccache libstdc++-static maven
yum-builddep java-1.7.0-openjdk
</pre>

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

The output should look something like this. It includes the source
disassembled Java bytecode and target HSAIL for reference. I have
added "-Dclumatra.verbose=true" flag in the project.clj. If you would
prefer terse test output, just set this to false.

<pre>
lein test
Initializing NoDisassemble Transformer
[HSAIL] library is libokra_x86_64.so
[HSAIL] using _OKRA_SIM_LIB_PATH_=/tmp/okraresource.dir_75590013030364124/libokra_x86_64.so
[GPU] registered initialization of Okra (total initialized: 1)

*** TESTING WITH OKRA ***
Reflection warning, no/disassemble.clj:10:3 - call to method replace can't be resolved (target class is unknown).
Reflection warning, no/disassemble.clj:21:28 - reference to field getCanonicalName can't be resolved.
Reflection warning, no/disassemble.clj:23:5 - call to method disassemble on org.eclipse.jdt.internal.core.util.Disassembler can't be resolved (argument types: java.lang.Object, java.lang.String, unknown).


lein test clojure.lang-test
"Elapsed time: 0.436649 msecs"
"Elapsed time: 0.775219 msecs"

lein test clumatra.core-test
OKRA: SIMULATED
// Compiled from core_test.clj (version 1.5 : 49.0, super bit)
public final class clumatra.core_test$fn$reify__1024 implements clumatra.core_test.LongKernel, clojure.lang.IObj {
  
  // Field descriptor #11 Lclojure/lang/Var;
  public static final clojure.lang.Var const__0;
  
  // Field descriptor #11 Lclojure/lang/Var;
  public static final clojure.lang.Var const__1;
  
  // Field descriptor #11 Lclojure/lang/Var;
  public static final clojure.lang.Var const__2;
  
  // Field descriptor #39 Lclojure/lang/IPersistentMap;
  final clojure.lang.IPersistentMap __meta;
  
  // Method descriptor #15 ()V
  // Stack: 2, Locals: 0
  public static {};
     0  ldc <String "clojure.core"> [17]
     2  ldc <String "aset"> [19]
     4  invokestatic clojure.lang.RT.var(java.lang.String, java.lang.String) : clojure.lang.Var [25]
     7  checkcast clojure.lang.Var [27]
    10  putstatic clumatra.core_test$fn$reify__1024.const__0 : clojure.lang.Var [29]
    13  ldc <String "clojure.core"> [17]
    15  ldc <String "int"> [31]
    17  invokestatic clojure.lang.RT.var(java.lang.String, java.lang.String) : clojure.lang.Var [25]
    20  checkcast clojure.lang.Var [27]
    23  putstatic clumatra.core_test$fn$reify__1024.const__1 : clojure.lang.Var [33]
    26  ldc <String "clojure.core"> [17]
    28  ldc <String "aget"> [35]
    30  invokestatic clojure.lang.RT.var(java.lang.String, java.lang.String) : clojure.lang.Var [25]
    33  checkcast clojure.lang.Var [27]
    36  putstatic clumatra.core_test$fn$reify__1024.const__2 : clojure.lang.Var [37]
    39  return
      Line numbers:
        [pc: 0, line: 107]
  
  // Method descriptor #41 (Lclojure/lang/IPersistentMap;)V
  // Stack: 2, Locals: 2
  public core_test$fn$reify__1024(clojure.lang.IPersistentMap arg0);
     0  aload_0
     1  invokespecial java.lang.Object() [43]
     4  aload_0
     5  aload_1
     6  putfield clumatra.core_test$fn$reify__1024.__meta : clojure.lang.IPersistentMap [45]
     9  return
      Line numbers:
        [pc: 0, line: 107]
  
  // Method descriptor #15 ()V
  // Stack: 2, Locals: 1
  public core_test$fn$reify__1024();
    0  aload_0
    1  aconst_null
    2  invokespecial clumatra.core_test$fn$reify__1024(clojure.lang.IPersistentMap) [47]
    5  return

  
  // Method descriptor #49 ()Lclojure/lang/IPersistentMap;
  // Stack: 1, Locals: 1
  public clojure.lang.IPersistentMap meta();
    0  aload_0
    1  getfield clumatra.core_test$fn$reify__1024.__meta : clojure.lang.IPersistentMap [45]
    4  areturn

  
  // Method descriptor #51 (Lclojure/lang/IPersistentMap;)Lclojure/lang/IObj;
  // Stack: 3, Locals: 2
  public clojure.lang.IObj withMeta(clojure.lang.IPersistentMap arg0);
    0  new clumatra.core_test$fn$reify__1024 [2]
    3  dup
    4  aload_1
    5  invokespecial clumatra.core_test$fn$reify__1024(clojure.lang.IPersistentMap) [47]
    8  areturn

  
  // Method descriptor #53 ([J[JI)V
  // Stack: 6, Locals: 4
  public void invoke(long[] in, long[] out, int gid);
     0  aload_2 [out]
     1  aconst_null
     2  astore_2 [out]
     3  checkcast long[] [55]
     6  iload_3 [gid]
     7  nop
     8  aload_1 [in]
     9  aconst_null
    10  astore_1 [in]
    11  checkcast long[] [55]
    14  iload_3 [gid]
    15  nop
    16  laload
    17  lconst_1
    18  ladd
    19  invokestatic clojure.lang.RT.aset(long[], int, long) : long [58]
    22  invokestatic clojure.lang.Numbers.num(long) : java.lang.Number [64]
    25  pop
    26  return
      Line numbers:
        [pc: 0, line: 107]
        [pc: 0, line: 109]
        [pc: 6, line: 109]
        [pc: 8, line: 109]
        [pc: 8, line: 109]
        [pc: 14, line: 109]
      Local variable table:
        [pc: 0, pc: 26] local: this index: 0 type: clumatra.core_test.fn.reify__1024
        [pc: 0, pc: 26] local: in index: 1 type: long[]
        [pc: 0, pc: 26] local: out index: 2 type: long[]
        [pc: 0, pc: 26] local: gid index: 3 type: int

}
Profiling info for clumatra.core_test$fn$reify__1024.invoke(long[], long[], int)
  canBeStaticallyBound: false
Profiling info for clojure.lang.RT.aset(long[], int, long)
  canBeStaticallyBound: true
Profiling info for clojure.lang.Numbers.num(long)
  canBeStaticallyBound: true
Profiling info for com.oracle.graal.replacements.BoxingSubstitutions$LongSubstitutions.valueOf(long)
  canBeStaticallyBound: true
Fixed Hsail is
==============
version 0:95: $full : $large;	
// instance method HotSpotMethod<core_test$fn$reify__1024.invoke(long[], long[], int)>	
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
	cbr $c0, @L10;
@L3:
	ld_global_s32 $s1, [$d2 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L9;
@L4:
	cmp_eq_b1_u64 $c0, $d1, 0; // null test 
	cbr $c0, @L5;
@L6:
	ld_global_s32 $s1, [$d1 + 12];
	cmp_ge_b1_u32 $c0, $s0, $s1;
	cbr $c0, @L8;
@L7:
	cvt_s64_s32 $d0, $s0;
	mul_s64 $d0, $d0, 8;
	add_u64 $d1, $d1, $d0;
	ld_global_s64 $d0, [$d1 + 16];
	add_s64 $d0, $d0, 0x1;
	cvt_s64_s32 $d1, $s0;
	mul_s64 $d1, $d1, 8;
	add_u64 $d2, $d2, $d1;
	st_global_s64 $d0, [$d2 + 16];
	ret;
@L1:
	mov_b32 $s0, -11;
@L11:
	ret;
@L9:
	mov_b32 $s0, -92;
	brn @L11;
@L8:
	mov_b32 $s0, -27;
	brn @L11;
@L10:
	mov_b32 $s0, -27;
	brn @L11;
@L5:
	mov_b32 $s0, -11;
	brn @L11;
};	

spawning Program: hsailasm temp_hsa.hsail -g -o temp_hsa.o
hsailasm succeeded
createProgram succeeded
createKernel succeeded
level 0, grid=32, group=1
pushPointerArg, addr=0
pushPointerArg, addr=0
pushPointerArg, addr=0
setPointerArg, addr=0xc0390088
setPointerArg, addr=0xc0718188
setPointerArg, addr=0xc07182c0
(0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31)  ->  (1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32)

...
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
