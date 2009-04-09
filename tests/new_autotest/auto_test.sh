#!/bin/bash
export LANG=en_GB.UTF-8

sendresult() {
	local result=$1
	if [ $result -eq 0 ]
	then
		subject="SUCCESS: XtreemFS automatic test"
	else
		subject="FAILED: XtreemFS automatic test"
	fi
	
cat $TEST_DIR/test_summary > $ATTACHMENT
echo "" >> $ATTACHMENT
cat $TEST_LOG >> $ATTACHMENT

mailx -a $ATTACHMENT -s "$subject" -r "your friendly XtreemFS test robot <kolbeck@zib.de>" xtreemfs-test@googlegroups.com<< EOF
$subject

The logfile of this test run is attached to this email.
Logfiles and databases can be found in $WKDIR on $hostname.

EOF

rm $ATTACHMENT
}

export JAVA_HOME=/opt/jdk1.6.0_13

rm -rf /scratch/disk1/autotest/*
mkdir -p /scratch/disk1/autotest/

XTREEMFS_DIR=`mktemp -d /scratch/disk1/autotest/xtreemfssrc_XXXXXXXXXX`

TEST_DIR=`mktemp -d /scratch/disk1/autotest/test_XXXXXXXXXX`

TEST_LOG="$TEST_DIR/testlog.txt"

ATTACHMENT="$TEST_DIR/summary_and_testlog.txt"

toScreen=0
revision=""
usessl=""
optionalFlags=""

tmp=`readlink -f $0`
TEST_BASEDIR=`dirname $tmp`


while getopts “hdsr:f:” OPTION
do
     case $OPTION in
         h)
             echo "-s for ssl, -d for output to screen and -r <num> for revision <num>"
             exit 1
             ;;
         d)
             toScreen=1
	     echo "results will be printed to screen!"
             ;;
	 s)
             usessl=" -s "
	     echo "using SSL for all tests!"
             ;;
         r)
             revision=" -r ${OPTARG} "
	     echo "using revision $revision"
             ;;
	 f)
	     optionalFlags=$OPTARG
	     ;;
         ?)
             usage
             exit
             ;;
     esac
done


date >> $TEST_LOG
cd $XTREEMFS_DIR
echo "CHECKOUT SVN SOURCES..." >> $TEST_LOG
svn -q $revision co "http://xtreemfs.googlecode.com/svn/trunk" >> $TEST_LOG 2>&1
if [ $? -ne 0 ]; then
	echo "FAILED: cannot checkout sources!" >> $TEST_LOG
	if [ $toScreen = "x" ]; then
		cat $TEST_LOG
	else
		date >> $TEST_LOG
		sendresult 1
	fi
	exit
fi

cd trunk

echo "COMPILING..." >> $TEST_LOG
make >> $TEST_LOG 2>&1
if [ $? -ne 0 ]; then
	echo "FAILED: cannot make sources!" >> $TEST_LOG
	if [ $toScreen -ne 0 ]; then
		cat $TEST_LOG
	else
		date >> $TEST_LOG
		sendresult 1
	fi
	exit
fi	

$TEST_BASEDIR/start_environment.sh $optionalFlags -d 2 -o 3 $usessl $XTREEMFS_DIR/trunk/ $TEST_DIR >> $TEST_LOG 2>&1
result=$? 2>&1

if [ $result -eq 0 ]
then
	$TEST_BASEDIR/execute_tests.sh $TEST_DIR >> $TEST_LOG 2>&1
	result=$?
fi

$TEST_BASEDIR/stop_environment.sh $TEST_DIR >> $TEST_LOG 2>&1


if [ $toScreen -ne 0 ]; then
	cat $TEST_LOG
else
	date >> $TEST_LOG
	sendresult $result
fi

