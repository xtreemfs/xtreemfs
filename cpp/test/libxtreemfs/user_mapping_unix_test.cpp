/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <list>
#include <string>
#include <sys/types.h>
#include <unistd.h>

#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/user_mapping_unix.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::util;

class UserMappingUnixTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    user_mapping_ = new UserMappingUnix();
    user_mapping_->Start();
  }

  virtual void TearDown() {
    user_mapping_->Stop();
    delete user_mapping_;

    shutdown_logger();
  }

  UserMapping* user_mapping_;
};

// Test user functions.
TEST_F(UserMappingUnixTest, UsernameRootIs0) {
  // Assuming the entry "root" exists.
  EXPECT_EQ(0, user_mapping_->UsernameToUID("root"));
}

TEST_F(UserMappingUnixTest, Username1Is1) {
  // Assuming there's no user with name "1111", the number itself should be
  // returned.
  EXPECT_EQ(1111, user_mapping_->UsernameToUID("1111"));
}

TEST_F(UserMappingUnixTest, Username2Pow32Minus1IsSame) {
  // 2^32-2 would overflow in case of int32_t but remains 2^32-1 with uint32_t.
  // 2^32-1 is the reserved -1.
  EXPECT_EQ(4294967294, user_mapping_->UsernameToUID("4294967294"));
}

TEST_F(UserMappingUnixTest, UsernameMinus1isNobody) {
  // -1 is used by chown to tell that uid or gid should not change. However,
  // the reverse mapping is not possible and should map to nobody.
  // (uid_t)-1 = 4294967295.
  EXPECT_EQ(65534, user_mapping_->UsernameToUID("-1"));
}

TEST_F(UserMappingUnixTest, Username2Pow32IsNobody) {
  // Out of range of uid_t.
  EXPECT_EQ(65534, user_mapping_->UsernameToUID("4294967296"));
}

TEST_F(UserMappingUnixTest, UsernameMinus2Pow31Minus1IsNobody) {
  // Negative values should be detected as errors (except -1).
  // Even -2^31-1 which must not overflow to 2^32-1.
  EXPECT_EQ(65534, user_mapping_->UsernameToUID("-2147483649"));
}

TEST_F(UserMappingUnixTest, Username1aIsNobody) {
  // Assuming the user "1a" does not exist.
  EXPECT_EQ(65534, user_mapping_->UsernameToUID("1a"));
}

TEST_F(UserMappingUnixTest, UID0isRoot) {
  // Assuming the entry "root" exists.
  EXPECT_EQ("root", user_mapping_->UIDToUsername(0));
}

TEST_F(UserMappingUnixTest, UID1111Is1111) {
  // Assuming user with id 1111 does not exist.
  EXPECT_EQ("1111", user_mapping_->UIDToUsername(1111));
}

TEST_F(UserMappingUnixTest, UID2Pow32Minus2IsSame) {
  // Assuming the group "4294967294" does not exist in the system.
  // 4294967294 is the max allowed id as chown's -1 corresponds to 4294967295.
  EXPECT_EQ("4294967294", user_mapping_->UIDToUsername(4294967294));
}

TEST_F(UserMappingUnixTest, UID2Pow32Minus1IsMinus1) {
  // (uid_t)-1 = 4294967295.
  EXPECT_EQ("-1", user_mapping_->UIDToUsername((uid_t)-1));
}

/** Tests for the range of UIDs from 0 to 2^16, if they do exist on this system
 *  and check if the mapping back from the user name to the UID does work.
 *
 *  In case there exists no user name for a UID, the UID itself will be
 *  returned as string. Therefore, this test should work on the complete range
 *  of UIDs (except for the special ID (uid_t)-1 (= 4294967295).
 *
 *  We only test till 2^16 (65536) and not the complete range (4294967294). */
TEST_F(UserMappingUnixTest, UIDsFrom0To2Pow16MapBackCorrectlyToUsernames) {
  for (uid_t uid = 0; uid <= 65536; uid++) {
    std::string username_for_uid = user_mapping_->UIDToUsername(uid);
    EXPECT_EQ(uid, user_mapping_->UsernameToUID(username_for_uid));
  }
}

