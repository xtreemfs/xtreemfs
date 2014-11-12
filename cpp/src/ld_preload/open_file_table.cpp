/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/open_file_table.h"

#include <boost/thread/lock_guard.hpp>

#include "ld_preload/passthrough.h"

OpenFile::OpenFile(xtreemfs::FileHandle* fh) : fh_(fh), offset_(0), tmp_file_(NULL), tmp_file_fd_(-1) {
}

void OpenFile::Initialise() {
  tmp_file_ = tmpfile();
  tmp_file_fd_ = fileno(tmp_file_);
}

void OpenFile::Deinitialise() {
  if (tmp_file_ != NULL) {
    fclose(tmp_file_);
    tmp_file_ = NULL;
    tmp_file_fd_ = -1;
  }
}

int OpenFile::GetFileDescriptor() {
  return tmp_file_fd_;
}

OpenFileTable::OpenFileTable() {
  // open a temporary file as base for returning valid file descriptors that do no interfere with the passthrough values
  tmp_file_ = tmpfile();
  tmp_file_fd_ = fileno(tmp_file_);
  next_fd_ = 10000;
}

OpenFileTable::~OpenFileTable() {
  fclose(tmp_file_);
}

int OpenFileTable::Register(xtreemfs::FileHandle* handle) {
  boost::lock_guard<boost::mutex> guard(mutex_);
  //const int fd = ((funcptr_dup)libc_dup)(tmp_file_fd_); // NOTE: calling dup(tmp_file_fd_) would lead to a deadlock here
  OpenFile open_file(handle);
  open_file.Initialise();
  const int fd = open_file.GetFileDescriptor();
//    const int fd = next_fd_++;
  open_files_.insert(std::make_pair(fd, open_file));
  xprintf(" +fd(%d)\n", fd);
  return fd;
}

void OpenFileTable::Unregister(int fd) {
  boost::lock_guard<boost::mutex> guard(mutex_);
  FileTable::iterator i = open_files_.find(fd);
  if (i == open_files_.end()) {
    assert(false);
  } else {
    i->second.Deinitialise();
  }
  open_files_.erase(fd);
  //((funcptr_close)libc_close)(fd); // close(fd);
  xprintf(" -fd(%d)\n", fd);
}

OpenFile OpenFileTable::Get(int fd) {
  boost::lock_guard<boost::mutex> guard(mutex_);
  FileTable::iterator i = open_files_.find(fd);
  if (i == open_files_.end()) {
    return OpenFile(NULL);
  } else {
    return i->second;
  }
}

int OpenFileTable::Set(int fd, xtreemfs::FileHandle* handle) {
  xprintf(" +fd(%d)\n", fd);
  boost::lock_guard<boost::mutex> guard(mutex_);
  // TODO: fix, see Register
  open_files_.insert(std::make_pair(fd, OpenFile(handle)));
  return fd;
}

void OpenFileTable::SetOffset(int fd, uint64_t offset) {
  boost::lock_guard<boost::mutex> guard(mutex_);
  FileTable::iterator i = open_files_.find(fd);
  if (i == open_files_.end()) {
    return;
  } else {
    i->second.offset_ = offset;
  }
}

bool OpenFileTable::Has(int fd) {
  boost::lock_guard<boost::mutex> guard(mutex_);
  return open_files_.find(fd) != open_files_.end();
}
