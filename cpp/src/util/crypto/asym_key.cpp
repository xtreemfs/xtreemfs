/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/asym_key.h"

#include <boost/scope_exit.hpp>
#include <string>
#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

/**
 * Generates an asymmetric key.
 * Currently only RSA is supported.
 *
 * @param alg_name  "RSA"
 * @param [bits]    Optional length of the key.
 */
AsymKey::AsymKey(std::string alg_name, int bits) {
  assert(alg_name == "RSA");

  BIGNUM* bne = NULL;
  RSA* rsa_key = NULL;

  BOOST_SCOPE_EXIT(&bne, &rsa_key) {
    /* Clean up */
      BN_clear_free(bne);
      RSA_free(rsa_key);
    }
  BOOST_SCOPE_EXIT_END

  bne = BN_new();
  if (1 != BN_set_word(bne, RSA_F4)) {
    LogAndThrowOpenSSLError();
  }

  rsa_key = RSA_new();
  if (bits == 0) {
    bits = 2048;
  }
  if (1 != RSA_generate_key_ex(rsa_key, bits, bne, NULL)) {
    LogAndThrowOpenSSLError();
  }

  key_ = EVP_PKEY_new();
  if (1 != EVP_PKEY_set1_RSA(key_, rsa_key)) {
    EVP_PKEY_free(key_);
    LogAndThrowOpenSSLError();
  }
}

/**
 * Constructs an AsymKey Object from a DER encoded key.
 *
 * @param key   A DER encoded asymmetric key.
 */
AsymKey::AsymKey(std::vector<unsigned char> encoded_key) {
  const unsigned char* p_encoded_key = encoded_key.data();
  key_ = d2i_AutoPrivateKey(NULL, &p_encoded_key, encoded_key.size());
}

/**
 * Constructs an AsymKey Object from an EVP_PKEY structure.
 *
 * @param key   Ownership is transfered.
 */
AsymKey::AsymKey(EVP_PKEY* key)
    : key_(key) {
}

AsymKey::AsymKey(const AsymKey& other)
  : key_(other.get_key()) {
  CRYPTO_add(&key_->references, 1, CRYPTO_LOCK_EVP_PKEY);
}

AsymKey& AsymKey::operator=(AsymKey other)
{
    std::swap(other.key_, key_);
    return *this;
}

AsymKey::~AsymKey() {
  EVP_PKEY_free(key_);
}

/**
 * @return  The key in DER encoding.
 */
std::vector<unsigned char> AsymKey::GetDEREncodedKey() const {
  int len = i2d_PrivateKey(key_, NULL);
  std::vector<unsigned char> buffer(len);
  unsigned char* p_buffer = buffer.data();
  i2d_PrivateKey(key_, &p_buffer);
  return buffer;
}

/**
 * @return  The internally stored key structure.
 */
EVP_PKEY* AsymKey::get_key() const {
  assert(key_ != NULL);
  return key_;
}

} /* namespace xtreemfs */
