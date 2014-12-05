/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef PRELOAD_OPEN_FILE_TABLE_H_
#define PRELOAD_OPEN_FILE_TABLE_H_

#include <map>
#include <pthread.h>
#include <cstdio>
#include <stdint.h>

#include <boost/thread/mutex.hpp>
#include "libxtreemfs/file_handle.h"

namespace xtreemfs {
class Client;
class VolumeHandle;
}

class OpenFile {
 public:
  OpenFile(xtreemfs::FileHandle* fh, size_t append_buffer_size = 0);

  void Initialise();
  void Deinitialise();
  int GetFileDescriptor();

  xtreemfs::FileHandle* fh_;
  uint64_t offset_;

private:
  FILE * tmp_file_;
  int tmp_file_fd_;

public:
  bool enable_append_buffer;
  size_t append_buffer_size;
  uint64_t local_buffer_offset; // local offset inside the buffer, also used as size, when buffer is flushed
  uint64_t buffer_start_offset; // offset where buffer content starts in file
  char* read_buffer;
  char* write_buffer;
};

class OpenFileTable {
 public:
  OpenFileTable();
  ~OpenFileTable();

  int Register(xtreemfs::FileHandle* handle, size_t append_buffer_size = 0);
  void Unregister(int fd);
  OpenFile& Get(int fd); // NOTE: see comment in cpp
  int Set(int fd, xtreemfs::FileHandle* handle);
  void SetOffset(int fd, uint64_t offset);
  bool Has(int fd);

 private:
  boost::mutex mutex_;
  typedef std::map<int, OpenFile> FileTable;
  FileTable open_files_;// GUARDED_BY(mutex_);
  FILE * tmp_file_;
  int tmp_file_fd_;
  int next_fd_;
};

#endif  // PRELOAD_OPEN_FILE_TABLE_H_
