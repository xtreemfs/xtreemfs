#!/bin/bash

XTREEMFS=$1
TEST_DIR=$4
HADOOP_VERSIONS="1.2.1"
VOLUME="$(basename $(dirname $(pwd)))"

echo "Prepare hadoop input"
   mkdir input
   echo "foo foo bar bar foo bar foo" > input/test1.txt
   echo "foo foo bar bar bar foo bar" > input/test2.txt

for VERSION in $HADOOP_VERSIONS; do

   echo "Test XtreemFS with hadoop $VERSION"

   #download and extract hadoop
   echo "Download Hadoop $VERSION..."
   wget -nv -O $TEST_DIR/hadoop-$VERSION.tar.gz http://archive.apache.org/dist/hadoop/core/hadoop-$VERSION/hadoop-$VERSION.tar.gz
   VOLUME_DIR=$PWD
   cd $TEST_DIR
   echo "Extract Hadoop $VERSION..."
   tar -zxf $TEST_DIR/hadoop-$VERSION.tar.gz
   cd $VOLUME_DIR

   #configure hadoop

   export HADOOP_PREFIX=$TEST_DIR/hadoop-$VERSION
   echo "Set HADOOP_PREFIX=$HADOOP_PREFIX"

   export HADOOP_CONF_DIR=$HADOOP_PREFIX/conf/
   echo "Set HADOOP_CONF_DIR=$HADOOP_CONF_DIR"

   echo "Set JAVA_HOME=$JAVA_HOME"
   sed -i 's/\(# export JAVA_HOME=\).*/export JAVA_HOME=$JAVA_HOME/' $HADOOP_PREFIX/conf/hadoop-env.sh

   echo "Copy XtreeemFSHadoopClient.jar to $HADOOP_PREFIX/lib/"
   cp $XTREEMFS/contrib/hadoop/dist/XtreemFSHadoopClient.jar $HADOOP_PREFIX/lib/

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
    <name>xtreemfs.volumeName</name>
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

   #run simple hadoop-job

   echo "Start JobTracker and TaskTracker..."
   $HADOOP_PREFIX/bin/start-mapred.sh
   #wait for complete start up
   sleep 10s
   
   if [[ -z $(jps | grep TaskTracker) || -z $(jps | grep JobTracker) ]]; 
      then echo "Hadoop start up failed!"; RESULT=-1;
      else
         echo "Run wordcount without buffer..."
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/hadoop-examples-$VERSION.jar wordcount /hadoop_test/input /hadoop_test/output

         JOB_STATUS=$($HADOOP_PREFIX/bin/hadoop job -list all | grep _0001 | cut -c 23)
         if [ $JOB_STATUS != "2" ];
            then echo "Hadoop job without buffer failed!"; RESULT=-1;
            else echo "Hadoop job without buffer  was successfull";
         fi

         rm -r $VOLUME_DIR/output
         
         echo "Run wordcount with buffer..."
         $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/hadoop-examples-$VERSION.jar wordcount -D xtreemfs.io.read.buffer=true -D xtreemfs.io.write.buffer=true /hadoop_test/input /hadoop_test/output

         rm -r $VOLUME_DIR/output

         JOB_STATUS=$($HADOOP_PREFIX/bin/hadoop job -list all | grep _0002 | cut -c 23) 
        
         if [ $JOB_STATUS != "2" ];
            then echo "Hadoop job with buffer failed!"; RESULT=-1;
            else echo "Hadoop job with buffer was successfull";
         fi
       

         echo "Stop JobTracker and TaskTracker..."
         $HADOOP_PREFIX/bin/stop-mapred.sh
   fi
done

exit $RESULT
