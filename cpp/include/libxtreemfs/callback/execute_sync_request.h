/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_CALLBACK_EXECUTE_SYNC_REQUEST_H_
#define CPP_INCLUDE_LIBXTREEMFS_CALLBACK_EXECUTE_SYNC_REQUEST_H_

//#ifdef __linux
//#include <csignal>
//#endif

#ifdef WIN32
#define NOMINMAX
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#else
#include <ctime>
#endif

#include <algorithm>
#include <boost/cstdint.hpp>
#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/thread/thread.hpp>
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

namespace xtreemfs {

namespace rpc {
class ClientRequestCallbackInterface;
}  // namespace rpc


/** Retries to execute the synchronous request "sync_function" up to "options.
 *  max_tries" times and may get interrupted. The "uuid_iterator" object is used
 *  to retrieve UUIDs or mark them as failed.
 *  If uuid_iterator_has_addresses=true, the resolving of the UUID is skipped
 *  and the string retrieved by uuid_iterator->GetUUID() is used as address.
 *  (in this case uuid_resolver may be NULL).
 *
 *  The interrupt handler is only registered, if a signal "options.
 *  interrupt_signal" is set.
 *
 *  The parameter delay_last_attempt should be set true, if this method is
 *  called with max_tries = 1 and one does the looping over the retries on its
 *  own (for instance in FileHandleImplementation::AcquireLock). If set to false
 *  this method would return immediately after the _last_ try and the caller would
 *  have to ensure the delay of options.retry_delay_s on its own.
 *
 *  Ownership of arguments is NOT transferred.
 *
 */
template<class ReturnMessageType, class F>
    ReturnMessageType ExecuteSyncRequest(F sync_function,
                                         UUIDIterator* uuid_iterator,
                                         UUIDResolver* uuid_resolver,
                                         int max_tries,
                                         const Options& options,
                                         bool uuid_iterator_has_addresses,
                                         bool delay_last_attempt,
                                         XCapHandler* xcap_handler,
                                         xtreemfs::pbrpc::XCap* xcap_in_req) {
  assert(uuid_iterator_has_addresses || uuid_resolver);
  assert((!xcap_handler && !xcap_in_req) || (xcap_handler && xcap_in_req));

  Interruptibilizer interrupt(options.interrupt_signal);

  int attempt = 0;
  bool redirected = false;
  ReturnMessageType response = NULL;
  std::string service_uuid = "";
  std::string service_address;

  // Retry unless maximum tries reached or interrupted.
  while ((++attempt <= max_tries || max_tries == 0) &&
         !Interruptibilizer::WasInterrupted()) {
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
      uuid_resolver->UUIDToAddress(service_uuid, &service_address, options);
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
    } catch (const boost::thread_interrupted& e) {
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
      // An error has happened. Differ between communication problems (retry
      // allowed) and application errors (need to pass to the caller).

      // Retry (and delay) only if at least one retry is left
      if (((attempt < max_tries || max_tries == 0) ||
           //                      or this last retry should be delayed
           (attempt == max_tries && delay_last_attempt)) &&
          // AND it is an recoverable error.
          (response->error()->error_type() == xtreemfs::pbrpc::IO_ERROR ||
           response->error()->error_type() == xtreemfs::pbrpc::INTERNAL_SERVER_ERROR ||  // NOLINT
           response->error()->error_type() == xtreemfs::pbrpc::REDIRECT)) {
        std::string error;
        xtreemfs::util::LogLevel level = xtreemfs::util::LEVEL_ERROR;

        // Special handling of REDIRECT "errors".
        if (response->error()->error_type() == xtreemfs::pbrpc::REDIRECT) {
          assert(response->error()->has_redirect_to_server_uuid());
          uuid_iterator->SetCurrentUUID(
              response->error()->redirect_to_server_uuid());
          // Log the redirect.
          level = xtreemfs::util::LEVEL_INFO;
          if (uuid_iterator_has_addresses) {
            error = "The server: " + service_address
                  + " redirected to the current master: "
                  + response->error()->redirect_to_server_uuid()
                  + " at attempt: " + boost::lexical_cast<std::string>(attempt);
          } else {
            error = "The server with the UUID: " + service_uuid
                  + " redirected to the current master with the UUID: "
                  + response->error()->redirect_to_server_uuid()
                  + " at attempt: " + boost::lexical_cast<std::string>(attempt);
          }

          // If it's the first redirect, do a fast retry and do not delay.
          if (!redirected) {
            if (xtreemfs::util::Logging::log->loggingActive(level)) {
              xtreemfs::util::Logging::log->getLog(level) << error << std::endl;
            }
            xtreemfs::util::ErrorLog::error_log->AppendError(error);
            redirected = true;
            // Do not count the first redirect as a try.
            if (max_tries != 0) {
              --attempt;
            }
            continue;
          }
        } else {
          // Communication error or Internal Server Error.

          // Mark the current UUID as failed and get the next one.
          if (uuid_iterator_has_addresses) {
            uuid_iterator->MarkUUIDAsFailed(service_address);
            uuid_iterator->GetUUID(&service_address);
          } else {
            uuid_iterator->MarkUUIDAsFailed(service_uuid);
            uuid_iterator->GetUUID(&service_uuid);
          }

          // Log only the first retry.
          if (attempt == 1 && max_tries != 1) {
            std::string retries_left = max_tries == 0 ? "infinite"
                : boost::lexical_cast<std::string>(max_tries - attempt);
            error = "Got no response from server "
                + (uuid_iterator_has_addresses ? service_address
                      : ( service_address + " (" + service_uuid + ")"))
                + ", retrying ("
                + boost::lexical_cast<std::string>(retries_left)
                + " attempts left)";
          }
        }

        // If the request did return before the timeout was reached, wait until
        // the timeout is up to avoid flooding.

        // delay = retry_delay - (current_time - request_sent_time)
        boost::posix_time::time_duration delay_time_left =
            boost::posix_time::seconds(options.retry_delay_s) -  // delay
            (boost::posix_time::microsec_clock::local_time() -   // current time
             request_sent_time);

        if (!delay_time_left.is_negative()) {
          // Append time left to error message.
          if (!error.empty()) {
            error += ", waiting "
                + boost::str(boost::format("%.1f") % (std::max(
                    0.0,
                    static_cast<double>(
                        delay_time_left.total_milliseconds()) / 1000)))
                + " more seconds till next attempt.";
            if (xtreemfs::util::Logging::log->loggingActive(level)) {
              xtreemfs::util::Logging::log->getLog(level) << error << std::endl;
            }
            xtreemfs::util::ErrorLog::error_log->AppendError(error);
            error.clear();
          }

          try {
              sleep_interruptible(delay_time_left.total_milliseconds());
          } catch (const boost::thread_interrupted& e) {
              if (response != NULL) {
                // Free response.
                response->DeleteBuffers();
                delete response;
              }
              throw;
          }
        }

      } else {
        break;  // Do not retry if error occurred - throw exception. // TODO(mno): where is the exception?
      }
    } else {
      // No error happened, check for possible interruption.
    }  // if (response->HasFailed())

    // Have we been interrupted?
    if (options.interrupt_signal && Interruptibilizer::WasInterrupted()) {
      if (xtreemfs::util::Logging::log->loggingActive(
              xtreemfs::util::LEVEL_INFO)) {
        std::string error = "Caught interrupt, aborting sync request.";
        xtreemfs::util::Logging::log->getLog(xtreemfs::util::LEVEL_INFO)
            << error << std::endl;
        xtreemfs::util::ErrorLog::error_log->AppendError(error);
      }
      // Clear the current response.
      if (response != NULL) {
        response->DeleteBuffers();
      }
      // Free response.
      delete response;
      response = NULL;
      break;  // Do not retry if interrupted.
    }
    if (response != NULL && !response->HasFailed()) {
      break;  // Do not retry if request was successful.
    }
  }

