#!/bin/bash

create_config() {
	mkdir -p $WKDIR/config
	if [ $? -ne 0 ]; then
		echo "ERROR: cannot create config directory $WKDIR/config"
		exit 1
	fi
	
	#create configs
	utils/create_testconfig.pl dir $JAVA_DEBUG localhost default $WKDIR > $WKDIR/config/dir.properties
	utils/create_testconfig.pl mrc $JAVA_DEBUG localhost default $WKDIR default > $WKDIR/config/mrc.properties
	
	for (( i=0 ; i<$NUM_OSDS ; i++ )) ; do
		utils/create_testconfig.pl osd$i $JAVA_DEBUG localhost default $WKDIR default > $WKDIR/config/osd$i.properties
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
	for (( i=1 ; i<=$NUM_OSDS ; i++ )) ; do
		echo "creating volume test_$i ..."

		mkdir $WKDIR/mnt_$i
		$XTREEMFS/AL/tools/mkvol -p RAID0,$STRIPE_WIDTH,$i http://localhost/test_$i
		if [ $? -ne 0 ]; then
			echo "FAILED: cannot create volume test_$i"
			shutdown_services; do_unmount; exit 1
		fi

		echo "mounting volume test_$i in mnt_$i..."

		$XTREEMFS/AL/src/xtreemfs -f $CLIENT_DEBUG \
		-o volume_url=http://localhost/test_$i \
		-o direct_io,logfile=$WKDIR/log/client_$i.log,debug=$CLIENT_DEBUG_LEVEL \
		$WKDIR/mnt_$i > $WKDIR/log/fuse_$i.log 2>&1 &

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
		$XTREEMFS/AL/src/xtreemfs -f $CLIENT_DEBUG \
		-o volume_url=http://localhost/test_$i \
		-o logfile=$WKDIR/log/client_nondirect_$i.log,debug=$CLIENT_DEBUG_LEVEL \
		$WKDIR/mnt_nondirect$i > $WKDIR/log/fuse_nondirect$i.log 2>&1 &

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

if [ $# -ne 3 ]
then
	echo "usage: $0 start|stop <xtreemfs dir> <wkdir>"
	exit 1
fi
XTREEMFS=$2
export XTREEMFS

export NUM_OSDS=3
export STRIPE_WIDTH=128
JAVA_DEBUG=2
CLIENT_DEBUG="-d"
CLIENT_DEBUG_LEVEL=1

# CREATE working directory for xtreemfs
export WKDIR=$3


if [ $1 = "start" ]
then
	create_config
	startup_services

	echo "waiting 10s for system to settle..."
	sleep 10

	do_mount 
	sleep 1

	check_mount
elif [ $1 = "stop" ]
then
	do_unmount

	shutdown_services
else
	echo "action can be start or stop, but not $1"
	exit 1
fi
exit $result
