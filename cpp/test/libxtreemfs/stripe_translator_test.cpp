/*
 * Copyright (c) 2014 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/scoped_array.hpp>
#include <string>
#include <vector>

#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "libxtreemfs/stripe_translator.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

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
  StripeTranslator* stripe_translator = new StripeTranslatorErasureCode();

  // Define stripe of chunks.
  size_t chunk_size = 1024;
  size_t chunk_count = 2;
  size_t buffer_size = chunk_size * chunk_count;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  // Define striping policy.
  StripingPolicy striping_policy;
  striping_policy.set_type(STRIPING_POLICY_ERASURECODE);
  striping_policy.set_stripe_size(chunk_size / 1024);
  striping_policy.set_width(chunk_count);

  StripeTranslator::PolicyContainer striping_policies;
  striping_policies.push_back(&striping_policy);

  // Translate to WriteRequests.
  vector<WriteOperation> operations;
  stripe_translator->TranslateWriteRequest(
      write_buf.get(), buffer_size, 0, striping_policies, &operations);

  vector<xtreemfs::WriteOperation> expected_operations;

    for (size_t i = 0; i < chunk_count; ++i) {
      vector<size_t> osd_offsets;
      osd_offsets.push_back(i);
      expected_operations.push_back(xtreemfs::WriteOperation(
          i, osd_offsets, chunk_size, 0, write_buf.get()+(chunk_size*i)));
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
    EXPECT_EQ(expected_operations[i].data, operations[i].data);
    EXPECT_EQ(expected_operations[i].obj_number, operations[i].obj_number);
    EXPECT_EQ(expected_operations[i].req_offset, operations[i].req_offset);
  }
}


}  // namespace xtreemfs
