#!/bin/bash

XTREEMFS=$1
if [ -z "$XTREEMFS" ]
then
  if [ -d "java/xtreemfs-servers" ]; then XTREEMFS="."; fi
fi
echo "XTREEMFS=$XTREEMFS"

TEST_DIR=$4
if [ -z $TEST_DIR ]
then
  TEST_DIR=/tmp/xtreemfs-junit
  if [ ! -d "$TEST_DIR" ]; then mkdir "$TEST_DIR"; fi
  if [ ! -d "${TEST_DIR}/log" ]; then mkdir "${TEST_DIR}/log"; fi
fi
echo "TEST_DIR: $TEST_DIR"

mvn --settings $XTREEMFS/java/settings.xml --activate-profiles xtreemfs-dev --file $XTREEMFS/java/pom.xml test 2>&1 | tee "${TEST_DIR}/log/junit.log"
exit ${PIPESTATUS[0]}
