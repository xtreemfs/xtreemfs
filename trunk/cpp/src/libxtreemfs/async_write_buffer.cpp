/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/async_write_buffer.h"

#include <cassert>
#include <cstring>

#include "xtreemfs/OSD.pb.h"

namespace xtreemfs {

AsyncWriteBuffer::AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                                   const char* data,
                                   size_t data_length,
                                   FileHandleImplementation* file_handle)
    : write_request(write_request),
      data_length(data_length),
      file_handle(file_handle),
      use_uuid_iterator(true) {
  assert(write_request && data && file_handle);
  this->data = new char[data_length];
  memcpy(this->data, data, data_length);
}

AsyncWriteBuffer::AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                                   const char* data,
                                   size_t data_length,
                                   FileHandleImplementation* file_handle,
                                   const std::string& osd_uuid)
    : write_request(write_request),
      data_length(data_length),
      file_handle(file_handle),
      use_uuid_iterator(false),
      osd_uuid(osd_uuid) {
  assert(write_request && data && file_handle);
  this->data = new char[data_length];
  memcpy(this->data, data, data_length);
}

AsyncWriteBuffer::~AsyncWriteBuffer() {
  delete write_request;
  delete[] data;
}

}  // namespace xtreemfs
