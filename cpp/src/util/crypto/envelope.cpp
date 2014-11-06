/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/envelope.h"

#include <boost/scoped_array.hpp>
#include <boost/scope_exit.hpp>
#include <string>
#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

/**
 * Seals a given plaintext in an envelope.
 *
 * @param cipher_name   The cipher to used for symmetric encryption.
 * @param pub_keys      The asymmetric keys to use for encrypting the symmetric
 *                      encryption key.
 * @param plaintext     The plaintext to encrypt.
 * @param[out] encrypted_keys   The with the pub_keys encrypted symmetric key.
 * @param[out] iv               The generated IV for the symmetric encryption.
 * @param[out] ciphertext       The encrypted plaintext.
 */
void Envelope::Seal(std::string cipher_name, std::vector<AsymKey> pub_keys,
                    boost::asio::const_buffer plaintext,
                    std::vector<std::vector<unsigned char> >* encrypted_keys,
                    std::vector<unsigned char>* iv,
                    std::vector<unsigned char>* ciphertext) const {
  assert(encrypted_keys && iv && ciphertext);

  const EVP_CIPHER* cipher;
  EVP_CIPHER_CTX *ctx;
  int len;
  int ciphertext_len;

  if ((cipher = EVP_get_cipherbyname(cipher_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }

  boost::scoped_array<EVP_PKEY*> pub_keys_p_array(
      new EVP_PKEY*[pub_keys.size()]);
  encrypted_keys->resize(pub_keys.size());
  boost::scoped_array<unsigned char*> encrypted_keys_p_array(
      new unsigned char*[pub_keys.size()]);
  boost::scoped_array<int> encrypted_keys_len(new int[pub_keys.size()]);
  iv->resize(EVP_CIPHER_iv_length(cipher));
  ciphertext->resize(
      boost::asio::buffer_size(plaintext) + EVP_CIPHER_block_size(cipher));

  for (int i = 0; i < pub_keys.size(); i++) {
    pub_keys_p_array[i] = pub_keys[i].get_key();
    encrypted_keys->at(i).resize(EVP_PKEY_size(pub_keys[i].get_key()));
    encrypted_keys_p_array[i] = encrypted_keys->at(i).data();
  }

  if (!(ctx = EVP_CIPHER_CTX_new())) {
    LogAndThrowOpenSSLError();
  }

  BOOST_SCOPE_EXIT(&ctx) {
      EVP_CIPHER_CTX_free(ctx);
    }
  BOOST_SCOPE_EXIT_END

  if (1
      != EVP_SealInit(ctx, cipher, encrypted_keys_p_array.get(),
                      encrypted_keys_len.get(), iv->data(),
                      pub_keys_p_array.get(), pub_keys.size())) {
    LogAndThrowOpenSSLError();
  }

  if (1
      != EVP_SealUpdate(
          ctx, ciphertext->data(), &len,
          boost::asio::buffer_cast<const unsigned char*>(plaintext),
          boost::asio::buffer_size(plaintext))) {
    LogAndThrowOpenSSLError();
  }
  ciphertext_len = len;

  if (1 != EVP_SealFinal(ctx, ciphertext->data() + len, &len)) {
    LogAndThrowOpenSSLError();
  }
  ciphertext_len += len;

  for (int i = 0; i < pub_keys.size(); i++) {
    encrypted_keys->at(i).resize(encrypted_keys_len[i]);
  }
  ciphertext->resize(ciphertext_len);
}

/**
 * Opens an envelope and returns the contained plaintext.
 *
 * @param cipher_name     The cipher to used for symmetric encryption.
 * @param priv_key        The asymmetric keys used for encrypting the symmetric
 *                        encryption key.
 * @param ciphertext      The ciphertext to encrypt.
 * @param encrypted_key   The with priv_key encrypted symmetric key.
 * @param iv              The IV of the symmetric encryption.
 * @param[out] plaintext  Buffer to write the decrypted ciphertext into.
 *                        Caller must ensure that buffer has sufficient length.
 * @return  The length of the plaintext.
 */
int Envelope::Open(std::string cipher_name, AsymKey priv_key,
                   boost::asio::const_buffer ciphertext,
                   boost::asio::const_buffer encrypted_key,
                   boost::asio::const_buffer iv,
                   boost::asio::mutable_buffer plaintext) const {
  const EVP_CIPHER* cipher;
  if ((cipher = EVP_get_cipherbyname(cipher_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }
  return Open(cipher, priv_key, ciphertext, encrypted_key, iv, plaintext);
}

/**
 * Opens an envelope and returns the contained plaintext.
 *
 * @param cipher_name     The cipher to used for symmetric encryption.
 * @param priv_key        The asymmetric keys used for encrypting the symmetric
 *                        encryption key.
 * @param ciphertext      The ciphertext to encrypt.
 * @param encrypted_key   The with priv_key encrypted symmetric key.
 * @param iv              The IV of the symmetric encryption.
 * @param[out] plaintext  The decrypted ciphertext.
 * @return  The length of the plaintext.
 */
void Envelope::Open(std::string cipher_name, AsymKey priv_key,
                    boost::asio::const_buffer ciphertext,
                    boost::asio::const_buffer encrypted_key,
                    boost::asio::const_buffer iv,
                    std::vector<unsigned char>* plaintext) const {
  assert(plaintext);
  const EVP_CIPHER* cipher;
  if ((cipher = EVP_get_cipherbyname(cipher_name.c_str())) == NULL) {
    LogAndThrowOpenSSLError();
  }
  plaintext->resize(
      boost::asio::buffer_size(ciphertext) + EVP_CIPHER_block_size(cipher));
  int plaintext_len = Open(cipher, priv_key, ciphertext, encrypted_key, iv,
                           boost::asio::buffer(*plaintext));
  plaintext->resize(plaintext_len);
}

int Envelope::Open(const EVP_CIPHER* cipher, AsymKey priv_key,
                   boost::asio::const_buffer ciphertext,
                   boost::asio::const_buffer encrypted_key,
                   boost::asio::const_buffer iv,
                   boost::asio::mutable_buffer plaintext) const {
  assert(boost::asio::buffer_size(iv) == EVP_CIPHER_iv_length(cipher));

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

  if (1
      != EVP_OpenInit(
          ctx, cipher,
          boost::asio::buffer_cast<const unsigned char*>(encrypted_key),
          boost::asio::buffer_size(encrypted_key),
          boost::asio::buffer_cast<const unsigned char*>(iv),
          priv_key.get_key())) {
    LogAndThrowOpenSSLError();
  }

  if (1
      != EVP_OpenUpdate(
          ctx, boost::asio::buffer_cast<unsigned char*>(plaintext), &len,
          boost::asio::buffer_cast<const unsigned char*>(ciphertext),
          boost::asio::buffer_size(ciphertext))) {
    LogAndThrowOpenSSLError();
  }
  plaintext_len = len;

  if (1
      != EVP_OpenFinal(
          ctx, boost::asio::buffer_cast<unsigned char*>(plaintext) + len,
          &len)) {
    LogAndThrowOpenSSLError();
  }
  plaintext_len += len;
  assert(plaintext_len <= boost::asio::buffer_size(plaintext));

  return plaintext_len;
}

} /* namespace xtreemfs */
