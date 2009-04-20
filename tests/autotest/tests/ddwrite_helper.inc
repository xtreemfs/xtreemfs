#!/bin/bash
#
# Issue a set of dd commands in the background to write in parallel into
# different files. The sum of the bandwidth should be a reasonable estimate
# of the peak bandwidth for writing to a filesystem in a very simple
# pattern.
#
# Copyright (c) Erich Focht <efocht at hpce dot nec dot com>
#
# $Id$

export MEGABYTES=$1
FILE=$2
NPROC=$3

if [ -z "$FILE" -o -z "$MEGABYTES" -o -z "$NPROC" ]; then
    echo "Usage: $0 <MEGABYTES> <OUTPUT_FILE_BASENAME> <NPROC>"
    exit 1
fi

cleanup() {
    for f in $TFILES; do
	[ -f $f ] && rm -rf $f
    done
}

check_length() {
    local file=$1
    local real target
    target=`expr $MEGABYTES \* 1000000`
    real=`stat -c "%s" $file`
    if [ -z "$real" ]; then
	echo "stat $file : execution failed!"
	return 1
    fi
    if [ $real -ne $target ]; then
	echo "WARNING: length of file is $real (should be $target)"
	return 1
    fi
    return 0
}

do_one () {
    local ID=$1
    local TEMP=$2
    #sync
    [ -e ${FILE}_$ID ] && rm -f ${FILE}_$ID
    /usr/bin/time -f "elapsed %e" -o $TEMP -- \
       dd if=/dev/zero of=${FILE}_$ID bs=1MB count=$MEGABYTES >/dev/null 2>&1
    /usr/bin/time -f "elapsed %e" -o $TEMP -a -- sync
    check_length ${FILE}_$ID || rm -f $TEMP
}

trap cleanup SIGINT
trap cleanup SIGHUP
trap cleanup SIGKILL

TFILES=""
PIDS=""
sync
for ((i=0; i<$NPROC; i++)) {
    TEMP=`mktemp`
    TFILES="$TFILES $TEMP"
    { do_one $i $TEMP </dev/null; } &
    PIDS="$PIDS $!"
}
#eval wait $PIDS
for p in $PIDS; do
    while [ -d /proc/$p ]; do
	sleep 1
    done
done

for TEMP in $TFILES; do
    [ ! -f $TEMP ] && continue
    if [ `grep -c elapsed $TEMP` -le 1 ]; then
	echo "No output!"
    else
	grep elapsed $TEMP | \
	    env LANG=C awk 'BEGIN{sum=0.0; CONVFMT = "%2.2f"}
                            {sum = sum + $2}
                            END{if (sum == 0.0) {print "0MB/s (no data?)"}
                                else{print ENVIRON["MEGABYTES"]/sum,"MB/s"}}'
    fi
    rm -f $TEMP
done

cleanup
exit 0