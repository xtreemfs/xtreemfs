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
#include "libxtreemfs/interrupt.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"
#include "util/error_log.h"
#include "util/logging.h"
#include "util/synchronized_queue.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::util;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

util::SynchronizedQueue<AsyncWriteHandler::CallbackEntry> AsyncWriteHandler::callback_queue;

AsyncWriteHandler::AsyncWriteHandler(
    FileInfo* file_info,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    xtreemfs::pbrpc::OSDServiceClient* osd_service_client,
    const xtreemfs::pbrpc::Auth& auth_bogus,
    const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus,
    const Options& volume_options)
    : state_(IDLE),
      pending_bytes_(0),
      pending_writes_(0),
      writing_paused_(false),
      waiting_blocking_threads_count_(0),
      file_info_(file_info),
      uuid_iterator_(uuid_iterator),
      uuid_resolver_(uuid_resolver),
      osd_service_client_(osd_service_client),
      auth_bogus_(auth_bogus),
      user_credentials_bogus_(user_credentials_bogus),
      volume_options_(volume_options),
      max_writeahead_(volume_options.max_writeahead),
      max_writeahead_requests_(volume_options.max_writeahead_requests),
      max_write_tries_(volume_options.max_write_tries),
      redirected_(false),
      fast_redirect_(false),
      worst_write_buffer_(0) {
  assert(file_info && uuid_iterator && uuid_resolver && osd_service_client);
}

AsyncWriteHandler::~AsyncWriteHandler() {
  if (pending_bytes_ > 0) {
    string path;
    file_info_->GetPath(&path);
    string error = "The AsyncWriteHandler for the file with the path: " + path
        + " has pending writes left. This should NOT happen.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
  }
  if (waiting_blocking_threads_count_ > 0) {
    string path;
    file_info_->GetPath(&path);
    string error = "The AsyncWriteHandler for the file"
        " with the path: " + path + " has remaining blocked threads waiting"
        " for the completion of pending writes left. This should NOT happen.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
  }
  if (waiting_observers_.size() > 0) {
    string path;
    file_info_->GetPath(&path);
    string error = "The AsyncWriteHandler for the file"
        " with the path: " + path + " has remaining observers (calls waiting"
        " for the completion of pending writes) left. This should NOT happen.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
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

    while (state_ != FINALLY_FAILED && (writing_paused_ ||
           (pending_bytes_ + write_buffer->data_length) > max_writeahead_ ||
            writes_in_flight_.size() == max_writeahead_requests_)) {
      // TODO(mberlin): Allow interruption and set the write status of the
      //                FileHandle of the interrupted write to an error state.
      pending_bytes_were_decreased_.wait(lock);
    }

    // NOTE: the following is done here to reach all threads that started
    //       waiting before the final failure
    if (state_ == FINALLY_FAILED) {
      string error =
          "Tried to asynchronously write to a finally failed write handler.";
      Logging::log->getLog(LEVEL_ERROR) << error << endl;
      throw PosixErrorException(POSIX_ERROR_EIO, error);
    }

    ++pending_writes_;
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

      DecreasePendingBytesHelper(write_buffer, &lock, true);
    }
    throw;
  }

  // Send out request.
  write_buffer->request_sent_time =
      boost::posix_time::microsec_clock::local_time();
  osd_service_client_->write(osd_address,
                             auth_bogus_,
                             user_credentials_bogus_,
                             write_buffer->write_request,
                             write_buffer->data,
                             write_buffer->data_length,
                             this,
                             reinterpret_cast<void*>(write_buffer));
}

