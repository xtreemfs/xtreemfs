/*
 * Copyright (c) 2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_UTIL_ERROR_LOG_H_
#define	CPP_INCLUDE_UTIL_ERROR_LOG_H_

#include "boost/thread.hpp"
#include <list>
#include <string>

namespace xtreemfs {
namespace util {

/**
 * A simple class that stores the last max_entries
 * error messages. Thread safe.
 */
class ErrorLog {
 public:
  static ErrorLog* error_log;

  ErrorLog(int max_entries) : init_count_(1), max_entries_(max_entries) {}
  ~ErrorLog() {}

  void AppendError(const std::string& message) {
    boost::mutex::scoped_lock lock(error_messages_mutex_);
    if (error_messages_.size() == max_entries_) {
      error_messages_.pop_front();
    }
    error_messages_.push_back(message);
  }
  
  std::list<std::string> error_messages() {
    boost::mutex::scoped_lock lock(error_messages_mutex_);
    return error_messages_;
  }

  void register_init() {
    ++init_count_;
  }

  bool register_shutdown() {
    if (init_count_ > 0) {
      return (--init_count_ == 0);
    }
    return false;
  }

 private:
  /** Contains the number of possible instances, by counting inits and shutdowns. */
  int init_count_;
  int max_entries_;
  boost::mutex error_messages_mutex_;
  std::list<std::string> error_messages_;
};

void initialize_error_log(int max_entries);

void shutdown_error_log();

}  // namespace util
}  // namespace xtreemfs


#endif	// CPP_INCLUDE_UTIL_ERROR_LOG_H_

