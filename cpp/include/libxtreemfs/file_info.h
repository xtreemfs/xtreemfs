/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_FILE_INFO_H_
#define CPP_INCLUDE_LIBXTREEMFS_FILE_INFO_H_

#include <stdint.h>

#include <boost/optional.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <map>
#include <string>

#include "libxtreemfs/async_write_handler.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/uuid_container.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class FileHandleImplementation;
class VolumeImplementation;

namespace pbrpc {
class Lock;
class Stat;
class UserCredentials;
}  // namespace pbrpc

/** Different states regarding osd_write_response_ and its write back. */
enum FilesizeUpdateStatus {
  kClean, kDirty, kDirtyAndAsyncPending, kDirtyAndSyncPending
};

class FileInfo {
 public:
  FileInfo(ClientImplementation* client,
           VolumeImplementation* volume,
           uint64_t file_id,
           const std::string& path,
           bool replicate_on_close,
           const xtreemfs::pbrpc::XLocSet& xlocset,
           const std::string& client_uuid);
  ~FileInfo();

  /** Returns a new FileHandle object to which xcap belongs.
   *
   * @remark Ownership is transferred to the caller.
   */
  FileHandleImplementation* CreateFileHandle(const xtreemfs::pbrpc::XCap& xcap,
                                             bool async_writes_enabled);

  /** See CreateFileHandle(xcap). Does not add file_handle to list of open
   *  file handles if used_for_pending_filesize_update=true.
   *
   *  This function will be used if a FileHandle was solely created to
   *  asynchronously write back a dirty file size update (osd_write_response_).
   *
   * @remark Ownership is transferred to the caller.
   */
  FileHandleImplementation* CreateFileHandle(
      const xtreemfs::pbrpc::XCap& xcap,
      bool async_writes_enabled,
      bool used_for_pending_filesize_update);

  /** Deregisters a closed FileHandle. Called by FileHandle::Close(). */
  void CloseFileHandle(FileHandleImplementation* file_handle);

  /** Decreases the reference count and returns the current value. */
  int DecreaseReferenceCount();

  /** Copies osd_write_response_ into response if not NULL. */
  void GetOSDWriteResponse(xtreemfs::pbrpc::OSDWriteResponse* response);

  /** Writes path_ to path. */
  void GetPath(std::string* path);

  /** Changes path_ to new_path if path_ == path. */
  void RenamePath(const std::string& path, const std::string& new_path);

  /** Compares "response" against the current "osd_write_response_". Returns
   *  true if response is newer and assigns "response" to "osd_write_response_".
   *
   *  If successful, a new file handle will be created and xcap is required to
   *  send the osd_write_response to the MRC in the background.
   *
   *  @remark   Ownership of response is transferred to this object if this
   *            method returns true. */
  bool TryToUpdateOSDWriteResponse(xtreemfs::pbrpc::OSDWriteResponse* response,
                                   const xtreemfs::pbrpc::XCap& xcap);

  /** Merge into a possibly outdated Stat object (e.g. from the StatCache) the
   *  current file size and truncate_epoch from a stored OSDWriteResponse. */
  void MergeStatAndOSDWriteResponse(xtreemfs::pbrpc::Stat* stat);

  /** Sends pending file size updates to the MRC asynchronously. */
  void WriteBackFileSizeAsync(const RPCOptions& options);

  /** Renews xcap of all file handles of this file asynchronously. */
  void RenewXCapsAsync(const RPCOptions& options);

  /** Releases all locks of process_id using file_handle to issue
   *  ReleaseLock(). */
  void ReleaseLockOfProcess(FileHandleImplementation* file_handle,
                            int process_id);

  /** Uses file_handle to release all known local locks. */
  void ReleaseAllLocks(FileHandleImplementation* file_handle);

  /** Blocks until all asynchronous file size updates are completed. */
  void WaitForPendingFileSizeUpdates();

  /** Called by the file size update callback of FileHandle. */
  void AsyncFileSizeUpdateResponseHandler(
      const xtreemfs::pbrpc::OSDWriteResponse& owr,
      FileHandleImplementation* file_handle,
      bool success);

