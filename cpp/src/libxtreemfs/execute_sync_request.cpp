/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/execute_sync_request.h"

#include <stdint.h>

#include <algorithm>
#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/format.hpp>
#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/thread/thread.hpp>
#include <ctime>
#include <google/protobuf/descriptor.h>
#include <iostream>
#include <string>

#include "libxtreemfs/interrupt.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/xcap_handler.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"
#include "rpc/sync_callback.h"
#include "util/error_log.h"
#include "util/logging.h"

using std::endl;
using std::string;
using namespace xtreemfs::util;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

/** Helper function which delays the execution and logs an error.
 *
 * The delay ensures the server won't be flooded.
 *
 * @throws  boost::thread_interrupted if interrupted.
 *
 * @remarks Ownership of "response" is transferred if function throws.
 */
void DelayNextRetry(const RPCOptions& options,
    const boost::posix_time::ptime& request_sent_time,
    const std::string& delay_error,
    const xtreemfs::util::LogLevel level,
    rpc::SyncCallbackBase* response) {
  // delay = retry_delay - (current_time - request_sent_time)
  boost::posix_time::time_duration delay_time_left =
      boost::posix_time::seconds(options.retry_delay_s()) -  // delay
      (boost::posix_time::microsec_clock::local_time() -   // current time
       request_sent_time);

  string msg = delay_error;
  if (!delay_time_left.is_negative() && !msg.empty()) {
    // Append time left to error message.
    msg += ", waiting "
        + boost::str(boost::format("%.1f") % (std::max(
              0.0,
              static_cast<double>(
                  delay_time_left.total_milliseconds()) / 1000)))
        + " more seconds till next attempt.";
  }

  if (!msg.empty()) {
    if (Logging::log->loggingActive(level)) {
      Logging::log->getLog(level) << msg << endl;
    }
    ErrorLog::error_log->AppendError(msg);
  }

  if (!delay_time_left.is_negative()) {
    try {
      Interruptibilizer::SleepInterruptible(
          static_cast<int>(delay_time_left.total_milliseconds()),
          options.was_interrupted_cb());
    } catch (const boost::thread_interrupted&) {
      if (response != NULL) {
        // Free response.
        response->DeleteBuffers();
        delete response;
      }
      throw;
    }
  }
}

