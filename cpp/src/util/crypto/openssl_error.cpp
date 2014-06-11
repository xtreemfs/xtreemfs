/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/openssl_error.h"

#include <openssl/err.h>

#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

namespace xtreemfs {
namespace util {

void LogAndThrowOpenSSLError() {
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
  throw XtreemFSException("OpenSSL error");
}

} /* namespace util */
} /* namespace xtreemfs */
