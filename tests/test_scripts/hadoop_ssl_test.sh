#!/bin/bash

XTREEMFS=$1
TEST_DIR=$4
HADOOP_VERSIONS="1.2.1"
VOLUME="$(basename $(dirname $(pwd)))"

for VERSION in $HADOOP_VERSIONS; do

   echo "Test XtreemFS with hadoop $VERSION"

   #download and extract hadoop
   echo "Download Hadoop $VERSION..."
   wget -nv -O $TEST_DIR/hadoop-$VERSION.tar.gz http://archive.apache.org/dist/hadoop/core/hadoop-$VERSION/hadoop-$VERSION.tar.gz
   VOLUME_DIR=$PWD
   cd $TEST_DIR
   echo "Extract Hadoop $VERSION..."
   tar -zxf $TEST_DIR/hadoop-$VERSION.tar.gz
   rm -rf $TEST_DIR/hadoop-$VERSION.tar.gz
   cd $VOLUME_DIR

   #configure hadoop

   export HADOOP_PREFIX=$TEST_DIR/hadoop-$VERSION
   echo "Set HADOOP_PREFIX=$HADOOP_PREFIX"

   export HADOOP_CONF_DIR=$HADOOP_PREFIX/conf/
   echo "Set HADOOP_CONF_DIR=$HADOOP_CONF_DIR"

   export HADOOP_LOG_DIR="$TEST_DIR/log/hadoop.log"
   echo "Set HADOOP_LOG_DIR=$HADOOP_LOG_DIR"


   echo "Copy XtreeemFSHadoopClient.jar to $HADOOP_PREFIX/lib/"
   cp $XTREEMFS/contrib/hadoop/dist/XtreemFSHadoopClient.jar $HADOOP_PREFIX/lib/

   echo "configure core-site.xml"

   CORE_SITE="
   <configuration>

   <property>
    <name>fs.xtreemfs.impl</name>
    <value>org.xtreemfs.common.clients.hadoop.XtreemFSFileSystem</value>
   </property>

   <property>
    <name>fs.default.name</name>
    <value>xtreemfs://localhost:32638</value>
   </property>

   <property>
    <name>xtreemfs.defaultVolumeName</name>
    <value>$VOLUME</value>
   </property>

   <property>
    <name>xtreemfs.client.debug</name>
    <value>false</value>
   </property>

   <property>
    <name>io.file.buffer.size</name>
    <value>131072</value>
   </property>

   <property>
    <name>xtreemfs.io.read.buffer</name>
    <value>false</value>
   </property>

   <property>
    <name>xtreemfs.io.buffer.size.read</name>
    <value>64</value>
   </property>

   <property>
    <name>xtreemfs.io.write.buffer</name>
    <value>false</value>
   </property>

   <property>
    <name>xtreemfs.io.buffer.size.write</name>
    <value>64</value>
   </property>

   <property>
    <name>xtreemfs.ssl.enabled</name>
    <value>true</value>
   </property>

   <property>
    <name>xtreemfs.ssl.credentialFile</name>
    <value>$XTREEMFS/tests/certs/Client.p12</value>
   </property>

   <property>
    <name>xtreemfs.ssl.credentialFile.passphrase</name>
    <value>passphrase</value>
   </property>

   <property>
    <name>xtreemfs.ssl.trustedCertificatesFile</name>
    <value>$XTREEMFS/tests/certs/trusted.jks</value>
   </property>

   <property>
    <name>xtreemfs.ssl.trustedCertificatesFile.passphrase</name>
    <value>passphrase</value>
   </property>

   <property>
     <name>xtreemfs.hadoop.version</name>
     <value>$VERSION</value>
   </property>

   </configuration>"

   echo $CORE_SITE > $HADOOP_PREFIX/conf/core-site.xml

   echo "configure mapred-site.xml"

   MAPRED_SITE="
   <configuration>

   <property>
    <name>mapred.job.tracker</name>
    <value>localhost:9001</value>
   </property>

   </configuration>"

   echo $MAPRED_SITE > $HADOOP_PREFIX/conf/mapred-site.xml

   #prepare input
   mkdir input

   wget -nv -O test.txt http://www.gutenberg.org/cache/epub/1661/pg1661.txt

   #test hadoop fs shell
   if [ -z "$($HADOOP_PREFIX/bin/hadoop fs -ls /hadoop_with_ssl_test | grep test.txt)" ]
      then echo hadoop fs -ls does not show test file!; RESULT=-1;
   fi

   $HADOOP_PREFIX/bin/hadoop fs -copyFromLocal test.txt /hadoop_with_ssl_test/input/

   if [ -z "$(ls | grep test.txt)" ]
      then echo ls does not show test file!; RESULT=-1;
   fi

   #run simple hadoop-job

   echo "Start JobTracker and TaskTracker..."
   $HADOOP_PREFIX/bin/hadoop-daemon.sh start jobtracker
   $HADOOP_PREFIX/bin/hadoop-daemon.sh start tasktracker
   #wait for complete start up
   sleep 10s

   if [[ -z "$(jps | grep TaskTracker)" || -z "$(jps | grep JobTracker)" ]]
      then echo "Hadoop start up failed!"; RESULT=-1;
      else
         echo "Run wordcount"
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/hadoop-examples-$VERSION.jar wordcount /hadoop_with_ssl_test/input /hadoop_with_ssl_test/output

         JOB_STATUS=$($HADOOP_PREFIX/bin/hadoop job -list all | grep _0001 | cut -c 23)
         if [ "$JOB_STATUS" != "2" ]
            then echo "Hadoop job without buffer failed!"; RESULT=-1;
            else echo "Hadoop job without buffer  was successfull";
         fi

         $HADOOP_PREFIX/bin/hadoop fs -rmr /hadoop_with_ssl_test/output

         echo "Stop JobTracker and TaskTracker..."
         $HADOOP_PREFIX/bin/hadoop-daemon.sh stop jobtracker
         $HADOOP_PREFIX/bin/hadoop-daemon.sh stop tasktracker

         # check if JobTacker and TaskTracker stop
         if [ -n "$(jps | grep TaskTracker)" ]
         then
            echo "TaskTracker does not stop, kill manually"
            TASKTRACKER_PID=$(jps | grep TaskTracker | cut -d ' ' -f1)
            kill $TASKTRACKER_PID
         fi

         if [ -n "$(jps | grep JobTracker)" ]
         then
            echo "JobTracker does not stop, kill manually"
            JOBTRACKER_PID=$(jps | grep JobTracker | cut -d ' ' -f1)
            kill $JOBTRACKER_PID
         fi

         #kill all remaining child processes
         CHILD_PIDS=$(jps | grep Child | cut -d ' ' -f1)
         if [ -n "$CHILD_PIDS" ]
            then kill $CHILD_PIDS
         fi
   fi
done

exit $RESULT
