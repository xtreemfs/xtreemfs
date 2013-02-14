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

typedef boost::function<int (int object_no, char* data)> ObjectReaderCb;
typedef boost::function<void (int object_no, const char* data, int size)> ObjectWriterCb;

class CachedObject {
 public:
  // Create the object in ReadPending state.
  CachedObject(int object_no, int object_size);
  ~CachedObject();

  // Flush data and free memory
  void Erase(const ObjectWriterCb& writer);

  int Read(int offset_in_object, char* buffer, int bytes_to_read,
           const ObjectReaderCb& reader);

  void Write(int offset_in_object, const char* buffer, 
             int bytes_to_write, const ObjectReaderCb& reader);

  void Flush(const ObjectWriterCb& writer);
  
  void Truncate(int new_object_size);

  uint64_t last_access() const;

  bool is_dirty() const;

 private:
  void ReadInternal(boost::unique_lock<boost::mutex>& lock,
                    const ObjectReaderCb& reader);

  void ReadObjectFromOSD(boost::unique_lock<boost::mutex>& lock, 
                         const ObjectReaderCb& reader);

  void WriteObjectToOSD(const ObjectWriterCb& writer);
    
  bool ReadPending();

  boost::mutex mutex_;
  std::deque<boost::condition_variable*> read_queue_;

  const int object_no_;
  const int object_size_;
  // Our buffer, always object_size_ large.
  char* data_;
  // The last object has fewer bytes than object_size_;
  int actual_size_;
  bool is_dirty_;
  uint64_t last_access_;
};

class ObjectCache {
 public:
  ObjectCache(size_t max_objects, int object_size);
  ~ObjectCache();
  
  // Read within a specific object
  int Read(int object_no, int offset_in_object, 
           char* buffer, int bytes_to_read,
           const ObjectReaderCb& reader,
           const ObjectWriterCb& writer);

  // Write within a specific object
  void Write(int object_no, int offset_in_object, 
             const char* buffer, int bytes_to_write,
             const ObjectReaderCb& reader,
             const ObjectWriterCb& writer);

  void Flush(const ObjectWriterCb& writer);

  void Truncate(int64_t new_size);

  int object_size() const;

 private:
  CachedObject* LookupObject(int object_no,
                             const ObjectWriterCb& writer);

  void CollectLeasedObject(const ObjectWriterCb& writer);

  boost::mutex mutex_;
  typedef std::map<uint64_t, CachedObject*> Cache;
  Cache cache_;
  // Maximum number of objects to cache.
  const size_t max_objects_;
  const int object_size_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_