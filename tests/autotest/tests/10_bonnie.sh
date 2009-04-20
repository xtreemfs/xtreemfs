#!/bin/bash
if [ $# -ne 1 ]
then
	echo "$0 <test directory>";
	exit 1;
fi

TEST_DIR=$1

. $TEST_DIR/globals.sh

. $TEST_BASEDIR/tests/utilities.inc
. $TEST_BASEDIR/binaries.sh

echo "running Bonnie64 on all volumes (direct_io and non-direct)..."

for volume in $VOLUMES $NONDIRECT_VOLUMES
do
	echo "bonnie on volume $volume..."
	$BONNIE_BIN -d $volume -s 100
	if [ $? -ne 0 ]
	then
		echo "ERROR: bonnie failed on $volume!"
		rm -rf $volume/*
		print_summary_message 1 "bonnie64" $volume
		exit 1
	fi
	rm -rf $volume/*
	print_summary_message 0 "bonnie64" $volume
done