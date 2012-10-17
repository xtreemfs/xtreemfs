/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/thread/thread.hpp>

#include "libxtreemfs/options.h"

namespace xtreemfs {

bool Interruptibilizer::WasInterrupted(const Options& options) {
  return (options.was_interrupted_function == NULL)
         ? false
         : (options.was_interrupted_function() == 1 ? true : false);
}

void Interruptibilizer::SleepInterruptible(int rel_time_ms,
                                           const Options& options) {
  const int sleep_interval_ms = 2000;

  int wait_time;
  while (rel_time_ms > 0 && !Interruptibilizer::WasInterrupted(options)) {
    wait_time = rel_time_ms > sleep_interval_ms ? sleep_interval_ms
                                                : rel_time_ms;
    rel_time_ms -= wait_time;

    boost::this_thread::sleep(boost::posix_time::millisec(wait_time));
  }
}

}  // namespace xtreemfs
