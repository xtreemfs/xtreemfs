#!/bin/bash

OBJECT_DIR=$1

# get device for object_dir
IFS=' ' read -r DEVICE TMP <<< $(df $OBJECT_DIR | grep dev)

# Determine device type
if [[ $DEVICE == *md* ]]; then
   # DEVICE is a RAID configuration
   DEVICES=$(IFS=' ' read -a FOO <<< $(cat /proc/mdstat | grep md0))
   DEVICES=${DEVICES[@]:4}
elif [[ $DEVICE == *sd* || $DEVICE == *hd* ]]; then
   # DEVICE is a single disk
   DEVICES=$DEVICE
else
   # unsupported device type
   echo 3; exit
fi

for DEVICE in $DEVICES; do     
   SMART_STATUS="$(sudo smartctl --health $DEVICE)"
   if [[ $SMART_STATUS == *FAILED* ]]; then
      echo 1; exit;
   fi
done 

# If no device's health test failed, return 0 (i.e. health test PASSED).
echo 0
