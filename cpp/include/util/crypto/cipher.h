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
  explicit Cipher(std::string algo_name);

  std::pair<std::vector<unsigned char>, int> encrypt(
      boost::asio::const_buffer plaintext, std::vector<unsigned char> key,
      boost::asio::mutable_buffer ciphertext);

  int decrypt(boost::asio::const_buffer ciphertext,
              std::vector<unsigned char> key, std::vector<unsigned char> iv,
              boost::asio::mutable_buffer plaintext);

  int block_size();

  int key_size();

  int iv_size();

 private:
  const EVP_CIPHER* cipher_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_CIPHER_H_
