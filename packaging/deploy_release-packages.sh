#!/bin/bash


usage() {
# deploys pacakges
cat <<EOF
Usage:
  $0 <dist-dir> unstable|stable [nocommit]

If 'nocommit' is specified, the generated files will be kept in the temporary
directory and not commited to the OpenSuse build service.
This is useful for building a package locally.
EOF
  exit 0
}

# parse command line options
if [ -z "$2" ]; then
  usage
fi

DIR=$1
CMD=$2
NOCOMMIT=$3
VERSION=`cat $DIR/VER`

TMP_DIR=/tmp/xtreemfs-upload

if [ ! -f "/usr/bin/osc" ]; then
  echo "osc command not found - please install osc first!"
  exit 1
fi

if [ -d "$TMP_DIR" ]; then
  rm -rf "$TMP_DIR"
fi

cd $DIR
if [ $CMD == "unstable" ]; then

  # create a tmp dir, check out current build files, delete all files
  mkdir -p $TMP_DIR
  cd $TMP_DIR
  osc co home:xtreemfs:unstable xtreemfs-testing
  rm $TMP_DIR/home:xtreemfs:unstable/xtreemfs-testing/*
  cd -
  
  # copy all new files, add new and delete old files, check in project
  cp xtreemfs-testing/* $TMP_DIR/home:xtreemfs:unstable/xtreemfs-testing
  osc addremove $TMP_DIR/home:xtreemfs:unstable/xtreemfs-testing/
  if [ -z "$NOCOMMIT" ]; then
    exit 0
    osc ci -m "update" $TMP_DIR/home:xtreemfs:unstable/xtreemfs-testing/
  
    rm -rf $TMP_DIR
  fi
  
elif [ $CMD == "stable" ]; then
    
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
  if [ -z "$NOCOMMIT" ]; then
    osc ci -m "imported xtreemfs $VERSION" xtreemfs-$VERSION
    cd -
    
    rm -rf $TMP_DIR
  fi
fi

if [ -n "$NOCOMMIT" ]; then
  cat <<EOF
The generated build service files were NOT commited to the OpenSuse build
service. Instead they are still present in: $TMP_DIR

You can use them to test building a package locally e.g.,

cd $TMP_DIR
cd home:xtreemfs:unstable/xtreemfs-testing
# Build the package for openSUSE_11.4, 64 Bit:
osc build openSUSE_11.4 x86_64 xtreemfs-testing.spec
EOF
fi