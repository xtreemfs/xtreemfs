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
  size_t chunk_size = 1; // in kB
  size_t buffer_size = chunk_size * 1024; // in B
  // testing 2 + 1 striping
  size_t chunk_count = 2;
  size_t parity_width = 1;
  boost::scoped_array<char> write_buffer(new char[buffer_size * chunk_count]());
  boost::scoped_array<char> expected_write_buffer(new char[buffer_size * chunk_count]());
  boost::scoped_array<char> expected_write_buffer_redundance(new char[buffer_size]());

  for(size_t i = 0; i < buffer_size; i++)
    write_buffer[i] = rand() % 256;

  // Define striping policy.
  StripingPolicy striping_policy;
  striping_policy.set_type(STRIPING_POLICY_ERASURECODE);
  striping_policy.set_stripe_size(chunk_size);
  striping_policy.set_width(chunk_count + parity_width);
  striping_policy.set_parity_width(parity_width);

  StripeTranslator::PolicyContainer striping_policies;
  striping_policies.push_back(&striping_policy);

  // Translate to WriteOperations.
  vector<WriteOperation> operations;
  stripe_translator->TranslateWriteRequest(
      write_buffer.get(), buffer_size * chunk_count, 0, striping_policies, &operations);

  for(size_t i = 0; i < buffer_size * chunk_count; i++)
    expected_write_buffer[i] = write_buffer[i];

  for(size_t i = 0; i < buffer_size; i++)
    expected_write_buffer_redundance[i] = expected_write_buffer[i] ^
                                          expected_write_buffer[i + 1024] ;

  vector<WriteOperation> expected_operations;
  for (size_t i = 0; i <= chunk_count; ++i) {
    vector<size_t> osd_offsets;

    osd_offsets.push_back(i);

    if(i == chunk_count){
      // Define expected redundant WriteOperation.
      expected_operations.push_back(
           xtreemfs::WriteOperation(i,
                                    osd_offsets,
                                    buffer_size,
                                    0 /* file offset */,
                                    expected_write_buffer_redundance.get()));
      continue;
    }

    expected_operations.push_back(
        xtreemfs::WriteOperation(i,
                                 osd_offsets,
                                 buffer_size,
                                 0 /* file offset */,
                                 expected_write_buffer.get() + (buffer_size * i))
                                );

  }


  // Verify WriteRequests.
  cout << "\n***************Comparing Write operations***************\n\n";
  ASSERT_EQ(expected_operations.size(), operations.size());
  cout << "operation vectors have the same length" << endl;
  for (size_t i = 0; i < expected_operations.size(); ++i) {
    ASSERT_EQ(expected_operations[i].osd_offsets.size(),
              operations[i].osd_offsets.size());
    cout << "OSD Offset containers in operation " << i << " have the same length" << endl;

    for (size_t j = 0; j < expected_operations[i].osd_offsets.size(); ++j) {
      EXPECT_EQ(expected_operations[i].osd_offsets[j],
                operations[i].osd_offsets[j]);
    }
    cout << "OSD Offsets match in operation " << i << endl;

    EXPECT_EQ(expected_operations[i].req_size, operations[i].req_size);
    cout << "req_size matches in operation " << i << endl;

    EXPECT_TRUE(ArraysMatch(expected_operations[i].data,
                            operations[i].data,
                            chunk_size));
    if(i < chunk_count) {
      cout << "data matches in operation " << i << endl;
    } else {
      cout << "parity data matches in operation " << i << endl;
    }

    EXPECT_EQ(expected_operations[i].obj_number, operations[i].obj_number);
    EXPECT_EQ(expected_operations[i].req_offset, operations[i].req_offset);
  }
  cout << "\n********************************************************\n";
}


}  // namespace xtreemfs
