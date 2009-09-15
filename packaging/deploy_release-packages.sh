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
  
  # create release packages on the server
  osc meta pkg home:xtreemfs xtreemfs-client-$VERSION --file $DIR/client-meta.xml
  osc meta pkg home:xtreemfs xtreemfs-server-$VERSION --file $DIR/server-meta.xml
  osc meta pkg home:xtreemfs xtreemfs-tools-$VERSION --file $DIR/tools-meta.xml
  
  # copy the source packes to the new packages
  osc co home:xtreemfs xtreemfs-client-$VERSION
  osc co home:xtreemfs xtreemfs-server-$VERSION
  osc co home:xtreemfs xtreemfs-tools-$VERSION
  cp xtreemfs-client/* $TMP_DIR/home:xtreemfs/xtreemfs-client-$VERSION
  cp xtreemfs-server/* $TMP_DIR/home:xtreemfs/xtreemfs-server-$VERSION
  cp xtreemfs-tools/* $TMP_DIR/home:xtreemfs/xtreemfs-tools-$VERSION
  
  # add and commit the new files
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-client-$VERSION/*
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-server-$VERSION/*
  osc add $TMP_DIR/home:xtreemfs/xtreemfs-tools-$VERSION/*
  
  cd -
  cd $TMP_DIR/home:xtreemfs
  osc ci -m " " xtreemfs-client-$VERSION xtreemfs-server-$VERSION xtreemfs-tools-$VERSION
  cd -
  
  rm -rf $TMP_DIR
  
fi
