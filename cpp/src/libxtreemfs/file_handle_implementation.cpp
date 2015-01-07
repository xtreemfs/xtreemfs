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
#include <memory>
#include <string>
#include <vector>

#include "libxtreemfs/async_write_buffer.h"
#include "libxtreemfs/execute_sync_request.h"
#include "libxtreemfs/file_info.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/stripe_translator.h"
#include "libxtreemfs/container_uuid_iterator.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/error_log.h"
#include "util/logging.h"
#include "xtreemfs/MRCServiceClient.h"
#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

// Fix ambiguous map error on Solaris (see
// http://groups.google.com/group/xtreemfs/msg/b44605dbbd7b6d0f)
using std::map;

namespace xtreemfs {

/** Constructor called by FileInfo.CreateFileHandle().
 *
 * @remark The ownership of all parameters will not be transferred. For every
 *         opened FileHandle Close() has to be called.
 */
FileHandleImplementation::FileHandleImplementation(
    ClientImplementation* client,
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
    ObjectCache* object_cache,
    const Options& options,
    const xtreemfs::pbrpc::Auth& auth_bogus,
    const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus)
    : client_(client),
      client_uuid_(client_uuid),
      mrc_uuid_iterator_(mrc_uuid_iterator),
      osd_uuid_iterator_(osd_uuid_iterator),
      uuid_resolver_(uuid_resolver),
      file_info_(file_info),
      osd_write_response_for_async_write_back_(NULL),
      mrc_service_client_(mrc_service_client),
      osd_service_client_(osd_service_client),
      stripe_translators_(stripe_translators),
      async_writes_enabled_(async_writes_enabled),
      async_writes_failed_(false),
      object_cache_(object_cache),
      volume_options_(options),
      auth_bogus_(auth_bogus),
      user_credentials_bogus_(user_credentials_bogus),
      xcap_manager_(xcap,
                    mrc_service_client,
                    uuid_resolver,
                    mrc_uuid_iterator,
                    auth_bogus_,
                    user_credentials_bogus_) {
  if (async_writes_enabled_) {
    object_cache_ = NULL;
  }
}

FileHandleImplementation::~FileHandleImplementation() {}


template<typename T>
T FileHandleImplementation::ExecuteViewCheckedOperation(
    boost::function<T()> operation) {

  RPCOptions options(volume_options_.max_view_renewals,
                     volume_options_.retry_delay_s, false,
                     volume_options_.was_interrupted_function);

  int attempt;
  for (attempt = 1;
          (attempt <= options.max_retries() || options.max_retries() == 0) &&
          !Interruptibilizer::WasInterrupted(options.was_interrupted_cb());
       attempt++) {

    try {
      return operation();

    } catch (const InvalidViewException& ex) {
      if (attempt == options.max_retries() && options.max_retries() > 0) {
        // The request did finally fail.
        // Append the number of attempts to the message and rethrow.
        string error_msg(ex.what());
        error_msg.append(" Request finally failed after " +
                         boost::lexical_cast<string>(attempt) + " attempts.");

        throw InvalidViewException(error_msg);
      } else {
        // Delay the xLocSet renewal and the next run of the operation.
        Interruptibilizer::SleepInterruptible((options.retry_delay_s() * 1000),
                  options.was_interrupted_cb());

        // Try to renew the XLocSet.
        RenewXLocSet();
      }
    }
  }

  // If we reach this, something went wrong.
  string error_msg = "The operation did fail due to an outdated view after "
      + boost::lexical_cast<string>(attempt) + " attempts.";

  if (Interruptibilizer::WasInterrupted(options.was_interrupted_cb())) {
    throw PosixErrorException(POSIX_ERROR_EINTR, error_msg);
  } else {
    throw XtreemFSException(error_msg);
  }
}

int FileHandleImplementation::Read(char *buf, size_t count, int64_t offset) {
  boost::function<int()> operation(
      boost::bind(&FileHandleImplementation::DoRead, this,
                  buf, count, offset));
  return ExecuteViewCheckedOperation(operation);
}

int FileHandleImplementation::DoRead(
    char *buf,
    size_t count,
    int64_t offset) {

  if (async_writes_enabled_) {
    file_info_->WaitForPendingAsyncWrites();
    ThrowIfAsyncWritesFailed();
  }

  // Prepare request object.
  FileCredentials file_credentials;
  xcap_manager_.GetXCap(file_credentials.mutable_xcap());
  boost::shared_ptr<UUIDContainer> osd_uuid_container =
      file_info_->GetXLocSetAndUUIDContainer(file_credentials.mutable_xlocs());
  // Use a reference for shorter code.
  const XLocSet& xlocs = file_credentials.xlocs();

  size_t received_data = 0;

  if (xlocs.replicas_size() == 0) {
    string path;
    file_info_->GetPath(&path);
    string error_msg = "No replica found for file: " + path;
    Logging::log->getLog(LEVEL_ERROR) << error_msg << endl;
    ErrorLog::error_log->AppendError(error_msg);
    throw PosixErrorException(
        POSIX_ERROR_EIO,
        "No replica found for file: " + path);
  }

  // get all striping policies
  StripeTranslator::PolicyContainer striping_policies;
  for (int32_t i = 0; i < xlocs.replicas_size(); ++i) {
    striping_policies.push_back(&(xlocs.replicas(i).striping_policy()));
  }

  // NOTE: We assume that all replicas use the same striping policy type and
  //       that all replicas use the same stripe size.
  const StripeTranslator* translator =
      GetStripeTranslator((*striping_policies.begin())->type());

  // Map offset to corresponding OSDs.
  std::vector<ReadOperation> operations;
  translator->TranslateReadRequest(buf, count, offset, striping_policies,
                                   &operations);

  boost::scoped_ptr<ContainerUUIDIterator> temp_uuid_iterator_for_striping;

  // Read all objects.
  for (size_t j = 0; j < operations.size(); j++) {
    // Differ between striping and the rest (replication, no replication).
    UUIDIterator* uuid_iterator;
    if (xlocs.replicas(0).osd_uuids_size() > 1) {
      // Replica is striped. Get a UUID iterator from OSD offsets
      temp_uuid_iterator_for_striping.reset(
          new ContainerUUIDIterator(osd_uuid_container,
                                    operations[j].osd_offsets));
      uuid_iterator = temp_uuid_iterator_for_striping.get();
    } else {
      // TODO(mberlin): Enhance UUIDIterator to read from different replicas.
      uuid_iterator = osd_uuid_iterator_;
    }

    if (object_cache_) {
      ObjectReaderFunction reader = boost::bind(
          &FileHandleImplementation::ReadFromOSD,
          this,
          uuid_iterator,
          file_credentials,
          _1, _2, 0, object_cache_->object_size());
      ObjectWriterFunction writer = boost::bind(
          &FileHandleImplementation::WriteToOSD,
          this,
          osd_uuid_iterator_,
          file_credentials,
          _1, 0, _2, _3);
      received_data += object_cache_->Read(
          operations[j].obj_number,
          operations[j].req_offset,
          operations[j].data,
          operations[j].req_size,
          reader, writer);
    } else {
      received_data +=
          ReadFromOSD(uuid_iterator, file_credentials, operations[j].obj_number,
          operations[j].data, operations[j].req_offset,
          operations[j].req_size);
    }
  }

  return received_data;
}

int FileHandleImplementation::ReadFromOSD(
    UUIDIterator* uuid_iterator,
    const FileCredentials& file_credentials,
    int object_no, char* buffer, int offset_in_object,
    int bytes_to_read) {
  readRequest rq;
  rq.set_file_id(file_credentials.xcap().file_id());
  rq.mutable_file_credentials()->CopyFrom(file_credentials);
  rq.set_object_number(object_no);
  rq.set_object_version(0);
  rq.set_offset(offset_in_object);
  rq.set_length(bytes_to_read);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(&xtreemfs::pbrpc::OSDServiceClient::read_sync,
                      osd_service_client_,
                      _1,
                      boost::cref(auth_bogus_),
                      boost::cref(user_credentials_bogus_),
                      &rq),
          uuid_iterator,
          uuid_resolver_,
          RPCOptions(volume_options_.max_read_tries,
                     volume_options_.retry_delay_s,
                     false,
                     volume_options_.was_interrupted_function),
          false,
          &xcap_manager_,
          rq.mutable_file_credentials()->mutable_xcap()));

  xtreemfs::pbrpc::ObjectData* data =
      static_cast<xtreemfs::pbrpc::ObjectData*>(response->response());
  // Insert data into read-buffer
  int data_length = response->data_length();
  memcpy(buffer, response->data(), data_length);
  // If zero_padding() > 0, the gap has to be filled with zeroes.
  memset(buffer + data_length, 0, data->zero_padding());

  int received_data = response->data_length() + data->zero_padding();
  response->DeleteBuffers();
  return received_data;
}