  /** Passes FileHandle::GetAttr() through to Volume. */
  void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      xtreemfs::pbrpc::Stat* stat);

  /** Compares "lock" against list of active locks.
   *
   *  Sets conflict_found to true and copies the conflicting, active lock into
   *  "conflicting_lock".
   *  If no conflict was found, "lock_for_pid_cached" is set to true if there
   *  exists already a lock for lock.client_pid(). Additionally,
   *  "cached_lock_for_pid_equal" will be set to true, lock is equal to the lock
   *  active for this pid. */
  void CheckLock(const xtreemfs::pbrpc::Lock& lock,
                 xtreemfs::pbrpc::Lock* conflicting_lock,
                 bool* lock_for_pid_cached,
                 bool* cached_lock_for_pid_equal,
                 bool* conflict_found);

  /** Returns true if a lock for "process_id" is known. */
  bool CheckIfProcessHasLocks(int process_id);

  /** Add a copy of "lock" to list of active locks. */
  void PutLock(const xtreemfs::pbrpc::Lock& lock);

  /** Remove locks equal to "lock" from list of active locks. */
  void DelLock(const xtreemfs::pbrpc::Lock& lock);

  /** Flushes pending async writes and file size updates. */
  void Flush(FileHandleImplementation* file_handle);

  /** Same as Flush(), takes special actions if called by FileHandle::Close().*/
  void Flush(FileHandleImplementation* file_handle, bool close_file);

  /** Flushes a pending file size update. */
  void FlushPendingFileSizeUpdate(FileHandleImplementation* file_handle);

  /** Calls async_write_handler_.Write().
   *
   * @remark Ownership of write_buffer is transferred to caller.
   */
  void AsyncWrite(AsyncWriteBuffer* write_buffer);

  /** Calls async_write_handler_.WaitForPendingWrites() (resulting in blocking
   *  until all pending async writes are finished).
   */
  void WaitForPendingAsyncWrites();

  /** Returns result of async_write_handler_.WaitForPendingWritesNonBlocking().
   *
   * @remark  Ownership is not transferred to the caller.
   */
  bool WaitForPendingAsyncWritesNonBlocking(
      boost::condition* condition_variable,
      bool* wait_completed,
      boost::mutex* wait_completed_mutex);


  void UpdateXLocSetAndRest(const xtreemfs::pbrpc::XLocSet& new_xlocset,
                                   bool replicate_on_close);

  void UpdateXLocSetAndRest(const xtreemfs::pbrpc::XLocSet& new_xlocset);

  /** Copies the XlocSet into new_xlocset. */
  void GetXLocSet(xtreemfs::pbrpc::XLocSet* new_xlocset);

  /** Copies the XlocSet into new_xlocset
   *  and returns the corresponding UUIDContainer.
   *  The UUIDcontainer is just valid for the associated XLocSet.
   */
  boost::shared_ptr<UUIDContainer> GetXLocSetAndUUIDContainer(
      xtreemfs::pbrpc::XLocSet* new_xlocset);

  /** Non-recursive scoped lock which is used to prevent concurrent XLocSet
   *  renewals from multiple FileHandles associated to the same FileInfo.
   *
   *  @see FileHandleImplementation::RenewXLocSet
   */
  class XLocSetRenewalLock {
    private:
      boost::mutex& m_;

    public:
      XLocSetRenewalLock(FileInfo* file_info) :
          m_(file_info->xlocset_renewal_mutex_) {
        m_.lock();
      }

      ~XLocSetRenewalLock() {
        m_.unlock();
      }
  };

 private:
  /** Same as FlushPendingFileSizeUpdate(), takes special actions if called by Close(). */
  void FlushPendingFileSizeUpdate(FileHandleImplementation* file_handle,
                                  bool close_file);

  /** See WaitForPendingFileSizeUpdates(). */
  void WaitForPendingFileSizeUpdatesHelper(boost::mutex::scoped_lock* lock);

  /** Reference to Client which did open this volume. */
  ClientImplementation* client_;

  /** Volume which did open this file. */
  VolumeImplementation* volume_;

  /** XtreemFS File ID of this file (does never change). */
  uint64_t file_id_;

  /** Path of the File, used for debug output and writing back the
   *  OSDWriteResponse to the MetadataCache. */
  std::string path_;

  /** Extracted from the FileHandle's XCap: true if an explicit close() has to
   *  be send to the MRC in order to trigger the on close replication. */
  bool replicate_on_close_;

  /** Number of file handles which hold a pointer on this object. */
  int reference_count_;

  /** Use this to protect reference_count_ and path_. */
  boost::mutex mutex_;

  /** List of corresponding OSDs. */
  xtreemfs::pbrpc::XLocSet xlocset_;

  /** UUIDIterator which contains the head OSD UUIDs of all replicas.
   *  It is used for non-striped files. */
  SimpleUUIDIterator osd_uuid_iterator_;

  /** This UUIDContainer contains all OSD UUIDs for all replicas and is
   *  constructed from the xlocset_ passed to this class on construction.
   *  It is used to construct a custom ContainerUUIDIterator on the fly when
   *  accessing striped files.
   *  It is managed by a smart pointer, because it has to outlast every
   *  ContainerUUIDIterator derived from it.
   * */
  boost::shared_ptr<UUIDContainer> osd_uuid_container_;

  /** Use this to protect xlocset_ and replicate_on_close_. */
  boost::mutex xlocset_mutex_;

  /** Use this to protect xlocset_ renewals. */
  boost::mutex xlocset_renewal_mutex_;

  /** List of active locks (acts as a cache). The OSD allows only one lock per
   *  (client UUID, PID) tuple. */
  std::map<unsigned int, xtreemfs::pbrpc::Lock*> active_locks_;

  /** Use this to protect active_locks_. */
  boost::mutex active_locks_mutex_;

  /** Random UUID of this client to distinguish them while locking. */
  const std::string& client_uuid_;

  /** List of open FileHandles for this file. */
  std::list<FileHandleImplementation*> open_file_handles_;

  /** Use this to protect open_file_handles_. */
  boost::mutex open_file_handles_mutex_;

  /** List of open FileHandles which solely exist to propagate a pending
   *  file size update (a OSDWriteResponse object) to the MRC.
   *
   * This extra list is needed to distinguish between the regular file handles
   * (see open_file_handles_) and the ones used for file size updates.
   * The intersection of both lists is empty.
   */
  std::list<FileHandleImplementation*> pending_filesize_updates_;

  /** Pending file size update after a write() operation, may be NULL.
   *
   * If osd_write_response_ != NULL, the file_size and truncate_epoch of the
   * referenced OSDWriteResponse have to be respected, e.g. when answering
   * a GetAttr request.
   * When all file handles to a file are closed, the information of the
   * stored osd_write_response_ will be merged back into the metadata cache.
   * This osd_write_response_ also corresponds to the "maximum" of all known
   * OSDWriteReponses. The maximum has the highest truncate_epoch, or if equal
   * compared to another response, the higher size_in_bytes value.
   */
  boost::scoped_ptr<xtreemfs::pbrpc::OSDWriteResponse> osd_write_response_;

  /** Denotes the state of the stored osd_write_response_ object. */
  FilesizeUpdateStatus osd_write_response_status_;

  /** XCap required to send an OSDWriteResponse to the MRC. */
  xtreemfs::pbrpc::XCap osd_write_response_xcap_;

  /** Always lock to access osd_write_response_, osd_write_response_status_,
   *  osd_write_response_xcap_ or pending_filesize_updates_. */
  boost::mutex osd_write_response_mutex_;

  /** Used by NotifyFileSizeUpdateCompletition() to notify waiting threads. */
  boost::condition osd_write_response_cond_;

  /** Proceeds async writes, handles the callbacks and provides a
   *  WaitForPendingWrites() method for barrier operations like read. */
  AsyncWriteHandler async_write_handler_;

  FRIEND_TEST(VolumeImplementationTestFastPeriodicFileSizeUpdate,
              WorkingPendingFileSizeUpdates);
  FRIEND_TEST(VolumeImplementationTest, FileSizeUpdateAfterFlush);
  FRIEND_TEST(VolumeImplementationTestFastPeriodicFileSizeUpdate,
              FileSizeUpdateAfterFlushWaitsForPendingUpdates);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingReleaseNonExistantLock);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingReleaseExistantLock);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingLastCloseReleasesAllLocks);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingReleaseLockOfProcess);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_FILE_INFO_H_
