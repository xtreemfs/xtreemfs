/*
 * Copyright (c) 2009-2011 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_RAID0_H_
#define CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_RAID0_H_

#include <algorithm>
#include <vector>

#include "libxtreemfs/stripe_translator.h"

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

class StripeTranslatorRaid0 : public StripeTranslator {
 public:
  virtual void TranslateWriteRequest(
      const char *buf,
      size_t size,
      uint64_t offset,
      PolicyContainer policies,
      std::vector<WriteOperation>* operations) const;

  virtual void TranslateReadRequest(
      char *buf,
      size_t size,
      uint64_t offset,
      PolicyContainer policies,
      std::vector<ReadOperation>* operations) const;

  virtual size_t ProcessReads(
          std::vector<ReadOperation>* operations,
          char *buf,
          size_t size,
          uint64_t offset,
          PolicyContainer policies,
          bool erasure
          ) const;
};
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODE_H_