// Test Group functions.
TEST_F(UserMappingUnixTest, GroupnameRootIs0) {
  // Assuming the entry "root" exists.
  EXPECT_EQ(0, user_mapping_->GroupnameToGID("root"));
}

TEST_F(UserMappingUnixTest, Groupname1Is1) {
  // Assuming there's no user with name "1111", the number itself should be
  // returned.
  EXPECT_EQ(1111, user_mapping_->GroupnameToGID("1111"));
}

TEST_F(UserMappingUnixTest, Groupname2Pow32Minus1IsSame) {
  // 2^32-2 would overflow in case of int32_t but remains 2^32-1 with uint32_t.
  // 2^32-1 is the reserved -1.
  EXPECT_EQ(4294967294, user_mapping_->GroupnameToGID("4294967294"));
}

TEST_F(UserMappingUnixTest, GroupnameMinus1isNobody) {
  // -1 is used by chown to tell that gid or gid should not change. However,
  // the reverse mapping is not possible and should map to nobody.
  // (gid_t)-1 = 4294967295.
  EXPECT_EQ(65534, user_mapping_->GroupnameToGID("-1"));
}

TEST_F(UserMappingUnixTest, Groupname2Pow32IsNobody) {
  // Out of range of gid_t.
  EXPECT_EQ(65534, user_mapping_->GroupnameToGID("4294967296"));
}

TEST_F(UserMappingUnixTest, GroupnameMinus2Pow31Minus1IsNobody) {
  // Negative values should be detected as errors (except -1).
  // Even -2^31-1 which must not overflow to 2^32-1.
  EXPECT_EQ(65534, user_mapping_->GroupnameToGID("-2147483649"));
}

TEST_F(UserMappingUnixTest, Groupname1aIsNobody) {
  // Assuming the user "1a" does not exist.
  EXPECT_EQ(65534, user_mapping_->GroupnameToGID("1a"));
}

TEST_F(UserMappingUnixTest, GID0isRoot) {
  // Assuming the entry "root" exists.
  EXPECT_EQ("root", user_mapping_->GIDToGroupname(0));
}

TEST_F(UserMappingUnixTest, GID1111Is1111) {
  // Assuming user with id 1111 does not exist.
  EXPECT_EQ("1111", user_mapping_->GIDToGroupname(1111));
}

TEST_F(UserMappingUnixTest, GID2Pow32Minus2IsSame) {
  // Assuming the group "4294967294" does not exist in the system.
  // 4294967294 is the max allowed id as chown's -1 corresponds to 4294967295.
  EXPECT_EQ("4294967294", user_mapping_->GIDToGroupname(4294967294));
}

TEST_F(UserMappingUnixTest, GID2Pow32Minus1IsMinus1) {
  // (gid_t)-1 = 4294967295.
  EXPECT_EQ("-1", user_mapping_->GIDToGroupname((gid_t)-1));
}

/** Tests for the range of GIDs from 0 to 2^16, if they do exist on this system
 *  and check if the mapping back from the group name to the GID does work.
 *
 *  In case there exists no group name for a GID, the GID itself will be
 *  returned as string. Therefore, this test should work on the complete range
 *  of GIDs (except for the special ID (gid_t)-1 (= 4294967295).
 *
 *  We only test till 2^16 (65536) and not the complete range (4294967294). */
TEST_F(UserMappingUnixTest, GIDsFrom0To2Pow16MapBackCorrectlyToGroupnames) {
  for (gid_t gid = 0; gid <= 65536; gid++) {
    std::string groupname_for_gid = user_mapping_->GIDToGroupname(gid);
    EXPECT_EQ(gid, user_mapping_->GroupnameToGID(groupname_for_gid));
  }
}

// GetGroupnames() tests.

/** Test if GetGroupnames returns at least the primary group of the user and
 *  its the first in list. */
TEST_F(UserMappingUnixTest, GetGroupnamesReturnsPrimaryGroupAtFirst) {
  string primary_group_of_current_user =
      user_mapping_->GIDToGroupname(getegid());
  list<string> groupnames;
  user_mapping_->GetGroupnames(0, getegid(), getpid(), &groupnames);
  ASSERT_GE(groupnames.size(), 1);

  EXPECT_EQ(primary_group_of_current_user,
            (*groupnames.begin()));
}

