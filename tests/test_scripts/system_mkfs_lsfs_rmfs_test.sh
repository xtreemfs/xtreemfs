#!/bin/bash

if [[ "$3" == pbrpcs://* || "$3" == pbrpcg://* ]]; then
  CREDS="--pkcs12-file-path $1/tests/certs/Client.p12 --pkcs12-passphrase passphrase"
fi

# test mkfs.xtreemfs
MKFS="$1/bin/mkfs.xtreemfs"
if [ ! -f "$MKFS" ]
then
  MKFS="mkfs.xtreemfs"
fi
COMMAND="$MKFS $CREDS $3test"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# test lsfs.xtreemfs
LSFS="$1/bin/lsfs.xtreemfs"
if [ ! -f "$LSFS" ]
then
  LSFS="lsfs.xtreemfs"
fi
COMMAND="$LSFS $CREDS $3"
echo "Running ${COMMAND}..."
$COMMAND | grep test
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# test rmfs.xtreemfs
RMFS="$1/bin/rmfs.xtreemfs"
if [ ! -f "$RMFS" ]
then
  RMFS="rmfs.xtreemfs"
fi
COMMAND="$RMFS -f $CREDS $3test"
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi
