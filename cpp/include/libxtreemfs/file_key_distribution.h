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

#include "pbrpc/RPC.pb.h"
#include "util/crypto/asym_key.h"
#include "util/crypto/asym_key_storage.h"
#include "util/crypto/envelope.h"
#include "util/crypto/rand.h"
#include "util/crypto/sign_algorithm.h"
#include "xtreemfs/Encryption.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

class FileHandle;
class VolumeImplementation;

class FileKeyDistribution {
 public:
  explicit FileKeyDistribution(VolumeImplementation* volume);

  FileHandle* OpenMetaFile(const pbrpc::UserCredentials& user_credentials,
                           const pbrpc::XCap& xcap,
                           const std::string& file_path, uint32_t mode,
                           pbrpc::FileLockbox* lockbox);

  void ChangeAccessRights(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& file_path, const xtreemfs::pbrpc::Stat& stat,
      xtreemfs::pbrpc::Setattrs to_set);

 private:
  pbrpc::FileLockbox GetFileKeys(const pbrpc::UserCredentials& user_credentials,
                                 const std::string& file_path,
                                 const std::string& file_id);

  pbrpc::FileLockbox CreateAndSetMetadataAndLockbox(
      const pbrpc::UserCredentials& user_credentials,
      const std::string& file_path, const std::string& file_id, uint32_t mode);

  void SetMetadataAndLockbox(const pbrpc::UserCredentials& user_credentials,
                             const std::string& file_path,
                             const pbrpc::FileMetadata& file_metadata,
                             const pbrpc::FileLockbox& lockbox_rw,
                             const pbrpc::FileLockbox& lockbox_r);

  pbrpc::FileLockbox GetLockbox(const pbrpc::UserCredentials& user_credentials,
                                const std::string& file_path,
                                const AsymKey& lockbox_sign_key,
                                const std::string& key_id, bool write_lockbox);

  void SetLockbox(const pbrpc::UserCredentials& user_credentials,
                  const std::string& file_path, const SignAlgorithm& sign_algo,
                  const std::vector<std::string>& key_ids,
                  const std::vector<AsymKey>& pub_enc_keys,
                  const pbrpc::FileLockbox& lockbox, bool write_lockbox);

  std::string GetAccessLockboxKeys(
      const pbrpc::UserCredentials& user_credentials,
      const std::string& file_path, AsymKey* lockbox_sign_key,
      bool* write_access);

  void GetSetLockboxKeys(const pbrpc::FileMetadata& file_metadata,
                         SignAlgorithm* file_sign_algo,
                         std::vector<std::string>* key_ids_rw,
                         std::vector<std::string>* key_ids_r);

  /**
   *
   */
  Rand rand;

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
