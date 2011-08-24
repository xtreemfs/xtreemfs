/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/file_handle_implementation.h"

#include <boost/bind.hpp>
#include <map>
#include <string>
#include <vector>

#include "util/logging.h"

#include "libxtreemfs/async_write_buffer.h"
#include "libxtreemfs/callback/execute_sync_request.h"
#include "libxtreemfs/file_info.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/stripe_translator.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "xtreemfs/MRCServiceClient.h"
#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

/** Constructor called by FileInfo.CreateFileHandle().
 *
 * @remark The ownership of all parameters will not be transferred. For every
 *         opened FileHandle Close() has to be called.
 */
FileHandleImplementation::FileHandleImplementation(
    const std::string& client_uuid,
    FileInfo* file_info,
    const xtreemfs::pbrpc::XCap& xcap,
    UUIDIterator* mrc_uuid_iterator,
    UUIDIterator* osd_uuid_iterator,
    UUIDResolver* uuid_resolver,
    xtreemfs::pbrpc::MRCServiceClient* mrc_service_client,
    xtreemfs::pbrpc::OSDServiceClient* osd_service_client,
    const std::map<xtreemfs::pbrpc::StripingPolicyType,
                   StripeTranslator*>& stripe_translators,
    bool async_writes_enabled,
    const Options& options,
    const xtreemfs::pbrpc::Auth& auth_bogus,
    const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus)
    : client_uuid_(client_uuid),
      mrc_uuid_iterator_(mrc_uuid_iterator),
      osd_uuid_iterator_(osd_uuid_iterator),
      uuid_resolver_(uuid_resolver),
      file_info_(file_info),
      xcap_(xcap),
      xcap_renewal_pending_(false),
      osd_write_response_for_async_write_back_(NULL),
      mrc_service_client_(mrc_service_client),
      osd_service_client_(osd_service_client),
      stripe_translators_(stripe_translators),
      async_writes_enabled_(async_writes_enabled),
      async_writes_failed_(false),
      volume_options_(options),
      auth_bogus_(auth_bogus),
      user_credentials_bogus_(user_credentials_bogus) {
}

FileHandleImplementation::~FileHandleImplementation() {}

