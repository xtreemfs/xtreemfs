/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/object_encryptor.h"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/random.hpp>
#include <boost/generator_iterator.hpp>
#include <algorithm>
#include <string>
#include <utility>
#include <vector>

#include "libxtreemfs/xtreemfs_exception.h"
#include "util/crypto/asym_key.h"
#include "util/crypto/base64.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/OSD.pb.h"

namespace xtreemfs {

namespace {

int RoundDown(int num, int multiple) {
  return num - num % multiple;
}

int RoundUp(int num, int multiple) {
  int rem = num % multiple;
  if (rem == 0)
    return num;
  return num + multiple - rem;
}

}  // anonymous namespace

/**
 * Creates an ObjectEncryptor
 *
 * @param user_credentials
 * @param volume    Ownership is not transfered.
 * @param file_id   XtreemFS File ID of the file to encrypt.
 * @param FileInfo  XtreemFS FileInfo of the file to encrypt.
 *                  Ownership is not transfered.
 * @param object_size   Object size in kB.
 */
ObjectEncryptor::ObjectEncryptor(const pbrpc::UserCredentials& user_credentials,
                                 VolumeImplementation* volume, uint64_t file_id,
                                 FileInfo* file_info, int object_size)
    : enc_block_size_(volume->volume_options().encryption_block_size),
      cipher_(volume->volume_options().encryption_cipher),
      sign_algo_(std::auto_ptr<AsymKey>(NULL),
                 volume->volume_options().encryption_hash),
      object_size_(object_size * 1024),
      file_info_(file_info),
      volume_(volume),
      volume_options_(volume->volume_options()) {
  assert(object_size_ >= enc_block_size_);
  assert(object_size_ % enc_block_size_ == 0);
  // TODO(plieser): file enc key
  std::string key_str = "01234567890123456789012345678901";
  file_enc_key_ = std::vector<unsigned char>(key_str.begin(), key_str.end());
  // TODO(plieser): file sign key
  std::string encoded_file_sign_key =
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
  std::auto_ptr<AsymKey> file_sign_key(
      new AsymKey(
          b64Encoder.Decode(boost::asio::buffer(encoded_file_sign_key))));
  sign_algo_.set_key(file_sign_key);

  std::string meta_file_name(
      "/.xtreemfs_enc_meta_files/" + boost::lexical_cast<std::string>(file_id));
  try {
    meta_file_ = volume->OpenFile(user_credentials, meta_file_name,
                                  pbrpc::SYSTEM_V_FCNTL_H_O_RDWR, 0777);
  } catch (const PosixErrorException& e) {  // NOLINT
    // file didn't exist yet
    try {
      meta_file_ = volume->OpenFile(
          user_credentials,
          meta_file_name,
          static_cast<pbrpc::SYSTEM_V_FCNTL>(pbrpc::SYSTEM_V_FCNTL_H_O_CREAT
              | pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
          0777);
    } catch (const PosixErrorException& e) {  // NOLINT
      // "/.xtreemfs_enc_meta_files" directory does not exist yet
      volume->MakeDirectory(user_credentials, "/.xtreemfs_enc_meta_files",
                            0777);
      meta_file_ = volume->OpenFile(
          user_credentials,
          meta_file_name,
          static_cast<pbrpc::SYSTEM_V_FCNTL>(pbrpc::SYSTEM_V_FCNTL_H_O_CREAT
              | pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
          0777);
    }
  }
}

ObjectEncryptor::~ObjectEncryptor() {
  if (meta_file_ != NULL) {
    meta_file_->Close();
  }
}

ObjectEncryptor::Operation::Operation(ObjectEncryptor* obj_enc, bool write)
    : obj_enc_(obj_enc),
      enc_block_size_(obj_enc->enc_block_size_),
      object_size_(obj_enc->object_size_),
      hash_tree_(obj_enc->meta_file_, &obj_enc->sign_algo_,
                 obj_enc->volume_options_, obj_enc->cipher_.iv_size()),
      old_file_size_(0) {
  if (obj_enc->volume_options_.encryption_cw == "serialize") {
    operation_lock_.reset(new FileLock(obj_enc_, 0, 0, write));
  }
}

ObjectEncryptor::ReadOperation::ReadOperation(ObjectEncryptor* obj_enc,
                                              int64_t offset, int count)
    : Operation(obj_enc, false) {
  if (count == 0) {
    return;
  }

  int start_block = offset / enc_block_size_;
  int end_block = (offset + count - 1) / enc_block_size_;

  if (obj_enc_->volume_options_.encryption_cw == "locks"
      || obj_enc_->volume_options_.encryption_cw == "snapshots") {
    // lock file
    file_lock_.reset(
        new FileLock(obj_enc_, start_block + 1, end_block - start_block + 1,
                     false));
  }
  boost::scoped_ptr<FileLock> meta_file_lock;
  if (obj_enc_->volume_options_.encryption_cw == "locks") {
    // lock meta file
    meta_file_lock.reset(new FileLock(obj_enc_, 0, 1, false));
  }

  hash_tree_.StartRead(start_block, end_block);
}

ObjectEncryptor::WriteOperation::WriteOperation(
    ObjectEncryptor* obj_enc, int64_t offset, int count,
    PartialObjectReaderFunction_sync reader_sync,
    PartialObjectWriterFunction_sync writer_sync)
    : Operation(obj_enc, true) {
  assert(count > 0);

  int start_block = offset / enc_block_size_;
  int end_block = (offset + count - 1) / enc_block_size_;
  int old_last_incomplete_enc_block;
  bool old_last_enc_block_complete = false;

  if (obj_enc->volume_options_.encryption_cw == "locks"
      || obj_enc_->volume_options_.encryption_cw == "snapshots") {
    // lock file
    file_lock_.reset(
        new FileLock(obj_enc_, start_block + 1, end_block - start_block + 1,
                     true));
  }

  while (true) {
    boost::scoped_ptr<FileLock> meta_file_lock;
    if (obj_enc->volume_options_.encryption_cw == "locks"
        || obj_enc_->volume_options_.encryption_cw == "snapshots") {
      // lock meta file
      meta_file_lock.reset(new FileLock(obj_enc_, 0, 1, false));
    }

    hash_tree_.Init();

    // increase file size if end of write is behind current file size
    old_file_size_ = hash_tree_.file_size();
    hash_tree_.set_file_size(std::max(old_file_size_, offset + count));

    old_last_incomplete_enc_block = old_file_size_ / enc_block_size_;
    if ((obj_enc->volume_options_.encryption_cw == "locks"
        || obj_enc_->volume_options_.encryption_cw == "snapshots")
        && old_last_incomplete_enc_block < start_block) {
      // The write is changing the file size but has not yet locked the old last
      // incomplete enc block.
      // To prevent a concurred write to change the file size too we must first
      // extend the file lock.
      try {
        file_lock_->Change(old_last_incomplete_enc_block + 1,
                           end_block - old_last_incomplete_enc_block + 1);
      } catch (const PosixErrorException& e) {  // NOLINT
        if (e.posix_errno() != pbrpc::POSIX_ERROR_EAGAIN) {
          // Only retry if there exists a conflicting lock and the server did
          // return an EAGAIN - otherwise rethrow the exception.
          throw;
        }
        // failed to lock required region, release meta file lock an try again
        continue;
      }
    }

    old_last_enc_block_complete = old_file_size_ % enc_block_size_ == 0;

    hash_tree_.StartWrite(
        start_block,
        offset % enc_block_size_ == 0,
        end_block,
        (offset + count) % enc_block_size_ == 0
            || (offset + count) >= old_file_size_,
        old_last_enc_block_complete);
    break;
  }

  if (old_last_incomplete_enc_block < start_block && !old_last_enc_block_complete) {
    // write is behind the old last enc block and it was incomplete,
    // so it musst be updated.
    int old_end_object_no = old_file_size_ / object_size_;
    int old_end_object_size = old_file_size_ % object_size_;
    Write_sync(old_end_object_no, NULL, old_end_object_size, 0, reader_sync,
               writer_sync);
  }
}

ObjectEncryptor::WriteOperation::~WriteOperation() {
  // TODO(plieser): catch exceptions
  boost::scoped_ptr<FileLock> meta_file_lock;
  if (obj_enc_->volume_options_.encryption_cw == "locks"
      || obj_enc_->volume_options_.encryption_cw == "snapshots") {
    // lock meta file
    meta_file_lock.reset(new FileLock(obj_enc_, 0, 1, true));
  }

  hash_tree_.FinishWrite();
}

ObjectEncryptor::TruncateOperation::TruncateOperation(
    ObjectEncryptor* obj_enc,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    int64_t new_file_size, PartialObjectReaderFunction_sync reader_sync,
    PartialObjectWriterFunction_sync writer_sync)
    : Operation(obj_enc, true) {
  // TODO(plieser): user_credentials needed?
  int new_end_enc_block_no;
  if (new_file_size > 0) {
    new_end_enc_block_no = (new_file_size - 1) / enc_block_size_;
  } else {
    new_end_enc_block_no = -1;
  }

  while (true) {
    boost::scoped_ptr<FileLock> meta_file_lock;
    if (obj_enc->volume_options_.encryption_cw == "locks"
        || obj_enc_->volume_options_.encryption_cw == "snapshots") {
      // lock meta file
      meta_file_lock.reset(new FileLock(obj_enc_, 0, 1, true));
    }

    hash_tree_.Init();

    int min_file_size = std::min(hash_tree_.file_size(), new_file_size);

    if (obj_enc->volume_options_.encryption_cw == "locks"
        || obj_enc_->volume_options_.encryption_cw == "snapshots") {
      // To prevent concurrent change of file size
      // lock the file from min_file_size to end.
      try {
        file_lock_.reset(
            new FileLock(obj_enc_, (min_file_size / enc_block_size_) + 1, 0,
                         true, false));
      } catch (const PosixErrorException& e) {  // NOLINT
        if (e.posix_errno() != pbrpc::POSIX_ERROR_EAGAIN) {
          // Only retry if there exists a conflicting lock and the server did
          // return an EAGAIN - otherwise rethrow the exception.
          throw;
        }
        // failed to lock required region, release meta file lock an try again
        continue;
      }
    }

    if (hash_tree_.file_size() == new_file_size) {
      // no truncation of hash tree needed.
      break;
    }

    bool min_end_enc_block_complete = min_file_size % enc_block_size_ == 0;

    hash_tree_.StartTruncate(new_end_enc_block_no, min_end_enc_block_complete);
    old_file_size_ = hash_tree_.file_size();
    hash_tree_.set_file_size(new_file_size);
    if (!min_end_enc_block_complete) {
      int min_end_object_no = (min_file_size - 1) / object_size_;
      int min_end_object_size = min_file_size % object_size_;
      Write_sync(min_end_object_no, NULL, min_end_object_size, 0, reader_sync,
                 writer_sync);
    }
    hash_tree_.FinishTruncate(user_credentials);
    break;
  }
}

/**
 * Read from an encrypted object synchronously.
 *
 * @param object_no
 * @param buffer    Ownership is not transfered.
 * @param offset_in_object
 * @param bytes_to_read
 * @param reader_sync   A synchronously reader for objects.
 * @param writer_sync   A synchronously writer for objects.
 * @return
 */
int ObjectEncryptor::Operation::Read_sync(
    int object_no, char* buffer, int offset_in_object, int bytes_to_read,
    PartialObjectReaderFunction_sync reader_sync,
    PartialObjectWriterFunction_sync writer_sync) {
  // TODO(plieser): writer_sync needed?
  // convert the sync reader/writer to async
  PartialObjectReaderFunction reader = boost::bind(
      &ObjectEncryptor::CallSyncReaderAsynchronously, reader_sync, _1, _2, _3,
      _4);
  PartialObjectWriterFunction writer = boost::bind(
      &ObjectEncryptor::CallSyncWriterAsynchronously, writer_sync, _1, _2, _3,
      _4);

  // call async read, wait for result and return it
  return Read(object_no, buffer, offset_in_object, bytes_to_read, reader,
              writer).get();
}

/**
 * Write from an encrypted object synchronously.
 *
 * @param object_no
 * @param buffer    Ownership is not transfered.
 * @param offset_in_object
 * @param bytes_to_write
 * @param reader_sync   A synchronously reader for objects.
 * @param writer_sync   A synchronously writer for objects.
 */
void ObjectEncryptor::Operation::Write_sync(
    int object_no, const char* buffer, int offset_in_object, int bytes_to_write,
    PartialObjectReaderFunction_sync reader_sync,
    PartialObjectWriterFunction_sync writer_sync) {
  // convert the sync reader/writer to async
  PartialObjectReaderFunction reader = boost::bind(
      &ObjectEncryptor::CallSyncReaderAsynchronously, reader_sync, _1, _2, _3,
      _4);
  PartialObjectWriterFunction writer = boost::bind(
      &ObjectEncryptor::CallSyncWriterAsynchronously, writer_sync, _1, _2, _3,
      _4);

  // call async write and wait until it is finished
  Write(object_no, buffer, offset_in_object, bytes_to_write, reader, writer)
      .wait();
  return;
}

/**
 * Calls a PartialObjectReaderFunction_sync and returns a future with the
 * result.
 *
 * @param reader_sync   A synchronously reader to be called.
 * @param object_no
 * @param buffer    Ownership is not transfered.
 * @param offset_in_object
 * @param bytes_to_read
 * @return
 */
boost::unique_future<int> ObjectEncryptor::CallSyncReaderAsynchronously(
    PartialObjectReaderFunction_sync reader_sync, int object_no, char* buffer,
    int offset_in_object, int bytes_to_read) {
  boost::promise<int> p;
  p.set_value(reader_sync(object_no, buffer, offset_in_object, bytes_to_read));
  return p.get_future();
}

/**
 * Calls a PartialObjectWriterFunction_sync and returns a future with the
 * result.
 *
 * @param writer_sync   A synchronously writer to be called.
 * @param object_no
 * @param buffer    Ownership is not transfered.
 * @param offset_in_object
 * @param bytes_to_write
 * @return
 */
boost::unique_future<void> ObjectEncryptor::CallSyncWriterAsynchronously(
    PartialObjectWriterFunction_sync writer_sync, int object_no,
    const char* buffer, int offset_in_object, int bytes_to_write) {
  writer_sync(object_no, buffer, offset_in_object, bytes_to_write);
  boost::promise<void> p;
  p.set_value();
  return p.get_future();
}

int ObjectEncryptor::Operation::EncryptEncBlock(
    int block_number, boost::asio::const_buffer plaintext,
    boost::asio::mutable_buffer ciphertext) {
  std::pair<std::vector<unsigned char>, int> encrypt_res = obj_enc_->cipher_
      .encrypt(plaintext, obj_enc_->file_enc_key_, ciphertext);
  std::vector<unsigned char>& iv = encrypt_res.first;
  int& ciphertext_len = encrypt_res.second;
  assert(ciphertext_len == boost::asio::buffer_size(plaintext));
  hash_tree_.SetLeaf(block_number, iv,
                     boost::asio::buffer(ciphertext, ciphertext_len));
  return ciphertext_len;
}

int ObjectEncryptor::Operation::DecryptEncBlock(
    int block_number, boost::asio::const_buffer ciphertext,
    boost::asio::mutable_buffer plaintext) {
  std::vector<unsigned char> iv = hash_tree_.GetLeaf(block_number, ciphertext);
  if (iv.size() == 0) {
    // block contains unencrypted 0 of a sparse file
    memset(boost::asio::buffer_cast<void*>(plaintext), 0,
           boost::asio::buffer_size(plaintext));
    return boost::asio::buffer_size(ciphertext);
  }
  int plaintext_len = obj_enc_->cipher_.decrypt(ciphertext,
                                                obj_enc_->file_enc_key_, iv,
                                                plaintext);
  assert(plaintext_len == boost::asio::buffer_size(ciphertext));
  return plaintext_len;
}

/**
 * Read from an encrypted object asynchronously.
 *
 * @param object_no
 * @param buffer    Ownership is not transfered.
 * @param offset_in_object
 * @param bytes_to_read
 * @param reader    An asynchronously reader for objects.
 * @param writer    An asynchronously writer for objects.
 * @return
 */
boost::unique_future<int> ObjectEncryptor::Operation::Read(
    int object_no, char* buffer, int offset_in_object, int bytes_to_read,
    PartialObjectReaderFunction reader, PartialObjectWriterFunction writer) {
  assert(bytes_to_read > 0);
  int object_offset = object_no * object_size_;
  if (hash_tree_.file_size() <= object_offset + offset_in_object) {
    // return if read start is behind file size
    boost::promise<int> promise;
    promise.set_value(0);
    return promise.get_future();
  }
  int ct_offset_in_object = RoundDown(offset_in_object, enc_block_size_);
  int ct_offset_diff = offset_in_object - ct_offset_in_object;
  // ciphertext bytes to read without regards to the file size
  int ct_bytes_to_read = RoundUp((offset_in_object + bytes_to_read),
                                 enc_block_size_) - ct_offset_in_object;

  std::vector<unsigned char> ciphertext(ct_bytes_to_read);
  boost::unique_future<int> bytes_read = reader(
      object_no, reinterpret_cast<char*>(ciphertext.data()),
      ct_offset_in_object, ct_bytes_to_read);
  // TODO(plieser): no wait
  bytes_read.wait();
  ciphertext.resize(bytes_read.get());

  int read_plaintext_len = 0;  // only to check correctness

  int ct_end_offset_in_object = ct_offset_in_object + bytes_read.get();
  int end_offset_in_object = std::min(ct_end_offset_in_object,
                                      offset_in_object + bytes_to_read);
  int ct_end_offset_diff = ct_end_offset_in_object - end_offset_in_object;
  int start_enc_block = (object_offset + ct_offset_in_object) / enc_block_size_;
  int end_enc_block = std::max(
      start_enc_block,
      (object_offset + ct_end_offset_in_object - 1) / enc_block_size_);
  int offset_end_enc_block = (end_enc_block - start_enc_block)
      * enc_block_size_;
  int buffer_offset = 0;
  int ciphertext_offset = 0;

  if (ct_offset_in_object != offset_in_object) {
    // first enc block is only partly read, handle it differently
    int ct_block_len = std::min(enc_block_size_, bytes_read.get());
    std::vector<unsigned char> tmp_pt_block(ct_block_len);
    boost::asio::const_buffer ct_block = boost::asio::buffer(ciphertext.data(),
                                                             ct_block_len);
    DecryptEncBlock(start_enc_block, ct_block,
                    boost::asio::buffer(tmp_pt_block));
    buffer_offset = std::min(
        static_cast<int>(tmp_pt_block.size()) - ct_offset_diff, bytes_to_read);
    std::copy(tmp_pt_block.begin() + ct_offset_diff,
              tmp_pt_block.begin() + ct_offset_diff + buffer_offset, buffer);
    read_plaintext_len += buffer_offset;

    // set start enc block, buffer_offset and ciphertext_offset for the rest
    start_enc_block++;
    ciphertext_offset += ct_block_len;
  }

  if (end_enc_block >= start_enc_block
      && (ct_end_offset_diff != 0 || ct_bytes_to_read != bytes_read.get())) {
    // last enc block is either not the same as the first or was not yet handled
    // and is only partly read
    int ct_block_len = bytes_read.get() - offset_end_enc_block;
    assert(ct_block_len > 0);
    assert(ct_block_len <= enc_block_size_);
    boost::asio::const_buffer ct_block = boost::asio::buffer(
        ciphertext.data() + offset_end_enc_block, ct_block_len);
    int buffer_offset_end_enc_block = buffer_offset
        + (end_enc_block - start_enc_block) * enc_block_size_;
    if (ct_end_offset_diff != 0) {
      std::vector<unsigned char> tmp_pt_block(ct_block_len);
      DecryptEncBlock(end_enc_block, ct_block,
                      boost::asio::buffer(tmp_pt_block));
      assert(
          bytes_to_read
              >= buffer_offset_end_enc_block + tmp_pt_block.size()
                  - ct_end_offset_diff);
      std::copy(tmp_pt_block.begin(), tmp_pt_block.end() - ct_end_offset_diff,
                buffer + buffer_offset_end_enc_block);
      read_plaintext_len += tmp_pt_block.size() - ct_end_offset_diff;
    } else {
      // last enc block is at the end of the file and incomplete
      assert(bytes_to_read >= buffer_offset_end_enc_block + ct_block_len);
      DecryptEncBlock(
          end_enc_block,
          ct_block,
          boost::asio::buffer(buffer + buffer_offset_end_enc_block,
                              ct_block_len));
      read_plaintext_len += ct_block_len;
    }

    // set end enc block for the rest
    end_enc_block--;
  }

  for (int i = start_enc_block; i <= end_enc_block; i++) {
    boost::asio::const_buffer ct_block = boost::asio::buffer(
        ciphertext.data() + ciphertext_offset, enc_block_size_);
    DecryptEncBlock(
        i, ct_block,
        boost::asio::buffer(buffer + buffer_offset, enc_block_size_));
    buffer_offset += enc_block_size_;
    ciphertext_offset += enc_block_size_;
    read_plaintext_len += enc_block_size_;
  }

  assert(buffer_offset <= bytes_to_read);
  assert(read_plaintext_len <= ciphertext.size());
  assert(read_plaintext_len <= bytes_to_read);
  assert(read_plaintext_len == end_offset_in_object - offset_in_object);
  boost::promise<int> promise;
  promise.set_value(end_offset_in_object - offset_in_object);
  return promise.get_future();
}

/**
 * Write from an encrypted object asynchronously.
 *
 * @param object_no
 * @param buffer    Ownership is not transfered.
 * @param offset_in_object
 * @param bytes_to_write
 * @param reader    An asynchronously reader for objects.
 * @param writer    An asynchronously writer for objects.
 * @return
 */
boost::unique_future<void> ObjectEncryptor::Operation::Write(
    int object_no, const char* buffer, int offset_in_object, int bytes_to_write,
    PartialObjectReaderFunction reader, PartialObjectWriterFunction writer) {
  int object_offset = object_no * object_size_;
  int ct_offset_in_object = RoundDown(offset_in_object, enc_block_size_);
  int ct_offset_diff = offset_in_object - ct_offset_in_object;
  // ciphertext bytes to write without regards to the file size
  int ct_bytes_to_write_ = RoundUp((offset_in_object + bytes_to_write),
                                   enc_block_size_) - ct_offset_in_object;
  int ct_bytes_to_write = std::min(
      ct_bytes_to_write_,
      static_cast<int>(hash_tree_.file_size() - object_offset
          - ct_offset_in_object));
  int ct_end_offset_in_object = ct_offset_in_object + ct_bytes_to_write;
  int end_offset_in_object = offset_in_object + bytes_to_write;  // ???
  int ct_end_offset_diff = ct_end_offset_in_object - end_offset_in_object;
  int start_enc_block = (object_offset + ct_offset_in_object) / enc_block_size_;
  int end_enc_block = (object_offset + ct_end_offset_in_object - 1)
      / enc_block_size_;
  int offset_end_enc_block = (end_enc_block - start_enc_block)
      * enc_block_size_;
  int buffer_offset = 0;
  int ciphertext_offset = 0;

  std::vector<unsigned char> ciphertext(ct_bytes_to_write);
  assert(ct_bytes_to_write > 0);

  if (ct_offset_diff != 0) {
    // first enc block is only partly written, handle it differently
    std::vector<unsigned char> new_pt_block(enc_block_size_);
    if (old_file_size_ > object_offset + ct_offset_in_object) {
      // 1. read the old enc block if it is not behind old file size
      std::vector<unsigned char> old_ct_block(enc_block_size_);
      boost::unique_future<int> bytes_read = reader(
          object_no, reinterpret_cast<char*>(old_ct_block.data()),
          ct_offset_in_object, enc_block_size_);
      // TODO(plieser): no wait
      bytes_read.wait();
      old_ct_block.resize(bytes_read.get());

      // 2. decrypt the old enc block
      DecryptEncBlock(start_enc_block, boost::asio::buffer(old_ct_block),
                      boost::asio::buffer(new_pt_block));
    }
    new_pt_block.resize(std::min(enc_block_size_, ct_bytes_to_write));

    // 3. get plaintext for new enc block by overwriting part of the old
    // plaintext
    buffer_offset = std::min(enc_block_size_ - ct_offset_diff, bytes_to_write);
    assert(buffer_offset <= bytes_to_write);
    assert(buffer_offset + ct_offset_diff <= new_pt_block.size());
    std::copy(buffer, buffer + buffer_offset,
              new_pt_block.begin() + ct_offset_diff);

    // 4. encrypt the new enc block and store the ciphertext in the write buffer
    EncryptEncBlock(start_enc_block, boost::asio::buffer(new_pt_block),
                    boost::asio::buffer(ciphertext));

    // set start enc block for the rest
    start_enc_block++;
    ciphertext_offset += new_pt_block.size();
  }

  if (end_enc_block >= start_enc_block
      && (ct_end_offset_diff != 0 || ct_bytes_to_write_ != ct_bytes_to_write)) {
    // last enc block is either not the same as the first or was not yet handled
    // and is only partly written
    int new_pt_block_len = ct_bytes_to_write - offset_end_enc_block;
    assert(new_pt_block_len > 0);
    assert(new_pt_block_len <= enc_block_size_);
    std::vector<unsigned char> new_pt_block(new_pt_block_len);
    if (old_file_size_ > object_offset + end_offset_in_object) {
      // 1. read the old enc block if it is not behind old file size or
      // is not getting completely overwritten
      std::vector<unsigned char> old_ct_block(enc_block_size_);
      boost::unique_future<int> bytes_read = reader(
          object_no, reinterpret_cast<char*>(old_ct_block.data()),
          ct_end_offset_in_object - new_pt_block_len, enc_block_size_);
      // TODO(plieser): no wait
      bytes_read.wait();
      old_ct_block.resize(bytes_read.get());

      // 2. decrypt the old enc block
      DecryptEncBlock(end_enc_block, boost::asio::buffer(old_ct_block),
                      boost::asio::buffer(new_pt_block));
    }

    // 3. get plaintext for new enc block by overwriting part of the old
    // plaintext
    int buffer_offset_end_enc_block = buffer_offset
        + (end_enc_block - start_enc_block) * enc_block_size_;
    std::copy(buffer + buffer_offset_end_enc_block, buffer + bytes_to_write,
              new_pt_block.begin());

    // 4. encrypt the new enc block and store the ciphertext in the write
    //    buffer
    EncryptEncBlock(
        end_enc_block,
        boost::asio::buffer(new_pt_block),
        boost::asio::buffer(
            ciphertext.data() + ct_bytes_to_write - new_pt_block.size(),
            new_pt_block.size()));

    // set end enc block for the rest
    end_enc_block--;
  }

  for (int i = start_enc_block; i <= end_enc_block; i++) {
    boost::asio::const_buffer pt_block = boost::asio::buffer(
        buffer + buffer_offset, enc_block_size_);
    EncryptEncBlock(
        i,
        pt_block,
        boost::asio::buffer(ciphertext.data() + ciphertext_offset,
                            enc_block_size_));
    buffer_offset += enc_block_size_;
    ciphertext_offset += enc_block_size_;
  }
  assert(buffer_offset <= bytes_to_write);

  return writer(object_no, reinterpret_cast<char*>(ciphertext.data()),
                ct_offset_in_object, ct_bytes_to_write);
}

/**
 * @param path  Path to file.
 * @return True if the file is an encryption meta file witch is not encrypted.
 */
bool ObjectEncryptor::IsEncMetaFile(const std::string& path) {
  return boost::starts_with(path, "/.xtreemfs_enc_meta_files/");
}

/**
 * Unlinks the encryption meta file of a file.
 *
 * @param user_credentials
 * @param volume    Ownership is not transfered.
 * @param file_id   XtreemFS File ID of the file.
 */
void ObjectEncryptor::Unlink(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    VolumeImplementation* volume, uint64_t file_id) {
  volume->Unlink(
      user_credentials,
      "/.xtreemfs_enc_meta_files/" + boost::lexical_cast<std::string>(file_id));
}

/**
 * Acquires a lock on the meta file.
 *
 * Range has the following semantic:
 *   [0, 1)  : Lock for the complete meta file
 *   [x, x+1): Lock for the encryption block x-1 of the file.
 */
ObjectEncryptor::FileLock::FileLock(ObjectEncryptor* obj_enc, uint64_t offset,
                                    uint64_t length, bool exclusive,
                                    bool wait_for_lock)
    : file_(obj_enc->meta_file_) {
  int process_id = obj_enc->file_info_->GenerateUniquePID();
  lock_.reset(
      file_->AcquireLock(process_id, offset, length, exclusive, wait_for_lock));
}

/**
 * Try's to change the range of the lock without waiting.
 *
 * @param offset  The new offset.
 * @param length  The new length.
 */
void ObjectEncryptor::FileLock::Change(uint64_t offset, uint64_t length) {
  lock_.reset(
      file_->AcquireLock(lock_->client_pid(), offset, length,
                         lock_->exclusive(), false));
}

ObjectEncryptor::FileLock::~FileLock() {
  file_->ReleaseLock(*lock_);
}

}  // namespace xtreemfs
