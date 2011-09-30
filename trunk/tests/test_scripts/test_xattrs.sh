#!/bin/bash

# test getfattr -d
COMMAND="getfattr -d -m x ."
echo "Running ${COMMAND}..."
$COMMAND | grep "xtreemfs.url"
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# test setfattr
COMMAND="setfattr -n key -v value ."
echo "Running ${COMMAND}..."
$COMMAND
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

# test getfattr -n
COMMAND="getfattr --only-values -n key ."
echo "Running ${COMMAND}..."
VALUE=`$COMMAND`
RESULT=$?
if [ "$RESULT" -ne "0" ]; then echo "$COMMAND failed"; exit $RESULT; fi

echo $VALUE
if [ "$VALUE" != "value" ]; then echo "could not retrieve extended attribute for file: expected=value, actual=$VALUE"; exit 1; fi