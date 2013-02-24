/*
 * Copyright (c) 2013 by Felix Hupfeld.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/object_cache.h"

#include <stdint.h>

#include <boost/scoped_array.hpp>
#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <map>

#include "libxtreemfs/xtreemfs_exception.h"

namespace xtreemfs {

static uint64_t Now() {
  return time(NULL);
}

CachedObject::CachedObject(int object_no, int object_size)
    : object_no_(object_no), object_size_(object_size), actual_size_(-1),
      is_dirty_(false), last_access_(Now()), read_has_failed_(true) {
}

CachedObject::~CachedObject() {
}

// Flush data and free memory
void CachedObject::FlushAndErase(const ObjectWriterFunction& writer) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  if (is_dirty_) {
    WriteObjectToOSD(writer);
  }
  DropLocked();
}

void CachedObject::Drop() {
  boost::unique_lock<boost::mutex> lock(mutex_);
  DropLocked();
}

void CachedObject::DropLocked() {
  is_dirty_ = false;
  actual_size_ = -1;
  data_.reset(NULL);
}

int CachedObject::Read(int offset_in_object,
                       char* buffer,
                       int bytes_to_read,
                       const ObjectReaderFunction& reader) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  ReadInternal(lock, reader);
  int actual_bytes = std::min(bytes_to_read, actual_size_ - offset_in_object);
  memcpy(buffer, &data_[offset_in_object], actual_bytes);
  last_access_ = Now();
  return actual_bytes;
}

void CachedObject::Write(int offset_in_object,
                         const char* buffer,
                         int bytes_to_write,
                         const ObjectReaderFunction& reader) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  // This can be optimized by not triggering a read for a full object write.
  ReadInternal(lock, reader);
  memcpy(&data_[offset_in_object], buffer, bytes_to_write);
  actual_size_ = std::max(actual_size_, offset_in_object + bytes_to_write);
  is_dirty_ = true;
  last_access_ = Now();
}

void CachedObject::Flush(const ObjectWriterFunction& writer) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  if (is_dirty_) {
    // Write out a copy of the data. In case of error (i.e. an exception)
    // we unwind the stack and do not mark the object as clean.
    WriteObjectToOSD(writer);
    is_dirty_ = false;
    // Other threads can continue to work with the buffer.
    // Another flush can happen in the meantime.
  }
}

void CachedObject::Truncate(int new_object_size) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  if (actual_size_ == new_object_size) {
    return;
  } else if (actual_size_ < new_object_size) {
    if (actual_size_ == -1) {
      data_.reset(new char[object_size_]);
      actual_size_ = 0;
    }
    // Zero out extra data, because we might truncate-extend the file
    // again and we do not zero-out data when shrinking.
    memset(&data_[actual_size_], 0, new_object_size - actual_size_);
  }
  // Nothing to do if actual_size_ > new_object_size, because we won't
  // read beyond the end of the data.
  actual_size_ = new_object_size;
}

uint64_t CachedObject::last_access() {
  boost::unique_lock<boost::mutex> lock(mutex_);
  return last_access_;
}

bool CachedObject::is_dirty() {
  boost::unique_lock<boost::mutex> lock(mutex_);
  return is_dirty_;
}

bool CachedObject::has_data() {
  boost::unique_lock<boost::mutex> lock(mutex_);
  return actual_size_ > 0;
}

void CachedObject::ReadInternal(boost::unique_lock<boost::mutex>& lock,
                                const ObjectReaderFunction& reader) {
  // We hold the lock here, so no other thread is modifying the current
  // state. However, another thread might already be requesting the data
  // from the OSD.
  if (actual_size_ == -1) {
    // We don't have valid data yet.
    if (data_.get() == NULL) {
      // Initial read. No other thread is retrieving the object.
      data_.reset(new char[object_size_]);
      memset(data_.get(), 0, object_size_);
      // No other thread will access the buffer concurrently.
      char* buffer_ptr = data_.get();
      int read_bytes = -1;
      try {
        lock.unlock();
        read_bytes = reader(object_no_, buffer_ptr);
        lock.lock();
      } catch(const XtreemFSException& e) {
        lock.lock();
        read_has_failed_ = true;
      }
      actual_size_ = read_bytes;
    } else {
      // Read already initiated by another thread. Enqueue us as waiting
      // for the data. Our predecessor will dequeue us and wake us up.
      boost::condition_variable* v = new boost::condition_variable();
      read_queue_.push_back(v);
      v->wait(lock);  // unlocks mutex
      delete v;  // has been dequeued already by our predecessor
    }
  }

  if (read_has_failed_) {
    throw IOException();
  }

  // We are done, next in line please.
  if (read_queue_.size() > 0) {
    // Wake up our successor in the queue. It will make progress when
    // we release the lock. The variable still belongs to the thread that enqueued it.
    boost::condition_variable* v = read_queue_.front();
    read_queue_.pop_front();
    v->notify_one();
  }
}

void CachedObject::WriteObjectToOSD(const ObjectWriterFunction& writer) {
  writer(object_no_, data_.get(), actual_size_);
}

ObjectCache::ObjectCache(size_t max_objects, int object_size)
  : max_objects_(max_objects), object_size_(object_size) {
}

ObjectCache::~ObjectCache() {
  for (Cache::iterator i = cache_.begin(); i != cache_.end(); ++i) {
    delete i->second;
  }
}

int ObjectCache::Read(int object_no, int offset_in_object,
                      char* buffer, int bytes_to_read,
                      const ObjectReaderFunction& reader,
                      const ObjectWriterFunction& writer) {
  assert(bytes_to_read + offset_in_object <= object_size_);
  CachedObject* object = LookupObject(object_no, writer);
  int read_bytes = object->Read(
      offset_in_object, buffer, bytes_to_read, reader);
  EvictObjects(writer);
  return read_bytes;
}

void ObjectCache::Write(int object_no, int offset_in_object,
                        const char* buffer, int bytes_to_write,
                        const ObjectReaderFunction& reader,
                        const ObjectWriterFunction& writer) {
  assert(bytes_to_write + offset_in_object <= object_size_);
  CachedObject* object = LookupObject(object_no, writer);
  object->Write(offset_in_object, buffer, bytes_to_write, reader);
  EvictObjects(writer);
}

void ObjectCache::Flush(const ObjectWriterFunction& writer) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  Cache::iterator i;
  for (i = cache_.begin(); i != cache_.end(); ++i) {
    CachedObject* object = i->second;
    object->Flush(writer);
  }
}

void ObjectCache::Truncate(int64_t new_size) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  int object_to_cut = static_cast<int>(new_size / object_size_);
  for (Cache::iterator i = cache_.begin(); i != cache_.end(); ++i) {
    if (i->first == object_to_cut) {
      i->second->Truncate(new_size % object_size_);
    } else if (i->first < object_to_cut) {
      // Extend the object to its full size, if it isn't already.
      i->second->Truncate(object_size_);
    } else if (i->first > object_to_cut) {
      i->second->Drop();
    }
  }
}

CachedObject* ObjectCache::LookupObject(int object_no,
                                        const ObjectWriterFunction& writer) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  Cache::iterator i = cache_.find(object_no);
  if (i == cache_.end()) {
    cache_[object_no] = new CachedObject(object_no, object_size_);
  }
  return cache_[object_no];
}

void ObjectCache::EvictObjects(const ObjectWriterFunction& writer) {
  uint64_t minimum_atime = std::numeric_limits<uint64_t>::max();
  Cache::iterator entry_with_minimum_atime;
  {
    boost::unique_lock<boost::mutex> lock(mutex_);
    int objects_with_data = 0;
    for (Cache::iterator i = cache_.begin(); i != cache_.end(); ++i) {
      if (!i->second->has_data()) {
        continue;
      }
      ++objects_with_data;
      if (minimum_atime > i->second->last_access()) {
        entry_with_minimum_atime = i;
        minimum_atime = i->second->last_access();
      }
    }
    if (objects_with_data <= max_objects_) {
      return;
    }
    assert(entry_with_minimum_atime != cache_.end());
  }
  // We only free the object's data, not its container in order to simplify
  // concurrency handling.
  entry_with_minimum_atime->second->FlushAndErase(writer);
}

int ObjectCache::object_size() const {
  return object_size_;
}

}  // namespace xtreemfs
