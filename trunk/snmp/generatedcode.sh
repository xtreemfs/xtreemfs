#!/bin/bash

JAVA=java
if [ ! -z "${JAVA_HOME}" ]; then
	    JAVA=$JAVA_HOME/bin/java
fi



CLASSPATH="../java/lib/jdmkrt.jar":"../java/lib/jdmktk.jar":"$CLASSPATH"
export CLASSPATH


$JAVA com.sun.jdmk.tools.MibGen -mc  -d ../java/servers/src/org/xtreemfs/common/monitoring/generatedcode -tp org.xtreemfs.common.monitoring.generatedcode xtreemfs-mib.txt  mib_core.txt 

exit 0;
