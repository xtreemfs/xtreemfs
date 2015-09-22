/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_
#define CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_

#include <stdint.h>

#include <list>
#include <vector>

#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class ReadOperation {
 public:
  typedef std::vector<size_t> OSDOffsetContainer;

  ReadOperation(size_t _obj_number, OSDOffsetContainer _osd_offsets,
                size_t _req_size, size_t _req_offset,
                char *_data, bool _is_aux = 0)
      : obj_number(_obj_number), osd_offsets(_osd_offsets),
        req_size(_req_size), req_offset(_req_offset),
        data(_data),
        is_aux(_is_aux),
        success(0),
        recv_size(0) {
  };

  size_t obj_number;
  OSDOffsetContainer osd_offsets;
  size_t req_size;
  size_t req_offset;
  char *data;
  bool is_aux;
  bool success;
  size_t recv_size;
};

class WriteOperation {
 public:
  typedef std::vector<size_t> OSDOffsetContainer;

  WriteOperation(size_t _obj_number, OSDOffsetContainer _osd_offsets,
                 size_t _req_size, size_t _req_offset,
                 const char *_data, bool _is_aux = 0)
      : obj_number(_obj_number), osd_offsets(_osd_offsets),
        req_size(_req_size), req_offset(_req_offset),
        data(_data),
        is_aux(_is_aux) {
  };

  size_t obj_number;
  OSDOffsetContainer osd_offsets;
  size_t req_size;
  size_t req_offset;
  const char *data;
  bool is_aux;
};

class StripeTranslator {
 public:
  typedef std::list<const xtreemfs::pbrpc::StripingPolicy*> PolicyContainer;

  virtual ~StripeTranslator() {}
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      uint64_t offset,
      PolicyContainer policies,
      std::vector<WriteOperation>* operations) const = 0;

  /*
   * fills operations vector with ReadOperations necessary for the Read operation. The read
   * operations are ordered to be excuted in the returned sequence.
   */
  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      uint64_t offset,
      PolicyContainer policies,
      std::vector<ReadOperation>* operations) const = 0;

  /*
   * post processing of executed read operations.
   * the operations vector holds the read operations including the data buffers and the
   * successful_reads vector shows which read operation was sucessfull.
   */
  virtual size_t ProcessReads(
          std::vector<ReadOperation>* operations,
          char *buf,
          size_t size,
          uint64_t offset,
          PolicyContainer policies,
          bool erasure
          ) const = 0;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_
