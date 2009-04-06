#!/bin/bash
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
		print_summary_message 1 "bonnie64" $volume
		exit 1
	fi
	print_summary_message 0 "bonnie64" $volume
done