int FileHandleImplementation::Read(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    char *buf,
    size_t count,
    off_t offset) {
  file_info_->WaitForPendingAsyncWrites();

  // Prepare request object.
  readRequest rq;
  {
    boost::mutex::scoped_lock lock(mutex_);

    if (async_writes_failed_) {
      throw PosixErrorException(POSIX_ERROR_EIO, "A previous asynchronous write"
          " did fail. No more actions on this file handle are allowed.");
    }
    // TODO(mberlin): XCap might expire while retrying a request. Provide a
    //                mechanism to renew the xcap in the request.
    rq.mutable_file_credentials()->mutable_xcap()->CopyFrom(xcap_);
    rq.set_file_id(xcap_.file_id());
  }
  file_info_->GetXLocSet(rq.mutable_file_credentials()->mutable_xlocs());
  // Use a reference for shorter code.
  const XLocSet& xlocs = rq.file_credentials().xlocs();

  size_t received_data = 0;

  if (xlocs.replicas_size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
        << "no replica found for file:" << file_info_->path() << std::endl;
    throw PosixErrorException(
        POSIX_ERROR_EIO,
        "no replica found for file:" + file_info_->path());
  }
  // Pick the first replica to determine striping policy.
  // (We assume that all replicas use the same striping policy.)
  const StripingPolicy& striping_policy = xlocs.replicas(0).striping_policy();
  const StripeTranslator* translator =
      GetStripeTranslator(striping_policy.type());

  // Map offset to corresponding OSDs.
  std::vector<ReadOperation> operations;
  translator->TranslateReadRequest(buf, count, offset, striping_policy,
                                   &operations);

  UUIDIterator temp_uuid_iterator_for_striping;
  string osd_uuid = "";
  // Read all objects.
  for (size_t j = 0; j < operations.size(); j++) {
    rq.set_object_number(operations[j].obj_number);
    rq.set_object_version(0);
    rq.set_offset(operations[j].req_offset);
    rq.set_length(operations[j].req_size);

    // Differ between striping and the rest (replication, no replication).
    UUIDIterator* uuid_iterator;
    if (xlocs.replicas(0).osd_uuids_size() > 1) {
      // Replica is striped. Pick UUID from xlocset.
      osd_uuid = GetOSDUUIDFromXlocSet(xlocs,
                                       0,  // Use first and only replica.
                                       operations[j].osd_offset);
      temp_uuid_iterator_for_striping.ClearAndAddUUID(osd_uuid);
      uuid_iterator = &temp_uuid_iterator_for_striping;
    } else {
      // TODO(mberlin): Enhance UUIDIterator to read from different replicas.
      uuid_iterator = osd_uuid_iterator_;
    }

    // TODO(mberlin): Update xloc list if newer version found (on OSD?).
    boost::scoped_ptr< SyncCallback<ObjectData> > response(
        ExecuteSyncRequest< SyncCallback<ObjectData>* >(
            boost::bind(&xtreemfs::pbrpc::OSDServiceClient::read_sync,
                        osd_service_client_,
                        _1,
                        boost::cref(auth_bogus_),
                        boost::cref(user_credentials),
                        &rq),
            uuid_iterator,
            uuid_resolver_,
            volume_options_.max_read_tries,
            volume_options_));

    // Insert data into read-buffer
    int data_length = response->data_length();
    memcpy(operations[j].data, response->data(), data_length);
    // If zero_padding() > 0, the gap has to be filled with zeroes.
    memset(operations[j].data + data_length,
           0,
           response->response()->zero_padding());

    received_data += response->data_length() +
                     response->response()->zero_padding();
    response->DeleteBuffers();
  }

  return received_data;
}

