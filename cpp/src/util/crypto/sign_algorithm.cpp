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

/**
 * @param key   The key to use for signing.
 * @param hash_name   The name of the used hash algorithm.
 */
SignAlgorithm::SignAlgorithm(const AsymKey& key, const std::string& hash_name)
    : key_(key) {
  if ((md_ = EVP_get_digestbyname(hash_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }
}

/**
 * Signs the given data.
 *
 * @param data  The data to sign.
 * @param[out] sig  Buffer to store the signature in.
 * @return  The actual size of the signature.
 */
int SignAlgorithm::Sign(const boost::asio::const_buffer& data,
                        const boost::asio::mutable_buffer& sig) const {
  EVP_MD_CTX* mdctx;
  size_t slen;

  /* Create the Message Digest Context */
  if ((mdctx = EVP_MD_CTX_create()) == NULL) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT((&mdctx)) {
    /* Clean up */
      EVP_MD_CTX_destroy(mdctx);
    }
  BOOST_SCOPE_EXIT_END

  /* Initialise the DigestSign operation */
  if (1 != EVP_DigestSignInit(mdctx, NULL, md_, NULL, key_.get_key())) {
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
  assert(slen <= boost::asio::buffer_size(sig));
  /* Obtain the signature */
  if (1
      != EVP_DigestSignFinal(mdctx,
                             boost::asio::buffer_cast<unsigned char*>(sig),
                             &slen)) {
    LogAndThrowOpenSSLError();
  }

  return slen;
}

/**
 * Signs the given data.
 *
 * @param data  The data to sign.
 * @return  The signature.
 */
std::vector<unsigned char> SignAlgorithm::Sign(
    const boost::asio::const_buffer& data) const {
  std::vector<unsigned char> sig(get_signature_size());
  int len = Sign(data, boost::asio::buffer(sig));
  sig.resize(len);
  return sig;
}

/**
 * Verifies a signature.
 *
 * @param data  The signed data.
 * @param sig   The signature.
 * @return  True if the signature is valid.
 */
bool SignAlgorithm::Verify(const boost::asio::const_buffer& data,
                           const boost::asio::const_buffer& sig) const {
  EVP_MD_CTX* mdctx;

  /* Create the Message Digest Context */
  if ((mdctx = EVP_MD_CTX_create()) == NULL) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT((&mdctx)) {
    /* Clean up */
      EVP_MD_CTX_destroy(mdctx);
    }
  BOOST_SCOPE_EXIT_END

  /* Initialise the DigestVerify operation */
  if (1 != EVP_DigestVerifyInit(mdctx, NULL, md_, NULL, key_.get_key())) {
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
 * @return  The maximum size of a signature in bytes.
 */
int SignAlgorithm::get_signature_size() const {
  return EVP_PKEY_size(key_.get_key());
}

/**
 * Sets a new signing key.
 *
 * @param key   The key to use for signing
 */
void SignAlgorithm::set_key(const AsymKey& key) {
  key_ = key;
}

} /* namespace xtreemfs */
