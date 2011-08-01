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
    const std::string& mrc_uuid,
    FileInfo* file_info,
    const xtreemfs::pbrpc::XCap& xcap,
    UUIDResolver* uuid_resolver,
    xtreemfs::pbrpc::MRCServiceClient* mrc_service_client,
    xtreemfs::pbrpc::OSDServiceClient* osd_service_client,
    const std::map<xtreemfs::pbrpc::StripingPolicyType,
                   StripeTranslator*>& stripe_translators,
    const Options& options,
    const xtreemfs::pbrpc::Auth& auth_bogus,
    const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus)
    : client_uuid_(client_uuid),
      mrc_uuid_(mrc_uuid),
      file_info_(file_info),
      xcap_(xcap),
      xcap_renewal_pending_(false),
      osd_write_response_for_async_write_back_(NULL),
      uuid_resolver_(uuid_resolver),
      mrc_service_client_(mrc_service_client),
      osd_service_client_(osd_service_client),
      stripe_translators_(stripe_translators),
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
  // Prepare request object.
  int current_replica_index;
  readRequest rq;
  {
    boost::mutex::scoped_lock lock(mutex_);
    rq.mutable_file_credentials()->mutable_xcap()->CopyFrom(xcap_);
    rq.set_file_id(xcap_.file_id());
  }
  file_info_->GetXLocSet(rq.mutable_file_credentials()->mutable_xlocs(),
                         &current_replica_index);
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

  string osd_uuid = "";
  string osd_address = "";
  int max_total_tries = volume_options_.max_read_tries;
  int attempts_so_far = 0;
  // Read all objects.
  for (size_t j = 0; j < operations.size(); j++) {
    // Retry read if failed whereas max_total_tries = 0 means infinite.
    for (int i = 0;
         attempts_so_far < max_total_tries || max_total_tries == 0;
         i++) {
      attempts_so_far++;

      rq.set_object_number(operations[j].obj_number);
      rq.set_object_version(0);
      rq.set_offset(operations[j].req_offset);
      rq.set_length(operations[j].req_size);

      // Pick UUID from xlocset (use the current replica).
      // TODO(mberlin): In case of read only replication, use a bitmask to
      //                store which replicas are available and select them in a
      //                round robin manner.
      osd_uuid = GetOSDUUIDFromXlocSet(xlocs,
                                       current_replica_index,
                                       operations[j].osd_offset);
      uuid_resolver_->UUIDToAddress(osd_uuid, &osd_address);

      // TODO(mberlin): Update xloc list if newer version found (on OSD?).
      boost::scoped_ptr< SyncCallback<ObjectData> > response;
      try {
        response.reset(ExecuteSyncRequest< SyncCallback<ObjectData>* >(
            boost::bind(&xtreemfs::pbrpc::OSDServiceClient::read_sync,
                        osd_service_client_,
                        boost::cref(osd_address),
                        boost::cref(auth_bogus_),
                        boost::cref(user_credentials),
                        &rq),
            1,  // Only one attempt, we retry on our own.
            volume_options_,
            attempts_so_far != max_total_tries));  // Delay all but last try.
      } catch(const IOException& e) {
        bool a_left = attempts_so_far < max_total_tries || max_total_tries == 0;
        Logging::log->getLog(LEVEL_ERROR)
            << (a_left ? "RETRYING" : "THROWING FAILURE")
            << " after attempt: " << attempts_so_far
            << " error in read (IO error) "
            << file_info_->path() << " " << count << " " << offset << endl;
        if (a_left) {
          // Use the next replica.
          current_replica_index++;
          if (current_replica_index == xlocs.replicas_size()) {
            current_replica_index = 0;
          }
          file_info_->set_current_replica_index(current_replica_index);
          continue;
        } else {
          throw;  // Last attempt failed, rethrow exception.
        }
      } catch(const InternalServerErrorException& e) {
        bool a_left = attempts_so_far < max_total_tries || max_total_tries == 0;
        Logging::log->getLog(LEVEL_ERROR)
            << (a_left ? "RETRYING" : "THROWING FAILURE")
            << " after attempt: " << attempts_so_far
            << "error in read (internal server error) "
            << file_info_->path() << " " << count << " " << offset << endl;
        if (a_left) {
          // Use the next replica.
          current_replica_index++;
          if (current_replica_index == xlocs.replicas_size()) {
            current_replica_index = 0;
          }
          file_info_->set_current_replica_index(current_replica_index);
          continue;
        } else {
          throw;  // Last attempt failed, rethrow exception.
        }
      } catch(const ReplicationRedirectionException& e) {
        // Find the index for the redirected UUID in the xlocset and set it as
        // the current replica.
        int new_replica_index = current_replica_index;
        for (int i = 0; i < xlocs.replicas_size(); i++) {
          if (xlocs.replicas(i).osd_uuids(0) == e.redirect_to_server_uuid_) {
            new_replica_index = i;
            break;
          }
        }
        if (new_replica_index == current_replica_index) {
          string attempt_string = boost::lexical_cast<string>(attempts_so_far);
          string error = "We were redirected at attempt " + attempt_string
              + " by the OSD with the UUID: " + osd_uuid
              + " to an OSD with the UUID: " + e.redirect_to_server_uuid_
              + " which was not found in the current XlocSet: "
              + xlocs.DebugString();
          Logging::log->getLog(LEVEL_ERROR) << error << endl;
          xtreemfs::util::ErrorLog::error_log->AppendError(error);
        }
        current_replica_index = new_replica_index;
        file_info_->set_current_replica_index(current_replica_index);

        // Always retry after a redirect - if needed, manipulate retry counter.
        if (max_total_tries != 0 && attempts_so_far == max_total_tries) {
          // This was the last retry, but we give it another chance.
          max_total_tries++;
        }
        continue;
      }

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

      break;  // Read was successful, do not retry again.
    }
  }

  return received_data;
}