int FileHandleImplementation::Write(const char *buf, size_t count,
                                    int64_t offset) {
  boost::function<int()> operation(
      boost::bind(&FileHandleImplementation::DoWrite, this,
                  buf, count, offset));
  return ExecuteViewCheckedOperation(operation);
}

int FileHandleImplementation::DoWrite(
    const char *buf,
    size_t count,
    int64_t offset) {
  if (async_writes_enabled_) {
    ThrowIfAsyncWritesFailed();
  }
  // Create copies of required data.
  FileCredentials file_credentials;
  xcap_manager_.GetXCap(file_credentials.mutable_xcap());
  file_info_->GetXLocSet(file_credentials.mutable_xlocs());
  // Use references for shorter code.
  const string& global_file_id = file_credentials.xcap().file_id();
  const XLocSet& xlocs = file_credentials.xlocs();

  if (xlocs.replicas_size() == 0) {
    string path;
    file_info_->GetPath(&path);
    string error = "No replica found for file: " + path;
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    throw PosixErrorException(POSIX_ERROR_EIO, error);
  }

  // Map operation to stripes.

  // get all striping policies
  StripeTranslator::PolicyContainer striping_policies;
  for (int32_t i = 0; i < xlocs.replicas_size(); ++i) {
    striping_policies.push_back(&(xlocs.replicas(i).striping_policy()));
  }

  // NOTE: We assume that all replicas use the same striping policy type and
  //       that all replicas use the same stripe size.
  const StripeTranslator* translator =
      GetStripeTranslator((*striping_policies.begin())->type());

  // Map offset to corresponding OSDs.
  std::vector<WriteOperation> operations;
  translator->TranslateWriteRequest(buf, count, offset, striping_policies,
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
            &xcap_manager_,
            GetOSDUUIDFromXlocSet(xlocs,
                                  0,  // Use first and only replica.
                                  operations[j].osd_offsets[0]));
      } else {
        write_buffer = new AsyncWriteBuffer(write_request,
                                            operations[j].data,
                                            operations[j].req_size,
                                            this,
                                            &xcap_manager_);
      }

      file_info_->AsyncWrite(write_buffer);

      // Processing of file size updates is handled by the FileInfo's
      // AsyncWriteHandler.
    }
  } else {
    // Synchronous writes.
    string osd_uuid = "";
    // Write all objects.
    for (size_t j = 0; j < operations.size(); j++) {
      // Differentiate between striping and the rest.
      UUIDIterator* uuid_iterator = NULL;
      SimpleUUIDIterator temp_uuid_iterator_for_striping;
      if (xlocs.replicas(0).osd_uuids_size() > 1) {
        // Replica is striped. Pick UUID from xlocset.
        osd_uuid = GetOSDUUIDFromXlocSet(xlocs,
                                         0,  // Use first and only replica.
                                         operations[j].osd_offsets[0]);
        temp_uuid_iterator_for_striping.AddUUID(osd_uuid);
        uuid_iterator = &temp_uuid_iterator_for_striping;
      } else {
        // TODO(mberlin): Enhance UUIDIterator to read from different replicas.
        uuid_iterator = osd_uuid_iterator_;
      }

      if (object_cache_ != NULL) {
        ObjectReaderFunction reader = boost::bind(
            &FileHandleImplementation::ReadFromOSD,
            this,
            uuid_iterator,
            file_credentials,
            _1, _2, 0, object_cache_->object_size());
        ObjectWriterFunction writer = boost::bind(
            &FileHandleImplementation::WriteToOSD,
            this,
            osd_uuid_iterator_,
            file_credentials,
            _1, 0, _2, _3);
        object_cache_->Write(operations[j].obj_number,
                             operations[j].req_offset,
                             operations[j].data,
                             operations[j].req_size,
                             reader, writer);
      } else {
            WriteToOSD(uuid_iterator, file_credentials,
                       operations[j].obj_number, operations[j].req_offset,
                       operations[j].data, operations[j].req_size);
      }
    }
  }

  return count;
}

void FileHandleImplementation::WriteToOSD(
    UUIDIterator* uuid_iterator,
    const FileCredentials& file_credentials,
    int object_no, int offset_in_object, const char* buffer,
    int bytes_to_write) {
  writeRequest write_request;
  write_request.mutable_file_credentials()->CopyFrom(file_credentials);
  write_request.set_file_id(file_credentials.xcap().file_id());
  write_request.set_object_number(object_no);
  write_request.set_object_version(0);
  write_request.set_offset(offset_in_object);
  write_request.set_lease_timeout(0);

  ObjectData *data = write_request.mutable_object_data();
  data->set_checksum(0);
  data->set_invalid_checksum_on_osd(false);
  data->set_zero_padding(0);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::OSDServiceClient::write_sync,
              osd_service_client_,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials_bogus_),
              &write_request,
              buffer,
              bytes_to_write),
          uuid_iterator,
          uuid_resolver_,
          RPCOptions(volume_options_.max_write_tries,
                      volume_options_.retry_delay_s,
                      false,
                      volume_options_.was_interrupted_function),
          false,
          &xcap_manager_,
          write_request.mutable_file_credentials()->mutable_xcap()));

  xtreemfs::pbrpc::OSDWriteResponse* write_response =
      static_cast<xtreemfs::pbrpc::OSDWriteResponse*>(response->response());
  // If the filesize has changed, remember OSDWriteResponse for later file
  // size update towards the MRC (executed by
  // VolumeImplementation::PeriodicFileSizeUpdate).
  if (write_response->has_size_in_bytes()) {
    XCap xcap;
    xcap_manager_.GetXCap(&xcap);
    if (file_info_->TryToUpdateOSDWriteResponse(write_response, xcap)) {
      // Do not delete "write_response" because ownership was transferred.
      delete [] response->data();
      delete response->error();
    } else {
      response->DeleteBuffers();
    }
  } else {
    response->DeleteBuffers();
  }
}

