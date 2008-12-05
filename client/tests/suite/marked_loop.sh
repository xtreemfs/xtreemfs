#!/bin/bash

if [ -d MARKED_BLOCKS ]; then
    echo "Removing files in directory MARKED_BLOCKS..."
    rm -f MARKED_BLOCKS/*
else
    mkdir MARKED_BLOCKS
fi

cd MARKED_BLOCKS

free=`df -m . |tail -1 | awk '{print $4}'`
nfiles=`expr \( $free - 480 \) / 100`
err=0
i=1
while [ $err -eq 0 ]; do
    echo "Removing files in directory MARKED_BLOCKS..." | tee -a ../mb.out
    echo "ROUND $i ::: writing $nfiles x 100MB files" | tee -a ../mb.out
    ../marked_block.pl --start=1 --nfiles=$nfiles --group=10 | tee ../mb_tmp.out
    err=`grep -c FAILED ../mb_tmp.out`
    i=`expr $i + 1`
done

if [ $err -ne 0 ]; then
    last=`tail -1 ../mb_tmp.out | sed -e "s/:.*$//"`
    echo "Found error in file $last" | tee -a ../mb.out
    ../marked_block.pl --check --file $last | tee -a ../mb.out
fi
kill $touchpid

