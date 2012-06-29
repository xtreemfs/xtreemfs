#!/bin/bash

# runs all JUnit tests in the subpackage 'org.xtreemfs.test' that end with '*Test.java'

TEST_DIR=$4
if [ -z $TEST_DIR ]; then TEST_DIR=/tmp/xtreemfs-junit; fi
echo "TEST_DIR: $TEST_DIR"

export XTREEMFS=$1
echo "XTREEMFS=$XTREEMFS"

# find all jars; build the classpath for running the JUnit tests
CLASSPATH="."
while read LINE; do
    CLASSPATH="$CLASSPATH:$LINE"
done < <(find $XTREEMFS/java -name \*.jar)

# find all source files for the unit tests
SOURCES=""
while read LINE; do
    SOURCES="$SOURCES $LINE"
done < <(find $XTREEMFS/java/servers/test -name \*.java)

CLASSES_DIR=$TEST_DIR/classes

# compile JUnit tests; store results in $CLASSES_DIR
mkdir -p $CLASSES_DIR
JAVAC_CALL="$JAVA_HOME/bin/javac -cp $CLASSPATH -d $TEST_DIR/classes $SOURCES"
echo "Running ${JAVAC_CALL}..."
$JAVAC_CALL
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

CLASSPATH="$CLASSPATH:$CLASSES_DIR"

# find and execute all JUnit tests among the class files
COUNTER=0
JUNIT_TESTS=""
while read LINE; do
    
	if [[ $LINE == *org/xtreemfs/test* ]]; then
	    
    	# check if test is located in the package 'org.xtreemfs.test' or a subpackage    
	    TEST=`echo $LINE | sed -e 's/.*org\/xtreemfs\/test\///;s/\.class//'`
	    # replace '/' with '.'
        TEST=org.xtreemfs.test.${TEST//\//\.}
        
	elif [[ $LINE == *org/xtreemfs/common* ]]; then
	    
	    # check if test is located in the package 'org.xtreemfs.common' or a subpackage
	    TEST=`echo $LINE | sed -e 's/.*org\/xtreemfs\/common\///;s/\.class//'`
	    # replace '/' with '.'
        TEST=org.xtreemfs.common.${TEST//\//\.}
	    
	else
	    # not a valid JUnit test
	    continue;
	fi

	echo "TEST: $TEST"
    
    # run each JUnit test separately in its own JVM
    JAVA_CALL="$JAVA_HOME/bin/java -cp $CLASSPATH org.junit.runner.JUnitCore $TEST"
	
    echo "Running ${JAVA_CALL}..."
    $JAVA_CALL
    RESULT=$?
    if [ "$RESULT" -ne "0" ]; then echo "$TEST failed; $COUNTER tests successfully executed"; exit $RESULT; fi
    
    COUNTER=`expr $COUNTER + 1`
    
done < <(find $CLASSES_DIR -name *Test.class -type f)

echo "$COUNTER tests successfully executed."