void AsyncWriteHandler::ReWrite(AsyncWriteBuffer* write_buffer,
                                bool copy_buffer,
                                boost::mutex::scoped_lock* lock) {
  assert(write_buffer && lock && lock->owns_lock() &&
         (state_ == HAS_FAILED_WRITES));

  write_buffer->retry_count_++;
  write_buffer->state_ = AsyncWriteBuffer::PENDING;
  ++pending_writes_;

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
    // In case of errors, throw exception.
    throw;
  }

  // Send out request.
  write_buffer->request_sent_time =
      boost::posix_time::microsec_clock::local_time();
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
  if ((state_ != IDLE) && (state_ != FINALLY_FAILED)) {
    writing_paused_ = true;
    waiting_blocking_threads_count_++;
    while ((state_ != IDLE) && (state_ != FINALLY_FAILED) ) {
      all_pending_writes_did_complete_.wait(lock);
    }
    waiting_blocking_threads_count_--;
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
  
  if ((state_ != IDLE)  && (state_ != FINALLY_FAILED)) {
    writing_paused_ = true;
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


void AsyncWriteHandler::ProcessCallbacks() {
  while(true) {
    const CallbackEntry& e = callback_queue.Dequeue();
    e.handler_->HandleCallback(
        e.response_message_,
        e.data_,
        e.data_length_,
        e.error_,
        e.context_);
    boost::this_thread::interruption_point();
  }
}

void AsyncWriteHandler::CallFinished(
    xtreemfs::pbrpc::OSDWriteResponse* response_message,
    char* data,
    boost::uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  callback_queue.Enqueue(CallbackEntry(
      this,
      response_message,
      data,
      data_length,
      error,
      context
      ));
}

void AsyncWriteHandler::HandleCallback(
    xtreemfs::pbrpc::OSDWriteResponse* response_message,
    char* data,
    boost::uint32_t data_length,
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    void* context) {
  boost::mutex::scoped_lock lock(mutex_);

  bool delete_response_message = true;

  --pending_writes_; // we received some answer we were waiting for

  // do nothing in case a write has finally failed
  if (state_ !=  FINALLY_FAILED) {
    AsyncWriteBuffer* write_buffer = reinterpret_cast<AsyncWriteBuffer*>(context);

    if (error) {
      write_buffer->state_ = AsyncWriteBuffer::FAILED;
      writing_paused_ = true;  // forbid new writes

      if(state_ != HAS_FAILED_WRITES) {
          state_ = HAS_FAILED_WRITES;
          worst_error_.MergeFrom(*error);
          worst_write_buffer_ = write_buffer;
      }

      bool delay_last_attempt = false;
      // TODO(mno): set this ^ somehow if needed: michael:
      // das brauch man dann, wenn man sich selber um den retry kuemmert, z. b. in FileHandleImplementation::AcquireLock()
      // bei connection refused kommt der request sofort zurueck (vor den 120 sekunden timeout) und dann muss man noch warten
      // aehnlich beim zweiten redirect

      // Resolve UUID first.
      std::string service_uuid = "";
      std::string service_address = "";
      uuid_iterator_->GetUUID(&service_uuid);
      uuid_resolver_->UUIDToAddress(service_uuid, &service_address);

      if (((write_buffer->retry_count_ < max_write_tries_ || max_write_tries_ == 0) ||
          // or this last retry should be delayed
          (write_buffer->retry_count_ == max_write_tries_ && delay_last_attempt)) &&
          // AND it is an recoverable error.
          (error->error_type() == xtreemfs::pbrpc::IO_ERROR ||
           error->error_type() == xtreemfs::pbrpc::INTERNAL_SERVER_ERROR ||  // NOLINT
           error->error_type() == xtreemfs::pbrpc::REDIRECT)) {
        std::string error_str;
        xtreemfs::util::LogLevel level = xtreemfs::util::LEVEL_ERROR;

        // Special handling of REDIRECT "errors".
        if (error->error_type() == xtreemfs::pbrpc::REDIRECT) {
          assert(error->has_redirect_to_server_uuid());

          // set the current error as new worst error if it is worse:
          // REDIRECT is worse than other error types and worse than a
          // previous REDIRECT error if it is from later sent request
          if ((worst_error_.error_type() != xtreemfs::pbrpc::REDIRECT) ||
              (worst_write_buffer_->request_sent_time <
               write_buffer->request_sent_time)) {
              worst_error_.CopyFrom(*error);
          }

          // Log the redirect.
          level = xtreemfs::util::LEVEL_INFO;
          error_str = "The server with the UUID: " + service_uuid
              + " redirected to the current master with the UUID: "
              + error->redirect_to_server_uuid()
              + " after attempt: "
              + boost::lexical_cast<std::string>(write_buffer->retry_count_);
          if (xtreemfs::util::Logging::log->loggingActive(level)) {
            xtreemfs::util::Logging::log->getLog(level) << error_str << std::endl;
          }
        } else {
          // Communication error or Internal Server Error.

          // set the current error as new worst error if it is worse:
          // a non-REDIRECT error is worse than another non-REDIRECT error
          // from a request with an earlier time stamp
          if ((worst_error_.error_type() != xtreemfs::pbrpc::REDIRECT) &&
              (worst_write_buffer_->request_sent_time <
               write_buffer->request_sent_time)) {
              worst_error_.CopyFrom(*error);
          }

          // Log only the first retry.
          if (write_buffer->retry_count_ == 1 && max_write_tries_ != 1) {
            std::string retries_left = max_write_tries_ == 0 ? "infinite"
                : boost::lexical_cast<std::string>(max_write_tries_
                    - write_buffer->retry_count_);
            error_str = "Got no response from server "
                + service_address + " (" + service_uuid + ")"
                + ", retrying ("
                + boost::lexical_cast<std::string>(retries_left)
                + " attempts left)";
            if (xtreemfs::util::Logging::log->loggingActive(level)) {
              xtreemfs::util::Logging::log->getLog(level) << error_str << std::endl;
            }

          }

        }
      } else { // if (recoverable error and retries left)
        // FAIL finally after too many retries, or unrecoverable errors
        state_ = FINALLY_FAILED;
        // finall cleanup is done when the last expected callback arrives

        // Log error.
        string error_type_name = boost::lexical_cast<string>(error->error_type());
        const ::google::protobuf::EnumValueDescriptor* enum_desc =
            ErrorType_descriptor()->FindValueByNumber(error->error_type());
        if (enum_desc) {
          error_type_name = enum_desc->name();
        }
        string error_message = "An async write sent to the server "
            + write_buffer->osd_uuid + " failed finally."
            + " Error type: " + error_type_name
            + " Error message: " + error->error_message()
            + " Complete error header: " + error->DebugString();
        Logging::log->getLog(LEVEL_ERROR) << error_message << endl;
        ErrorLog::error_log->AppendError(error_message);

        // Cleanup before throwing (normally done at the end)
        if (delete_response_message) {
          delete response_message;
        }
        delete [] data;
        delete error;
      }
    } else { // if (error)
      // Write was successful.
      if (state_ != HAS_FAILED_WRITES) {
        // Tell FileInfo about the OSDWriteResponse.
        if (response_message->has_size_in_bytes()) {
          XCap xcap;
          write_buffer->file_handle->GetXCap(&xcap);
          if (file_info_->TryToUpdateOSDWriteResponse(response_message, xcap)) {
            // Ownership of response_message was transferred, do not delete it.
            delete_response_message = false;
          }
        }
      }

      write_buffer->state_ = AsyncWriteBuffer::SUCCEEDED;
      DeleteBufferHelper(&lock);  // do all deletes
    }

    // start retrying when this is the callback of the last response
    // all handling of fails is done here
    if ((state_ == HAS_FAILED_WRITES) && (pending_writes_ == 0)) {

      // handle all errors according to the most relevant one
      // NOTE: only handle-able errors with enough retries can make it
      //       until here

      if (worst_error_.error_type() == xtreemfs::pbrpc::REDIRECT) {
        uuid_iterator_->SetCurrentUUID(worst_error_.redirect_to_server_uuid());
        // first fast reconnect
        if (!redirected_) {
          redirected_ = true;
          fast_redirect_ = true;
        }
      } else {
        // Mark the current UUID as failed and get the next one.
        uuid_iterator_->MarkUUIDAsFailed(worst_write_buffer_->osd_uuid);
      }

      // delay retries to avoid flooding.
      // delay = retry_delay - (current_time - request_sent_time)
      boost::posix_time::time_duration delay_time_left =
          boost::posix_time::seconds(volume_options_.retry_delay_s) -  // delay
          (boost::posix_time::microsec_clock::local_time() -   // current time
           worst_write_buffer_->request_sent_time);

      // Log time left
      if (xtreemfs::util::Logging::log->loggingActive(xtreemfs::util::LEVEL_INFO)) {
        xtreemfs::util::Logging::log->getLog(xtreemfs::util::LEVEL_INFO)
            << "Retrying. Waiting " << boost::lexical_cast<std::string>(
                (delay_time_left.is_negative() || fast_redirect_) ? 0 :
                    delay_time_left.total_seconds())
            << " more seconds till next retry."
            << std::endl;
      }

      if(!(fast_redirect_ || delay_time_left.is_negative())){
        try {
          sleep_interruptible(delay_time_left.total_milliseconds());  // boost::thread interruption point
        } catch (const boost::thread_interrupted& e) {
          // Cleanup.
          if (delete_response_message) {
            delete response_message;
          }
          delete [] data;
          delete error;

          throw;
        }
      } else {
          fast_redirect_ = false;
      }

      // rewrite all in list (leading successfully sent entries have been
      // deleted by the DeleteBufferHelper() call above)
      std::list<AsyncWriteBuffer*>::iterator it;
      for(it = writes_in_flight_.begin(); it != writes_in_flight_.end(); ++it) {
        ReWrite(*it, false, &lock);
      }
      // reset states
      state_ = WRITES_PENDING;
      worst_error_.Clear();
      worst_write_buffer_ = 0;
    }
  } else { // state_ == FINALLY_FAILED
      // clean up when last expected callback arrives
      if(pending_writes_ == 0) {
          CleanUp(&lock);
      }

  } // if (state_ != FINALLY_FAILED)

  // Cleanup.
  if (delete_response_message) {
    delete response_message;
  }
  delete [] data;
  delete error;
}

void AsyncWriteHandler::IncreasePendingBytesHelper(
    AsyncWriteBuffer* write_buffer,
    boost::mutex::scoped_lock* lock) {
  assert(write_buffer && lock && lock->owns_lock());

  pending_bytes_ += write_buffer->data_length;
  writes_in_flight_.push_back(write_buffer);
  assert(writes_in_flight_.size() <= max_writeahead_requests_);

  state_ = WRITES_PENDING;
}

void AsyncWriteHandler::DecreasePendingBytesHelper(
    AsyncWriteBuffer* write_buffer,
    boost::mutex::scoped_lock* lock,
    bool delete_buffer) {
  assert(write_buffer && lock && lock->owns_lock());

  pending_bytes_ -= write_buffer->data_length;

  if(delete_buffer) {
    // the buffer is deleted
    writes_in_flight_.remove(write_buffer);
    delete write_buffer;
  }

  if (pending_bytes_ == 0) {
    state_ = IDLE;
    redirected_ = false;
    fast_redirect_ = false;

    if (writing_paused_) {
      writing_paused_ = false;
      NotifyWaitingObserversAndClearAll(lock);
    }
    // Issue notify_all as long as there are remaining blocked threads.
    //
    // Please note the following here: After the two notify_all()s on the
    // condition variables all_pending_writes_did_complete_ and
    // pending_bytes_were_decreased_, two different thread types
    // (waiting blocked ones AND further waiting writes) do race for
    // re-acquiring the lock on mutex_.
    // Example:
    // T1: write1           state_ = PENDING
    // T2: getattr          writing_paused_ = true => blocked as state_ != IDLE
    // T1: write2   =>  blocked as writing_paused_ = true
    // Tx: write1 callback: state = IDLE, writing_paused_ = false
    // T1: write2 succeeds to obtain lock on mutex_ *before* getattr
    //              => state = IDLE (writing_paused_ remains false)
    // Tx: write2 callback: state = IDLE, writing paused remains false
    //     - however its necessary to notify the blocked getattr.
    // As you can see the order of concurrent writes and reads/getattrs
    // is undefined and we don't enforce any order as it's up to the user to
    // synchronize his threads himself when working on the same file.
    if (waiting_blocking_threads_count_ > 0) {
      all_pending_writes_did_complete_.notify_all();
    }
  }
  // Tell blocked writers there may be enough space/writing was unpaused now.
  pending_bytes_were_decreased_.notify_all();
}

void AsyncWriteHandler::DeleteBufferHelper(
    boost::mutex::scoped_lock* lock) {
  assert(lock && lock->owns_lock());

  // delete all leading successfully sent entries
  std::list<AsyncWriteBuffer*>::iterator it;
  for(it = writes_in_flight_.begin(); it != writes_in_flight_.end(); ++it) {
      if((*it)->state_ == AsyncWriteBuffer::SUCCEEDED) {
          DecreasePendingBytesHelper(*it, lock, false);
          delete *it;  // delete buffer
          it = writes_in_flight_.erase(it);  // delete pointer to buffer in list
      } else {
        break;  // break the loop on first occurrence of a not yet successfully
                // sent element
      }
  }
}

void AsyncWriteHandler::CleanUp(boost::mutex::scoped_lock* lock) {
  assert(lock && lock->owns_lock() && (state_ == FINALLY_FAILED));

  std::list<AsyncWriteBuffer*>::iterator it;
  for (it = writes_in_flight_.begin(); it != writes_in_flight_.end(); ++it) {
    (*it)->file_handle->MarkAsyncWritesAsFailed(); // mark all file handles as failed
    delete *it;  // delete buffers
    it = writes_in_flight_.erase(it);  // delete pointers to buffer in list
  }

  // wake up all waiting threads
  NotifyWaitingObserversAndClearAll(lock);
  if (waiting_blocking_threads_count_ > 0) {
    all_pending_writes_did_complete_.notify_all();
  }
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
