/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "rpc/record_marker.h"

#ifdef _WIN32
#include <Winsock2.h>
#else
// Linux & co.
#include <arpa/inet.h>
#endif

#include <iostream>

#include "util/logging.h"

namespace xtreemfs {
namespace rpc {
using namespace xtreemfs::util;

RecordMarker::RecordMarker(uint32_t header_len,
                           uint32_t message_len,
                           uint32_t data_len)
    : header_len_(header_len),
      message_len_(message_len),
      data_len_(data_len) {
}

RecordMarker::RecordMarker(const char* buffer) {
  const uint32_t* tmp = reinterpret_cast<const uint32_t*> (buffer);
  this->header_len_ = ntohl(tmp[0]);
  this->message_len_ = ntohl(tmp[1]);
  this->data_len_ = ntohl(tmp[2]);
}

void RecordMarker::serialize(char* buffer) const {
  uint32_t* tmp = reinterpret_cast<uint32_t*> (buffer);
  tmp[0] = htonl(this->header_len_);
  tmp[1] = htonl(this->message_len_);
  tmp[2] = htonl(this->data_len_);
}

uint32_t RecordMarker::data_len() const {
  return data_len_;
}

uint32_t RecordMarker::message_len() const {
  return message_len_;
}

uint32_t RecordMarker::header_len() const {
  return header_len_;
}

}  // namespace rpc
}  // namespace xtreemfs
