#!/bin/bash
set -e

XTREEMFS_LOG_DIR=/var/log/xtreemfs
XTREEMFS_HOME=/var/lib/xtreemfs
XTREEMFS_ETC=/etc/xos/xtreemfs
XTREEMFS_USER=xtreemfs
XTREEMFS_GROUP=xtreemfs

group_exists=`grep -c 1$XTREEMFS_GROUP /etc/group || true`
if [ $group_exists -eq 0 ]; then
    groupadd $XTREEMFS_GROUP
    echo "created group $XTREEMFS_GROUP"
fi
exists=`grep -c $XTREEMFS_USER /etc/passwd || true`
if [ $exists -eq 0 ]; then
    mkdir $XTREEMFS_HOME
    useradd -r --home $XTREEMFS_HOME -g $XTREEMFS_GROUP $XTREEMFS_USER
    chown $XTREEMFS_USER $XTREEMFS_HOME
    echo "created user $XTREEMFS_USER and data directory $XTREEMFS_HOME"
else
    if [ ! -d $XTREEMFS_HOME ]; then
        mkdir $XTREEMFS_HOME
        echo "user $XTREEMFS_USER exists but created data directory $XTREEMFS_HOME"
    fi
    owner=`stat -c %U $XTREEMFS_HOME`
    if [ $owner != $XTREEMFS_USER ]; then
        echo "directory $XTREEMFS_HOME is not owned by $XTREEMFS_USER, executing chmod (may take some time)"
        chown -R $XTREEMFS_USER $XTREEMFS_HOME
    fi
fi

if [ ! -e $XTREEMFS_LOG_DIR ]; then
    mkdir $XTREEMFS_LOG_DIR
    chown -R $XTREEMFS_USER $XTREEMFS_LOG_DIR
fi

if [ -e $XTREEMFS_ETC ]; then
    group=`stat -c %G $XTREEMFS_ETC`
    if [ $group != $XTREEMFS_GROUP ]; then
        echo "directory $XTREEMFS_ETC is not owned by $XTREEMFS_GROUP, executing chmod (may take some time)"
        chgrp -R $XTREEMFS_GROUP $XTREEMFS_ETC
    fi
    if [ -f $XTREEMFS_ETC/*.properties ]; then
        echo "setting $XTREEMFS_ETC/*.properties 0750, executing chmod"
        chmod 0750 $XTREEMFS_ETC/*.properties
    fi
    if [ -d "$XTREEMFS_ETC/truststore/" ]
    then
        echo "setting $XTREEMFS_ETC/truststore/ to 0750, executing chmod (may take some time)"
        chmod -R 0750 $XTREEMFS_ETC/truststore/
    fi
fi
