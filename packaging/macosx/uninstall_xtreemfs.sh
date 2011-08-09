#!/bin/bash

xtreemfs_files="/usr/local/bin/mount.xtreemfs /usr/local/bin/mkfs.xtreemfs /usr/local/bin/rmfs.xtreemfs /usr/local/bin/lsfs.xtreemfs /usr/local/bin/xtfsutil /usr/local/share/xtreemfs/xtreemfs_logo_transparent.icns /sbin/mount_xtreemfs"
xtreemfs_dirs="/usr/local/share/xtreemfs"

if [ $UID -ne 0 -a -z "$SUDO_USER" ]
then
  echo "Please run this script as root. Prepend 'sudo ' to the command to do so."
  exit 1
fi

echo "Remove all XtreemFS files from the system? Please type YES."

read check
if [ "$check" = "YES" ]
then
  echo "Deleting all XtreemFS files now."
  echo
  for file in $xtreemfs_files
  do
    rm -v "$file"
  done
  echo
  for dir in $xtreemfs_dirs
  do
    rmdir "$dir"
  done

  echo
  echo "Processed all XtreemFS files and directories. If no errors occured, all files were successfully deleted."
  echo "Please delete this script manually: sudo rm /usr/local/bin/uninstall_xtreemfs.sh"
else
  echo "XtreemFS uninstallation aborted."
fi
