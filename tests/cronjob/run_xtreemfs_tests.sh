#!/bin/bash

# Copyright (c) 2006-2011 by Björn Kolbeck, Zuse Institute Berlin
#               2011-2013 by Michael Berlin, Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.


# This script downloads the latest XtreemFS sources and runs the integration tests.
# As of April 2013, we run this script internally every night. The results of the
# tests are posted to the internal mailing list xtreemfs-test@googlegroups.com.
#
# Run it as cron job as follows: /usr/bin/wget https://raw.githubusercontent.com/xtreemfs/xtreemfs/master/tests/cronjob/run_xtreemfs_tests.sh -q -O /tmp/run_xtreemfs_tests.sh && bash -l /tmp/run_xtreemfs_tests.sh

# Environment
export LANG=en_US.UTF-8

# Global variables
TEST_ID=`date +%Y%m%dT%H%M%S`
DIR_PREFIX="/scratch/autotests"
XTREEMFS_DIR="$DIR_PREFIX/checkouts/xtreemfs_checkout-$TEST_ID"
TEST_DIR="$DIR_PREFIX/tests/xtreemfs_test-$TEST_ID"
TEST_LOG="$TEST_DIR/test.log"
TEST_SUMMARY="$TEST_DIR/summary.log"
ATTACHMENT="$TEST_DIR/summary_and_testlog.txt"
ATTACHMENT_ZIP_LOGS="$TEST_DIR/client_and_server_logs.zip"

if [ ! -d "$DIR_PREFIX" ]
then
  mkdir -p "$DIR_PREFIX"
fi
if [ ! -d "$DIR_PREFIX" ]
then
  echo "ERROR: $DIR_PREFIX does not exist."
  exit 1
fi


# Helper functions
sendresult() {
  local result=$1
  if [ $result -eq 0 ]
  then
          subject="SUCCESS: XtreemFS automatic test"
  else
          subject="FAILED: XtreemFS automatic test"
  fi
  cat $TEST_LOG > $ATTACHMENT
  echo "" >> $ATTACHMENT
  if [ -e $TEST_SUMMARY ]
  then
    cat $TEST_SUMMARY >> $ATTACHMENT
  fi

  # Pack log files as .zip archive
  if [ -d "$TEST_DIR""/log" ]
  then
    # Work-around the problem that 'zip' always stores the complete path :(
    current_dir="$PWD"
    cd "$TEST_DIR""/log"
    zip -r -q -9 $ATTACHMENT_ZIP_LOGS *.log
    cd "$current_dir"
  fi

  if [ -f "$ATTACHMENT_ZIP_LOGS" ]
  then
    mailx_additional_params="-a $ATTACHMENT_ZIP_LOGS"
  fi
  mailx -a $ATTACHMENT $mailx_additional_params -s "$subject" xtreemfs-test@googlegroups.com<< EOF
$subject

The logfile of this test run and the logs of servers and clients are attached to this email.
Logfiles and databases can be found in: $TEST_DIR

EOF

  rm $ATTACHMENT
  if [ -f $ATTACHMENT_ZIP_LOGS ]
  then
    rm $ATTACHMENT_ZIP_LOGS
  fi
}

# Kill any remaining test runs
killall python /usr/bin/python iozone &>/dev/null && sleep 5
killall -9 python /usr/bin/python iozone &>/dev/null
killall -9 mount.xtreemfs &>/dev/null
killall -9 java &>/dev/null
for mount in $(mount|grep ^xtreemfs@|cut -d" " -f3)
do
  fusermount -u "$mount" &>/dev/null
done

# Current directories already exist? Remove them first.
if [ -d "$TEST_DIR" ]
then
  rm -r "$TEST_DIR"
fi
if [ -d "$XTREEMFS_DIR" ]
then
  rm -r "$XTREEMFS_DIR"
fi

# Cleanup test directory if disk is full
min_free_space_mb=30000 # Remove all tests until enough space is available
min_free_inodes=2000000
while true
do
  # Enough free space?
  free_space_mb=$(df -Pm $DIR_PREFIX | grep -v ^Filesystem | awk '{ print $4 }')
  free_inodes=$(df -i $DIR_PREFIX | grep -v ^Filesystem | awk '{ print $4 }')
  if [ $free_space_mb -ge $min_free_space_mb ] && [ $free_inodes -ge $min_free_inodes ]
  then
    break
  fi

  echo -n "Not enough free space/inodes:"
  echo -n "requiring at least ${min_free_space_mb}M of free space (got ${free_space_mb}M)"
  echo    " and at least ${min_free_inodes} free inodes (got ${free_inodes})."

  # Any dirs left to delete?
  for dir in "$DIR_PREFIX/checkouts/" "$DIR_PREFIX/tests/"
  do
    oldest_dir=$(ls -1At "$dir" | tail -n1)
    if [ -n "$oldest_dir" ]
    then
      echo "Deleting ${dir}${oldest_dir} ..."
      rm -rf "$dir""$oldest_dir" || break
    fi
  done
done

# Create directories
mkdir -p $DIR_PREFIX
mkdir -p $XTREEMFS_DIR
mkdir -p $TEST_DIR

# Check out using SSH for passwordless push using deploy keys.
cd $XTREEMFS_DIR
git clone git@github.com:xtreemfs/xtreemfs.git . &> $TEST_LOG

# Compile
# 2012-11-02(mberlin): Try to disable optimizations in client compilation.
# Build client unit tests.
export BUILD_CLIENT_TESTS=true
export CPPFLAGS=-O0
make client_debug server hadoop-client &>$TEST_LOG
if [ $? -ne 0 ]; then
  echo "FAILED: cannot make sources!" >> $TEST_LOG
  date >> $TEST_LOG
  sendresult 1
  exit
fi

# Run xtfs_test
# rm $TEST_LOG
cd $XTREEMFS_DIR/tests
python -u xtestenv -t $TEST_DIR full &> $TEST_SUMMARY
result=$?
sendresult $result
