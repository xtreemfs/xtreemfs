#!/usr/bin/env python 
# -*- coding: utf-8  -*-

# Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

from os import path, getcwd, remove, rmdir
import subprocess
import argparse
from tempfile import mkdtemp, mkstemp
from time import sleep


# Parse the Arguments
parser = argparse.ArgumentParser()
parser.add_argument("xtreemfs_dir")
parser.add_argument("dir_url")
parser.add_argument("mrc_url")
parser.add_argument("test_dir")

args = parser.parse_args()
dir_url = args.dir_url.rstrip("/")

# Parse the volume name and the test name from the directory structure
test_name = path.basename(getcwd())
volume_name = path.basename(path.dirname(getcwd()))

# Paths of the used tools
xtfsutil = path.join(args.xtreemfs_dir, "bin", "xtfsutil")
mount_xtreemfs = path.join(args.xtreemfs_dir, "bin", "mount.xtreemfs")
umount_xtreemfs = path.join(args.xtreemfs_dir, "bin", "umount.xtreemfs")

# Store the paths to the control and the temp mountpoints 
tmpdir = mkdtemp(prefix="xtfs")
mnt_path = path.join(tmpdir, test_name)
ctrl_path = getcwd()


#################################
# Test transparent view renewal #
#################################

# Mount the volume at a temp dir
subprocess.check_call([mount_xtreemfs, 
    "--max-view-renewals", "0", 
    "--retry-delay", "1",
    dir_url + "/" + volume_name, tmpdir])

#  Create the file and write some ascii 1.
file_name = "test1"
file_mnt = path.join(mnt_path, file_name)
file_ctrl = path.join(ctrl_path, file_name)
with open(file_ctrl, "w", 0) as f: 
    f.write("1111")

# Make it read only replicated
subprocess.check_call([xtfsutil, "-r",  "RONLY", file_ctrl])

try:
    # Reopen the tmp mounted file unbuffered and read after the view was 
    # changed on the control volume
    with open(file_mnt, "r", 0) as f:

        # Add a replica to assert the XLocSet version increases.
        subprocess.check_call([xtfsutil, "-a", file_ctrl])
        
        # Try to read from the file and assert its content is the same.
        result = f.read(1)
        assert result == "1", "Read returned wrong file contents."
finally: 
    # Cleanup
    subprocess.call([umount_xtreemfs, tmpdir])
    remove(file_ctrl)


#######################################################
# Test error on invalid view (with renewals disabled) #
#######################################################

# Mount the volume at a temp dir and log its output
_, logfile = mkstemp(suffix=".log", prefix=tmpdir)
subprocess.check_call([mount_xtreemfs, 
    "--max-view-renewals", "1", 
    "--retry-delay", "1",
    "-l", logfile, "-d", "WARNING",
    dir_url + "/" + volume_name, tmpdir])

#  Create the file and write some ascii 1.
file_name = "test2"
file_mnt = path.join(mnt_path, file_name)
file_ctrl = path.join(ctrl_path, file_name)
with open(file_ctrl, "w", 0) as f: 
    f.write("1111")

# Make it read only replicated
subprocess.check_call([xtfsutil, "-r",  "RONLY", file_ctrl])

try:
    # Reopen the tmp mounted file unbuffered and read after the view was
    # changed on the control volume
    with open(file_mnt, "r", 0) as f:

        # Add a replica to assert the XLocSet version increases.
        subprocess.check_call([xtfsutil, "-a", file_ctrl])
        
        # Try to read from the file and assert its content is the same.
        # Should throw IOError
        result = f.read(1)

        assert False, "The Filehandle should be invalid due to the new view"

except IOError as e:
    assert True
finally: 
    # Cleanup
    subprocess.call([umount_xtreemfs, tmpdir])
    remove(file_ctrl)

view_error = False
with open(logfile, "r") as f:
    for line in f:
        view_error = view_error or (
            "denied the requested operation because the clients view is " 
            "outdated. The request will be retried once the view is renewed." 
                in line)


# Cleanup tmpfiles
remove(logfile)
rmdir(tmpdir)

assert view_error, "View error should have occured"
