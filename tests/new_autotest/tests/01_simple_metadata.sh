#!/bin/bash
. $TEST_BASEDIR/tests/utilities.inc

cleanup() {
	rm -rf $MOUNT_DIR/1/*
	print_summary_message $1 'simple_metadata' "mnt_1"
	exit $1
}

#use first mounted volume for metadata tests
tmp=($VOLUMES)
DIRNAME=${tmp[0]}

echo "simple metadata only operations test in $DIRNAME"

echo -n "mkdir..."
mkdir -p $DIRNAME/testdir/subdir/moredir || cleanup 1 ; echo "OK"

if [ ! -d $DIRNAME/testdir/subdir/moredir ]
then
	echo "FAILED. mkdir did not create directory!"
	cleanup 1;
fi
	
echo -n "rmdir..."
rmdir $DIRNAME/testdir/subdir/moredir || cleanup 1 ; echo "OK"

if [ -e $DIRNAME/testdir/subdir/moredir ]
then
	echo "FAILED. rmdir did not remove directory!"
	cleanup 1;
fi

echo -n "rename dir..."
mv $DIRNAME/testdir $DIRNAME/renamed || cleanup 1 ; echo "OK"

if [ -e $DIRNAME/testdir ]
then
	echo "FAILED. rename did not remove old entry!"
	cleanup 1;
fi

if [ ! -e $DIRNAME/renamed ]
then
	echo "FAILED. rename did not create new entry!"
	cleanup 1;
fi

echo -n "touch file..."
touch $DIRNAME/testfile || cleanup 1 ; echo "OK"

if [ ! -e $DIRNAME/testfile ]
then
	echo "FAILED. touch did not create file!"
	cleanup 1;
fi

echo -n "delete file..."
rm $DIRNAME/testfile || cleanup 1 ; echo "OK"

if [ -e $DIRNAME/testfile ]
then
	echo "FAILED. unlink did not remove file!"
	cleanup 1;
fi

echo -n "touch file..."
touch $DIRNAME/testfile2 || cleanup 1 ; echo "OK"

if [ ! -e $DIRNAME/testfile2 ]
then
	echo "FAILED. touch did not create file!"
	cleanup 1;
fi

echo -n "rename file..."
mv $DIRNAME/testfile2 $DIRNAME/renamedfile || cleanup 1 ; echo "OK"

if [ -e $DIRNAME/testfile2 ]
then
	echo "FAILED. rename did not remove old entry!"
	cleanup 1;
fi

if [ ! -e $DIRNAME/renamedfile ]
then
	echo "FAILED. rename did not create new entry!"
	cleanup 1;
fi

echo -n "delete file..."
rm $DIRNAME/renamedfile || cleanup 1 ; echo "OK"

if [ -e $DIRNAME/renamedfile ]
then
	echo "FAILED. unlink did not remove file!"
	cleanup 1;
fi

echo -n "recreate file..."
touch $DIRNAME/testfile2 || cleanup 1 ; echo "OK"

if [ ! -e $DIRNAME/testfile2 ]
then
	echo "FAILED. touch did not create file!"
	cleanup 1;
fi

echo -n "ls..."
ls $DIRNAME > /dev/null || cleanup 1 ; echo "OK"

echo -n "chmod file..."
chmod a+rw $DIRNAME/testfile2 || cleanup 1; echo "OK"

echo -n "create softlink..."
ln -s $DIRNAME/testfile2 $DIRNAME/softlink || cleanup 1; echo "OK"

if [ ! -e $DIRNAME/softlink ]
then
	echo "FAILED. symlink did not create new entry!"
	cleanup 1;
fi

mv $DIRNAME/softlink $DIRNAME/softlink2

target=`readlink $DIRNAME/softlink2`
target=`basename $target`
if [ $target != "softlink" ]
then
	echo "FAILED. softlink2 has wrong target after rename: $target!"
	cleanup1;
fi

echo -n "create hardlink..."
ln $DIRNAME/testfile2 $DIRNAME/hardlink || cleanup 1; echo "OK"

if [ ! -e $DIRNAME/hardlink ]
then
	echo "FAILED. hardlink did not create new entry!"
	cleanup 1;
fi

echo -n "delete softlink..."
rm $DIRNAME/softlink || cleanup 1; echo "OK"

if [ -e $DIRNAME/softlink ]
then
	echo "FAILED. unlink did not remove softlink!"
	cleanup 1;
fi
 
echo -n "delete hardlink..."
rm $DIRNAME/hardlink || cleanup 1; echo "OK"

if [ -e $DIRNAME/softlink ]
then
	echo "FAILED. unlink did not remove hardlink!"
	cleanup 1;
fi

cleanup 0
