#!/bin/bash

# run a series of benchmarks with the xtfs_benchmark tools on the test setup

XTREEMFS=$1
DIR_SERVER=$2
MRC_SERVER=$3
TEST_DIR=$4

LOGFILE=$TEST_DIR/log/xtfs_benchmark.log

# save stdout and stderr to file descriptors 3 and 4, then redirect them
exec 3>&1 4>&2 >> $LOGFILE 2>&1

exec $JAVA_HOME/bin/java -ea -cp $XTREEMFS/java/servers/dist/XtreemFS.jar:$XTREEMFS/java/foundation/dist/Foundation.jar:$XTREEMFS/java/lib/*:/usr/share/java/XtreemFS.jar:/usr/share/java/protobuf-java-2.3.0.jar:/usr/share/java/Foundation.jar:. \
  org.xtreemfs.utils.xtfs_benchmark.xtfs_benchmark -sw -sr -rw -rr -fw -fr -t 2 --stripe-size 128K --stripe-width 1 -ssize 1g -rsize 100m --file-size 4K --basefile-size 3g --dir-address $DIR_SERVER volumeA volumeB

# minimal version for testing
# exec $JAVA_HOME/bin/java -ea -cp $XTREEMFS/java/servers/dist/XtreemFS.jar:$XTREEMFS/java/foundation/dist/Foundation.jar:$XTREEMFS/java/lib/*:/usr/share/java/XtreemFS.jar:/usr/share/java/protobuf-java-2.3.0.jar:/usr/share/java/Foundation.jar:. \
#   org.xtreemfs.utils.xtfs_benchmark.xtfs_benchmark -sw -sr -rw -rr -fw -fr -t 2 --stripe-size 128K --stripe-width 1 -ssize 10m -rsize 1m --file-size 4K --basefile-size 100m --dir-address $DIR_SERVER volumeA volumeB

# restore stdout and stderr
exec 1>&3 2>&4
