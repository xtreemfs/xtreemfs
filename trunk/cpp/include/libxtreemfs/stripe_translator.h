/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_
#define CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_

#include <vector>

#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class ReadOperation {
 public:
  ReadOperation(size_t _obj_number, size_t _osd_offset,
                size_t _req_size, size_t _req_offset,
                char *_data)
      : obj_number(_obj_number), osd_offset(_osd_offset),
        req_size(_req_size), req_offset(_req_offset),
        data(_data) {
  };

  size_t obj_number;
  size_t osd_offset;
  size_t req_size;
  size_t req_offset;
  char *data;
};

class WriteOperation {
 public:
  WriteOperation(size_t _obj_number, size_t _osd_offset,
                 size_t _req_size, size_t _req_offset,
                 const char *_data)
      : obj_number(_obj_number), osd_offset(_osd_offset),
        req_size(_req_size), req_offset(_req_offset),
        data(_data) {
  };

  size_t obj_number;
  size_t osd_offset;
  size_t req_size;
  size_t req_offset;
  const char *data;
};

class StripeTranslator {
 public:
  virtual ~StripeTranslator() {}
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      off_t offset,
      const xtreemfs::pbrpc::StripingPolicy& policy,
      std::vector<WriteOperation>* operations) const = 0;

  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      off_t offset,
      const xtreemfs::pbrpc::StripingPolicy& policy,
      std::vector<ReadOperation>* operations) const = 0;
};

class StripeTranslatorRaid0 : public StripeTranslator {
 public:
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      off_t offset,
      const xtreemfs::pbrpc::StripingPolicy& policy,
      std::vector<WriteOperation>* operations) const;

  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      off_t offset,
      const xtreemfs::pbrpc::StripingPolicy& policy,
      std::vector<ReadOperation>* operations) const;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_
