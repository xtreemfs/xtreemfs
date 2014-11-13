/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/file_key_distribution.h"

#include <sys/stat.h>

#include <string>
#include <vector>

#include "libxtreemfs/helper.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/crypto/asym_key.h"
#include "util/crypto/cipher.h"
#include "util/crypto/base64.h"
#include "xtreemfs/Encryption.pb.h"

using xtreemfs::pbrpc::Stat;
using xtreemfs::pbrpc::SymEncBytes;
using xtreemfs::pbrpc::FileLockbox;
using xtreemfs::pbrpc::SignedFileLockbox;

namespace xtreemfs {

FileKeyDistribution::FileKeyDistribution(VolumeImplementation* volume)
    : key_storage_(volume->volume_options().encryption_pub_keys_path,
                   volume->volume_options().encryption_priv_keys_path,
                   volume->volume_options().ssl_pem_key_pass),
      volume_(volume) {
}

FileHandle* FileKeyDistribution::OpenMetaFile(
    const pbrpc::UserCredentials& user_credentials, const pbrpc::XCap& xcap,
    const std::string& file_path, std::vector<unsigned char>* file_enc_key,
    SignAlgorithm* file_sign_algo) {
  assert(file_enc_key && file_sign_algo);

  std::string meta_file_name(
      "/.xtreemfs_enc_meta_files/"
          + boost::lexical_cast<std::string>(ExtractFileIdFromXCap(xcap)));
  FileHandle* meta_file;
  try {
    meta_file = volume_->OpenFile(user_credentials, meta_file_name,
                                  pbrpc::SYSTEM_V_FCNTL_H_O_RDWR, 0777);
    GetFileKeys(user_credentials, file_path, xcap.file_id(), file_enc_key,
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
    CreateAndSetNewLockbox(user_credentials, file_path, xcap.file_id(),
                           file_enc_key, file_sign_algo);
  }

  return meta_file;
}

/**
 * Get the encryption and sign key for a file
 *
 * @param user_credentials  Credentials of the user requesting the keys.
 * @param file_path         File path to the file.
 * @param file_id           Full XtreemFS File ID (Volume UUID and File ID).
 * @param[out] file_enc_key
 * @param[out] file_sign_algo
 *
 * @throws XtreemFSException
 */
void FileKeyDistribution::GetFileKeys(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const std::string& file_id,
    std::vector<unsigned char>* file_enc_key, SignAlgorithm* file_sign_algo) {
  assert(file_enc_key && file_sign_algo);

  SignAlgorithm owner_sig_algo(AsymKey(),
                               volume_->volume_options().encryption_hash);
  bool write_access;
  std::string key_id = GetAccessLockboxKeys(user_credentials, file_path,
                                            &owner_sig_algo, &write_access);

  // get asym encrypted lockbox encryption key
  std::string enc_lockbox_enc_key;
  volume_->GetXAttr(user_credentials, file_path,
                    "xtreemfs_.enc.lockbox_rw_enc_key." + key_id,
                    &enc_lockbox_enc_key);

  // get signed then encrypted lockbox
  std::string ste_lockbox_str;
  volume_->GetXAttr(
      user_credentials, file_path,
      write_access ? "xtreemfs_.enc.lockbox_rw" : "xtreemfs_.enc.lockbox_r",
      &ste_lockbox_str);
  SymEncBytes ste_lockbox;
  ste_lockbox.ParseFromString(ste_lockbox_str);

  // decrypt signed file lockbox
  std::vector<unsigned char> signed_lockbox_buffer;
  envelope_.Open(ste_lockbox.cipher(), key_storage_.GetPrivKey(key_id),
                 boost::asio::buffer(ste_lockbox.ciphertext()),
                 boost::asio::buffer(enc_lockbox_enc_key),
                 boost::asio::buffer(ste_lockbox.iv()), &signed_lockbox_buffer);

  // verify signature of file lockbox
  SignedFileLockbox signed_lockbox;
  signed_lockbox.ParseFromArray(signed_lockbox_buffer.data(),
                                signed_lockbox_buffer.size());
  owner_sig_algo.Verify(boost::asio::buffer(signed_lockbox.lockbox()),
                        boost::asio::buffer(signed_lockbox.signature()));

  // read lockbox
  FileLockbox lockbox;
  lockbox.ParseFromString(signed_lockbox.lockbox());
  if (lockbox.file_id() != file_id) {
    throw XtreemFSException("Wrong FileID in lockbox");
  }
  file_enc_key->assign(lockbox.enc_key().begin(), lockbox.enc_key().end());
  file_sign_algo->set_key(
      AsymKey(
          std::vector<unsigned char>(lockbox.sign_key().begin(),
                                     lockbox.sign_key().end())));
}

/**
 * Create and sets a new file lockbox for a newly created file.
 *
 * @param user_credentials  Owner of the file.
 * @param file_path         File path to the file.
 * @param file_id   Full XtreemFS File ID (Volume UUID and File ID).
 * @param[out] file_enc_key
 * @param[out] file_sign_algo
 */
void FileKeyDistribution::CreateAndSetNewLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const std::string& file_id,
    std::vector<unsigned char>* file_enc_key, SignAlgorithm* file_sign_algo) {
  assert(file_enc_key && file_sign_algo);

  // create read/write lockbox
  FileLockbox lockbox_rw;
  lockbox_rw.set_file_id(file_id);
  // TODO(plieser): random salt
  lockbox_rw.set_file_id_salt("00");
  lockbox_rw.set_cipher(volume_->volume_options().encryption_cipher);
  // generate and set file enc key
  Cipher cipher(volume_->volume_options().encryption_cipher);
  cipher.GenerateKey(file_enc_key);
  lockbox_rw.set_enc_key(file_enc_key->data(), file_enc_key->size());
  // generate and set file sign key
  AsymKey file_sign_key("RSA");
  if (file_sign_algo) {
    file_sign_algo->set_key(file_sign_key);
  }
  std::vector<unsigned char> encoded_file_sign_key = file_sign_key
      .GetDEREncodedKey();
  lockbox_rw.set_sign_key(encoded_file_sign_key.data(),
                          encoded_file_sign_key.size());

  // create read only lockbox
  FileLockbox lockbox_r(lockbox_rw);
  std::vector<unsigned char> encoded_pub_file_sign_key = file_sign_key
      .GetDEREncodedPubKey();
  lockbox_r.set_sign_key(encoded_pub_file_sign_key.data(),
                         encoded_pub_file_sign_key.size());

  SignAlgorithm owner_sig_algo(AsymKey(),
                               volume_->volume_options().encryption_hash);
  std::vector<std::string> key_ids_rw;
  std::vector<std::string> key_ids_r;
  GetSetLockboxKeys(user_credentials, file_path, &owner_sig_algo, &key_ids_rw,
                    &key_ids_r);

  SetLockbox(user_credentials, file_path, owner_sig_algo, key_ids_rw,
             key_storage_.GetPubKeys(key_ids_rw), lockbox_rw, true);
  SetLockbox(user_credentials, file_path, owner_sig_algo, key_ids_r,
             key_storage_.GetPubKeys(key_ids_r), lockbox_r, false);
}

/**
 * Encrypt then signs a given lockbox and then sets it for the given file.
 *
 * @param user_credentials  Owner of the file.
 * @param file_path         File path to the file the lockbox belongs.
 * @param sig_algo          Sign algorithm to sign lockbox with.
 * @param key_ids_          Key IDs of users/groups who should have access.
 * @param pub_enc_keys      Public keys of users/groups who should have access.
 * @param lockbox           The lockbox to set.
 * @param write_lockbox     True if it is a read/write lockbox
 */
void FileKeyDistribution::SetLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const SignAlgorithm& sig_algo,
    const std::vector<std::string>& key_ids,
    const std::vector<AsymKey>& pub_enc_keys, const pbrpc::FileLockbox& lockbox,
    bool write_lockbox) {
  assert(key_ids.size() == pub_enc_keys.size());
  if (key_ids.size() == 0) {
    return;
  }

  // sign file lockbox
  SignedFileLockbox signed_lockbox;
  signed_lockbox.set_lockbox(lockbox.SerializeAsString());
  std::vector<unsigned char> sig = sig_algo.Sign(
      boost::asio::buffer(signed_lockbox.lockbox()));
  signed_lockbox.set_signature(sig.data(), sig.size());

  // encrypt signed file lockbox
  std::vector<std::vector<unsigned char> > encrypted_keys;
  std::vector<unsigned char> iv;
  std::vector<unsigned char> ciphertext;
  envelope_.Seal(volume_->volume_options().encryption_cipher, pub_enc_keys,
                 boost::asio::buffer(signed_lockbox.SerializeAsString()),
                 &encrypted_keys, &iv, &ciphertext);

  // store signed then encrypted lockbox
  SymEncBytes ste_lockbox;
  ste_lockbox.set_cipher(volume_->volume_options().encryption_cipher);
  ste_lockbox.set_iv(iv.data(), iv.size());
  ste_lockbox.set_ciphertext(ciphertext.data(), ciphertext.size());
  volume_->SetXAttr(
      user_credentials, file_path,
      write_lockbox ? "xtreemfs_.enc.lockbox_rw" : "xtreemfs_.enc.lockbox_r",
      ste_lockbox.SerializeAsString(), static_cast<pbrpc::XATTR_FLAGS>(0));

  // store asym encrypted lockbox encryption keys
  for (int i = 0; i < key_ids.size(); i++) {
    volume_->SetXAttr(
        user_credentials, file_path,
        "xtreemfs_.enc.lockbox_rw_enc_key." + key_ids[i],
        std::string(encrypted_keys[i].begin(), encrypted_keys[i].end()),
        static_cast<pbrpc::XATTR_FLAGS>(0));
  }
}

