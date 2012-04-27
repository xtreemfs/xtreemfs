/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_
#define CPP_INCLUDE_LIBXTREEMFS_INTERRUPT_H_

#include <boost/thread/thread.hpp>

#include "libxtreemfs/options.h"
#include "util/logging.h"

#ifdef __linux
#include <csignal>
#endif

namespace xtreemfs {

/**
 * Wrapper for boost::thread::sleep which catches the interruption exception
 * without doing anything.
 */
void sleep_interruptible(const unsigned int& rel_time_in_ms);

#ifdef __unix

/** This class encapsulates the registration and unregistration of an
 *  interruption signal handler for a given signal (interrupt_signal).
 */
class Interruptibilizer {
 public:
  Interruptibilizer(int interrupt_signal)
    : interrupt_signal_(interrupt_signal),
      previous_signal_handler_(SIG_IGN) {
    if (interrupt_signal_) {
      previous_signal_handler_ = signal(interrupt_signal_,
                                        InterruptSyncRequest);
      if(previous_signal_handler_ == SIG_ERR) {
          util::Logging::log->getLog(util::LEVEL_ERROR)
              << "Failed to set the signal Handler." << std::endl;
      }
      // clear current interruption state if this is not a nested call (which
      // is detected by checking the previous signal handler
      if (previous_signal_handler_ != InterruptSyncRequest) {
        was_interrupted_ = false;
      }
    }
  }

  ~Interruptibilizer() {
    // Remove signal handler.
    if (interrupt_signal_) {
      sighandler_t res = signal(interrupt_signal_, previous_signal_handler_);
      if(res == SIG_ERR) {
          util::Logging::log->getLog(util::LEVEL_ERROR)
              << "Failed to re-set the signal Handler." << std::endl;
      }
    }
  }

  static bool WasInterrupted() {
    return was_interrupted_;
  }

 private:
  /** NOTE: never call malloc() in a handler because another malloc() could be
   *  in progress which might lead to a deadlock situation.
   */
  static void InterruptSyncRequest(int signal) {
    if (util::Logging::log->loggingActive(util::LEVEL_DEBUG)) {
        util::Logging::log->getLog(util::LEVEL_DEBUG)
          << "INTERRUPT triggered, setting was_interrupted_ true." << std::endl;
    }
    was_interrupted_ = true;
  }

  static __thread bool was_interrupted_;
  const int interrupt_signal_;
  sighandler_t previous_signal_handler_;
};

#else  // for all other platforms
class Interruptibilizer {
 public:
  Interruptibilizer(int interrupt_signal) {}
  static bool WasInterrupted() {
    return false;
  }

}
#endif

} // namespace xtreemfs

#endif
