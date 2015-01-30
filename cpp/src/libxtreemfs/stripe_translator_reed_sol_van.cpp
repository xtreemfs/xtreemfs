/*
 * Copyright (c) 2009-2015 by Jan Fajerski, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/stripe_translator_reed_sol_van.h"
#include "libxtreemfs/xtreemfs_exception.h"

#include <algorithm>
#include <boost/dynamic_bitset.hpp>
#include <jerasure.h>
#include <reed_sol.h>
#include <vector>
#include <iostream>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

void StripeTranslatorReedSolVan::Encode(
          unsigned int k,
          unsigned int m,
          unsigned int w,
          char **data,
          char **coding,
          unsigned int stripe_size
        ) const {
  //encode data here

  int *matrix = reed_sol_vandermonde_coding_matrix(k, m, w);
  jerasure_matrix_encode(
      static_cast<int>(k),
      static_cast<int>(m),
      static_cast<int>(w),
      matrix,
      data,
      coding,
      static_cast<int>(stripe_size));

}

void StripeTranslatorReedSolVan::Decode(
          unsigned int k,
          unsigned int m,
          unsigned int w,
          char **data,
          char **coding,
          vector<int> &erasures,
          unsigned int stripe_size
        ) const {
  // decode data here

  int *matrix = reed_sol_vandermonde_coding_matrix(k, m, w);
  erasures.push_back(-1);
  jerasure_matrix_decode(
      k,
      m,
      w,
      matrix,
      1,
      // workaround to get the raw data...in C++11 vector has a member var for that
      &erasures.front(),
      data,
      coding,
      stripe_size);
}

}  // namespace xtreemfs
