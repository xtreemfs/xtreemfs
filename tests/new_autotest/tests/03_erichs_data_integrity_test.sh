#!/bin/bash
. $TEST_BASEDIR/tests/utilities.inc

echo "erichs data integrity test..."

for volume in $VOLUMES
do
	echo "writing 20x1MB files with marked data in $volume, checking data "
	$TEST_BASEDIR/tests/marked_block.pl --start=1 --nfiles=20 --size=1 \
	--group=10 --base=$volume/integritytest
	if [ $? -ne 0 ]; then
		echo "test failed!"
		rm -rf $volume/*
		print_summary_message 1 'data_integrity' $volume
		exit 1;
	fi
	rm -rf $volume/*
	print_summary_message 0 'data_integrity' $volume
done
