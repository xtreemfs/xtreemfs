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
#include "util/logging.h"

namespace xtreemfs {

class Interruptibilizer {
 public:
  //typedef int (*query_function)(void);  // this also works without changes, but does not support functor objects
  typedef boost::function0<int> query_function;

  static void Initialize(query_function f);

  static bool WasInterrupted();

  static void SleepInterruptible(int rel_time_in_ms);

 private:
  static query_function f_;
};

}  // namespace xtreemfs

#endif
