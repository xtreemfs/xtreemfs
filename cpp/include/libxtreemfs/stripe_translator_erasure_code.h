/*
 * Copyright (c) 2009-2011 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODE_H_
#define CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODE_H_

#include "libxtreemfs/stripe_translator.h"
#include <algorithm>
#include <vector>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

class StripeTranslatorErasureCode : public StripeTranslator {
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
 private:
  virtual size_t subtract_extra_reads(
      size_t received_data,
      int64_t offset,
      unsigned int stripe_size,
      unsigned int width,
      unsigned int parity_width) const;

};
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODE_H_
