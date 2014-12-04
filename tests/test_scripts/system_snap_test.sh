#!/bin/bash

export XTREEMFS=$1
echo "XTREEMFS=$XTREEMFS"

DIR_URL=$2
MRC_URL=$3
TEST_DIR=$4

if [[ "$DIR_URL" == pbrpcs://* || "$DIR_URL" == pbrpcg://* ]]; then
  CREDS="-c $XTREEMFS/tests/certs/Client.p12 -cpass passphrase -t $XTREEMFS/tests/certs/trusted.jks -tpass passphrase"
fi

VOL_URL=$($XTREEMFS/bin/xtfsutil .. | grep "XtreemFS URL" | cut -b 13-  | tr -d ' ')

cleanup() {
  TMP_MNT=$1

  echo "Cleanup"

  COMMAND="$XTREEMFS/bin/umount.xtreemfs $TMP_MNT"
  echo "Running $COMMAND"; $COMMAND
  rmdir $TMP_MNT

  COMMAND="$XTREEMFS/bin/xtfsutil --delete-snapshot mySnap ."
  echo "Running $COMMAND"; $COMMAND
}

#
# enable and create a snapshot
#

# enable snapshots
COMMAND="$XTREEMFS/bin/xtfsutil --enable-snapshots .."
echo "Running ${COMMAND}"
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# create an empty directory and a file
echo "Create a directory and files"
mkdir ./newdir
echo "test" > test.txt
echo "hello" > world.txt

# wait until the file is removed from the open file table and written to the mrc
echo "Waiting for 91s until the files are closed"
sleep 91s
echo "Continue"

# take a snapshot
COMMAND="$XTREEMFS/bin/xtfsutil --create-snapshot mySnap ."
echo "Running ${COMMAND}"
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# list all snapshots
COMMAND="$XTREEMFS/bin/xtfsutil --list-snapshots .."
echo "Running ${COMMAND}"
$COMMAND |grep mySnap
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# mount the snapshot
TMP_MNT=$(mktemp -d)
COMMAND="$XTREEMFS/bin/mount.xtreemfs $VOL_URL@mySnap $TMP_MNT"
echo "Running ${COMMAND}"
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# check if the files exist
echo "Checking snapshot file consistency"
if [ ! -e $TMP_MNT/test.txt ] || [ ! -e $TMP_MNT/world.txt ]; then
  ERROR="Files missing in snapshot"
fi
  
if [ ! -e $TMP_MNT/newdir ]; then
  ERROR="Directory 'newdir' missing in snapshot \n$ERROR"
fi

CONT=$(<$TMP_MNT/world.txt)
if [ "$CONT" != "hello" ]; then
  ERROR="world.txt mismatch in snapshot: '$CONT' != 'hello' \n$ERROR"
fi

CONT=$(< $TMP_MNT/test.txt)
if [ "$CONT" != "test" ]; then
  ERROR="test.txt mismatch in snapshot: '$CONT' != 'test' \n$ERROR"
fi

# exit if some file errors occured
if [ -n "$ERROR" ]; then
  echo -e "$ERROR"
  cleanup $TMP_MNT
  exit 1
fi


# check that the data is retained in the snapshot on change and deletion
echo "Modifying original files and checking snapshot consistency"
rm test.txt
echo "goodbye" > world.txt

# echo "Waiting for 61s until the files are closed/deleted"
# sleep 61s
# echo "Continue"

CONT=$(<$TMP_MNT/world.txt)
if [ "$CONT" != "hello" ]; then
  ERROR="changed world.txt mismatch in snapshot: '$CONT' != 'hello' \n$ERROR"
fi

CONT=$(< $TMP_MNT/test.txt)
if [ "$CONT" != "test" ]; then
  ERROR="removed test.txt mismatch in snapshot: '$CONT' != 'test' \n$ERROR"
fi

# exit if some file errors occured
if [ -n "$ERROR" ]; then
  echo -e "$ERROR"
  cleanup $TMP_MNT
  exit 1
fi

# unmount the snapshot and remove the directory
COMMAND="$XTREEMFS/bin/umount.xtreemfs $TMP_MNT"
echo "Running ${COMMAND}"
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

rmdir $TMP_MNT

# delete a snapshot
COMMAND="$XTREEMFS/bin/xtfsutil --delete-snapshot mySnap ."
echo "Running ${COMMAND} ..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

