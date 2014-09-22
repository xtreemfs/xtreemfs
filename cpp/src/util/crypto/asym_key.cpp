/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/asym_key.h"

#include <boost/scope_exit.hpp>
#include <string>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

/**
 * Generates an asymmetric key.
 * Currently only RSA is supported.
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
 * Constructs an asymmetric key from an EVP_PKEY structure.
 * @param key
 */
AsymKey::AsymKey(EVP_PKEY* key)
    : key_(key) {
}

AsymKey::~AsymKey() {
  EVP_PKEY_free(key_);
}
EVP_PKEY* AsymKey::get_key() {
  return key_;
}

} /* namespace xtreemfs */