/**
 * Get the necessary keys to decrypt and verify the file lockbox.
 *
 * @param user_credentials  Credentials of the user requesting the keys.
 * @param file_path         File path to the file the lockbox belongs.
 * @param[out] file_sign_algo   Sign algorithm to verify lockbox signature.
 * @param[out] write_access     True if user has write access.
 * @return  ID of the key the user is given access with.
 */
std::string FileKeyDistribution::GetAccessLockboxKeys(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, SignAlgorithm* file_sign_algo,
    bool* write_access) {
  assert(file_sign_algo && write_access);
  std::string key_id;

  // get file access right
  Stat stat;
  volume_->GetAttr(user_credentials, file_path, &stat);

  // get public key of lockbox signer (the owner)
  file_sign_algo->set_key(
      key_storage_.GetPubKey("user." + stat.user_id() + ".sign"));
  // get ID of the key the user is given access with
  if (stat.user_id() == user_credentials.username()) {
    // accessing user is the owner
    *write_access = true;
    key_id = "user." + stat.user_id() + ".enc";
  } else if (std::find(user_credentials.groups().begin(),
                       user_credentials.groups().end(), stat.group_id())
      != user_credentials.groups().end()) {
    // accessing user is in the owning group
    *write_access = stat.mode() & S_IWGRP;
    if (!*write_access && (stat.mode() & S_IWOTH)) {
      // others has more rights then group
      *write_access = true;
      key_id = "others.enc";
    }
    key_id = "group." + stat.group_id() + ".enc";
  } else {
    // access granted via others
    *write_access = stat.mode() & S_IWOTH;
    key_id = "others.enc";
  }
  return key_id;
}