void FileHandleImplementation::Flush() {
  Flush(false);
}

void FileHandleImplementation::Flush(bool close_file) {
  boost::function<void()> operation(
      boost::bind(&FileHandleImplementation::DoFlush, this, close_file));
  ExecuteViewCheckedOperation(operation);
}

void FileHandleImplementation::DoFlush(bool close_file) {
  if (object_cache_ != NULL) {
    FileCredentials file_credentials;
    xcap_manager_.GetXCap(file_credentials.mutable_xcap());
    file_info_->GetXLocSet(file_credentials.mutable_xlocs());
    ObjectWriterFunction writer = boost::bind(
        &FileHandleImplementation::WriteToOSD,
        this,
        osd_uuid_iterator_,
        file_credentials,
        _1, 0, _2, _3);
    object_cache_->Flush(writer);
  }

  file_info_->Flush(this, close_file);

  if (DidAsyncWritesFail()) {
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
    int64_t new_file_size) {
  file_info_->WaitForPendingAsyncWrites();
  ThrowIfAsyncWritesFailed();

  XCap xcap;
  xcap_manager_.GetXCap(&xcap);

  // 1. Call truncate at the MRC (in order to increase the trunc epoch).
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
    ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::MRCServiceClient::ftruncate_sync,
            mrc_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials_bogus_),
            &xcap),
        mrc_uuid_iterator_,
        uuid_resolver_,
        RPCOptionsFromOptions(volume_options_),
        false,
        &xcap_manager_,
        &xcap));

  xtreemfs::pbrpc::XCap* updated_xcap = static_cast<xtreemfs::pbrpc::XCap*>(
      response->response());
  xcap_manager_.SetXCap(*updated_xcap);
  response->DeleteBuffers();

  TruncatePhaseTwoAndThree(new_file_size);
}

void FileHandleImplementation::TruncatePhaseTwoAndThree(int64_t new_file_size) {
  boost::function<void()> operation(
      boost::bind(&FileHandleImplementation::DoTruncatePhaseTwoAndThree, this,
                  new_file_size));
  ExecuteViewCheckedOperation(operation);
}

void FileHandleImplementation::DoTruncatePhaseTwoAndThree(
    int64_t new_file_size) {
  // 2. Call truncate at the head OSD.
  truncateRequest truncate_rq;
  file_info_->GetXLocSet(
      truncate_rq.mutable_file_credentials()->mutable_xlocs());
  xcap_manager_.GetXCap(truncate_rq.mutable_file_credentials()->mutable_xcap());
  truncate_rq.set_file_id(truncate_rq.file_credentials().xcap().file_id());
  truncate_rq.set_new_file_size(new_file_size);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::OSDServiceClient::truncate_sync,
              osd_service_client_,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials_bogus_),
              &truncate_rq),
          osd_uuid_iterator_,
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_),
          false,
          &xcap_manager_,
          truncate_rq.mutable_file_credentials()->mutable_xcap()));
  xtreemfs::pbrpc::OSDWriteResponse* write_response =
      static_cast<xtreemfs::pbrpc::OSDWriteResponse*>(response->response());

  assert(write_response->has_size_in_bytes());

  // Register the osd write response at this file's FileInfo.
  XCap xcap;
  xcap_manager_.GetXCap(&xcap);
  if (file_info_->TryToUpdateOSDWriteResponse(write_response, xcap)) {
    // Do not delete "write_response" because ownership was transferred.
    delete [] response->data();
    delete response->error();
  } else {
    response->DeleteBuffers();
  }

  if (object_cache_ != NULL) {
    object_cache_->Truncate(new_file_size);
  }

  // 3. Update the file size at the MRC.
  file_info_->FlushPendingFileSizeUpdate(this);
}

void FileHandleImplementation::GetAttr(
     const xtreemfs::pbrpc::UserCredentials& user_credentials,
     xtreemfs::pbrpc::Stat* stat) {
  file_info_->GetAttr(user_credentials, stat);
}

xtreemfs::pbrpc::Lock* FileHandleImplementation::AcquireLock(
    int process_id,
    uint64_t offset,
    uint64_t length,
    bool exclusive,
    bool wait_for_lock) {
  boost::function<xtreemfs::pbrpc::Lock*()> operation(
      boost::bind(&FileHandleImplementation::DoAcquireLock, this,
                  process_id, offset, length, exclusive, wait_for_lock));

  return ExecuteViewCheckedOperation(operation);
}

