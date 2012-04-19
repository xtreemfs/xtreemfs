/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_
#define CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_

#include <boost/thread/tss.hpp>

#include "libxtreemfs/options.h"
#include "util/logging.h"

#ifdef __linux
#include <csignal>
#endif

namespace xtreemfs {

/** This class encapsulates the registration and deregistration of an
 *  interruption signal handler for a given signal (interrupt_signal).
 *
 */
class SignalHandler {
 public:
  SignalHandler(int interrupt_signal)
    : interrupt_signal_(interrupt_signal),
      previous_signal_handler_(SIG_IGN) {}

  bool RegisterHandler() {
  #ifdef __linux
    if (interrupt_signal_) {
      intr_pointer_.reset(NULL);  // Clear current interruption state.
      previous_signal_handler_ = signal(interrupt_signal_,
                                        InterruptSyncRequest);
      return previous_signal_handler_ != SIG_ERR; // report error
    }
  #endif
    return true;
  }

  bool UnregisterHandler() {
  #ifdef __linux
    // Remove signal handler.
    if (interrupt_signal_) {
      return signal(interrupt_signal_, previous_signal_handler_) != SIG_ERR;
    }
  #endif
    return true;
  }

  bool WasInterrupted() const {
    return intr_pointer_.get() != 0;
  }

 private:

  static void InterruptSyncRequest(int signal) {
    if (util::Logging::log->loggingActive(util::LEVEL_DEBUG)) {
        util::Logging::log->getLog(util::LEVEL_DEBUG)
          << "INTERRUPT triggered, setting TLS pointer" << std::endl;
    }
    intr_pointer_.reset(&dummy_);
  }

  /** The TLS pointer is set to != 0 if the current operation shall be
   *  interrupted.
   */
  static boost::thread_specific_ptr<int> intr_pointer_;

  /** Calling malloc() inside a signal handler is a really bad idea as there may
   *  occur a dead lock on the lock of the heap: that happens if a malloc is in
   *  progress, which already obtained a lock on the heap, and now a signal
   *  handler is called (and executed in the same thread) and also tries to
   *  execute malloc.
   *
   *  For this reason the TLS is filled with the address of a static integer
   *  instead of a new-ly created integer.
   */
  static int dummy_;

  int interrupt_signal_;
  sighandler_t previous_signal_handler_;
};

/** This class is used in combination with SignalHandler in the way a
 *  scoped_lock is used with a mutex in order to make make a scoped block
 *  interruptible by signal.
 */
class Interruptibilizer {
 public:
  Interruptibilizer(SignalHandler handler) : handler_(handler) {
    handler_.RegisterHandler();
  }

  ~Interruptibilizer() {
    handler_.UnregisterHandler();
  }

  bool WasInterrupted() const {
    return handler_.WasInterrupted();
  }

 private:
  SignalHandler handler_;
};


} // namespace xtreemfs

#endif
