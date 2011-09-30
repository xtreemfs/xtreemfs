#!/bin/bash

TEST_DIR=$4
export XTREEMFS=$1
echo "XTREEMFS=$XTREEMFS"

if [[ "$2" == pbrpcs://* || "$2" == pbrpcg://* ]]; then
  CREDS="-c $1/tests/certs/Client.p12 -cpass passphrase -t $1/tests/certs/trusted.jks -tpass passphrase"
fi

#
# get status
#

# execute chstatus command
COMMAND="$1/bin/xtfs_chstatus -dir $2 $CREDS test-osd0"
echo "Running ${COMMAND}..."
$COMMAND | grep "online"
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

#
# set OSD 'locked'
#

COMMAND="$1/bin/xtfs_chstatus -dir $2 $CREDS test-osd0 locked"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

COMMAND="$1/bin/xtfs_chstatus -dir $2 $CREDS test-osd0"
echo "Running ${COMMAND}..."
$COMMAND | grep "locked"
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

#
# restore old 'online' status
#

COMMAND="$1/bin/xtfs_chstatus -dir $2 $CREDS test-osd0 online"
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# execute chstatus command
COMMAND="$1/bin/xtfs_chstatus -dir $2 $CREDS test-osd0"
echo "Running ${COMMAND}..."
$COMMAND | grep "online"
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi