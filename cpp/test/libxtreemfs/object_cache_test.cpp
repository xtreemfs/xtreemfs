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

const int kObjectSize = 10;

class FakeOsdFile {
 public:
  FakeOsdFile() : size_(0), reads_(0), writes_(0) {
    data_.reset(new char[500]);
  }
  int Read(int object_no, char* buffer) {
    reads_++;
    int offset = object_no * kObjectSize;
    int bytes_to_read = std::min(kObjectSize, size_ - offset);
    memcpy(buffer, &data_[offset], bytes_to_read);
    return bytes_to_read;
  }
  void Write(int object_no, const char* data, int bytes_to_write) {
    writes_++;
    int offset = object_no * kObjectSize;
    memcpy(&data_[offset], data, bytes_to_write);
    size_ = std::max(size_, offset  + bytes_to_write);
  }
  boost::scoped_array<char> data_;
  int size_;
  int reads_;
  int writes_;
};

class ObjectCacheTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    util::initialize_logger(util::LEVEL_WARN);
    cache_.reset(new ObjectCache(2, kObjectSize));
    reader_ = boost::bind(&FakeOsdFile::Read, &osd_file_, _1, _2);
    writer_ = boost::bind(&FakeOsdFile::Write, &osd_file_, _1, _2, _3);
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
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  char buffer[10];
  EXPECT_EQ(9, cache_->Read(0, 0, buffer, 10, &reader_));
  EXPECT_EQ(0, strcmp(buffer, "TestData"));
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  cache_->Flush(&writer_);
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(1, osd_file_.writes_);
  EXPECT_EQ(9, osd_file_.size_);
}

}  // namespace xtreemfs
