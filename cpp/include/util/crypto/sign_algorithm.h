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
  SignAlgorithm(const AsymKey& key, const std::string& hash_name);

  int Sign(const boost::asio::const_buffer& data,
           const boost::asio::mutable_buffer& sig) const;

  std::vector<unsigned char> Sign(const boost::asio::const_buffer& data) const;

  bool Verify(const boost::asio::const_buffer& data,
              const boost::asio::const_buffer& sig) const;

  const std::string& get_hash_name() const;

  int get_signature_size() const;

  void set_key(const AsymKey& key);

 private:
  // not owned by the class
  const EVP_MD* md_;

  // the name of hash algorithm used for signing
  const std::string hash_name_;

  // key used for signing
  AsymKey key_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_SIGN_ALGORITHM_H_
