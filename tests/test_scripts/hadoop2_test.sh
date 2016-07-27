#!/bin/bash

XTREEMFS=$1
TEST_DIR=$4
HADOOP_VERSIONS="2.2.0 2.3.0 2.4.1 2.5.2 2.6.4 2.7.2"
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

   export HADOOP_MAPRED_HOME=$HADOOP_PREFIX
   export HADOOP_COMMON_HOME=$HADOOP_PREFIX
   export HADOOP_HDFS_HOME=$HADOOP_PREFIX
   export YARN_HOME=$HADOOP_PREFIX

   export HADOOP_CONF_DIR=$HADOOP_PREFIX/etc/hadoop
   echo "Set HADOOP_CONF_DIR=$HADOOP_CONF_DIR"

   export HADOOP_LOG_DIR="$TEST_DIR/log/hadoop.log"
   echo "Set HADOOP_LOG_DIR=$HADOOP_LOG_DIR"

   echo "Copy xtreemfs-hadoop-client.jar to $HADOOP_PREFIX/share/hadoop/common"
   cp $XTREEMFS/contrib/hadoop/target/xtreemfs-hadoop-client.jar $HADOOP_PREFIX/share/hadoop/common

   echo "configure core-site.xml"

   CORE_SITE="
   <configuration>

   <property>
    <name>fs.xtreemfs.impl</name>
    <value>org.xtreemfs.common.clients.hadoop.XtreemFSFileSystem</value>
    <description>The file system for xtreemfs: URIs.</description>
   </property>

   <property>
    <name>fs.AbstractFileSystem.xtreemfs.impl</name>
    <value>org.xtreemfs.common.clients.hadoop.XtreemFS</value>
   </property>

   <property>
    <name>fs.defaultFS</name>
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

   echo $CORE_SITE > $HADOOP_CONF_DIR/core-site.xml

   echo "configure mapred-site.xml"

   MAPRED_SITE="
   <configuration>

   <property>
    <name>mapred.job.tracker</name>
    <value>localhost:9001</value>
    <description>Listening address for the JobTracker.</description>
   </property>

   <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
   </property>

   </configuration>"

   echo $MAPRED_SITE > $HADOOP_CONF_DIR/mapred-site.xml

   echo "configure yarn-site.xml"

   YARN_SITE="
   <configuration>

   <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
   </property>

   <property>
    <name>yarn.nodemanager.aux-services.mapreduce_shuffle.class</name>
    <value>org.apache.hadoop.mapred.ShuffleHandler</value>
   </property>

   </configuration>"

   echo $YARN_SITE > $HADOOP_CONF_DIR/yarn-site.xml

   #prepare input
   mkdir input

   wget -nv http://www.gutenberg.org/ebooks/76.txt.utf-8
   iconv -c -f utf-8 -t ascii 76.txt.utf-8 > test.txt

   #test hadoop fs shell
   if [ -z "$($HADOOP_PREFIX/bin/hadoop fs -ls /hadoop2_test | grep test.txt)" ]
      then echo "hadoop fs -ls does not show test file!"; RESULT=-1;
   fi

   $HADOOP_PREFIX/bin/hadoop fs -copyFromLocal test.txt /hadoop2_test/input/

   if [ -z "$(ls | grep test.txt)" ]
      then echo "ls does not show test file!"; RESULT=-1;
   fi

   #run simple hadoop-job

   echo "Start NodeManager, ResourceManager and JobHistoryServer..."
   $HADOOP_PREFIX/sbin/yarn-daemon.sh start nodemanager
   $HADOOP_PREFIX/sbin/yarn-daemon.sh start resourcemanager
   $HADOOP_PREFIX/sbin/mr-jobhistory-daemon.sh start historyserver
   #wait for complete start up
   sleep 10s

   if [[ -z "$(jps | grep NodeManager)" || -z "$(jps | grep ResourceManager)" || -z "$(jps | grep JobHistoryServer)" ]]
      then echo "Hadoop start up failed!"; RESULT=-1;
      else
         echo "Run wordcount without buffer..."
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/share/hadoop/mapreduce/hadoop-mapreduce-examples-$VERSION.jar  wordcount /hadoop2_test/input /hadoop2_test/output

         if [ -z "$($HADOOP_PREFIX/bin/mapred job -list all | grep _0001.*SUCCEEDED)" ]
            then echo "Hadoop job without buffer failed!"; RESULT=-1;
            else echo "Hadoop job without buffer was successfull";

            #verify output
            sed "s/^\(.*\)[[:space:]*]\(.*\)/\2 \1/" output/part-r-00000 | sort -bnr > hadoop_tmp.txt
            cat input/test.txt | tr -s [:space:] '\n' | grep -v "^\s*$" | sort | uniq -c | sort -bnr > cross_check.txt
            if [ -n "$(diff -w hadoop_tmp.txt cross_check.txt)" ]
               then echo "Hadoop produced wrong output!"; RESULT=-1;
            fi
         fi
         $HADOOP_PREFIX/bin/hadoop fs -rm -r /hadoop2_test/output

         echo "Run wordcount with buffer..."
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/share/hadoop/mapreduce/hadoop-mapreduce-examples-$VERSION.jar wordcount -D xtreemfs.io.read.buffer=true -D xtreemfs.io.write.buffer=true /hadoop2_test/input /hadoop2_test/output


         if [ -z "$($HADOOP_PREFIX/bin/mapred job -list all | grep _0002.*SUCCEEDED)" ]
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
   fi

   echo "Stop Hadoop..."
   $HADOOP_PREFIX/sbin/yarn-daemon.sh stop nodemanager
   $HADOOP_PREFIX/sbin/yarn-daemon.sh stop resourcemanager
   $HADOOP_PREFIX/sbin/mr-jobhistory-daemon.sh stop historyserver

   # check if servers stop
   if [ -n "$(jps | grep NodeManager)" ]
   then
      echo "NodeManager does not stop, kill manually"
      NODEMANAGER_PID=$(jps | grep NodeManager | cut -d ' ' -f1)
      kill $NODEMANAGER_PID
   fi

   if [ -n "$(jps | grep ResourceManager)" ]
   then
      echo "ResourceManager does not stop, kill manually"
      RESOURCEMANAGER_PID=$(jps | grep ResourceManager | cut -d ' ' -f1)
      kill $RESOURCEMANAGER_PID
   fi

   if [ -n "$(jps | grep JobHistoryServer)" ]
   then
      echo "JobHistoryServer does not stop, kill manually"
      HISTORYSERVER_PID=$(jps | grep JobHistoryServer | cut -d ' ' -f1)
      kill $HISTORYSERVER_PID
   fi
   #kill all remaining child processes
   CHILD_PIDS=$(jps | grep Child | cut -d ' ' -f1)
   if [ -n "$CHILD_PIDS" ]
      then kill $CHILD_PIDS
   fi
done

exit $RESULT
