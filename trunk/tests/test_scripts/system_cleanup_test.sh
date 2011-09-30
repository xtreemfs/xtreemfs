#!/bin/bash

TEST_DIR=$4
export XTREEMFS=$1
echo "XTREEMFS=$XTREEMFS"

if [[ "$2" == pbrpcs://* || "$2" == pbrpcg://* ]]; then
  CREDS="-c $1/tests/certs/Client.p12 -cpass passphrase -t $1/tests/certs/trusted.jks"
fi

#
# clean volume with one file
#

# create file
echo "test" > file.txt

# execute cleanup command
COMMAND="$1/bin/xtfs_cleanup -dir $2 -i -r $CREDS uuid:test-osd0"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# execute cleanup command
COMMAND="$1/bin/xtfs_cleanup -dir $2 -wait -r $CREDS uuid:test-osd0"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi