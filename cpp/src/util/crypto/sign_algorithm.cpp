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

SignAlgorithm::SignAlgorithm(std::auto_ptr<AsymKey> key,
                             const std::string& alg_name) {
  if ((md_ = EVP_get_digestbyname(alg_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }

  if (key.get() != NULL) {
    set_key(key);
  }
}

void SignAlgorithm::Sign(boost::asio::const_buffer data,
                         boost::asio::mutable_buffer sig) const {
  assert(key_.get() != NULL);
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
  if (1 != EVP_DigestSignInit(mdctx, NULL, md_, NULL, key_->get_key())) {
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
  assert(slen == boost::asio::buffer_size(sig));
  /* Obtain the signature */
  if (1
      != EVP_DigestSignFinal(mdctx,
                             boost::asio::buffer_cast<unsigned char*>(sig),
                             &slen)) {
    LogAndThrowOpenSSLError();
  }
}

std::vector<unsigned char> SignAlgorithm::Sign(
    boost::asio::const_buffer data) const {
  std::vector<unsigned char> sig(signature_size_);
  Sign(data, boost::asio::buffer(sig));
  return sig;
}

bool SignAlgorithm::Verify(boost::asio::const_buffer data,
                           boost::asio::const_buffer sig) const {
  assert(key_.get() != NULL);
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
  if (1 != EVP_DigestVerifyInit(mdctx, NULL, md_, NULL, key_->get_key())) {
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
      != EVP_DigestVerifyFinal(
          mdctx,
          const_cast<unsigned char*>(boost::asio::buffer_cast<
              const unsigned char*>(sig)),
          boost::asio::buffer_size(sig))) {
    return false;
  }

  return true;
}

/**
 * @return  Size of the signature in bytes
 */
int SignAlgorithm::get_signature_size() const {
  assert(key_.get() != NULL);
  return signature_size_;
}

void SignAlgorithm::set_key(std::auto_ptr<AsymKey> key) {
  assert(key.get() != NULL);
  key_ = key;

  RSA* rsa_key = EVP_PKEY_get1_RSA(key_->get_key());

  BOOST_SCOPE_EXIT(&rsa_key) {
    /* Clean up */
      RSA_free(rsa_key);
    }
  BOOST_SCOPE_EXIT_END

  signature_size_ = RSA_size(rsa_key);
}

} /* namespace xtreemfs */
