/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/file_key_distribution.h"

#include <sys/stat.h>

#include <boost/scope_exit.hpp>
#include <string>
#include <vector>

#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "libxtreemfs/volume_implementation.h"
#include "util/crypto/cipher.h"

using xtreemfs::pbrpc::FileLockbox;
using xtreemfs::pbrpc::FileMetadata;
using xtreemfs::pbrpc::SETATTR_MODE;
using xtreemfs::pbrpc::SETATTR_UID;
using xtreemfs::pbrpc::SETATTR_GID;
using xtreemfs::pbrpc::SignedBytes;
using xtreemfs::pbrpc::Stat;
using xtreemfs::pbrpc::SymEncBytes;

namespace xtreemfs {

FileKeyDistribution::FileKeyDistribution(VolumeImplementation* volume)
    : key_storage_(volume->volume_options().encryption_pub_keys_path,
                   volume->volume_options().encryption_priv_keys_path,
                   volume->volume_options().ssl_pem_key_pass),
      volume_(volume) {
}

FileHandle* FileKeyDistribution::OpenMetaFile(
    const pbrpc::UserCredentials& user_credentials, const pbrpc::XCap& xcap,
    const std::string& file_path, uint32_t mode, pbrpc::FileLockbox* lockbox) {
  assert(lockbox);

  std::string meta_file_name(
      "/.xtreemfs_enc_meta_files/"
          + boost::lexical_cast<std::string>(ExtractFileIdFromXCap(xcap)));
  FileHandle* meta_file;
  bool created_meta_file = false;
  try {
    meta_file = volume_->OpenFile(user_credentials, meta_file_name,
                                  pbrpc::SYSTEM_V_FCNTL_H_O_RDWR, 0777);
  } catch (const PosixErrorException& e) {  // NOLINT
    // file didn't exist yet
    meta_file = volume_->OpenFile(
        user_credentials,
        meta_file_name,
        static_cast<pbrpc::SYSTEM_V_FCNTL>(pbrpc::SYSTEM_V_FCNTL_H_O_CREAT
            | pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
        0777);
    created_meta_file = true;
  }

  try {
    if (created_meta_file) {
      *lockbox = CreateAndSetMetadataAndLockbox(user_credentials, file_path,
                                                xcap.file_id(), mode);
    } else {
      *lockbox = GetFileKeys(user_credentials, file_path, xcap.file_id());
    }
  } catch (const std::exception& e) {  // NOLINT
    if (meta_file) {
      meta_file->Close();
    }
    throw;
  }

  return meta_file;
}

void FileKeyDistribution::ChangeAccessRights(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const xtreemfs::pbrpc::Stat& stat,
    xtreemfs::pbrpc::Setattrs to_set) {
  if (!((to_set & SETATTR_MODE) || (to_set & SETATTR_UID)
      || (to_set & SETATTR_GID))) {
    return;
  }

  if (to_set & SETATTR_UID) {
    LogAndThrowXtreemFSException(
        "Changing the owner of an encrypted file is not supported");
  }

  // get old signed meta data
  std::string signed_file_mdata_str;
  volume_->GetXAttr(user_credentials, file_path, "xtreemfs.enc.meta_data",
                    &signed_file_mdata_str);
  SignedBytes signed_file_mdata;
  signed_file_mdata.ParseFromString(signed_file_mdata_str);
  FileMetadata old_meta_data;
  old_meta_data.ParseFromString(signed_file_mdata.data());
  // verify signature
  AsymKey owner_sign_key = key_storage_.GetPubKey(
      "user." + old_meta_data.user_id() + ".sign");
  SignAlgorithm owner_sign_algo(owner_sign_key, signed_file_mdata.hash_algo());
  owner_sign_algo.Verify(boost::asio::buffer(signed_file_mdata.data()),
                         boost::asio::buffer(signed_file_mdata.signature()));
  // verify old metadata
  if (old_meta_data.user_id() != user_credentials.username()) {
    LogAndThrowXtreemFSException(
        "Only the file owner can change the access rights");
  }

  if ((to_set & SETATTR_GID)
      && std::find(user_credentials.groups().begin(),
                   user_credentials.groups().end(), stat.group_id())
          == user_credentials.groups().end()) {
    LogAndThrowXtreemFSException(
        "Changing the group owner to a group the user is not a member of is"
        "not supported");
  }

  // Generate new meta data.
  FileMetadata new_meta_data(old_meta_data);
  std::vector<unsigned char> salt = rand.Bytes(16);
  new_meta_data.set_salt(salt.data(), salt.size());
  if (to_set & SETATTR_MODE) {
    new_meta_data.set_mode(stat.mode());
  }
  if (to_set & SETATTR_GID) {
    new_meta_data.set_group_id(stat.group_id());
  }

  // create new read/write lockbox
  // TODO(plieser): delete old lockboxes
  FileLockbox new_lockbox_rw = GetLockbox(
      user_credentials, file_path,
      key_storage_.GetPubKey("user." + old_meta_data.user_id() + ".enc"),
      "user." + old_meta_data.user_id() + ".enc", true);
  new_lockbox_rw.set_salt(new_meta_data.salt());
  // generate and set new file enc key
  Cipher cipher(volume_->volume_options().encryption_cipher);
  std::vector<unsigned char> enc_key;
  cipher.GenerateKey(&enc_key);
  new_lockbox_rw.set_enc_key(enc_key.data(), enc_key.size());
  // generate and set new file sign key
  AsymKey file_sign_key("RSA");
  std::vector<unsigned char> encoded_file_sign_key = file_sign_key
      .GetDEREncodedKey();
  new_lockbox_rw.set_sign_key(encoded_file_sign_key.data(),
                              encoded_file_sign_key.size());

  // create new read only lockbox
  FileLockbox new_lockbox_r(new_lockbox_rw);
  std::vector<unsigned char> encoded_pub_file_sign_key = AsymKey(
      std::vector<unsigned char>(new_lockbox_rw.sign_key().begin(),
                                 new_lockbox_rw.sign_key().end()))
      .GetDEREncodedPubKey();
  new_lockbox_r.set_sign_key(encoded_pub_file_sign_key.data(),
                             encoded_pub_file_sign_key.size());

  // reencrypt file
  {
    FileHandleImplementation* file =
        reinterpret_cast<FileHandleImplementation*>(volume_->OpenFile(
            user_credentials, file_path, pbrpc::SYSTEM_V_FCNTL_H_O_RDWR));

    BOOST_SCOPE_EXIT((&file)) {
        file->Close();
      }
    BOOST_SCOPE_EXIT_END

    file->Reencrypt(user_credentials, enc_key, file_sign_key);
  }

  SetMetadataAndLockbox(user_credentials, file_path, new_meta_data,
                        new_lockbox_rw, new_lockbox_r);
}

/**
 * Get the encryption and sign key for a file
 *
 * @param user_credentials  Credentials of the user requesting the keys.
 * @param file_path         File path to the file.
 * @param file_id           Full XtreemFS File ID (Volume UUID and File ID).
 * @return The lockbox the user is given access with.
 *
 * @throws XtreemFSException
 */
pbrpc::FileLockbox FileKeyDistribution::GetFileKeys(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const std::string& file_id) {

  AsymKey owner_sign_key;
  bool write_access;
  std::string key_id = GetAccessLockboxKeys(user_credentials, file_path,
                                            &owner_sign_key, &write_access);

  // get lockbox
  FileLockbox lockbox = GetLockbox(user_credentials, file_path, owner_sign_key,
                                   key_id, write_access);
  if (lockbox.file_id() != file_id) {
    LogAndThrowXtreemFSException(
        "Wrong FileID in lockbox of file file_path. Expected: '" + file_id
            + "', contains '" + lockbox.file_id() + "'.");
  }

  return lockbox;
}

/**
 * Create and sets a new file lockbox for a file.
 *
 * @param user_credentials  Owner of the file.
 * @param file_path         Path to the file.
 * @param file_metadata     File metadata.
 * @return The lockbox the user is given access with.
 */
pbrpc::FileLockbox FileKeyDistribution::CreateAndSetMetadataAndLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const std::string& file_id, uint32_t mode) {
  // create new FileMetadata
  FileMetadata file_metadata;
  file_metadata.set_file_id(file_id);
  std::vector<unsigned char> file_id_salt = rand.Bytes(16);
  file_metadata.set_file_id_salt(file_id_salt.data(), file_id_salt.size());
  std::vector<unsigned char> salt = rand.Bytes(16);
  file_metadata.set_salt(salt.data(), salt.size());
  file_metadata.set_user_id(user_credentials.username());
  file_metadata.set_group_id(user_credentials.groups(0));
  file_metadata.set_mode(mode);

  // create read/write lockbox
  FileLockbox lockbox_rw;
  lockbox_rw.set_file_id(file_metadata.file_id());
  lockbox_rw.set_file_id_salt(file_metadata.file_id_salt());
  lockbox_rw.set_salt(file_metadata.salt());
  lockbox_rw.set_cipher(volume_->volume_options().encryption_cipher);
  lockbox_rw.set_block_size(volume_->volume_options().encryption_block_size);
  lockbox_rw.set_hash(volume_->volume_options().encryption_hash);
  // generate and set file enc key
  Cipher cipher(volume_->volume_options().encryption_cipher);
  std::vector<unsigned char> enc_key;
  cipher.GenerateKey(&enc_key);
  lockbox_rw.set_enc_key(enc_key.data(), enc_key.size());
  // generate and set file sign key
  AsymKey file_sign_key("RSA");
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

  SetMetadataAndLockbox(user_credentials, file_path, file_metadata, lockbox_rw,
                        lockbox_r);

  return lockbox_rw;
}

/**
 * Sets the signed metadata and lockboxes.
 *
 * @param user_credentials  Owner of the file.
 * @param file_path   Path to the file.
 * @param file_metadata   Metadata to sign.
 * @param lockbox_rw  Read/write lockbox.
 * @param lockbox_r   Read lockbox.
 */
void FileKeyDistribution::SetMetadataAndLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const pbrpc::FileMetadata& file_metadata,
    const pbrpc::FileLockbox& lockbox_rw, const pbrpc::FileLockbox& lockbox_r) {

  SignAlgorithm owner_sign_algo(AsymKey(),
                                volume_->volume_options().encryption_hash);
  std::vector<std::string> key_ids_rw;
  std::vector<std::string> key_ids_r;
  GetSetLockboxKeys(file_metadata, &owner_sign_algo, &key_ids_rw, &key_ids_r);

  SetLockbox(user_credentials, file_path, owner_sign_algo, key_ids_rw,
             key_storage_.GetPubKeys(key_ids_rw), lockbox_rw, true);
  SetLockbox(user_credentials, file_path, owner_sign_algo, key_ids_r,
             key_storage_.GetPubKeys(key_ids_r), lockbox_r, false);

  // sign FileMetadata
  SignedBytes signed_file_metadata;
  signed_file_metadata.set_data(file_metadata.SerializeAsString());
  signed_file_metadata.set_hash_algo(owner_sign_algo.get_hash_name());
  std::vector<unsigned char> sig = owner_sign_algo.Sign(
      boost::asio::buffer(signed_file_metadata.data()));
  signed_file_metadata.set_signature(sig.data(), sig.size());
  // set signed FileMetadata
  volume_->SetXAttr(user_credentials, file_path, "xtreemfs.enc.meta_data",
                    signed_file_metadata.SerializeAsString(),
                    static_cast<pbrpc::XATTR_FLAGS>(0));
}

