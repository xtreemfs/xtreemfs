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
#include <map>
#endif

namespace xtreemfs {


#ifdef __linux

/**
 * This class deactivates a signal in its ctor and restores the previous state
 * in its destructor.
 * This was introduced to deactive the signal handling for the executing thread
 * before locking the global mutex for the pseudo-thread-local-storage map of
 * Interruptibilizer. This is necessary because a signal handler call could
 * deadlock when the map is already locked inside the same thread.
 */
class DeInterruptibilizer {
 public:
  DeInterruptibilizer(int interrupt_signal);
  ~DeInterruptibilizer();
 private:
  /** The signal used by this instance. */
  const int interrupt_signal_;
  /** The previous signal handler (will be restored by the destructor. */
  sighandler_t previous_signal_handler_;
};

/** This class encapsulates the registration and deregistration of an
 *  interruption signal handler for a given signal (interrupt_signal).
 */
class Interruptibilizer {
 public:
  Interruptibilizer(int interrupt_signal);
  ~Interruptibilizer();

  /**
   * Returns whether the an interrupt was handled within the current thread.
   * This variant is faster than its static counterpart but, of course, needs
   * an instance of Interruptibilizer.
   */
  bool WasInterrupted() const;

  /** Returns whether the an interrupt was handled within the current thread.
   *
   *  This method is static to ease access without having to pass around
   *  Interruptibilizer instances.
   *  However, this variant is more expensive, since it has to find the
   *  correct map entry by querying a thread id. It also needs the
   *  signal to deactivate signal handling for it while the map is locked.
   */
  static bool WasInterrupted(int interrupt_signal);

  /** 
   *  @remarks never call malloc() in a handler because another malloc() could
   *           be in progress which might lead to a deadlock situation.
   */
  static void InterruptHandler(int signal);


 private:
  typedef std::map<boost::thread::id, bool> map_type;

  /** Mutual exclusion for access to thread_local_was_interrupted_map_. */
  static boost::mutex map_mutex_;
  /** Pseudo-TLS (thread local storage) implemented with a global map. */
  static map_type thread_local_was_interrupted_map_;

  /** Iterator to the own entry in thread_local_was_interrupted_map_ map. */
  std::map<boost::thread::id, bool>::iterator was_interrupted_iterator_;
  /** The signal used by this instance. */
  const int interrupt_signal_;
  /** The previous signal handler (will be restored by the destructor. */
  sighandler_t previous_signal_handler_;
};

#else  // for all other platforms
class Interruptibilizer {
 public:
  Interruptibilizer(int interrupt_signal) {}
  static bool WasInterrupted() {
    return false;
  }

};
#endif

/** Wrapper for boost::thread::sleep which checks for interruptions by
 *  the signal handler.
 *
 * @remarks this function contains a boost::thread interruption point and
 *          thus might throw boost::thread_interrupted.
 */
void sleep_interruptible(int rel_time_in_ms, const Interruptibilizer& interrupt);


} // namespace xtreemfs

#endif
