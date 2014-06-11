/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_MESSAGE_DIGEST_H_
#define CPP_INCLUDE_UTIL_CRYPTO_MESSAGE_DIGEST_H_

#include <openssl/evp.h>

#include <boost/asio/buffer.hpp>
#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <string>
#include <vector>

namespace xtreemfs {

class MessageDigest {
 public:
  explicit MessageDigest(std::string algo_name);

  std::vector<unsigned char> digest(boost::asio::const_buffer buffer1,
                                    boost::asio::const_buffer buffer2 =
                                        boost::asio::const_buffer());

  int digest_size();

 private:
  const EVP_MD* md;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_MESSAGE_DIGEST_H_
