#!/bin/bash
#
# Basic Access Layer tests automatization.
#
# Starts DS, MRC, and NOSDS OSDs (can be passed in as env variable) on the
# local host. Then creates NOSDS volumes with the different striping
# patterns. Mounts the volumes and runs some basic tests inside them:
# - ddwrite.sh : parallel write from multiple processes to multiple files
# - create/delete 1000 empty files (by touch)
# - create delete 1000 1 byte files (by echo)
#
# All tests, mounts and storing of config files is done in a temporary
# directory which is cleaned up after the tests are finished. If you want
# to keep the directory, pass in a non-zero env variable $KEEPDIR. You
# can also stop the script after the tests are finished, in that case
# daemons will still be running and the volumes will be mounted.
#
# Copyright (c) Erich Focht <efocht at hpce dot nec dot com>
#
# $Id: basic_ALtests 333 2007-05-30 12:34:17Z efocht $

PIDS=
VOLUMES=
AUTOTEST=
NOSDS=${NOSDS:=1}
STRIPE=${STRIPE:=128}
KEEPDIR=${KEEPDIR:=}
NDEBUG=4
TDIR=

cleanup() {
    for p in $PIDS; do
	if [ -d /proc/$p ]; then
	    echo "Killing process $p"
	    kill -9 $p
	fi
    done
    for v in $VOLUMES; do
	if [ `grep -c "$v" /proc/mounts` -gt 0 ]; then
	    echo "Unmounting volume $v"
	    fusermount -u $v
	fi
    done
    if [ -z "$KEEPDIR" ]; then
	echo "Removing temporary directory $TDIR"
	[ -n "$TDIR" ] && rm -rf $TDIR
    else
	echo "+++ Keeping temporary directory $TDIR !!!"
	echo "+++ You'll need to clean it up manually!"
    fi
    exit 0
}

