#!/bin/bash
. $TEST_BASEDIR/tests/utilities.inc
. $TEST_BASEDIR/binaries.sh

echo "running dbench with 5 clients on direct_io volumes ..."

for volume in $VOLUMES
do
	echo "dbench on volume $volume..."
	$DBENCH_BIN -c $DBENCH_CLIENT -D $volume 5

	if [ $? -ne 0 ]
	then
		echo "ERROR: dbench failed on $volume!"
		rm -rf $volume/*
		print_summary_message 1 "DBench" $volume
		exit 1
	fi
	rm -rf $volume/*
	print_summary_message 0 "DBench" $volume
done
exit 0
