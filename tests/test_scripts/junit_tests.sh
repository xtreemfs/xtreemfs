#!/bin/bash

# runs all JUnit tests in the subpackage 'org.xtreemfs.test' that end with '*Test.java'

TEST_DIR=$4
if [ -z $TEST_DIR ]
then
  TEST_DIR=/tmp/xtreemfs-junit
  if [ ! -d "$TEST_DIR" ]; then mkdir "$TEST_DIR"; fi
  if [ ! -d "${TEST_DIR}/log" ]; then mkdir "${TEST_DIR}/log"; fi
fi
echo "TEST_DIR: $TEST_DIR"

JUNIT_LOG_FILE="${TEST_DIR}/log/junit.log"

XTREEMFS=$1
if [ -z "$XTREEMFS" ]
then
  # Try to guess the XtreemFS svn root.
  if [ -d "java/servers" ]; then XTREEMFS="."; fi
fi
echo "XTREEMFS=$XTREEMFS"

# Method which returns a regex list of possible ports in use by the server webinterface and RPC server.
function get_xtreemfs_ports() {
  offset=$(grep "PORT_RANGE_OFFSET = " "${XTREEMFS}/java/servers/test/org/xtreemfs/test/SetupUtils.java" | grep -oE "[0-9]+")
  
  default_ports="30638 30636 30639 29637 29640 29641 29642 29643 29644 29645 29646 29647 29648 29649 29650 29651 32638 32636 32639 32637 32640 32641 32642 32643 32644 32645 32646 32647 32648 32649 32650 32651"
  
  for port in $default_ports
  do
    default_ports=${default_ports/$port/$(($port + $offset))}
  done
  
  # Construct final regex:
  echo ":("${default_ports// /|}")"
}

# The used Sun webserver does allow to set the socket option SO_REUSEADDR.
# Therefore, a subsequent JUnit test may fail with "address already in use"
# because the webinterface of an XtreemFS server from a previous test is still
# in the TIME_WAIT state.
# Additionally, it also checks for regular ports since ReplicationTest keeps
# failing with BindException despite enabled SO_REUSEADDR.
function wait_for_time_wait_ports() {
  ports_regex=$(get_xtreemfs_ports)
  
  while [ -n "$(netstat -n -a -t | grep -E "$ports_regex")" ]
  do
    sleep 1
  done
}

# find all jars; build the classpath for running the JUnit tests
CLASSPATH="."
while read LINE; do
    CLASSPATH="$CLASSPATH:$LINE"
done < <(find $XTREEMFS/java -name \*.jar)

# find all source files for the unit tests
SOURCES=""
for PROJECT in servers flease foundation
do
  SOURCES="$SOURCES "$(find "java/${PROJECT}/test" -name \*.java -printf "%p ")
done

CLASSES_DIR=$TEST_DIR/classes

# compile JUnit tests; store results in $CLASSES_DIR
mkdir -p $CLASSES_DIR
JAVAC_CALL="$JAVA_HOME/bin/javac -cp $CLASSPATH -d $TEST_DIR/classes $SOURCES"
echo "Compiling tests..."
# echo "Compiling tests: ${JAVAC_CALL}"
$JAVAC_CALL
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

CLASSPATH="$CLASSPATH:$CLASSES_DIR"

# find and execute all JUnit tests among the class files
rm -f "$JUNIT_LOG_FILE"
COUNTER=0
FAILED=0
JUNIT_TESTS=""
while read LINE; do

  if [[ $LINE = *ExternalIntegrationTest.class ]]
  then
      # not a valid JUnit test
      continue;
  fi

  # Transform a path of the form "/tmp/xtreemfs-junit/classes/org/xtreemfs/test/mrc/OSDPolicyTest.class" to the form "org.xtreemfs.test.mrc.OSDPolicyTest".
  TEST=`echo $LINE | sed -r -e 's|^.*(org\/xtreemfs\/.+)\.class\$|\1|'`
  # replace '/' with '.'
  TEST=${TEST//\//\.}

  # run each JUnit test separately in its own JVM
  JAVA_CALL="$JAVA_HOME/bin/java -Xmx1000m -ea -cp $CLASSPATH org.junit.runner.JUnitCore $TEST"

  RESULT=1
  i=0
  while [ $i -le 3 -a $RESULT -ne 0 ]
  do
    echo -n "Running test `expr $COUNTER + 1`: $TEST ... "
    $JAVA_CALL >> "$JUNIT_LOG_FILE" 2>&1
    RESULT=$?
    i=`expr $i + 1`
  
    if [ "$RESULT" -ne "0" ]; then
      echo -n "FAILURE, waiting for ports to become free before retrying ... "

      # Log netstat output to debug "address already in use" problems.
      temp_file="$(mktemp netstat.XXXXXX)"
      netstat -n -t -a &> "$temp_file.all"
      netstat -n -t -l &> "$temp_file.listen"
      netstat -n -t -a -o &> "$temp_file.all+timer"

      # Wait for all ports to become free before retrying in case the cause was the "address already in use" problem.
      before_wait_ports=$(date +%s)
      wait_for_time_wait_ports
      after_wait_ports=$(date +%s)
      echo " ports free after $((after_wait_ports - before_wait_ports))s."
    else
      echo "ok"
    fi
  done

  if [ "$RESULT" -ne "0" ]; then
    FAILED=`expr $FAILED + 1`
  fi

  COUNTER=`expr $COUNTER + 1`
    
done < <(find $CLASSES_DIR -name *Test.class -type f)

echo "`expr $COUNTER - $FAILED` / $COUNTER tests successfully executed."

if [ "$FAILED" -ne "0" ]; then exit 1; fi

# Report crashes.
grep "has crashed" "$JUNIT_LOG_FILE" >/dev/null
if [ $? -eq 0 ]
then
  echo "However, during the test services did crash. Examine the log file junit.log for more information."
  exit 2
fi
