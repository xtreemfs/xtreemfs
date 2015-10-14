/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *               2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/make_shared.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/shared_ptr.hpp>
#include <sstream>
#include <string>
#include <vector>

#include "libxtreemfs/stripe_translator.h"
#include "libxtreemfs/uuid_container.h"
#include "libxtreemfs/uuid_item.h"
#include "libxtreemfs/container_uuid_iterator.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

// factory function, works fine with all default constructible iterator types
template<class IteratorType>
UUIDIterator* CreateUUIDIterator() {
  return new IteratorType();
}

// functor to encapsulate different uuid adding mechanisms
template<class IteratorType>
struct UUIDAdder;

template<>
struct UUIDAdder<SimpleUUIDIterator> {
  void operator()(UUIDIterator* it, string uuid) {
    static_cast<SimpleUUIDIterator*>(it)->AddUUID(uuid);
  }
};

template<>
struct UUIDAdder<ContainerUUIDIterator> {
  void operator()(UUIDIterator* it, string uuid) {
    created_items_.push_back(new UUIDItem(uuid));
    // safe downcast here
    static_cast<ContainerUUIDIterator*>(it)->AddUUIDItem(created_items_.back());
  }

  typedef vector<UUIDItem*> ItemPtrList;

  ~UUIDAdder() {
    for (ItemPtrList::iterator it = created_items_.begin();
        it != created_items_.end();
        ++it) {
      delete *it;
    }
    created_items_.clear();
  }
 private:
  vector<UUIDItem*> created_items_;
};


template<class IteratorType>
class UUIDIteratorTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    uuid_iterator_.reset(CreateUUIDIterator<IteratorType>());
  }

  virtual void TearDown() {
    shutdown_logger();
    atexit(google::protobuf::ShutdownProtobufLibrary);
  }

  boost::scoped_ptr<UUIDIterator> uuid_iterator_;
  UUIDAdder<IteratorType> adder_;
};

#if GTEST_HAS_TYPED_TEST

using testing::Types;

typedef Types<SimpleUUIDIterator, ContainerUUIDIterator> Implementations;

TYPED_TEST_CASE(UUIDIteratorTest, Implementations);

TYPED_TEST(UUIDIteratorTest, ListWithOneUUID) {
  string uuid1 = "uuid1";
  string current_uuid;

  this->adder_(this->uuid_iterator_.get(), uuid1);
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // If there is only one UUID and it fails, there is no other choice and it
  // should be always returned.
  for (int i = 0; i < 2; i++) {
    this->uuid_iterator_->MarkUUIDAsFailed(current_uuid);
    this->uuid_iterator_->GetUUID(&current_uuid);
    EXPECT_EQ(uuid1, current_uuid);
  }
}

TYPED_TEST(UUIDIteratorTest, ClearLeavesEmptyList) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  this->adder_(this->uuid_iterator_.get(), uuid1);
  this->adder_(this->uuid_iterator_.get(), uuid2);

  // Clear the list.
  this->uuid_iterator_->Clear();

  // There should be no element left and GetUUID should fail.
  string current_uuid;
  EXPECT_THROW({this->uuid_iterator_->GetUUID(&current_uuid);},
               UUIDIteratorListIsEmpyException);
}

TYPED_TEST(UUIDIteratorTest, ResetAfterEndOfList) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string current_uuid;

  // Fill iterator with two UUIDs.
  this->adder_(this->uuid_iterator_.get(), uuid1);
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  this->adder_(this->uuid_iterator_.get(), uuid2);
  // No entry is marked as failed yet, the first UUID is still available.
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // Mark the current entry (uuid1) as failed. uuid2 now current?
  this->uuid_iterator_->MarkUUIDAsFailed(uuid1);
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);

  // Mark uuid1 again as failed - should have no consequences.
  this->uuid_iterator_->MarkUUIDAsFailed(uuid1);
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);

  // Also mark uuid2 as failed now. Now all entries have failed. As we did reach
  // the end of the list, the status of all entries should be reset and set to
  // the first entry in the list.
  this->uuid_iterator_->MarkUUIDAsFailed(uuid2);
  for (list<UUIDItem*>::iterator it
         = this->uuid_iterator_->uuids_.begin();
       it != this->uuid_iterator_->uuids_.end();
       ++it) {
    EXPECT_FALSE((*it)->IsFailed());
  }
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
}

TYPED_TEST(UUIDIteratorTest, SetCurrentUUID) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string uuid3 = "uuid3";
  string current_uuid;

  this->adder_(this->uuid_iterator_.get(), uuid1);
  this->adder_(this->uuid_iterator_.get(), uuid2);
  this->adder_(this->uuid_iterator_.get(), uuid3);

  // First UUID is current.
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // Set third as current one.
  this->uuid_iterator_->SetCurrentUUID(uuid3);
  this->uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid3, current_uuid);

  // Fail the third (and last one): current UUID should be the first again.
  this->uuid_iterator_->MarkUUIDAsFailed(uuid3);
  this->uuid_iterator_->GetUUID(&current_uuid);
  for (list<UUIDItem*>::iterator it
         = this->uuid_iterator_->uuids_.begin();
       it != this->uuid_iterator_->uuids_.end();
       ++it) {
    EXPECT_FALSE((*it)->IsFailed());
  }
  EXPECT_EQ(uuid1, current_uuid);
}

