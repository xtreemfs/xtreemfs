/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/object_encryptor.h"

#include <boost/lexical_cast.hpp>
#include <algorithm>
#include <string>
#include <utility>
#include <vector>

#include "xtreemfs/GlobalTypes.pb.h"
#include "libxtreemfs/xtreemfs_exception.h"

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
      // TODO(plieser): files in one dir (creat if not exist yet)
      hash_tree_(cipher_.iv_size(), volume->volume_options()),
      object_size_(object_size * 1024),
      file_info_(file_info),
      old_file_size_(0),
      volume_(volume),
      volume_options_(volume->volume_options()) {
  assert(object_size_ >= enc_block_size_);
  assert(object_size_ % enc_block_size_ == 0);
  // TODO(plieser): file enc key
  std::string key_str = "01234567890123456789012345678901";
  file_enc_key_ = std::vector<unsigned char>(key_str.begin(), key_str.end());
  xtreemfs::pbrpc::Stat stat;
  file_info_->GetAttr(user_credentials, &stat);
  // TODO(plieser): file_size_ must be a trustable input (needed for read behind
  //                file size)
  //                if file_size_ can be trusted, is hash tree for empty file
  //                still needed?
  file_size_ = stat.size();

  std::string meta_file_name(
      "/.xtreemfs_enc_meta_files_" + boost::lexical_cast<std::string>(file_id));
  int max_leaf;
  if (file_size_ != 0) {
    max_leaf = (file_size_ - 1) / enc_block_size_;
  } else {
    max_leaf = -1;
  }
  FileHandle* meta_file;
  try {
    meta_file = volume->OpenFile(user_credentials, meta_file_name,
                                 pbrpc::SYSTEM_V_FCNTL_H_O_RDWR, 0777, true);
  } catch (const PosixErrorException& e) {  // NOLINT
    // file didn't exist yet
    max_leaf = -2;
    meta_file =
        volume->OpenFile(
            user_credentials,
            meta_file_name,
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(pbrpc::SYSTEM_V_FCNTL_H_O_CREAT
                | pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
            0777, true);
  }
  hash_tree_.Init(meta_file, max_leaf);
}

void ObjectEncryptor::StartRead(int64_t offset, int count) {
  if (offset >= file_size_ || count == 0) {
    return;
  }
  hash_tree_.StartRead(offset / enc_block_size_,
                       (offset + count - 1) / enc_block_size_);
}

void ObjectEncryptor::StartWrite(int64_t offset, int count,
                                 PartialObjectReaderFunction_sync reader_sync,
                                 PartialObjectWriterFunction_sync writer_sync) {
  assert(count > 0);

  // increase file size if end of write is behind current file size
  old_file_size_ = file_size_;
  file_size_ = std::max(file_size_, offset + count);

  hash_tree_.StartWrite(
      offset / enc_block_size_,
      offset % enc_block_size_ == 0,
      (offset + count - 1) / enc_block_size_,
      (offset + count) % enc_block_size_ == 0
          || (offset + count) >= old_file_size_,
      old_file_size_ % enc_block_size_ == 0);

  if (file_size_ > old_file_size_ && old_file_size_ % enc_block_size_ != 0
      && file_size_ / enc_block_size_ != old_file_size_ / enc_block_size_) {
    // write is behind the old last enc block and it was incomplete,
    // so it's hash must be updated
    int old_end_object_no = old_file_size_ / object_size_;
    int old_end_object_size = old_file_size_ % object_size_;
    Write_sync(old_end_object_no, NULL, old_end_object_size, 0, reader_sync,
               writer_sync);
  }
}

void ObjectEncryptor::FinishWrite() {
  hash_tree_.FinishWrite();
}

void ObjectEncryptor::Truncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    int64_t new_file_size, PartialObjectReaderFunction_sync reader_sync,
    PartialObjectWriterFunction_sync writer_sync) {
  int old_end_object_no = file_size_ / object_size_;
  int new_end_object_no = new_file_size / object_size_;
  int old_end_object_size = file_size_ % object_size_;
  int new_end_object_size = new_file_size % object_size_;
  int new_end_enc_block_no;
  if (new_file_size > 0) {
    new_end_enc_block_no = (new_file_size - 1) / enc_block_size_;
  } else {
    new_end_enc_block_no = -1;
  }

  old_file_size_ = file_size_;
  if (new_file_size > file_size_) {
    if (file_size_ % enc_block_size_ == 0) {
      hash_tree_.StartTruncate(new_end_enc_block_no, true);
      file_size_ = new_file_size;
      hash_tree_.FinishTruncate(user_credentials);
    } else {
      hash_tree_.StartTruncate(new_end_enc_block_no, false);
      file_size_ = new_file_size;
      Write_sync(old_end_object_no, NULL, old_end_object_size, 0, reader_sync,
                 writer_sync);
      hash_tree_.FinishTruncate(user_credentials);
    }
  } else if (new_file_size < file_size_) {
    if (new_file_size % enc_block_size_ == 0) {
      hash_tree_.StartTruncate(new_end_enc_block_no, true);
      file_size_ = new_file_size;
      hash_tree_.FinishTruncate(user_credentials);
    } else {
      hash_tree_.StartTruncate(new_end_enc_block_no, false);
      file_size_ = new_file_size;
      Write_sync(new_end_object_no, NULL, new_end_object_size, 0, reader_sync,
                 writer_sync);
      hash_tree_.FinishTruncate(user_credentials);
    }
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
int ObjectEncryptor::Read_sync(int object_no, char* buffer,
                               int offset_in_object, int bytes_to_read,
                               PartialObjectReaderFunction_sync reader_sync,
                               PartialObjectWriterFunction_sync writer_sync) {
  // convert the sync reader/writer to async
  PartialObjectReaderFunction reader = boost::bind(
      &ObjectEncryptor::CallSyncReaderAsynchronously, this, reader_sync, _1, _2,
      _3, _4);
  PartialObjectWriterFunction writer = boost::bind(
      &ObjectEncryptor::CallSyncWriterAsynchronously, this, writer_sync, _1, _2,
      _3, _4);

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
void ObjectEncryptor::Write_sync(int object_no, const char* buffer,
                                 int offset_in_object, int bytes_to_write,
                                 PartialObjectReaderFunction_sync reader_sync,
                                 PartialObjectWriterFunction_sync writer_sync) {
  // convert the sync reader/writer to async
  PartialObjectReaderFunction reader = boost::bind(
      &ObjectEncryptor::CallSyncReaderAsynchronously, this, reader_sync, _1, _2,
      _3, _4);
  PartialObjectWriterFunction writer = boost::bind(
      &ObjectEncryptor::CallSyncWriterAsynchronously, this, writer_sync, _1, _2,
      _3, _4);

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

int ObjectEncryptor::EncryptEncBlock(int block_number,
                                     boost::asio::const_buffer plaintext,
                                     boost::asio::mutable_buffer ciphertext) {
  std::pair<std::vector<unsigned char>, int> encrypt_res = cipher_.encrypt(
      plaintext, file_enc_key_, ciphertext);
  std::vector<unsigned char>& iv = encrypt_res.first;
  int& ciphertext_len = encrypt_res.second;
  assert(ciphertext_len == boost::asio::buffer_size(plaintext));
  hash_tree_.SetLeaf(block_number, iv,
                     boost::asio::buffer(ciphertext, ciphertext_len));
  return ciphertext_len;
}

int ObjectEncryptor::DecryptEncBlock(int block_number,
                                     boost::asio::const_buffer ciphertext,
                                     boost::asio::mutable_buffer plaintext) {
  std::vector<unsigned char> iv = hash_tree_.GetLeaf(block_number, ciphertext);
  if (iv.size() == 0) {
    // block contains unencrypted 0 of a sparse file
    memset(boost::asio::buffer_cast<void*>(plaintext), 0,
           boost::asio::buffer_size(plaintext));
    return boost::asio::buffer_size(ciphertext);
  }
  int plaintext_len = cipher_.decrypt(ciphertext, file_enc_key_, iv, plaintext);
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
boost::unique_future<int> ObjectEncryptor::Read(
    int object_no, char* buffer, int offset_in_object, int bytes_to_read,
    PartialObjectReaderFunction reader, PartialObjectWriterFunction writer) {
  assert(bytes_to_read > 0);
  int object_offset = object_no * object_size_;
  if (file_size_ <= object_offset + offset_in_object) {
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
//  return boost::make_ready_future(end_offset_in_object - offset_in_object);
//  return boost::make_future(end_offset_in_object - offset_in_object);
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
boost::unique_future<void> ObjectEncryptor::Write(
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
      static_cast<int>(file_size_ - object_offset - ct_offset_in_object));
  int ct_end_offset_in_object = ct_offset_in_object + ct_bytes_to_write;
  int end_offset_in_object = offset_in_object + bytes_to_write;  // ???
  int ct_end_offset_diff = ct_end_offset_in_object - end_offset_in_object;
  int start_enc_block = (object_offset + ct_offset_in_object) / enc_block_size_;
  int end_enc_block = std::max(
      start_enc_block,
      (object_offset + ct_end_offset_in_object - 1) / enc_block_size_);
  int offset_end_enc_block = (end_enc_block - start_enc_block)
      * enc_block_size_;
  int buffer_offset = 0;
  int ciphertext_offset = 0;

  std::vector<unsigned char> ciphertext(ct_bytes_to_write);

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

}  // namespace xtreemfs
