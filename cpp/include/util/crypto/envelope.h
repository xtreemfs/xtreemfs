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
  void Seal(const std::string& cipher, const std::vector<AsymKey>& pub_keys,
            const boost::asio::const_buffer& plaintext,
            std::vector<std::vector<unsigned char> >* encrypted_keys,
            std::vector<unsigned char>* iv,
            std::vector<unsigned char>* ciphertext) const;

  int Open(const std::string& cipher_name, const AsymKey& priv_key,
           const boost::asio::const_buffer& ciphertext,
           const boost::asio::const_buffer& encrypted_key,
           const boost::asio::const_buffer& iv,
           const boost::asio::mutable_buffer& plaintext) const;

  void Open(const std::string& cipher_name, const AsymKey& priv_key,
            const boost::asio::const_buffer& ciphertext,
            const boost::asio::const_buffer& encrypted_key,
            const boost::asio::const_buffer& iv,
            std::vector<unsigned char>* plaintext) const;

 private:
  int Open(const EVP_CIPHER* cipher, const AsymKey& priv_key,
           const boost::asio::const_buffer& ciphertext,
           const boost::asio::const_buffer& encrypted_key,
           const boost::asio::const_buffer& iv,
           const boost::asio::mutable_buffer& plaintext) const;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_ENVELOPE_H_
