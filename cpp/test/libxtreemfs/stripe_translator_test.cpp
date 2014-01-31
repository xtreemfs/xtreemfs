/*
 * Copyright (c) 2014 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/scoped_array.hpp>
#include <boost/scoped_ptr.hpp>
#include <vector>

#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "libxtreemfs/stripe_translator.h"
#include "libxtreemfs/stripe_translator_erasure_code.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;


namespace xtreemfs {

template<typename T>
::testing::AssertionResult ArraysMatch(const T* expected, const T* actual,
    size_t size) {
  for (size_t i = 0; i < size; ++i) {
    if (expected[i] != actual[i]) {
      return ::testing::AssertionFailure() << "array[" << i << "] ("
          << actual[i] << ") != expected[" << i << "] (" << expected[i] << ")";
    }
  }

  return ::testing::AssertionSuccess();
}

class StripeTranslatorTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);
  }

  virtual void TearDown() {
    shutdown_logger();
  }
};

TEST_F(StripeTranslatorTest, ErasureCodeNormalOperation) {
  // Initialize StripeTranslator.
  boost::scoped_ptr<StripeTranslator>stripe_translator(new StripeTranslatorErasureCode());

  // Define stripe of chunks.
  size_t kChunkSize = 1024;
  size_t kChunkCount = 2;
  size_t kBufferSize = kChunkSize * kChunkCount;
  boost::scoped_array<char> write_buffer(new char[kBufferSize]());

  // Define striping policy.
  StripingPolicy striping_policy;
  striping_policy.set_type(STRIPING_POLICY_ERASURECODE);
  striping_policy.set_stripe_size(kChunkSize / 1024);
  striping_policy.set_width(kChunkCount);

  StripeTranslator::PolicyContainer striping_policies;
  striping_policies.push_back(&striping_policy);

  // Translate to WriteOperations.
  vector<WriteOperation> operations;
  stripe_translator->TranslateWriteRequest(
      write_buffer.get(), kBufferSize, 0, striping_policies, &operations);

  vector<xtreemfs::WriteOperation> expected_operations;
  boost::scoped_array<char> expected_write_buffer(new char[kBufferSize]());
  for (size_t i = 0; i < kChunkCount; ++i) {
    vector<size_t> osd_offsets;
    osd_offsets.push_back(i);
    expected_operations.push_back(
        xtreemfs::WriteOperation(i, osd_offsets, kChunkSize,
            0 /* file offset */,
            expected_write_buffer.get() + (kChunkSize * i)));
  }

  // Verify WriteRequests.
  ASSERT_EQ(expected_operations.size(), operations.size());
  for (size_t i = 0; i < expected_operations.size(); ++i) {
    ASSERT_EQ(expected_operations[i].osd_offsets.size(),
              operations[i].osd_offsets.size());
    for (size_t j = 0; j < expected_operations[i].osd_offsets.size(); ++j) {
      EXPECT_EQ(expected_operations[i].osd_offsets[j],
                operations[i].osd_offsets[j]);
    }

    EXPECT_EQ(expected_operations[i].req_size, operations[i].req_size);
    EXPECT_TRUE(ArraysMatch(expected_operations[i].data,
                            operations[i].data,
                            kChunkSize));
    EXPECT_EQ(expected_operations[i].obj_number, operations[i].obj_number);
    EXPECT_EQ(expected_operations[i].req_offset, operations[i].req_offset);
  }
}


}  // namespace xtreemfs
