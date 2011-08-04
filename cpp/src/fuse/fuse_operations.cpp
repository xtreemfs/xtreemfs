/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "fuse/fuse_operations.h"

#include "fuse/fuse_adapter.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

xtreemfs::FuseAdapter* fuse_adapter = NULL;

int xtreemfs_fuse_getattr(const char *path, struct stat *statbuf) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG) << "getattr on path " << path << endl;
  }
  return fuse_adapter->getattr(path, statbuf);
}

int xtreemfs_fuse_readlink(const char *path, char *link, size_t size) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "xtreemfs_fuse_readlink on path " << path << endl;
  }
  return fuse_adapter->readlink(path, link, size);
}

int xtreemfs_fuse_mknod(const char *path, mode_t mode, dev_t dev) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_fuse_mknod on path "
         << path << endl;
  }
  return fuse_adapter->mknod(path, mode, dev);
}

int xtreemfs_fuse_mkdir(const char *path, mode_t mode) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_fuse_mkdir on path "
         << path << endl;
  }
  return fuse_adapter->mkdir(path, mode);
}

int xtreemfs_fuse_unlink(const char *path) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_fuse_unlink " << path
         << endl;
  }
  return fuse_adapter->unlink(path);
}

int xtreemfs_fuse_rmdir(const char *path) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_fuse_rmdir on path " << path
        << endl;
  }
  return fuse_adapter->rmdir(path);
}

int xtreemfs_fuse_symlink(const char *path, const char *link) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_fuse_symlink on path "
        << path << endl;
  }
  return fuse_adapter->symlink(path, link);
}

int xtreemfs_fuse_rename(const char *path, const char *newpath) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "xtreemfs_fuse_rename on path " << path << " to " << newpath <<
        endl;
  }
  return fuse_adapter->rename(path, newpath);
}

int xtreemfs_fuse_link(const char *path, const char *newpath) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "xtreemfs_fuse_link on path " << path << " " << newpath << endl;
  }
  return fuse_adapter->link(path, newpath);
}

int xtreemfs_fuse_chmod(const char *path, mode_t mode) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_chmod on path " << path
        << endl;
  }
  return fuse_adapter->chmod(path, mode);
}

int xtreemfs_fuse_lock(const char* path,
                       struct fuse_file_info *fi,
                       int cmd,
                       struct flock* flock_) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    string log_command;
    switch(cmd) {
      case F_GETLK:
        log_command = "check lock";
        break;
      case F_SETLK:
        log_command = "set lock";
        break;
      case F_SETLKW:
        log_command = "set lock and wait";
        break;
      default:
        log_command = "unknown lock command";
        break;
    }
    string log_type;
    switch(flock_->l_type) {
      case F_UNLCK:
        log_type = "unlock";
        break;
      case F_RDLCK:
        log_type = "read lock";
        break;
      case F_WRLCK:
        log_type = "write lock";
        break;
      default:
        log_type = "unknown lock type";
        break;
    }
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_lock on path " << path
        << " command: " << log_command << " type: " << log_type << " start: "
        << flock_->l_start << " length: "<< flock_->l_len << " pid: "
        << flock_->l_pid << endl;
  }
  return fuse_adapter->lock(path, fi, cmd, flock_);
}

int xtreemfs_fuse_chown(const char *path, uid_t uid, gid_t gid) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_chown on path " << path
        << endl;
  }
  return fuse_adapter->chown(path, uid, gid);
}

int xtreemfs_fuse_truncate(const char *path, off_t new_file_size) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "xtreemfs_fuse_truncate on path " << path
        << " size:" << new_file_size << endl;
  }
  return fuse_adapter->truncate(path, new_file_size);
}

int xtreemfs_fuse_utime(const char *path, struct utimbuf *ubuf) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_utime on path " << path
        << endl;
  }
  return fuse_adapter->utime(path, ubuf);
}

int xtreemfs_fuse_utimens(const char *path, const struct timespec tv[2]) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_fuse_utimens on path "
        << path << endl;
  }
  return fuse_adapter->utimens(path, tv);
}

int xtreemfs_fuse_open(const char *path, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_open on path " << path
         << endl;
  }
  return fuse_adapter->open(path, fi);
}

int xtreemfs_fuse_release(const char *path, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_release " << path
         << endl;
  }
  return fuse_adapter->release(path, fi);
}


int xtreemfs_fuse_read(
    const char *path, char *buf,
    size_t size, off_t offset, struct fuse_file_info *fi) {
  int count = fuse_adapter->read(path, buf, size, offset, fi);
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "xtreemfs_fuse_read " << path << " s:" << size <<  " o:"
        << offset << " r:" << count << endl;
  }
  return count;
}

int xtreemfs_fuse_write(const char *path, const char *buf, size_t size,
    off_t offset, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_write " << path
        << " size: " << size << endl;
  }
  return fuse_adapter->write(path, buf, size, offset, fi);
}

