#!/bin/bash
echo $*

if [[ $# -lt 3 ]]; then
  echo "Params missing: XTREEMFS_BASE_DIR DIR_URL MRC_URL [OUTPUT_DIRECTORY]"
  exit 1
fi

set -v

XTREEMFS=${1%/}
DIR_URL=$(sed "s,/$,," <<< $2)
MRC_URL=$(sed "s,/$,," <<< $3)
OUT_DIR=$(sed "s,/$,," <<< $4)

if [[ -z $OUT_DIR ]]; then
  OUT_DIR=$HOME/ld_preload_benchmark
  echo "Using $OUT_DIR for output"
fi


TMP_DIR=$(mktemp -d)
MNT_DIR=$TMP_DIR/mnt
mkdir $MNT_DIR


# File sizes to copy in MB
FILE_SIZE="512"

# Stripe sizes in kB
STRIPE_SIZE="128"

# Repeat the test x times
REPEAT=10000

# Generate the volume
VOL_NAME="ld_preload_benchmark_stripesize-$STRIPE_SIZE"
$XTREEMFS/bin/mkfs.xtreemfs -s $STRIPE_SIZE -w 1 $MRC_URL/$VOL_NAME

# Mount the volume to the tmp dir
# $XTREEMFS/bin/mount.xtreemfs -d WARN -o direct_io -o sync_read -o sync $DIR_URL/$VOL_NAME $MNT_DIR
$XTREEMFS/bin/mount.xtreemfs -d WARN -o direct_io $DIR_URL/$VOL_NAME $MNT_DIR

# Set the LD_PRELOAD options
XTREEMFS_PRELOAD_OPTIONS="$DIR_URL/$VOL_NAME /xtreemfs"
LD_PRELOAD="$XTREEMFS/cpp/build/libxtreemfs_preload.so"

echo "Volume Information:" 
$XTREEMFS/bin/xtfsutil $MNT_DIR

# Library test
$XTREEMFS/cpp/build/benchmark_libxtreemfs_rand --raw_log $OUT_DIR/libxtreemfs-$FILE_SIZE.raw $DIR_URL/$VOL_NAME $REPEAT $FILE_SIZE libxtreemfs.file \
  | tee -a $OUT_DIR/libxtreemfs.csv

ls -lah $MNT_DIR
rm $MNT_DIR/libxtreemfs.file

# LD_PRELOAD test
XTREEMFS_PRELOAD_OPTIONS="$XTREEMFS_PRELOAD_OPTIONS" LD_PRELOAD="$LD_PRELOAD" \
  $XTREEMFS/cpp/build/benchmark_preload_rand --raw_log $OUT_DIR/ld_preload-$FILE_SIZE.raw ld_preload $REPEAT $FILE_SIZE /xtreemfs/ld_preload.file \
  | tee -a $OUT_DIR/ld_preload.csv
ls -lah $MNT_DIR
rm $MNT_DIR/ld_preload.file

# Fuse test
$XTREEMFS/cpp/build/benchmark_preload_rand --raw_log $OUT_DIR/fuse-$FILE_SIZE.raw fuse $REPEAT $FILE_SIZE $MNT_DIR/fuse.file \
  | tee -a $OUT_DIR/fuse.csv
ls -lah $MNT_DIR
rm $MNT_DIR/fuse.file


# Clear and unmount the volume
rm -rf $MNT_DIR/*
$XTREEMFS/bin/umount.xtreemfs $MNT_DIR
$XTREEMFS/bin/rmfs.xtreemfs -f $MRC_URL/$VOL_NAME


# Clear the TMP_DIR
rm -rf $TMP_DIR

