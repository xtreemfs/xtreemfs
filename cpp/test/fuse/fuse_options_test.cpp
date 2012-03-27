/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <cstring>

#include <string>

#include "fuse/fuse_options.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

class FuseOptionsTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);
  }

  virtual void TearDown() {
    shutdown_logger();
  }
};

TEST_F(FuseOptionsTest, TestCommandLineMultipleOptionsPerMinusoOption) {
  int argc = 7;
  char** argv = new char*[argc];
  argv[0] = strdup("mount.xtreemfs");
  argv[1] = strdup("-o");
  argv[2] = strdup("allow_other,bogus");
  argv[3] = strdup("-o");
  argv[4] = strdup("bogus2");
  argv[5] = strdup("localhost/test");
  argv[6] = strdup("/mnt/xtreemfs");

  xtreemfs::FuseOptions options;

  ASSERT_NO_THROW({
    options.ParseCommandLine(argc, argv);
  });
  ASSERT_TRUE(!options.show_help && !options.empty_arguments_list
              && !options.show_version);

  ASSERT_EQ(3, options.fuse_options.size());
  EXPECT_EQ("bogus2", options.fuse_options[0]);
  // Split options are added to the end of fuse_options.
  EXPECT_EQ("allow_other", options.fuse_options[1]);
  EXPECT_EQ("bogus", options.fuse_options[2]);

  delete[] argv;
}

}  // namespace xtreemfs
