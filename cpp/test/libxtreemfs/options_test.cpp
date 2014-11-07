#include <gtest/gtest.h>

#include "libxtreemfs/options.h"

namespace xtreemfs {

TEST(OptionsTest, TestParseCommandLineAndConfigFile) {
  const char* n_argv[] = { "executable", "--log-level=client.log" };

  xtreemfs::Options options;
  options.ParseCommandLine(2, const_cast<char**>(n_argv));

  ASSERT_EQ("INFO", options.log_file_path);
}

}  // namespace xtreemfs