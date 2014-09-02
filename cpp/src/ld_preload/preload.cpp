/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/preload.h"

#include <pthread.h>
#include <cstdlib>
#include <cstring>
#include <exception>
#include <stdio.h>
#include <fcntl.h>
#include <list>
#include <string>
#include <boost/atomic.hpp>
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/system_user_mapping_unix.h"
#include "libxtreemfs/volume_implementation.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

#include "ld_preload/environment.h"
#include "ld_preload/misc.h"
#include "ld_preload/passthrough.h"

static Environment* env = NULL;

/**
 * The environment should be initialised exactly once during shared library initialisation.
 * Prior to that, all operations work in pass-through mode.
 * To overcome undefined global initialisation order, we use a local static variable.
 * This variable is accesd by a single writer (lib (de)initialisation), and multiple readers (exported operations).
 * Writer: toggle == true (toggles is_initialised)
 * Reader: toggle == false (returns the current value of is_initialised)
 *
 * NOTE: pthread_once initialisation for the overlay leads to a deadlock due to a re-entry situation:
 * - environment ctor is called the first time via pthread_once
 * - which leads a write somewhere (probably to a socket during client initialisation)
 * - which then enters the pthread_once initialisation mechanism again and blocks deadlocks
 */
bool overlay_initialized(bool toggle /* defaults to: false */) {
  static boost::atomic<bool> is_initilized(false);

  // writer
  if(toggle) {
    return is_initilized.exchange(!is_initilized.load());
  }

  // reader
  return is_initilized.load();
}

void initialize_env() {
  env = new Environment();
  xprintf("env initialised\n");
}

void deinitialize_env() {
  xprintf("deinitialize_env()\n");
  delete env;
  xprintf("env deinitialised\n");
}

Environment* get_env() {
  return env;
}

bool is_xtreemfs_fd(int fd) {
  return env->open_file_table_.Has(fd);
}

bool is_xtreemfs_path(const char *path) {
  Path path_obj(path);
  return path_obj.IsXtreemFS();
}

/* Library constructor */
void
__attribute__((constructor))
init_preload(void) {
  initialize_passthrough_if_necessary();
  xprintf("library initialisation 1\n");
  xprintf("library initialisation 2\n");
  initialize_env();
  xprintf("library initialisation 3\n");
  bool ret = overlay_initialized(true); // activate overlay filesystem
  xprintf("library initialised! %d\n", ret);
}

/* Library destructor */
void
__attribute__((destructor))
deinit_preload(void) {
  xprintf("library deinitialisation started.\n");
  bool ret = overlay_initialized(true); // deactivate overlay filesystem
  deinitialize_env();
  xprintf("library deinitialised! %d\n", ret);
}

/* XtreemFS equivalents to POSIX file system calls */

int xtreemfs_open(const char* pathname, int flags, int mode) {
  xprintf(" open on xtreemfs(%s)\n", pathname);
  Path path(pathname);
  path.Parse();

  xtreemfs::Volume* volume = env->GetVolume();
  const xtreemfs::pbrpc::SYSTEM_V_FCNTL xtreem_flags = ConvertFlagsUnixToXtreemFS(flags);

  xtreemfs::FileHandle* handle = volume->OpenFile(
      env->user_creds_,
      path.GetXtreemFSPath(),
      xtreem_flags,
      mode);

  int fd = env->open_file_table_.Register(handle);
  xprintf(" open on xtreemfs(%s) -> %d\n", pathname, fd);
  return fd;
}