int FileHandleImplementation::Write(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const char *buf,
    size_t count,
    off_t offset) {
  // Create copies of required data.
  FileCredentials file_credentials;
  {
    boost::mutex::scoped_lock lock(mutex_);

    if (async_writes_failed_) {
      assert(async_writes_enabled_);
      throw PosixErrorException(POSIX_ERROR_EIO, "A previous asynchronous write"
          " did fail. No further writes on this file handle are allowed.");
    }

    file_credentials.mutable_xcap()->CopyFrom(xcap_);
  }
  file_info_->GetXLocSet(file_credentials.mutable_xlocs());
  // Use references for shorter code.
  const string& global_file_id = file_credentials.xcap().file_id();
  const XLocSet& xlocs = file_credentials.xlocs();

  if (xlocs.replicas_size() == 0) {
    string error = "No replica found for file: " + file_info_->path();
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    throw PosixErrorException(POSIX_ERROR_EIO, error);
  }

  // Map operation to stripes.
  vector<WriteOperation> operations;
  const StripingPolicy& striping_policy = xlocs.replicas(0).striping_policy();
  const StripeTranslator* translator =
      GetStripeTranslator(striping_policy.type());
  translator->TranslateWriteRequest(buf,
                                    count,
                                    offset,
                                    striping_policy,
                                    &operations);

  if (async_writes_enabled_) {
    string osd_uuid = "";
    writeRequest* write_request = NULL;
    // Write all objects.
    for (size_t j = 0; j < operations.size(); j++) {
      write_request = new writeRequest();
      write_request->mutable_file_credentials()->CopyFrom(file_credentials);
      write_request->set_file_id(global_file_id);

      write_request->set_object_number(operations[j].obj_number);
      write_request->set_object_version(0);
      write_request->set_offset(operations[j].req_offset);
      write_request->set_lease_timeout(0);

      ObjectData* data = write_request->mutable_object_data();
      data->set_checksum(0);
      data->set_invalid_checksum_on_osd(false);
      data->set_zero_padding(0);

      // Create new WriteBuffer and differ between striping and the rest (
      // (replication = use UUIDIterator, no replication = set specific UUID).
      AsyncWriteBuffer* write_buffer;
      if (xlocs.replicas(0).osd_uuids_size() > 1) {
        // Replica is striped. Pick UUID from xlocset.
        write_buffer = new AsyncWriteBuffer(
            write_request,
            operations[j].data,
            operations[j].req_size,
            this,
            GetOSDUUIDFromXlocSet(xlocs,
                                  0,  // Use first and only replica.
                                  operations[j].osd_offset));
      } else {
        write_buffer = new AsyncWriteBuffer(write_request,
                                            operations[j].data,
                                            operations[j].req_size,
                                            this);
      }

      // TODO(mberlin): Currently the UserCredentials are ignored by the OSD and
      //                therefore we avoid copying them into write_buffer.
      file_info_->AsyncWrite(write_buffer);

      // Processing of file size updates is handled by the FileInfo's
      // AsyncWriteHandler.
    }
  } else {
    // Synchronous writes.
    UUIDIterator temp_uuid_iterator_for_striping;
    string osd_uuid = "";
    writeRequest write_request;
    write_request.mutable_file_credentials()->CopyFrom(file_credentials);
    write_request.set_file_id(global_file_id);
    // Write all objects.
    for (size_t j = 0; j < operations.size(); j++) {
      write_request.set_object_number(operations[j].obj_number);
      write_request.set_object_version(0);
      write_request.set_offset(operations[j].req_offset);
      write_request.set_lease_timeout(0);

      ObjectData *data = write_request.mutable_object_data();
      data->set_checksum(0);
      data->set_invalid_checksum_on_osd(false);
      data->set_zero_padding(0);

      // Differ between striping and the rest (replication, no replication).
      UUIDIterator* uuid_iterator;
      if (xlocs.replicas(0).osd_uuids_size() > 1) {
        // Replica is striped. Pick UUID from xlocset.
        osd_uuid = GetOSDUUIDFromXlocSet(xlocs,
                                         0,  // Use first and only replica.
                                         operations[j].osd_offset);
        temp_uuid_iterator_for_striping.ClearAndAddUUID(osd_uuid);
        uuid_iterator = &temp_uuid_iterator_for_striping;
      } else {
        // TODO(mberlin): Enhance UUIDIterator to read from different replicas.
        uuid_iterator = osd_uuid_iterator_;
      }

      boost::scoped_ptr< SyncCallback<OSDWriteResponse> > response(
          ExecuteSyncRequest< SyncCallback<OSDWriteResponse>* >(
              boost::bind(
                  &xtreemfs::pbrpc::OSDServiceClient::write_sync,
                  osd_service_client_,
                  _1,
                  boost::cref(auth_bogus_),
                  boost::cref(user_credentials),
                  &write_request,
                  operations[j].data,
                  operations[j].req_size),
              uuid_iterator,
              uuid_resolver_,
              volume_options_.max_write_tries,
              volume_options_));

      // If the filesize has changed, remember OSDWriteResponse for later file
      // size update towards the MRC (executed by
      // VolumeImplementation::PeriodicFileSizeUpdate).
      if (response->response()->has_size_in_bytes()) {
        if (file_info_->TryToUpdateOSDWriteResponse(response->response(),
                                                    xcap_)) {
          // Free everything except the response.
          delete response->data();
          delete response->error();
        } else {
          response->DeleteBuffers();
        }
      } else {
        response->DeleteBuffers();
      }
    }  // objects loop.
  }

  return count;
}

void FileHandleImplementation::Flush() {
  Flush(false);
}

void FileHandleImplementation::Flush(bool close_file) {
  file_info_->Flush(this, close_file);

  boost::mutex::scoped_lock lock(mutex_);
  if (async_writes_failed_) {
    string path;
    file_info_->GetPath(&path);
    string error = "Flush for file: " + path + " did not succeed flushing"
        " all pending writes as at least one asynchronous write did fail.";

    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
    throw PosixErrorException(POSIX_ERROR_EIO, error);
  }
}

