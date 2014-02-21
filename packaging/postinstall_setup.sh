#!/bin/bash
set -e

XTREEMFS_LOG_DIR=/var/log/xtreemfs
XTREEMFS_HOME=/var/lib/xtreemfs
XTREEMFS_ETC=/etc/xos/xtreemfs
XTREEMFS_USER=xtreemfs
XTREEMFS_GROUP=xtreemfs
XTREEMFS_GENERATE_UUID_SCRIPT="${XTREEMFS_ETC}/generate_uuid"

# When executed during POST installation, do not be verbose.
VERBOSE=0
script_name=$(basename "$0")
if [ "$script_name" = "postinstall_setup.sh" ]
then
  VERBOSE=1
fi

# generate UUIDs
if [ -x "$XTREEMFS_GENERATE_UUID_SCRIPT" ]; then
  for service in dir mrc osd; do
    "$XTREEMFS_GENERATE_UUID_SCRIPT" "${XTREEMFS_ETC}/${service}config.properties"
    [ $VERBOSE -eq 1 ] && echo "Generated UUID for service: $service"
  done
else
  echo "UUID can't be generated automatically. Please enter a correct UUID in each config file of an XtreemFS service."
fi


group_exists=`grep -c $XTREEMFS_GROUP /etc/group || true`
if [ $group_exists -eq 0 ]; then
    groupadd $XTREEMFS_GROUP
    [ $VERBOSE -eq 1 ] && echo "created group $XTREEMFS_GROUP"
fi
exists=`grep -c $XTREEMFS_USER /etc/passwd || true`
if [ $exists -eq 0 ]; then
    mkdir $XTREEMFS_HOME
    useradd -r --home $XTREEMFS_HOME -g $XTREEMFS_GROUP $XTREEMFS_USER
    chown $XTREEMFS_USER $XTREEMFS_HOME
    [ $VERBOSE -eq 1 ] && echo "created user $XTREEMFS_USER and data directory $XTREEMFS_HOME"
fi
if [ ! -d $XTREEMFS_HOME ]; then
    mkdir -m750 $XTREEMFS_HOME
    chown $XTREEMFS_USER $XTREEMFS_HOME
    [ $VERBOSE -eq 1 ] && echo "user $XTREEMFS_USER exists but data directory $XTREEMFS_HOME had to be created"
fi
owner=`stat -c %U $XTREEMFS_HOME`
if [ "$owner" != "$XTREEMFS_USER" ]; then
    [ $VERBOSE -eq 1 ] && echo "directory $XTREEMFS_HOME is not owned by $XTREEMFS_USER, executing chown"
    chown $XTREEMFS_USER $XTREEMFS_HOME
fi

if [ ! -e $XTREEMFS_LOG_DIR ]; then
    mkdir $XTREEMFS_LOG_DIR
    chown -R $XTREEMFS_USER $XTREEMFS_LOG_DIR
fi

if [ -e $XTREEMFS_ETC ]; then
    group=`stat -c %G $XTREEMFS_ETC 2>/dev/null`
    if [ $group != $XTREEMFS_GROUP ]; then
        [ $VERBOSE -eq 1 ] && echo "directory $XTREEMFS_ETC is owned by $group, should be owned by $XTREEMFS_GROUP, executing chgrp (may take some time)"
        chgrp -R $XTREEMFS_GROUP $XTREEMFS_ETC
    fi
    for file in `ls $XTREEMFS_ETC/*.properties 2>/dev/null`; do
      if [ -f $file -a "$(stat -c %a $file)" != "640" ]; then
          [ $VERBOSE -eq 1 ] && echo "setting $file 0640, executing chmod"
          chmod 0640 $file
      fi
    done
    if [ -d "$XTREEMFS_ETC/truststore/" ]
    then
        if [ "$(stat -c %a "$XTREEMFS_ETC/truststore/")" != "750" ]
        then
            [ $VERBOSE -eq 1 ] && echo "setting $XTREEMFS_ETC/truststore/ to 0750, executing chmod (may take some time)"
            chmod -R u=rwX,g=rX,o= $XTREEMFS_ETC/truststore/
        fi
    fi
fi
