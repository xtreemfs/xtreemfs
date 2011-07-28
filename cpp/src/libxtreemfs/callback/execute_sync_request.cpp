/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/callback/execute_sync_request.h"
#include "rpc/client_request.h"
#include "rpc/client_request_callback_interface.h"

using namespace xtreemfs::rpc;
using namespace xtreemfs::util;

namespace xtreemfs {

boost::thread_specific_ptr<int> intr_pointer;

void InterruptSyncRequest(int signal) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "INTERRUPT triggered, setting TLS pointer" << std::endl;
  }
  intr_pointer.reset(new int(0));
}

}  // namespace xtreemfs