clean_pid() {
    local pid=$1
    sleep 1
    if [ ! -d /proc/$pid ]; then
	echo "Process $pid doesn't exist any more! Exiting!!!!"
	cat $TDIR/*.log
	cleanup
    else
	PIDS="$PIDS $pid"
    fi
}

make_mount_vol() {
    local name=$1 width=$2

    echo "## making volume $name (stripesize=$STRIPE, width=$width)"
    ./xtfs_mkvol -p RAID0,$STRIPE,$width http://localhost/$name || cleanup
    mkdir -p $MDIR/$name
    echo "## mounting volume to $MDIR/$name"
    [ -n "$NO_DEBUG" ] || DBGFLAG="-d"
    set -x
    ./xtfs_mount -f $DBGFLAG -o volume_url=http://localhost/$name \
	-o direct_io,logfile=$TDIR/$name.log,debug=$NDEBUG \
	$MDIR/$name > $TDIR/${name}_fuse.log 2>&1 &
    set +x
    VOLUMES="$VOLUMES $MDIR/$name"
}

test_running() {
    for p in $PIDS; do
	if [ ! -d /proc/$p ]; then
	    echo "Process $p disappeared! Exiting..."
	    cat $TDIR/*_run.log
	    cleanup
	fi
    done
}

test_fuse() {
    if [ `/sbin/lsmod | egrep -c '^fuse '` -lt 1 ]; then
	echo "!!! fuse module not found! You must load it to continue!!!"
	echo "!!! do (as root): 'modprobe fuse'"
	exit 1
    fi
}

print_meminfo() {
    ps -C xtreemfs -o pid,cmd,rss,size
}

usage() {
    cat <<EOF
Basic Access Layer tests automatization.

Starts DS, MRC, and NOSDS OSDs on the local host. Then creates NOSDS
volumes with the different striping patterns. Mounts the volumes and
waits for an input from the keyboard. If 'test' is entered, runs some
basic tests on the mounted volumes:
- ddwrite.sh : parallel write from multiple processes to multiple files
- create/delete 1000 empty files (by touch)
- create delete 1000 1 byte files (by echo)

All tests, mounts and storing of config files is done in a temporary
directory which is cleaned up after the tests are finished (or the ENTER
key has been pressed at the prompt).

Usage:
   $0 [--auto|-a] [--keep|-k] [--nosds|-n <NOSDS>] [--stripe|-s <STRIPE>]

OPTIONS:
  --auto|-a        : don't wait for input, run tests and clean up after
                     60 seconds.
  --keep|-k        : keep temporary directory with all logs and data.
  --nosds|-n NOSDS : specify number of OSDS which should be started locally.
                     The default is 1. The OSD ports will be starting with
                     32640 and incremented.
  --stripe|-s SIZE : specify stripe size in kb for the volumes.
  --dir|-D DIR     : run the tests in the directory passed as argument. The
                     default is a randomly generated directory in /tmp.
  --debug|-d LEVEL : set debug level for xtreemfs client debugging. Higher
                     value shows more detailed output. Default value is
                     currently 4 (until the code stabilizes).

EOF
}

# parse arguments
while [ -n "$1" ]; do
    case "$1" in
	--auto|-a)
	    AUTOTEST=1
	    shift
	    ;;
	--keep|-k)
	    KEEPDIR=1
	    shift
	    ;;
	--nosds|-n)
	    shift
	    if [ -z "$1" ]; then
		echo "You must pass a number of OSDS to --nosds!"
		usage
		exit 1
	    fi
	    NOSDS=$1
	    shift
	    ;;
	--stripe|-s)
	    shift
	    if [ -z "$1" ]; then
		echo "You must pass the stripe size (in kb) to --stripe!"
		usage
		exit 1
	    fi
	    STRIPE=$1
	    shift
	    ;;
	--debug|-d)
	    shift
	    if [ -z "$1" ]; then
		echo "You must pass the debug level to --debug!"
		usage
		exit 1
	    fi
	    NDEBUG=$1
	    shift
	    ;;
	--dir|-D)
	    shift
	    if [ -z "$1" ]; then
		echo "You must pass a directory to --dir!"
		usage
		exit 1
	    fi
	    TDIR=$1
	    shift
	    ;;
	*)
	    echo "Unknown option $1"
	    usage
	    exit 1
	    ;;
    esac
done

# make a temporary directory for all the cruft
if [ -n "$TDIR" ]; then
    [ ! -d $TDIR ] && mkdir -p $TDIR
else
    TDIR=`mktemp -d /tmp/xtreemfs_XXXXXXXX`
fi
echo "Temporary directory for cfg, logs and data is $TDIR"

# check for FUSE module
test_fuse

##
## Startup servers
##

echo -n "# starting DS locally"
set -x
./xtreemfs_start ds -s $TDIR/db -i -d http://localhost:32638 -c $TDIR/ds.cfg --debug \
    -l $TDIR/ds.log > $TDIR/ds_run.log 2>&1 &
set +x
clean_pid $! && echo " ($!)"

echo -n "# starting MRC locally"
set -x
./xtreemfs_start mrc -s $TDIR/db -i -d http://localhost:32638 -c $TDIR/mrc.cfg --debug \
    -l $TDIR/mrc.log > $TDIR/mrc_run.log 2>&1 &
set +x
clean_pid $! && echo " ($!)"
sleep 3

# start NOSDS instances of OSD servers
for (( i=0 ; i<$NOSDS ; i++ )) ; do
    echo -n "# starting OSD$i locally"
    osd_port=`expr 32640 + $i`
    set -x
    ./xtreemfs_start osd -s $TDIR/osd$i -i -d http://localhost:32638 -c $TDIR/osd$i.cfg \
	-l $TDIR/osd$i.log --debug -p $osd_port \
	> $TDIR/osd${i}_run.log 2>&1 &
    set +x
    clean_pid $! && echo " ($!)"
done
echo "... sleeping 10s to allow OSDs to register with DS & MRC"
sleep 10
test_running

##
## Make and mount volumes
##
MDIR=$TDIR/mnt; mkdir -p $MDIR
for (( width=1 ; width<=$NOSDS; width++ )); do
    make_mount_vol x$width $width
done

if [ -z "$AUTOTEST" ]; then
    echo "Enter 'test' if you want to run the tests."
    echo "Anything else will skip the tests and cleanup."
    echo -n "> "
    read key
else
    key=test
fi
if [ "$key" = "test" ]; then

##
## do some tests
##

top -b -n 1 | grep xtreemfs
print_meminfo

echo "*** Starting parallel ddwrite tests ***"
for (( width=1 ; width<=$NOSDS; width++ )); do
    vol=x$width
    echo "==== Writing in volume $vol ===="
    for (( c=1; c<=16 ; c=c*2 )); do
	echo "  --- 10MB, $c client(s) ---"
	../AL/tests/suite/ddwrite.sh 10 $MDIR/$vol/t10MB$c $c
	print_meminfo
    done
    echo "==== writing 20x1MB files with marked data, checking data ===="
    ../AL/tests/suite/marked_block.pl --start=1 --nfiles=20 --size=1 \
        --group=10 --base=$MDIR/$vol/testfile
    echo "==== done ===="
done

rm -rf $MDIR/$vol/t10MB* $MDIR/$vol/testfile*

for (( width=1 ; width<=$NOSDS; width++ )); do
    vol=x$width
    echo "*** Creating/Renaming/Deleting 100 empty files ***"
    ../AL/tests/suite/createFiles.sh $MDIR/$vol/t 100 -r
    print_meminfo
    echo "*** Creating/Deleting 100 empty files ***"
    ../AL/tests/suite/createFiles.sh $MDIR/$vol/t 100
    print_meminfo
    echo "*** Creating/Deleting 100 1 byte files ***"
    ../AL/tests/suite/createFiles.sh $MDIR/$vol/b 100 -w
    print_meminfo
done


# add more tests


if [ -z "$AUTOTEST" ]; then
    echo "Press ENTER when you are ready to quit."
    echo -n "> "
    read key
else
    echo "+++ Finished.Sleeping 20s before cleanup"
    sleep 20
fi

fi


cleanup

