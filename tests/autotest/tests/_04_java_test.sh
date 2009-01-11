#!/bin/bash
. tests/utilities.inc.sh

echo "tar/javac/java test..."

for volume in $NONDIRECT_VOLUMES
do
	echo -n "untarring jre in volume $volume..."
	testdir=`pwd`
	cd $volume
	tar --no-same-owner --no-same-permissions -xzf $testdir/jre160.tgz
	if [ $? -ne 0 ]; then
		echo "cannot untar JRE into $volume!"
		print_summary_message 1 "tar/javac/jar" $volume
		exit 1
	fi
	echo "OK"
	cp $testdir/HelloWorld.class .

	echo -n "executing HelloWorld.java..."
	jre1.6.0_03/bin/java HelloWorld
	if [ $? -ne 0 ]; then
		echo "cannot execute HelloWorld!"
		print_summary_message 1 "tar/javac/jar" $volume
		exit 1
	fi
	echo "OK"

	sleep 10
	lsof jre1.6.0_03/bin/java

	rm -r --interactive=never $volume/*
	if [ $? -ne 0 ]; then
		echo "cannot cleanup directory!"
		print_summary_message 1 "tar/javac/jar" $volume
		lsof +D $volume
		ls -lR $volume/
		tail -c4096 $WKDIR/mrc_operations.log
		exit 1
	fi
	echo "OK"
	print_summary_message 0 "tar/javac/jar" $volume

	cd $testdir
done
