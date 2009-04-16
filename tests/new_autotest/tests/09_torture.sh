#!/bin/bash
. $TEST_BASEDIR/tests/utilities.inc


echo "running TortureXtreemFS..."
i=1
for volume in $VOLNAMES
do
	if [ $SSL_ENABLED -eq 0 ]
	then
		echo "torture on volume $volume..."
		$JAVA_HOME/bin/java -cp $XTREEMFS_DIR/servers/dist/XtreemFS.jar org.xtreemfs.sandbox.tests.TortureXtreemFS -v $volume oncrpc://localhost:32638 oncrpc://localhost:32636
	else
		echo "torture (with SSL) on volume $volume..."
		$JAVA_HOME/bin/java -cp $XTREEMFS_DIR/servers/dist/XtreemFS.jar org.xtreemfs.sandbox.tests.TortureXtreemFS -c $XTREEMFS_DIR/servers/test/certs/Client.p12 -cpass passphrase -t $XTREEMFS_DIR/servers/test/certs/trusted.jks -tpass passphrase -v $volume oncrpcs://localhost:32638 oncrpcs://localhost:32636
	fi

        if [ $? -ne 0 ]
	then
		echo "ERROR: torture failed on volume $volume!"
		print_summary_message 1 "torture" $volume
		exit 1
	fi
	i=$(( $i+1 ))
	print_summary_message 0 "torture" $volume
done
