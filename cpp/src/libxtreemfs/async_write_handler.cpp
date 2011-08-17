/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/async_write_handler.h"

#include <cassert>

#include <boost/lexical_cast.hpp>
#include <google/protobuf/descriptor.h>
#include <string>

#include "libxtreemfs/async_write_buffer.h"
#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/file_info.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"
#include "util/error_log.h"
#include "util/logging.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::util;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

AsyncWriteHandler::AsyncWriteHandler(
    FileInfo* file_info,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    xtreemfs::pbrpc::OSDServiceClient* osd_service_client,
    const xtreemfs::pbrpc::Auth& auth_bogus,
    const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus,
    int max_writeahead,
    int max_write_tries)
    : state_(IDLE),
      writing_paused(false),
      pending_bytes_(0),
      file_info_(file_info),
      uuid_iterator_(uuid_iterator),
      uuid_resolver_(uuid_resolver),
      osd_service_client_(osd_service_client),
      auth_bogus_(auth_bogus),
      user_credentials_bogus_(user_credentials_bogus),
      max_writeahead_(max_writeahead),
      max_write_tries_(max_write_tries) {
  assert(file_info && uuid_iterator && uuid_resolver && osd_service_client);
}

AsyncWriteHandler::~AsyncWriteHandler() {
  if (pending_bytes_ > 0) {
    string path;
    file_info_->GetPath(&path);
    string error = "The AsyncWriteHandler for the file with the path: " + path
        + " has pending writes left. This should NOT happen.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    xtreemfs::util::ErrorLog::error_log->AppendError(error);
  }
  if (waiting_observers_.size() > 0) {
    string path;
    file_info_->GetPath(&path);
    string error = "The AsyncWriteHandler for the file"
        " with the path: " + path + " has remaining observers (calls waiting"
        " for the completion of pending writes) left. This should NOT happen.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    xtreemfs::util::ErrorLog::error_log->AppendError(error);
  }
  for (list<WaitForCompletionObserver*>::iterator it
           = waiting_observers_.begin();
       it != waiting_observers_.end();
       ++it) {
    delete *it;
  }
}

void AsyncWriteHandler::Write(AsyncWriteBuffer* write_buffer) {
  assert(write_buffer);

  if (write_buffer->data_length > max_writeahead_) {
    throw XtreemFSException("The maximum allowed writeahead size: "
        + boost::lexical_cast<string>(max_writeahead_)
        + " is smaller than the size of this write request: "
        + boost::lexical_cast<string>(write_buffer->data_length));
  }

  // Append to list of writes in flight.
  {
    boost::mutex::scoped_lock lock(mutex_);
    // Block if there are currently no writes allowed OR the writeahead is
    // exceeded.
    while (writing_paused || (pending_bytes_ + write_buffer->data_length) >
                             max_writeahead_) {
      // TODO(mberlin): Allow interruption and set the write status of the
      //                FileHandle of the interrupted write to an error state.
      pending_bytes_were_decreased_.wait(lock);
    }

    IncreasePendingBytesHelper(write_buffer, &lock);
  }

  // Retrieve address for UUID.
  string osd_uuid, osd_address;
  if (write_buffer->use_uuid_iterator) {
    uuid_iterator_->GetUUID(&osd_uuid);
    // Store used OSD in write_buffer for the callback.
    write_buffer->osd_uuid = osd_uuid;
  } else {
    osd_uuid = write_buffer->osd_uuid;
  }
  try {
    uuid_resolver_->UUIDToAddress(osd_uuid, &osd_address);
  } catch(const exception& e) {
    // In case of errors, remove write again and throw exception.
    {
      boost::mutex::scoped_lock lock(mutex_);

      DecreasePendingBytesHelper(write_buffer, &lock);
    }
    throw;
  }

  // Send out request.
  osd_service_client_->write(osd_address,
                             auth_bogus_,
                             user_credentials_bogus_,
                             write_buffer->write_request,
                             write_buffer->data,
                             write_buffer->data_length,
                             this,
                             reinterpret_cast<void*>(write_buffer));
}

void AsyncWriteHandler::WaitForPendingWrites() {
  boost::mutex::scoped_lock lock(mutex_);
  if (state_ != IDLE) {
    writing_paused = true;
    while (state_ != IDLE) {
      // TODO(mberlin): Saw a Read() hanging here infinitely when running
      //                dbench (in the cleanup phase?). Find cause and fix it.
      all_pending_writes_did_complete_.wait(lock);
    }
  }
}