void FileHandleImplementation::Truncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    off_t new_file_size) {
  file_info_->WaitForPendingAsyncWrites();

  XCap xcap_copy;
  {
    boost::mutex::scoped_lock lock(mutex_);

    if (async_writes_failed_) {
      throw PosixErrorException(POSIX_ERROR_EIO, "A previous asynchronous write"
          " did fail. No further actions on this file handle are allowed.");
    }
    xcap_copy.CopyFrom(xcap_);
  }

  // 1. Call truncate at the MRC (in order to increase the trunc epoch).
  boost::scoped_ptr< SyncCallback<XCap> > response(
    ExecuteSyncRequest< SyncCallback<XCap>* >(
        boost::bind(
            &xtreemfs::pbrpc::MRCServiceClient::ftruncate_sync,
            mrc_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &xcap_copy),
        mrc_uuid_iterator_,
        uuid_resolver_,
        volume_options_.max_tries,
        volume_options_));
  {
    boost::mutex::scoped_lock lock(mutex_);
    xcap_.CopyFrom(*(response->response()));
  }
  response->DeleteBuffers();

  TruncatePhaseTwoAndThree(user_credentials, new_file_size);
}

void FileHandleImplementation::TruncatePhaseTwoAndThree(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    off_t new_file_size) {
  // 2. Call truncate at the head OSD.
  truncateRequest truncate_rq;
  file_info_->GetXLocSet(
      truncate_rq.mutable_file_credentials()->mutable_xlocs());
  {
    boost::mutex::scoped_lock lock(mutex_);
    truncate_rq.mutable_file_credentials()->mutable_xcap()->CopyFrom(xcap_);
    truncate_rq.set_file_id(xcap_.file_id());
  }
  truncate_rq.set_new_file_size(new_file_size);

  boost::scoped_ptr< SyncCallback<OSDWriteResponse> > response(
      ExecuteSyncRequest< SyncCallback<OSDWriteResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::OSDServiceClient::truncate_sync,
              osd_service_client_,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &truncate_rq),
          osd_uuid_iterator_,
          uuid_resolver_,
          volume_options_.max_tries,
          volume_options_));

  assert(response->response()->has_size_in_bytes());
  // Free the rest of the msg.
  delete response->data();
  delete response->error();

  // Register the osd write response at this file's FileInfo.
  file_info_->TryToUpdateOSDWriteResponse(response->response(), xcap_);

  // 3. Update the file size at the MRC.
  file_info_->FlushPendingFileSizeUpdate(this);
}

void FileHandleImplementation::GetAttr(
     const xtreemfs::pbrpc::UserCredentials& user_credentials,
     xtreemfs::pbrpc::Stat* stat) {
  file_info_->GetAttr(user_credentials, stat);
}

