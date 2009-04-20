#!/bin/bash

usage() {
	myname=`basename $1`
	echo "$myname <test directory>"
	echo "unmounts all clients and shuts down the servers."
	echo ""
}

shutdown_services() {
	$TEST_BASEDIR/utils/service.sh ignoreme $TEST_DIR dir stop
	$TEST_BASEDIR/utils/service.sh ignoreme $TEST_DIR mrc stop
	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		$TEST_BASEDIR/utils/service.sh ignoreme $TEST_DIR osd$i stop
	done
}

do_unmount() {
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "unmounting volume test_$i ..."

		if [ `grep -c "$TEST_DIR/mnt/$i" /proc/mounts` -gt 0 ]; then
			echo "Unmounting volume test_$i"
			fusermount -u $TEST_DIR/mnt/$i
		else
			echo "volume test_$i not mounted in $TEST_DIR/mnt/$i, probably crashed"
		fi

		if [ `grep -c "$TEST_DIR/mnt/nondirect_$i" /proc/mounts` -gt 0 ]; then
			echo "Unmounting volume test_$i (nondirect)"
			fusermount -u $TEST_DIR/mnt/nondirect_$i
		else
			echo "volume test_$i not mounted in $TEST_DIR/mnt/nondirect_$i, probably crashed"
		fi

	done
}

if [ $# -ne 1 ]
then
	usage $0;
	exit 1;
fi

tmp=`readlink -f $0`
export TEST_BASEDIR=`dirname $tmp`

TEST_DIR=$1
TEST_DIR=${TEST_DIR%/}

NUM_OSDS=`ls $TEST_DIR/run/osd* | wc -l`

do_unmount

shutdown_services

echo "test environment stopped"
