#!/bin/bash

XTREEMFS_DIR=$(echo $PWD)

junit_tests(){
  tests/test_scripts/junit_tests.sh
  # return 0
}

cpp_unit_tests(){
  tests/test_scripts/cpp_unit_tests.sh
  # return 0
}

junit_tests
jstatus="$?"
if [ $jstatus -ne 0 ]; then
  export JUNIT_FAILED="true"
else
  export JUNIT_FAILED="false"
fi

cpp_unit_tests
cstatus="$?"
if [ $cstatus -ne 0 ]; then
  export CPP_FAILED="true"
else
  export CPP_FAILED="false"
fi

cd $XTREEMFS_DIR

if [[ $jstatus -eq 0 ]] && [[ $cstatus -eq 0 ]]; then
  return 0
else
  return 1
fi
