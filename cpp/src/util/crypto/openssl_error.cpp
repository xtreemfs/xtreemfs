/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/openssl_error.h"

#include <openssl/err.h>

#include "libxtreemfs/xtreemfs_exception.h"
#include "util/error_log.h"
#include "util/logging.h"

namespace xtreemfs {
namespace util {

void LogAndThrowOpenSSLError(std::string error_msg) {
  int flags, line;
  const char *data, *file;
  unsigned long code;
  char buf[256];

  while ((code = ERR_get_error_line_data(&file, &line, &data, &flags)) != 0) {
    ERR_error_string_n(code, buf, sizeof buf);
    util::Logging::log->getLog(util::LEVEL_ERROR) << "OpenSSL error code: "
                                                  << code << " in " << file
                                                  << " line " << line << "."
                                                  << std::endl;
    if (data && (flags & ERR_TXT_STRING)) {
      util::Logging::log->getLog(util::LEVEL_ERROR) << "OpenSSL error data: "
                                                    << data << std::endl;
    }
  }
  error_msg = "OpenSSL error: " + error_msg;
  util::Logging::log->getLog(util::LEVEL_ERROR) << error_msg << std::endl;
  ErrorLog::error_log->AppendError(error_msg);
  throw XtreemFSException(error_msg);
}

} /* namespace util */
} /* namespace xtreemfs */
