/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/thread/thread.hpp>

#include "libxtreemfs/execute_sync_request.h"

namespace xtreemfs {

bool Interruptibilizer::WasInterrupted(InterruptedCallback cb) {
  return cb == NULL ? false : cb() == 1;
}

void Interruptibilizer::SleepInterruptible(int64_t rel_time_ms,
                                           InterruptedCallback cb) {
  const int sleep_interval_ms = 2000;

  int64_t wait_time;
  while (rel_time_ms > 0 && !Interruptibilizer::WasInterrupted(cb)) {
    wait_time = rel_time_ms > sleep_interval_ms ? sleep_interval_ms
                                                : rel_time_ms;
    rel_time_ms -= wait_time;

    boost::this_thread::sleep(boost::posix_time::millisec(wait_time));
  }
}

}  // namespace xtreemfs
