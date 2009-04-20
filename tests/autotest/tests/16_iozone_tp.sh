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

echo "running iozone throughput test..."

for volume in $VOLUMES
do
	echo "iozone on volume $volume..."
	dir=`pwd`
	cd $volume
	if [ $? -ne 0 ]
	then
		cd $dir
		echo "ERROR: cannot chdir to $volume! client crashed?"
		print_summary_message 1 "IOZoneTP" $volume
		exit 1
	fi


	$IOZONE_BIN -t 1 -r 128k -s 20m
	if [ $? -ne 0 ]
	then
		cd $dir
		echo "ERROR: IOZone failed on $volume!"
		rm -rf $volume/*
		print_summary_message 1 "IOZoneTP" $volume
		exit 1
	fi

	$IOZONE_BIN -t 5 -r 128k -s 20m
	if [ $? -ne 0 ]
	then
		cd $dir
		echo "ERROR: IOZone failed on $volume!"
		rm -rf $volume/*
		print_summary_message 1 "IOZoneTP" $volume
		exit 1
	fi

	$IOZONE_BIN -t 10 -r 128k -s 20m
	if [ $? -ne 0 ]
	then
		cd $dir
		echo "ERROR: IOZone failed on $volume!"
		rm -rf $volume/*
		print_summary_message 1 "IOZoneTP" $volume
		exit 1
	fi
	rm -rf $volume/*
	print_summary_message 0 "IOZoneTP" $volume
	cd $dir
done