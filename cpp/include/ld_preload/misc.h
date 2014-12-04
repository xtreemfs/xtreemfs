/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef PRELOAD_MISC_H_
#define PRELOAD_MISC_H_

#include <string>
#include <stdint.h>
#include <sys/types.h>

#include "libxtreemfs/pbrpc_url.h"
#include "xtreemfs/GlobalTypes.pb.h"

#include "ld_preload/environment.h"
#include "ld_preload/passthrough.h"

xtreemfs::pbrpc::SYSTEM_V_FCNTL ConvertFlagsUnixToXtreemFS(int flags);
int TranslateMode(const char* mode);
int ConvertXtreemFSErrnoToUnix(xtreemfs::pbrpc::POSIXErrno xtreemfs_errno);

template<class T>
void ConvertXtreemFSStatToUnix(const xtreemfs::pbrpc::Stat& xtreemfs_stat, T* unix_stat, xtreemfs::SystemUserMappingUnix& system_user_mapping) {
  unix_stat->st_dev = xtreemfs_stat.dev();
  unix_stat->st_blksize = 8 * 128 * 1024;
  unix_stat->st_ino = xtreemfs_stat.ino();  // = fileId
  unix_stat->st_mode = xtreemfs_stat.mode();
  unix_stat->st_nlink = xtreemfs_stat.nlink();

  // Map user- and groupnames.
  unix_stat->st_uid = system_user_mapping.UsernameToUID(xtreemfs_stat.user_id());
  unix_stat->st_gid = system_user_mapping.GroupnameToGID(xtreemfs_stat.group_id());

  unix_stat->st_size = xtreemfs_stat.size();
#ifdef __linux
  unix_stat->st_atim.tv_sec  = xtreemfs_stat.atime_ns() / 1000000000;
  unix_stat->st_atim.tv_nsec = xtreemfs_stat.atime_ns() % 1000000000;
  unix_stat->st_mtim.tv_sec  = xtreemfs_stat.mtime_ns() / 1000000000;
  unix_stat->st_mtim.tv_nsec = xtreemfs_stat.mtime_ns() % 1000000000;
  unix_stat->st_ctim.tv_sec  = xtreemfs_stat.ctime_ns() / 1000000000;
  unix_stat->st_ctim.tv_nsec = xtreemfs_stat.ctime_ns() % 1000000000;
#elif __APPLE__
  unix_stat->st_atimespec.tv_sec  = xtreemfs_stat.atime_ns() / 1000000000;
  unix_stat->st_atimespec.tv_nsec = xtreemfs_stat.atime_ns() % 1000000000;
  unix_stat->st_mtimespec.tv_sec  = xtreemfs_stat.mtime_ns() / 1000000000;
  unix_stat->st_mtimespec.tv_nsec = xtreemfs_stat.mtime_ns() % 1000000000;
  unix_stat->st_ctimespec.tv_sec  = xtreemfs_stat.ctime_ns() / 1000000000;
  unix_stat->st_ctimespec.tv_nsec = xtreemfs_stat.ctime_ns() % 1000000000;
#endif

  unix_stat->st_rdev = 0;
  unix_stat->st_blocks = xtreemfs_stat.size() / 512;
}

#endif  // PRELOAD_MISC_H_
