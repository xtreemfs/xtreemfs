
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

void initialize_error_log(int max_enries) {
  ErrorLog::error_log = new ErrorLog(max_enries);
}

void shutdown_error_log() {
  delete ErrorLog::error_log;
}

ErrorLog* ErrorLog::error_log = NULL;

}  // namespace util
}  // namespace xtreemfs