xtreemfs::pbrpc::Lock* FileHandleImplementation::AcquireLock(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    int process_id,
    boost::uint64_t offset,
    boost::uint64_t length,
    bool exclusive,
    bool wait_for_lock) {
  // Create lockRequest object for the acquire lock request.
  lockRequest lock_request;
  lock_request.mutable_lock_request()->set_client_uuid(client_uuid_);
  lock_request.mutable_lock_request()->set_client_pid(process_id);
  lock_request.mutable_lock_request()->set_offset(offset);
  lock_request.mutable_lock_request()->set_length(length);
  lock_request.mutable_lock_request()->set_exclusive(exclusive);

  // Check active locks first.
  Lock* conflicting_lock = new Lock();
  bool lock_for_pid_cached, cached_lock_for_pid_equal, conflict_found;
  file_info_->CheckLock(lock_request.lock_request(),
                        conflicting_lock,
                        &lock_for_pid_cached,
                        &cached_lock_for_pid_equal,
                        &conflict_found);
  if (conflict_found) {
    delete conflicting_lock;
    throw PosixErrorException(POSIX_ERROR_EAGAIN, "conflicting lock");
  }
  // We allow only one lock per PID, i.e. an existing lock can be always
  // overwritten. In consequence, AcquireLock always has to be executed except
  // the new lock is equal to the current lock.
  if (cached_lock_for_pid_equal) {
    // Reuse memory of conflicting_lock.
    conflicting_lock->CopyFrom(lock_request.lock_request());
    return conflicting_lock;
  }

  // Cache could not be used. Complete lockRequest and send to OSD.
  delete conflicting_lock;
  file_info_->GetXLocSet(
      lock_request.mutable_file_credentials()->mutable_xlocs());
  {
    boost::mutex::scoped_lock lock(mutex_);
    lock_request.mutable_file_credentials()->mutable_xcap()
        ->CopyFrom(xcap_);
  }

  boost::scoped_ptr< SyncCallback<Lock> > response;
  if (!wait_for_lock) {
    response.reset(ExecuteSyncRequest< SyncCallback<Lock>* >(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_acquire_sync,
            osd_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &lock_request),
        osd_uuid_iterator_,
        uuid_resolver_,
        volume_options_.max_tries,
        volume_options_));
  } else {
    // Retry to obtain the lock in case of EAGAIN responses.
    int retries_left = volume_options_.max_tries;
    while (retries_left == 0 || retries_left--) {
      try {
        response.reset(ExecuteSyncRequest< SyncCallback<Lock>* >(
            boost::bind(
                &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_acquire_sync,
                osd_service_client_,
                _1,
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &lock_request),
            osd_uuid_iterator_,
            uuid_resolver_,
            1,
            volume_options_,
            false,  // UUIDIterator contains UUIDs and not addresses.
            true));  // true means to delay this attempt in case of errors.
        break;  // If successful, do not retry again.
      } catch(const PosixErrorException& e) {
        if (e.posix_errno() != POSIX_ERROR_EAGAIN) {
          // Only retry if there exists a conflicting lock and the server did
          // return an EAGAIN - otherwise rethrow the exception.
          response->DeleteBuffers();
          throw;
        }
      }
    }
  }
  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  // "Cache" new lock.
  file_info_->PutLock(*(response->response()));

  return response->response();
}

xtreemfs::pbrpc::Lock* FileHandleImplementation::CheckLock(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    int process_id,
    boost::uint64_t offset,
    boost::uint64_t length,
    bool exclusive) {
  // Create lockRequest object for the check lock request.
  lockRequest lock_request;
  lock_request.mutable_lock_request()->set_client_uuid(client_uuid_);
  lock_request.mutable_lock_request()->set_client_pid(process_id);
  lock_request.mutable_lock_request()->set_offset(offset);
  lock_request.mutable_lock_request()->set_length(length);
  lock_request.mutable_lock_request()->set_exclusive(exclusive);

  // Check active locks first.
  Lock* conflicting_lock = new Lock();
  bool lock_for_pid_cached, cached_lock_for_pid_equal, conflict_found;
  file_info_->CheckLock(lock_request.lock_request(),
                        conflicting_lock,
                        &lock_for_pid_cached,
                        &cached_lock_for_pid_equal,
                        &conflict_found);
  if (conflict_found) {
    return conflicting_lock;
  }
  // We allow only one lock per PID, i.e. an existing lock can be always
  // overwritten.
  if (lock_for_pid_cached) {
    conflicting_lock->CopyFrom(lock_request.lock_request());
    return conflicting_lock;
  }

  // Cache could not be used. Complete lockRequest and send to OSD.
  delete conflicting_lock;
  file_info_->GetXLocSet(
      lock_request.mutable_file_credentials()->mutable_xlocs());
  {
    boost::mutex::scoped_lock lock(mutex_);
    lock_request.mutable_file_credentials()->mutable_xcap()
        ->CopyFrom(xcap_);
  }

  boost::scoped_ptr< SyncCallback<Lock> > response(
    ExecuteSyncRequest< SyncCallback<Lock>* >(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_check_sync,
            osd_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &lock_request),
        osd_uuid_iterator_,
        uuid_resolver_,
        volume_options_.max_tries,
        volume_options_));
  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  return response->response();
}

