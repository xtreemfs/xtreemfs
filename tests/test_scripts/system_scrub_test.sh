#!/bin/bash

TEST_DIR=$4
export XTREEMFS=$1
echo "XTREEMFS=$XTREEMFS"

if [[ "$2" == pbrpcs://* || "$2" == pbrpcg://* ]]; then
  CREDS="-c $1/tests/certs/Client.p12 -cpass passphrase -t $1/tests/certs/trusted.jks -tpass passphrase"
fi

#
# scrub volume with one file
#

# create file
echo "test" > file.txt

# execute scrub command
COMMAND="$1/bin/xtfs_scrub -dir $2 $CREDS -repair nomdcache"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

#
# create file, delete objects and scrub
#

# parse the volume ID
#FILE_ID=`getfattr --only-values -n xtreemfs.file_id ..`
#COLON_INDEX=`awk -v a="$FILE_ID" -v b=":" 'BEGIN{print index(a,b)}'`
#VOL_ID=${FILE_ID:0:$COLON_INDEX-1}

# delete file data
VOLUME_DIR=$TEST_DIR/data/osd0/`find . |grep 00000`
echo "Deleting data at $VOLUME_DIR..."
rm -rf $VOLUME_DIR/*

# execute scrub command
COMMAND="$1/bin/xtfs_scrub -dir $2 $CREDS -repair nomdcache"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# check if the file size was corrected
SIZE=`stat --printf=%s file.txt`
if [ "$SIZE" -ne "0" ]; then echo "ERROR: Wrong file size detected after scrubbing: expected=0, actual=$SIZE"; exit 1; fi

#
# create and scrub 100 files
#

# create 100 files
echo "creating 100 files ..."
for i in `seq 1 100`; do echo "test" > $i.txt; done

# execute scrub command
COMMAND="$1/bin/xtfs_scrub -dir $2 $CREDS -repair nomdcache"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi