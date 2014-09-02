/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef PRELOAD_PASSTHROUGH_H_
#define PRELOAD_PASSTHROUGH_H_

#include <stdio.h>
#include <unistd.h>

typedef int (*funcptr_open)(const char*, int, int);
typedef int (*funcptr_close)(int);

typedef ssize_t (*funcptr_read)(int, void*, size_t);
typedef ssize_t (*funcptr_write)(int, const void*, size_t);
typedef ssize_t (*funcptr_pread)(int, void*, size_t, off_t);
typedef ssize_t (*funcptr_pwrite)(int, const void*, size_t, off_t);

typedef int (*funcptr_dup)(int);
typedef int (*funcptr_dup2)(int, int);
typedef off_t (*funcptr_lseek)(int, off_t, int);

typedef int (*funcptr_stat)(const char*, struct stat*);
typedef int (*funcptr_fstat)(int, struct stat*);
typedef int (*funcptr___xstat)(int, const char*, struct stat*);
typedef int (*funcptr___xstat64)(int, const char*, struct stat64*);
typedef int (*funcptr___fxstat)(int, int, struct stat*);
typedef int (*funcptr___fxstat64)(int, int, struct stat64*);
typedef int (*funcptr___lxstat)(int, const char*, struct stat*);
typedef int (*funcptr___lxstat64)(int, const char*, struct stat64*);

typedef FILE* (*funcptr_fopen)(const char*, const char*);
typedef int (*funcptr_truncate)(const char*, off_t);
typedef int (*funcptr_ftruncate)(int, off_t);

typedef int (*funcptr_setxattr)(const char*, const char*, const void*, size_t, int);
typedef int (*funcptr_fsetxattr)(int, const char*, const void*, size_t, int);

typedef int (*funcptr_fsync)(int);

extern void* libc_open;
extern void* libc_close;
extern void* libc___close;
extern void* libc_pread;
extern void* libc_read;
extern void* libc_write;
extern void* libc_dup;
extern void* libc_dup2;
extern void* libc_lseek;

extern void* libc_stat;
extern void* libc_fstat;
extern void* libc___xstat;
extern void* libc___xstat64;
extern void* libc___fxstat;
extern void* libc___fxstat64;
extern void* libc___lxstat;
extern void* libc___lxstat64;

extern void* libc_fopen;
extern void* libc_truncate;
extern void* libc_ftruncate;

extern void* libattr_setxattr;
extern void* libattr_fsetxattr;

extern void* libc_fsync;

void initialize_passthrough_if_necessary();

#ifdef XTREEMFS_PRELOAD_QUIET
  #define xprintf(...)
#else
  #define xprintf(...) fprintf(xtreemfs_stdout() ? xtreemfs_stdout() : stdout, __VA_ARGS__)
#endif

FILE* xtreemfs_stdout();

#endif  // PRELOAD_PASSTHROUGH_H_
