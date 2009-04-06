#!/bin/bash

#set up test environment

create_config() {
	flags="-d $DEBUG "
	if [ $SSL_ENABLED -ne 0 ]
	then
		flags="$flags -s -c $XTREEMFS_DIR/servers/test/certs/ "
	fi

	#create configs
	$TEST_BASEDIR/utils/generate_config.sh $flags dir $TEST_DIR
	$TEST_BASEDIR/utils/generate_config.sh $flags mrc $TEST_DIR
	
	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		$TEST_BASEDIR/utils/generate_config.sh $flags osd$i $TEST_DIR
	done

}

startup_services() {
	$TEST_BASEDIR/utils/service.sh $XTREEMFS_DIR $TEST_DIR dir start
	if [ $? -ne 0 ]; then
		echo "FAILED: cannot start dir"; exit 1
	fi
	sleep 1

	$TEST_BASEDIR/utils/service.sh $XTREEMFS_DIR $TEST_DIR mrc start
	if [ $? -ne 0 ]; then
		echo "FAILED: cannot start mrc"; $TEST_BASEDIR/stop_environment.sh $TEST_DIR; exit 1
	fi
	sleep 1 

	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		$TEST_BASEDIR/utils/service.sh $XTREEMFS_DIR $TEST_DIR osd$i start
		if [ $? -ne 0 ]; then
			echo "FAILED: cannot start osd$i"; $TEST_BASEDIR/stop_environment.sh $TEST_DIR; exit 1
		fi
	done
	
}


prepare_test_directory() {
	testdir=$1
	mkdir -p $testdir
	if [ $? -ne 0 ]
	then
		echo "ERROR: cannot create test directory $testdir"
		exit 1
	fi
	mkdir -p $testdir/run
	mkdir -p $testdir/data
	mkdir -p $testdir/config
	mkdir -p $testdir/mnt
	mkdir -p $testdir/log
}

check_java() {
	if [ -z "$JAVA_HOME" ]; then
		echo "\$JAVA_HOME not set, JDK/JRE 1.6 required"
		exit
	else
		JVERS=`$JAVA_HOME/bin/java -version 2>&1 | grep "java version" | \
			cut -d " " -f 3`
		perl -e " exit 1 if ($JVERS < \"1.6.0\");"
		if [ $? -eq 1 ]; then
			echo "ERROR: java version is $JVERS but required is >= 1.6.0"
			exit 1
		fi
	fi
	echo "java home             $JAVA_HOME"
}

check_xtreemfsdir() {
	local XTFSDIR=$1
	if [ ! -e $XTFSDIR/client/bin/xtfs_mount ]
	then
		echo "ERROR: $XTFSDIR/client/bin/xtfs_mount does not exist"
		exit 1
	fi
	
	if [ ! -x $XTFSDIR/client/bin/xtfs_mount ]
	then
		echo "ERROR: $XTFSDIR/client/bin/xtfs_mount is not executable"
		exit 1
	fi
	
	if [ ! -e $XTFSDIR/servers/dist/XtreemFS.jar ]
	then
		echo "ERROR: $XTFSDIR/java/dist/XtreemFS.jar does not exist"
		exit 1
	fi

        if [ ! -e $XTFSDIR/servers/lib/BabuDB-0.1.0-RC.jar ]
        then
                echo "ERROR: $XTFSDIR/java/lib/BabuDB-0.1.0-RC.jar does not exist"
                exit 1
        fi
}

