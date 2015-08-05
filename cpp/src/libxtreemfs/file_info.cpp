/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/file_info.h"

#include <boost/make_shared.hpp>

#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume_implementation.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/OSD.pb.h"

using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;
using namespace std;

namespace xtreemfs {

FileInfo::FileInfo(
    ClientImplementation* client,
    VolumeImplementation* volume,
    uint64_t file_id,
    const std::string& path,
    bool replicate_on_close,
    const xtreemfs::pbrpc::XLocSet& xlocset,
    const std::string& client_uuid)
    : client_(client),
      volume_(volume),
      file_id_(file_id),
      path_(path),
      replicate_on_close_(replicate_on_close),
      reference_count_(0),
      xlocset_(xlocset),
      osd_uuid_iterator_(xlocset),
      client_uuid_(client_uuid),
      osd_write_response_(NULL),
      osd_write_response_status_(kClean),
#ifdef _MSC_VER
// Disable "warning C4355: 'this' : used in base member initializer list".
// We can ignore that warning because we know that AsyncWriteHandler's
// constructor doesn't dereference the pointer passed to it.
#pragma warning(push)
#pragma warning(disable:4355)
#endif  // _MSC_VER
      async_write_handler_(this,
                           &osd_uuid_iterator_,
                           volume->uuid_resolver(),
                           volume->osd_service_client(),
                           volume->auth_bogus(),
                           volume->user_credentials_bogus(),
                           volume->volume_options(),
                           client->GetAsyncWriteCallbackQueue()) {
#ifdef _MSC_VER
#pragma warning(pop)
#endif  // _MSC_VER

  // Make an UUID container managed by a smart pointer.
  osd_uuid_container_ = boost::make_shared<UUIDContainer>(xlocset);
}

FileInfo::~FileInfo() {
  assert(active_locks_.size() == 0);
}

FileHandleImplementation* FileInfo::CreateFileHandle(
    const xtreemfs::pbrpc::XCap& xcap,
    bool async_writes_enabled) {
  return CreateFileHandle(xcap, async_writes_enabled, false);
}

FileHandleImplementation* FileInfo::CreateFileHandle(
    const xtreemfs::pbrpc::XCap& xcap,
    bool async_writes_enabled,
    bool used_for_pending_filesize_update) {
  FileHandleImplementation* file_handle = new FileHandleImplementation(
      client_,
      volume_->client_uuid(),
      this,
      xcap,
      volume_->mrc_uuid_iterator(),
      &osd_uuid_iterator_,
      volume_->uuid_resolver(),
      volume_->mrc_service_client(),
      volume_->osd_service_client(),
      volume_->stripe_translators(),
      async_writes_enabled,
      volume_->volume_options(),
      volume_->auth_bogus(),
      volume_->user_credentials_bogus());

  // Add file_handle to list.
  if (!used_for_pending_filesize_update) {
    boost::mutex::scoped_lock lock_refcount(mutex_);
    boost::mutex::scoped_lock lock_fhlist(open_file_handles_mutex_);

    ++reference_count_;
    open_file_handles_.push_back(file_handle);
  }

  return file_handle;
}

void FileInfo::CloseFileHandle(FileHandleImplementation* file_handle) {
  // Pending async writes and file size updates have already been flushed
  // by file_handle.

  // Remove file handle.
  {
    boost::mutex::scoped_lock lock_fhlist(open_file_handles_mutex_);

    open_file_handles_.remove(file_handle);
  }
  // Waiting does not require a lock on the open_file_handles_.
  file_handle->WaitForAsyncOperations();
  // Defer the deletion of file_handle as it might be needed by
  // VolumeImplementation::CloseFile() to release all locks.

  // At this point the file_handle is already removed from the list of open file
  // handles, but the reference_count is not decreased yet. This has to happen
  // after locking the open_file_table_ in Volume.
  volume_->CloseFile(file_id_, this, file_handle);
}

int FileInfo::DecreaseReferenceCount() {
  boost::mutex::scoped_lock lock(mutex_);
  --reference_count_;
  assert(reference_count_ >= 0);

  return reference_count_;
}

void FileInfo::MergeStatAndOSDWriteResponse(xtreemfs::pbrpc::Stat* stat) {
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  if (osd_write_response_.get()) {
    // Check if information in Stat is newer than osd_write_response_.
    if (stat->truncate_epoch() < osd_write_response_->truncate_epoch() ||
        (stat->truncate_epoch() == osd_write_response_->truncate_epoch()
         && stat->size() < osd_write_response_->size_in_bytes())) {
      // Information in Stat has to be merged with osd_write_response_.
      stat->set_size(osd_write_response_->size_in_bytes());
      stat->set_truncate_epoch(osd_write_response_->truncate_epoch());

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "getattr: merged infos from osd_write_response, size: "
            << stat->size() << endl;
      }
    }
  }
}