void FileHandleImplementation::ReleaseLock(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    int process_id,
    boost::uint64_t offset,
    boost::uint64_t length,
    bool exclusive) {
  Lock lock;
  lock.set_client_uuid(client_uuid_);
  lock.set_client_pid(process_id);
  lock.set_offset(offset);
  lock.set_length(length);
  lock.set_exclusive(exclusive);
  ReleaseLock(user_credentials, lock);
}

void FileHandleImplementation::ReleaseLock(const xtreemfs::pbrpc::Lock& lock) {
  ReleaseLock(user_credentials_bogus_, lock);
}

void FileHandleImplementation::ReleaseLock(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const xtreemfs::pbrpc::Lock& lock) {
  // Only release locks which are known to this client.
  if (!file_info_->CheckIfProcessHasLocks(lock.client_pid())) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "FileHandleImplementation::ReleaseLock: Skipping unlock request "
             "as there is no lock known for the PID: " << lock.client_pid()
          << " (Lock description: " << lock.offset() << ", " << lock.length()
          << ", " << lock.exclusive() << ")" << endl;
    }
    return;
  }

  lockRequest unlock_request;
  file_info_->GetXLocSet(
      unlock_request.mutable_file_credentials()->mutable_xlocs());
  {
    boost::mutex::scoped_lock lock(mutex_);
    unlock_request.mutable_file_credentials()->mutable_xcap()
        ->CopyFrom(xcap_);
  }
  unlock_request.mutable_lock_request()->CopyFrom(lock);

  boost::scoped_ptr< SyncCallback<emptyResponse> > response(
    ExecuteSyncRequest< SyncCallback<emptyResponse>* >(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_release_sync,
            osd_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &unlock_request),
        osd_uuid_iterator_,
        uuid_resolver_,
        volume_options_.max_tries,
        volume_options_));
  response->DeleteBuffers();

  file_info_->DelLock(lock);
}

void FileHandleImplementation::ReleaseLockOfProcess(int process_id) {
  file_info_->ReleaseLockOfProcess(this, process_id);
}

void FileHandleImplementation::PingReplica(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& osd_uuid) {
  // Get xlocset. and check if osd_uuid is included.
  readRequest read_request;
  file_info_->GetXLocSet(
      read_request.mutable_file_credentials()->mutable_xlocs());
  const XLocSet& xlocs = read_request.file_credentials().xlocs();

  // Check if osd_uuid is part of the xlocset.
  if (xlocs.replicas_size() == 0) {
    throw UUIDNotInXlocSetException("The XlocSet contains no replicas.");
  }
  bool uuid_found = false;
  for (int i = 0; i < xlocs.replicas_size(); i++) {
    // Always check only the head OSD.
    if (xlocs.replicas(i).osd_uuids(0) == osd_uuid) {
      uuid_found = true;
      // Check replication flags, if it's a full replica.
      if (!(xlocs.replicas(i).replication_flags() & REPL_FLAG_FULL_REPLICA)) {
        // Nothing to do here because the replication does not need to be
        // triggered for partial replicas.
        return;
      }
      break;
    }
  }
  if (!uuid_found) {
    throw UUIDNotInXlocSetException("UUID: " + osd_uuid + " not found in the "
        "xlocset: " + xlocs.DebugString());
  }

  // Read one byte from the replica to trigger the replication.
  read_request.set_object_number(0);
  read_request.set_object_version(0);
  read_request.set_offset(0);
  read_request.set_length(1);  // 1 Byte.

  UUIDIterator temp_uuid_iterator;
  temp_uuid_iterator.AddUUID(osd_uuid);

  boost::scoped_ptr< SyncCallback<ObjectData> > response(
      ExecuteSyncRequest< SyncCallback<ObjectData>* >(
          boost::bind(&xtreemfs::pbrpc::OSDServiceClient::read_sync,
              osd_service_client_,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &read_request),
          &temp_uuid_iterator,
          uuid_resolver_,
          volume_options_.max_tries,
          volume_options_));
  // We don't care about the result.
  response->DeleteBuffers();
}

