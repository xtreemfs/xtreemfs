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
#include <string>
#include <vector>

namespace xtreemfs {

/**
 * Wrapper class for an asymmetric key.
 */
class AsymKey {
 public:
  AsymKey(std::string alg_name, int bits = 0);

  explicit AsymKey(std::vector<unsigned char> encoded_key);

  explicit AsymKey(EVP_PKEY* key);

  AsymKey(const AsymKey& other);

  AsymKey& operator=(AsymKey other);

  ~AsymKey();

  std::vector<unsigned char> GetDEREncodedKey() const;

  EVP_PKEY* get_key() const;

 private:
  // owned by the class
  EVP_PKEY* key_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_H_