/**
 * Get the necessary keys to encrypt and sign the file lockbox.
 *
 * @param user_credentials  Owner of the file.
 * @param file_path         File path to the file the lockbox belongs.
 * @param[out] file_sign_algo   Sign algorithm to sign lockbox.
 * @param[out] key_ids_rw   Key IDs of users/groups who have write access.
 * @param[out] key_ids_r    Key IDs of users/groups who have read only access.
 */
void FileKeyDistribution::GetSetLockboxKeys(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, SignAlgorithm* file_sign_algo,
    std::vector<std::string>* key_ids_rw, std::vector<std::string>* key_ids_r) {
  assert(file_sign_algo && key_ids_rw && key_ids_r);
  key_ids_rw->clear();
  key_ids_r->clear();

  // get file access right
  // TODO(plieser): access rights must be a trustable input
  Stat stat;
  volume_->GetAttr(user_credentials, file_path, &stat);

  // get private key of lockbox signer (the owner)
  file_sign_algo->set_key(
      key_storage_.GetPrivKey("user." + stat.user_id() + ".sign"));

  // owner has always write access
  key_ids_rw->push_back("user." + stat.user_id() + ".enc");
  if (stat.mode() & S_IWGRP) {
    // owner group has write access
    key_ids_rw->push_back("group." + stat.group_id() + ".enc");
  } else if (stat.mode() & S_IRGRP) {
    // owner group has read only access
    key_ids_r->push_back("group." + stat.group_id() + ".enc");
  }
  if (stat.mode() & S_IWOTH) {
    // others has write access
    key_ids_rw->push_back("group." + stat.group_id() + ".enc");
  } else if (stat.mode() & S_IROTH) {
    // others has read only access
    key_ids_r->push_back("group." + stat.group_id() + ".enc");
  }
}

}      // namespace xtreemfs
