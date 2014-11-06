/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/file_key_distribution.h"

#include <string>
#include <vector>

#include "libxtreemfs/helper.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/crypto/cipher.h"
#include "util/crypto/base64.h"
#include "xtreemfs/Encryption.pb.h"

using xtreemfs::pbrpc::SymEncBytes;
using xtreemfs::pbrpc::FileLockbox;
using xtreemfs::pbrpc::SignedFileLockbox;

namespace xtreemfs {

FileKeyDistribution::FileKeyDistribution(VolumeImplementation* volume)
    : sign_algo_(std::auto_ptr<AsymKey>(NULL),
                 volume->volume_options().encryption_hash),
      volume_(volume) {
  // TODO(plieser): get user keys
  std::string encoded_user_sign_key =
      "MIICXgIBAAKBgQDHo3StkYya5L/T3/ZIGTdmt347cwldwxKO3uAA8iBlG+dowht8"
          "NHfar0hT61HnRVsH5tcmGB2r9HT4EhAFlrO9vT8QaQrQL6cX5f731YsOeYlC6gAz"
          "azWROnc2/jt5b9vcS4DlV6nz4ud8PEH9SkhtC4CdaWxWH03AoSEyVC0vBwIDAQAB"
          "AoGBAL6fK7zDmn8X5ra3ReEn+sdgc+7t88aMij7TPw6IIziIAVj85uOc8chkz+oZ"
          "atYqWjZcS5j7M/HJ9JoeHSBI+ouEGG/roTXlpzOr9U30qlQYi4WIOHscZGxd+/3y"
          "d1XwwQkkLaJOGp1meOLRMasfSIlpVA6c1EDu2ANVx6hoUkBpAkEA7+jIfRNQul+A"
          "ko0Ds3lCbd5o5JELMP+paMGvls9XwLc9np//C0OPMdmSKz+dRxOQ66YvWjT5mThI"
          "roFoRQIvxQJBANUHOifHucAybmyDvt5w0ZoDj0jXRU1nNfV8tsV21w1qTCfai1uD"
          "JpUSLhUZJvfnkQb4WOwCj3Swd1KbNv9cpFsCQQCxvmrD2Aqoelc8vMMwNjfURMK8"
          "DQYYoGI4Hb/k4Otn+ZrqqimAg+ZUjZiw+CmjXkixfmd40uTV8xBOUcwZzJvtAkBY"
          "ArhgHv/7C9rbMkL1G589BiN4cJfNNsrwNSo9wq9ud3AnNv9EO5cBF5W6Wb3jxeQB"
          "ATGbsCMcjpt9oWrDbb7pAkEA3YDyrjg6/Nrr2Q/mi2nIib6HJ3BEG2ds16VYAsKQ"
          "7E8Bd+fRvCvM1ZLuuaJMkPNL9jkPX/11qB8g7HOtHtA3/Q==";
  Base64Encoder b64Encoder;
  std::auto_ptr<AsymKey> user_sign_key(
      new AsymKey(
          b64Encoder.Decode(boost::asio::buffer(encoded_user_sign_key))));
  sign_algo_.set_key(user_sign_key);
  user_enc_key_ = AsymKey(
      b64Encoder.Decode(boost::asio::buffer(encoded_user_sign_key)));
}

FileHandle* FileKeyDistribution::OpenMetaFile(
    const pbrpc::UserCredentials& user_credentials, const pbrpc::XCap& xcap,
    std::vector<unsigned char>* file_enc_key, SignAlgorithm* file_sign_algo) {
  assert(file_enc_key && file_sign_algo);

  std::string meta_file_name(
      "/.xtreemfs_enc_meta_files/"
          + boost::lexical_cast<std::string>(ExtractFileIdFromXCap(xcap)));
  FileHandle* meta_file;
  try {
    meta_file = volume_->OpenFile(user_credentials, meta_file_name,
                                  pbrpc::SYSTEM_V_FCNTL_H_O_RDWR, 0777);
    GetFileKeys(user_credentials, xcap, meta_file_name, file_enc_key,
                file_sign_algo);
  } catch (const PosixErrorException& e) {  // NOLINT
    // file didn't exist yet
    try {
      meta_file = volume_->OpenFile(
          user_credentials,
          meta_file_name,
          static_cast<pbrpc::SYSTEM_V_FCNTL>(pbrpc::SYSTEM_V_FCNTL_H_O_CREAT
              | pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
          0777);
    } catch (const PosixErrorException& e) {  // NOLINT
      // "/.xtreemfs_enc_meta_files" directory does not exist yet
      volume_->MakeDirectory(user_credentials, "/.xtreemfs_enc_meta_files",
                             0777);
      meta_file = volume_->OpenFile(
          user_credentials,
          meta_file_name,
          static_cast<pbrpc::SYSTEM_V_FCNTL>(pbrpc::SYSTEM_V_FCNTL_H_O_CREAT
              | pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
          0777);
    }
    SetLockbox(user_credentials, meta_file_name,
               CreateNewLockbox(xcap, file_enc_key, file_sign_algo));
  }

  return meta_file;
}

void FileKeyDistribution::GetFileKeys(
    const pbrpc::UserCredentials& user_credentials, const pbrpc::XCap& xcap,
    const std::string& meta_file_name, std::vector<unsigned char>* file_enc_key,
    SignAlgorithm* file_sign_algo) {
  assert(file_enc_key && file_sign_algo);

  // get asym encrypted lockbox encryption key
  std::string enc_lockbox_enc_key;
  volume_->GetXAttr(
      user_credentials, meta_file_name,
      "xtreemfs_.enc.lockbox_rw_enc_key.user." + user_credentials.username(),
      &enc_lockbox_enc_key);

  // get signed then encrypted lockbox
  std::string ste_lockbox_str;
  volume_->GetXAttr(user_credentials, meta_file_name,
                    "xtreemfs_.enc.lockbox_rw", &ste_lockbox_str);
  SymEncBytes ste_lockbox;
  ste_lockbox.ParseFromString(ste_lockbox_str);

  // decrypt signed file lockbox
  std::vector<unsigned char> signed_lockbox_buffer;
  envelope_.Open(ste_lockbox.cipher(), user_enc_key_,
                 boost::asio::buffer(ste_lockbox.ciphertext()),
                 boost::asio::buffer(enc_lockbox_enc_key),
                 boost::asio::buffer(ste_lockbox.iv()), &signed_lockbox_buffer);

  // verify signature of file lockbox
  SignedFileLockbox signed_lockbox;
  signed_lockbox.ParseFromArray(signed_lockbox_buffer.data(),
                                signed_lockbox_buffer.size());
  sign_algo_.Verify(boost::asio::buffer(signed_lockbox.lockbox()),
                    boost::asio::buffer(signed_lockbox.signature()));

  // read lockbox
  FileLockbox lockbox;
  lockbox.ParseFromString(signed_lockbox.lockbox());
  if (lockbox.file_id() != xcap.file_id()) {
    throw XtreemFSException("Wrong FileID in lockbox");
  }
  file_enc_key->assign(lockbox.enc_key().begin(), lockbox.enc_key().end());
  file_sign_algo->set_key(
      std::auto_ptr<AsymKey>(
          new AsymKey(
              std::vector<unsigned char>(lockbox.sign_key().begin(),
                                         lockbox.sign_key().end()))));
}

/**
 * Create a new file lockbox for a newly created file.
 *
 * @param xcap
 * @param volume
 * @param[out] file_enc_key
 * @param[out] file_sign_algo
 * @return
 */
pbrpc::FileLockbox FileKeyDistribution::CreateNewLockbox(
    const pbrpc::XCap& xcap, std::vector<unsigned char>* file_enc_key,
    SignAlgorithm* file_sign_algo) {
  assert(file_enc_key);

  FileLockbox lockbox;
  lockbox.set_file_id(xcap.file_id());
  // TODO(plieser): random salt
  lockbox.set_file_id_salt("00");
  lockbox.set_cipher(volume_->volume_options().encryption_cipher);
  // generate and set file enc key
  Cipher cipher(volume_->volume_options().encryption_cipher);
  cipher.GenerateKey(file_enc_key);
  lockbox.set_enc_key(file_enc_key->data(), file_enc_key->size());
  // generate and set file sign key
  AsymKey file_sign_key("RSA");
  if (file_sign_algo) {
    file_sign_algo->set_key(std::auto_ptr<AsymKey>(new AsymKey(file_sign_key)));
  }
  std::vector<unsigned char> encoded_file_sign_key = file_sign_key
      .GetDEREncodedKey();
  lockbox.set_sign_key(encoded_file_sign_key.data(),
                       encoded_file_sign_key.size());

  return lockbox;
}

void FileKeyDistribution::SetLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& meta_file_name, const FileLockbox& lockbox) {
  // sign file lockbox
  SignedFileLockbox signed_lockbox;
  signed_lockbox.set_lockbox(lockbox.SerializeAsString());
  std::vector<unsigned char> sig = sign_algo_.Sign(
      boost::asio::buffer(signed_lockbox.lockbox()));
  signed_lockbox.set_signature(sig.data(), sig.size());

  // encrypt signed file lockbox
  std::vector<std::vector<unsigned char> > encrypted_keys;
  std::vector<unsigned char> iv;
  std::vector<unsigned char> ciphertext;
  envelope_.Seal(volume_->volume_options().encryption_cipher,
                 std::vector<AsymKey>(1, user_enc_key_),
                 boost::asio::buffer(signed_lockbox.SerializeAsString()),
                 &encrypted_keys, &iv, &ciphertext);

  // store signed then encrypted lockbox
  SymEncBytes ste_lockbox;
  ste_lockbox.set_cipher(volume_->volume_options().encryption_cipher);
  ste_lockbox.set_iv(iv.data(), iv.size());
  ste_lockbox.set_ciphertext(ciphertext.data(), ciphertext.size());
  volume_->SetXAttr(user_credentials, meta_file_name,
                    "xtreemfs_.enc.lockbox_rw", ste_lockbox.SerializeAsString(),
                    static_cast<pbrpc::XATTR_FLAGS>(0));

  // store asym encrypted lockbox encryption key
  volume_->SetXAttr(
      user_credentials, meta_file_name,
      "xtreemfs_.enc.lockbox_rw_enc_key.user." + user_credentials.username(),
      std::string(encrypted_keys[0].begin(), encrypted_keys[0].end()),
      static_cast<pbrpc::XATTR_FLAGS>(0));
}

}      // namespace xtreemfs
