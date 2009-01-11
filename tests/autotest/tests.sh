#!/bin/bash
#
# Driver for automated testing of XtreemFS.
# Requires utils/* for creating server configs and starting the servers.
# Servers will run on localhost, with storage from the locally mounted
# filesystem(s).
#
# Usage:
#      ./tests.sh <PATH_TO_XTREEMFS_TREE> [SSL_CERTIFICATE_FILE]
#
# When a certificate file isn't specified, the tests will run without SSL.
#
# Tests are executed from the tests/ subdirectory, only scripts with names
# matching [0-9][0-9]_*.sh are executed.
# By default 3 (three) OSDs will be created, and the stripe size will be set
# to 128KB. This can be changed by setting the environment variables
# $NUM_OSDS and $STRIPE_WIDTH to other values before starting the tests.
#
# $Id$
#

create_config() {
	mkdir -p $WKDIR/config
	if [ $? -ne 0 ]; then
		echo "ERROR: cannot create config directory $WKDIR/config"
		exit 1
	fi
	
	if [ "$USECERT" != "" ]
	then
		USESSL=" use_ssl"
	else
		USESSL=""
	fi

	#create configs
	utils/create_testconfig.pl dir $JAVA_DEBUG localhost default $WKDIR default $USESSL> $WKDIR/config/dir.properties
	utils/create_testconfig.pl mrc $JAVA_DEBUG localhost default $WKDIR default $USESSL> $WKDIR/config/mrc.properties
	
	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		utils/create_testconfig.pl osd$i $JAVA_DEBUG localhost default $WKDIR default $USESSL > $WKDIR/config/osd$i.properties
	done

}

startup_services() {
	utils/service.sh -w $WKDIR -x $XTREEMFS dir start
	if [ $? -ne 0 ]; then
		echo "FAILED: cannot start dir"; exit 1
	fi
	sleep 1

	utils/service.sh -w $WKDIR -x $XTREEMFS mrc start
	if [ $? -ne 0 ]; then
		echo "FAILED: cannot start mrc"; shutdown_services; exit 1
	fi
	sleep 1 

	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		utils/service.sh -w $WKDIR -x $XTREEMFS osd$i start
		if [ $? -ne 0 ]; then
			echo "FAILED: cannot start osd$i"; shutdown_services; exit 1
		fi
	done
	
}

shutdown_services() {
	utils/service.sh -w $WKDIR dir stop
	utils/service.sh -w $WKDIR mrc stop
	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		utils/service.sh -w $WKDIR osd$i stop
	done
}

do_mount() {
	VOLUMES=""
	if [ $USECERT != "" ]
	then
		sslmnt="-o ssl_cert=$USECERT "
		sslmkvol=" -c $USECERT "
		schema="https://"
	else
		sslmnt=""
		sslmkvol=""
		schema="http://"
	fi
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "creating volume test_$i ..."

		mkdir $WKDIR/mnt_$i
		$XTREEMFS/client/bin/xtfs_mkvol $sslmkvol -p RAID0,$STRIPE_WIDTH,$i ${schema}localhost/test_$i
		if [ $? -ne 0 ]; then
			echo "FAILED: cannot create volume test_$i"
			shutdown_services; do_unmount; exit 1
		fi

		echo "mounting volume test_$i in mnt_$i..."

		#$XTREEMFS/bin/xtfs_mount -f $CLIENT_DEBUG $sslmnt\
		#-o volume_url=${schema}localhost/test_$i \
		#-o direct_io,logfile=$WKDIR/log/mnt_$i.log,debug=$CLIENT_DEBUG_LEVEL \
		#$WKDIR/mnt_$i > $WKDIR/log/fuse_$i.log 2>&1 &
		$XTREEMFS/client/bin/xtfs_mount $sslmnt -o direct_io,dirservice=http://localhost:32638,volume_url=${schema}localhost/test_$i $WKDIR/mnt_$i 

		if [ $? -ne 0 ]; then
			echo "FAILED: cannot mount volume test_$i to $WKDIR/mnt_$i"
			shutdown_services; do_unmount; exit 1
		fi
		VOLUMES="$VOLUMES $WKDIR/mnt_$i"
	done
	export VOLUMES

	NONDIRECT_VOLUMES=""
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "mounting volume test_$i in mnt_nondirect$i..."

		mkdir $WKDIR/mnt_nondirect$i
		#$XTREEMFS/bin/xtfs_mount -f $CLIENT_DEBUG $sslmnt\
		#-o volume_url=${schema}localhost/test_$i \
		#-o logfile=/dev/null,debug=$CLIENT_DEBUG_LEVEL \
		#$WKDIR/mnt_nondirect$i > $WKDIR/log/fuse_nondirect$i.log 2>&1 &
		$XTREEMFS/client/bin/xtfs_mount $sslmnt -o dirservice=http://localhost:32638,volume_url=${schema}localhost/test_$i \
		$WKDIR/mnt_nondirect$i

		if [ $? -ne 0 ]; then
			echo "FAILED: cannot mount volume test_$i to $WKDIR/mnt_nondirect$i"
			shutdown_services; do_unmount; exit 1
		fi
		NONDIRECT_VOLUMES="$NONDIRECT_VOLUMES $WKDIR/mnt_nondirect$i"
	done
	export NONDIRECT_VOLUMES
}

