/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <stdio.h>
#include <stdarg.h>
#include <stdint.h>
#include <fcntl.h>

#include "ld_preload/passthrough.h"
#include "ld_preload/preload.h"

/* Interceptor functions */

extern "C" {

int open(__const char *path, int flags, ...) {
  initialize_passthrough_if_necessary();
  xprintf(" open(%s)\n", path);

  // if O_CREAT is set, mode MUST be specified and is ignored otherwise (at least on linux, see man 2 open)
  // we only need to acquire the mode, when it must be specified, and ignore it otherwise
  mode_t mode;
  if (flags & O_CREAT) {
    va_list ap;
    va_start(ap, flags);
    mode = va_arg(ap, mode_t);
    va_end(ap);
  }

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    return xtreemfs_open(path, flags, mode);
  } else {
    xprintf(" open calling libc_open(%s)\n", path);
    int ret = ((funcptr_open)libc_open)(path, flags, mode);
    xprintf(" open libc_open(%s) returned %d\n", path, ret);
    return ret;
  }
}

int open64(__const char *path, int flags, ...) {
  initialize_passthrough_if_necessary();
  xprintf(" open64(%s)\n", path);

  mode_t mode;
  if (flags & O_CREAT) {
    va_list ap;
    va_start(ap, flags);
    mode = va_arg(ap, mode_t);
    va_end(ap);
  }

  return open(path, flags | O_LARGEFILE, mode);
}

#undef creat
int creat(__const char *name, mode_t mode) {
  return open(name, O_CREAT | O_WRONLY | O_TRUNC, mode);
}

int close(int fd) {
  initialize_passthrough_if_necessary();
  xprintf(" close(%d)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_close(fd);
  } else {
    xprintf(" close passthrough(%d)\n", fd);
    return ((funcptr_close)libc_close)(fd);
  }
}

// failed attempt to intercept libc-internal close calls

int _close(int fd) {
  xprintf(" _close(%d)\n", fd);
  return close(fd);
}

int __close(int fd) {
  xprintf(" __close(%d)\n", fd);
  return close(fd);
}

// TODO: do we need this?
/*
int fclose(FILE *fp) {
  initialize_passthrough_if_necessary();
  xprintf(" fclose(%d)\n", fd);

  if (is_xtreemfs_fd(fd)) {
    return xtreemfs_fclose(fd);
  } else {
    xprintf(" fclose passthrough(%d)\n", fd);
    return ((funcptr_close)libc_fclose)(fd);
  }
}
*/

ssize_t pread(int fd, void* buf, size_t nbyte, off_t offset) {
  initialize_passthrough_if_necessary();
  xprintf(" pread(%d)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_pread(fd, buf, nbyte, offset);
  } else {
    return ((funcptr_pread)libc_pread)(fd, buf, nbyte, offset);
  }
}

ssize_t pread64(int fd, void* buf, size_t nbyte, __off64_t offset) {
  initialize_passthrough_if_necessary();
  xprintf(" pread64(%d)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_pread(fd, buf, nbyte, offset);
  } else {
    return ((funcptr_pread)libc_pread)(fd, buf, nbyte, offset);
  }
}

ssize_t read(int fd, void* buf, size_t nbyte) {
  initialize_passthrough_if_necessary();
  xprintf(" read(%d)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_read(fd, buf, nbyte);
  } else {
    return ((funcptr_read)libc_read)(fd, buf, nbyte);
  }
}

ssize_t write(int fd, const void* buf, size_t nbyte) {
  initialize_passthrough_if_necessary();
  xprintf(" write(%d)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    xprintf(" write(%d) xtreemfs\n", fd);
    return xtreemfs_write(fd, buf, nbyte);
  } else {
    xprintf(" write(%d) passthrough\n", fd);
    return ((funcptr_write)libc_write)(fd, buf, nbyte);
  }
}