xtreemfs::pbrpc::Lock* FileHandleImplementation::DoAcquireLock(
    int process_id,
    uint64_t offset,
    uint64_t length,
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
  std::auto_ptr<Lock> conflicting_lock(new Lock());
  bool lock_for_pid_cached, cached_lock_for_pid_equal, conflict_found;
  file_info_->CheckLock(lock_request.lock_request(),
                        conflicting_lock.get(),
                        &lock_for_pid_cached,
                        &cached_lock_for_pid_equal,
                        &conflict_found);
  if (conflict_found) {
    throw PosixErrorException(POSIX_ERROR_EAGAIN, "conflicting lock");
  }
  // We allow only one lock per PID, i.e. an existing lock can be always
  // overwritten. In consequence, AcquireLock always has to be executed except
  // the new lock is equal to the current lock.
  if (cached_lock_for_pid_equal) {
    conflicting_lock->CopyFrom(lock_request.lock_request());
    return conflicting_lock.release();
  }

  // Cache could not be used. Complete lockRequest and send to OSD.
  file_info_->GetXLocSet(
      lock_request.mutable_file_credentials()->mutable_xlocs());
  xcap_manager_.GetXCap(
      lock_request.mutable_file_credentials()->mutable_xcap());

  boost::scoped_ptr<rpc::SyncCallbackBase> response;
  if (!wait_for_lock) {
    response.reset(ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_acquire_sync,
            osd_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials_bogus_),
            &lock_request),
        osd_uuid_iterator_,
        uuid_resolver_,
        RPCOptionsFromOptions(volume_options_),
        false,
        &xcap_manager_,
        lock_request.mutable_file_credentials()->mutable_xcap()));
  } else {
    // Retry to obtain the lock in case of EAGAIN responses.
    int retries_left = volume_options_.max_tries;
    while (retries_left == 0 || retries_left--) {
      try {
        response.reset(ExecuteSyncRequest(
            boost::bind(
                &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_acquire_sync,
                osd_service_client_,
                _1,
                boost::cref(auth_bogus_),
                boost::cref(user_credentials_bogus_),
                &lock_request),
            osd_uuid_iterator_,
            uuid_resolver_,
            RPCOptions(1,
                       volume_options_.retry_delay_s,
                       true,  //  delay this attempt in case of errors.
                       volume_options_.was_interrupted_function),
            false,  // UUIDIterator contains UUIDs and not addresses.
            &xcap_manager_,
            lock_request.mutable_file_credentials()->mutable_xcap()));
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
  xtreemfs::pbrpc::Lock* lock =
      static_cast<xtreemfs::pbrpc::Lock*>(response->response());
  file_info_->PutLock(*lock);

  return lock;
}

xtreemfs::pbrpc::Lock* FileHandleImplementation::CheckLock(
    int process_id,
    uint64_t offset,
    uint64_t length,
    bool exclusive) {
  boost::function<xtreemfs::pbrpc::Lock*()> operation(
      boost::bind(&FileHandleImplementation::DoCheckLock, this,
                  process_id, offset, length, exclusive));

  return ExecuteViewCheckedOperation(operation);
}

xtreemfs::pbrpc::Lock* FileHandleImplementation::DoCheckLock(
    int process_id,
    uint64_t offset,
    uint64_t length,
    bool exclusive) {
  // Create lockRequest object for the check lock request.
  lockRequest lock_request;
  lock_request.mutable_lock_request()->set_client_uuid(client_uuid_);
  lock_request.mutable_lock_request()->set_client_pid(process_id);
  lock_request.mutable_lock_request()->set_offset(offset);
  lock_request.mutable_lock_request()->set_length(length);
  lock_request.mutable_lock_request()->set_exclusive(exclusive);

  // Check active locks first.
  std::auto_ptr<Lock> conflicting_lock(new Lock());
  bool lock_for_pid_cached, cached_lock_for_pid_equal, conflict_found;
  file_info_->CheckLock(lock_request.lock_request(),
                        conflicting_lock.get(),
                        &lock_for_pid_cached,
                        &cached_lock_for_pid_equal,
                        &conflict_found);
  if (conflict_found) {
    return conflicting_lock.release();
  }
  // We allow only one lock per PID, i.e. an existing lock can be always
  // overwritten.
  if (lock_for_pid_cached) {
    conflicting_lock->CopyFrom(lock_request.lock_request());
    return conflicting_lock.release();
  }

  // Cache could not be used. Complete lockRequest and send to OSD.
  file_info_->GetXLocSet(
      lock_request.mutable_file_credentials()->mutable_xlocs());
  xcap_manager_.GetXCap(
      lock_request.mutable_file_credentials()->mutable_xcap());

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
    ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_check_sync,
            osd_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials_bogus_),
            &lock_request),
        osd_uuid_iterator_,
        uuid_resolver_,
        RPCOptionsFromOptions(volume_options_),
        false,
        &xcap_manager_,
        lock_request.mutable_file_credentials()->mutable_xcap()));
  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  return static_cast<xtreemfs::pbrpc::Lock*>(response->response());
}

