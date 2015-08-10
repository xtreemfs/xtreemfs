
/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "util/logging.h"

#include <ostream>

#include <boost/thread.hpp>
#include <string>
#include "util/error_log.h"

namespace xtreemfs {
namespace util {

void initialize_error_log(int max_entries) {
  // Do not initialize the error log multiple times.
  if (ErrorLog::error_log) {
    ErrorLog::error_log->register_init();
    return;
  }

  ErrorLog::error_log = new ErrorLog(max_entries);
}

void shutdown_error_log() {
  // Delete the logging only if no instance is left.
  if (ErrorLog::error_log && ErrorLog::error_log->register_shutdown()) {
    delete ErrorLog::error_log;
    ErrorLog::error_log = NULL;
  }
}

ErrorLog* ErrorLog::error_log = NULL;

}  // namespace util
}  // namespace xtreemfs