  // Request was successful.
  if (response && !response->HasFailed()) {
    return response;
  }

  // Output number of retries if not failed at the first retry.
  std::string retry_count_msg;
  if (attempt > 1) {
    retry_count_msg = ". Request finally failed after: "
       + boost::lexical_cast<std::string>(attempt) + " attempts.";
  } else {
    retry_count_msg = "";
  }
  // Max attempts reached or non-IO error seen. Throw an exception.
  if (response != NULL) {
    // Copy error information in order to delete buffers before the throw.
    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error_resp = response->error();
    const xtreemfs::pbrpc::ErrorType error_type = error_resp->error_type();
    const std::string error_message = error_resp->error_message();
    const xtreemfs::pbrpc::POSIXErrno posix_errno = error_resp->posix_errno();

    // Free buffers.
    response->DeleteBuffers();
    delete response;

    // By default all errors are logged as errors.
    xtreemfs::util::LogLevel level = xtreemfs::util::LEVEL_ERROR;
    // String for complete error text which will be logged.
    std::string error;

    // Throw an exception.
    switch (error_type) {
      case xtreemfs::pbrpc::ERRNO:  {
        // Posix errors are usually not logged as errors.
        level = xtreemfs::util::LEVEL_INFO;
        if (posix_errno == xtreemfs::pbrpc::POSIX_ERROR_ENOENT) {
          level = xtreemfs::util::LEVEL_DEBUG;
        }
        std::string posix_errono_string
            = boost::lexical_cast<std::string>(posix_errno);
        const ::google::protobuf::EnumValueDescriptor* enum_desc =
            xtreemfs::pbrpc::POSIXErrno_descriptor()->
                FindValueByNumber(posix_errno);
        if (enum_desc) {
            posix_errono_string = enum_desc->name();
        }
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " denied the requested operation."
              " Error Value: " + posix_errono_string
            + " Error message: " + error_message
            + retry_count_msg;
        if (xtreemfs::util::Logging::log->loggingActive(level)) {
          xtreemfs::util::Logging::log->getLog(level) << error << std::endl;
          xtreemfs::util::ErrorLog::error_log->AppendError(error);
        }
        throw PosixErrorException(posix_errno, error);
      }
      case xtreemfs::pbrpc::IO_ERROR:  {
        error = "The client encountered a communication error sending a request"
            " to the server: "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + ". Error: " + error_message + retry_count_msg;
        if (xtreemfs::util::Logging::log->loggingActive(level)) {
          xtreemfs::util::Logging::log->getLog(level) << error << std::endl;
        }
        xtreemfs::util::ErrorLog::error_log->AppendError(error);
        throw IOException(error_message);
      }
      case xtreemfs::pbrpc::INTERNAL_SERVER_ERROR:  {
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " returned an internal server error: " + error_message
            + retry_count_msg;
        if (xtreemfs::util::Logging::log->loggingActive(level)) {
          xtreemfs::util::Logging::log->getLog(level) << error << std::endl;
        }
        xtreemfs::util::ErrorLog::error_log->AppendError(error);
        throw InternalServerErrorException(error_message);
      }
      case xtreemfs::pbrpc::REDIRECT:  {
        throw XtreemFSException("This error (A REDIRECT error was not handled "
            "and retried but thrown instead) should never happen. Report this");
      }
      default:  {
        std::string error_type_name
            = boost::lexical_cast<std::string>(error_type);
        const ::google::protobuf::EnumValueDescriptor* enum_desc =
            xtreemfs::pbrpc::ErrorType_descriptor()->
                FindValueByNumber(error_type);
        if (enum_desc) {
          error_type_name = enum_desc->name();
        }
        error = "The server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + " returned an error: " + error_type_name
            + " Error: " + error_message + retry_count_msg;
        if (xtreemfs::util::Logging::log->loggingActive(level)) {
          xtreemfs::util::Logging::log->getLog(level) << error << std::endl;
        }
        xtreemfs::util::ErrorLog::error_log->AppendError(error);
        throw XtreemFSException(error);
      }
    }
  } else {
    // No Response given, probably interrupted.
    throw PosixErrorException(
        xtreemfs::pbrpc::POSIX_ERROR_EINTR,
        "The operation (sending a request to the server "
            + (uuid_iterator_has_addresses ? service_address
                  : ( service_address + " (" + service_uuid + ")"))
            + ") was aborted by the user at attempt: "
            // attempt + 1 because the interrupt is only possible after the
            // request came back.
            + boost::lexical_cast<std::string>(attempt + 1) + ".");
  } // if (response != NULL)
}