int dup(int oldfd) {
  initialize_passthrough_if_necessary();
  xprintf(" dup(%d)\n", oldfd);

  if (overlay_initialized() && is_xtreemfs_fd(oldfd)) {
    //xprintf("calling xtreemfs dup(%d)\n", oldfd);
    //return xtreemfs_dup(oldfd);
    xprintf(" NOT IMPLEMENTED: dup for xtreemfs fd(%d)\n", oldfd);
    return -1;
  } else {
    xprintf(" calling pasthrought dup(%d)\n", oldfd);
    return ((funcptr_dup)libc_dup)(oldfd);
  }
}

int dup2(int oldfd, int newfd) {
  initialize_passthrough_if_necessary();
  xprintf(" dup2(%d, %d)\n", oldfd, newfd);

  if (overlay_initialized() && (is_xtreemfs_fd(newfd) || is_xtreemfs_fd(oldfd))) {
    xprintf(" NOT IMPLEMENTED: dup2 for xtreemfs fd(%d, %d)\n", oldfd, newfd);
    return -1;
  } else {
    return ((funcptr_dup2)libc_dup2)(oldfd, newfd);
  }

//  if (overlay_initialized() && is_xtreemfs_fd(newfd)) {
//    xprintf(" dest is xtreemfs fd\n");
//    xtreemfs_close(newfd);
//  } else {
//    xprintf(" dest is system fd\n");
//    ((funcptr_close)libc_close)(newfd);
//  }
//
//  if (overlay_initialized() && is_xtreemfs_fd(oldfd)) {
//    return xtreemfs_dup2(oldfd, newfd);
//  } else {
//    xprintf(" dup2 passthrough\n");
//    return ((funcptr_dup2)libc_dup2)(oldfd, newfd);
//  }
}

off_t lseek(int fd, off_t offset, int mode) {
  initialize_passthrough_if_necessary();
  xprintf(" lseek(%d, %ld, %d)\n", fd, offset, mode);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_lseek(fd, offset, mode);
  } else {
    return ((funcptr_lseek)libc_lseek)(fd, offset, mode);
  }
}

int stat(const char *path, struct stat *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" stat(%s, ...)\n", path);

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    return xtreemfs_stat(path, buf);
  } else {
    return ((funcptr_stat)libc_stat)(path, buf);
  }
}

int fstat(int fd, struct stat *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" fstat(%d, ...)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_fstat(fd, buf);
  } else {
    return ((funcptr_fstat)libc_fstat)(fd, buf);
  }
}

int __xstat(int ver, const char *path, struct stat *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" __xstat(%d, %s, ...)\n", ver, path);

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    return xtreemfs_stat(path, buf);
  } else {
    return ((funcptr___xstat)libc___xstat)(ver, path, buf);
  }
}

int __xstat64(int ver, const char *path, struct stat64 *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" __xstat64(%d, %s, ...)\n", ver, path);
  xprintf(" __xstat64(%d, %s, ...), errno(%d)\n", ver, path, errno);
  return -1;

//  int i = strstr(path, "/gfs1/work/bzaztsch/job-output") == path;
//  xprintf(" __xstat64(%s), returning -1 WITHOUT actually calling GetAttr, errno(%d), path(%p), strstr(%p)\n", path, errno, path, i);
//  return -1;
//  //if (overlay_initialized() && (strstr(path, "/gfs1/work/bzaztsch/job-output") == path)) {
//  if ((strstr(path, "/gfs1/work/bzaztsch/job-output") == path)) {
//  xprintf(" __xstat64(%s), returning -1 WITHOUT actually calling GetAttr, errno(%d)\n", path, errno);
//  return -1;
  if (overlay_initialized() && is_xtreemfs_path(path)) {
    return xtreemfs_stat64(path, buf);
  } else {
    return ((funcptr___xstat64)libc___xstat64)(ver, path, buf);
  }
}

int __fxstat(int ver, int fd, struct stat *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" __fxstat(%d, %d, ...)\n", ver, fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_fstat(fd, buf);
  } else {
    return ((funcptr___fxstat)libc___fxstat)(ver, fd, buf);
  }
}