int xtreemfs_fuse_statfs(const char *path, struct statvfs *statv) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_statfs " << path
        << endl;
  }
  return fuse_adapter->statfs(path, statv);
}

/** Unlink fsync(), flush() requests are NOT initiated from the user.
 *
 * Instead, flush() is a Fuse internal mechanism to avoid the problem that
 * the return value of release() will be ignored.
 *
 * Therefore, a flush() will be called by Fuse with every close() executed by
 * the user. Only errors returned by this flush() operation can be returned
 * to the close() of the user.
 */
int xtreemfs_fuse_flush(const char *path, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_flush " << path
        << endl;
  }
  return fuse_adapter->flush(path, fi);
}

int xtreemfs_fuse_fsync(const char *path, int datasync,
    struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_fsync " << path
        << endl;
  }

  // We ignore the datasync parameter as all metadata operations are
  // synchronous and therefore never have to be flushed.
  return fuse_adapter->flush(path, fi);
}

int xtreemfs_fuse_setxattr(
    const char *path, const char *name,
    const char *value, size_t size, int flags) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "xtreemfs_fuse_setxattr " << " " << path << " " << name << endl;
  }
  return fuse_adapter->setxattr(path, name, value, size, flags);
}

int xtreemfs_fuse_getxattr(
    const char *path, const char *name, char *value, size_t size) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "xtreemfs_fuse_getxattr " << " " << path << " " << name << " "
        << size << endl;
  }
  return fuse_adapter->getxattr(path, name, value, size);
}

int xtreemfs_fuse_listxattr(const char *path, char *list, size_t size) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "xtreemfs_fuse_listxattr " << path << " " << size << endl;
  }
  return fuse_adapter->listxattr(path, list, size);
}

int xtreemfs_fuse_removexattr(const char *path, const char *name) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "xtreemfs_fuse_removexattr " << " " << path << " " << name << endl;
  }
  return fuse_adapter->removexattr(path, name);
}

int xtreemfs_fuse_opendir(const char *path, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_opendir " << path
        << endl;
  }
  return fuse_adapter->opendir(path, fi);
}

int xtreemfs_fuse_readdir(
    const char *path, void *buf,
    fuse_fill_dir_t filler, off_t offset, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_readdir " << path
        << endl;
  }
  return fuse_adapter->readdir(path, buf, filler, offset, fi);
}

int xtreemfs_fuse_releasedir(const char *path, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG) && path != NULL) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_releasedir " << path
        << endl;
  }
  return fuse_adapter->releasedir(path, fi);
}

int xtreemfs_fuse_fsyncdir(
    const char *path, int datasync, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_fsyncdir " << path
        << endl;
  }

  // Like fsync, but for directories - not required for XtreemFS.
  return 0;
}

void *xtreemfs_fuse_init(struct fuse_conn_info *conn) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_init " << endl;
  }

  // http://sourceforge.net/apps/mediawiki/fuse/index.php?title=Fuse_file_info
  // TODO(mberlin): Check for valid parameters.
  conn->async_read = 5;
  conn->max_readahead = 10 * 128 * 1024;
  conn->max_write = 128 * 1024;

#if FUSE_MAJOR_VERSION > 2 || (FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8)  // NOLINT
  conn->capable
    = FUSE_CAP_ASYNC_READ | FUSE_CAP_BIG_WRITES
      | FUSE_CAP_ATOMIC_O_TRUNC | FUSE_CAP_POSIX_LOCKS;
  conn->want
    = FUSE_CAP_ASYNC_READ | FUSE_CAP_BIG_WRITES
      | FUSE_CAP_ATOMIC_O_TRUNC | FUSE_CAP_POSIX_LOCKS;
#endif

  struct fuse_context* context = fuse_get_context();
  return context->private_data;
}

void xtreemfs_fuse_destroy(void *userdata) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_destroy " << endl;
  }
}

/**
 * This method will only be called by Fuse if "-o default_permissions" is not
 * send to Fuse (for instance before changing the working directory).
 *
 * If "-o default_permissions" is enabled, Fuse does determine on its own, based
 * on the result of the getattr, if the user is allowed to access the directory.
 */
int xtreemfs_fuse_access(const char *path, int mask) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_fuse_access " << path
        << endl;
  }
  return fuse_adapter->access(path, mask);
}

int xtreemfs_fuse_create(const char *path, mode_t mode,
    struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)  << "create on path " << path << endl;
  }
  return fuse_adapter->create(path, mode, fi);
}

int xtreemfs_fuse_ftruncate(
    const char *path, off_t new_file_size, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "xtreemfs_fuse_ftruncate on path " << path
        << " size:" << new_file_size << endl;
  }
  return fuse_adapter->ftruncate(path, new_file_size, fi);
}

int xtreemfs_fuse_fgetattr(
    const char *path, struct stat *statbuf, struct fuse_file_info *fi) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)  << "fgetattr on path " << path << endl;
  }
  return fuse_adapter->fgetattr(path, statbuf, fi);
}