do_mount() {
	VOLUMES=""
	if [ $SSL_ENABLED -ne 0 ]
	then
		sslflags="--pkcs12-file-path=$XTREEMFS_DIR/servers/test/certs/Client.p12 --pkcs12-passphrase=passphrase "
		schema="oncrpcs://"
	else
		sslflags=""
		schema="oncrpc://"
	fi
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "creating volume test_$i ..."

		mkdir $TEST_DIR/mnt/$i
		$XTREEMFS_DIR/client/bin/xtfs_mkvol $sslflags -p RAID0 -s $STRIPE_WIDTH -w $i ${schema}localhost/test_$i
		if [ $? -ne 0 ]; then
			echo "FAILED: cannot create volume test_$i"
			$TEST_BASEDIR/stop_environment.sh $TEST_DIR
			exit 1
		fi

		echo "mounting volume test_$i in $i..."

		echo "mouting: $XTREEMFS_DIR/client/bin/xtfs_mount $CLIENT_FLAGS $sslflags -o direct_io   ${schema}localhost:32638/test_$i $TEST_DIR/mnt/$i"
		$XTREEMFS_DIR/client/bin/xtfs_mount $CLIENT_FLAGS $sslflags -o direct_io   ${schema}localhost:32638/test_$i $TEST_DIR/mnt/$i > $TEST_DIR/log/client_$i.log 2>&1 &

		if [ $? -ne 0 ]; then
			echo "FAILED: cannot mount volume test_$i to $TEST_DIR/mnt/$i"
			$TEST_BASEDIR/stop_environment.sh $TEST_DIR
			exit 1
		fi
		VOLUMES="$VOLUMES $TEST_DIR/mnt/$i"
		VOLNAMES="$VOLNAMES test_$i"
	done
	export VOLUMES
	export VOLNAMES

	NONDIRECT_VOLUMES=""
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "mounting volume test_$i in nondirect_$i..."

		mkdir $TEST_DIR/mnt/nondirect_$i
		#$XTREEMFS/bin/xtfs_mount -f $CLIENT_DEBUG $sslmnt\
		#-o volume_url=${schema}localhost/test_$i \
		#-o logfile=/dev/null,debug=$CLIENT_DEBUG_LEVEL \
		#$WKDIR/mnt_nondirect$i > $WKDIR/log/fuse_nondirect$i.log 2>&1 &
		$XTREEMFS_DIR/client/bin/xtfs_mount $CLIENT_FLAGS $sslmnt ${schema}localhost:32638/test_$i \
		$TEST_DIR/mnt/nondirect_$i > $TEST_DIR/log/client_nondirect$i.log 2>&1 &

		if [ $? -ne 0 ]; then
			echo "FAILED: cannot mount volume test_$i to $TEST_DIR/mnt/nondirect_$i"
			$TEST_BASEDIR/stop_environment.sh $TEST_DIR
			exit 1
		fi
		NONDIRECT_VOLUMES="$NONDIRECT_VOLUMES $TEST_DIR/mnt/nondirect_$i"
	done
	export NONDIRECT_VOLUMES
}

check_mount() {

	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do

		if [ `grep -c "$TEST_DIR/mnt/$i" /proc/mounts` -eq 0 ]; then
			echo "FAILED: volume test_$i not mounted in $TEST_DIR/mnt/$i, probably crashed"
			$TEST_BASEDIR/stop_environment.sh $TEST_DIR
			exit 1
		fi

		if [ `grep -c "$TEST_DIR/mnt/nondirect_$i" /proc/mounts` -eq 0 ]; then
			echo "FAILED: volume test_$i (nondirect) not mounted in $TEST_DIR/mnt/nondirect_$i, probably crashed"
			$TEST_BASEDIR/stop_environment.sh $TEST_DIR
			exit 1
		fi

	done

}

usage() {

	myname=`basename $1`
	echo "$myname -d -s -o -w <xtreemfs source> <test directory>"
	echo "-d enabled debug"
	echo "-s enables SSL (using certs from trunk/servers/test/certs)"
	echo "-o <num_osds> sets the number of OSDs to use"
	echo "-w <width> sets the striping with in kB"
	echo ""
}


SSL_ENABLED=0
DEBUG=1
NUM_OSDS=1
STRIPE_WIDTH=128
CLIENT_FLAGS=""

while getopts ":sc:d:w:o:f:" Option
# Initial declaration.
# a, b, c, d, e, f, and g are the options (flags) expected.
# The : after option 'e' shows it will have an argument passed with it.
do
  case $Option in
    s ) SSL_ENABLED=1
	;;
    d ) DEBUG=$OPTARG
	;;
    o ) NUM_OSD=$OPTARG
	;;
    w ) STRIPE_WIDTH=$OPTARG
	;;
    f ) CLIENT_FLAGS=$OPTARG
  esac
done
shift $(($OPTIND - 1))

if [ $# -ne 2 ]
then
	usage $0;
	exit 1;
fi

tmp=`readlink -f $0`
export TEST_BASEDIR=`dirname $tmp`

if [ $# -ne 2 ]
then
	usage $0;
	exit 1;
fi

XTREEMFS_DIR=$1
TEST_DIR=$2

prepare_test_directory $TEST_DIR

check_java
check_xtreemfsdir $XTREEMFS_DIR

create_config
startup_services
sleep 5


do_mount
sleep 2
check_mount

echo "export NONDIRECT_VOLUMES=\"$NONDIRECT_VOLUMES\"" > $TEST_DIR/globals.sh
echo "export VOLUMES=\"$VOLUMES\"" >> $TEST_DIR/globals.sh
echo "export VOLNAMES=\"$VOLNAMES\"" >> $TEST_DIR/globals.sh
echo "export TEST_BASEDIR=\"$TEST_BASEDIR\"" >> $TEST_DIR/globals.sh
echo "export TEST_SUMMARY=\"$TEST_DIR/test_summary\"" >> $TEST_DIR/globals.sh
echo "export MOUNT_DIR=\"$TEST_DIR/mnt\"" >> $TEST_DIR/globals.sh
echo "export XTREEMFS_DIR=\"$XTREEMFS_DIR\"" >> $TEST_DIR/globals.sh

