/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_ENVELOPE_H_
#define CPP_INCLUDE_UTIL_CRYPTO_ENVELOPE_H_

#include <openssl/evp.h>

#include <boost/asio/buffer.hpp>
#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <string>
#include <vector>

#include "util/crypto/asym_key.h"

namespace xtreemfs {

class Envelope {
 public:
  void Seal(std::string cipher, std::vector<AsymKey> pub_keys,
            boost::asio::const_buffer plaintext,
            std::vector<std::vector<unsigned char> >* encrypted_keys,
            std::vector<unsigned char>* iv,
            std::vector<unsigned char>* ciphertext) const;

  int Open(std::string cipher_name, AsymKey priv_key,
           boost::asio::const_buffer ciphertext,
           boost::asio::const_buffer encrypted_key,
           boost::asio::const_buffer iv,
           boost::asio::mutable_buffer plaintext) const;

  void Open(std::string cipher_name, AsymKey priv_key,
            boost::asio::const_buffer ciphertext,
            boost::asio::const_buffer encrypted_key,
            boost::asio::const_buffer iv,
            std::vector<unsigned char>* plaintext) const;

 private:
  int Open(const EVP_CIPHER* cipher, AsymKey priv_key,
           boost::asio::const_buffer ciphertext,
           boost::asio::const_buffer encrypted_key,
           boost::asio::const_buffer iv,
           boost::asio::mutable_buffer plaintext) const;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_ENVELOPE_H_
