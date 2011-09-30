/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_RECORD_MARKER_H_
#define CPP_INCLUDE_RPC_RECORD_MARKER_H_

#include <boost/cstdint.hpp>
#include <cstddef>

namespace xtreemfs {
namespace rpc {
using boost::uint32_t;

class RecordMarker {
 public:
  RecordMarker(uint32_t header_len,
               uint32_t message_len,
               uint32_t data_len);
  explicit RecordMarker(const char* buffer);
  void serialize(char* buffer) const;

  uint32_t data_len() const;
  uint32_t message_len() const;
  uint32_t header_len() const;

  static std::size_t get_size() {
    return sizeof(uint32_t)*3;
  }

 private:
  uint32_t header_len_;
  uint32_t message_len_;
  uint32_t data_len_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_RECORD_MARKER_H_

