/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_OBJECT_ENCRYPTOR_H_
#define CPP_INCLUDE_LIBXTREEMFS_OBJECT_ENCRYPTOR_H_

#include <boost/function.hpp>
#include <boost/thread/future.hpp>

#include "libxtreemfs/hash_tree_ad.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume_implementation.h"

namespace xtreemfs {

/**
 * These are injected functions that provide partial read and write
 * functionality for objects.
 */
typedef boost::function<
    boost::unique_future<int>(int object_no, char* buffer, int offset_in_object,
                              int bytes_to_read)> PartialObjectReaderFunction;
typedef boost::function<
    boost::unique_future<void>(int object_no, const char* buffer,
                               int offset_in_object, int bytes_to_write)> PartialObjectWriterFunction;
typedef boost::function<
    int(int object_no, char* buffer, int offset_in_object, int bytes_to_read)> PartialObjectReaderFunction_sync;
typedef boost::function<
    void(int object_no, const char* buffer, int offset_in_object,
         int bytes_to_write)> PartialObjectWriterFunction_sync;

/**
 * Encrypts/Decrypts an object.
 */
class ObjectEncryptor {
 public:
  ObjectEncryptor(const pbrpc::UserCredentials& user_credentials,
                  VolumeImplementation* volume, uint64_t file_id);

  boost::unique_future<int> Read(int object_no, char* buffer,
                                 int offset_in_object, int bytes_to_read,
                                 PartialObjectReaderFunction reader,
                                 PartialObjectWriterFunction writer);

  boost::unique_future<void> Write(int object_no, const char* buffer,
                                   int offset_in_object, int bytes_to_write,
                                   PartialObjectReaderFunction reader,
                                   PartialObjectWriterFunction writer);

  int Read_sync(int object_no, char* buffer, int offset_in_object,
                int bytes_to_read, PartialObjectReaderFunction_sync reader_sync,
                PartialObjectWriterFunction_sync writer_sync);

  void Write_sync(int object_no, const char* buffer, int offset_in_object,
                  int bytes_to_write,
                  PartialObjectReaderFunction_sync reader_sync,
                  PartialObjectWriterFunction_sync writer_sync);

 private:
  boost::unique_future<int> CallSyncReaderAsynchronously(
      PartialObjectReaderFunction_sync reader_sync, int object_no, char* buffer,
      int offset_in_object, int bytes_to_read);

  boost::unique_future<void> CallSyncWriterAsynchronously(
      PartialObjectWriterFunction_sync writer_sync, int object_no,
      const char* buffer, int offset_in_object, int bytes_to_write);

  HashTreeAD hash_tree_;

  /**
   * Pointer to the volume. Not owned by the class.
   */
  VolumeImplementation* volume_;

  /**
   * Reference to volume options. Not owned by class.
   */
  const Options& volume_options_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_OBJECT_ENCRYPTOR_H_