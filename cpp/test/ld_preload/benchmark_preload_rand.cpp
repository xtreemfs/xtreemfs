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
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>

#include <boost/program_options.hpp>

#include "ld_preload/clock.hpp"

using namespace std;

using namespace cb::time;

namespace po = boost::program_options;


int main(int argc, char* argv[]) {

  unsigned seed = 12345;

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
    ("size", po::value<int>(&size_MiB)->required(), "Size in MiB of the file")
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
  unsigned int blocks = size_B / 4096;
  char* data = new char[size_B];
  char buffer[4096];

  // Write the file
  int file = open(path.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0777);
  ssize_t write_ret = write(file, (void*) data, size_B);
  close(file);


  // Seed pseudorandom generator
  srand(seed);

  // TODO if RAND_MAX < blocks errors

  // Run the write benchmarks
  int wfile = open(path.c_str(), O_WRONLY);
  TimeAverageVariance write_time(runs);
  for (int i = 0; i < runs; ++i)
  {
    // Calculate offset
    int r = rand();
    int b = r % blocks;
    int o = b * 4096;

    lseek(wfile, o, SEEK_SET);

    // Write single block
    WallClock clock;
    write(wfile, (void*)data, 4096);
    fsync(wfile);
    write_time.add(clock);
  }

  // Close the file
  close(wfile);


  // Run the write benchmarks
  int rfile = open(path.c_str(), O_RDONLY);
  TimeAverageVariance read_time(runs);
  for (int i = 0; i < runs; ++i)
  {
    // Calculate offset
    int r = rand();
    int b = r % blocks;
    int o = b * 4096;

    lseek(rfile, o, SEEK_SET);

    // Write single block
    WallClock clock;
    read(rfile, (void*)buffer, 4096);
    fsync(rfile);
    read_time.add(clock);
  }
  // Close the file
  close(rfile);



  // Return the results
  const double block_MiB = 4096.0 / (1024 * 1024);
  const double throughput_write = (block_MiB / (write_time.average() * 0.000001));
  const double throughput_read = (block_MiB / (read_time.average() * 0.000001));
  cout << "access\t" << "benchmark\t" << TimeAverageVariance::getHeaderString() << "\tsize (MiB)" << "\tthroughput (MiB/s)" << endl
   << access << "\t" << "in\t" << write_time.toString() << "\t" << block_MiB << "\t" << throughput_write << endl
   << access << "\t" << "out\t" << read_time.toString() << "\t" << block_MiB << "\t" << throughput_read << endl;


  // Write raw results if log prefix is set
  if (vm.count("raw_log")) {
    int raw_log_len = raw_log.length();
    raw_log.append("-in");
    write_time.toFile(raw_log);

    raw_log.resize(raw_log_len);
    raw_log.append("-out");
    read_time.toFile(raw_log);
  }

  return 0;
}
