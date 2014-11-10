/*
 * Copyright (c) 2014 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include "libxtreemfs/options.h"

namespace xtreemfs {

TEST(OptionsTest, TestParseCommandLineAndConfigFile) {
  xtreemfs::Options options;
  
  {
    // Fail on multiple log level specifications.
    const char* n_argv[] = { "executable", "--log-level=DEBUG", "-dINFO" };
    ASSERT_THROW(
        options.ParseCommandLine(3, const_cast<char**>(n_argv)),
        std::exception
    );
  }
  
  {
    // Explicit specification overrides alternative specification.
    // Short alternative specification.
    const char* n_argv[] = {
        "executable",
        "--log-level=DEBUG",
        "-o", "-d=INFO"
    };
    options.ParseCommandLine(4, const_cast<char**>(n_argv));
    ASSERT_EQ("DEBUG", options.log_level_string);
  }
  
  {
    // Alternative specification complements explicit specifications.
    const char* n_argv[] = {
        "executable",
        "--log-file-path=/tmp/test.log",
        "-o", "-d=INFO"
    };
    options.ParseCommandLine(4, const_cast<char**>(n_argv));
    ASSERT_EQ("/tmp/test.log", options.log_file_path);
    ASSERT_EQ("INFO", options.log_level_string);
  }
  
  {
    // Multiple long alternative specifications.
    const char* n_argv[] = {
        "executable",
        "-o", "log-file-path=/tmp/test.log",
        "-o", "log-level=INFO"
    };
    options.ParseCommandLine(5, const_cast<char**>(n_argv));
    ASSERT_EQ("/tmp/test.log", options.log_file_path);
    ASSERT_EQ("INFO", options.log_level_string);
  }
  
  {
    // Comma separated alternative specification.
    const char* n_argv[] = {
        "executable",
        "-o", "log-file-path=/tmp/test.log,-d=INFO"
    };
    options.ParseCommandLine(3, const_cast<char**>(n_argv));
    ASSERT_EQ("/tmp/test.log", options.log_file_path);
    ASSERT_EQ("INFO", options.log_level_string);
  }
  
  {
    // Example from what can be passed in from /etc/fstab.
    const char* n_argv[] = {
        "executable",
        "-o", "_netdev,pkcs12-file-path=/tmp/pkcs12.p12,log-level=DEBUG"
    };
    options.ParseCommandLine(3, const_cast<char**>(n_argv));
    ASSERT_EQ("/tmp/pkcs12.p12", options.ssl_pkcs12_path);
    ASSERT_EQ("DEBUG", options.log_level_string);
  }
  
}

}  // namespace xtreemfs