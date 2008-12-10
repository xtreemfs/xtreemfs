#!/bin/bash
. tests/utilities.inc.sh

cleanup() {
	rm -rf $WKDIR/mnt_1/*
	print_summary_message $1 'simple_metadata' "mnt_1"
	exit $1
}

#use first mounted volume for metadata tests
tmp=($VOLUMES)
DIRNAME=${tmp[0]}

echo "simple metadata only operations test in $DIRNAME"

echo -n "mkdir..."
mkdir -p $DIRNAME/testdir/subdir/moredir || cleanup 1 ; echo "OK"

echo -n "rmdir..."
rmdir $DIRNAME/testdir/subdir/moredir || cleanup 1 ; echo "OK"

echo -n "rename dir..."
mv $DIRNAME/testdir $WKDIR/mnt_1/renamed || cleanup 1 ; echo "OK"

echo -n "touch file..."
touch $DIRNAME/testfile || cleanup 1 ; echo "OK"

echo -n "delete file..."
rm $DIRNAME/testfile || cleanup 1 ; echo "OK"

echo -n "touch file..."
touch $DIRNAME/testfile2 || cleanup 1 ; echo "OK"

echo -n "rename file..."
mv $DIRNAME/testfile2 $DIRNAME/renamedfile || cleanup 1 ; echo "OK"

echo -n "delete file..."
rm $DIRNAME/renamedfile || cleanup 1 ; echo "OK"

echo -n "recreate file..."
touch $DIRNAME/testfile2 || cleanup 1 ; echo "OK"

echo -n "ls..."
ls $DIRNAME > /dev/null || cleanup 1 ; echo "OK"

echo -n "chmod file..."
chmod a+rw $DIRNAME/testfile2 || cleanup 1; echo "OK"

echo -n "create softlink..."
ln -s $DIRNAME/testfile2 $DIRNAME/softlink || cleanup 1; echo "OK"

echo -n "create hardlink..."
ln $DIRNAME/testfile2 $DIRNAME/hardlink || cleanup 1; echo "OK"

echo -n "delete softlink..."
rm $DIRNAME/softlink || cleanup 1; echo "OK"
 
echo -n "delete hardlink..."
rm $DIRNAME/hardlink || cleanup 1; echo "OK"

cleanup 0
