/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/scoped_ptr.hpp>
#include <string>

#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

class UUIDIteratorTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    uuid_iterator_.reset(new UUIDIterator());
  }

  virtual void TearDown() {
    shutdown_logger();
  }

  boost::scoped_ptr<UUIDIterator> uuid_iterator_;
};

TEST_F(UUIDIteratorTest, ListWithOneUUID) {
  string uuid1 = "uuid1";
  string current_uuid;

  uuid_iterator_->AddUUID(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // If there is only one UUID and it fails, there is no other choice and it
  // should be always returned.
  for (int i = 0; i < 2; i++) {
    uuid_iterator_->MarkUUIDAsFailed(current_uuid);
    uuid_iterator_->GetUUID(&current_uuid);
    EXPECT_EQ(uuid1, current_uuid);
  }
}

TEST_F(UUIDIteratorTest, EmptyList) {
  string current_uuid;
  EXPECT_THROW({uuid_iterator_->GetUUID(&current_uuid);},
               UUIDIteratorListIsEmpyException);
}

TEST_F(UUIDIteratorTest, ClearLeavesEmptyList) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  uuid_iterator_->AddUUID(uuid1);
  uuid_iterator_->AddUUID(uuid2);

  // Clear the list.
  uuid_iterator_->Clear();

  // There should be no element left and GetUUID should fail.
  string current_uuid;
  EXPECT_THROW({uuid_iterator_->GetUUID(&current_uuid);},
               UUIDIteratorListIsEmpyException);
}

TEST_F(UUIDIteratorTest, LaterAddsDoNotBreakIterator) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string uuid3 = "uuid3";
  string current_uuid;

  uuid_iterator_->AddUUID(uuid1);
  uuid_iterator_->AddUUID(uuid2);

  // Mark first uuid as failed and the second takes over.
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
  uuid_iterator_->MarkUUIDAsFailed(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);

  // Add a third uuid which should be the next element if the second does fail.
  uuid_iterator_->AddUUID(uuid3);
  uuid_iterator_->MarkUUIDAsFailed(uuid2);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid3, current_uuid);

  // After all uuids have failed, the first will be returned again.
  uuid_iterator_->MarkUUIDAsFailed(uuid3);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
}

TEST_F(UUIDIteratorTest, ResetAfterEndOfList) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string current_uuid;

  // Fill iterator with two UUIDs.
  uuid_iterator_->AddUUID(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  uuid_iterator_->AddUUID(uuid2);
  // No entry is marked as failed yet, the first UUID is still available.
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // Mark the current entry (uuid1) as failed. uuid2 now current?
  uuid_iterator_->MarkUUIDAsFailed(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);

  // Mark uuid1 again as failed - should have no consequences.
  uuid_iterator_->MarkUUIDAsFailed(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);

  // Also mark uuid2 as failed now. Now all entries have failed. As we did reach
  // the end of the list, the status of all entries should be reset and set to
  // the first entry in the list.
  uuid_iterator_->MarkUUIDAsFailed(uuid2);
  for (list<UUIDIterator::UUIDItem*>::iterator it
         = uuid_iterator_->uuids_.begin();
       it != uuid_iterator_->uuids_.end();
       ++it) {
    EXPECT_FALSE((*it)->marked_as_failed);
  }
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
}

TEST_F(UUIDIteratorTest, SetCurrentUUID) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string uuid3 = "uuid3";
  string current_uuid;

  uuid_iterator_->AddUUID(uuid1);
  uuid_iterator_->AddUUID(uuid2);
  uuid_iterator_->AddUUID(uuid3);

  // First UUID is current.
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // Set third as current one.
  EXPECT_TRUE(uuid_iterator_->SetCurrentUUID(uuid3));
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid3, current_uuid);

  // Fail the third (and last one): current UUID should be the first again.
  uuid_iterator_->MarkUUIDAsFailed(uuid3);
  uuid_iterator_->GetUUID(&current_uuid);
  for (list<UUIDIterator::UUIDItem*>::iterator it
         = uuid_iterator_->uuids_.begin();
       it != uuid_iterator_->uuids_.end();
       ++it) {
    EXPECT_FALSE((*it)->marked_as_failed);
  }
  EXPECT_EQ(uuid1, current_uuid);
}

}  // namespace xtreemfs