/** Executes the request without delaying the last try and no xcap handler. */
template<class ReturnMessageType, class F>
    ReturnMessageType ExecuteSyncRequest(F sync_function,
                                         UUIDIterator* uuid_iterator,
                                         UUIDResolver* uuid_resolver,
                                         int max_tries,
                                         const Options& options) {
  return ExecuteSyncRequest<ReturnMessageType>(sync_function,
                                               uuid_iterator,
                                               uuid_resolver,
                                               max_tries,
                                               options,
                                               false,
                                               false,
                                               NULL,
                                               NULL);
}

/** Executes the request without a xcap handler. */
template<class ReturnMessageType, class F>
    ReturnMessageType ExecuteSyncRequest(F sync_function,
                                         UUIDIterator* uuid_iterator,
                                         UUIDResolver* uuid_resolver,
                                         int max_tries,
                                         const Options& options,
                                         bool uuid_iterator_has_addresses,
                                         bool delay_last_attempt) {
  return ExecuteSyncRequest<ReturnMessageType>(sync_function,
                                               uuid_iterator,
                                               uuid_resolver,
                                               max_tries,
                                               options,
                                               uuid_iterator_has_addresses,
                                               delay_last_attempt,
                                               NULL,
                                               NULL);
}

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CALLBACK_EXECUTE_SYNC_REQUEST_H_
