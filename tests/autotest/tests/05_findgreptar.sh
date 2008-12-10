#!/bin/bash
. tests/utilities.inc.sh

echo "untar, find and grep test..."

for volume in $NONDIRECT_VOLUMES $VOLUMES
do
	echo -n "untarring includes in volume $volume..."
	testdir=`pwd`
	cd $volume
	tar xzf $testdir/httperf-0.8.tar.gz
	if [ $? -ne 0 ]; then
		echo "cannot untar includes into $volume!"
		print_summary_message 1 "tar/find/grep" $volume
		exit 1
	fi
	echo "OK"

	echo -n "search for *.cpp files..."
	find . -name "*.cpp"
	if [ $? -ne 0 ]; then
		echo "cannot execute find!"
		print_summary_message 1 "tar/find/grep" $volume
		exit 1
	fi
	echo "OK"

	echo -n "grep -R for 'ttest'..."
	grep -R 'ttest' .
	if [ $? -gt 1 ]; then
		echo "cannot execute grep!"
		print_summary_message 1 "tar/find/grep" $volume
		exit 1
	fi
	echo "OK"

	echo -n "cleaning up..."
	rm -r $volume/*
	if [ $? -ne 0 ]; then
		ls -laR $volume/
		echo "cannot cleanup directory!"
		print_summary_message 1 "tar/find/grep" $volume
		exit 1
	fi
	echo "OK"
	print_summary_message 0 "tar/find/grep" $volume

	cd $testdir
done