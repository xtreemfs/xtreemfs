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
bool Interruptibilizer::initialized_ = false;
int Interruptibilizer::interrupt_signal_ = 0;
struct sigaction Interruptibilizer::previous_signal_action_;

DeInterruptibilizer::DeInterruptibilizer(int interrupt_signal)
  : interrupt_signal_(interrupt_signal) {
  // Block signal if configured
  if (interrupt_signal_) {
    sigset_t set;
    sigemptyset(&set);
    sigaddset(&set, interrupt_signal_);

    int r = pthread_sigmask(SIG_BLOCK, &set, NULL);
    if (r) {
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to block signal." << std::endl;
    }
  }
}

DeInterruptibilizer::~DeInterruptibilizer() {
  // Unblock signal if configured
  if (interrupt_signal_) {
    sigset_t set;
    sigemptyset(&set);
    sigaddset(&set, interrupt_signal_);

    int r = pthread_sigmask(SIG_UNBLOCK, &set, NULL);
    if (r) {
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to unblock signal." << std::endl;
    }
  }
}

Interruptibilizer::Interruptibilizer() {
  if (interrupt_signal_) {
    DeInterruptibilizer di(interrupt_signal_);
    boost::mutex::scoped_lock lock(map_mutex_);
    // insert will change nothing if entry already exists, but return a
    // pair of the iterator to the existing or inserted entry and
    // a bool indicating whether the new entry was actualy inserted
    std::pair<map_type::iterator, bool> res =
        thread_local_was_interrupted_map_.insert(
            map_type::value_type(boost::this_thread::get_id(),
            map_type::mapped_type(false, 1)));
    was_interrupted_iterator_ = res.first;

    // if entry exised, increment counter
    if(!res.second) {
      ++(was_interrupted_iterator_->second.second);
    }
  }
}

Interruptibilizer::~Interruptibilizer() {
  if (interrupt_signal_) {
    // delete the map entry if this was the first instance in a chain
    // of nested calls (all nested instances will have InterruptSyncRequest
    // as previous_signal_handler_)
    if (was_interrupted_iterator_->second.second == 1) {
      DeInterruptibilizer di(interrupt_signal_);
      boost::mutex::scoped_lock lock(map_mutex_);
      thread_local_was_interrupted_map_.erase(was_interrupted_iterator_);
    } else {
      ++(was_interrupted_iterator_->second.second);
    }
  }
}

// TODO: reinitialize with new signal..
// TODO: use de-interruptibilizer where necessary

void Interruptibilizer::Initialize(int interrupt_signal) {
  assert(!initialized_);
  /*
  if(initialized_) {
    if(previous_signal_handler_ == SIG_ERR) {
      util::Logging::log->getLog(util::LEVEL_WARN)
          << "Got more than one Interruptibilizer::Initialize()." << std::endl;
     }
  }*/


  if (interrupt_signal) {
    interrupt_signal_ = interrupt_signal;

    struct sigaction new_signal_action;
    new_signal_action.sa_handler = InterruptHandler;
    sigemptyset(&new_signal_action.sa_mask);
    new_signal_action.sa_flags = 0;

    int ret = sigaction(interrupt_signal,
                        &new_signal_action,
                        &previous_signal_action_);
    if(ret) { // return value != 0 is error
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to set the signal handler." << std::endl;
    }
  }
}

void Interruptibilizer::Reinitialize(int interrupt_signal) {
  if (interrupt_signal_) {
    DeInterruptibilizer di_old(interrupt_signal_);
    DeInterruptibilizer di_new(interrupt_signal);

    struct sigaction new_signal_action;
    new_signal_action.sa_handler = InterruptHandler;
    sigemptyset(&new_signal_action.sa_mask);
    new_signal_action.sa_flags = 0;

    // restore old signal handler
    int ret = sigaction(interrupt_signal_,  // NOTE: old signal == member
                        &previous_signal_action_,
                        NULL);
    if(ret) { // return value != 0 is error
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to set back the previous signal handler." << std::endl;
    }

    // reinstall our handler for the new signal
    ret = sigaction(interrupt_signal,  // NOTE: new signal = parameter
                    &new_signal_action,
                    &previous_signal_action_);
    if(ret) { // return value != 0 is error
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to set the signal handler to an new signal." << std::endl;
    }

    interrupt_signal_ = interrupt_signal;
  }
}

void Interruptibilizer::Deinitialize() {
  if (interrupt_signal_) {
    // restore old signal handler
    int ret = sigaction(interrupt_signal_,
                        &previous_signal_action_,
                        NULL);
    if(ret) { // return value != 0 is error
      util::Logging::log->getLog(util::LEVEL_ERROR)
          << "Failed to set back the previous signal handler." << std::endl;
    }
  }
}

bool Interruptibilizer::WasInterrupted() const {
  if (interrupt_signal_) {
      DeInterruptibilizer di(interrupt_signal_);
      boost::mutex::scoped_lock lock(map_mutex_);
      return was_interrupted_iterator_->second.first;
  } else {
      return false;
  }
}


bool Interruptibilizer::WasInterruptedStatic() {
  if (interrupt_signal_) {
    DeInterruptibilizer di(interrupt_signal_);
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
      return it->second.first;
    } else {
      return false;
    }
  } else {
    return false;
  }
}

void Interruptibilizer::InterruptHandler(int signal) {
  assert(signal == interrupt_signal_);
  if (util::Logging::log->loggingActive(util::LEVEL_DEBUG)) {
    util::Logging::log->getLog(util::LEVEL_DEBUG)
        << "INTERRUPT triggered, setting was_interrupted_ true." << std::endl;
  }

  {
    boost::mutex::scoped_lock lock(map_mutex_);
    // NOTE: An entry in the map MUST exist when the handler is called.
    //       This is guaranteed by the constructor.
    thread_local_was_interrupted_map_[boost::this_thread::get_id()].first
        = true;
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
