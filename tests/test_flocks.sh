#!/bin/bash

touch testfile

(
flock -n -x 10
if [ $? -ne 0 ]
then
    echo "could not acquire log!"
    exit 1
fi
sleep 10
flock -u 10
) 10>testfile &

sleep 1

(
flock -n -x 11
if [ $? -ne 1 ]
then
    echo "could acquire lock!"
    exit 1
fi
) 11>testfile

if [ $? -ne 0 ]
then
    echo "failed"
    exit 1
fi

echo "locking works :)"
exit 0
