/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/execute_sync_request.h"

namespace xtreemfs {

void ParseAndThrowRequestError(
    const xtreemfs::pbrpc::ErrorType& error_type,
    const xtreemfs::pbrpc::POSIXErrno& posix_errno,
    bool uuid_iterator_has_addresses,
    const std::string& service_address,
    const std::string& service_uuid,
    const std::string& error_message,
    const std::string& retry_count_msg,
    int attempt) {
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
}

}  // namespace xtreemfs
