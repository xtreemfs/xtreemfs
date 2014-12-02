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

namespace {
void WriteToBuffer(int object_no, const char* buffer_in, int offset_in_object,
                   int bytes_to_write,
                   xtreemfs::pbrpc::writeRequest* write_request,
                   char** buffer_out, size_t* buffer_out_len) {
  assert(buffer_in && buffer_out && buffer_out_len);
  assert(write_request->object_number() == object_no);
  write_request->set_offset(offset_in_object);
  *buffer_out_len = bytes_to_write;
  *buffer_out = new char[bytes_to_write];
  memcpy(*buffer_out, buffer_in, bytes_to_write);
}

}  // namespace unnamed

namespace xtreemfs {

AsyncWriteBuffer::AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                                   const char* data,
                                   size_t data_length,
                                   FileHandleImplementation* file_handle,
                                   XCapHandler* xcap_handler)
    : write_request(write_request),
      data_length(data_length),
      file_handle(file_handle),
      xcap_handler_(xcap_handler),
      use_uuid_iterator(true),
      state_(PENDING),
      retry_count_(0) {
  assert(write_request && data && file_handle);
  this->data = new char[data_length];
  memcpy(this->data, data, data_length);
}

AsyncWriteBuffer::AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                                   const char* data,
                                   size_t data_length,
                                   FileHandleImplementation* file_handle,
                                   XCapHandler* xcap_handler,
                                   const std::string& osd_uuid)
    : write_request(write_request),
      data_length(data_length),
      file_handle(file_handle),
      xcap_handler_(xcap_handler),
      use_uuid_iterator(false),
      osd_uuid(osd_uuid),
      state_(PENDING),
      retry_count_(0) {
  assert(write_request && data && file_handle);
  this->data = new char[data_length];
  memcpy(this->data, data, data_length);
}

AsyncWriteBuffer::AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                                   const char* data,
                                   size_t data_length,
                                   FileHandleImplementation* file_handle,
                                   XCapHandler* xcap_handler,
                                   boost::shared_ptr<ObjectEncryptor::WriteOperation> enc_write_op,
                                   PartialObjectReaderFunction reader_partial)
    : write_request(write_request),
      data_length(data_length),
      file_handle(file_handle),
      xcap_handler_(xcap_handler),
      use_uuid_iterator(true),
      state_(PENDING),
      retry_count_(0),
      enc_write_op_(enc_write_op) {
  assert(write_request && data && file_handle);

  PartialObjectWriterFunction writer_partial = boost::bind(&WriteToBuffer, _1,
                                                           _2, _3, _4,
                                                           write_request,
                                                           &this->data,
                                                           &this->data_length);
  enc_write_op_->Write(write_request->object_number(), data,
                       write_request->offset(), data_length, reader_partial,
                       writer_partial);
}

AsyncWriteBuffer::AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                                   const char* data,
                                   size_t data_length,
                                   FileHandleImplementation* file_handle,
                                   XCapHandler* xcap_handler,
                                   const std::string& osd_uuid,
                                   boost::shared_ptr<ObjectEncryptor::WriteOperation> enc_write_op,
                                   PartialObjectReaderFunction reader_partial)
    : write_request(write_request),
      data_length(data_length),
      file_handle(file_handle),
      xcap_handler_(xcap_handler),
      use_uuid_iterator(false),
      osd_uuid(osd_uuid),
      state_(PENDING),
      retry_count_(0),
      enc_write_op_(enc_write_op) {
  assert(write_request && data && file_handle);

  PartialObjectWriterFunction writer_partial = boost::bind(&WriteToBuffer, _1,
                                                           _2, _3, _4,
                                                           write_request,
                                                           &this->data,
                                                           &this->data_length);
  enc_write_op_->Write(write_request->object_number(), data,
                       write_request->offset(), data_length, reader_partial,
                       writer_partial);
}

AsyncWriteBuffer::~AsyncWriteBuffer() {
  delete write_request;
  delete[] data;
}

}  // namespace xtreemfs
