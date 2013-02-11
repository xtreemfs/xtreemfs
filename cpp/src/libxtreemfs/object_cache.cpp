#include "libxtreemfs/object_cache.h"

#include <stdint.h>

#include <boost/optional.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <map>
#include <string>

#include "xtreemfs/OSD.pb.h"
#include "rpc/sync_callback.h"

namespace xtreemfs {

static uint64_t Now() {
  return time(NULL);
}

CachedObject::CachedObject(int object_no, int object_size) 
    : object_no_(object_no), object_size_(object_size),
      data_(NULL), size_(0), is_dirty_(false), 
      last_access_(Now()) {}
CachedObject::~CachedObject() {
  delete [] data_;
}

// Flush data and free memory
void CachedObject::Erase(ObjectWriter* writer) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  if (is_dirty_) {
    WriteObjectToOSD(writer);
    is_dirty_ = false;
    delete [] data_;
    data_ = NULL;
    lock.unlock(); 
  }
}

int CachedObject::Read(int offset_in_object, char* buffer, int bytes_to_read,
          ObjectReader* reader) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  int read = ReadInternal(lock, reader);
  memcpy(buffer, &data_[offset_in_object], bytes_to_read);
  return read;
}

void CachedObject::Write(int offset_in_object, const char* buffer, 
            int bytes_to_write, ObjectReader* reader) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  ReadInternal(lock, reader);
  memcpy(data_, buffer, bytes_to_write);
}

void CachedObject::Flush(ObjectWriter* writer) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  if (is_dirty_) {
    // Write out a copy of the data asynchronously.
    WriteObjectToOSD(writer);
    is_dirty_ = false;
    // Other threads can continue to work with the buffer.
    // Another flush can happen in the meantime.
  }
  lock.unlock(); 
}

uint64_t CachedObject::last_access() const {
  return last_access_;
}

bool CachedObject::is_dirty() const {
  return is_dirty_;
}

int CachedObject::ReadInternal(boost::unique_lock<boost::mutex>& lock,
                  ObjectReader* reader) {
  if (ReadPending()) {
    if (read_queue_.size() == 0) {
      // Initial read
      ReadObjectFromOSD(lock, reader);  // unlocked synchronous read.
    } else {
      // Read already initiated
      boost::condition_variable* v = new boost::condition_variable();
      read_queue_.push_back(v);
      v->wait(lock);  // unlocks mutex
      delete v;  // has been dequeued already by our predecessor
    }
  }
    
  // We are done, next in line please.
  // It will enter when we release the lock, and wake up its successor.
  if (read_queue_.size() > 0) {
    boost::condition_variable* v = read_queue_.front();
    read_queue_.pop_front();
    v->notify_one();
  }
  last_access_ = Now();
  return 0;  // TODO
}

void CachedObject::ReadObjectFromOSD(boost::unique_lock<boost::mutex>& lock, 
                        ObjectReader* reader) {
  lock.unlock();
  boost::scoped_ptr<rpc::SyncCallbackBase> response((*reader)(object_no_));
  lock.lock();
  xtreemfs::pbrpc::ObjectData* data =
      static_cast<xtreemfs::pbrpc::ObjectData*>(response->response());
        
  data_ = new char[object_size_];
  size_ = object_size_;
  int data_length = response->data_length();
  memcpy(data_, response->data(), data_length);
  // If zero_padding() > 0, the gap has to be filled with zeroes.
  memset(data_ + data_length, 0, data->zero_padding());

  response->DeleteBuffers();
}

void CachedObject::WriteObjectToOSD(ObjectWriter* writer) {
  boost::scoped_ptr<rpc::SyncCallbackBase> response((*writer)(object_no_, data_));
  xtreemfs::pbrpc::OSDWriteResponse* write_response =
      static_cast<xtreemfs::pbrpc::OSDWriteResponse*>(response->response());
  response->DeleteBuffers();
}
    
bool CachedObject::ReadPending() {
  return data_ == NULL;
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
          ObjectReader* reader) {
  CachedObject* object = LookupObject(object_no);
  return object->Read(offset_in_object, buffer, bytes_to_read, reader);
}

void ObjectCache::Write(int object_no, int offset_in_object, 
            const char* buffer, int bytes_to_write,
            ObjectReader* reader) {
  CachedObject* object = LookupObject(object_no);
  object->Write(offset_in_object, buffer, bytes_to_write, reader);
}

void ObjectCache::Flush(ObjectWriter* writer) {
  while (true) {
    boost::unique_lock<boost::mutex> lock(mutex_);
    Cache::iterator i;
    for (i = cache_.begin(); i != cache_.end(); ++i) {
      if (i->second->is_dirty()) {
        CachedObject* object = i->second;
        lock.unlock();
        object->Flush(writer);
        break;  // start from beginning
      }
    }
    if (i == cache_.end()) {
      break;
    }
  }
}

CachedObject* ObjectCache::LookupObject(int object_no) {
  boost::unique_lock<boost::mutex> lock(mutex_);
  Cache::iterator i = cache_.find(object_no);
  if (i == cache_.end()) {
    cache_[object_no] = new CachedObject(object_no, object_size_);

    if (cache_.size() > max_objects_) {
      CollectLeasedObject();
    }
  }
  return cache_[object_no];
  }

void ObjectCache::CollectLeasedObject() {
}

}  // namespace xtreemfs