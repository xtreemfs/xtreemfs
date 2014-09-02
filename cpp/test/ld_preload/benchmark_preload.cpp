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

#include <boost/program_options.hpp>

#include "ld_preload/clock.hpp"

using namespace std;

using namespace cb::time;

namespace po = boost::program_options;


int main(int argc, char* argv[]) {

  int runs;
  int size_MiB;
  string raw_log;
  string path;
  string access;

  po::options_description desc("Options");
  desc.add_options()
    ("raw_log", po::value<string>(&raw_log), "Path-prefix where raw results will be logged (path_prefix-{in,out}")
    ("access",  po::value<string>(&access)->required(), "Required for logging." )
    ("runs", po::value<int>(&runs)->required(), "Times to run the benchmarks.")
    ("size", po::value<int>(&size_MiB)->required(), "Number of MiB to write")
    ("path",  po::value<string>(&path)->required(), "Path to file to write and read" )
  ;

  po::positional_options_description p;
  p.add("access", 1);
  p.add("runs", 1);
  p.add("size", 1);
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
  unsigned int size_B = 1024 * 1024 * size_MiB;
  char* data = new char[size_B];



  // Open the in_file
  int in_file = open(path.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0777);

  // Do 5 warmup write runs
  for (int i = 0; i < 5; ++i) {
    ssize_t write_ret = write(in_file, (void*) data, size_B);
    lseek(in_file, 0, SEEK_SET);
  }

  // Run the write benchmarks
  TimeAverageVariance in_time(runs);
  for (int i = 0; i < runs; ++i)
  {
      WallClock clock;
      ssize_t write_ret = write(in_file, (void*)data, size_B);
      in_time.add(clock);
      lseek(in_file, 0, SEEK_SET);
  }

  // Close the file
  close(in_file);



  // Open the out_file
  int out_file = open(path.c_str(), O_RDONLY);

  // Do 5 warmup read runs
  for (int i = 0; i < 5; ++i) {
    ssize_t read_ret = read(out_file, (void*)data, size_B);
    lseek(in_file, 0, SEEK_SET);
  }
  fsync(out_file);

  // Run the read benchmarks
  TimeAverageVariance out_time(runs);
  for (int i = 0; i < runs; ++i)
  {
      WallClock clock;
      ssize_t read_ret = read(out_file, (void*)data, size_B);
      fsync(out_file);
      out_time.add(clock);
      lseek(out_file, 0, SEEK_SET);
  }

  // Close the file
  close(out_file);



  // Return the results
  cout << "access\t" << "benchmark\t" << TimeAverageVariance::getHeaderString() <<  "\tsize (MiB)" << endl
      << access << "\tin\t" << in_time.toString() << "\t" << size_MiB << endl
      << access << "\tout\t" << out_time.toString() << "\t" << size_MiB << endl;

  // Write raw results if log prefix is set
  if (vm.count("raw_log")) {
    int raw_log_len = raw_log.length();
    raw_log.append("-in");
    in_time.toFile(raw_log);

    raw_log.resize(raw_log_len);
    raw_log.append("-out");
    out_time.toFile(raw_log);
  }

  return 0;
}
