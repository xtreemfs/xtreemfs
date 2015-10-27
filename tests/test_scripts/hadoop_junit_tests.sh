#!/bin/bash

XTREEMFS=$1
TEST_DIR=$4
HADOOP_VERSIONS="2.7.1"

# the test queries this volume
export XTREEMFS_DEFAULT_VOLUME="$(basename $(dirname $(pwd)))"

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

  ant test -f $XTREEMFS/contrib/hadoop/build.xml 2>&1 > $TEST_DIR/log/hadoop-$VERSION-junit.log

done