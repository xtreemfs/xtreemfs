/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/misc.h"

#include <pthread.h>
#include <cstdlib>
#include <cstring>
#include <stdio.h>
#include <fcntl.h>
#include <list>
#include <string>
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/system_user_mapping_unix.h"
#include "libxtreemfs/volume_implementation.h"

xtreemfs::pbrpc::SYSTEM_V_FCNTL ConvertFlagsUnixToXtreemFS(int flags) {
  int result = 0;

  #define CHECK(result, flags, unix, proto) { \
  if ((flags & unix) != 0) result |= proto; \
  }
  CHECK(result, flags, O_RDONLY   , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDONLY);
  CHECK(result, flags, O_WRONLY   , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_WRONLY);
  CHECK(result, flags, O_RDWR     , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR);
  CHECK(result, flags, O_APPEND   , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_APPEND);
  CHECK(result, flags, O_CREAT    , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT);
  CHECK(result, flags, O_TRUNC    , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC);
  CHECK(result, flags, O_EXCL     , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_EXCL);
  CHECK(result, flags, O_SYNC     , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_SYNC);
  #ifdef __linux
  CHECK(result, flags, O_DSYNC    , xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_SYNC);
  #endif

  xprintf("flags: %o\nresult: %o\n", flags, result);
  return xtreemfs::pbrpc::SYSTEM_V_FCNTL(result);
}

int TranslateMode(const char* mode) {
  if (strcmp(mode, "r") == 0) {
    return O_RDONLY;
  } else if (strcmp(mode, "r+") == 0) {
    return O_RDWR;
  } else if (strcmp(mode, "w") == 0) {
    return O_WRONLY | O_CREAT | O_TRUNC;
  } else if (strcmp(mode, "w+") == 0) {
    return O_RDWR | O_CREAT | O_TRUNC;
  } else if (strcmp(mode, "a") == 0) {
    return O_APPEND | O_WRONLY | O_CREAT;
  } else if (strcmp(mode, "a+") == 0) {
    return O_APPEND | O_RDWR | O_CREAT;
  }
  return 0;
}

int ConvertXtreemFSErrnoToUnix(xtreemfs::pbrpc::POSIXErrno xtreemfs_errno) {
  switch (xtreemfs_errno) {
    case xtreemfs::pbrpc::POSIX_ERROR_EPERM:
      return EPERM;
    case xtreemfs::pbrpc::POSIX_ERROR_ENOENT:
      return ENOENT;
    case xtreemfs::pbrpc::POSIX_ERROR_EINTR:
      return EINTR;
    case xtreemfs::pbrpc::POSIX_ERROR_EIO:
      return EIO;
    case xtreemfs::pbrpc::POSIX_ERROR_EAGAIN:
      return EAGAIN;
    case xtreemfs::pbrpc::POSIX_ERROR_EACCES:
      return EACCES;
    case xtreemfs::pbrpc::POSIX_ERROR_EEXIST:
      return EEXIST;
    case xtreemfs::pbrpc::POSIX_ERROR_EXDEV:
      return EXDEV;
    case xtreemfs::pbrpc::POSIX_ERROR_ENODEV:
      return ENODEV;
    case xtreemfs::pbrpc::POSIX_ERROR_ENOTDIR:
      return ENOTDIR;
    case xtreemfs::pbrpc::POSIX_ERROR_EISDIR:
      return EISDIR;
    case xtreemfs::pbrpc::POSIX_ERROR_EINVAL:
      return EINVAL;
    case xtreemfs::pbrpc::POSIX_ERROR_ENOTEMPTY:
      return ENOTEMPTY;
    case xtreemfs::pbrpc::POSIX_ERROR_ENODATA:
      return ENODATA;

    default:
      return xtreemfs_errno;
  }
}
