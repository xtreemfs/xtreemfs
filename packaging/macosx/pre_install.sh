#!/bin/bash

XTREEMFS_UNINSTALL_SCRIPT="/usr/local/bin/uninstall_xtreemfs.sh"

# Uninstall previous XtreemFS version.
if [ -x "$XTREEMFS_UNINSTALL_SCRIPT" ]
then
  "$XTREEMFS_UNINSTALL_SCRIPT" noprompt
  rm "$XTREEMFS_UNINSTALL_SCRIPT"
fi

# Create directories which do not exist yet.
xtreemfs_dirs="/usr/local/share/xtreemfs /usr/local/bin"
for dir in $xtreemfs_dirs
do
  if [ ! -d "$dir" ]
  then
    mkdir -p "$dir"
    chmod 755 "$dir"
    chown root:wheel "$dir"
  fi
done

# Create symlink "mount_xtreemfs" for Mac.
ln -s /usr/local/bin/mount.xtreemfs /sbin/mount_xtreemfs

# Exit succesfully.
exit 0
