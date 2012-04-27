/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

namespace xtreemfs {

#ifdef __unix
__thread bool Interruptibilizer::was_interrupted_ = false;
#endif

void sleep_interruptible(const unsigned int& rel_time_in_ms) {
  const unsigned int intervall_in_ms = 100;
  unsigned int runs = rel_time_in_ms / intervall_in_ms
      + ((rel_time_in_ms % intervall_in_ms) > 0 ? 1 : 0);

  for (unsigned int i = 0;
       i < runs && !Interruptibilizer::WasInterrupted();
       ++i) {
    boost::this_thread::sleep(boost::posix_time::millisec(intervall_in_ms));
  }
}

} // namespace xtreemfs