void FileHandleImplementation::ReleaseLock(
    int process_id,
    uint64_t offset,
    uint64_t length,
    bool exclusive) {
  Lock lock;
  lock.set_client_uuid(client_uuid_);
  lock.set_client_pid(process_id);
  lock.set_offset(offset);
  lock.set_length(length);
  lock.set_exclusive(exclusive);
  ReleaseLock(lock);
}

void FileHandleImplementation::ReleaseLock(
    const xtreemfs::pbrpc::Lock& lock) {
  boost::function<void()> operation(
      boost::bind(&FileHandleImplementation::DoReleaseLock, this, lock));
  ExecuteViewCheckedOperation(operation);
}

void FileHandleImplementation::DoReleaseLock(
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
  xcap_manager_.GetXCap(
      unlock_request.mutable_file_credentials()->mutable_xcap());
  unlock_request.mutable_lock_request()->CopyFrom(lock);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
    ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_lock_release_sync,
            osd_service_client_,
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials_bogus_),
            &unlock_request),
        osd_uuid_iterator_,
        uuid_resolver_,
        RPCOptionsFromOptions(volume_options_),
        false,
        &xcap_manager_,
        unlock_request.mutable_file_credentials()->mutable_xcap()));
  response->DeleteBuffers();

  file_info_->DelLock(lock);
}

void FileHandleImplementation::ReleaseLockOfProcess(int process_id) {
  file_info_->ReleaseLockOfProcess(this, process_id);
}

void FileHandleImplementation::PingReplica(
    const std::string& osd_uuid) {
  boost::function<void()> operation(
      boost::bind(&FileHandleImplementation::DoPingReplica, this, osd_uuid));
  ExecuteViewCheckedOperation(operation);
}

void FileHandleImplementation::DoPingReplica(
    const std::string& osd_uuid) {
  // Get xlocset. and check if osd_uuid is included.
  readRequest read_request;
  xcap_manager_.GetXCap(
      read_request.mutable_file_credentials()->mutable_xcap());
  read_request.set_file_id(read_request.file_credentials().xcap().file_id());
  file_info_->GetXLocSet(
      read_request.mutable_file_credentials()->mutable_xlocs());
  const XLocSet& xlocs = read_request.file_credentials().xlocs();

  // Check if osd_uuid is part of the xlocset.
  if (xlocs.replicas_size() == 0) {
    throw UUIDNotInXlocSetException("The XlocSet contains no replicas.");
  }
  bool uuid_found = false;
  for (int i = 0; i < xlocs.replicas_size(); i++) {
    // TODO(mberlin): Every OSD in a striped replica has to be pinged.
    // Always check only the head OSD.
    if (xlocs.replicas(i).osd_uuids(0) == osd_uuid) {
      uuid_found = true;
      // Check replication flags, if it's a full replica.
      if (xlocs.replica_update_policy() == "ronly" &&
          !(xlocs.replicas(i).replication_flags() & REPL_FLAG_FULL_REPLICA)) {
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

  SimpleUUIDIterator temp_uuid_iterator;
  temp_uuid_iterator.AddUUID(osd_uuid);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(&xtreemfs::pbrpc::OSDServiceClient::read_sync,
              osd_service_client_,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials_bogus_),
              &read_request),
          &temp_uuid_iterator,
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_),
          false,
          &xcap_manager_,
          read_request.mutable_file_credentials()->mutable_xcap()));
  // We don't care about the result.
  response->DeleteBuffers();
}

