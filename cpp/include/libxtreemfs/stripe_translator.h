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
                char *_data)
      : obj_number(_obj_number), osd_offsets(_osd_offsets),
        req_size(_req_size), req_offset(_req_offset),
        data(_data) {
  };

  size_t obj_number;
  OSDOffsetContainer osd_offsets;
  size_t req_size;
  size_t req_offset;
  char *data;
};

class WriteOperation {
 public:
  typedef std::vector<size_t> OSDOffsetContainer;

  WriteOperation(size_t _obj_number, OSDOffsetContainer _osd_offsets,
                 size_t _req_size, size_t _req_offset,
                 const char *_data)
      : obj_number(_obj_number), osd_offsets(_osd_offsets),
        req_size(_req_size), req_offset(_req_offset),
        data(_data) {
  };

  size_t obj_number;
  OSDOffsetContainer osd_offsets;
  size_t req_size;
  size_t req_offset;
  const char *data;
};

class StripeTranslator {
 public:
  typedef std::list<const xtreemfs::pbrpc::StripingPolicy*> PolicyContainer;

  virtual ~StripeTranslator() {}
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<WriteOperation>* operations) const = 0;

  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<ReadOperation>* operations) const = 0;
};

class StripeTranslatorRaid0 : public StripeTranslator {
 public:
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<WriteOperation>* operations) const;

  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<ReadOperation>* operations) const;
};

class StripeTranslatorEC : public StripeTranslator {
 public:
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<WriteOperation>* operations) const;

  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<ReadOperation>* operations) const;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_H_
