/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_H_
#define CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_H_

#include <openssl/evp.h>

#include <boost/asio/buffer.hpp>
#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <boost/noncopyable.hpp>
#include <string>

namespace xtreemfs {

class AsymKey : private boost::noncopyable {
 public:
  explicit AsymKey(std::string alg_name, int bits = 0);

  explicit AsymKey(EVP_PKEY* key);

  ~AsymKey();

  EVP_PKEY* get_key();

 private:
  EVP_PKEY* key_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_H_