/**
 * Gets the lockbox for a file.
 *
 * @param user_credentials
 * @param file_path   Path to the file.
 * @param lockbox_sign_key  The key the lockbox is signed with.
 * @param key_id  The id of the key with which the access is granted.
 * @param write_lockbox   True if the write lockbox should be retrieved.
 * @return The lockbox.
 */
pbrpc::FileLockbox FileKeyDistribution::GetLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const AsymKey& lockbox_sign_key,
    const std::string& key_id, bool write_lockbox) {
  // get asym encrypted lockbox encryption key
  std::string enc_lockbox_enc_key;
  volume_->GetXAttr(
      user_credentials,
      file_path,
      std::string("xtreemfs.enc.")
          + (write_lockbox ? "lockbox_rw_enc_key." : "lockbox_r_enc_key.")
          + key_id,
      &enc_lockbox_enc_key);
  assert(enc_lockbox_enc_key != "");

  // get signed then encrypted lockbox
  std::string ste_lockbox_str;
  volume_->GetXAttr(
      user_credentials, file_path,
      write_lockbox ? "xtreemfs.enc.lockbox_rw" : "xtreemfs.enc.lockbox_r",
      &ste_lockbox_str);
  SymEncBytes ste_lockbox;
  ste_lockbox.ParseFromString(ste_lockbox_str);
  assert(ste_lockbox.iv() != "");
  assert(ste_lockbox.ciphertext() != "");

  // decrypt signed file lockbox
  std::vector<unsigned char> signed_lockbox_buffer;
  envelope_.Open(ste_lockbox.cipher(), key_storage_.GetPrivKey(key_id),
                 boost::asio::buffer(ste_lockbox.ciphertext()),
                 boost::asio::buffer(enc_lockbox_enc_key),
                 boost::asio::buffer(ste_lockbox.iv()), &signed_lockbox_buffer);

  // verify signature of file lockbox
  SignedBytes signed_lockbox;
  signed_lockbox.ParseFromArray(signed_lockbox_buffer.data(),
                                signed_lockbox_buffer.size());
  SignAlgorithm owner_sign_algo(lockbox_sign_key, signed_lockbox.hash_algo());
  owner_sign_algo.Verify(boost::asio::buffer(signed_lockbox.data()),
                         boost::asio::buffer(signed_lockbox.signature()));

  // read lockbox
  FileLockbox lockbox;
  lockbox.ParseFromString(signed_lockbox.data());
  return lockbox;
}

