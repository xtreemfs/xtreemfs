#!/bin/bash

usage() {
	myname=`basename $1`
	echo "$myname <test directory>"
	echo "runs all tests"
	echo ""
}

if [ $# -ne 1 ]
then
	usage $0;
	exit 1;
fi

TEST_DIR=$1


#execute_tests

echo -e "\n\n================== STARTING TESTS ====================\n\n"

echo -e "started on `date`"

result=0

for testfile in $TEST_BASEDIR/tests/*.sh
do
	echo -e "TEST: $testfile\n"
	echo -e "start: `date`\n"

	$testfile $TEST_DIR

	if [ $? -ne 0 ]; then
		result=1
		echo -e "FAILED: $testfile\n"
	fi

    echo -e "\n done: `date`\n"
	echo -e "\n-----------------------------------------------------\n"
done

echo -e "finished on `date`"

if [ $result -eq 0 ]; then
	echo -e "\n\n================== S U C C E S S ====================\n\n"
else
	echo -e "\n\n=================== F A I L E D =====================\n\n"
fi
exit $result