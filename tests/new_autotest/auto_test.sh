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

rm $attchmnt
}

export JAVA_HOME=/opt/jdk1.6.0_13
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
revision=""
usessl=""


while getopts “hdsr:” OPTION
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
             usessl=" /home/bjko/test/certs/al/al.p12 "
	     echo "using SSL for all tests!"
             ;;
         r)
             revision=" -r ${OPTARG} "
	     echo "using revision $revision"
             ;;
         ?)
             usage
             exit
             ;;
     esac
done


date >> $testlog
cd $svndir
echo "CHECKOUT SVN SOURCES..." >> $testlog
svn -q $revision co "http://xtreemfs.googlecode.com/svn/trunk" >> $testlog 2>&1
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

#rm -rf $svndir
