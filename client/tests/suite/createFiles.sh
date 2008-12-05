#!/bin/bash
#
# Create files: testing file creation, deletion, a bit of
# metadata performance.
#
# Copyright (c) Erich Focht <efocht at hpce dot nec dot com>
#
# $Id: createFiles.sh 3546 2008-08-08 18:51:37Z jmalo $

export FILE=$1
export NFILES=$2
export OPER=$3
NONEMPTY=
RENAME=

if [ -z "$FILE" -o -z "$NFILES" ]; then
    echo "Usage: $0 <OUTPUT_FILE_BASENAME> <NFILES> [-w|-r]"
    exit 1
fi

if [ "$OPER" = "-w" ]; then
    NONEMPTY=1
elif [ "$OPER" = "-r" ]; then
    RENAME=1
fi

cleanup() {
    for f in $TFILES; do
	[ -f $f ] && rm -rf $f
    done
}

do_touch() {
    /usr/bin/time -f "elapsed %e" -o $TEMP -- \
	bash -c "for (( i=0; i<$NFILES; i++ )); do \
	            touch ${FILE}_\$i; \
	         done"
}

do_byte() {
    local TEMP=$1
    /usr/bin/time -f "elapsed %e" -o $TEMP -- \
	bash -c "for (( i=0; i<$NFILES; i++ )); do \
	            echo 1 >${FILE}_\$i ; \
	         done"
}

do_rename() {
    local TEMP=$1
    /usr/bin/time -f "elapsed %e" -o $TEMP -- \
	bash -c "for (( i=0; i<$NFILES; i++ )); do \
	            mv ${FILE}_\$i ${FILE}_new_\$i ; \
	         done"
}

do_rm_renamed() {
    local TEMP=$1
    /usr/bin/time -f "elapsed %e" -o $TEMP -- \
	bash -c "for (( i=0; i<$NFILES; i++ )); do \
	            rm -f ${FILE}_new_\$i ; \
	         done"
}

do_remove() {
    local TEMP=$1
    /usr/bin/time -f "elapsed %e" -o $TEMP -- \
	bash -c "for (( i=0; i<$NFILES; i++ )); do \
	            rm -f ${FILE}_\$i ; \
	         done"
}


TFILES=""

TEMP=`mktemp /tmp/tmp.XXXXXXXXXX`
TFILES="$TFILES $TEMP"

if [ -z "$NONEMPTY" ]; then
    echo "touching $NFILES files ..."
    do_touch $TEMP
    cat $TEMP
    if [ -n "$RENAME" ]; then
	echo "renaming $NFILES files ..."
	do_rename $TEMP
	cat $TEMP
	ls `dirname $FILE`
	echo "deleting $NFILES renamed files ..."
	do_rm_renamed $TEMP
	cat $TEMP
	ls `dirname $FILE`
    else
	echo "deleting $NFILES files ..."
	do_remove $TEMP
	cat $TEMP
    fi
else
    echo "writing 1 byte to $NFILES files ..."
    do_byte $TEMP
    cat $TEMP
    echo "deleting $NFILES files ..."
    do_remove $TEMP
    cat $TEMP
fi

cleanup
