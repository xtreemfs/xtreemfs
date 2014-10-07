#!/bin/bash

OBJECT_DIR=$1

# get device for object_dir
IFS=' ' read -r DEVICE TMP <<< $(df $OBJECT_DIR | grep dev)

# Determine device type
if [[ $DEVICE == *md* ]]; then
   # DEVICE is a RAID configuration
   DEVICES=$(IFS=' ' read -a TMP <<< $(cat /proc/mdstat | grep $DEVICE))
   DEVICES=${DEVICES[@]:4}
elif [[ $DEVICE == *sd* || $DEVICE == *hd* ]]; then
   # DEVICE is a single disk
   DEVICES=$DEVICE
else
   # unsupported device type
   echo "unsupported device type"
   exit 3
fi

for DEVICE in $DEVICES; do     
   SMART_STATUS="$(sudo smartctl --health $DEVICE)"
   echo $SMART_STATUS
   if [[ $SMART_STATUS == *PASSED* ]]
      then
         continue;
   elif [[ $SMART_STATUS == *FAILED* ]]
      then
         exit 1
   else 
      exit 3
   fi
done 

# If no device's health test failed, return 0 (i.e. health test PASSED).
exit 0
