/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_
#define CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_

#include <boost/function.hpp>

namespace xtreemfs {
  
typedef boost::function0<int> InterruptedCallback;

/** Aggregates helper functions which check for an interrupted request or are
 *  responsible for the delay between two request execution attempts. */
class Interruptibilizer {
 public:
  static bool WasInterrupted(InterruptedCallback cb);

  /** Wrapper for boost::thread::sleep which checks for interruptions by
   *  the signal handler.
   *
   * @remarks this function contains a boost::thread interruption point and
   *          thus might throw boost::thread_interrupted.
   */
  static void SleepInterruptible(int rel_time_ms, InterruptedCallback cb);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_
