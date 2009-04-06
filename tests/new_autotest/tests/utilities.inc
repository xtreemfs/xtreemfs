#!/bin/bash
print_summary_message() {
	volname=`basename $3`
	if [ $1 -eq 0 ]
	then
		printf "%-30s %-15s %5s\n" $2 $volname "ok" >> $TEST_SUMMARY
	else
		printf "%-30s %-15s %5s\n" $2 $volname "FAILED" >> $TEST_SUMMARY
	fi
}