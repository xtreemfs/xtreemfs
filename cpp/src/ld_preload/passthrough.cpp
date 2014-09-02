/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/passthrough.h"

#include <dlfcn.h>
#include <pthread.h>
#include <stdio.h>

void* libc;
void* libc_open;
void* libc_close;
void* libc___close;
void* libc_read;
void* libc_write;
void* libc_pread;
void* libc_pwrite;
void* libc_dup;
void* libc_dup2;
void* libc_lseek;
void* libc_stat;
void* libc_fstat;
void* libc___xstat;
void* libc___xstat64;
void* libc___fxstat;
void* libc___fxstat64;
void* libc___lxstat;
void* libc___lxstat64;

void* libc_fopen;
void* libc_truncate;
void* libc_ftruncate;

void* libattr;
void* libattr_setxattr;
void* libattr_fsetxattr;

void* libc_fsync;

// Our "copy" of stdout, because the application might close stdout
// or reuse the first file descriptors for other purposes.
static FILE* fdout = 0;

FILE* xtreemfs_stdout() {
  return fdout;
}

static void initialize_passthrough() {
  xprintf("initialize_passthrough(): Setting up pass-through\n");
  libc = dlopen("libc.so.6", RTLD_LAZY); // TODO: link with correct libc, version vs. 32 bit vs. 64 bit
  libc_open = dlsym(libc, "open");
  libc_close = dlsym(libc, "close");
  libc___close = dlsym(libc, "__close");
  libc_read = dlsym(libc, "read");
  libc_write = dlsym(libc, "write");
  libc_pread = dlsym(libc, "pread");
  libc_pwrite = dlsym(libc, "pwrite");
  libc_dup = dlsym(libc, "dup");
  libc_dup2 = dlsym(libc, "dup2");
  libc_lseek = dlsym(libc, "lseek");

  libc_stat = dlsym(libc, "stat");
  libc_fstat = dlsym(libc, "fstat");
  libc___xstat = dlsym(libc, "__xstat");
  libc___xstat64 = dlsym(libc, "__xstat64");
  libc___fxstat = dlsym(libc, "__fxstat");
  libc___fxstat64 = dlsym(libc, "__fxstat64");
  libc___lxstat = dlsym(libc, "__lxstat");
  libc___lxstat64 = dlsym(libc, "__lxstat64");

  libc_fopen = dlsym(libc, "fopen");
  libc_truncate = dlsym(libc, "truncate");
  libc_ftruncate = dlsym(libc, "ftruncate");

  libattr = dlopen("libattr.so.1", RTLD_LAZY);
  libattr_setxattr = dlsym(libattr, "setxattr");
  libattr_fsetxattr = dlsym(libattr, "setxattr");

  libc_fsync = dlsym(libc, "fsync");

  int stdout2 = ((funcptr_dup)libc_dup)(1);

  if (stdout2 != -1) {
    fdout = fdopen(stdout2, "a");
  }
  xprintf("initialize_passthrough(): New stdout %d\n", stdout2);
}

static pthread_once_t passthrough_initialized = PTHREAD_ONCE_INIT;

void initialize_passthrough_if_necessary() {
  pthread_once(&passthrough_initialized, initialize_passthrough);
}