void FileHandleImplementation::WaitForPendingXCapRenewal() {
  boost::mutex::scoped_lock lock(mutex_);
  while (xcap_renewal_pending_) {
    xcap_renewal_pending_cond_.wait(lock);
  }
}

void FileHandleImplementation::Close() {
  try {
    Flush(true);  // true = Tell Flush() the file will be closed.
  } catch(const XtreemFSException& e) {
    // Current C++ does not allow to store the exception and rethrow it outside
    // the catch block. We also don't want to use an extra meta object with a
    // destructor that could execute the cleanup code.
    // Therefore, CloseFileHandle() has to be called here and after the try/
    // catch block.
    file_info_->CloseFileHandle(this);

    // Rethrow exception.
    throw;
  }
  file_info_->CloseFileHandle(this);
}

boost::uint64_t FileHandleImplementation::GetFileId() {
  boost::mutex::scoped_lock lock(mutex_);

  return GetFileIdHelper(&lock);
}

boost::uint64_t FileHandleImplementation::GetFileIdHelper(
    boost::mutex::scoped_lock* lock) {
  assert(lock && lock->owns_lock());

  return ExtractFileIdFromXCap(xcap_);
}

const StripeTranslator* FileHandleImplementation::GetStripeTranslator(
    xtreemfs::pbrpc::StripingPolicyType type) {
  // Find the corresponding StripingPolicy.
  map<StripingPolicyType, StripeTranslator*>::const_iterator it
    = stripe_translators_.find(type);

  // Type not found.
  if (it == stripe_translators_.end()) {
    throw XtreemFSException("No StripingPolicy found for type: " + type);
  } else {
    // Type found.
    return it->second;
  }
}

void FileHandleImplementation::GetXCap(xtreemfs::pbrpc::XCap* xcap) {
  assert(xcap);

  boost::mutex::scoped_lock lock(mutex_);
  xcap->CopyFrom(xcap_);
}

void FileHandleImplementation::MarkAsyncWritesAsFailed() {
  boost::mutex::scoped_lock lock(mutex_);
  async_writes_failed_ = true;
}

void FileHandleImplementation::WriteBackFileSize(
    const xtreemfs::pbrpc::OSDWriteResponse& owr,
    bool close_file) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "WriteBackFileSize: file_id: " <<  GetFileId()
        << " # bytes: " << owr.size_in_bytes()
        << " close file? " << close_file
        << endl;
  }

  xtreemfs_update_file_sizeRequest rq;
  {
    boost::mutex::scoped_lock lock(mutex_);
    rq.mutable_xcap()->CopyFrom(xcap_);
  }
  rq.mutable_osd_write_response()->CopyFrom(owr);
  rq.set_close_file(close_file);

  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::
                  xtreemfs_update_file_size_sync,
              mrc_service_client_,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials_bogus_),
              &rq),
          mrc_uuid_iterator_,
          uuid_resolver_,
          volume_options_.max_tries,
          volume_options_));
  response->DeleteBuffers();
}

