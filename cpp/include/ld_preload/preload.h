/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef PRELOAD_PRELOAD_H_
#define PRELOAD_PRELOAD_H_

#include <stdint.h>
#include <sys/types.h>

#include "ld_preload/environment.h"

bool overlay_initialized(bool toggle = false);
Environment* get_env();

bool is_xtreemfs_fd(int fd);
bool is_xtreemfs_path(const char *path);

int xtreemfs_open(const char* pathname, int flags, int mode);
int xtreemfs_close(int fd);
uint64_t xtreemfs_pread(int fd, void* buf, uint64_t nbyte, uint64_t offset);
uint64_t xtreemfs_read(int fd, void* buf, uint64_t nbyte);
uint64_t xtreemfs_write(int fd, const void* buf, uint64_t nbyte);
int xtreemfs_dup2(int oldfd, int newfd);
int xtreemfs_dup(int fd);
off_t xtreemfs_lseek(int fd, off_t offset, int mode);
int xtreemfs_stat(const char *path, struct stat *buf);
int xtreemfs_stat64(const char *pathname, struct stat64 *buf);
int xtreemfs_fstat(int fd, struct stat *buf);
int xtreemfs_fstat64(int fd, struct stat64 *buf);

int xtreemfs_setxattr(const char *pathname, const char *name, const void *value, size_t size, int flags);
int xtreemfs_fsetxattr(int fd, const char *name, const void *value, size_t size, int flags);

int xtreemfs_fsync(int fd);

#endif  // PRELOAD_PRELOAD_H_

