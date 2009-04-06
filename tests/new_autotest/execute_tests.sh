#!/bin/bash

if [ $# -ne 1 ]
then
	usage $0;
	exit 1;
fi

TEST_DIR=$1

. $TEST_DIR/globals.sh

#execute_tests

echo -e "\n\n================== STARTING TESTS ====================\n\n"

result=0

for testfile in $TEST_BASEDIR/tests/*.sh
do
	echo -e "TEST: $testfile\n"

	$testfile

	if [ $? -ne 0 ]; then
		result=1
		echo -e "FAILED: $testfile\n"
		break;
	fi

	echo -e "\n-----------------------------------------------------\n"
done

if [ $result -eq 0 ]; then
	echo -e "\n\n================== S U C C E S S ====================\n\n"
else
	echo -e "\n\n=================== F A I L E D =====================\n\n"
fi