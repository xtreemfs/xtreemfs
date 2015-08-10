/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_UTIL_LOGGING_H_
#define CPP_INCLUDE_UTIL_LOGGING_H_

#include <ostream>
#include <string>

namespace xtreemfs {
namespace util {

enum LogLevel {
  LEVEL_EMERG = 0,
  LEVEL_ALERT = 1,
  LEVEL_CRIT = 2,
  LEVEL_ERROR = 3,
  LEVEL_WARN = 4,
  LEVEL_NOTICE = 5,
  LEVEL_INFO = 6,
  LEVEL_DEBUG = 7
};

class Logging {
 public:
  static Logging* log;

  explicit Logging(LogLevel level = LEVEL_ERROR);
  Logging(LogLevel level, std::ostream* stream);
  virtual ~Logging();

  std::ostream& getLog(LogLevel level) {
    return getLog(level, "?", 0);
  }

  std::ostream& getLog(LogLevel level, const char* file, int line);

  bool loggingActive(LogLevel level);

  void register_init();
  bool register_shutdown();

 private:
  /** Log stream. */
  std::ostream& log_stream_;

  /** Contains the pointer to the stream which has to be freed by the shutdown
   *  method. */
  std::ostream* log_file_stream_;

  LogLevel level_;

  /** Contains the number of possible instances, by counting inits and shutdowns. */
  int init_count_;

  char levelToChar(LogLevel level);
};

LogLevel stringToLevel(std::string stringLevel, LogLevel defaultLevel);

void initialize_logger(LogLevel level);
void initialize_logger(LogLevel level, std::string logfilePath);
void initialize_logger(std::string stringLevel,
                       std::string logfilePath,
                       LogLevel defaultLevel);
void shutdown_logger();

}  // namespace util
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_UTIL_LOGGING_H_
