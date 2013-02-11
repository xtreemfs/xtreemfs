#include <gtest/gtest.h>

#include <stdint.h>

#include <boost/lexical_cast.hpp>
#include <boost/scoped_ptr.hpp>
#include <string>

#include "libxtreemfs/object_cache.h"
#include "libxtreemfs/helper.h"
#include "util/logging.h"
#include "rpc/sync_callback.h"

namespace xtreemfs {

class FakeOsdFile {
 public:
  rpc::SyncCallbackBase* Read(int object_no) {
    rpc::SyncCallbackBase* result = new rpc::SyncCallbackBase();
    return result;
  }
  rpc::SyncCallbackBase* Write(int object_no, const char* data) {
    rpc::SyncCallbackBase* result = new rpc::SyncCallbackBase();
    return result;
  }
};

class ObjectCacheTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    util::initialize_logger(util::LEVEL_WARN);
    cache_.reset(new ObjectCache(10, 100));
    reader_ = boost::bind(&FakeOsdFile::Read, &osd_file_, _1);
    writer_ = boost::bind(&FakeOsdFile::Write, &osd_file_, _1, _2);
  }

  virtual void TearDown() {
    google::protobuf::ShutdownProtobufLibrary();
    util::shutdown_logger();
  }

  FakeOsdFile osd_file_;
  boost::scoped_ptr<ObjectCache> cache_;
  ObjectReader reader_;
  ObjectWriter writer_;
};


TEST_F(ObjectCacheTest, BasicUse) {
  cache_->Write(0, 0, "TestData", 9, &reader_);
}

}  // namespace xtreemfs
