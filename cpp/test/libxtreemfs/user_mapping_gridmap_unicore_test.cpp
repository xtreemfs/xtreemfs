/*
 * Copyright (c) 2011-2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/thread/thread.hpp>
#include <cstdio>
#include <sys/stat.h>
#include <sys/types.h>

#include <fstream>
#include <list>
#include <string>

#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/user_mapping_gridmap_unicore.h"
#include "util/logging.h"
#include "pbrpc/RPC.pb.h"


using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::util;
using namespace xtreemfs::pbrpc;

class UserMappingGridmapUnicoreTestGeneral : public ::testing::Test {
 protected:
  static const int kWaittimeForReload;

  virtual string GetGridmapFileContent() = 0;

  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    gridmap_file_path_ = "gridmap_file_unicore";

    // Check if file already exists.
    struct stat stat_buf;
    if (!stat(gridmap_file_path_.c_str(), &stat_buf)) {
      ASSERT_FALSE("The temporary gridmap file does already exist");
    }

    // Create a temporary gridmap file in the working directory.
    ofstream out(gridmap_file_path_.c_str());
    out << GetGridmapFileContent();
    out.close();
    ASSERT_FALSE(out.fail());

    user_mapping_.reset(new UserMappingGridmapUnicore(
        gridmap_file_path_,
        1));  // Check every second for changes.
    user_mapping_->Start();
  }

  virtual void TearDown() {
    user_mapping_->Stop();

    // Delete gridmap file.
    remove(gridmap_file_path_.c_str());

    shutdown_logger();
    atexit(google::protobuf::ShutdownProtobufLibrary);
  }

  std::string gridmap_file_path_;

  boost::scoped_ptr<UserMapping> user_mapping_;
};

const int UserMappingGridmapUnicoreTestGeneral::kWaittimeForReload = 2;

class UserMappingGridmapUnicore6Test
    : public UserMappingGridmapUnicoreTestGeneral {
 private:
  virtual string GetGridmapFileContent() {
    return "225;zib;root:dgms0006:dgls0050;user;mosgrid:lifescience;CN=Patrick Schaefer,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE\n";  // NOLINT
  }
};

TEST_F(UserMappingGridmapUnicore6Test, TestBasicDNAndOUResolving) {
  string dn = "CN=Patrick Schaefer,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE";  // NOLINT
  string result;
  user_mapping_->LocalToGlobalUsername("root", &result);
  EXPECT_EQ(dn, result);
  user_mapping_->GlobalToLocalUsername(dn, &result);
  EXPECT_EQ("root", result);

  // All OUs will be returned as list of groups.
  UserCredentials uc;
  user_mapping_->GetGroupnames("root", &uc);
  ASSERT_EQ(2, uc.groups_size());
  EXPECT_EQ("CSR", uc.groups(0));
  EXPECT_EQ("Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)",
            uc.groups(1));

  // Groups in general do not work and always return "root" or 0.
  user_mapping_->LocalToGlobalGroupname("CSR", &result);
  EXPECT_EQ("root", result);
  user_mapping_->LocalToGlobalGroupname("Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)", &result);  // NOLINT
  EXPECT_EQ("root", result);
  user_mapping_->GlobalToLocalGroupname("unknowngroup", &result);
  EXPECT_EQ("root", result);

  // List of groups is empty for unknown users.
  UserCredentials uc2;
  user_mapping_->GetGroupnames("nobody", &uc2);
  ASSERT_EQ(0, uc2.groups_size());
}

TEST_F(UserMappingGridmapUnicore6Test, GridmapFileReload) {
  string dn = "CN=Patrick Schaefer,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE";  // NOLINT

  string result;
  user_mapping_->LocalToGlobalUsername("root", &result);
  EXPECT_EQ(dn, result);
  user_mapping_->GlobalToLocalUsername(dn, &result);
  EXPECT_EQ("root", result);

  UserCredentials uc;
  user_mapping_->GetGroupnames("root", &uc);
  ASSERT_EQ(2, uc.groups_size());
  EXPECT_EQ("CSR", uc.groups(0));
  EXPECT_EQ("Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)",
            uc.groups(1));

  // Rewrite file with another entry.
  ofstream out(gridmap_file_path_.c_str());
  out << "225;zib;root:dgms0006:dgls0050;user;mosgrid:lifescience;CN=Dummy Username,OU=Dummy OU 1,O=GridGermany,C=DE\n";  // NOLINT
  out.close();
  ASSERT_FALSE(out.fail());
  // Wait for reload.
  boost::this_thread::sleep(boost::posix_time::seconds(kWaittimeForReload));

  // Old entry is no longer visible.
  user_mapping_->GlobalToLocalUsername(dn, &result);
  EXPECT_EQ(dn, result);
  // New entry can be seen.
  string new_dn = "CN=Dummy Username,OU=Dummy OU 1,O=GridGermany,C=DE";
  user_mapping_->LocalToGlobalUsername("root", &result);
  EXPECT_EQ(new_dn, result);
  user_mapping_->GlobalToLocalUsername(new_dn, &result);
  EXPECT_EQ("root", result);
  UserCredentials uc2;
  user_mapping_->GetGroupnames("root", &uc2);
  ASSERT_EQ(1, uc2.groups_size());
  EXPECT_EQ("Dummy OU 1", uc2.groups(0));
}

// Test for Unicore Version < 6.
class UserMappingGridmapUnicoreTest
    : public UserMappingGridmapUnicoreTestGeneral {
 private:
  virtual string GetGridmapFileContent() {
    return "root:dgms0006=CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE\n";
  }
};

TEST_F(UserMappingGridmapUnicoreTest, TestBasicDNAndOUResolving) {
  string dn = "CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE";

  string result;
  user_mapping_->LocalToGlobalUsername("root", &result);
  EXPECT_EQ(dn, result);
  user_mapping_->GlobalToLocalUsername(dn, &result);
  EXPECT_EQ("root", result);

  // All OUs will be returned as list of groups.
  UserCredentials uc;
  user_mapping_->GetGroupnames("root", &uc);
  ASSERT_EQ(1, uc.groups_size());
  EXPECT_EQ("CSR", uc.groups(0));

  // Groups in general do not work and always return "root" or 0.
  user_mapping_->LocalToGlobalGroupname("CSR", &result);
  EXPECT_EQ("root", result);
  user_mapping_->LocalToGlobalGroupname("Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB)", &result);  // NOLINT
  EXPECT_EQ("root", result);
  user_mapping_->GlobalToLocalGroupname("unknowngroup", &result);
  EXPECT_EQ("root", result);

  // List of groups is empty for unknown users.
  UserCredentials uc2;
  user_mapping_->GetGroupnames("nobody", &uc2);
  ASSERT_EQ(0, uc2.groups_size());
}

TEST_F(UserMappingGridmapUnicoreTest, GridmapFileReload) {
  string dn = "CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE";

  string result;
  user_mapping_->LocalToGlobalUsername("root", &result);
  EXPECT_EQ(dn, result);
  user_mapping_->GlobalToLocalUsername(dn, &result);
  EXPECT_EQ("root", result);

  UserCredentials uc;
  user_mapping_->GetGroupnames("root", &uc);
  ASSERT_EQ(1, uc.groups_size());
  EXPECT_EQ("CSR", uc.groups(0));

  // Rewrite file with another entry.
  ofstream out(gridmap_file_path_.c_str());
  out << "root:dgms0006=CN=Dummy Username,OU=Dummy OU 1,O=GridGermany,C=DE\n";  // NOLINT
  out.close();
  ASSERT_FALSE(out.fail());
  // Wait for reload.
  boost::this_thread::sleep(boost::posix_time::seconds(kWaittimeForReload));

  // Old entry is no longer visible.
  user_mapping_->GlobalToLocalUsername(dn, &result);
  EXPECT_EQ(dn, result);
  // New entry can be seen.
  string new_dn = "CN=Dummy Username,OU=Dummy OU 1,O=GridGermany,C=DE";
  user_mapping_->LocalToGlobalUsername("root", &result);
  EXPECT_EQ(new_dn, result);
  user_mapping_->GlobalToLocalUsername(new_dn, &result);
  EXPECT_EQ("root", result);
  UserCredentials uc2;
  user_mapping_->GetGroupnames("root", &uc2);
  ASSERT_EQ(1, uc2.groups_size());
  EXPECT_EQ("Dummy OU 1", uc2.groups(0));
}
