/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#define FUSE_USE_VERSION 26

#include <errno.h>
#include <csignal>
#include <cstdio>
#include <cstring>
#include <fuse.h>
#include <sys/stat.h>
#include <unistd.h>

#include <algorithm>
#include <iostream>
#include <list>
#include <string>
#include <vector>

#include "util/logging.h"

#include "fuse/fuse_adapter.h"
#include "fuse/fuse_operations.h"
#include "fuse/fuse_options.h"
#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;
using namespace xtreemfs::util;

int main(int argc, char **argv) {
  // Parse command line options.
  xtreemfs::FuseOptions options;
  bool invalid_commandline_parameters = false;
  try {
    options.ParseCommandLine(argc, argv);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "Invalid parameters found, error: " << e.what() << endl << endl;
    invalid_commandline_parameters = true;
  }
  // Display help if needed.
  if (options.empty_arguments_list) {
    cout << options.ShowCommandLineUsage() << endl;
    return 1;
  }
  if (options.show_help || invalid_commandline_parameters) {
    cout << options.ShowCommandLineHelp() << endl;
    return 1;
  }
  // Show only the version.
  if (options.show_version) {
    cout << options.ShowVersion("mount.xtreemfs") << endl;
    return 1;
  }

  // In case of background operation: Fork before threads are created.
  int fd[2];
  int kErrorBufferSize = 1024;
  char error_output[kErrorBufferSize];
  memset(&error_output, 0, kErrorBufferSize);
  if (!options.foreground) {
    if (pipe(fd) < 0) {
      cerr << "Failed to create pipe. (Needed to send process to background.)";
      return 2;
    }
    pid_t pid = fork();
    if (pid < 0) {
      cerr << "Failed to fork(). (Needed to send process to background.)";
      return 3;
    }
    // Evaluate pipe from daemonized thread.
    if (pid > 0) {  // Parent
      // Cleanup the static memory in the parent to pass the valgrind
      // leak check.
      google::protobuf::ShutdownProtobufLibrary();

      signal(SIGINT, SIG_IGN);  // Ignore interrupt signals in parent.
      // Close write end.
      close(fd[1]);
      fd_set read_fds;
      FD_ZERO(&read_fds);
      FD_SET(fd[0], &read_fds);
      // Wait until there is something to read at the other end.
      select(fd[0]+1, &read_fds, NULL, NULL, NULL);
      int count = read(fd[0], error_output, kErrorBufferSize);
      if (count == 0) {
        // No error found, exiting.
        return 0;
      } else {
        printf("mount.xtreemfs failed: %s\n", error_output);
        return 4;
      }
    } else {  // Child.
      // Close read end of pipe.
      close(fd[0]);
    }
  }

  // Child only from here.
  // Run client and open volume.
  list<char*> required_fuse_options;
  try {
    fuse_adapter = new xtreemfs::FuseAdapter(&options);
    fuse_adapter->Start(&required_fuse_options);
  } catch(const xtreemfs::XtreemFSException& e) {
    if (options.foreground) {
      cerr << "mount.xtreemfs failed: " << e.what() << endl;
    } else {
      // Tell parent about error: write error to pipe and exit.
      write(fd[1], e.what(), min(static_cast<int>(strlen(e.what()) + 1),
                                 kErrorBufferSize));
    }
    fuse_adapter->Stop();
    delete fuse_adapter;
    return 5;
  }

  // Setup fuse and pass client and volume objects.
  struct fuse_chan* fuse_channel = NULL;
  struct fuse* fuse_ = NULL;
  char* mount_point = NULL;
  // Fill in operations.
  struct fuse_operations xtreemfs_fuse_ops;
  xtreemfs_fuse_ops.getattr = xtreemfs_fuse_getattr;
  xtreemfs_fuse_ops.readlink = xtreemfs_fuse_readlink;
  // no .getdir -- that's deprecated
  xtreemfs_fuse_ops.getdir = NULL;
  xtreemfs_fuse_ops.mknod = xtreemfs_fuse_mknod;
  xtreemfs_fuse_ops.mkdir = xtreemfs_fuse_mkdir;
  xtreemfs_fuse_ops.unlink = xtreemfs_fuse_unlink;
  xtreemfs_fuse_ops.rmdir = xtreemfs_fuse_rmdir;
  xtreemfs_fuse_ops.symlink = xtreemfs_fuse_symlink;
  xtreemfs_fuse_ops.rename = xtreemfs_fuse_rename;
  xtreemfs_fuse_ops.link = xtreemfs_fuse_link;
  xtreemfs_fuse_ops.chmod = xtreemfs_fuse_chmod;
  xtreemfs_fuse_ops.chown = xtreemfs_fuse_chown;
  xtreemfs_fuse_ops.truncate = xtreemfs_fuse_truncate;
  xtreemfs_fuse_ops.utime = xtreemfs_fuse_utime;
  xtreemfs_fuse_ops.open = xtreemfs_fuse_open;
  xtreemfs_fuse_ops.read = xtreemfs_fuse_read;
  xtreemfs_fuse_ops.write = xtreemfs_fuse_write;
  xtreemfs_fuse_ops.statfs = xtreemfs_fuse_statfs;
  xtreemfs_fuse_ops.flush = xtreemfs_fuse_flush;
  xtreemfs_fuse_ops.release = xtreemfs_fuse_release;
  xtreemfs_fuse_ops.fsync = xtreemfs_fuse_fsync;
  xtreemfs_fuse_ops.setxattr = xtreemfs_fuse_setxattr;
  xtreemfs_fuse_ops.getxattr = xtreemfs_fuse_getxattr;
  xtreemfs_fuse_ops.listxattr = xtreemfs_fuse_listxattr;
  xtreemfs_fuse_ops.removexattr = xtreemfs_fuse_removexattr;
  xtreemfs_fuse_ops.opendir = xtreemfs_fuse_opendir;
  xtreemfs_fuse_ops.readdir = xtreemfs_fuse_readdir;
  xtreemfs_fuse_ops.releasedir = xtreemfs_fuse_releasedir;
  xtreemfs_fuse_ops.fsyncdir = xtreemfs_fuse_fsyncdir;
  xtreemfs_fuse_ops.init = xtreemfs_fuse_init;
  xtreemfs_fuse_ops.destroy = xtreemfs_fuse_destroy;
  xtreemfs_fuse_ops.access = xtreemfs_fuse_access;
  xtreemfs_fuse_ops.create = xtreemfs_fuse_create;
  xtreemfs_fuse_ops.ftruncate = xtreemfs_fuse_ftruncate;
  xtreemfs_fuse_ops.fgetattr = xtreemfs_fuse_fgetattr;
  xtreemfs_fuse_ops.lock = xtreemfs_fuse_lock;
  xtreemfs_fuse_ops.utimens = xtreemfs_fuse_utimens;

  xtreemfs_fuse_ops.bmap = NULL;

#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )  // NOLINT
  xtreemfs_fuse_ops.ioctl = NULL;
  xtreemfs_fuse_ops.poll= NULL;
#endif


  // Forward args.
  vector<char*> fuse_opts;
  // Fuse does not parse the first parameter, thus set it to "mount.xtreemfs".
  fuse_opts.push_back((strdup("mount.xtreemfs")));
  for (int i = 0; i < options.fuse_options.size(); i++) {
    // Prepend "-o" to every Fuse option.
    fuse_opts.push_back(strdup(
        (string("-o") + options.fuse_options[i]).c_str()));
  }
  for (list<char*>::iterator it = required_fuse_options.begin();
       it != required_fuse_options.end(); ++it) {
    fuse_opts.push_back((*it));
  }
  struct fuse_args fuse_args = FUSE_ARGS_INIT(fuse_opts.size(), &fuse_opts[0]);
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "About to call fuse_mount using "
        << fuse_opts.size() << " parameters: " << endl;
    for (int i = 0; i < fuse_opts.size(); i++) {
      Logging::log->getLog(LEVEL_DEBUG) << "\t" << fuse_opts[i] << endl;
    }
  }

  // Create Fuse channel (mount_point will be freed by fuse_teardown()).
  mount_point = strdup(options.mount_point.c_str());
  fuse_channel = fuse_mount(mount_point, &fuse_args);
  if (fuse_channel == NULL) {
    fuse_opt_free_args(&fuse_args);
    for (int i = 0; i < fuse_opts.size(); i++) {
      free(fuse_opts[i]);
    }
    free(mount_point);
    // Stop FuseAdapter.
    fuse_adapter->Stop();
    delete fuse_adapter;
    return errno;
  }
  // Create Fuse filesystem.
  fuse_ = fuse_new(
      fuse_channel,
      &fuse_args,
      &xtreemfs_fuse_ops,
      sizeof(xtreemfs_fuse_ops),
      NULL);
  fuse_opt_free_args(&fuse_args);
  if (fuse_ == NULL) {
    // Avoid "Transport endpoint is not connected" in case fuse_new failed.
    fuse_unmount(mount_point, fuse_channel);
    for (int i = 0; i < fuse_opts.size(); i++) {
      free(fuse_opts[i]);
    }
    free(mount_point);
    // Stop FuseAdapter.
    fuse_adapter->Stop();
    delete fuse_adapter;
    return errno;
  }

  // Send to background.
  if (!options.foreground) {
    // Close write end of pipe as no error was encountered.
    close(fd[1]);
    // Daemonize. (Do everything (except for the fork()), the regular daemon()
    // would have done, too.)
    // 1. Change the file mode mask.
    umask(0);
    // 2. Create a new SID for the child process.
    if (setsid() < 0) {
        return 6;
    }

    // 3. Change the current working directory.  This prevents the current
    // directory from being locked; hence not being able to remove it.
    if ((chdir("/")) < 0) {
      return 7;
    }
    // 4. Redirect standard files to /dev/null.
    freopen( "/dev/null", "r", stdin);
    freopen( "/dev/null", "w", stdout);
    freopen( "/dev/null", "w", stderr);
  }

  // Run fuse.
  fuse_set_signal_handlers(fuse_get_session(fuse_));
  fuse_loop_mt(fuse_);
  // Cleanup
  fuse_teardown(fuse_, mount_point);
  for (int i = 0; i < fuse_opts.size(); i++) {
    free(fuse_opts[i]);
  }

  // Stop FuseAdapter.
  fuse_adapter->Stop();
  delete fuse_adapter;

  return 0;
}
