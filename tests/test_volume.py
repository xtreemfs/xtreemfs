# Copyright (c) 2009-2011 by Minor Gordon, Bjoern Kolbeck, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

from time import sleep
import sys, os, subprocess, signal

DEBUG_LEVELS = [ 'EMERG', 'ALERT', 'CRIT', 'ERR', 'WARNING', 'NOTICE', 'INFO', 'DEBUG' ]

class Volume:
    def __init__(self,
                 name,
                 mount_point_dir_path,
                 xtreemfs_dir,
                 debug_level,
                 mount_options,
                 mkfs_options,
                 mrc_uri,
                 dir_uri,
                 pkcs12_file_path,
                 pkcs12_passphrase,
                 stripe_width,
                 stripe_size,
                 stripe_parity_width,
                 rwr_policy,
                 rwr_factor,
                 ronly_factor):
        self.__mount_point_dir_path = os.path.abspath(mount_point_dir_path)
        self.__name = name
        self.__debug_level = debug_level
        self.__xtreemfs_dir = xtreemfs_dir
        self.__mount_options = mount_options
        self.__mkfs_options = mkfs_options
        self.__mrc_uri = mrc_uri
        if not mrc_uri.endswith("/"):
            self.__mrc_uri += "/"

        self.__dir_uri = dir_uri
        self.__pkcs12_file_path = pkcs12_file_path
        self.__pkcs12_passphrase = pkcs12_passphrase
        self.__stripe_width = stripe_width
        self.__stripe_size = stripe_size
        self.__stripe_parity_width = stripe_parity_width
        self.__rwr_policy = rwr_policy
        self.__rwr_factor = rwr_factor
        self.__ronly_factor = ronly_factor
        

    def create(self):
        mkfs_xtreemfs_file_path = os.path.abspath(os.path.join(self.__xtreemfs_dir, "bin", "mkfs.xtreemfs"))
        if not os.path.exists(mkfs_xtreemfs_file_path):
            mkfs_xtreemfs_file_path = "mkfs.xtreemfs" # Assume it's in the global path

        mkfs_xtreemfs_args = [mkfs_xtreemfs_file_path]
        mkfs_xtreemfs_args.extend(("-d", DEBUG_LEVELS[int(self.__debug_level)]))
        mkfs_xtreemfs_args.extend(("-p", 'RAID0'))
        if self.__pkcs12_file_path is not None: mkfs_xtreemfs_args.extend(("--pkcs12-file-path", self.__pkcs12_file_path))
        if self.__pkcs12_passphrase is not None: mkfs_xtreemfs_args.extend(("--pkcs12-passphrase", self.__pkcs12_passphrase))
        mkfs_xtreemfs_args.extend(("-s", str(self.__stripe_size)))
        mkfs_xtreemfs_args.extend(("-w", str(self.__stripe_width)))
        mkfs_xtreemfs_args.extend(self.__mkfs_options)

        mkfs_xtreemfs_args.append(self.__mrc_uri + self.__name)
        mkfs_xtreemfs_args = " ".join(mkfs_xtreemfs_args)
        print "xtestenv: creating volume", self.__name, "with", mkfs_xtreemfs_args

        retcode = subprocess.call(mkfs_xtreemfs_args, shell=True)
        if retcode != 0:
            raise RuntimeError("Failed to create volume: " + self.__name + " You can use the option --clean-test-dir to clean previous data from the test dir. mkfs.xtreemfs return value: " + str(retcode) + " Executed command: " + mkfs_xtreemfs_args)

    def get_mount_point_dir_path(self):
        return self.__mount_point_dir_path

    def get_name(self):
        return self.__name

    def mount(self, log_file_path):
        xtfsutil_file_path = os.path.abspath(os.path.join(self.__xtreemfs_dir, "bin", "xtfsutil"))
        if not os.path.exists(xtfsutil_file_path):
            xtfsutil_file_path = "xtfsutil" # Assume it's in the global path
        
        try: os.mkdir(self.__mount_point_dir_path)
        except: pass

        mount_xtreemfs_file_path = os.path.abspath(os.path.join(self.__xtreemfs_dir, "bin", "mount.xtreemfs"))
        if not os.path.exists(mount_xtreemfs_file_path):
            mount_xtreemfs_file_path = "mount.xtreemfs" # Assume it's in the global path

        mount_xtreemfs_args = [mount_xtreemfs_file_path]
        mount_xtreemfs_args.append("-f") # So we can redirect stdout and stderr
        mount_xtreemfs_args.extend(("-d", DEBUG_LEVELS[int(self.__debug_level)]))
        mount_xtreemfs_args.extend(self.__mount_options)
        if self.__pkcs12_file_path is not None: mount_xtreemfs_args.extend(("--pkcs12-file-path", self.__pkcs12_file_path))
        if self.__pkcs12_passphrase is not None: mount_xtreemfs_args.extend(("--pkcs12-passphrase", self.__pkcs12_passphrase))

        volume_uri = self.__dir_uri
        if not volume_uri.endswith("/"): volume_uri += "/"
        volume_uri += self.__name
        mount_xtreemfs_args.append(volume_uri)

        mount_xtreemfs_args.append(self.get_mount_point_dir_path())

        if log_file_path is None:
            stdout = sys.stdout
            stderr = sys.stderr
        else:
            stderr = stdout = open(log_file_path, "a")

        print "xtestenv: mounting volume", self.__name, "at", self.get_mount_point_dir_path(), "with", " ".join(mount_xtreemfs_args)

        # Use subprocess.Popen instead of subprocess.call to run in the background
        p = subprocess.Popen(mount_xtreemfs_args, stderr=stderr, stdout=stdout)
        sleep(1.0)
        if p.returncode is not None:
            raise RuntimeError("Failed to mount volume '" + self.__name + "' error: " + str(p.returncode))
          
        # Use 'waitpid' to touch any zombies and ensure that these are cleaned up first before checking /proc/<pid>.
        try: os.waitpid(p.pid, os.WNOHANG)
        # We dont care about the actual result of waitpid.
        except OSError: pass
          
        if not os.path.exists("/proc/" + str(p.pid)):
            raise RuntimeError("Failed to mount volume '" + self.__name + "' error: mount.xtreemfs did not successfully start.")

        # enable replication
        if self.__rwr_factor > 0:
            command = (xtfsutil_file_path + " " +
                       "--set-drp " +
                       "--replication-policy="+self.__rwr_policy + " " +
                       "--replication-factor="+str(self.__rwr_factor) + " " +
                       self.__mount_point_dir_path)
            retcode = subprocess.call(command, shell=True)
            if retcode != 0:
                raise RuntimeError("Failed to enable read-write replication on volume: " + self.__name
                    + " xtfsutil return value: " + str(retcode)
                    + " Executed command: " + command)

        # enable replicate on close for ronly replication
        if self.__ronly_factor > 0:
            command = (xtfsutil_file_path + " " +
                       "--set-drp " +
                       "--replication-policy=readonly " +
                       "--replication-factor="+str(self.__rwr_factor) + " " +
                       self.__mount_point_dir_path)
            retcode = subprocess.call(command, shell=True)
            if retcode != 0:
                raise RuntimeError("Failed to enable read/only replication on volume: " + self.__name
                    + " xtfsutil return value: " + str(retcode)
                    + " Executed command: " + command)

        if self.__stripe_parity_width > 0:
            command = (xtfsutil_file_path + " " +
                       "--set-dsp " +
                       "--striping-policy=EC " +
                       "--striping-policy-width=" + str(self.__stripe_width) + " " +
                       "--striping-policy-stripe-size=" + str(self.__stripe_size) + " " +
                       "--striping-policy-parity=" + str(self.__stripe_parity_width) + " " +
                       self.__mount_point_dir_path)
            retcode = subprocess.call(command, shell=True)
            if retcode != 0:
                raise RuntimeError("Failed to enable erasure code replication on volume: " + self.__name
                    + " xtfsutil return value: " + str(retcode)
                    + " Executed command: " + command)

    def unmount(self):
        for mounts_line in open("/proc/mounts").readlines():
            mounts_line_parts = mounts_line.split()
            test_device = mounts_line_parts[0]
            test_mount_point_dir_path = mounts_line_parts[1]
            if test_mount_point_dir_path.endswith(self.get_mount_point_dir_path()):
                fusermount_args = " ".join(["fusermount", "-u", "-z", self.get_mount_point_dir_path()])
                print "xtestenv: unmounting volume", self.get_name(), "with", fusermount_args
                retcode = subprocess.call(fusermount_args, shell=True)
                if retcode != 0:
                    print("Failed to unmount volume: " + self.__name + " fusermount -u return value: " + str(retcode))

