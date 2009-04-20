#!/bin/bash
if [ $# -ne 1 ]
then
	echo "$0 <test directory>";
	exit 1;
fi

TEST_DIR=$1

. $TEST_DIR/globals.sh

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

if [ $SSL_ENABLED -eq 0 ]
then
	echo "list all volumes on the local MRC..."
	bin/xtfs_lsvol localhost
else
	echo "list all volumes on the local MRC (with SSL)..."
	bin/xtfs_lsvol --pkcs12-file-path=$DIRNAME/servers/test/certs/Client.p12 --pkcs12-passphrase=passphrase localhost
fi
	
if [ $? -ne 0 ]
then
	echo "ERROR: cannot execute xtfs_lsvol!"
	cleanup 1
fi

cleanup 0
