/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_
#define CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_

#include <boost/thread/thread.hpp>
#include <boost/function.hpp>

#include "libxtreemfs/options.h"

namespace xtreemfs {

class Interruptibilizer {
 public:
  // NOTE: the boost::function typedef could be replaced with
  // typedef int (*query_function)(void);
  // which would also works without changes, but would not support
  // functor objects
  typedef boost::function0<int> query_function;

  static void Initialize(query_function f);

  static bool WasInterrupted();

  /** Wrapper for boost::thread::sleep which checks for interruptions by
   *  the signal handler.
   *
   * @remarks this function contains a boost::thread interruption point and
   *          thus might throw boost::thread_interrupted.
   */
  static void SleepInterruptible(int rel_time_ms);

 private:
  static query_function f_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_
