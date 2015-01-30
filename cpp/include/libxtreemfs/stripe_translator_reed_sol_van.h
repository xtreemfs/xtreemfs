/*
 * Copyright (c) 2009-2015 by Jan Fajerski, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_REED_SOL_VAN_H_
#define CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_REED_SOL_VAN_H_

#include "libxtreemfs/stripe_translator_erasure_codes.h"
#include <algorithm>
#include <vector>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

class StripeTranslatorReedSolVan : public StripeTranslatorErasureCodes {
 public:
   virtual void Encode(
          unsigned int k,
          unsigned int m,
          unsigned int w,
          char *data[],
          char **coding,
          unsigned int stripe_size
           ) const;

   virtual void Decode(
          unsigned int k,
          unsigned int m,
          unsigned int w,
          char **data,
          char **coding,
          vector<int> &erasures,
          unsigned int stripe_size
           ) const;

};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_STRIPE_TRANSLATOR_REED_SOL_VAN_H_
