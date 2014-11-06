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
#include "util/crypto/asym_key.h"
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
                           std::vector<unsigned char>* file_enc_key,
                           SignAlgorithm* file_sign_algo);

 private:
  void GetFileKeys(const pbrpc::UserCredentials& user_credentials,
                   const pbrpc::XCap& xcap, const std::string& meta_file_name,
                   std::vector<unsigned char>* file_enc_key,
                   SignAlgorithm* file_sign_algo);

  pbrpc::FileLockbox CreateNewLockbox(const pbrpc::XCap& xcap,
                                      std::vector<unsigned char>* file_enc_key,
                                      SignAlgorithm* file_sign_algo);

  void SetLockbox(const pbrpc::UserCredentials& user_credentials,
                  const std::string& meta_file_name,
                  const pbrpc::FileLockbox& lockbox);

  /**
   * Sign algorithm used to sign the lockbox.
   */
  SignAlgorithm sign_algo_;

  /**
   * User key for asymmetric encryption.
   */
  AsymKey user_enc_key_;

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