int xtreemfs_close(int fd) {
  xprintf(" close xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);
  env->open_file_table_.Unregister(fd);
  //handle.fh_->Flush(); // implicit by close
  handle.fh_->Close(); // TODO: error code
  return 0;
}

uint64_t xtreemfs_pread(int fd, void* buf, uint64_t nbyte, uint64_t offset) {
  xprintf(" read xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);
  return handle.fh_->Read((char*)buf, nbyte, offset);
}

uint64_t xtreemfs_read(int fd, void* buf, uint64_t nbyte) {
  xprintf(" read xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);
  int read = handle.fh_->Read((char*)buf, nbyte, handle.offset_);
  env->open_file_table_.SetOffset(fd, handle.offset_ + read);
  return read;
}

uint64_t xtreemfs_write(int fd, const void* buf, uint64_t nbyte) {
  xprintf(" write xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);
  int written = handle.fh_->Write((char*)buf, nbyte, handle.offset_);
  env->open_file_table_.SetOffset(fd, handle.offset_ + written);
  return written;
}

int xtreemfs_dup2(int oldfd, int newfd) {
  xprintf(" dup2 xtreemfs(%d, %d)\n", oldfd, newfd);
  OpenFile handle = env->open_file_table_.Get(oldfd);
  if (handle.fh_ == NULL) {
    xprintf(" dup2 error(%d, %d)\n", oldfd, newfd);
    return -1;
  }
  xprintf(" dup2 fffxtreemfs(%d, %d)\n", oldfd, newfd);
  xtreemfs::FileHandle* new_handle; // = handle.fh_->Duplicate(); // TODO: implement Duplicate

  xprintf(" dup2 yxtreemfs(%d, %d)\n", oldfd, newfd);
  env->open_file_table_.Set(newfd, new_handle);
  return newfd;
}

int xtreemfs_dup(int fd) {
  xprintf(" dup xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);
  xtreemfs::FileHandle* new_handle; // = handle.fh_->Duplicate(); // TODO: implement Duplicate
  return env->open_file_table_.Register(new_handle);
}

off_t xtreemfs_lseek(int fd, off_t offset, int mode) {
  xprintf(" lseek xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);

  switch (mode) {
    case SEEK_SET:
      env->open_file_table_.SetOffset(fd, offset);
      return offset;
    case SEEK_CUR:
      env->open_file_table_.SetOffset(fd, handle.offset_ + offset);
      return handle.offset_ + offset;
    case SEEK_END:
      env->open_file_table_.SetOffset(fd, offset);  // TODO
      return offset;
  }
  return EINVAL;
}

/* T should be "struct stat" or "struct stat64" */
template<typename T>
static int xtreemfs_stat_impl(const char *pathname, T *buf) {
  Path path(pathname);
  path.Parse();
  xtreemfs::pbrpc::Stat stat;

  try {
    env->GetVolume()->GetAttr(env->user_creds_, path.GetXtreemFSPath(), &stat);
  } catch(const xtreemfs::PosixErrorException& e) {
    errno = ConvertXtreemFSErrnoToUnix(e.posix_errno());
    xprintf(" xtreemfs_stat_impl(%s), returning -1, errno(%d)\n", pathname, errno);
    return -1;
  } catch(const xtreemfs::XtreemFSException& e) {
    xprintf(" xtreemfs_stat_impl(%s), returning -1, errno(%d)\n", pathname, errno);
    errno = EIO;
    return -1;
  } catch(const std::exception& e) {
    xprintf("A non-XtreemFS exception occurred: %s", std::string(e.what()).c_str());
    xprintf(" xtreemfs_stat_impl(%s), returning -1, errno(%d)\n", pathname, errno);
    errno = EIO;
    return -1;
  }

  ConvertXtreemFSStatToUnix(stat, buf, env->GetSystemUserMapping());
  xprintf(" xtreemfs_fstat_impl(%s), returning 0\n", pathname);
  return 0;
}

/* T should be "struct stat" or "struct stat64" */
template<typename T>
static int xtreemfs_fstat_impl(int fd, T *buf) {
  OpenFile handle = env->open_file_table_.Get(fd);
  xtreemfs::pbrpc::Stat stat;

  try {
    handle.fh_->GetAttr(env->user_creds_, &stat);
  } catch(const xtreemfs::PosixErrorException& e) {
    errno = ConvertXtreemFSErrnoToUnix(e.posix_errno());
    return -1;
  } catch(const xtreemfs::XtreemFSException& e) {
    errno = EIO;
    return -1;
  } catch(const std::exception& e) {
    xprintf("A non-XtreemFS exception occurred: %s", std::string(e.what()).c_str());
    errno = EIO;
    return -1;
  }

  ConvertXtreemFSStatToUnix(stat, buf, env->GetSystemUserMapping());
  return 0;
}


int xtreemfs_stat(const char *pathname, struct stat *buf) {
  xprintf(" xtreemfs_stat(%s)\n", pathname);
  return xtreemfs_stat_impl(pathname, buf);
}

int xtreemfs_stat64(const char *pathname, struct stat64 *buf) {
  xprintf(" xtreemfs_stat64(%s)\n", pathname);
  return xtreemfs_stat_impl(pathname, buf);
}

int xtreemfs_fstat(int fd, struct stat *buf) {
  xprintf(" xtreemfs_fstat(%d)\n", fd);
  return xtreemfs_fstat_impl(fd, buf);
}

int xtreemfs_fstat64(int fd, struct stat64 *buf) {
  xprintf(" xtreemfs_fstat64(%d)\n", fd);
  return xtreemfs_fstat_impl(fd, buf);
}


int xtreemfs_setxattr(const char *pathname, const char *name, const void *value, size_t size, int flags) {
  Path path(pathname);
  path.Parse();

  try {
    env->GetVolume()->SetXAttr(env->user_creds_, path.GetXtreemFSPath(), std::string(name),
        std::string(static_cast<const char*>(value), size), static_cast<xtreemfs::pbrpc::XATTR_FLAGS>(flags));
  } catch(const xtreemfs::PosixErrorException& e) {
    errno = ConvertXtreemFSErrnoToUnix(e.posix_errno());
    return -1;
  } catch(const xtreemfs::XtreemFSException& e) {
    errno = EIO;
    return -1;
  } catch(const std::exception& e) {
    xprintf("A non-XtreemFS exception occurred: %s", std::string(e.what()).c_str());
    errno = EIO;
    return -1;
  }
  return 0;
}

int xtreemfs_fsetxattr(int fd, const char *name, const void *value, size_t size, int flags) {
//  OpenFile handle = env->open_file_table_.Get(fd);
//  env->GetVolume()->SetXAttr(env->user_creds_, path.GetXtreemFSPath(), name, value, flags);
//
//  handle.fh_->SetX
  return 0;
}

int xtreemfs_fsync(int fd) {
  OpenFile handle = env->open_file_table_.Get(fd);
  handle.fh_->Flush();
  return 0;
}
