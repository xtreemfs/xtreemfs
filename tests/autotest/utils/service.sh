#!/bin/bash

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
		echo "ERROR: $XTFSDIR does not contain an xtreemfs executable\n"
		exit 1
	fi
	
	if [ ! -x $XTFSDIR/client/bin/xtfs_mount ]
	then
		echo "ERROR: $XTFSDIR/AL/src/xtreemfs is not executable\n"
		exit 1
	fi
	
	if [ ! -e $XTFSDIR/servers/dist/XtreemFS.jar ]
	then
		echo "ERROR: $XTFSDIR/java/dist/XtreemFS.jar does not exist\n"
		exit 1
	fi
	
	if [ ! -e $XTFSDIR/servers/lib/hsqldb.jar ]
	then
		echo "ERROR: $XTFSDIR/java/lib/hsqldb.jar does not exist\n"
		exit 1
	fi
}

start_service() {
	local service=$1 class=$2
	mkdir -p $WKDIR/run/
	mkdir -p $WKDIR/log/
	if [ -e $WKDIR/run/$service.run ]
	then
		echo "ERROR: service already running or remove lock file $WKDIR/$1.run"
		exit 1
	fi
	$JAVA_HOME/bin/java -cp $XTREEMFS/servers/dist/XtreemFS.jar:$XTREEMFS/servers/lib/hsqldb.jar $class $WKDIR/config/$service.properties > $WKDIR/log/$service.log 2>&1 &
	
	servicepid=$!
	sleep 1
	if [ ! -d /proc/$servicepid ]
	then
		echo "ERROR: process $servicepid crashed during startup"
		tail -n10 $WKDIR/log/$service.log
		echo "see full log in $WKDIR/log/$service.log"
		exit 1
	fi

	echo $servicepid > $WKDIR/run/$service.run
	echo "$service started"
	echo ""
}

stop_service() {
	local service=$1

	if [ ! -e $WKDIR/run/$service.run ]
	then
		echo "ERROR: service $service not running!"
		exit 1
	fi
	
	kill `cat $WKDIR/run/$service.run`
	rm $WKDIR/run/$service.run
	echo "$service stopped"
	echo ""
}

if [ $# -lt 2 ]
then
	echo "usage: $0 -w workingdir -x xtreemfsdir service start/stop"
	exit 1
fi

#parse options
while getopts ":w:x:" Option
do
  case $Option in
    w     ) WKDIR=$OPTARG; export WKDIR;;
    x     ) XTREEMFS=$OPTARG; export XTREEMFS;;
    *     ) echo "unknown option $Option" exit 1;;   # DEFAULT
  esac
done
shift $(($OPTIND - 1))

SERVICE=$1
ACTION=$2

if [ ! $WKDIR ]
then
	echo "please specify working directory with -w or by setting environment variable WKDIR"
	exit 1
fi

if [ $ACTION == "start" ]
then
	if [ ! $XTREEMFS ]
	then
		echo "please specify xtreemfs directory with -w or by setting environment variable XTREEMFS"
		exit 1
	fi
	
	#check if XTREEMFS is a valid dir
	
	check_xtreemfsdir $XTREEMFS
	check_java
	
	echo "xtreemfs directory is $XTREEMFS"

fi
echo "working directory is  $WKDIR"

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