void FileHandleImplementation::Close() {
  try {
    Flush(true);  // true = Tell Flush() the file will be closed.
  } catch(const XtreemFSException&) {
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

const StripeTranslator* FileHandleImplementation::GetStripeTranslator(
    xtreemfs::pbrpc::StripingPolicyType type) {
  // Find the corresponding StripingPolicy.
  map<StripingPolicyType, StripeTranslator*>::const_iterator it
    = stripe_translators_.find(type);

  // Type not found.
  if (it == stripe_translators_.end()) {
    throw XtreemFSException("No StripingPolicy found for type: " + StripePolicyTypeToString(type));
  } else {
    // Type found.
    return it->second;
  }
}

void FileHandleImplementation::MarkAsyncWritesAsFailed() {
  boost::mutex::scoped_lock lock(mutex_);
  async_writes_failed_ = true;
}

bool FileHandleImplementation::DidAsyncWritesFail() {
  boost::mutex::scoped_lock lock(mutex_);
  return async_writes_failed_;
}

void FileHandleImplementation::ThrowIfAsyncWritesFailed() {
  if (DidAsyncWritesFail()) {
    throw PosixErrorException(POSIX_ERROR_EIO, "A previous asynchronous write"
        " did fail. No more actions on this file handle are allowed.");
  }
}

void FileHandleImplementation::WriteBackFileSize(
    const xtreemfs::pbrpc::OSDWriteResponse& owr,
    bool close_file) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "WriteBackFileSize: file_id: " << xcap_manager_.GetFileId()
        << " # bytes: " << owr.size_in_bytes()
        << " close file? " << close_file
        << endl;
  }

  xtreemfs_update_file_sizeRequest rq;
  xcap_manager_.GetXCap(rq.mutable_xcap());
  rq.mutable_osd_write_response()->CopyFrom(owr);
  rq.set_close_file(close_file);

  // Set vivaldi coordinates if vivaldi is enabled.
  // According to UpdateFileSizeOperation.java sent coordinates are only
  // evaluated if close_file in the request is set to true.
  if (close_file && volume_options_.vivaldi_enable) {
    rq.mutable_coordinates()->CopyFrom(this->client_->GetVivaldiCoordinates());
  }

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
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
          RPCOptionsFromOptions(volume_options_),
          false,
          &xcap_manager_,
          rq.mutable_xcap()));
  response->DeleteBuffers();
}

void FileHandleImplementation::WriteBackFileSizeAsync(const RPCOptions& options) { // NOLINT
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
    rq.mutable_osd_write_response()
        ->CopyFrom(*osd_write_response_for_async_write_back_);
  }
  xcap_manager_.GetXCap(rq.mutable_xcap());
  // Set to false because a close would use a sync writeback.
  rq.set_close_file(false);

  // NOTE: no vivaldi coordinates needed since close_file is set false.

  try {
    string mrc_uuid;
    string mrc_address;
    mrc_uuid_iterator_->GetUUID(&mrc_uuid);
    uuid_resolver_->UUIDToAddressWithOptions(mrc_uuid, &mrc_address, options);
    mrc_service_client_->xtreemfs_update_file_size(mrc_address,
                                                   auth_bogus_,
                                                   user_credentials_bogus_,
                                                   &rq,
                                                   this,
                                                   NULL);
  } catch (const XtreemFSException&) {
    // Do nothing.
  }
}

void FileHandleImplementation::CallFinished(
    xtreemfs::pbrpc::timestampResponse* response_message,
    char* data,
    uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  boost::scoped_ptr<timestampResponse> autodelete_xcap(response_message);
  boost::scoped_ptr<RPCHeader::ErrorResponse> autodelete_error(error);
  boost::scoped_array<char> autodelete_data(data);
  if (error) {
    string path;
    file_info_->GetPath(&path);
    LogLevel level = LEVEL_WARN;
    if (error->posix_errno() == POSIX_ERROR_ENOENT) {
      level = LEVEL_DEBUG;
    }
    string error_msg = "Async filesize update for file: " + path
        + " failed. Error: " + error->DebugString();
    if (Logging::log->loggingActive(level)) {
      Logging::log->getLog(level) << error_msg << endl;
    }
    if (level != LEVEL_DEBUG) {
      ErrorLog::error_log->AppendError(error_msg);
    }
  }

  file_info_->AsyncFileSizeUpdateResponseHandler(
      *(osd_write_response_for_async_write_back_.get()),
      this,
      error == NULL);
}

void FileHandleImplementation::set_osd_write_response_for_async_write_back(
    const xtreemfs::pbrpc::OSDWriteResponse& owr) {
  boost::mutex::scoped_lock lock(mutex_);

  // Per file size update a new FileHandle will be created, i.e. do not allow
  // to set an OSD Write Response twice.
  assert(!osd_write_response_for_async_write_back_.get());
  osd_write_response_for_async_write_back_.reset(new OSDWriteResponse(owr));
}

void FileHandleImplementation::WaitForAsyncOperations() {
  xcap_manager_.WaitForPendingXCapRenewal();
}

void FileHandleImplementation::ExecutePeriodTasks(const RPCOptions& options) {
  xcap_manager_.RenewXCapAsync(options);
}

void FileHandleImplementation::GetXCap(xtreemfs::pbrpc::XCap* xcap) {
  assert(xcap);
  xcap_manager_.GetXCap(xcap);
}

