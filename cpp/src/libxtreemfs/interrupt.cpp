/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

namespace xtreemfs {

#ifdef __linux
boost::mutex Interruptibilizer::map_mutex_;
Interruptibilizer::map_type Interruptibilizer::thread_local_was_interrupted_map_;

DeInterruptibilizer::DeInterruptibilizer(int interrupt_signal)
  : interrupt_signal_(interrupt_signal),
    previous_signal_handler_(SIG_IGN) {
  if (interrupt_signal_) {
    // register signal handler
    previous_signal_handler_ = signal(interrupt_signal_, SIG_IGN);
    if(previous_signal_handler_ == SIG_ERR) {
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to disable the signal Handler." << std::endl;
    }
  }
}

DeInterruptibilizer::~DeInterruptibilizer() {
  if (interrupt_signal_) {
    // Restore previous signal handler
    sighandler_t res = signal(interrupt_signal_, previous_signal_handler_);
    if(res == SIG_ERR) {
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to re-set the signal Handler." << std::endl;
    }
  }
}

Interruptibilizer::Interruptibilizer(int interrupt_signal)
  : interrupt_signal_(interrupt_signal),
    previous_signal_handler_(SIG_IGN) {
  if (interrupt_signal_) {

    // clear current interruption state if this is not a nested call (which
    // is detected by checking the previous signal handler
    {
      DeInterruptibilizer di(interrupt_signal_);
      boost::mutex::scoped_lock lock(map_mutex_);
      // insert will change nothing if entry already exists, but return a
      // pair of the iterator to the existing entry and the corresponding
      // value
      was_interrupted_iterator_ = thread_local_was_interrupted_map_
          .insert(map_type::value_type(boost::this_thread::get_id(), false))
          .first;
    }

    // register signal handler AFTER adding a map entry
    previous_signal_handler_ = signal(interrupt_signal_,
                                      InterruptHandler);
    if(previous_signal_handler_ == SIG_ERR) {
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to set the signal Handler." << std::endl;
    }
  }
}

Interruptibilizer::~Interruptibilizer() {
  if (interrupt_signal_) {
    // Restore previous signal handler
    sighandler_t res = signal(interrupt_signal_, previous_signal_handler_);
    if(res == SIG_ERR) {
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to re-set the signal Handler." << std::endl;
    }

    // delete the map entry if this was the first instance in a chain
    // of nested calls (all nested instances will have InterruptSyncRequest
    // as previous_signal_handler_)
    if (previous_signal_handler_ != InterruptHandler) {
      DeInterruptibilizer di(interrupt_signal_);
      boost::mutex::scoped_lock lock(map_mutex_);
      thread_local_was_interrupted_map_.erase(was_interrupted_iterator_);
    }
  }
}


bool Interruptibilizer::WasInterrupted() const {
  DeInterruptibilizer di(interrupt_signal_);
  boost::mutex::scoped_lock lock(map_mutex_);
  return was_interrupted_iterator_->second;
}


bool Interruptibilizer::WasInterrupted(int interrupt_signal) {
  DeInterruptibilizer di(interrupt_signal);
  boost::mutex::scoped_lock lock(map_mutex_);

  // NOTE: When interrupt_signal is set to 0, nothing happens on construction
  //       and destruction. So this *static* method is called without having
  //       an instance of this class in the current thread.
  //       To avoid implicitly inserting an new entry by the [] operator, we
  //       have to use find instead (which should not increase the cost of
  //       this method).
  //       By design decision, there always must be a Interruptibilizer
  //       instance when inside libxtreemfs code.
  map_type::iterator it = thread_local_was_interrupted_map_
                 .find(boost::this_thread::get_id());
  if (it != thread_local_was_interrupted_map_.end()) {
    return it->second;
  } else {
    return false;
  }
}

void Interruptibilizer::InterruptHandler(int signal) {
  if (util::Logging::log->loggingActive(util::LEVEL_DEBUG)) {
    util::Logging::log->getLog(util::LEVEL_DEBUG)
        << "INTERRUPT triggered, setting was_interrupted_ true." << std::endl;
  }

  {
    boost::mutex::scoped_lock lock(map_mutex_);
    // NOTE: An entry in the map MUST exist when the handler is called.
    //       This is guaranteed by the constructor.
    thread_local_was_interrupted_map_[boost::this_thread::get_id()] = true;
  }
}

#endif  // __linux

void sleep_interruptible(int rel_time_in_ms, const Interruptibilizer& interrupt) {
  assert(rel_time_in_ms >= 0);
  const int intervall_in_ms = 100;
  int runs = rel_time_in_ms / intervall_in_ms
      + ((rel_time_in_ms % intervall_in_ms) > 0 ? 1 : 0);

  for (int i = 0; i < runs && !interrupt.WasInterrupted(); ++i) {
    boost::this_thread::sleep(boost::posix_time::millisec(intervall_in_ms));
  }
}

}  // namespace xtreemfs
