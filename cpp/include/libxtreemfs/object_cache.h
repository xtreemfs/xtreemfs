#ifndef CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_
#define CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_

#include <stdint.h>

#include <boost/scoped_ptr.hpp>
#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/function.hpp>

#include <deque>
#include <map>

namespace xtreemfs {
  
namespace rpc {
class SyncCallbackBase;
}

typedef boost::function<rpc::SyncCallbackBase* (int object_no)> ObjectReader;
typedef boost::function<rpc::SyncCallbackBase* (int object_no, const char* data)> ObjectWriter;

class CachedObject {
 public:
  // Create the object in ReadPending state.
  CachedObject(int object_no, int object_size);
  ~CachedObject();

  // Flush data and free memory
  void Erase(ObjectWriter* writer);

  int Read(int offset_in_object, char* buffer, int bytes_to_read,
           ObjectReader* reader);

  void Write(int offset_in_object, const char* buffer, 
             int bytes_to_write, ObjectReader* reader);

  void Flush(ObjectWriter* writer);

  uint64_t last_access() const;

  bool is_dirty() const;

 private:
  int ReadInternal(boost::unique_lock<boost::mutex>& lock,
                   ObjectReader* reader);

  void ReadObjectFromOSD(boost::unique_lock<boost::mutex>& lock, 
                         ObjectReader* reader);

  void WriteObjectToOSD(ObjectWriter* writer);
    
  bool ReadPending();

  boost::mutex mutex_;
  std::deque<boost::condition_variable*> read_queue_;

  const int object_no_;
  const int object_size_;
  char* data_;
  int size_;
  bool is_dirty_;
  uint64_t last_access_;
};

class ObjectCache {
 public:
  ObjectCache(size_t max_objects, int object_size);
  ~ObjectCache();
  
  int Read(int object_no, int offset_in_object, 
           char* buffer, int bytes_to_read,
           ObjectReader* reader);

  void Write(int object_no, int offset_in_object, 
             const char* buffer, int bytes_to_write,
             ObjectReader* reader);

  void Flush(ObjectWriter* writer);

 private:
  CachedObject* LookupObject(int object_no);

  void CollectLeasedObject();

  boost::mutex mutex_;
  typedef std::map<uint64_t, CachedObject*> Cache;
  Cache cache_;
  // Maximum number of objects to cache.
  const size_t max_objects_;
  const int object_size_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_