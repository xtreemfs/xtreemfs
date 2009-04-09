#!/bin/bash
. $TEST_BASEDIR/tests/utilities.inc

cleanup() {
	rm -rf $MOUNT_DIR/1/*
	print_summary_message $1 'makextreemfs' "nondirect_1"
	exit $1
}

#use first mounted volume for metadata tests
tmp=($NONDIRECT_VOLUMES)
DIRNAME=${tmp[0]}

currentdir=`pwd`
cd $DIRNAME

svn co http://xtreemfs.googlecode.com/svn/trunk/client
if [ $? -ne 0 ]
then
	echo "ERROR: client checkout failed!"
	cleanup 1
fi

cd client
if [ $? -ne 0 ]
then
	echo "ERROR: cannot cd to client directory!"
	cleanup 1
fi

python scons.py
if [ $? -ne 0 ]
then
	echo "ERROR: client build failed!"
	cleanup 1
fi

bin/xtfs_lsvol localhost
if [ $? -ne 0 ]
then
	echo "ERROR: cannot execute xtfs_lsvol!"
	cleanup 1
fi

cleanup 0