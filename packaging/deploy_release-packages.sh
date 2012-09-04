#!/bin/bash


usage() {
# deploys pacakges
cat <<EOF
Usage:
  $0 <dist-dir> unstable|testing|stable [nocommit]

Available "targets":
  unstable  = beta versions which contain latest changes
  testing   = only used for testing a release
  stable    = stable releases only

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
if [ $CMD = "unstable" -o $CMD = "testing" ]; then

  # create a tmp dir, check out current build files, delete all files
  mkdir -p $TMP_DIR
  cd $TMP_DIR
  osc co home:xtreemfs:$CMD xtreemfs-testing
  rm $TMP_DIR/home:xtreemfs:$CMD/xtreemfs-testing/*
  cd -
  
  # copy all new files, add new and delete old files, check in project
  cp xtreemfs-testing/* $TMP_DIR/home:xtreemfs:$CMD/xtreemfs-testing
  osc addremove $TMP_DIR/home:xtreemfs:$CMD/xtreemfs-testing/
  if [ -z "$NOCOMMIT" ]; then
    osc ci -m "update" $TMP_DIR/home:xtreemfs:$CMD/xtreemfs-testing/
  
    rm -rf $TMP_DIR
  fi
  
elif [ $CMD = "stable" ]; then
  # Determine subproject for given version number
  subproject=$(echo "$VER" | awk -F. '{ if (NF == 3) print $1"."$2".x"; else print "no_stable_version" }')
  if [ "$subproject" = "no_stable_version" ]; then
    echo "Failed to determine the subproject for this stable release: $VER"
    echo
    echo "Check if the required version number format x.y.z was used."
    exit 1
  fi

  # Check if the determined subproject already exists
  subproject_name="home:xtreemfs:$subproject"
  osc meta prj "$subproject_name" &>/dev/null
  if [ $rc -ne 0 ]; then
    echo "The subproject '$subproject_name' does not exist yet. Create it first from the webinterface and see the docu for additional steps."
    exit 1
  fi
    
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
# Build the package for openSUSE_12.1:
osc build --ccache openSUSE_12.1 xtreemfs-testing.spec
EOF
fi