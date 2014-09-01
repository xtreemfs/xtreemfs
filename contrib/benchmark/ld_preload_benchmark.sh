#!/bin/bash
echo $*

if [[ $# -lt 3 ]]; then
  echo "Params missing: XTREEMFS_BASE_DIR DIR_URL MRC_URL [OUTPUT_DIRECTORY] [DATA_DIRECTORY]"
  exit 1
fi


XTREEMFS=${1%/}
DIR_URL=$(sed "s,/$,," <<< $2)
MRC_URL=$(sed "s,/$,," <<< $3)
OUT_DIR=$(sed "s,/$,," <<< $4)
DATA_DIR=$(sed "s,/$,," <<< $5)

if [[ -z $OUT_DIR ]]; then
  OUT_DIR=$HOME/ld_preload_benchmark
  echo "Using $OUT_DIR for output"
fi

# LOG=$OUT_DIR/log.txt
RESULT=$OUT_DIR/result.csv
TIME="/usr/bin/time -f %e,%U,%S"
echo "method,direction,stripesize in kB,filesize in MB,real in s,user in s, system in s" > $RESULT

TMP_DIR=$(mktemp -d)
MNT_DIR=$TMP_DIR/mnt
mkdir $MNT_DIR


# File sizes to copy in MB
FILE_SIZES="1	2	4	8	16	32	64	128	256	512	1024	2048	4096"

# Stripe sizes in kB
STRIPE_SIZES="128"

# Repeat the test x times
REPEAT=10

# Generate test data
if [[ -z $DATA_DIR ]] || [[ ! -d $DATA_DIR ]]; then
  DATA_DIR=$TMP_DIR/data
  mkdir $DATA_DIR
fi

for FILE_SIZE in $FILE_SIZES; do
  echo "Generate ${S}MB of test data"
  dd if=/dev/zero of=$DATA_DIR/$FILE_SIZE bs=1M count=$FILE_SIZE
done

# Run benchmarks for every STRIPE and every FILE SIZE
for STRIPE_SIZE in $STRIPE_SIZES; do

  # Generate the volume
  VOL_NAME="ld_preload_benchmark_stripesize-$STRIPE_SIZE"
  echo $XTREEMFS/bin/mkfs.xtreemfs -s $STRIPE_SIZE -w 1 $MRC_URL/$VOL_NAME
  $XTREEMFS/bin/mkfs.xtreemfs -s $STRIPE_SIZE -w 1 $MRC_URL/$VOL_NAME

  # Mount the volume to the tmp dir
  echo $XTREEMFS/bin/mount.xtreemfs -d WARN $DIR_URL/$VOL_NAME $MNT_DIR
  $XTREEMFS/bin/mount.xtreemfs -d WARN $DIR_URL/$VOL_NAME $MNT_DIR

  # Set the LD_PRELOAD options
  XTREEMFS_PRELOAD_OPTIONS="--log-level WARNING $DIR_URL/$VOL_NAME /xtreemfs"
  LD_PRELOAD="$XTREEMFS/cpp/build/libxtreemfs_preload.so"

  echo "Volume Information:" 
  $XTREEMFS/bin/xtfsutil $MNT_DIR


  for FILE_SIZE in $FILE_SIZES; do

    for (( i=0; i<$REPEAT ; i++)); do
  
      REPEAT_DIR="repeat-$i"
      mkdir $MNT_DIR/$REPEAT_DIR

      ####
      echo "Copy ${FILE_SIZE}MB to volume:"
      echo -n "fuse,in,$STRIPE_SIZE,$FILE_SIZE," | tee -a $RESULT
      { 
        $TIME cp $DATA_DIR/$FILE_SIZE $MNT_DIR/$REPEAT_DIR/fuse-$FILE_SIZE ;

      } 2> >(tee -a $RESULT)
      
      echo -n "preload,in,$STRIPE_SIZE,$FILE_SIZE," | tee -a $RESULT
      { 
        XTREEMFS_PRELOAD_OPTIONS="$XTREEMFS_PRELOAD_OPTIONS" LD_PRELOAD="$LD_PRELOAD" \
          $TIME cp $DATA_DIR/$FILE_SIZE /xtreemfs/$REPEAT_DIR/preload-$FILE_SIZE ; 

      } 2> >(tee -a $RESULT) 


      ###
      echo "Copy ${FILE_SIZE}MB from volume:"
      mkdir $DATA_DIR/out

      echo -n "fuse,out,$STRIPE_SIZE,$FILE_SIZE," | tee -a $RESULT
      { 
        $TIME cp $MNT_DIR/$REPEAT_DIR/fuse-$FILE_SIZE $DATA_DIR/out/fuse-$FILE_SIZE ; 

      } 2> >(tee -a $RESULT)
      
      echo -n "preload,out,$STRIPE_SIZE,$FILE_SIZE," | tee -a $RESULT
      { 
        XTREEMFS_PRELOAD_OPTIONS="$XTREEMFS_PRELOAD_OPTIONS" LD_PRELOAD="$LD_PRELOAD" \
          $TIME cp /xtreemfs/$REPEAT_DIR/preload-$FILE_SIZE $DATA_DIR/out/preload-$FILE_SIZE ; 

      } 2> >(tee -a $RESULT)

      rm -rf $DATA_DIR/out


      ###
      echo "Copy ${FILE_SIZE}MB within volume:" 
      
      echo -n "fuse,internal,$STRIPE_SIZE,$FILE_SIZE," | tee -a $RESULT
      { 
        $TIME cp $MNT_DIR/$REPEAT_DIR/fuse-$FILE_SIZE $MNT_DIR/$REPEAT_DIR/fuse-internal-$FILE_SIZE ;

      } 2> >(tee -a $RESULT)
      
      echo -n "preload,internal,$STRIPE_SIZE,$FILE_SIZE," | tee -a $RESULT
      { 
        XTREEMFS_PRELOAD_OPTIONS="$XTREEMFS_PRELOAD_OPTIONS" LD_PRELOAD="$LD_PRELOAD" \
          $TIME cp /xtreemfs/$REPEAT_DIR/preload-$FILE_SIZE /xtreemfs/$REPEAT_DIR/preload-internal-$FILE_SIZE ; 

      } 2> >(tee -a $RESULT)

      rm -rf $MNT_DIR/$REPEAT_DIR

    done # repeat

  done # file_size

  
  # Clear and unmount the volume
  rm -rf $MNT_DIR/*
  $XTREEMFS/bin/umount.xtreemfs $MNT_DIR
  $XTREEMFS/bin/rmfs.xtreemfs -f $MRC_URL/$VOL_NAME

done

# Clear the TMP_DIR
rm -rf $TMP_DIR

