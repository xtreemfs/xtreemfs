/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_CIPHER_H_
#define CPP_INCLUDE_UTIL_CRYPTO_CIPHER_H_

#include <openssl/evp.h>

#include <boost/asio/buffer.hpp>
#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <string>
#include <utility>
#include <vector>

namespace xtreemfs {

class Cipher {
 public:
  explicit Cipher(const std::string& algo_name);

  std::pair<std::vector<unsigned char>, int> encrypt(
      boost::asio::const_buffer plaintext,
      const std::vector<unsigned char>& key,
      boost::asio::mutable_buffer ciphertext) const;

  int decrypt(boost::asio::const_buffer ciphertext,
              const std::vector<unsigned char>& key,
              const std::vector<unsigned char>& iv,
              boost::asio::mutable_buffer plaintext) const;

  void GenerateKey(std::vector<unsigned char>* key) const;

  int block_size() const;

  int key_size() const;

  int iv_size() const;

 private:
  // not owned by the class
  const EVP_CIPHER* cipher_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_CIPHER_H_
