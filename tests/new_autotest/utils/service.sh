#!/bin/bash


start_service() {
	local service=$1 class=$2
	if [ -e $TEST_DIR/run/$service.run ]
	then
		echo "ERROR: service already running or remove lock file $TEST_DIR/$1.run"
		exit 1
	fi
	$JAVA_HOME/bin/java -cp $XTREEMFS_DIR/servers/dist/XtreemFS.jar:$XTREEMFS_DIR/servers/lib/BabuDB-0.1.0-RC.jar $class $TEST_DIR/config/$service.config > $TEST_DIR/log/$service.log 2>&1 &
	
	servicepid=$!
	sleep 1
	if [ ! -d /proc/$servicepid ]
	then
		echo "ERROR: process $servicepid crashed during startup"
		tail -n10 $TEST_DIR/log/$service.log
		echo "see full log in $TEST_DIR/log/$service.log"
		exit 1
	fi

	echo $servicepid > $TEST_DIR/run/$service.run
	echo "$service started"
	echo ""
}

stop_service() {
	local service=$1

	if [ ! -e $TEST_DIR/run/$service.run ]
	then
		echo "ERROR: service $service not running!"
		exit 1
	fi
	
	kill `cat $TEST_DIR/run/$service.run`
	rm $TEST_DIR/run/$service.run
	echo "$service stopped"
	echo ""
}

if [ $# -ne 4 ]
then
	echo "usage: $0 <XtreemFS dir> <test dir> {dir|mrc|osd0..9} {start|stop}"
	exit 1
fi

export XTREEMFS_DIR=$1
export TEST_DIR=$2
SERVICE=$3
ACTION=$4


#START SERVICE	

if [ $SERVICE == "dir" ]
then
	if [ $ACTION == "start" ]; then
		start_service "dir" "org.xtreemfs.dir.DIR"
	else
		stop_service "dir"
	fi
elif [ $SERVICE == "mrc" ]
then
	if [ $ACTION == "start" ]; then
		start_service "mrc" "org.xtreemfs.mrc.MRC"
	else
		stop_service "mrc"
	fi
elif [ ${SERVICE:0:3} == "osd" ]
then
	if [ $ACTION == "start" ]; then
		start_service $SERVICE "org.xtreemfs.osd.OSD"
	else
		stop_service $SERVICE
	fi
fi

