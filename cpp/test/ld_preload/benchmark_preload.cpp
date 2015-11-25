/*
 * Copyright (c) 2014 by Matthias Noack (Zuse Institute Berlin)
 *                       Johannes Dillmann (Zuse Institute Berlin)
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <iostream>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <algorithm>

#include <boost/program_options.hpp>

#include "ld_preload/clock.hpp"

using namespace std;

using namespace cb::time;

namespace po = boost::program_options;

inline int64_t writeBuffered(int file, char* data, int64_t size_B, size_t buffer_B) {
  int64_t ret = 0;

  for (int64_t offset = 0; offset < size_B; offset = offset + buffer_B) {
    size_t size = min(size_B - offset, (int64_t) buffer_B);
    ssize_t write_ret = write(file, (void*) data, size);

    if (write_ret < 0) {
      return write_ret;
    }
    ret = ret + write_ret;
  }

  return ret;
}

inline int64_t readBuffered(int file, char* data, int64_t size_B, size_t buffer_B) {
  int64_t ret = 0;

  for (int64_t offset = 0; offset < size_B; offset = offset + buffer_B) {
    size_t size = min(size_B - offset, (int64_t) buffer_B);
    ssize_t read_ret = read(file, (void*) data, size);

    if (read_ret < 0) {
      return read_ret;
    }
    ret = ret + read_ret;
  }

  return ret;
}


int main(int argc, char* argv[]) {

  int runs;
  int size_MiB;
  int buffer_KiB;
  string raw_log;
  string path;
  string access;

  po::options_description desc("Options");
  desc.add_options()
    ("raw_log", po::value<string>(&raw_log), "Path-prefix where raw results will be logged (path_prefix-{in,out}")
    ("access",  po::value<string>(&access)->required(), "Required for logging." )
    ("runs", po::value<int>(&runs)->required(), "Times to run the benchmarks.")
    ("size", po::value<int>(&size_MiB)->required(), "Number of MiB to write")
    ("buffer", po::value<int>(&buffer_KiB)->required(), "Size of the buffer to read/write at once")
    ("path",  po::value<string>(&path)->required(), "Path to file to write and read" )
  ;

  po::positional_options_description p;
  p.add("access", 1);
  p.add("runs", 1);
  p.add("size", 1);
  p.add("buffer", 1);
  p.add("path", 1);

  po::variables_map vm;

  try {
    po::store(po::command_line_parser(argc, argv).
              options(desc).positional(p).run(), vm);
    po::notify(vm);
  } catch(const std::exception& e) {
    // Show usage message
    cout << "Invalid Options: " <<  e.what() << endl
        << desc << endl;

    return 1;
  }

  // Allocate memory to write/read
  int64_t size_B = 1024 * 1024 * (int64_t) size_MiB;
  size_t buffer_B = 1024 * (size_t) buffer_KiB;
  char* data = new char[buffer_B];



  // Open the in_file
  int in_file = open(path.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0777);

  // Do 5 warmup write runs
  for (int i = 0; i < 5; ++i) {
    lseek(in_file, 0, SEEK_SET);
    writeBuffered(in_file, data, size_B, buffer_B);
  }

  // Run the write benchmarks
  TimeAverageVariance in_time(runs);
  for (int i = 0; i < runs; ++i) {
    lseek(in_file, 0, SEEK_SET);

    WallClock clock;
    int64_t write_ret = writeBuffered(in_file, data, size_B, buffer_B);
    fsync(in_file);
    in_time.add(clock);

    if (write_ret < 0) {
      perror("Write error:");
      return 1;
    }
  }

  // Close the file
  close(in_file);



  // Open the out_file
  int out_file = open(path.c_str(), O_RDONLY);

  // Do 5 warmup read runs
  for (int i = 0; i < 5; ++i) {
    lseek(out_file, 0, SEEK_SET);
    int64_t read_ret = readBuffered(out_file, data, size_B, buffer_B);
  }
  fsync(out_file);

  // Run the read benchmarks
  TimeAverageVariance out_time(runs);
  for (int i = 0; i < runs; ++i) {
    lseek(out_file, 0, SEEK_SET);

    WallClock clock;
    int64_t read_ret = readBuffered(out_file, data, size_B, buffer_B);
    fsync(out_file);
    out_time.add(clock);

    if (read_ret < 0) {
      perror("Read error:");
      return 1;
    }
  }

  // Close the file
  close(out_file);



  // Return the results
  cout << "access\t" << "benchmark\t" << TimeAverageVariance::getHeaderString() << "\tsize (MiB)" << "\tbufsize (KiB)" << "\tthroughput (MiB/s)" << endl
   << access << "\t" << "write\t" << in_time.toString() << "\t" << size_MiB << "\t" << buffer_KiB << "\t" << (size_MiB / (in_time.average() * 0.000001)) << endl
   << access << "\t" << "read\t" << out_time.toString() << "\t" << size_MiB << "\t" << buffer_KiB << "\t" <<(size_MiB / (out_time.average() * 0.000001)) << endl;


  // Write raw results if log prefix is set
  if (vm.count("raw_log")) {
    int raw_log_len = raw_log.length();
    raw_log.append("-write");
    in_time.toFile(raw_log);

    raw_log.resize(raw_log_len);
    raw_log.append("-read");
    out_time.toFile(raw_log);
  }

  return 0;
}
