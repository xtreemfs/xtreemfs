/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_FUSE_FUSE_OPERATIONS_H_
#define CPP_INCLUDE_FUSE_FUSE_OPERATIONS_H_

#include <sys/types.h>

#define FUSE_USE_VERSION 26
#include <fuse.h>

namespace xtreemfs {
class FuseAdapter;
}

/** Contains functions which are passed into fuse_operations struct.
 * @file
 *
 * The functions in this file are merely placeholders which call the actual
 * functions of the FuseAdapter instance pointed to by fuse_adapter.
 */

/** Points to the FuseAdapter instance created by mount.xtreemfs.cpp. */
extern xtreemfs::FuseAdapter* fuse_adapter;

extern "C" int xtreemfs_fuse_getattr(const char *path, struct stat *statbuf);
extern "C" int xtreemfs_fuse_readlink(const char *path, char *link,
                                      size_t size);
extern "C" int xtreemfs_fuse_mknod(const char *path, mode_t mode, dev_t dev);
extern "C" int xtreemfs_fuse_mkdir(const char *path, mode_t mode);
extern "C" int xtreemfs_fuse_unlink(const char *path);
extern "C" int xtreemfs_fuse_rmdir(const char *path);
extern "C" int xtreemfs_fuse_symlink(const char *path, const char *link);
extern "C" int xtreemfs_fuse_rename(const char *path, const char *newpath);
extern "C" int xtreemfs_fuse_link(const char *path, const char *newpath);
extern "C" int xtreemfs_fuse_chmod(const char *path, mode_t mode);
extern "C" int xtreemfs_fuse_chown(const char *path, uid_t uid, gid_t gid);
extern "C" int xtreemfs_fuse_truncate(const char *path, off_t new_file_size);
extern "C" int xtreemfs_fuse_utime(const char *path, struct utimbuf *ubuf);
extern "C" int xtreemfs_fuse_lock(const char *, struct fuse_file_info *,
                                  int cmd, struct flock *);
extern "C" int xtreemfs_fuse_utimens(const char *path,
                                     const struct timespec tv[2]);
extern "C" int xtreemfs_fuse_open(const char *path, struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_read(const char *path, char *buf, size_t size,
                                  off_t offset, struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_write(
    const char *path,
    const char *buf,
    size_t size,
    off_t offset,
    struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_statfs(const char *path, struct statvfs *statv);
extern "C" int xtreemfs_fuse_flush(const char *path, struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_release(const char *path,
                                     struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_fsync(const char *path, int datasync,
                                   struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_setxattr(
    const char *path,
    const char *name,
    const char *value,
    size_t size,
    int flags);
extern "C" int xtreemfs_fuse_getxattr(const char *path, const char *name,
                                      char *value, size_t size);
extern "C" int xtreemfs_fuse_listxattr(const char *path, char *list,
                                       size_t size);
extern "C" int xtreemfs_fuse_removexattr(const char *path, const char *name);
extern "C" int xtreemfs_fuse_opendir(const char *path,
                                     struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_readdir(
    const char *path,
    void *buf,
    fuse_fill_dir_t filler,
    off_t offset,
    struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_releasedir(const char *path,
                                        struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_fsyncdir(const char *path, int datasync,
                                      struct fuse_file_info *fi);
extern "C" void *xtreemfs_fuse_init(struct fuse_conn_info *conn);
extern "C" void xtreemfs_fuse_destroy(void *userdata);
extern "C" int xtreemfs_fuse_access(const char *path, int mask);
extern "C" int xtreemfs_fuse_create(const char *path, mode_t mode,
                                    struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_ftruncate(const char *path, off_t new_file_size,
                                       struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_fgetattr(const char *path, struct stat *statbuf,
                                      struct fuse_file_info *fi);
extern "C" int xtreemfs_fuse_lock(const char* path, struct fuse_file_info *fi,
                                  int cmd, struct flock* flock_);

#endif  // CPP_INCLUDE_FUSE_FUSE_OPERATIONS_H_
