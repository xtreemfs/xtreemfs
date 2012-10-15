/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_ASYNC_WRITE_BUFFER_H_
#define CPP_INCLUDE_LIBXTREEMFS_ASYNC_WRITE_BUFFER_H_

#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <string>

namespace xtreemfs {

namespace pbrpc {
class writeRequest;
}  // namespace pbrpc

class FileHandleImplementation;
class XCapHandler;

struct AsyncWriteBuffer {
  /** Possible states of this object. */
  enum State {
    PENDING,
    FAILED,
    SUCCEEDED
  };

  /**
   * @remark Ownership of write_request is transferred to this object.
   */
  AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                   const char* data,
                   size_t data_length,
                   FileHandleImplementation* file_handle,
                   XCapHandler* xcap_handler);

  /**
   * @remark Ownership of write_request is transferred to this object.
   */
  AsyncWriteBuffer(xtreemfs::pbrpc::writeRequest* write_request,
                   const char* data,
                   size_t data_length,
                   FileHandleImplementation* file_handle,
                   XCapHandler* xcap_handler,
                   const std::string& osd_uuid);

  ~AsyncWriteBuffer();

  /** Additional information of the write request. */
  xtreemfs::pbrpc::writeRequest* write_request;

  /** Actual payload of the write request. */
  char* data;

  /** Length of the payload. */
  size_t data_length;

  /** FileHandle which did receive the Write() command. */
  FileHandleImplementation* file_handle;

  /** XCapHandler, used to update the XCap in case of retries. */
  XCapHandler* xcap_handler_;

  /** Set to false if the member "osd_uuid" is used instead of the FileInfo's
   *  osd_uuid_iterator in order to determine the OSD to be used. */
  bool use_uuid_iterator;

  /** UUID of the OSD which was used for the last retry or if use_uuid_iterator
   *  is false, this variable is initialized to the OSD to be used. */
  std::string osd_uuid;

  /** Resolved UUID */
  std::string service_address;

  /** Current state of the object. */
  State state_;

  /** Retry count.*/
  int retry_count_;

  /** Time when the request was sent */
  boost::posix_time::ptime request_sent_time;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_ASYNC_WRITE_BUFFER_H_
