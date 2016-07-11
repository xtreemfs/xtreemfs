#!/bin/bash

# test relies on this variable
export XTREEMFS=$1

TEST_DIR=$4
HADOOP_VERSIONS="0.23.11 2.2.0 2.3.0 2.4.1 2.5.2 2.6.4 2.7.2"

# the test queries this volume
export XTREEMFS_DEFAULT_VOLUME="$(basename $(dirname $(pwd)))"

RESULT=0
for VERSION in $HADOOP_VERSIONS; do
  echo "Running JUnit Tests for Hadoop Adapter and Hadoop $VERSION..."
  mvn --settings $XTREEMFS/contrib/hadoop/settings.xml --activate-profiles xtreemfs-hadoop-client-dev --file $XTREEMFS/contrib/hadoop/pom.xml --define hadoop-common.version=$VERSION test 2>&1 > $TEST_DIR/log/hadoop-$VERSION-junit.log

  if [ $? -eq 0 ]; then
    echo "JUnit Tests for Hadoop Adapter and Hadoop $VERSION successful."
  else
    echo "JUnit Tests for Hadoop Adapter and Hadoop $VERSION failed, see $TEST_DIR/log/hadoop-$VERSION-junit.log for details."
    RESULT=1
  fi
done

exit $RESULT