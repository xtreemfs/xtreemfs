/*
 * Copyright (c) 2009-2015 by Jan Fajerski, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODES_H_
#define CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODES_H_

#include "libxtreemfs/stripe_translator.h"
#include <algorithm>
#include <vector>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

class StripeTranslatorErasureCodes : public StripeTranslator {
 public:
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<WriteOperation>* operations) const;

  virtual size_t TranslateReadRequest(
      char *buf,
      size_t size,
      int64_t offset,
      PolicyContainer policies,
      std::vector<ReadOperation>* operations) const;

  virtual size_t ProcessReads(
          std::vector<ReadOperation>* operations,
          boost::dynamic_bitset<>* successful_reads,
          PolicyContainer policies,
          size_t received_data,
          int64_t offset
          ) const;

  virtual void Encode(
          unsigned int k,
          unsigned int m,
          unsigned int w,
          const char *data[],
          char **coding,
          unsigned int stripe_size
          ) const = 0;

  virtual void Decode(
          unsigned int k,
          unsigned int m,
          unsigned int w,
          char **data,
          char **coding,
          int *erasures,
          unsigned int stripe_size
          ) const = 0;
};
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODES_H_
