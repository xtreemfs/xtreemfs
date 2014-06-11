/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/message_digest.h"

#include <boost/scope_exit.hpp>
#include <string>
#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

MessageDigest::MessageDigest(std::string alg_name) {
  if ((md = EVP_get_digestbyname(alg_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }
}

std::vector<unsigned char> MessageDigest::digest(
    boost::asio::const_buffer buffer1, boost::asio::const_buffer buffer2) {
  EVP_MD_CTX* mdctx;

  if ((mdctx = EVP_MD_CTX_create()) == NULL) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT(&mdctx) {
      EVP_MD_CTX_destroy(mdctx);
    }
  BOOST_SCOPE_EXIT_END

  if (1 != EVP_DigestInit_ex(mdctx, md, NULL)) {
    LogAndThrowOpenSSLError();
  }

  if (1
      != EVP_DigestUpdate(mdctx, boost::asio::buffer_cast<const void*>(buffer1),
                          boost::asio::buffer_size(buffer1))) {
    LogAndThrowOpenSSLError();
  }

  if (1
      != EVP_DigestUpdate(mdctx, boost::asio::buffer_cast<const void*>(buffer2),
                          boost::asio::buffer_size(buffer2))) {
    LogAndThrowOpenSSLError();
  }

  std::vector<unsigned char> hash_value(digest_size());
  unsigned int hash_length;
  if (1 != EVP_DigestFinal_ex(mdctx, hash_value.data(), &hash_length)) {
    LogAndThrowOpenSSLError();
  }
  assert(hash_length <= hash_value.size());

  return hash_value;
}

int MessageDigest::digest_size() {
  return EVP_MD_size(md);
}

} /* namespace xtreemfs */
