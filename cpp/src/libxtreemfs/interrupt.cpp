/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

#include <boost/date_time/posix_time/posix_time_types.hpp>

namespace xtreemfs {

Interruptibilizer::query_function Interruptibilizer::f_ = NULL;

void Interruptibilizer::Initialize(query_function f) {
  f_ = f;
}

bool Interruptibilizer::WasInterrupted() {
  // TODO(mno): NULL check still needed and usefull?
  return (f_ == NULL) ? false : static_cast<bool>(f_());
}

void Interruptibilizer::SleepInterruptible(int rel_time_in_ms) {
  const int sleep_interval_ms = 2000;

  int wait_time;
  while (rel_time_ms > 0 && !interrupt.WasInterrupted()) {
    wait_time = rel_time_ms > sleep_interval_ms ? sleep_interval_ms
                                                : rel_time_ms;
    rel_time_ms -= wait_time;

    boost::this_thread::sleep(boost::posix_time::millisec(wait_time));
  }
}

}  // namespace xtreemfs