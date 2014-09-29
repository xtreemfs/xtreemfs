/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/base64.h"

#include <boost/scope_exit.hpp>
#include <string>
#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

std::string Base64Encoder::Encode(boost::asio::const_buffer data) {
  BIO *bio, *b64;

  b64 = BIO_new(BIO_f_base64());
  BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
  bio = BIO_new(BIO_s_mem());
  BIO_push(b64, bio);

  BIO_write(b64, boost::asio::buffer_cast<const void*>(data),
            boost::asio::buffer_size(data));
  // TODO(plieser): compiler warning unused result
  BIO_flush(b64);

  std::vector<char> vec(BIO_ctrl_pending(bio));
  BIO_read(bio, vec.data(), vec.size());
  std::string str(vec.begin(), vec.end());

  BIO_free_all(b64);

  return str;
}

std::vector<unsigned char> Base64Encoder::Decode(
    boost::asio::const_buffer data) {
  BIO *bio, *b64;

  b64 = BIO_new(BIO_f_base64());
  BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
  bio = BIO_new_mem_buf(
      const_cast<void*>(boost::asio::buffer_cast<const void*>(data)),
      boost::asio::buffer_size(data));
  BIO_push(b64, bio);

  std::vector<unsigned char> vec(BIO_ctrl_pending(bio));
  BIO_read(b64, vec.data(), vec.size());

  BIO_free_all(b64);

  return vec;
}

} /* namespace xtreemfs */
