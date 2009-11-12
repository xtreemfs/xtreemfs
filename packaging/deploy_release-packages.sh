#!/bin/bash


usage() {
# deploys pacakges
cat <<EOF
Usage:
  $0 <dist-dir> test|release
EOF
	exit 0
}

# parse command line options
if [ -z "$2" ]; then
	usage
fi

DIR=$1
CMD=$2
VERSION=`cat $DIR/VER`

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
  osc co home:xtreemfs xtreemfs-testing
  osc delete $TMP_DIR/home:xtreemfs/xtreemfs-testing/*
  cd -
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-testing
  cd -
  
  # copy all new files, check in files
  cp xtreemfs-testing/* $TMP_DIR/home:xtreemfs/xtreemfs-testing
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-testing/*
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-testing
  cd -
  
  rm -rf $TMP_DIR
  
elif [ $CMD == "release" ]; then
    
  # create release packages on the server
  osc meta pkg home:xtreemfs xtreemfs-$VERSION --file meta.xml
      
  # create a tmp dir, check out current build files
  mkdir -p $TMP_DIR
  cd $TMP_DIR
  
  # copy the source packes to the new packages
  osc co home:xtreemfs xtreemfs-$VERSION
  
  cd -
  
  cp xtreemfs/* $TMP_DIR/home:xtreemfs/xtreemfs-$VERSION
    
  # add and commit the new files
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-$VERSION/*
  
  cd -
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-$VERSION
  cd -
  
  rm -rf $TMP_DIR
  
fi
