/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_STORAGE_H_
#define CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_STORAGE_H_

#include <openssl/evp.h>

#include <boost/asio/ssl/detail/openssl_init.hpp>
#include <map>
#include <string>
#include <vector>

#include "util/crypto/asym_key.h"

namespace xtreemfs {

/**
 * A simple key storage for asymmetric keys.
 * All keys are stored in the PEM format in a file with the name of the key id
 * and the file extension .pub.pem and .priv.pem.
 */
class AsymKeyStorage {
 public:
  AsymKeyStorage(const std::string& path_pub_keys,
                 const std::string& path_priv_keys,
                 const std::string& pem_key_pass);

  AsymKey GetPubKey(const std::string& key_id);

  std::vector<AsymKey> GetPubKeys(const std::vector<std::string>& key_ids);

  AsymKey GetPrivKey(const std::string& key_id);

 private:
  /**
   * Path to the directory there the public keys are stored.
   */
  std::string path_pub_keys_;

  /**
   * Path to the directory there the private keys are stored.
   */
  std::string path_priv_keys_;

  /**
   * Passphrase for the private keys.
   */
  std::string pem_key_pass_;

  /**
   * Cache for the stored private keys.
   */
  std::map<std::string, AsymKey> priv_key_cache_;

  /**
   * Cache for the stored public keys.
   */
  std::map<std::string, AsymKey> pub_key_cache_;

  // Ensure openssl is initialised.
  boost::asio::ssl::detail::openssl_init<> init_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_ASYM_KEY_STORAGE_H_