/**
 * Encrypt then signs a given lockbox and then sets it for the given file.
 *
 * @param user_credentials  Owner of the file.
 * @param file_path         File path to the file the lockbox belongs.
 * @param sign_algo         Sign algorithm to sign lockbox with.
 * @param key_ids_          Key IDs of users/groups who should have access.
 * @param pub_enc_keys      Public keys of users/groups who should have access.
 * @param lockbox           The lockbox to set.
 * @param write_lockbox     True if it is a read/write lockbox
 */
void FileKeyDistribution::SetLockbox(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, const SignAlgorithm& sign_algo,
    const std::vector<std::string>& key_ids,
    const std::vector<AsymKey>& pub_enc_keys, const pbrpc::FileLockbox& lockbox,
    bool write_lockbox) {
  assert(key_ids.size() == pub_enc_keys.size());
  if (key_ids.size() == 0) {
    return;
  }

  // sign file lockbox
  SignedBytes signed_lockbox;
  signed_lockbox.set_data(lockbox.SerializeAsString());
  signed_lockbox.set_hash_algo(sign_algo.get_hash_name());
  std::vector<unsigned char> sig = sign_algo.Sign(
      boost::asio::buffer(signed_lockbox.data()));
  signed_lockbox.set_signature(sig.data(), sig.size());

  // encrypt signed file lockbox
  std::vector<std::vector<unsigned char> > encrypted_keys;
  std::vector<unsigned char> iv;
  std::vector<unsigned char> ciphertext;
  envelope_.Seal(volume_->volume_options().encryption_cipher, pub_enc_keys,
                 boost::asio::buffer(signed_lockbox.SerializeAsString()),
                 &encrypted_keys, &iv, &ciphertext);
  assert(iv.size() > 0);
  assert(ciphertext.size() > 0);

  // store signed then encrypted lockbox
  SymEncBytes ste_lockbox;
  ste_lockbox.set_cipher(volume_->volume_options().encryption_cipher);
  ste_lockbox.set_iv(iv.data(), iv.size());
  ste_lockbox.set_ciphertext(ciphertext.data(), ciphertext.size());
  volume_->SetXAttr(
      user_credentials, file_path,
      write_lockbox ? "xtreemfs.enc.lockbox_rw" : "xtreemfs.enc.lockbox_r",
      ste_lockbox.SerializeAsString(), static_cast<pbrpc::XATTR_FLAGS>(0));

  // store asym encrypted lockbox encryption keys
  for (int i = 0; i < key_ids.size(); i++) {
    volume_->SetXAttr(
        user_credentials,
        file_path,
        std::string("xtreemfs.enc.")
            + (write_lockbox ? "lockbox_rw_enc_key." : "lockbox_r_enc_key.")
            + key_ids[i],
        std::string(encrypted_keys[i].begin(), encrypted_keys[i].end()),
        static_cast<pbrpc::XATTR_FLAGS>(0));
  }
}

