export JAVA_HOME=`pwd`/graal/graal/jdk1.8.0-internal/product
export PATH=$JAVA_HOME/bin:$PATH
export JAVACMD=$JAVA_HOME/bin/java
$@
