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
	
cat $TEST_SUMMARY > $attchmnt
echo "" >> $attchmnt
cat $testlog >> $attchmnt

mailx -a $attchmnt -s "$subject" -r "your friendly XtreemFS test robot <kolbeck@zib.de>" xtreemfs-test@googlegroups.com<< EOF
$subject

The logfile of this test run is attached to this email.
Logfiles and databases can be found in $WKDIR on $hostname.

EOF

rm $attchmnt $testlog
}

export JAVA_HOME=/opt/jdk1.6.0_02
export ANT_HOME=`pwd`"/apache-ant-1.7.0/"

rm -rf /scratch/disk1/xtreemfs_test/*

hostname=`hostname -a`

testlog=`mktemp /scratch/disk1/xtreemfs_autotest_XXXXXXXXXX`.txt

export TEST_SUMMARY=`mktemp /scratch/disk1/xtreemfs_autotest_summary_XXXXXXXXXX`.txt

attchmnt=`mktemp /scratch/disk1/xtreemfs_attachmnt_XXXXXXXXXX`.txt

svndir=`mktemp -d /scratch/disk1/xtreemfssrc_XXXXXXXXXX`

#check out sources
testdir=`pwd`

toScreen=0
if [ $# -gt 0 ]
then
	toScreen=1
fi
revision=""
if [ $# -gt 1 ]
then
	revision=" -r $2 "
	echo "using revision $revision"
fi

usessl=""
if [ $# -gt 2 ]
then
	usessl=" /home/bjko/test/certs/al/al.p12 "
	echo "using SSL for all tests!"
fi

if [ $toScreen -eq 1 ]; then
	echo "results will be printed to screen!"
fi


date >> $testlog
cd $svndir
echo "CHECKOUT SVN SOURCES..." >> $testlog
svn $revision co "http://xtreemfs.googlecode.com/svn/trunk" >> $testlog 2>&1
if [ $? -ne 0 ]; then
	echo "FAILED: cannot checkout sources!" >> $testlog
	if [ $1 = "x" ]; then
		cat $testlog
	else
		date >> $testlog
		sendresult 1
	fi
	rm -rf $svndir
	exit
fi

cd trunk

cd client
grep -v 'XTREEMOS_ENV = y' make.config > make.config.tmp
mv make.config.tmp make.config
cd ..

echo "COMPILING..." >> $testlog
make >> $testlog 2>&1
if [ $? -ne 0 ]; then
	echo "FAILED: cannot make sources!" >> $testlog
	if [ $toScreen -ne 0 ]; then
		cat $testlog
	else
		date >> $testlog
		sendresult 1
	fi
	rm -rf $svndir
	exit
fi	

cd $testdir

./tests.sh $svndir/trunk/ $usessl >> $testlog 2>&1
result=$?
if [ $toScreen -ne 0 ]; then
	cat $testlog
else
	date >> $testlog
	sendresult $result
fi

rm -rf $svndir
