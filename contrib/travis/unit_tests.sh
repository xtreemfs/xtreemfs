#!/bin/bash

XTREEMFS_DIR=$(echo $PWD)

junit_tests(){
  tests/test_scripts/junit_tests.sh
  # return 0
}

junit_tests
jstatus="$?"
if [ $jstatus -ne 0 ]; then
  export JUNIT_FAILED="true"
else
  export JUNIT_FAILED="false"
fi

cd $XTREEMFS_DIR

if [[ $jstatus -eq 0 ]]; then
  return 0
else
  return 1
fi
