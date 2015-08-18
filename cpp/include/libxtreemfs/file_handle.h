/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_H_
#define CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_H_

#include <stdint.h>

namespace xtreemfs {

namespace pbrpc {
class Lock;
class Stat;
class UserCredentials;
}  // namespace pbrpc

class FileHandle {
 public:
  virtual ~FileHandle() {}

  /** Read from a file 'count' bytes starting at 'offset' into 'buf'.
   *
   * @param buf[out]            Buffer to be filled with read data.
   * @param count               Number of requested bytes.
   * @param offset              Offset in bytes.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @return    Number of bytes read.
   */
  virtual int Read(
      char *buf,
      size_t count,
      int64_t offset) = 0;

  /** Write to a file 'count' bytes at file offset 'offset' from 'buf'.
   *
   * @attention     If asynchronous writes are enabled (which is the default
   *                unless the file was opened with O_SYNC or async writes
   *                were disabled globally), no possible write errors can be
   *                returned as Write() does return immediately after putting
   *                the write request into the send queue instead of waiting
   *                until the result was received.
   *                In this case, only after calling Flush() or Close() occurred
   *                write errors are returned to the user.
   *
   * @param buf[in]             Buffer which contains data to be written.
   * @param count               Number of bytes to be written from buf.
   * @param offset              Offset in bytes.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @return    Number of bytes written (see @attention above).
   */
  virtual int Write(
      const char *buf,
      size_t count,
      int64_t offset) = 0;

  /** Flushes pending writes and file size updates (corresponds to a fsync()
   *  system call).
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Flush() = 0;

  /** Truncates the file to "new_file_size_ bytes".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param new_file_size       New size of the file.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   **/
  virtual void Truncate(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      int64_t new_file_size) = 0;

  /** Retrieve the attributes of this file and writes the result in "stat".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param stat[out]           Pointer to Stat which will be overwritten.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      xtreemfs::pbrpc::Stat* stat) = 0;

  /** Sets a lock on the specified file region and returns the resulting Lock
   *  object.
   *
   * If the acquisition of the lock fails, PosixErrorException will be thrown
   * and posix_errno() will return POSIX_ERROR_EAGAIN.
   *
   * @param process_id      ID of the process to which the lock belongs.
   * @param offset          Start of the region to be locked in the file.
   * @param length          Length of the region.
   * @param exclusive       shared/read lock (false) or write/exclusive (true)?
   * @param wait_for_lock   if true, blocks until lock acquired.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::Lock* AcquireLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive,
      bool wait_for_lock) = 0;

  /** Checks if the requested lock does not result in conflicts. If true, the
   *  returned Lock object contains the requested 'process_id' in 'client_pid',
   *  otherwise the Lock object is a copy of the conflicting lock.
   *
   * @param process_id      ID of the process to which the lock belongs.
   * @param offset          Start of the region to be locked in the file.
   * @param length          Length of the region.
   * @param exclusive       shared/read lock (false) or write/exclusive (true)?
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::Lock* CheckLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive) = 0;

  /** Releases "lock".
   *
   * @param process_id      ID of the process to which the lock belongs.
   * @param offset          Start of the region to be locked in the file.
   * @param length          Length of the region.
   * @param exclusive       shared/read lock (false) or write/exclusive (true)?
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void ReleaseLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive) = 0;

  /** Releases "lock" (parameters given in Lock object).
   *
   * @param lock    Lock to be released.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void ReleaseLock(
      const xtreemfs::pbrpc::Lock& lock) = 0;

  /** Releases the lock possibly hold by "process_id". Use this before closing
   *  a file to ensure POSIX semantics:
   *
   * "All locks associated with a file for a given process shall be removed
   *  when a file descriptor for that file is closed by that process or the
   *  process holding that file descriptor terminates."
   *  (http://pubs.opengroup.org/onlinepubs/009695399/functions/fcntl.html)
   *
   * @param process_id  ID of the process whose lock shall be released.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void ReleaseLockOfProcess(int process_id) = 0;

  /** Triggers the replication of the replica on the OSD with the UUID
   *  "osd_uuid" if the replica is a full replica (and not a partial one).
   *
   * The Replica had to be added beforehand and "osd_uuid" has to be included
   * in the XlocSet of the file.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param osd_uuid    UUID of the OSD where the replica is located.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   * @throws UUIDNotInXlocSetException
   */
  virtual void PingReplica(
      const std::string& osd_uuid) = 0;

  /** Closes the open file handle (flushing any pending data).
   *
   * @attention The libxtreemfs implementation does NOT count the number of
   *            pending operations. Make sure that there're no pending
   *            operations on the FileHandle before you Close() it.
   *
   * @attention Please execute ReleaseLockOfProcess() first if there're multiple
   *            open file handles for the same file and you want to ensure the
   *            POSIX semantics that with the close of a file handle the lock
   *            (XtreemFS allows only one per tuple (client UUID, Process ID))
   *            of the process will be closed.
   *            If you do not care about this, you don't have to release any
   *            locks on your own as all locks will be automatically released if
   *            the last open file handle of a file will be closed.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws FileInfoNotFoundException
   * @throws FileHandleNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Close() = 0;
};

}  // namespace xtreemfs


#endif  // CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_H_
