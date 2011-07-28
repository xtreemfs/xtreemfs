#!/bin/bash

if [[ "$3" == pbrpcs://* || "$3" == pbrpcg://* ]]; then
  CREDS="--pkcs12-file-path $1/tests/certs/Client.p12 --pkcs12-passphrase passphrase"
fi

# test mkfs.xtreemfs
COMMAND="$1/bin/mkfs.xtreemfs $CREDS $3test"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# test lsfs.xtreemfs
COMMAND="$1/bin/lsfs.xtreemfs $CREDS $3"
echo "Running ${COMMAND}..."
$COMMAND | grep test
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# test rmfs.xtreemfs
COMMAND="$1/bin/rmfs.xtreemfs $CREDS $3test"
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi