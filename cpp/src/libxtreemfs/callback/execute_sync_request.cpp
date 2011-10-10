/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/callback/execute_sync_request.h"

using namespace xtreemfs::rpc;
using namespace xtreemfs::util;

namespace xtreemfs {

/** The TLS pointer is set to != NULL if the current operation shall be
 *  interrupted. */
boost::thread_specific_ptr<int> intr_pointer(NULL);

/** Calling malloc() inside a signal handler is a really bad idea as there may
 *  occur a dead lock on the lock of the heap: that happens if a malloc is in
 *  progress, which already obtained a lock on the heap, and now a signal
 *  handler is called (and executed in the same thread) and also tries to
 *  execute malloc.
 *
 *  For this reason the TLS is filled with the address of a static integer
 *  instead of a new-ly created integer.
 */
int dummy_integer = 1;

void InterruptSyncRequest(int signal) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "INTERRUPT triggered, setting TLS pointer" << std::endl;
  }
  intr_pointer.reset(&dummy_integer);
}

}  // namespace xtreemfs