void FileHandleImplementation::WriteBackFileSizeAsync() {
  xtreemfs_update_file_sizeRequest rq;
  {
    boost::mutex::scoped_lock lock(mutex_);
    if (!osd_write_response_for_async_write_back_.get()) {
      return;
    }

    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      string path;
      file_info_->GetPath(&path);
      Logging::log->getLog(LEVEL_DEBUG)
          << "update_file_size: " << path << " # bytes: "
          << osd_write_response_for_async_write_back_->size_in_bytes()
          << endl;
    }

    rq.mutable_xcap()->CopyFrom(xcap_);
    rq.mutable_osd_write_response()
        ->CopyFrom(*osd_write_response_for_async_write_back_);
  }
  // Set to false because a close would use a sync writeback.
  rq.set_close_file(false);

  string mrc_uuid;
  string mrc_address;
  mrc_uuid_iterator_->GetUUID(&mrc_uuid);
  uuid_resolver_->UUIDToAddress(mrc_uuid, &mrc_address);
  mrc_service_client_->xtreemfs_update_file_size(mrc_address,
                                                 auth_bogus_,
                                                 user_credentials_bogus_,
                                                 &rq,
                                                 this,
                                                 NULL);
}

void FileHandleImplementation::RenewXCapAsync() {
  XCap xcap_copy;
  {
    boost::mutex::scoped_lock lock(mutex_);

    // TODO(mberlin): Only renew after some time has elapsed.
    // TODO(mberlin): Cope with local clocks which have a high clock skew.
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "Renew XCap for file_id: " <<  GetFileIdHelper(&lock)
            << "Expiration in: " << (xcap_.expire_time_s() - time(NULL))
            << endl;
    }

    // Create a copy of the XCap.
    xcap_copy.CopyFrom(xcap_);
    xcap_renewal_pending_ = true;
  }

  string mrc_uuid;
  string mrc_address;
  mrc_uuid_iterator_->GetUUID(&mrc_uuid);
  uuid_resolver_->UUIDToAddress(mrc_uuid, &mrc_address);
  mrc_service_client_->xtreemfs_renew_capability(
      mrc_address,
      auth_bogus_,
      user_credentials_bogus_,
      &xcap_copy,
      this,
      NULL);
}

void FileHandleImplementation::CallFinished(
    xtreemfs::pbrpc::timestampResponse* response_message,
    char* data,
    boost::uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  if (error) {
    Logging::log->getLog(LEVEL_WARN)
        << "error in async filesize update " << file_info_->path() << endl;
    Logging::log->getLog(LEVEL_WARN) << error->DebugString() << endl;
  }

  file_info_->AsyncFileSizeUpdateResponseHandler(
      *(osd_write_response_for_async_write_back_.get()),
      this,
      error == NULL);

  // Cleanup.
  delete response_message;
  delete data;
  delete error;
}

void FileHandleImplementation::CallFinished(
    xtreemfs::pbrpc::XCap* new_xcap,
    char* data,
    boost::uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  boost::mutex::scoped_lock lock(mutex_);

  if (error) {
    Logging::log->getLog(LEVEL_ERROR)
        << "error in XCap renewal " << file_info_->path() << endl;
    Logging::log->getLog(LEVEL_ERROR) << error->DebugString() << endl;
  } else {
    // Overwrite current XCap only by a newer one (i.e. later expire time).
    if (new_xcap->expire_time_s() > xcap_.expire_time_s()) {
      xcap_.CopyFrom(*new_xcap);

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
           << "XCap renewed for file_id: " << GetFileIdHelper(&lock) << endl;
      }
    }
  }

  // Cleanup.
  delete new_xcap;
  delete data;
  delete error;

  xcap_renewal_pending_ = false;
  xcap_renewal_pending_cond_.notify_all();
}

void FileHandleImplementation::set_osd_write_response_for_async_write_back(
    const xtreemfs::pbrpc::OSDWriteResponse& owr) {
  boost::mutex::scoped_lock lock(mutex_);

  // Per file size update a new FileHandle will be created, i.e. do not allow
  // to set an OSD Write Response twice.
  assert(!osd_write_response_for_async_write_back_.get());
  osd_write_response_for_async_write_back_.reset(new OSDWriteResponse(owr));
}

}  // namespace xtreemfs
