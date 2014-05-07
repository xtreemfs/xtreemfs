/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/object_encryptor.h"

namespace xtreemfs {

/**
 * Creates an ObjectEncryptor
 *
 * @param fileInfo    Ownership is not transfered.
 * @param volume_options
 */
ObjectEncryptor::ObjectEncryptor(FileInfo* fileInfo,
                                 const Options& volume_options)
    : fileInfo(fileInfo),
      volume_options(volume_options) {
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
  return p.get_future();
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
  return reader(object_no, buffer, offset_in_object, bytes_to_read);
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
  return writer(object_no, buffer, offset_in_object, bytes_to_write);
}

}  // namespace xtreemfs
