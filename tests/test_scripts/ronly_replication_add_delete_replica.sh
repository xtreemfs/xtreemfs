#!/bin/bash

# Abort if any command fails.
set -e

TEMP_FILENAME="test__ronly_replication_add_delete_replica.bin"
TEMP_FILENAME_REPLICATED_FULL="test__ronly_replication_add_delete_replica.bin.replicated_full"
TEMP_FILENAME_REPLICATED_PARTIAL="test__ronly_replication_add_delete_replica.bin.replicated_partial"
XTFSUTIL="$1/bin/xtfsutil"

if [ ! -e "$XTFSUTIL" ]
then
  echo "xtfsutil not found. Make sure that \$1 is set to the top directory of the XtreemFS source tree (current value: $1)"
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

# Cleanup
rm -f "$TEMP_FILENAME" "$TEMP_FILENAME_REPLICATED_FULL" "$TEMP_FILENAME_REPLICATED_PARTIAL"