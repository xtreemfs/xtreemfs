#!/bin/bash

XTREEMFS=$1
TEST_DIR=$4
HADOOP_VERSIONS="1.0.4 1.1.2 1.2.1"
VOLUME="$(basename $(dirname $(pwd)))"

echo "Prepare hadoop input"

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


   echo "Copy xtreemfs-hadoop-client.jar to $HADOOP_PREFIX/lib/"
   cp $XTREEMFS/contrib/hadoop/target/xtreemfs-hadoop-client.jar $HADOOP_PREFIX/lib/

   echo "configure core-site.xml"

   CORE_SITE="
   <configuration>

   <property>
    <name>fs.xtreemfs.impl</name>
    <value>org.xtreemfs.common.clients.hadoop.XtreemFSFileSystem</value>
    <description>The file system for xtreemfs: URIs.</description>
   </property>

   <property>
    <name>fs.default.name</name>
    <value>xtreemfs://localhost:32638</value>
    <description>Address for the DIR.</description>
   </property>

   <property>
    <name>xtreemfs.defaultVolumeName</name>
    <value>$VOLUME</value>
    <description>Name of the volume to use within XtreemFS.</description>
   </property>

   <property>
    <name>xtreemfs.client.debug</name>
    <value>false</value>
   </property>

   <property>
    <name>io.file.buffer.size</name>
    <value>131072</value>
    <description>Default buffer size when accessing files.</description>
   </property>

   <property>
    <name>xtreemfs.io.read.buffer</name>
    <value>false</value>
    <description>
      Enable/Disable the read buffer in theXtreemFSHadoopClient
    </description>
   </property>

   <property>
    <name>xtreemfs.io.buffer.size.read</name>
    <value>64</value>
    <description>
      Buffer size of the read buffer in the XtreemFSHadoopClient
    </description>
   </property>

   <property>
    <name>xtreemfs.io.write.buffer</name>
    <value>false</value>
    <description>
      Enable/Disable the write buffer in the XtreemFSHadoopClient
    </description>
   </property>

   <property>
    <name>xtreemfs.io.buffer.size.write</name>
    <value>64</value>
    <description>
      Buffer size of the write buffer in the XtreemFSHadoopClient
    </description>
   </property>

   <property>
     <name>xtreemfs.jni.enabled</name>
     <value>true</value>
   </property>

   <property>
     <name>xtreemfs.jni.libraryPath</name>
     <value>$XTREEMFS/cpp/build</value>
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
    <description>Listening address for the JobTracker.</description>
   </property>

   </configuration>"

   echo $MAPRED_SITE > $HADOOP_PREFIX/conf/mapred-site.xml

   #prepare input
   mkdir input

   wget -nv -O test.txt http://www.gutenberg.org/cache/epub/1661/pg1661.txt

   #test hadoop fs shell
   if [ -z "$($HADOOP_PREFIX/bin/hadoop fs -ls /hadoop_test | grep test.txt)" ]
      then echo hadoop fs -ls does not show test file!; RESULT=-1;
   fi

   $HADOOP_PREFIX/bin/hadoop fs -copyFromLocal test.txt /hadoop_test/input/

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
         echo "Run wordcount without buffer..."
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/hadoop-examples-$VERSION.jar wordcount /hadoop_test/input /hadoop_test/output

         JOB_STATUS=$($HADOOP_PREFIX/bin/hadoop job -list all | grep _0001 | cut -c 23)
         if [ "$JOB_STATUS" != "2" ]
            then echo "Hadoop job without buffer failed!"; RESULT=-1;
            else echo "Hadoop job without buffer  was successfull";

            #verify output
            sed "s/^\(.*\)[[:space:]*]\(.*\)/\2 \1/" output/part-r-00000 | sort -bnr > hadoop_tmp.txt
            cat input/test.txt | tr -s [:space:] '\n' | grep -v "^\s*$" | sort | uniq -c | sort -bnr > cross_check.txt
            if [ -n "$(diff -w hadoop_tmp.txt cross_check.txt)" ]
               then echo "Hadoop produced wrong output!"; RESULT=-1;
            fi
         fi

         $HADOOP_PREFIX/bin/hadoop fs -rmr /hadoop_test/output

         echo "Run wordcount with buffer..."
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/hadoop-examples-$VERSION.jar wordcount -D xtreemfs.io.read.buffer=true -D xtreemfs.io.write.buffer=true /hadoop_test/input /hadoop_test/output

         JOB_STATUS=$($HADOOP_PREFIX/bin/hadoop job -list all | grep _0002 | cut -c 23)

         if [ "$JOB_STATUS" != "2" ]
            then echo "Hadoop job with buffer failed!"; RESULT=-1;
            else
               echo "Hadoop job with buffer was successfull"

            #verify output
            sed "s/^\(.*\)[[:space:]*]\(.*\)/\2 \1/" output/part-r-00000 | sort -bnr > hadoop_tmp.txt
            cat input/test.txt | tr -s [:space:] '\n' | grep -v "^\s*$" | sort | uniq -c | sort -bnr > cross_check.txt
            if [ -n "$(diff -w hadoop_tmp.txt cross_check.txt)" ]
               then echo "Hadoop produced wrong output!"; RESULT=-1;
            fi
         fi

         $HADOOP_PREFIX/bin/hadoop fs -rmr /hadoop_test/output

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
