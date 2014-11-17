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
};
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_ERASURE_CODE_H_
