/*
 * Copyright (c) 2014 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include "libxtreemfs/options.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace xtreemfs::util;

namespace xtreemfs {
    
 class OptionsTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);
  }

  virtual void TearDown() {
    shutdown_logger();
  }
  
  xtreemfs::Options options_;
};

TEST_F(OptionsTest, TestFailOnMultipleLogLevelOptions) {
  const char* n_argv[] = { "executable", "--log-level=DEBUG", "-dINFO" };
  ASSERT_THROW(
      options_.ParseCommandLine(3, const_cast<char**>(n_argv)),
      InvalidCommandLineParametersException
  );
}

TEST_F(OptionsTest, TestExplicitSpecificationOverride) {
  const char* n_argv[] = {
      "executable",
      "--log-level=DEBUG",
      "-o", "-d=INFO"
  };
  options_.ParseCommandLine(4, const_cast<char**>(n_argv));
  ASSERT_EQ("DEBUG", options_.log_level_string);
}

TEST_F(OptionsTest, TestExplicitAndAlternativeCombination) {
  const char* n_argv[] = {
      "executable",
      "--log-file-path=/tmp/test.log",
      "-o", "-d=INFO"
  };
  options_.ParseCommandLine(4, const_cast<char**>(n_argv));
  ASSERT_EQ("/tmp/test.log", options_.log_file_path);
  ASSERT_EQ("INFO", options_.log_level_string);
}

TEST_F(OptionsTest, TestMultipleLongAlternativeOptions) {
  const char* n_argv[] = {
      "executable",
      "-o", "log-file-path=/tmp/test.log",
      "-o", "log-level=INFO"
  };
  options_.ParseCommandLine(5, const_cast<char**>(n_argv));
  ASSERT_EQ("/tmp/test.log", options_.log_file_path);
  ASSERT_EQ("INFO", options_.log_level_string);
}

TEST_F(OptionsTest, TestCommaSeparatedAlternativeOptions) {
  const char* n_argv[] = {
      "executable",
      "-o", "log-file-path=/tmp/test.log,-d=INFO"
  };
  options_.ParseCommandLine(3, const_cast<char**>(n_argv));
  ASSERT_EQ("/tmp/test.log", options_.log_file_path);
  ASSERT_EQ("INFO", options_.log_level_string);
}

TEST_F(OptionsTest, TestFstabExample) {
  const char* n_argv[] = {
      "executable",
      "-o", "_netdev,pkcs12-file-path=/tmp/pkcs12.p12,log-level=DEBUG"
  };
  options_.ParseCommandLine(3, const_cast<char**>(n_argv));
  ASSERT_EQ("/tmp/pkcs12.p12", options_.ssl_pkcs12_path);
  ASSERT_EQ("DEBUG", options_.log_level_string);
}

}  // namespace xtreemfs