/** Retries to execute the synchronous request "sync_function" up to "options.
 *  options.max_retries()" times and may get interrupted. The "uuid_iterator"
 *  object is used to retrieve UUIDs or mark them as failed.
 *  If uuid_iterator_has_addresses=true, the resolving of the UUID is skipped
 *  and the string retrieved by uuid_iterator->GetUUID() is used as address.
 *  (in this case uuid_resolver may be NULL).
 *
 *  The parameter delay_last_attempt should be set true, if this method is
 *  called with options.max_retries() = 1 and one does the looping over the
 *  retries on its own (for instance in FileHandleImplementation::AcquireLock).
 *  If set to false this method would return immediately after the _last_ try
 *  and the caller would have to ensure the delay of options.retry_delay_s on
 *
 *  Ownership of arguments is NOT transferred.
 *
 */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    bool uuid_iterator_has_addresses,
    XCapHandler* xcap_handler,
    xtreemfs::pbrpc::XCap* xcap_in_req) {
  assert(uuid_iterator_has_addresses || uuid_resolver);
  assert((!xcap_handler && !xcap_in_req) || (xcap_handler && xcap_in_req));

  const int kMaxRedirectsInARow = 5;

  int attempt = 0;
  int redirects_in_a_row = 0;
  bool max_redirects_in_a_row_exceeded = false;
  rpc::SyncCallbackBase* response = NULL;
  string service_uuid = "";
  string service_address;

  // Retry unless maximum tries reached or interrupted.
  while ((++attempt <= options.max_retries() || options.max_retries() == 0) &&
         !Interruptibilizer::WasInterrupted(options.was_interrupted_cb())) {
    // Delete any previous response;
    if (response != NULL) {
      response->DeleteBuffers();
      delete response;
    }

    // Resolve UUID first.
    if (uuid_iterator_has_addresses) {
      uuid_iterator->GetUUID(&service_address);
    } else {
      uuid_iterator->GetUUID(&service_uuid);
      uuid_resolver->UUIDToAddressWithOptions(service_uuid,
                                              &service_address,
                                              options);
    }

    // Execute request.

    // Send out request.
    boost::posix_time::ptime request_sent_time =
        boost::posix_time::microsec_clock::local_time();

    if (attempt > 1 && xcap_handler && xcap_in_req) {
      xcap_handler->GetXCap(xcap_in_req);
    }
    response = sync_function(service_address);

    bool has_failed;
    try {
      has_failed = response->HasFailed();
    } catch (const boost::thread_interrupted&) {
        if (response != NULL) {
          // Wait until request was processed - otherwise leaks and accesses
          // to deleted memory may occur.
          response->HasFailed();
          // Free response.
          response->DeleteBuffers();
          delete response;
        }
        throw;
    }

    // Check response.
    if (has_failed) {
      // Retry only if it is a recoverable error (REDIRECT, IO_ERROR, INTERNAL_SERVER_ERROR).  // NOLINT
      bool retry = false;
      // Message to be logged and respective log level if retry occurs.
      string delay_error;
      LogLevel level = LEVEL_ERROR;

      const RPCHeader::ErrorResponse err = *(response->error());

      if (err.error_type() == REDIRECT) {
        retry = true;
        redirects_in_a_row++;

        assert(err.has_redirect_to_server_uuid());
        uuid_iterator->SetCurrentUUID(err.redirect_to_server_uuid());

        level = LEVEL_INFO;
        if (uuid_iterator_has_addresses) {
          delay_error = "The server: " + service_address
                + " redirected to the current master: "
                + err.redirect_to_server_uuid()
                + " at attempt: " + boost::lexical_cast<string>(attempt);
        } else {
          delay_error = "The server with the UUID: " + service_uuid
                + " redirected to the current master with the UUID: "
                + err.redirect_to_server_uuid()
                + " at attempt: " + boost::lexical_cast<string>(attempt);
        }

        // Ignore the number of attempts if kMaxRedirectsInARow is not reached.
        if (redirects_in_a_row <= kMaxRedirectsInARow) {
          --attempt;
        } else {
          max_redirects_in_a_row_exceeded = true;
          level = LEVEL_ERROR;
        }

        // If it's the first redirect, do a fast retry and do not delay.
        if (redirects_in_a_row == 1) {
          if (Logging::log->loggingActive(level)) {
            Logging::log->getLog(level) << delay_error << endl;
          }
          ErrorLog::error_log->AppendError(delay_error);
          continue;
        }
      } else {
        redirects_in_a_row = 0;
      }

      if (err.error_type() == IO_ERROR ||
          err.error_type() == INTERNAL_SERVER_ERROR) {
        // Log only the first retry.
        if (attempt == 1 && options.max_retries() != 1) {
         string retries_left = options.max_retries() == 0 ? "infinite"
             : boost::lexical_cast<string>(options.max_retries() - attempt);
         delay_error = "Got no response from server "
             + (uuid_iterator_has_addresses ? service_address
                   : ( service_address + " (" + service_uuid + ")"))
             + ", retrying ("
             + boost::lexical_cast<string>(retries_left)
             + " attempts left) (Possible reason: The server is using SSL,"
             + " and the client is not.)";
        }

        retry = true;
        // Mark the current UUID as failed and get the next one.
        if (uuid_iterator_has_addresses) {
         uuid_iterator->MarkUUIDAsFailed(service_address);
         uuid_iterator->GetUUID(&service_address);
        } else {
         uuid_iterator->MarkUUIDAsFailed(service_uuid);
         uuid_iterator->GetUUID(&service_uuid);
        }
      }

      // Retry (and delay)?
      if (retry &&
           // Attempts left
          (attempt < options.max_retries() || options.max_retries() == 0 ||
           // or this last retry should be delayed.
           (attempt == options.max_retries() && options.delay_last_attempt()))) {  // NOLINT
        DelayNextRetry(options, request_sent_time, delay_error, level, response);  // NOLINT
      } else {
        break;  // Do not retry if error occurred - throw exception below.
      }
    } else {
      // No error happened, check for possible interruption.
    }  // if (response->HasFailed())

    // Have we been interrupted?
    if (Interruptibilizer::WasInterrupted(options.was_interrupted_cb())) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        string error = "Caught interrupt, aborting sync request.";
        Logging::log->getLog(LEVEL_INFO) << error << endl;
        ErrorLog::error_log->AppendError(error);
      }
      // Clear the current response.
      if (response != NULL) {
        response->DeleteBuffers();
      }
      delete response;
      response = NULL;
      break;  // Do not retry if interrupted.
    }

    // Do not retry if request was successful.
    if (response != NULL && !response->HasFailed()) {
      break;
    }
  }  // while("attempts left" || "not interrupted")

  // Request was successful.
  if (response && !response->HasFailed()) {
    if (attempt > 1 || max_redirects_in_a_row_exceeded) {
      string msg = "After retrying the client succeeded to receive a response"
          " at attempt " + boost::lexical_cast<string>(attempt)
          + " from server: "
          + (uuid_iterator_has_addresses ? service_address
                 : ( service_address + " (" + service_uuid + ")"));
      Logging::log->getLog(LEVEL_INFO) << msg << endl;
      ErrorLog::error_log->AppendError(msg);
    }
    return response;
  }

  // Output number of retries if not failed at the first retry.
  string retry_count_msg;
  if (attempt > 1) {
    retry_count_msg = ". Request finally failed after: "
       + boost::lexical_cast<string>(attempt) + " attempts.";
  } else {
    retry_count_msg = "";
  }
  // Max attempts reached or non-IO error seen. Throw an exception.
  if (response != NULL) {
    // Copy error information in order to delete buffers before the throw.
    const RPCHeader::ErrorResponse& error_resp = *(response->error());
    const ErrorType error_type = error_resp.error_type();
    string error_message = error_resp.error_message();
    if (error_message.empty()) {
      error_message = "none given";
    }
    const POSIXErrno posix_errno = error_resp.posix_errno();
    string redirect_target = "";
    if (error_resp.has_redirect_to_server_uuid()) {
      redirect_target = error_resp.redirect_to_server_uuid();
    }

    // Free buffers.
    response->DeleteBuffers();
    delete response;

    // By default all errors are logged as errors.
    LogLevel level = LEVEL_ERROR;
    // String for complete error text which will be logged.
    string error;

    // Throw an exception.
    switch (error_type) {
      case ERRNO:  {
        // Posix errors are usually not logged as errors.
        level = LEVEL_INFO;
        if (posix_errno == POSIX_ERROR_ENOENT) {
          level = LEVEL_DEBUG;
        }
        if (posix_errno == POSIX_ERROR_EIO) {
          level = LEVEL_ERROR;
        }
        string posix_errno_string = boost::lexical_cast<string>(posix_errno);
        const ::google::protobuf::EnumValueDescriptor* enum_desc =
            POSIXErrno_descriptor()->FindValueByNumber(posix_errno);
        if (enum_desc) {
          posix_errno_string = enum_desc->name();
        }
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " denied the requested operation."
              " Error Value: " + posix_errno_string
            + " Error message: " + error_message
            + retry_count_msg;
        if (Logging::log->loggingActive(level)) {
          Logging::log->getLog(level) << error << endl;
          ErrorLog::error_log->AppendError(error);
        }
        throw PosixErrorException(posix_errno, error);
      }
      case IO_ERROR:  {
        error = "The client encountered a communication error sending a request"
            " to the server: "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + ". Error: " + error_message + retry_count_msg;
        if (Logging::log->loggingActive(level)) {
          Logging::log->getLog(level) << error << endl;
        }
        ErrorLog::error_log->AppendError(error);
        throw IOException(error_message);
      }
      case INTERNAL_SERVER_ERROR:  {
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " returned an internal server error: " + error_message
            + retry_count_msg;
        if (Logging::log->loggingActive(level)) {
          Logging::log->getLog(level) << error << endl;
        }
        ErrorLog::error_log->AppendError(error);
        throw InternalServerErrorException(error_message);
      }
      case REDIRECT:  {
        error = "Too many redirections occurred. There is probably something"
            " wrong with the replication. The last redirect seen came from the"
            " server: "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " and pointed to: " + redirect_target
            + retry_count_msg;
        if (Logging::log->loggingActive(level)) {
          Logging::log->getLog(level) << error << endl;
        }
        ErrorLog::error_log->AppendError(error);
        throw XtreemFSException(error);
      }
      case INVALID_VIEW: {
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                : ( service_address + " (" + service_uuid + ")"))
            + " denied the requested operation because the clients view is " +
            + "outdated. The request will be retried once the view is renewed.";
        if (Logging::log->loggingActive(level)) {
          Logging::log->getLog(level) << error << endl;
        }
        ErrorLog::error_log->AppendError(error);
        throw InvalidViewException(error);
      }
      default:  {
        string error_type_name
            = boost::lexical_cast<string>(error_type);
        const ::google::protobuf::EnumValueDescriptor* enum_desc =
            ErrorType_descriptor()->FindValueByNumber(error_type);
        if (enum_desc) {
          error_type_name = enum_desc->name();
        }
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " returned an error: " + error_type_name
            + " Error: " + error_message + retry_count_msg;
        if (Logging::log->loggingActive(level)) {
          Logging::log->getLog(level) << error << endl;
        }
        ErrorLog::error_log->AppendError(error);
        throw XtreemFSException(error);
      }
    }
  } else {
    // No Response given, probably interrupted.
    throw PosixErrorException(
        POSIX_ERROR_EINTR,
        "The operation (sending a request to the server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + ") was aborted by the user at attempt: "
            // attempt + 1 because the interrupt is only possible after the
            // request came back.
            + boost::lexical_cast<string>(attempt + 1) + ".");
  } // if (response != NULL)
}

/** Executes the request without delaying the last try and no xcap handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options) {
  return ExecuteSyncRequest(sync_function,
                            uuid_iterator,
                            uuid_resolver,
                            options,
                            false,
                            NULL,
                            NULL);
}

/** Executes the request without a xcap handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    bool uuid_iterator_has_addresses) {
  return ExecuteSyncRequest(sync_function,
                            uuid_iterator,
                            uuid_resolver,
                            options,
                            uuid_iterator_has_addresses,
                            NULL,
                            NULL);
}

}  // namespace xtreemfs