bool FileInfo::TryToUpdateOSDWriteResponse(
    xtreemfs::pbrpc::OSDWriteResponse* response,
    const xtreemfs::pbrpc::XCap& xcap) {
  assert(response);
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  // Determine the new maximum of osd_write_response_.
  if (CompareOSDWriteResponses(response, osd_write_response_.get()) == 1) {
    // Take over pointer.
    osd_write_response_.reset(response);
    osd_write_response_xcap_.CopyFrom(xcap);
    osd_write_response_status_ = kDirty;

    return true;
  } else {
    return false;
  }
}

void FileInfo::WriteBackFileSizeAsync(const RPCOptions& options) {
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  // Only update pending file size updates.
  if (osd_write_response_.get() && osd_write_response_status_ == kDirty) {
    FileHandleImplementation* file_handle =
        CreateFileHandle(osd_write_response_xcap_, false, true);
    pending_filesize_updates_.push_back(file_handle);
    osd_write_response_status_ = kDirtyAndAsyncPending;
    file_handle->set_osd_write_response_for_async_write_back(
        *(osd_write_response_.get()));
    file_handle->WriteBackFileSizeAsync(options);
  }
}

void FileInfo::RenewXCapsAsync(const RPCOptions& options) {
  boost::mutex::scoped_lock lock(open_file_handles_mutex_);

  for (list<FileHandleImplementation*>::iterator it =
           open_file_handles_.begin();
       it != open_file_handles_.end();
       ++it) {
    (*it)->ExecutePeriodTasks(options);
  }
}

void FileInfo::GetOSDWriteResponse(
    xtreemfs::pbrpc::OSDWriteResponse* response) {
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  if (osd_write_response_) {
    response->CopyFrom(*(osd_write_response_.get()));
  }
}

void FileInfo::GetPath(std::string* path) {
  boost::mutex::scoped_lock lock(mutex_);

  *path = path_;
}

void FileInfo::RenamePath(const std::string& path,
                          const std::string& new_path) {
  boost::mutex::scoped_lock lock(mutex_);

  if (path_ == path) {
    path_ = new_path;
  }
}

void FileInfo::WaitForPendingFileSizeUpdates() {
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  WaitForPendingFileSizeUpdatesHelper(&lock);
}

void FileInfo::WaitForPendingFileSizeUpdatesHelper(
    boost::mutex::scoped_lock* lock) {
  assert(lock->owns_lock());
  while (pending_filesize_updates_.size() > 0) {
    osd_write_response_cond_.wait(*lock);
  }
}

void FileInfo::AsyncFileSizeUpdateResponseHandler(
    const xtreemfs::pbrpc::OSDWriteResponse& owr,
    FileHandleImplementation* file_handle,
    bool success) {
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  // Only change the status if the OSDWriteResponse has not changed meanwhile.
  if (CompareOSDWriteResponses(&owr, osd_write_response_.get()) == 0) {
    // The status must not have changed.
    assert(osd_write_response_status_ == kDirtyAndAsyncPending);
    if (success) {
      osd_write_response_status_ = kClean;
    } else {
      osd_write_response_status_ = kDirty;  // Still dirty.
    }
  }

  // Always remove the temporary FileHandle.
  pending_filesize_updates_.remove(file_handle);
  delete file_handle;
  if (pending_filesize_updates_.size() == 0) {
    osd_write_response_cond_.notify_all();
  }
}

void FileInfo::GetAttr(const xtreemfs::pbrpc::UserCredentials& user_credentials,
                       xtreemfs::pbrpc::Stat* stat) {
  string path;
  GetPath(&path);
  volume_->GetAttr(user_credentials, path, false, stat, this);
}

