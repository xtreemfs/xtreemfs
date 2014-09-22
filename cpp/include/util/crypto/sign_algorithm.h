/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_SIGN_ALGORITHM_H_
#define CPP_INCLUDE_UTIL_CRYPTO_SIGN_ALGORITHM_H_

#include <openssl/evp.h>

#include <boost/asio/buffer.hpp>
#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <string>
#include <vector>

#include "util/crypto/asym_key.h"

namespace xtreemfs {

class SignAlgorithm {
 public:
  SignAlgorithm(std::auto_ptr<AsymKey> key, std::string alg_name);

  std::vector<unsigned char> Sign(boost::asio::const_buffer data);

  bool Verify(boost::asio::const_buffer data, boost::asio::mutable_buffer sig);

  int get_signature_size();

 private:
  const EVP_MD* md_;
  std::auto_ptr<AsymKey> key_;
  int signature_size_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_SIGN_ALGORITHM_H_
