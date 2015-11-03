#!/bin/bash

# test relies on this variable
export XTREEMFS=$1

TEST_DIR=$4
HADOOP_VERSIONS="2.7.1"

# the test queries this volume
export XTREEMFS_DEFAULT_VOLUME="$(basename $(dirname $(pwd)))"

RESULT=0
for VERSION in $HADOOP_VERSIONS; do

  cd $TEST_DIR

  echo "Downloading Hadoop $VERSION..."
  wget -nv -O hadoop-$VERSION.tar.gz http://archive.apache.org/dist/hadoop/core/hadoop-$VERSION/hadoop-$VERSION.tar.gz

  echo "Extracting Hadoop $VERSION..."
  tar -zxf hadoop-$VERSION.tar.gz
  rm -rf hadoop-$VERSION.tar.gz

  # these are the environment variables needed by the test
  export HADOOP_HOME=$TEST_DIR/hadoop-$VERSION
  export HADOOP_PREFIX=$HADOOP_HOME
  echo "Set HADOOP_HOME=HADOOP_PREFIX=$HADOOP_PREFIX"

  echo "Running JUnit Tests for Hadoop Adapter and Hadoop $VERSION..."
  ant test -f $XTREEMFS/contrib/hadoop/build.xml 2>&1 > $TEST_DIR/log/hadoop-$VERSION-junit.log

  grep "BUILD SUCCESSFUL" $TEST_DIR/log/hadoop-$VERSION-junit.log >/dev/null
  if [ $? -eq 0 ]; then
    echo "JUnit Tests for Hadoop Adapter and Hadoop $VERSION successful."
  else
    echo "JUnit Tests for Hadoop Adapter and Hadoop $VERSION failed, see $TEST_DIR/log/hadoop-$VERSION-junit.log for details."
    RESULT=1
  fi

done
exit $RESULT