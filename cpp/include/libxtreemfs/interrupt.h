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
 * This class blocks a signal in its constructor and restores the previous
 * state in its destructor.
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
};

/** This class encapsulates the registration and deregistration of an
 *  interruption signal handler for a given signal (interrupt_signal) and
 *  manages the interruption state per thread.
 */
class Interruptibilizer {
 public:
  Interruptibilizer();
  ~Interruptibilizer();

  static void Initialize(int interrupt_signal);
  static void Reinitialize(int new_interrupt_signal);
  static void Deinitialize();

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
   *  correct map entry by querying a thread id.
   */
  static bool WasInterruptedStatic();

  /** 
   *  @remarks never call malloc() in a handler because another malloc() could
   *           be in progress which might lead to a deadlock situation.
   */
  static void InterruptHandler(int signal);


 private:
  /** The internal map type used for thread local storage. The key is the
   *  thread ID, the value is a pair with the interruption state for that
   *  thread and an instance counter for that thread. The latter is needed
   *  to detect when the last instance for a thread is destructed, which
   *  means the map entry can be removed. */
  typedef std::map<boost::thread::id, std::pair<bool, int> > map_type;

  /** Mutual exclusion for access to thread_local_was_interrupted_map_. */
  static boost::mutex map_mutex_;

  /** Pseudo-TLS (thread local storage) implemented with a global map.
   *  It uses the thread-id as a key and stores a pair consisting of a bool
   *  containing the interruption state and an instance counter to manage
   *  entry removal. See also map_type. */
  static map_type thread_local_was_interrupted_map_;

  /** Iterator to the own entry in thread_local_was_interrupted_map_ map. */
  map_type::iterator was_interrupted_iterator_;

  /** A flag that is set once the first instance was initialised. */
  static bool initialized_;
  /** The signal used by this instance. */
  static int interrupt_signal_;
  /** The previous signal handler (will be restored by the Deinitialize()). */
  static struct sigaction previous_signal_action_;
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
