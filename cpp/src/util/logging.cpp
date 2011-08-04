/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "util/logging.h"

#include <ctime>

#include <boost/thread.hpp>
#include <fstream>
#include <iostream>
#include <ostream>
#include <string>

using namespace std;

namespace xtreemfs {
namespace util {

Logging::Logging(LogLevel level, std::ostream* stream)
    : log_stream_(*stream), log_file_stream_(stream), level_(level) {
}

Logging::Logging(LogLevel level)
    : log_stream_(std::cout), log_file_stream_(NULL), level_(level) {
}

Logging::~Logging() {
  if (log_file_stream_) {
    delete log_file_stream_;
  }
}

std::ostream& Logging::getLog(LogLevel level, const char* file, int line) {
  timeval current_time;
  gettimeofday(&current_time, 0);
  struct tm* tm = localtime(&current_time.tv_sec);

  log_stream_
      << "[ " << levelToChar(level)
      << " | " << file << ":" << line << " | "

      << setw(2) << (tm->tm_mon + 1) << "/" << setw(2) << tm->tm_mday << " "
      << setfill('0') << setw(2) << tm->tm_hour << ":"
      << setfill('0') << setw(2) << tm->tm_min << ":"
      << setfill('0') << setw(2) << tm->tm_sec << "."
      << setfill('0') << setw(3) << (current_time.tv_usec / 1000) << " | "

      << boost::this_thread::get_id() << " ] "
      // Reset modifiers.
      << setfill(' ');
  return log_stream_;
}

char Logging::levelToChar(LogLevel level) {
  switch (level) {
    case LEVEL_EMERG: return 'e';
    case LEVEL_ALERT: return 'A';
    case LEVEL_CRIT: return 'C';
    case LEVEL_ERROR: return 'E';
    case LEVEL_WARN: return 'W';
    case LEVEL_NOTICE: return 'N';
    case LEVEL_INFO: return 'I';
    case LEVEL_DEBUG: return 'D';
  }
  std::cerr << "Could not determine log level." << std::endl;
  return 'U';  // unkown
}

bool Logging::loggingActive(LogLevel level) {
  return (level <= level_);
}

LogLevel stringToLevel(std::string stringLevel, LogLevel defaultLevel) {
  if (stringLevel == "EMERG") {
    return LEVEL_EMERG;
  } else if (stringLevel == "ALERT") {
    return LEVEL_ALERT;
  } else if (stringLevel == "CRIT") {
    return LEVEL_CRIT;
  } else if (stringLevel == "ERR") {
    return LEVEL_ERROR;
  } else if (stringLevel == "WARNING") {
    return LEVEL_WARN;
  } else if (stringLevel == "NOTICE") {
    return LEVEL_NOTICE;
  } else if (stringLevel == "INFO") {
    return LEVEL_INFO;
  } else if (stringLevel == "DEBUG") {
    return LEVEL_DEBUG;
  } else {
    // Return the default.
    return defaultLevel;
  }
}

void initialize_logger(std::string stringLevel,
                       std::string logfilePath,
                       LogLevel defaultLevel) {
  initialize_logger(stringToLevel(stringLevel, defaultLevel), logfilePath);
}

/**
 * Log to a file given by logfilePath. If logfilePath is empty,
 * stdout is used.
 */
void initialize_logger(LogLevel level, std::string logfilePath) {
  // Do not initialize the logging multiple times.
  if (Logging::log) {
    return;
  }

  if (!logfilePath.empty()) {
    ofstream* logfile = new std::ofstream(logfilePath.c_str());
    if (logfile != NULL && logfile->is_open()) {
      cerr << "Logging to file " << logfilePath.c_str() << "." << endl;
      Logging::log = new Logging(level, logfile);
      return;
    }
    cerr << "Could not log to file " << logfilePath.c_str()
        << ". Fallback to stdout." << endl;
  }
  // in case of an error, log to stdout
  Logging::log = new Logging(level);
}

/**
 * Log to stdout
 */
void initialize_logger(LogLevel level) {
  // Do not initialize the logging multiple times.
  if (Logging::log) {
    return;
  }
  Logging::log = new Logging(level);
}

void shutdown_logger() {
  delete Logging::log;
  Logging::log = NULL;
}

Logging* Logging::log = NULL;

}  // namespace util
}  // namespace xtreemfs