void FileInfo::Flush(FileHandleImplementation* file_handle) {
  Flush(file_handle, false);
}

void FileInfo::Flush(FileHandleImplementation* file_handle, bool close_file) {
  // We don't wait only for file_handle's pending writes but for all writes of
  // this file.
  WaitForPendingAsyncWrites();

  FlushPendingFileSizeUpdate(file_handle, close_file);
}

void FileInfo::FlushPendingFileSizeUpdate(
    FileHandleImplementation* file_handle) {
  FlushPendingFileSizeUpdate(file_handle, false);
}

void FileInfo::FlushPendingFileSizeUpdate(FileHandleImplementation* file_handle,
                                          bool close_file) {
  // File size write back.
  boost::mutex::scoped_lock lock(osd_write_response_mutex_);

  bool no_response_sent = true;
  if (osd_write_response_.get()) {
    WaitForPendingFileSizeUpdatesHelper(&lock);
    if (osd_write_response_status_ == kDirty) {
      osd_write_response_status_ = kDirtyAndSyncPending;
      // Create a copy of OSDWriteResponse to pass to FileHandle.
      OSDWriteResponse response_copy(*(osd_write_response_.get()));
      lock.unlock();

      try {
        file_handle->WriteBackFileSize(response_copy, close_file);
      } catch (const XtreemFSException&) {
        osd_write_response_status_ = kDirty;
        throw;  // Rethrow error.
      }

      lock.lock();
      no_response_sent = false;
      // Only update the status if the response object has not changed
      // meanwhile.
      if (CompareOSDWriteResponses(osd_write_response_.get(),
                                   &response_copy) == 0) {
        osd_write_response_status_ = kClean;
      }
    }
  }

  if (no_response_sent && close_file && replicate_on_close_) {
    // Send an explicit close only if the on-close-replication should be
    // triggered. Use an empty OSDWriteResponse object therefore.
    OSDWriteResponse empty_osd_write_response;
    file_handle->WriteBackFileSize(empty_osd_write_response, close_file);
  }
}


void FileInfo::CheckLock(const xtreemfs::pbrpc::Lock& lock,
                         xtreemfs::pbrpc::Lock* conflicting_lock,
                         bool* lock_for_pid_cached,
                         bool* cached_lock_for_pid_equal,
                         bool* conflict_found) {
  assert(conflicting_lock);
  assert(lock_for_pid_cached);
  assert(cached_lock_for_pid_equal);
  assert(lock.client_uuid() == client_uuid_);

  boost::mutex::scoped_lock mutex_lock(active_locks_mutex_);

  *cached_lock_for_pid_equal = false;
  *conflict_found = false;
  *lock_for_pid_cached = false;

  for (map<unsigned int, Lock*>::iterator it = active_locks_.begin();
       it != active_locks_.end();
       ++it) {
    if (it->first == lock.client_pid()) {
      *lock_for_pid_cached = true;
      if (CheckIfLocksAreEqual(lock, *(it->second))) {
        *cached_lock_for_pid_equal = true;
      }
      continue;
    }

    if (CheckIfLocksDoConflict(lock, *(it->second))) {
      *conflict_found = true;
      conflicting_lock->CopyFrom(*(it->second));
      // A conflicting lock has a higher priority than a cached lock with the
      // same PID.
      break;
    }
  }
}

bool FileInfo::CheckIfProcessHasLocks(int process_id) {
  boost::mutex::scoped_lock mutex_lock(active_locks_mutex_);

  // There may be only up to one lock per process_id. No loop required.
  map<unsigned int, Lock*>::const_iterator it = active_locks_.find(process_id);
  return it != active_locks_.end();
}

void FileInfo::PutLock(const xtreemfs::pbrpc::Lock& lock) {
  assert(lock.client_uuid() == client_uuid_);

  boost::mutex::scoped_lock mutex_lock(active_locks_mutex_);

  map<unsigned int, Lock*>::iterator it = active_locks_.find(lock.client_pid());
  if (it != active_locks_.end()) {
    delete it->second;
    active_locks_.erase(it);
  }
  Lock* new_lock = new Lock(lock);
  active_locks_[lock.client_pid()] = new_lock;
}

