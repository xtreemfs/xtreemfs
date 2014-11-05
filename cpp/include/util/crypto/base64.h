/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_BASE64_H_
#define CPP_INCLUDE_UTIL_CRYPTO_BASE64_H_

#include <openssl/evp.h>

#include <boost/asio/buffer.hpp>
#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <string>
#include <vector>

namespace xtreemfs {

class Base64Encoder {
 public:
  std::string Encode(boost::asio::const_buffer data) const;

  std::vector<unsigned char> Decode(boost::asio::const_buffer data) const;

 private:
  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_BASE64_H_
