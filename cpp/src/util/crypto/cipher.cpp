/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/cipher.h"

#include <boost/scope_exit.hpp>
#include <string>
#include <utility>
#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

Cipher::Cipher(std::string alg_name) {
  if ((cipher_ = EVP_get_cipherbyname(alg_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }
}

/**
 * @return    Pair with first object being the iv and the second the  length of
 *            the ciphertext.
 */

std::pair<std::vector<unsigned char>, int> Cipher::encrypt(
    boost::asio::const_buffer plaintext, std::vector<unsigned char> key,
    boost::asio::mutable_buffer ciphertext) {
  assert(key_size() == key.size());

  EVP_CIPHER_CTX *ctx;
  int len;
  int ciphertext_len;
  std::vector<unsigned char> iv(iv_size());
  RAND_bytes(iv.data(), iv.size());

  if (!(ctx = EVP_CIPHER_CTX_new())) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT(&ctx) {
      EVP_CIPHER_CTX_free(ctx);
    }
  BOOST_SCOPE_EXIT_END

  if (1 != EVP_EncryptInit_ex(ctx, cipher_, NULL, key.data(), iv.data())) {
    LogAndThrowOpenSSLError();
  }

  if (1
      != EVP_EncryptUpdate(
          ctx, boost::asio::buffer_cast<unsigned char*>(ciphertext), &len,
          boost::asio::buffer_cast<const unsigned char*>(plaintext),
          boost::asio::buffer_size(plaintext))) {
    LogAndThrowOpenSSLError();
  }
  ciphertext_len = len;

  if (1
      != EVP_EncryptFinal_ex(
          ctx, boost::asio::buffer_cast<unsigned char*>(ciphertext) + len,
          &len)) {
    LogAndThrowOpenSSLError();
  }
  ciphertext_len += len;
  assert(ciphertext_len <= boost::asio::buffer_size(ciphertext));

  return std::make_pair(iv, ciphertext_len);
}

/**
 * @return    Length of plaintext.
 */
int Cipher::decrypt(boost::asio::const_buffer ciphertext,
                    std::vector<unsigned char> key,
                    std::vector<unsigned char> iv,
                    boost::asio::mutable_buffer plaintext) {
  EVP_CIPHER_CTX *ctx;
  int len;
  int plaintext_len;

  if (!(ctx = EVP_CIPHER_CTX_new())) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT(&ctx) {
      EVP_CIPHER_CTX_free(ctx);
    }
  BOOST_SCOPE_EXIT_END

  if (1 != EVP_DecryptInit_ex(ctx, cipher_, NULL, key.data(), iv.data())) {
    LogAndThrowOpenSSLError();
  }

  if (1
      != EVP_DecryptUpdate(
          ctx, boost::asio::buffer_cast<unsigned char*>(plaintext), &len,
          boost::asio::buffer_cast<const unsigned char*>(ciphertext),
          boost::asio::buffer_size(ciphertext))) {
    LogAndThrowOpenSSLError();
  }
  plaintext_len = len;

  if (1
      != EVP_DecryptFinal_ex(
          ctx, boost::asio::buffer_cast<unsigned char*>(plaintext) + len,
          &len)) {
    LogAndThrowOpenSSLError();
  }
  plaintext_len += len;
  assert(plaintext_len <= boost::asio::buffer_size(plaintext));
  return plaintext_len;
}

int Cipher::block_size() {
  return EVP_CIPHER_block_size(cipher_);
}

int Cipher::key_size() {
  return EVP_CIPHER_key_length(cipher_);
}

int Cipher::iv_size() {
  return EVP_CIPHER_iv_length(cipher_);
}

} /* namespace xtreemfs */