TYPED_TEST(UUIDIteratorTest, DebugString) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string uuid3 = "uuid3";

  EXPECT_EQ("[  ]", this->uuid_iterator_->DebugString());

  this->adder_(this->uuid_iterator_.get(), uuid1);
  EXPECT_EQ("[ [ uuid1, 0] ]", this->uuid_iterator_->DebugString());

  this->adder_(this->uuid_iterator_.get(), uuid2);
  EXPECT_EQ("[ [ uuid1, 0], [ uuid2, 0] ]", this->uuid_iterator_->DebugString());

  this->adder_(this->uuid_iterator_.get(), uuid3);
  EXPECT_EQ("[ [ uuid1, 0], [ uuid2, 0], [ uuid3, 0] ]", this->uuid_iterator_->DebugString());
}

#endif  // GTEST_HAS_TYPED_TEST

// Tests for SimpleUUIDIterator

class SimpleUUIDIteratorTest : public UUIDIteratorTest<SimpleUUIDIterator> {
 public:
  void SetUp() {
    UUIDIteratorTest<SimpleUUIDIterator>::SetUp();
    simple_uuid_iterator_ =
        static_cast<SimpleUUIDIterator*>(uuid_iterator_.get());
  }
 protected:
  SimpleUUIDIterator* simple_uuid_iterator_;
};

TEST_F(SimpleUUIDIteratorTest, LaterAddsDoNotBreakIterator) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string uuid3 = "uuid3";
  string current_uuid;

  this->adder_(this->uuid_iterator_.get(), uuid1);
  this->adder_(this->uuid_iterator_.get(), uuid2);

  // Mark first uuid as failed and the second takes over.
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
  uuid_iterator_->MarkUUIDAsFailed(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);

  // Add a third uuid which should be the next element if the second does fail.
  this->adder_(this->uuid_iterator_.get(), uuid3);
  uuid_iterator_->MarkUUIDAsFailed(uuid2);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid3, current_uuid);

  // After all uuids have failed, the first will be returned again.
  uuid_iterator_->MarkUUIDAsFailed(uuid3);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
}

TEST_F(SimpleUUIDIteratorTest, SetCurrentUUIDAddsUnknownUUID) {
  string uuid1 = "uuid1";
  string current_uuid;

  // UUID1 not added so far. Setting it will automatically add it.
  uuid_iterator_->SetCurrentUUID(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);
}

TEST_F(SimpleUUIDIteratorTest, AddMultipleAddresses) {
  string uuid1 = "uuid1";
  string uuid2 = "uuid2";
  string uuid3 = "uuid3";
  string current_uuid;

  // Add the first UUID directly
  this->adder_(this->uuid_iterator_.get(), uuid1);
  // and the other from a ServiceAddreses list
  ServiceAddresses addresses;
  addresses.Add("uuid2");
  addresses.Add("uuid3");
  simple_uuid_iterator_->AddUUIDs(addresses);

  // uuid1 should still be the current uuid
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid1, current_uuid);

  // If uuid1 is marked as failed, the current should be uuid2
  uuid_iterator_->MarkUUIDAsFailed(uuid1);
  uuid_iterator_->GetUUID(&current_uuid);
  EXPECT_EQ(uuid2, current_uuid);
}

// Tests for ContainerUUIDIterator

class ContainerUUIDIteratorTest
  : public UUIDIteratorTest<ContainerUUIDIterator> {};

TEST_F(ContainerUUIDIteratorTest, CreateContainerAndGetUUID) {
  // Create container from XLocList
  pbrpc::XLocSet xlocs;
  pbrpc::StripingPolicyType type = pbrpc::STRIPING_POLICY_RAID0;
  const int replicaCount = 3;
  stringstream sstream;

  for (int i = 0; i < replicaCount; ++i) {
    pbrpc::Replica* replica = xlocs.add_replicas();
    const int stripe_width = i + 2;

    for (int j = 0; j < stripe_width; ++j) {
      sstream.str("");
      sstream << "uuid" << i << j;
      replica->add_osd_uuids(sstream.str());
      replica->mutable_striping_policy()->set_type(type);
      replica->mutable_striping_policy()->set_stripe_size(128);  // kb
      replica->mutable_striping_policy()->set_width(stripe_width);
    }
  }

  // get all striping policies
  StripeTranslator::PolicyContainer striping_policies;
  for (int i = 0; i < xlocs.replicas_size(); ++i) {
    striping_policies.push_back(&(xlocs.replicas(i).striping_policy()));
  }

  boost::shared_ptr<UUIDContainer> uuid_container =
      boost::make_shared<UUIDContainer>(xlocs);

  // NOTE: We assume that all replicas use the same striping policy type and
  //       that all replicas use the same stripe size.
  boost::scoped_ptr<StripeTranslator> translator(new StripeTranslatorRaid0());
  // Map offset to corresponding OSDs.
  std::vector<ReadOperation> operations;
  off_t read_offsets[] = {0, 128 * 1024};

  for (int i = 0; i < sizeof(read_offsets) / sizeof(off_t); ++i) {
    operations.clear();
    translator->TranslateReadRequest(NULL,
                                     128 * 1024,
                                     read_offsets[i],
                                     striping_policies,
                                     &operations);

    // Create a UUIDIterator for a specific set of offsets
    boost::scoped_ptr<ContainerUUIDIterator> uuid_iterator(
        new ContainerUUIDIterator(uuid_container,
                                  operations[0].osd_offsets));

    // check results
    string actual;
    for (int j = 0; j < replicaCount; ++j) {
      uuid_iterator->GetUUID(&actual);
      uuid_iterator->MarkUUIDAsFailed(actual);
      sstream.str("");
      sstream << "uuid" << j << operations[0].osd_offsets[j];
      EXPECT_EQ(sstream.str(), actual);
    }
  }
}

}  // namespace xtreemfs
