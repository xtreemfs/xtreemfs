/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <cstdio>
#include <sys/stat.h>
#include <sys/types.h>

#include <fstream>
#include <list>
#include <string>

#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/user_mapping_gridmap_globus.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::util;

class UserMappingGridmapGlobusTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    gridmap_file_path_ = "gridmap_file_globus";

    // Check if file already exists.
    struct stat stat_buf;
    if (!stat(gridmap_file_path_.c_str(), &stat_buf)) {
      ASSERT_FALSE("The temporary gridmap file does already exist");
    }

    // Create a temporary gridmap file in the working directory.
    ofstream out(gridmap_file_path_.c_str());
    out << "\"/C=DE/O=GridGermany/OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)/OU=CSR/CN=Michael Berlin\" root\n";
    out.close();

    user_mapping_.reset(new UserMappingGridmapGlobus(
        UserMapping::GetUserMappingSystemType(),
        gridmap_file_path_,
        1));  // Check every second for changes.
    user_mapping_->Start();
  }

  virtual void TearDown() {
    user_mapping_->Stop();

    // Delete gridmap file.
    remove(gridmap_file_path_.c_str());

    shutdown_logger();
  }
  std::string gridmap_file_path_;

  boost::scoped_ptr<UserMapping> user_mapping_;
};

TEST_F(UserMappingGridmapGlobusTest, TestBasicDNAndOUResolving) {
  EXPECT_EQ("CN=Michael Berlin,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE",
            user_mapping_->UIDToUsername(0));
  EXPECT_EQ(0,
            user_mapping_->UsernameToUID("CN=Michael Berlin,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE"));

  // All OUs will be returned as list of groups.
  list<string> list_of_groups;
  user_mapping_->GetGroupnames(0, 0, 0, &list_of_groups);
  ASSERT_EQ(2, list_of_groups.size());
  EXPECT_EQ("CSR",
            *(list_of_groups.begin()));
  EXPECT_EQ("Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)",
            *(++list_of_groups.begin()));

  // If a user is not found, the result of the system usermapping will be
  // returned.
  boost::scoped_ptr<UserMapping> system_user_mapping(
      UserMapping::CreateUserMapping(UserMapping::GetUserMappingSystemType()));
  string system_user = system_user_mapping->UIDToUsername(1);
  EXPECT_EQ(system_user, user_mapping_->UIDToUsername(1));
  uid_t system_user_uid = system_user_mapping->UsernameToUID("unknownuser");
  EXPECT_EQ(system_user_uid, user_mapping_->UsernameToUID("unknownuser"));

  // Groups in general do not work and always return "root" or 0.
  EXPECT_EQ("root", user_mapping_->GIDToGroupname(0));
  EXPECT_EQ("root", user_mapping_->GIDToGroupname(1));
  EXPECT_EQ(0, user_mapping_->GroupnameToGID("unknowngroup"));

  // List of groups is empty for unknown users.
  list<string> empty_list_of_groups;
  user_mapping_->GetGroupnames(1, 0, 0, &empty_list_of_groups);
  ASSERT_EQ(0, empty_list_of_groups.size());

}

TEST_F(UserMappingGridmapGlobusTest, GridmapFileReload) {
  EXPECT_EQ("CN=Michael Berlin,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE",  // NOLINT
            user_mapping_->UIDToUsername(0));
  EXPECT_EQ(0,
            user_mapping_->UsernameToUID("CN=Michael Berlin,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE"));  // NOLINT

  list<string> list_of_groups;
  user_mapping_->GetGroupnames(0, 0, 0, &list_of_groups);
  ASSERT_EQ(2, list_of_groups.size());
  EXPECT_EQ("CSR",
            *(list_of_groups.begin()));
  EXPECT_EQ("Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)",
            *(++list_of_groups.begin()));

  // Rewrite file with another entry.
  sleep(1);
  ofstream out(gridmap_file_path_.c_str());
  out << "\"/C=DE/O=GridGermany/OU=Dummy OU 1/CN=Dummy Username\" root\n";
  out.close();
  // Wait for reload.
  sleep(2);

  // Old entry is no longer visible.
  EXPECT_EQ(65534,
            user_mapping_->UsernameToUID("CN=Michael Berlin,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE"));  // NOLINT
  // New entry can be seen.
  EXPECT_EQ("CN=Dummy Username,OU=Dummy OU 1,O=GridGermany,C=DE",
            user_mapping_->UIDToUsername(0));
  EXPECT_EQ(0,
            user_mapping_->UsernameToUID("CN=Dummy Username,OU=Dummy OU 1,O=GridGermany,C=DE"));  // NOLINT
  list<string> list_of_groups_new;
  user_mapping_->GetGroupnames(0, 0, 0, &list_of_groups_new);
  ASSERT_EQ(1, list_of_groups_new.size());
  EXPECT_EQ("Dummy OU 1",
            *(list_of_groups_new.begin()));
}