/**
 * Get the necessary keys to decrypt and verify the file lockbox.
 *
 * @param user_credentials  Credentials of the user requesting the keys.
 * @param file_path         File path to the file the lockbox belongs.
 * @param[out] lockbox_sign_key   Sign key to verify lockbox signature.
 * @param[out] write_access     True if user has write access.
 * @return  ID of the key the user is given access with.
 */
std::string FileKeyDistribution::GetAccessLockboxKeys(
    const pbrpc::UserCredentials& user_credentials,
    const std::string& file_path, AsymKey* lockbox_sign_key,
    bool* write_access) {
  assert(lockbox_sign_key && write_access);
  std::string key_id;

  // get file access right
  Stat stat;
  volume_->GetAttr(user_credentials, file_path, &stat);

  // get public key of lockbox signer (the owner)
  *lockbox_sign_key = key_storage_.GetPubKey(
      "user." + stat.user_id() + ".sign");
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
 * @param file_metadata   File metadata.
 * @param[out] file_sign_algo   Sign algorithm to sign lockbox.
 * @param[out] key_ids_rw   Key IDs of users/groups who have write access.
 * @param[out] key_ids_r    Key IDs of users/groups who have read only access.
 */
void FileKeyDistribution::GetSetLockboxKeys(
    const pbrpc::FileMetadata& file_metadata, SignAlgorithm* file_sign_algo,
    std::vector<std::string>* key_ids_rw, std::vector<std::string>* key_ids_r) {
  assert(file_sign_algo && key_ids_rw && key_ids_r);
  key_ids_rw->clear();
  key_ids_r->clear();

  // get private key of lockbox signer (the owner)
  file_sign_algo->set_key(
      key_storage_.GetPrivKey("user." + file_metadata.user_id() + ".sign"));

  // owner has always write access
  key_ids_rw->push_back("user." + file_metadata.user_id() + ".enc");
  if (file_metadata.mode() & S_IWGRP) {
    // owner group has write access
    key_ids_rw->push_back("group." + file_metadata.group_id() + ".enc");
  } else if (file_metadata.mode() & S_IRGRP) {
    // owner group has read only access
    key_ids_r->push_back("group." + file_metadata.group_id() + ".enc");
  }
  if (file_metadata.mode() & S_IWOTH) {
    // others has write access
    key_ids_rw->push_back("others.enc");
  } else if (file_metadata.mode() & S_IROTH) {
    // others has read only access
    key_ids_r->push_back("others.enc");
  }
}

}      // namespace xtreemfs
