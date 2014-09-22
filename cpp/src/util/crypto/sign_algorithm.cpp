/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/sign_algorithm.h"

#include <boost/scope_exit.hpp>
#include <string>
#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

SignAlgorithm::SignAlgorithm(std::auto_ptr<AsymKey> key, std::string alg_name)
    : key_(key) {
  if ((md_ = EVP_get_digestbyname(alg_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }

  RSA* rsa_key = EVP_PKEY_get1_RSA(key_.get()->get_key());

  BOOST_SCOPE_EXIT(&rsa_key) {
    /* Clean up */
      RSA_free(rsa_key);
    }
  BOOST_SCOPE_EXIT_END

  signature_size_ = RSA_size(rsa_key);
}

std::vector<unsigned char> SignAlgorithm::Sign(boost::asio::const_buffer data) {
  EVP_MD_CTX* mdctx;
  size_t slen;

  /* Create the Message Digest Context */
  if ((mdctx = EVP_MD_CTX_create()) == NULL) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT(&mdctx) {
    /* Clean up */
      EVP_MD_CTX_destroy(mdctx);
    }
  BOOST_SCOPE_EXIT_END

  /* Initialise the DigestSign operation */
  if (1 != EVP_DigestSignInit(mdctx, NULL, md_, NULL, key_.get()->get_key())) {
    LogAndThrowOpenSSLError();
  }

  /* Call update with the message */
  if (1
      != EVP_DigestSignUpdate(mdctx,
                              boost::asio::buffer_cast<const void*>(data),
                              boost::asio::buffer_size(data))) {
    LogAndThrowOpenSSLError();
  }

  /* Finalise the DigestSign operation */
  /* First call EVP_DigestSignFinal with a NULL sig parameter to obtain the length of the
   * signature. Length is returned in slen */
  if (1 != EVP_DigestSignFinal(mdctx, NULL, &slen)) {
    LogAndThrowOpenSSLError();
  }
  assert(slen == signature_size_);
  std::vector<unsigned char> sig(slen);
  /* Obtain the signature */
  if (1 != EVP_DigestSignFinal(mdctx, sig.data(), &slen)) {
    LogAndThrowOpenSSLError();
  }

  return sig;
}

bool SignAlgorithm::Verify(boost::asio::const_buffer data,
                           boost::asio::mutable_buffer sig) {
  EVP_MD_CTX* mdctx;

  /* Create the Message Digest Context */
  if ((mdctx = EVP_MD_CTX_create()) == NULL) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT(&mdctx) {
    /* Clean up */
      EVP_MD_CTX_destroy(mdctx);
    }
  BOOST_SCOPE_EXIT_END

  /* Initialise the DigestVerify operation */
  if (1
      != EVP_DigestVerifyInit(mdctx, NULL, md_, NULL, key_.get()->get_key())) {
    LogAndThrowOpenSSLError();
  }

  /* Call update with the message */
  if (1
      != EVP_DigestVerifyUpdate(mdctx,
                                boost::asio::buffer_cast<const void*>(data),
                                boost::asio::buffer_size(data))) {
    LogAndThrowOpenSSLError();
  }

  /* Finalise the DigestVerify operation */
  if (1
      != EVP_DigestVerifyFinal(mdctx,
                               boost::asio::buffer_cast<unsigned char*>(sig),
                               boost::asio::buffer_size(sig))) {
    return false;
  }

  return true;
}

/**
 * @return  Size of the signature in bytes
 */
int SignAlgorithm::get_signature_size() {
  return signature_size_;
}

} /* namespace xtreemfs */
