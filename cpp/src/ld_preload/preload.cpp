/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/preload.h"

#include <pthread.h>
#include <algorithm>
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

uint64_t xtreemfs_flush_write_buffer(OpenFile& handle);

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

  int fd = env->open_file_table_.Register(handle, env->options_.enable_append_buffer ? env->options_.append_buffer_size : 0);
  xprintf(" open on xtreemfs(%s) -> %d\n", pathname, fd);
  return fd;
}

int xtreemfs_close(int fd) {
  xprintf(" close xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);

  if(env->options_.enable_append_buffer)
    xtreemfs_flush_write_buffer(handle);

  env->open_file_table_.Unregister(fd);
  //handle.fh_->Flush(); // implicit by close
  handle.fh_->Close(); // TODO: error code
  return 0;
}

bool check_offset(int fd, uint64_t offset, const char* msg) {
  const uint64_t alignment = 0x40000; // 256 KiB
  if (offset % alignment != 0) {
    xprintf("ALIGNMENT-FAIL(%s): fd(%d), offset(%ld), alignment(%ld)\n", msg, fd, offset, alignment);
    return false;
  }
  return true;
}

uint64_t xtreemfs_pread(int fd, void* buf, uint64_t n_byte, uint64_t offset) {
  xprintf(" pread xtreemfs(%d)\n", fd);
//  check_offset(fd, offset, "xtreemfs_pread");
  OpenFile& handle = env->open_file_table_.Get(fd);

  // read only complete buffers from xtreemfs
  if (env->options_.enable_append_buffer) {
    const size_t buffer_size = env->options_.append_buffer_size;

    // read first complete buffer
    uint64_t read_offset = (offset / buffer_size) * buffer_size; // start offset at alignment boundary
    uint64_t buffer_offset = offset % buffer_size; // actual offset realative to alignment boundary
//xprintf(" pread xtreemfs XXXXXXX, read_buffer(%ld), buffer_size(%ld), read_offset(%ld)\n", handle.read_buffer, buffer_size, read_offset);    
    uint64_t n_read = handle.fh_->Read(handle.read_buffer, buffer_size, read_offset); // read a complete buffer from alignment boundary
//xprintf(" pread xtreemfs XXXXXXX2\n");
    if (n_read <= buffer_offset) // nothing was read for the caller, done
      return 0;
    
    n_read -= buffer_offset; // bytes read from caller offset, to end
    
    uint64_t n_read_caller = std::min(n_read, n_byte); // number of byte the caller wants (or at least gets) from this buffer starting at buffer_offset
//xprintf(" pread xtreemfs QQQQQQQ\n");        
    // memcpy(dest, src, n) to caller buffer
    memcpy((char*)buf, handle.read_buffer + buffer_offset, n_read_caller);
    
    if (n_read_caller == n_byte) // we got all the caller wanted
      return n_read_caller;
        
    uint64_t complete_buffers = (n_byte - n_read_caller) / buffer_size; // complete buffers to read
    read_offset += buffer_size; // advance aligned offset by one buffer
    for (size_t i = 0; i < complete_buffers; ++i) {
//xprintf(" pread xtreemfs CCCCCCC\n");    
      n_read = handle.fh_->Read((char*)buf + n_read_caller, buffer_size, read_offset); // read a complete buffer directly into the caller memory
      
      if (n_read < buffer_size) {
        return n_read_caller + n_read; // incomplete read by xtreemfs, we are done
      } 
      
      n_read_caller += buffer_size;
      read_offset += buffer_size;
    }
  
    // read remaining incomplete buffer
    uint64_t n_remaining = n_byte - n_read_caller;
    if (n_remaining > 0) {
//xprintf(" pread xtreemfs BBBBBBBB\n");    
      n_read = handle.fh_->Read(handle.read_buffer, buffer_size, read_offset);
      uint64_t n_copy = std::min(n_read, n_remaining);
//xprintf(" pread xtreemfs ZZZZZZZZ\n");    
      memcpy((char*)buf + n_read_caller, handle.read_buffer, n_copy);
      n_read_caller += n_copy;
    }
//xprintf(" pread xtreemfs YYYYYYY\n");        
    return n_read_caller;
  } else {
    return handle.fh_->Read((char*)buf, n_byte, offset);
  }
}

uint64_t xtreemfs_read(int fd, void* buf, uint64_t nbyte) {
  xprintf(" read xtreemfs(%d)\n", fd);
  OpenFile handle = env->open_file_table_.Get(fd);
//  check_offset(fd, handle.offset_, "xtreemfs_read");
  //int read = handle.fh_->Read((char*)buf, nbyte, handle.offset_);
  uint64_t read = xtreemfs_pread(fd, buf, nbyte, handle.offset_);
  env->open_file_table_.SetOffset(fd, handle.offset_ + read);
  return read;
}

uint64_t xtreemfs_flush_write_buffer(OpenFile& handle) {
    uint64_t written = 0;
    if (handle.local_buffer_offset > 0 ) {
  xprintf(" flushing write buffer: local_offset(%ld), start_offset(%ld)\n", handle.local_buffer_offset, handle.buffer_start_offset);
      written = handle.fh_->Write(handle.write_buffer, handle.local_buffer_offset, handle.buffer_start_offset);
    }
    
     handle.local_buffer_offset = 0;
    
    return written;
}

uint64_t xtreemfs_write(int fd, const void* buf, uint64_t n_byte) {
  xprintf(" write xtreemfs(%d)\n", fd);
  OpenFile& handle = env->open_file_table_.Get(fd);
//  check_offset(fd, handle.offset_, "xtreemfs_write");

  // write only complete buffers to xtreemfs and cache partial writes locally
  if (env->options_.enable_append_buffer) {
    const size_t buffer_size = env->options_.append_buffer_size;

    xprintf(" write xtreemfs(%d), handle.offset_(%ld), n_byte(%ld), handle.local_buffer_offset(%ld), o mod bs(%ld) \n", fd, handle.offset_, n_byte, handle.local_buffer_offset,  handle.offset_ % buffer_size);
    //assert((handle.offset_ % buffer_size) == handle.local_buffer_offset); // make sure this write continues where the last write ended

    uint64_t write_offset = handle.local_buffer_offset; //handle.offset_ / buffer_size; // start offset at alignment boundary
    uint64_t n_written = 0; // returned by xtreemfs
    uint64_t n_written_caller = 0; // reported to the caller
    uint64_t n_copy = 0;
    
    // append to buffer
    if (write_offset != 0) {
//xprintf(" write xtreemfs(%d) append_buffer\n", fd);
//xprintf(" write xtreemfs(%d), handle.buffer_start_offset(%ld), o mod bs(%ld) \n", fd, handle.buffer_start_offset, (handle.offset_ / buffer_size) * buffer_size);
      assert(handle.buffer_start_offset == (handle.offset_ / buffer_size) * buffer_size); // make sure the append-buffer is logically mapped to the right block/line/stripe 

      n_written_caller = std::min(buffer_size - write_offset, n_byte);
      memcpy((char*)handle.write_buffer + write_offset, buf, n_written_caller);
      handle.local_buffer_offset += n_written_caller;

      if (handle.local_buffer_offset == buffer_size) {
        n_written = xtreemfs_flush_write_buffer(handle);
        env->open_file_table_.SetOffset(fd, handle.buffer_start_offset + buffer_size);
        assert(n_written == buffer_size);
      }

      if (n_written_caller == n_byte)
        return n_written_caller; // done      
    }
    // we have written and flushed up an alignment boundary, or start from one
    uint64_t complete_buffers = (n_byte - n_written_caller) / buffer_size; // complete buffers to write
    uint64_t caller_buffer_offset = n_written_caller;
    for (size_t i = 0; i < complete_buffers; ++i) {
//xprintf(" write xtreemfs(%d) full block handle.offset_(%ld)\n", fd, handle.offset_);
      // write-through complete buffers
      assert(caller_buffer_offset % buffer_size == 0);
      assert(handle.offset_ % buffer_size == 0);
      n_written = handle.fh_->Write((char*)buf + caller_buffer_offset, buffer_size, handle.offset_);
      
      if (n_written < buffer_size) {
        // TODO: something is wrong...
        assert(false);
      } 
      
      env->open_file_table_.SetOffset(fd, handle.offset_ + buffer_size);
      n_written_caller += buffer_size;
      caller_buffer_offset += buffer_size;
    }
    // append remaining incomplete buffer
    if (n_written_caller < n_byte) {
//xprintf(" write xtreemfs(%d) start buffer from remainder, handle.offset_(%ld)\n", fd, handle.offset_);
      n_copy = n_byte - n_written_caller;
      assert(n_copy < buffer_size);
      memcpy((char*)handle.write_buffer, (char*)buf + n_written_caller, n_copy);
      n_written_caller += n_copy;
      handle.local_buffer_offset = n_copy;
      handle.buffer_start_offset = handle.offset_;
      // NOTE: handle.offset_ is *not* set here, since we keep it in sync with the actually written data on the server
    }
    assert(n_written_caller == n_byte);
    return n_written_caller; // done
  } else {
    int written = handle.fh_->Write((char*)buf, n_byte, handle.offset_);
    env->open_file_table_.SetOffset(fd, handle.offset_ + written);
    return written;
  }
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
