#!/bin/bash

# Abort if any command fails.
set -e

export XTREEMFS="$1"
DIR_URL="$2"
if [[ "$DIR_URL" == pbrpcs://* || "$DIR_URL" == pbrpcg://* ]]
then
  CREDS="-c $XTREEMFS/tests/certs/Client.p12 -cpass passphrase -t $XTREEMFS/tests/certs/trusted.jks -tpass passphrase"
fi
VOLUME="$(basename `dirname $PWD`)"

TEMP_FILENAME="test__ronly_replication_add_delete_replica.bin"
TEMP_FILENAME_REPLICATED_FULL="test__ronly_replication_add_delete_replica.bin.replicated_full"
TEMP_FILENAME_REPLICATED_PARTIAL="test__ronly_replication_add_delete_replica.bin.replicated_partial"
XTFSUTIL="$XTREEMFS/bin/xtfsutil"
XTFS_SCRUB="$XTREEMFS/bin/xtfs_scrub"

if [ ! -e "$XTFSUTIL" ]
then
  echo "xtfsutil not found. Make sure that \$1 is set to the top directory of the XtreemFS source tree (current value: $XTREEMFS)"
  exit 1
fi
if [ ! -e "$XTFS_SCRUB" ]
then
  echo "xtfs_scrub not found. Make sure that \$1 is set to the top directory of the XtreemFS source tree (current value: $XTREEMFS)"
  exit 1
fi

# We assume that we're already inside the root directory of the mounted XtreemFS volume to be tested.

# Create a file with at least two objects and create another copy where we will add and remove replicas from it.
dd if=/dev/urandom bs=128k count=2 of="$TEMP_FILENAME" status=noxfer
cp "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_FULL"
diff "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_FULL"
cp "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_PARTIAL"
diff "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_PARTIAL"

## FULL Replica
# Remember the OSD UUID of the original replica
original_osd=$("$XTFSUTIL" "$TEMP_FILENAME_REPLICATED_FULL" | grep "OSD 1" | awk '{ print $3 }')
original_permissions=$(stat -c "%a" "$TEMP_FILENAME_REPLICATED_FULL")
# Add a full replica
"$XTFSUTIL" -r ronly "$TEMP_FILENAME_REPLICATED_FULL"
readonly_permissions=$(stat -c "%a" "$TEMP_FILENAME_REPLICATED_FULL")
if [ "$original_permissions" == "$readonly_permissions" ]
then
  cat <<EOF
Changing to the readonly policy does affect the actual permissions of the file as write permissions are removed.

However, permissions did not change. Before: $original_permissions After: $readonly_permissions
EOF
  exit 1
fi
"$XTFSUTIL" -a --full "$TEMP_FILENAME_REPLICATED_FULL"
# echo "Display md5sums of original file and copied file, which has two replicas:"
# md5sum "$TEMP_FILENAME"
# md5sum "$TEMP_FILENAME_REPLICATED_FULL"
echo "Waiting at least 5 seconds until the replication of the newly added full replica has probably completed..."
sleep 5

# Scrub the file and make sure that both replicas are "complete"
"$XTFS_SCRUB" -dir $DIR_URL $CREDS "$VOLUME"
if [ $("$XTFSUTIL" "$TEMP_FILENAME_REPLICATED_FULL" | grep "Replication Flags" | grep "complete" | wc -l) -ne 2 ]
then
  cat <<EOF
The xtfs_scrub run did not set the recently added full replica to "complete" - however, it should have been already complete.
EOF
  exit 1
fi

# Delete the original replica
"$XTFSUTIL" -d $original_osd "$TEMP_FILENAME_REPLICATED_FULL"
# echo "md5sum of the left, second replica:"
# md5sum "$TEMP_FILENAME_REPLICATED_FULL"
# Check if diff succeeds
diff "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_FULL"

# Reset replication policy to none and check if original permissions are restored.
"$XTFSUTIL" -r none "$TEMP_FILENAME_REPLICATED_FULL"
readwrite_permissions=$(stat -c "%a" "$TEMP_FILENAME_REPLICATED_FULL")
if [ "$original_permissions" != "$readwrite_permissions" ]
then
  cat <<EOF
After changing back the replication policy to 'none', the original permissions of a file should be in effect again.

However, permissions were not restored. Before setting readonly: $original_permissions After setting none: $readwrite_permissions
EOF
  exit 1
fi

## PARTIAL Replica
# Remember the OSD UUID of the original replica
original_osd=$("$XTFSUTIL" "$TEMP_FILENAME_REPLICATED_PARTIAL" | grep "OSD 1" | awk '{ print $3 }')

# Add a partial replica
"$XTFSUTIL" -r ronly "$TEMP_FILENAME_REPLICATED_PARTIAL"
"$XTFSUTIL" -a "$TEMP_FILENAME_REPLICATED_PARTIAL"

# Delete the original replica
# This should NOT succeed as the left partial replica is not marked as complete yet in the MRC.
"$XTFSUTIL" -d $original_osd "$TEMP_FILENAME_REPLICATED_PARTIAL" &>/dev/null && echo "ERROR: xtfsutil succeeded to delete the last full replica and now only partial replicas are left, i.e. the data of the file is lost." && false

# Set the Replica Selection Policy to "random". Read the partial replica multiple times, assuming we hit the partial replica at least once.
"$XTFSUTIL" --set-rsp 3002 ..
for i in {1..100}
do
  cat "$TEMP_FILENAME_REPLICATED_PARTIAL" >/dev/null
done
"$XTFSUTIL" --set-rsp "" ..
# xtfs_scrub the volume and make sure the partial replica was set to 'complete'
"$XTFS_SCRUB" -dir $DIR_URL $CREDS "$VOLUME"
if [ $("$XTFSUTIL" "$TEMP_FILENAME_REPLICATED_PARTIAL" | grep "Replication Flags" | grep "complete" | wc -l) -ne 2 ]
then
  cat <<EOF
The xtfs_scrub run did not set the recently added partial replica to "complete" - however, it should have been already complete since it was read multiple times.
EOF
  exit 1
fi

# Cleanup
rm -f "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_FULL" "$TEMP_FILENAME_REPLICATED_PARTIAL"