/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_RAND_H_
#define CPP_INCLUDE_UTIL_CRYPTO_RAND_H_

#include <openssl/evp.h>

#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <vector>

namespace xtreemfs {

class Rand {
 public:
  std::vector<unsigned char> Bytes(int num) const;

 private:
  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_RAND_H_
