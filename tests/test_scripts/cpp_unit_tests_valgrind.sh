#!/bin/bash

# Copyright (c) 2014 by Michael Berlin, Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.

# This test runs all C++ unit tests through Valgrind which will check for
# memory leaks.
#
# Make sure that you did run export BUILD_CLIENT_TESTS=true before running
# "make client_debug". Otherwise, the unit tests won't be built.

set -e

function warn_missing_url() {
  cat <<EOF
INFO: URL to XtreemFS $1 not given as $2 parameter.

INFO: Tests will use the default URL at localhost and the default port.
INFO: Make sure to run an XtreemFS setup on this address or specify a different URL as argument.

EOF
}

hash valgrind 2>/dev/null || {
  echo "ERROR: valgrind not found, but required by this test."
  exit 1
}

# Parse arguments.
TEST_DIR=$4
if [ -z $TEST_DIR ]
then
  TEST_DIR=/tmp/xtreemfs-cpp-valgrind
  if [ ! -d "$TEST_DIR" ]; then mkdir "$TEST_DIR"; fi
  if [ ! -d "${TEST_DIR}/log" ]; then mkdir "${TEST_DIR}/log"; fi
fi
export XTREEMFS_TEST_DIR="$TEST_DIR"
echo "INFO: TEST_DIR: $TEST_DIR"
VALGRIND_LOG_FILE="${TEST_DIR}/log/valgrind.log"

if [ -n "$1" ]
then
  XTREEMFS_DIR="$1"
else
  # Try to guess the path of the XtreemFS repository.
  [ -d "cpp" ] && XTREEMFS_DIR="."
  [ -d "../cpp" ] && XTREEMFS_DIR=".."
  [ -d "../../cpp" ] && XTREEMFS_DIR="../.."
  if [ -n "$XTREEMFS_DIR" ]
  then
    echo "INFO: Path to XtreemFS repository auto-detected and set to: ${XTREEMFS_DIR}"
  else
    echo "ERROR: Path to XtreemFS repository not found. Set it as first parameter. Aborting."
    exit 2
  fi
fi

if [ -n "$2" ]
then
  export XTREEMFS_DIR_URL="$2"
else
  warn_missing_url "DIR" "second"
fi
if [ -n "$3" ]
then
  export XTREEMFS_MRC_URL="$3"
else
  warn_missing_url "MRC" "third"
fi

# Run tests
cd "$XTREEMFS_DIR"
cd cpp/build

global_rc=0
for test in test_*
do
  # disable test_object_cache due to frequent failures (feature is not maintained actively)
  if [ "$test" = "test_object_cache" ]
  then
    continue
  fi

  set +e
  valgrind --leak-check=full --show-reachable=yes --error-exitcode=23 --suppressions="${XTREEMFS_DIR}/cpp/valgrind.supp" ./$test &>>$VALGRIND_LOG_FILE
  rc=$?
  set -e
  # Add some whitespace to the logfile between runs.
  echo -e "\n\n\n" >> $VALGRIND_LOG_FILE
  
  if [ $rc -eq 0 ]
  then
    echo "Valgrind memory-leak check PASSED for: $test"
  else
    echo "Valgrind memory-leak check FAILED for: $test"
    global_rc=1
  fi
done

exit $global_rc