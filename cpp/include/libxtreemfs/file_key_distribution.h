/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_FILE_KEY_DISTRIBUTION_H_
#define CPP_INCLUDE_LIBXTREEMFS_FILE_KEY_DISTRIBUTION_H_

#include <string>
#include <vector>

#include "libxtreemfs/volume_implementation.h"
#include "util/crypto/asym_key_storage.h"
#include "util/crypto/envelope.h"
#include "util/crypto/sign_algorithm.h"
#include "xtreemfs/Encryption.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class FileKeyDistribution {
 public:
  explicit FileKeyDistribution(VolumeImplementation* volume);

  FileHandle* OpenMetaFile(const pbrpc::UserCredentials& user_credentials,
                           const pbrpc::XCap& xcap,
                           const std::string& file_path,
                           std::vector<unsigned char>* file_enc_key,
                           SignAlgorithm* file_sign_algo);

 private:
  void GetFileKeys(const pbrpc::UserCredentials& user_credentials,
                   const std::string& file_path, const std::string& file_id,
                   std::vector<unsigned char>* file_enc_key,
                   SignAlgorithm* file_sign_algo);

  void CreateAndSetNewLockbox(const pbrpc::UserCredentials& user_credentials,
                              const std::string& file_path,
                              const std::string& file_id,
                              std::vector<unsigned char>* file_enc_key,
                              SignAlgorithm* file_sign_algo);

  void SetLockbox(const pbrpc::UserCredentials& user_credentials,
                  const std::string& file_path, const SignAlgorithm& sig_algo,
                  const std::vector<std::string>& key_ids,
                  const std::vector<AsymKey>& pub_enc_keys,
                  const pbrpc::FileLockbox& lockbox, bool write_lockbox);

  std::string GetAccessLockboxKeys(
      const pbrpc::UserCredentials& user_credentials,
      const std::string& file_path, SignAlgorithm* file_sign_algo,
      bool* write_access);

  void GetSetLockboxKeys(const pbrpc::UserCredentials& user_credentials,
                         const std::string& file_path,
                         SignAlgorithm* file_sign_algo,
                         std::vector<std::string>* key_ids_rw,
                         std::vector<std::string>* key_ids_r);

  /**
   * Key storage from there to get the public and private keys.
   */
  AsymKeyStorage key_storage_;

  /**
   * Asymmetric encrypter.
   */
  Envelope envelope_;

  /**
   * Pointer to the volume. Not owned by the class.
   */
  VolumeImplementation* volume_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_FILE_KEY_DISTRIBUTION_H_
