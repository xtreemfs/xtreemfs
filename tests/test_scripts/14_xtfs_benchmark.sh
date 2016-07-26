#!/bin/bash

# run a series of benchmarks with the xtfs_benchmark tools on the test setup

XTREEMFS=$1
DIR_SERVER=$2
MRC_SERVER=$3
TEST_DIR=$4
VOLUME="$(basename $(dirname $(pwd)))"

exec $JAVA_HOME/bin/java -ea -cp $XTREEMFS/java/xtreemfs-servers/target/xtreemfs.jar:/usr/share/java/xtreemfs.jar:. \
  org.xtreemfs.utils.xtfs_benchmark.xtfs_benchmark -sw -sr -rw -rr -fw -fr --stripe-size 128K --stripe-width 1 -ssize 500m -rsize 100m --file-size 64K --basefile-size 500m --dir-addresses $DIR_SERVER --user $USER --group $USER $VOLUME

# minimal version for testing
# exec $JAVA_HOME/bin/java -ea -cp $XTREEMFS/java/xtreemfs-servers/target/xtreemfs.jar:/usr/share/java/xtreemfs.jar:. \
#   org.xtreemfs.utils.xtfs_benchmark.xtfs_benchmark -sw -sr -rw -rr -fw -fr -t 1 --stripe-size 128K --stripe-width 1 -ssize 10m -rsize 1m --file-size 64K --basefile-size 100m --dir-address $DIR_SERVER $VOLUME

