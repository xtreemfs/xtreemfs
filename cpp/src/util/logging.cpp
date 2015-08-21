/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "util/logging.h"

#ifdef WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#else
#include <ctime>
#endif

#include <boost/thread.hpp>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <ostream>
#include <string>

using namespace std;

namespace xtreemfs {
namespace util {

Logging::Logging(LogLevel level, std::ostream* stream)
    : log_stream_(*stream), log_file_stream_(stream), level_(level), init_count_(1) {
}

Logging::Logging(LogLevel level)
    : log_stream_(std::cout), log_file_stream_(NULL), level_(level), init_count_(1) {
}

Logging::~Logging() {
  if (log_file_stream_) {
    delete log_file_stream_;
  }
}

std::ostream& Logging::getLog(LogLevel level, const char* file, int line) {
#ifdef WIN32
  SYSTEMTIME st, lt;
  GetSystemTime(&st);
  GetLocalTime(&lt);
#else
  timeval current_time;
  gettimeofday(&current_time, 0);
  struct tm* tm = localtime(&current_time.tv_sec);
#endif

  log_stream_
      << "[ " << levelToChar(level) << " | "
      // NOTE(mberlin): Disabled output of __FILE__ and __LINE__ since they are
      // not used in the current (3/2012) code base.
//      << file << ":" << line << " | "

      << setiosflags(ios::dec)
#ifdef WIN32
      << setw(2) << lt.wMonth << "/" << setw(2) << lt.wDay << " "
      << setfill('0') << setw(2) << lt.wHour << ":"
      << setfill('0') << setw(2) << lt.wMinute << ":"
      << setfill('0') << setw(2) << lt.wSecond << "."
      << setfill('0') << setw(3) << lt.wMilliseconds << " | "
#else
      << setw(2) << (tm->tm_mon + 1) << "/" << setw(2) << tm->tm_mday << " "
      << setfill('0') << setw(2) << tm->tm_hour << ":"
      << setfill('0') << setw(2) << tm->tm_min << ":"
      << setfill('0') << setw(2) << tm->tm_sec << "."
      << setfill('0') << setw(3) << (current_time.tv_usec / 1000) << " | "
#endif
      << left << setfill(' ') << setw(14)
      << boost::this_thread::get_id() << " ] "
      // Reset modifiers.
      << setfill(' ') << resetiosflags(ios::hex | ios::left);
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

void Logging::register_init() {
  ++init_count_;
}

bool Logging::register_shutdown() {
  if (init_count_ > 0) {
    return (--init_count_ == 0);
  }
  return false;
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
    Logging::log->register_init();
    return;
  }

  if (!logfilePath.empty()) {
    ofstream* logfile = new std::ofstream(logfilePath.c_str(),
                                          std::ios_base::out | 
                                          std::ios_base::app);
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
    Logging::log->register_init();
    return;
  }
  Logging::log = new Logging(level);
}

void shutdown_logger() {
  // Delete the logging only if no instance is left.
  if (Logging::log && Logging::log->register_shutdown()) {
    delete Logging::log;
    Logging::log = NULL;
  }
}

Logging* Logging::log = NULL;

}  // namespace util
}  // namespace xtreemfs