void FileHandleImplementation::RenewXLocSet() {
  XLocSet xlocset_to_renew, xlocset_current;

  // Store the current xLocSet before entering the renewal mutex section.
  file_info_->GetXLocSet(&xlocset_to_renew);

  {
    FileInfo::XLocSetRenewalLock lock(file_info_);

    // Renew the xLocSet if it has not been renewed yet by another process.
    file_info_->GetXLocSet(&xlocset_current);
    if (xlocset_current.version() <= xlocset_to_renew.version()) {
      // Build the request and call the MRC synchronously.
      xtreemfs_get_xlocsetRequest request;
      XCap* xcap_in_req = request.mutable_xcap();
      GetXCap(xcap_in_req);

      boost::scoped_ptr<rpc::SyncCallbackBase> response(
          ExecuteSyncRequest(
              boost::bind(
                  &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_get_xlocset_sync,
                  mrc_service_client_,
                  _1,
                  boost::cref(auth_bogus_),
                  boost::cref(user_credentials_bogus_),
                  &request),
              mrc_uuid_iterator_,
              uuid_resolver_,
              RPCOptionsFromOptions(volume_options_),
              false,
              &xcap_manager_,
              xcap_in_req));

      xtreemfs::pbrpc::XLocSet* xlocset_new =
          static_cast<xtreemfs::pbrpc::XLocSet*>(response->response());

      file_info_->UpdateXLocSetAndRest(*xlocset_new);

      response->DeleteBuffers();
    }
  }
}

XCapManager::XCapManager(
    const xtreemfs::pbrpc::XCap& xcap,
    xtreemfs::pbrpc::MRCServiceClient* mrc_service_client,
    UUIDResolver* uuid_resolver,
    UUIDIterator* mrc_uuid_iterator,
    const xtreemfs::pbrpc::Auth& auth_bogus,
    const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus) :
        xcap_(xcap),
        xcap_renewal_pending_(false),
        mrc_service_client_(mrc_service_client),
        uuid_resolver_(uuid_resolver),
        mrc_uuid_iterator_(mrc_uuid_iterator),
        auth_bogus_(auth_bogus),
        user_credentials_bogus_(user_credentials_bogus) {}

void XCapManager::WaitForPendingXCapRenewal() {
  boost::mutex::scoped_lock lock(mutex_);
  while (xcap_renewal_pending_) {
    xcap_renewal_pending_cond_.wait(lock);
  }
}

void XCapManager::GetXCap(xtreemfs::pbrpc::XCap* xcap) {
  assert(xcap);

  boost::mutex::scoped_lock lock(mutex_);
  xcap->CopyFrom(xcap_);
}

void XCapManager::SetXCap(const xtreemfs::pbrpc::XCap& xcap) {
  boost::mutex::scoped_lock lock(mutex_);
  xcap_.CopyFrom(xcap);
}

uint64_t XCapManager::GetFileId() {
  boost::mutex::scoped_lock lock(mutex_);
  return ExtractFileIdFromXCap(xcap_);
}

void XCapManager::RenewXCapAsync(const RPCOptions& options) {
  // TODO(mberlin): Only renew after some time has elapsed.
  // TODO(mberlin): Cope with local clocks which have a high clock skew.
  {
    boost::mutex::scoped_lock lock(mutex_);
    if (xcap_renewal_pending_) {
      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "XCap renew already in progress, ignoring. file_id: "
            << GetFileId() << " Expiration in: "
            << (xcap_.expire_time_s() - time(NULL))
            << endl;
      }
      return;
    }

    xcap_renewal_pending_ = true;
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "Renew XCap for file_id: " << GetFileId()
        << " Expiration in: " << (xcap_.expire_time_s() - time(NULL))
        << endl;
  }

  XCap xcap;
  GetXCap(&xcap);

  string mrc_uuid;
  string mrc_address;
  try {
    mrc_uuid_iterator_->GetUUID(&mrc_uuid);
    uuid_resolver_->UUIDToAddressWithOptions(mrc_uuid, &mrc_address, options);
    mrc_service_client_->xtreemfs_renew_capability(
        mrc_address,
        auth_bogus_,
        user_credentials_bogus_,
        &xcap,
        this,
        NULL);
  } catch (const XtreemFSException&) {
    // do nothing.
  }
}

void XCapManager::CallFinished(
    xtreemfs::pbrpc::XCap* new_xcap,
    char* data,
    uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  boost::scoped_ptr<XCap> autodelete_xcap(new_xcap);
  boost::scoped_ptr<RPCHeader::ErrorResponse> autodelete_error(error);
  boost::scoped_array<char> autodelete_data(data);

  if (error != NULL) {
    Logging::log->getLog(LEVEL_ERROR)
        << "Renewing XCap of file: " << GetFileId()
        << " failed. Error: " << error->DebugString() << endl;
    ErrorLog::error_log->AppendError(
        "Renewing XCap failed: " + error->DebugString());
  } else {
    // Overwrite current XCap only by a newer one (i.e. later expire time).
    if (new_xcap->expire_time_s() > xcap_.expire_time_s()) {
      SetXCap(*new_xcap);

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "XCap renewed for file_id: " << GetFileId() << endl;
      }
    }
  }
  boost::mutex::scoped_lock lock(mutex_);
  xcap_renewal_pending_ = false;
  xcap_renewal_pending_cond_.notify_all();
}

}  // namespace xtreemfs