/*
 * @param condition_variable notify_one() is called on it if there are no more
 *                           pending writes.
 * @param wait_completed     Set to true if the pending async writes did finish.
 *                           Needed for observers who wait on condition_variable
 *                           and may be subject to spurious wake ups.
 * @param wait_completed_mutex  Mutex of observer which guards wait_completed.
 *
 * @return  True if the wait would have blocked and an observer was registered,
 *          otherwise false (i. e. the observer was not registered).
 *
 * @remark  Ownership is not transferred to the caller.
 */
bool AsyncWriteHandler::WaitForPendingWritesNonBlocking(
    boost::condition* condition_variable,
    bool* wait_completed,
    boost::mutex* wait_completed_mutex) {
  assert(condition_variable && wait_completed && wait_completed_mutex);
  boost::mutex::scoped_lock lock(mutex_);
  
  if (state_ != IDLE) {
    writing_paused = true;
    waiting_observers_.push_back(new WaitForCompletionObserver(
        condition_variable,
        wait_completed,
        wait_completed_mutex));
    return true;
  } else {
    *wait_completed = true;
    return false;
  }
}

void AsyncWriteHandler::CallFinished(
    xtreemfs::pbrpc::OSDWriteResponse* response_message,
    char* data,
    boost::uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  AsyncWriteBuffer* write_buffer = reinterpret_cast<AsyncWriteBuffer*>(context);
  bool delete_response_message = true;

  if (error) {
    // An error occured.
    // No retry supported yet, just acknowledge the write as failed.

    // Tell FileHandle that its async write status is broken from now on.
    write_buffer->file_handle->MarkAsyncWritesAsFailed();

    // Log error.
    string error_type_name = boost::lexical_cast<string>(error->error_type());
    const ::google::protobuf::EnumValueDescriptor* enum_desc =
        ErrorType_descriptor()->FindValueByNumber(error->error_type());
    if (enum_desc) {
      error_type_name = enum_desc->name();
    }
    string error_message = "An async write sent to the server "
        + write_buffer->osd_uuid + " failed."
        + " Error type: " + error_type_name
        + " Error message: " + error->error_message()
        + " Complete error header: " + error->DebugString();
    Logging::log->getLog(LEVEL_ERROR) << error_message << endl;
    ErrorLog::error_log->AppendError(error_message);

    {
      boost::mutex::scoped_lock lock(mutex_);
      DecreasePendingBytesHelper(write_buffer, &lock);
    }
  } else {
    // Write was successful.

    // Tell FileInfo about the OSDWriteResponse.
    if (response_message->has_size_in_bytes()) {
      XCap xcap;
      write_buffer->file_handle->GetXCap(&xcap);
      if (file_info_->TryToUpdateOSDWriteResponse(response_message, xcap)) {
        // Ownership of response_message was transferred, do not delete it.
        delete_response_message = false;
      }
    }

    {
      boost::mutex::scoped_lock lock(mutex_);
      DecreasePendingBytesHelper(write_buffer, &lock);
    }
  }

  // Cleanup.
  if (delete_response_message) {
    delete response_message;
  }
  delete data;
  delete error;
}

void AsyncWriteHandler::IncreasePendingBytesHelper(
    AsyncWriteBuffer* write_buffer,
    boost::mutex::scoped_lock* lock) {
  assert(write_buffer && lock && lock->owns_lock());

  pending_bytes_ += write_buffer->data_length;
  writes_in_flight_.push_back(write_buffer);

  state_ = WRITES_PENDING;
}

void AsyncWriteHandler::DecreasePendingBytesHelper(
    AsyncWriteBuffer* write_buffer,
    boost::mutex::scoped_lock* lock) {
  assert(write_buffer && lock && lock->owns_lock());

  writes_in_flight_.remove(write_buffer);
  pending_bytes_ -= write_buffer->data_length;
  delete write_buffer;

  if (pending_bytes_ == 0) {
    state_ = IDLE;
    if (writing_paused) {
      writing_paused = false;
      NotifyWaitingObserversAndClearAll(lock);
      all_pending_writes_did_complete_.notify_all();
    }
  }
  // Tell blocked writers there may be enough space/writing was unpaused now.
  pending_bytes_were_decreased_.notify_all();
}

void AsyncWriteHandler::NotifyWaitingObserversAndClearAll(
    boost::mutex::scoped_lock* lock) {
  assert(lock && lock->owns_lock());

  // Tell waiting observers that the write did finish.
  for (list<WaitForCompletionObserver*>::iterator it
           = waiting_observers_.begin();
       it != waiting_observers_.end();
       ++it) {
    boost::mutex::scoped_lock lock(*((*it)->wait_completed_mutex));
    *((*it)->wait_completed) = true;
    (*it)->condition_variable->notify_one();
    delete *it;
  }
  waiting_observers_.clear();
}

}  // namespace xtreemfs
