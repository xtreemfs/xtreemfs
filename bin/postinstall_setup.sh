#!/bin/bash

XTREEMFS_LOG_DIR=/var/log/xtreemfs
XTREEMFS_HOME=/var/lib/xtreemfs
XTREEMFS_USER=xtreemfs

exists=`grep -c $XTREEMFS_USER /etc/passwd`
if [ $exists -eq 0 ]
then
        mkdir $XTREEMFS_HOME
        useradd -r --home $XTREEMFS_HOME $XTREEMFS_USER
        chown $XTREEMFS_USER $XTREEMFS_HOME
        echo "created user $XTREEMFS_USER and data directory $XTREEMFS_HOME"
else
        if [ ! -d $XTREEMFS_HOME ]
        then
            mkdir $XTREEMFS_HOME
            echo "user $XTREEMFS_USER exists but created data directory $XTREEMFS_HOME"
        fi
        owner=`stat -c %U $XTREEMFS_HOME`
        if [ $owner != $XTREEMFS_USER ]
        then
            echo "directory $XTREEMFS_HOME is not owned by $XTREEMFS_USER, executing chmod (may take some time)"
            chown -R $XTREEMFS_USER $XTREEMFS_HOME
        fi
fi

if [ ! -e $XTREEMFS_LOG_DIR ]
then
        mkdir $XTREEMFS_LOG_DIR
        chown -R $XTREEMFS_USER $XTREEMFS_LOG_DIR
fi