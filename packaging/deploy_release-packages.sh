#!/bin/bash


usage() {
# deploys pacakges
cat <<EOF
Usage:
  $0 <dist-dir> test|release
EOF
	exit 0
}


VERSION=
XTREEMFS_HOME_DIR=

# parse command line options
if [ -z "$2" ]; then
	usage
fi

DIR=$1
CMD=$2

TMP_DIR=/tmp/xtreemfs-upload

if [ ! -f "/usr/bin/osc" ]; then
  echo "osc command not found - please install osc first!"
  exit 1
fi

cd $DIR
if [ $CMD == "test" ]; then

  # create a tmp dir, check out current build files, delete all files
  mkdir -p $TMP_DIR
  cd $TMP_DIR
  osc co home:xtreemfs xtreemfs-client-testing
  osc co home:xtreemfs xtreemfs-server-testing
  osc co home:xtreemfs xtreemfs-tools-testing
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-client-testing/*
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-server-testing/*
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-tools-testing/*
  cd -
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-client-testing xtreemfs-server-testing xtreemfs-tools-testing
  cd -
  
  # copy all new files, check in files
  cp xtreemfs-client-testing/* $TMP_DIR/home:xtreemfs/xtreemfs-client-testing
  cp xtreemfs-server-testing/* $TMP_DIR/home:xtreemfs/xtreemfs-server-testing
  cp xtreemfs-tools-testing/* $TMP_DIR/home:xtreemfs/xtreemfs-tools-testing
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-client-testing/*
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-server-testing/*
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-tools-testing/*
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-client-testing xtreemfs-server-testing xtreemfs-tools-testing
  cd -
  
  rm -rf $TMP_DIR
  
elif [ $CMD == "release" ]; then
  
  # create a tmp dir, check out current build files, delete all files
  mkdir -p $TMP_DIR
  cd $TMP_DIR
  osc co home:xtreemfs xtreemfs-client
  osc co home:xtreemfs xtreemfs-server
  osc co home:xtreemfs xtreemfs-tools
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-client/*
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-server/*
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-tools/*
  cd -
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-client xtreemfs-server xtreemfs-tools
  cd -
  
  # copy all new files, check in files
  cp xtreemfs-client/* $TMP_DIR/home:xtreemfs/xtreemfs-client
  cp xtreemfs-server/* $TMP_DIR/home:xtreemfs/xtreemfs-server
  cp xtreemfs-tools/* $TMP_DIR/home:xtreemfs/xtreemfs-tools
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-client/*
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-server/*
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-tools/*
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-client xtreemfs-server xtreemfs-tools
  cd -
  
  rm -rf $TMP_DIR
  
fi