int __fxstat64(int ver, int fd, struct stat64 *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" __fxstat64(%d, %d, ...)\n", ver, fd);
  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    return xtreemfs_fstat64(fd, buf);
  } else {
    return ((funcptr___fxstat64)libc___fxstat64)(ver, fd, buf);
  }
}

extern int __lxstat(int ver, const char *path, struct stat *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" __lxstat(%d, %s, ...)\n", ver, path);

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    xprintf(" NOT IMPLEMENTED: lstat for xtreemfs (%s)\n", path);
    return -1;
  } else {
    return ((funcptr___lxstat)libc___lxstat)(ver, path, buf);
  }
}

extern int __lxstat64(int ver, const char *path, struct stat64 *buf) {
  initialize_passthrough_if_necessary();
  xprintf(" __lxstat64(%d, %s, ...)\n", ver, path);

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    xprintf(" NOT IMPLEMENTED: lstat64 for xtreemfs (%s)\n", path);
    return -1;
  } else {
    return ((funcptr___lxstat64)libc___lxstat64)(ver, path, buf);
  }
}

/*
extern int __fxstatat(int __ver, int __fildes, const char *__filename, struct stat *__stat_buf, int __flag) {
  initialize_passthrough_if_necessary();
  xprintf(" __fxstatat\n");
  return -1;
}

extern int __fxstatat64(int __ver, int __fildes, const char *__filename, struct stat64 *__stat_buf, int __flag) {
  initialize_passthrough_if_necessary();
  xprintf(" __fxstatat64\n");
  return -1;
}
*/

FILE *fopen(const char *path, const char *mode) {
  initialize_passthrough_if_necessary();

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    xprintf(" WARNING: fopen(%s, %s) called for xtreemfs\n", path, mode);
  }

  return ((funcptr_fopen)libc_fopen)(path, mode);
}

int truncate(const char *path, off_t length) {
  initialize_passthrough_if_necessary();
  xprintf(" truncate(%s, %ld)\n", path, length);

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    xprintf("  NOT IMPLEMENTED: truncate for xtreemfs (%s)\n", path);
    return -1;
  } else {
    return ((funcptr_truncate)libc_truncate)(path, length);
  }
}

int ftruncate(int fd, off_t length) {
  initialize_passthrough_if_necessary();
  xprintf(" ftruncate(%d, %ld)\n", fd, length);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    xprintf(" NOT IMPLEMENTED: ftruncate for xtreemfs fd(%d)", fd);
    return -1;
  } else {
    return ((funcptr_ftruncate)libc_ftruncate)(fd, length);
  }
}

int setxattr(const char *path, const char *name, const void *value, size_t size, int flags) {
  initialize_passthrough_if_necessary();
  xprintf(" setxattr(%s, %s, ...)\n", path, name);

  if (overlay_initialized() && is_xtreemfs_path(path)) {
    xprintf(" xtreemfs_setxattr(%s, %s, ...)", path, name);
    return xtreemfs_setxattr(path, name, value, size, flags);
  } else {
    return ((funcptr_setxattr)libattr_setxattr)(path, name, value, size, flags);
  }
}

//int lsetxattr (const char *path, const char *name,
//                const void *value, size_t size, int flags);

int fsetxattr(int fd, const char *name, const void *value, size_t size, int flags) {
  initialize_passthrough_if_necessary();
  xprintf(" fsetxattr(%d, %s, ...)\n", fd, name);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    xprintf(" NOT IMPLEMENTED: fsetxattr for xtreemfs fd(%d)", fd);
    return -1;
  } else {
    return ((funcptr_fsetxattr)libattr_fsetxattr)(fd, name, value, size, flags);
  }
}


int fsync(int fd) {
  initialize_passthrough_if_necessary();
  xprintf(" fsync(%d)\n", fd);

  if (overlay_initialized() && is_xtreemfs_fd(fd)) {
    xprintf(" fsync(%d) xtreemfs\n", fd);
    return xtreemfs_fsync(fd);
  } else {
    xprintf(" fsync(%d) passthrough\n", fd);
    return ((funcptr_fsync)libc_fsync)(fd);
  }
}

}  // extern "C"
