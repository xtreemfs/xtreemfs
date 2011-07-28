#!/bin/bash

TEST_DIR=$4
export XTREEMFS=$1
echo "XTREEMFS=$XTREEMFS"

if [[ "$3" == pbrpcs://* || "$3" == pbrpcg://** ]]; then
  CREDS="-c $1/tests/certs/Client.p12 -cpass passphrase -t $1/tests/certs/trusted.jks"
fi

# test dump
COMMAND="$1/bin/xtfs_mrcdbtool -mrc $3 $CREDS dump $TEST_DIR/dump.xml"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# check the dump
cat $TEST_DIR/dump.xml |grep "<filesystem "

rm $TEST_DIR/dump.xml