#!/bin/bash
. tests/utilities.inc.sh

echo "running TortureXtreemFS..."
i=1
for volume in $VOLUMES
do
	vol=`basename $volume`
	echo "torture on volume $vol..."
	$JAVA_HOME/bin/java -cp $XTREEMFS/java/dist/XtreemFS.jar org.xtreemfs.sandbox.tests.TortureXtreemFS http://localhost:32636 test_$i http://localhost:32638 $vol
	i=$(( $i+1 ))
	print_summary_message 0 "torture" $volume
done