check_mount() {

	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do

		if [ `grep -c "$WKDIR/mnt_$i" /proc/mounts` -eq 0 ]; then
			echo "FAILED: volume test_$i not mounted in $WKDIR/mnt_$i, probably crashed"
			do_unmount
			shutdown_services
			exit 1
		fi

		if [ `grep -c "$WKDIR/mnt_nondirect$i" /proc/mounts` -eq 0 ]; then
			echo "FAILED: volume test_$i (nondirect) not mounted in $WKDIR/mnt_$i, probably crashed"
			do_unmount
			shutdown_services
			exit 1
		fi

	done

}

do_unmount() {
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "unmounting volume test_$i ..."

		if [ `grep -c "$WKDIR/mnt_$i" /proc/mounts` -gt 0 ]; then
			echo "Unmounting volume test_$i"
			/usr/local/bin/fusermount -u $WKDIR/mnt_$i
		else
			echo "volume test_$i not mounted in $WKDIR/mnt_$i, probably crashed"
		fi

		if [ `grep -c "$WKDIR/mnt_nondirect$i" /proc/mounts` -gt 0 ]; then
			echo "Unmounting volume test_$i (nondirect)"
			/usr/local/bin/fusermount -u $WKDIR/mnt_nondirect$i
		else
			echo "volume test_$i not mounted in $WKDIR/mnt_nondirect$i, probably crashed"
		fi

	done
}


#STATIC CONFIGURATION

if [ ! $JAVA_HOME ]
then
	export JAVA_HOME="/opt/jdk1.6.0_02"
fi

if [ $# -lt 1 ]
then
	echo "usage: $0 <xtreemfs dir> [<use_ssl>]"
	exit 1
fi
XTREEMFS=$1
export XTREEMFS
if [ $# -eq 2 ]
then
	USECERT=$2
else
	USECERT=""
fi
export USECERT
echo "using SSL: $USECERT"

export NUM_OSDS=${NUM_OSDS:-3}
export STRIPE_WIDTH=${STRIPE_WIDTH:-128}
JAVA_DEBUG=1
CLIENT_DEBUG=""
CLIENT_DEBUG_LEVEL=0

# if ran from inside the automated tests script, use ZIB-specific settings
# (actually a bad idea to hardcode the ZIB paths... [EF])

if [ -n "$TEST_SUMMARY" ]; then
    # CREATE working directory for xtreemfs
    export WKDIR=/scratch/disk1/xtreemfs_test/`date +'%Y-%m-%d_%H.%M.%S'`
else
    export STANDALONE_RUN=y
    export WKDIR=`mktemp -d`
    export TEST_SUMMARY=$WKDIR/test_summary_`date +'%Y-%m-%d_%H.%M.%S'`
fi

create_config
startup_services

echo "waiting 10s for system to settle..."
sleep 10

do_mount
sleep 1

check_mount

#execute_tests

echo -e "\n\n================== STARTING TESTS ====================\n\n"

result=0

for testfile in tests/[0-9][0-9]_*.sh
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

do_unmount

shutdown_services

if [ -n "$STANDALONE_RUN" ]; then
    cp $TEST_SUMMARY .
    rm -rf $WKDIR
fi

exit $result