void FileInfo::DelLock(const xtreemfs::pbrpc::Lock& lock) {
  assert(lock.client_uuid() == client_uuid_);
  boost::mutex::scoped_lock mutex_lock(active_locks_mutex_);

  map<unsigned int, Lock*>::iterator it = active_locks_.find(lock.client_pid());
  if (it != active_locks_.end()) {
    // Only up to one lock per PID. If its unlocked, just delete it.
    delete it->second;
    active_locks_.erase(it);
  }
}

void FileInfo::ReleaseLockOfProcess(FileHandleImplementation* file_handle,
                                    int process_id) {
  boost::mutex::scoped_lock mutex_lock(active_locks_mutex_);

  // There may be only up to one lock per process_id. No loop required.
  map<unsigned int, Lock*>::iterator it = active_locks_.find(process_id);
  if (it != active_locks_.end()) {
    Lock lock(*(it->second));
    // Leave critical section.
    mutex_lock.unlock();

    file_handle->ReleaseLock(lock);
  }
}

void FileInfo::ReleaseAllLocks(FileHandleImplementation* file_handle) {
  // Do not use pointers here to ensure the deletion of this list - otherwise
  // a ReleaseLock() may fail and the memory wont be freed.
  list<Lock> active_locks_copy;
  {
    // Create a copy to avoid longer locking periods and ensure that ReleaseLock
    // can delete the lock from active_locks_ without invalidating the iterator.
    boost::mutex::scoped_lock mutex_lock(active_locks_mutex_);

    for (map<unsigned int, Lock*>::iterator it = active_locks_.begin();
         it != active_locks_.end();
         ++it) {
      active_locks_copy.push_back((*(it->second)));
    }
  }

  for (list<Lock>::const_iterator it = active_locks_copy.begin();
       it != active_locks_copy.end();
       ++it) {
    // The lock itself will be deleted by ReleaseLock.
    file_handle->ReleaseLock(*it);
  }
}

void FileInfo::AsyncWrite(AsyncWriteBuffer* write_buffer) {
  async_write_handler_.Write(write_buffer);
}

void FileInfo::WaitForPendingAsyncWrites() {
  async_write_handler_.WaitForPendingWrites();
}

bool FileInfo::WaitForPendingAsyncWritesNonBlocking(
    boost::condition* condition_variable,
    bool* wait_completed,
    boost::mutex* wait_completed_mutex) {
  return async_write_handler_.
      WaitForPendingWritesNonBlocking(condition_variable,
                                      wait_completed,
                                      wait_completed_mutex);
}

void FileInfo::UpdateXLocSetAndRest(const xtreemfs::pbrpc::XLocSet& new_xlocset,
                                    bool replicate_on_close) {
  boost::mutex::scoped_lock lock(xlocset_mutex_);

  xlocset_.CopyFrom(new_xlocset);
  osd_uuid_iterator_.ClearAndGetOSDUUIDsFromXlocSet(new_xlocset);
  osd_uuid_container_ = boost::make_shared<UUIDContainer>(new_xlocset);

  replicate_on_close_ = replicate_on_close;
}

void FileInfo::UpdateXLocSetAndRest(
    const xtreemfs::pbrpc::XLocSet& new_xlocset) {
  boost::mutex::scoped_lock lock(xlocset_mutex_);

  xlocset_.CopyFrom(new_xlocset);
  osd_uuid_iterator_.ClearAndGetOSDUUIDsFromXlocSet(new_xlocset);
  osd_uuid_container_ = boost::make_shared<UUIDContainer>(new_xlocset);
}

void FileInfo::GetXLocSet(xtreemfs::pbrpc::XLocSet* new_xlocset) {
  assert(new_xlocset);
  boost::mutex::scoped_lock lock(xlocset_mutex_);
  new_xlocset->CopyFrom(xlocset_);
}

boost::shared_ptr<UUIDContainer> FileInfo::GetXLocSetAndUUIDContainer(xtreemfs::pbrpc::XLocSet* new_xlocset) {
  assert(new_xlocset);
  boost::mutex::scoped_lock lock(xlocset_mutex_);
  new_xlocset->CopyFrom(xlocset_);

  return osd_uuid_container_;
}

}  // namespace xtreemfs