int FileHandleImplementation::Write(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const char *buf,
    size_t count,
    off_t offset) {
  // Prepare request object.
  int current_replica_index;
  writeRequest rq;
  {
    boost::mutex::scoped_lock lock(mutex_);
    rq.mutable_file_credentials()->mutable_xcap()->CopyFrom(xcap_);
    rq.set_file_id(xcap_.file_id());
  }
  file_info_->GetXLocSet(rq.mutable_file_credentials()->mutable_xlocs(),
                         &current_replica_index);
  // Use a reference for shorter code.
  const XLocSet& xlocs = rq.file_credentials().xlocs();

  if (xlocs.replicas_size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
        << "no replica found for file:" << file_info_->path() << endl;
    throw PosixErrorException(
        POSIX_ERROR_EIO,
        "no replica found for file:" + file_info_->path());
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

  string osd_uuid = "";
  string osd_address = "";
  int max_total_tries = volume_options_.max_write_tries;
  int attempts_so_far = 0;

  // Write all objects.
  for (size_t j = 0; j < operations.size(); j++) {
    // Retry write if failed.
    for (int i = 0;
         attempts_so_far < max_total_tries || max_total_tries == 0;
         i++) {
      attempts_so_far++;
      const WriteOperation& operation = operations[j];

      rq.set_object_number(operation.obj_number);
      rq.set_object_version(0);
      rq.set_offset(operation.req_offset);
      rq.set_lease_timeout(0);

      ObjectData *data = rq.mutable_object_data();
      data->set_checksum(0);
      data->set_invalid_checksum_on_osd(false);
      data->set_zero_padding(0);

      // Pick UUID from xlocset.
      osd_uuid = GetOSDUUIDFromXlocSet(xlocs,
                                       current_replica_index,
                                       operation.osd_offset);
      uuid_resolver_->UUIDToAddress(osd_uuid, &osd_address);

      boost::scoped_ptr< SyncCallback<OSDWriteResponse> > response;
      try {
        response.reset(ExecuteSyncRequest< SyncCallback<OSDWriteResponse>* >(
            boost::bind(
                &xtreemfs::pbrpc::OSDServiceClient::write_sync,
                osd_service_client_,
                boost::cref(osd_address),
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &rq,
                operation.data,
                operation.req_size),
            1,  // Only one attempt, we retry on our own.
            volume_options_,
            attempts_so_far != max_total_tries));  // Delay all but last try.
      } catch(const IOException& e) {
        bool a_left = attempts_so_far < max_total_tries || max_total_tries == 0;
        Logging::log->getLog(LEVEL_ERROR)
            << (a_left ? "RETRYING" : "THROWING FAILURE")
            << " after attempt: " << attempts_so_far
            << "error in write (IO error) "
            << file_info_->path() << " " << count << " " << offset << endl;
        if (a_left) {
          // Use the next replica.
          current_replica_index++;
          if (current_replica_index == xlocs.replicas_size()) {
            current_replica_index = 0;
          }
          file_info_->set_current_replica_index(current_replica_index);
          continue;  // Retry.
        } else {
          throw;  // Last attempt failed, rethrow exception.
        }
      } catch(const InternalServerErrorException& e) {
        bool a_left = attempts_so_far < max_total_tries || max_total_tries == 0;
        Logging::log->getLog(LEVEL_ERROR)
            << (a_left ? "RETRYING" : "THROWING FAILURE")
            << " after attempt: " << attempts_so_far
            << "error in write (internal server error) "
            << file_info_->path() << " " << count << " " << offset << endl;
        if (a_left) {
          // Use the next replica.
          current_replica_index++;
          if (current_replica_index == xlocs.replicas_size()) {
            current_replica_index = 0;
          }
          file_info_->set_current_replica_index(current_replica_index);
          continue;  // Retry.
        } else {
          throw;  // Last attempt failed, rethrow exception.
        }
      } catch(const ReplicationRedirectionException& e) {
        // Find the index for the redirected UUID in the xlocset and set it as
        // the current replica.
        int new_replica_index = current_replica_index;
        for (int i = 0; i < xlocs.replicas_size(); i++) {
          if (xlocs.replicas(i).osd_uuids(0) == e.redirect_to_server_uuid_) {
            new_replica_index = i;
            break;
          }
        }
        if (new_replica_index == current_replica_index) {
          string error = "We were redirected by the OSD "
              "with the UUID: " + osd_uuid + " to an OSD with the UUID: "
              + e.redirect_to_server_uuid_ + ") which was not found in the "
              "current XlocSet: " + xlocs.DebugString();
          Logging::log->getLog(LEVEL_ERROR) << error << endl;
          xtreemfs::util::ErrorLog::error_log->AppendError(error);
        }
        current_replica_index = new_replica_index;
        file_info_->set_current_replica_index(current_replica_index);

        // Always retry after a redirect - if needed, manipulate retry counter.
        if (max_total_tries != 0 && attempts_so_far == max_total_tries) {
          // This was the last retry, but we give it another chance.
          max_total_tries++;
        }
        continue;
      }

      // If the filesize has changed, remember OSDWriteResponse for later file
      // size update towards the MRC (executed by filesize_writeback_thread_).
      if (response->response()->has_size_in_bytes()) {
        if (file_info_->TryToUpdateOSDWriteResponse(response->response(),
                                                    xcap_)) {
          // Free everything except the response.
          delete response->data();
          delete response->error();
        } else {
          response->DeleteBuffers();
        }
      }

      break;  // Write of object was successful.
    }  // retry loop.
  }  // objects loop.

  return count;
}

void FileHandleImplementation::Flush() {
  file_info_->Flush(this);
}

void FileHandleImplementation::Truncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    off_t new_file_size) {
  XCap xcap_copy;
  {
    boost::mutex::scoped_lock lock(mutex_);
    xcap_copy.CopyFrom(xcap_);
  }

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  // 1. Call truncate at the MRC (in order to increase the trunc epoch).
  boost::scoped_ptr< SyncCallback<XCap> > response(
    ExecuteSyncRequest< SyncCallback<XCap>* >(
        boost::bind(
            &xtreemfs::pbrpc::MRCServiceClient::ftruncate_sync,
            mrc_service_client_,
            boost::cref(mrc_address),
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &xcap_copy),
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
  int current_replica_index;
  truncateRequest truncate_rq;
  file_info_->GetXLocSet(
      truncate_rq.mutable_file_credentials()->mutable_xlocs(),
      &current_replica_index);
  {
    boost::mutex::scoped_lock lock(mutex_);
    truncate_rq.mutable_file_credentials()->mutable_xcap()->CopyFrom(xcap_);
    truncate_rq.set_file_id(xcap_.file_id());
  }
  truncate_rq.set_new_file_size(new_file_size);

  // Try all available replicas and handle redirects.
  string osd_uuid = "";
  string osd_address = "";
  int max_total_tries = volume_options_.max_tries;
  int attempts_so_far = 0;
  const XLocSet& xlocs = truncate_rq.file_credentials().xlocs();
  boost::scoped_ptr< SyncCallback<OSDWriteResponse> > response;
  for (int i = 0;
       attempts_so_far < max_total_tries || max_total_tries == 0;
       i++) {
    attempts_so_far++;

    // Pick UUID from xlocset.
    osd_uuid = GetOSDUUIDFromXlocSet(xlocs,
                                     current_replica_index,
                                     0);  // Head OSD of striping pattern.
    uuid_resolver_->UUIDToAddress(osd_uuid, &osd_address);

    try {
      response.reset(ExecuteSyncRequest< SyncCallback<OSDWriteResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::OSDServiceClient::truncate_sync,
              osd_service_client_,
              boost::cref(osd_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &truncate_rq),
          1,  // Only one attempt, we retry on our own.
          volume_options_,
          attempts_so_far != max_total_tries));  // Delay all but last try.
    } catch(const IOException& e) {
      bool a_left = attempts_so_far < max_total_tries || max_total_tries == 0;
      Logging::log->getLog(LEVEL_ERROR)
          << (a_left ? "RETRYING" : "THROWING FAILURE")
          << " after attempt: " << attempts_so_far
          << "error in truncate (IO error) "
          << file_info_->path() << " new size: " << new_file_size << endl;
      if (a_left) {
        // Use the next replica.
        current_replica_index++;
        if (current_replica_index == xlocs.replicas_size()) {
          current_replica_index = 0;
        }
        file_info_->set_current_replica_index(current_replica_index);
        continue;  // Retry.
      } else {
        throw;  // Last attempt failed, rethrow exception.
      }
    } catch(const InternalServerErrorException& e) {
      bool a_left = attempts_so_far < max_total_tries || max_total_tries == 0;
      Logging::log->getLog(LEVEL_ERROR)
          << (a_left ? "RETRYING" : "THROWING FAILURE")
          << " after attempt: " << attempts_so_far
          << "error in truncate (internal server error) "
          << file_info_->path() << " new size: " << new_file_size << endl;
      if (a_left) {
        // Use the next replica.
        current_replica_index++;
        if (current_replica_index == xlocs.replicas_size()) {
          current_replica_index = 0;
        }
        file_info_->set_current_replica_index(current_replica_index);
        continue;  // Retry.
      } else {
        throw;  // Last attempt failed, rethrow exception.
      }
    } catch(const ReplicationRedirectionException& e) {
      // Find the index for the redirected UUID in the xlocset and set it as
      // the current replica.
      int new_replica_index = current_replica_index;
      for (int i = 0; i < xlocs.replicas_size(); i++) {
        if (xlocs.replicas(i).osd_uuids(0) == e.redirect_to_server_uuid_) {
          new_replica_index = i;
          break;
        }
      }
      if (new_replica_index == current_replica_index) {
        string error = "We were redirected by the OSD "
            "with the UUID: " + osd_uuid + " to an OSD with the UUID: "
            + e.redirect_to_server_uuid_ + ") which was not found in the "
            "current XlocSet: " + xlocs.DebugString();
        Logging::log->getLog(LEVEL_ERROR) << error << endl;
        xtreemfs::util::ErrorLog::error_log->AppendError(error);
      }
      current_replica_index = new_replica_index;
      file_info_->set_current_replica_index(current_replica_index);

      // Always retry after a redirect - if needed, manipulate retry counter.
      if (max_total_tries != 0 && attempts_so_far == max_total_tries) {
        // This was the last retry, but we give it another chance.
        max_total_tries++;
      }
      continue;
    }

    break;  // Truncate was successful.
  }  // retry loop.

  assert(response->response()->has_size_in_bytes());
  // Free the rest of the msg.
  delete response->data();
  delete response->error();

  // Register the osd write response at this file's FileInfo.
  file_info_->TryToUpdateOSDWriteResponse(response->response(), xcap_);

  // 3. Update the file size at the MRC.
  file_info_->Flush(this);
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
  int current_replica_index;
  file_info_->GetXLocSet(
      lock_request.mutable_file_credentials()->mutable_xlocs(),
      &current_replica_index);
  {
    boost::mutex::scoped_lock lock(mutex_);
    lock_request.mutable_file_credentials()->mutable_xcap()
        ->CopyFrom(xcap_);
  }

  string osd_address;
  uuid_resolver_->UUIDToAddress(
      GetOSDUUIDFromXlocSet(lock_request.file_credentials().xlocs(),
                            current_replica_index,
                            0),
      &osd_address);

  boost::scoped_ptr< SyncCallback<Lock> > response;
  if (!wait_for_lock) {
    response.reset(ExecuteSyncRequest< SyncCallback<Lock>* >(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_acquire_sync,
            osd_service_client_,
            boost::cref(osd_address),
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &lock_request),
        volume_options_.max_tries,
        volume_options_));
  } else {
    int retries_left = volume_options_.max_tries;
    // TODO(mberlin): Try all available replicas.
    while (retries_left == 0 || retries_left--) {
      try {
        response.reset(ExecuteSyncRequest< SyncCallback<Lock>* >(
            boost::bind(
                &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_acquire_sync,
                osd_service_client_,
                boost::cref(osd_address),
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &lock_request),
            1,
            volume_options_,
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
  int current_replica_index;
  file_info_->GetXLocSet(
      lock_request.mutable_file_credentials()->mutable_xlocs(),
      &current_replica_index);
  {
    boost::mutex::scoped_lock lock(mutex_);
    lock_request.mutable_file_credentials()->mutable_xcap()
        ->CopyFrom(xcap_);
  }

  string osd_address;
  uuid_resolver_->UUIDToAddress(
      GetOSDUUIDFromXlocSet(lock_request.file_credentials().xlocs(),
                            current_replica_index,
                            0),
      &osd_address);

  boost::scoped_ptr< SyncCallback<Lock> > response(
    ExecuteSyncRequest< SyncCallback<Lock>* >(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_check_sync,
            osd_service_client_,
            boost::cref(osd_address),
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &lock_request),
        volume_options_.max_tries,
        volume_options_));
  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  return response->response();
}

void FileHandleImplementation::ReleaseLock(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const xtreemfs::pbrpc::Lock& lock) {
  ReleaseLock(user_credentials, lock, true);
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
  ReleaseLock(user_credentials, lock, true);
}

void FileHandleImplementation::ReleaseLock(const xtreemfs::pbrpc::Lock& lock,
                                           bool update_cache) {
  ReleaseLock(user_credentials_bogus_, lock, update_cache);
}

void FileHandleImplementation::ReleaseLock(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const xtreemfs::pbrpc::Lock& lock,
    bool update_cache) {
  int current_replica_index;
  lockRequest unlock_request;
  file_info_->GetXLocSet(
      unlock_request.mutable_file_credentials()->mutable_xlocs(),
      &current_replica_index);
  {
    boost::mutex::scoped_lock lock(mutex_);
    unlock_request.mutable_file_credentials()->mutable_xcap()
        ->CopyFrom(xcap_);
  }
  unlock_request.mutable_lock_request()->CopyFrom(lock);

  string osd_address;
  uuid_resolver_->UUIDToAddress(
      GetOSDUUIDFromXlocSet(unlock_request.file_credentials().xlocs(),
                            current_replica_index,
                            0),
      &osd_address);

  boost::scoped_ptr< SyncCallback<emptyResponse> > response(
    ExecuteSyncRequest< SyncCallback<emptyResponse>* >(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_release_sync,
            osd_service_client_,
            boost::cref(osd_address),
            boost::cref(auth_bogus_),
            boost::cref(user_credentials),
            &unlock_request),
        volume_options_.max_tries,
        volume_options_));
  response->DeleteBuffers();

  if (update_cache) {
    file_info_->DelLock(lock);
  }
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
        "xlocset: " + xlocs.SerializeAsString());
  }

  // Read one byte from the replica to trigger the replication.
  read_request.set_object_number(0);
  read_request.set_object_version(0);
  read_request.set_offset(0);
  read_request.set_length(1);  // 1 Byte.

  string osd_address;
  uuid_resolver_->UUIDToAddress(osd_uuid, &osd_address);

  boost::scoped_ptr< SyncCallback<ObjectData> > response(
      ExecuteSyncRequest< SyncCallback<ObjectData>* >(
          boost::bind(&xtreemfs::pbrpc::OSDServiceClient::read_sync,
              osd_service_client_,
              boost::cref(osd_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &read_request),
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
  file_info_->CloseFileHandle(this);
}

boost::uint64_t FileHandleImplementation::GetFileId() {
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

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);
  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::
                  xtreemfs_update_file_size_sync,
              mrc_service_client_,
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials_bogus_),
              &rq),
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

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);
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
            << "Renew XCap for file_id: " <<  GetFileId()
            << "Expiration in: " << (xcap_.expire_time_s() - time(NULL))
            << endl;
    }

    // Create a copy of the XCap.
    xcap_copy.CopyFrom(xcap_);
    xcap_renewal_pending_ = true;
  }

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);
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
           << "XCap renewed for file_id: " << GetFileId() << endl;
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
