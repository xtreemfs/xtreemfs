/**
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 * This specific file is based on an example found in this article:
 * http://www.quantnet.com/cplusplus-multithreading-boost/
 */

#ifndef CPP_INCLUDE_UTIL_SYNCHRONIZED_QUEUE_H_
#define CPP_INCLUDE_UTIL_SYNCHRONIZED_QUEUE_H_

#include <boost/thread/thread.hpp>
#include <queue>

namespace xtreemfs {
namespace util {

/** Queue class that has thread synchronization. Intended to synchronize
 *  threads in a producer consumer scenario.*/
template <typename T>
class SynchronizedQueue {
 public:

  /** Add data to the queue and notify others. Never blocks. */
  void Enqueue(const T& data) {
    boost::mutex::scoped_lock lock(mutex_);
    queue_.push(data);
    // Notify others that data is ready
    queue_not_empty_cond_.notify_one();
  }

  /** Get data from the queue. Blocks if no data is available. */
  T Dequeue() {
    boost::mutex::scoped_lock lock(mutex_);

    // When there is no data, wait till someone fills it.
    // Lock is automatically released in the wait and obtained
    // again after the wait
    while (queue_.size() == 0) {
      queue_not_empty_cond_.wait(lock);
    }

    T result = queue_.front();
    queue_.pop();
    return result;
  }

 private:
  /** STL queue for data storage. */
  std::queue<T> queue_;
  /** The mutex to synchronize queue access. */
  boost::mutex mutex_;
  /** The condition to wait for if the queue is empty. */
  boost::condition_variable queue_not_empty_cond_;

};

}  // namespace util
}  // namespace xtreemfs
#endif
