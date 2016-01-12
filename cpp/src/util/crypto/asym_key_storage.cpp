/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/asym_key_storage.h"

#include <boost/bind.hpp>
#include <boost/scope_exit.hpp>
#include <algorithm>
#include <map>
#include <string>
#include <vector>

#include "libxtreemfs/helper.h"
#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

/**
 * @param path_pub_keys   Path to the directory there the public keys are
 *                        stored.
 * @param path_priv_keys  path to the directory there the private keys are
 *                        stored.
 * @param pem_key_pass    Passphrase for the private keys.
 */
AsymKeyStorage::AsymKeyStorage(const std::string& path_pub_keys,
                               const std::string& path_priv_keys,
                               const std::string& pem_key_pass)
    : path_pub_keys_(path_pub_keys),
      path_priv_keys_(path_priv_keys),
      pem_key_pass_(pem_key_pass) {
}

/**
 * @param key_id  The key id of the public key to retrieve.
 * @return  The public key to retrieve.
 */
AsymKey AsymKeyStorage::GetPubKey(const std::string& key_id) {
  // search for key in cache
  std::map<std::string, AsymKey>::iterator it;
  if ((it = pub_key_cache_.find(key_id)) != pub_key_cache_.end()) {
    return it->second;
  }

  // read key from PEM file
  FILE * fp;
  if (!(fp = fopen((path_pub_keys_ + key_id + ".pub.pem").c_str(), "r"))) {
    LogAndThrowXtreemFSException(
        "Failed to open public key file " + key_id + ".pub.pem in "
            + path_pub_keys_);
  }

  BOOST_SCOPE_EXIT((&fp)) {
      fclose(fp);
    }
  BOOST_SCOPE_EXIT_END

  EVP_PKEY * evp_key = PEM_read_PUBKEY(fp, NULL, NULL, NULL);
  if (evp_key == NULL) {
    LogAndThrowOpenSSLError(
        "Failed to read public key file " + key_id + ".pub.pem in "
            + path_pub_keys_);
  }
  AsymKey key(evp_key);

  // store key in cache
  pub_key_cache_[key_id] = key;

  return key;
}

/**
 * @param key_id  The key ids of the public keys to retrieve.
 * @return  The public keys to retrieve.
 */
std::vector<AsymKey> AsymKeyStorage::GetPubKeys(
    const std::vector<std::string>& key_ids) {
  std::vector<AsymKey> pub_keys(key_ids.size());
  std::transform(key_ids.begin(), key_ids.end(), pub_keys.begin(),
                 boost::bind(&AsymKeyStorage::GetPubKey, this, _1));
  return pub_keys;
}

/**
 * @param key_id  The key id of the private key to retrieve.
 * @return  The private key to retrieve.
 */
AsymKey AsymKeyStorage::GetPrivKey(const std::string& key_id) {
  // search for key in cache
  std::map<std::string, AsymKey>::iterator it;
  if ((it = priv_key_cache_.find(key_id)) != priv_key_cache_.end()) {
    return it->second;
  }

  // read key from PEM file
  FILE * fp;
  if (!(fp = fopen((path_priv_keys_ + key_id + ".priv.pem").c_str(), "r"))) {
    LogAndThrowXtreemFSException(
        "Failed to open private key file " + key_id + ".priv.pem in "
            + path_priv_keys_);
  }

  BOOST_SCOPE_EXIT((&fp)) {
      fclose(fp);
    }
  BOOST_SCOPE_EXIT_END

  EVP_PKEY * evp_key = PEM_read_PrivateKey(
      fp, NULL, NULL, const_cast<char*>(pem_key_pass_.c_str()));
  if (evp_key == NULL) {
    LogAndThrowOpenSSLError(
        "Failed to read private key file " + key_id + ".priv.pem in "
            + path_priv_keys_);
  }
  AsymKey key(evp_key);

  // store key in cache
  priv_key_cache_[key_id] = key;

  return key;
}

} /* namespace xtreemfs */
