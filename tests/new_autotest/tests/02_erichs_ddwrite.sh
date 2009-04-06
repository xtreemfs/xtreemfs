#!/bin/bash
. $TEST_BASEDIR/tests/utilities.inc

echo "erichs ddwrite test..."

for volume in $VOLUMES
do
	echo "writing in volume $volume..."
	for (( c=1; c<=16 ; c=c*2 )); do
		echo "  --- 10MB, $c client(s) ---"
		$TEST_BASEDIR/tests/ddwrite_helper.inc 10 $volume/t10MB$c $c
		if [ $? -ne 0 ]; then
			echo "test failed!"
			print_summary_message 1 'dd_write' $volume
			rm -rf $volume*
			exit 1;
		fi
	done
	print_summary_message 0 'dd_write' $volume
done