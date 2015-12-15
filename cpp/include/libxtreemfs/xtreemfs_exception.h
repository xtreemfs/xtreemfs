/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_XTREEMFS_EXCEPTION_H_
#define CPP_INCLUDE_LIBXTREEMFS_XTREEMFS_EXCEPTION_H_

#include <stdint.h>

#include <boost/lexical_cast.hpp>
#include <stdexcept>
#include <string>

#include "pbrpc/RPC.pb.h"

namespace xtreemfs {

class XtreemFSException : public std::runtime_error {
 public:
  explicit XtreemFSException(const std::string& msg)
    : std::runtime_error(msg) {}
};

class PosixErrorException : public XtreemFSException {
 public:
  PosixErrorException(xtreemfs::pbrpc::POSIXErrno posix_errno,
                      const std::string& msg)
    : XtreemFSException(msg),
      posix_errno_(posix_errno) {}
  xtreemfs::pbrpc::POSIXErrno posix_errno() const { return posix_errno_; }
 private:
  xtreemfs::pbrpc::POSIXErrno posix_errno_;
};

/** Will be thrown, if there was an IO_ERROR in the RPC Client on the client
 *  side. */
class IOException : public XtreemFSException {
 public:
  IOException() : XtreemFSException("IOError occurred.") {}
  explicit IOException(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Will be thrown if the server did return an INTERNAL_SERVER_ERROR. */
class InternalServerErrorException : public XtreemFSException {
 public:
  InternalServerErrorException()
   : XtreemFSException("Internal Server Error received.") {}
  explicit InternalServerErrorException(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Thrown if FileInfo for given file_id was not found in OpenFileTable.
 *
 * Every FileHandle does reference a FileInfo object where per-file properties
 * are stored. This exception should never occur as it means there was no
 * FileInfo for the FileHandle.
 */
class FileInfoNotFoundException : public XtreemFSException {
 public:
  explicit FileInfoNotFoundException(uint64_t file_id)
    : XtreemFSException("The FileInfo object was not found in the OpenFileTable"
        " for the FileId: " + boost::lexical_cast<std::string>(file_id)) {}
};

/** Thrown if FileHandle for given file_id was not found in FileHandleList. */
class FileHandleNotFoundException : public XtreemFSException {
 public:
  explicit FileHandleNotFoundException()
    : XtreemFSException("The FileHandle object was not found in the "
        "FileHandleList") {}
};

class AddressToUUIDNotFoundException : public XtreemFSException {
 public:
  explicit AddressToUUIDNotFoundException(const std::string& uuid)
    : XtreemFSException("Address for UUID not found: " + uuid) {}
};

class VolumeNotFoundException : public XtreemFSException {
 public:
  explicit VolumeNotFoundException(const std::string& volume_name)
    : XtreemFSException("Volume not found: " + volume_name) {}
};

class OpenFileHandlesLeftException : public XtreemFSException {
 public:
  OpenFileHandlesLeftException() : XtreemFSException("There are remaining open "
      "FileHandles which have to be closed first.") {}
};

/** Thrown if the DIR Service did return a AddressMapping which is not known. */
class UnknownAddressSchemeException : public XtreemFSException {
 public:
  explicit UnknownAddressSchemeException(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Thrown if a given UUID was not found in the xlocset of a file. */
class UUIDNotInXlocSetException : public XtreemFSException {
 public:
  explicit UUIDNotInXlocSetException(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Thrown if there was an operation on an UUIDIterator requested which could
 *  not be fulfilled, e.g. retrieve a UUID, because the list of UUIDs of the
 *  UUIDIterator was empty. */
class UUIDIteratorListIsEmpyException : public XtreemFSException {
 public:
  explicit UUIDIteratorListIsEmpyException(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Thrown if there was an empty replicas list in an XlocSet. */
class EmptyReplicaListInXlocSet : public XtreemFSException {
 public:
  explicit EmptyReplicaListInXlocSet(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Thrown if there was no head OSD for a replica listed in an XlocSet. */
class NoHeadOSDInXlocSet : public XtreemFSException {
 public:
  explicit NoHeadOSDInXlocSet(const std::string& msg)
    : XtreemFSException(msg) {}
};

/** Thrown if the given URL was not parsed correctly. */
class InvalidURLException : public XtreemFSException {
 public:
  explicit InvalidURLException(const std::string& msg)
    : XtreemFSException(msg) {}
};

class InvalidCommandLineParametersException : public XtreemFSException {
 public:
  explicit InvalidCommandLineParametersException(const std::string& msg)
    : XtreemFSException(msg) {}
};

class InvalidViewException : public XtreemFSException {
 public:
  explicit InvalidViewException(const std::string& msg)
    : XtreemFSException(msg) {}
};

class InsufficientVoucherException : public XtreemFSException {
 public:
  explicit InsufficientVoucherException(const std::string& msg)
    : XtreemFSException(msg) {}
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_XTREEMFS_EXCEPTION_H_
