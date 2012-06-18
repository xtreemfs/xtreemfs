/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

#include <boost/date_time/posix_time/posix_time_types.hpp>

namespace xtreemfs
{

  Interruptibilizer::query_function Interruptibilizer::f_ = NULL;

  void
  Interruptibilizer::Initialize(query_function f)
  {
    f_ = f;
  }

  bool
  Interruptibilizer::WasInterrupted()
  {
    return (f_ == NULL) ? false : static_cast<bool>(f_()); // TODO: NULL check still needed and usefull?
  }

  void
  Interruptibilizer::SleepInterruptible(int rel_time_in_ms)
  {
    assert(rel_time_in_ms >= 0);
    const int intervall_in_ms = 100;
    int runs = rel_time_in_ms / intervall_in_ms
        + ((rel_time_in_ms % intervall_in_ms) > 0 ? 1 : 0);

    for (int i = 0; i < runs && !Interruptibilizer::WasInterrupted(); ++i)
      {
        boost::this_thread::sleep(boost::posix_time::millisec(intervall_in_ms));
      }
  }

} // namespace xtreemfs
