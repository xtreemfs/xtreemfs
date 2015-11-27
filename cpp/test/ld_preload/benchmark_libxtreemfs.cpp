/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cstring>

#include <iostream>
#include <string>
#include <stdint.h>
#include <algorithm>

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"  // xtreemfs::pbrpc::UserCredentials
#include "xtreemfs/MRC.pb.h"  // xtreemfs::pbrpc::Stat


#include <boost/program_options.hpp>

#include "ld_preload/clock.hpp"

using namespace std;
using namespace cb::time;
namespace po = boost::program_options;


inline int64_t writeBuffered(xtreemfs::FileHandle* file, char* data, int64_t size_B, size_t buffer_B, WallClock* clock = 0, std::vector<Clock::TimeT>* times_per_buffer = 0) {
  int64_t ret = 0;

  int i = 0;
  for (int64_t offset = 0; offset < size_B; offset = offset + buffer_B, ++i) {
    size_t size = min(size_B - offset, (int64_t) buffer_B);
    int write_ret = file->Write(data, size, offset);

    if (write_ret < 0) {
      return write_ret;
    }
    ret = ret + write_ret;

    if (clock && times_per_buffer)
      (*times_per_buffer)[i] = clock->elapsed();
  }

  return ret;
}


inline int64_t readBuffered(xtreemfs::FileHandle* file, char* data, int64_t size_B, size_t buffer_B, WallClock* clock = 0, std::vector<Clock::TimeT>* times_per_buffer = 0) {
  int64_t ret = 0;

  int i = 0;
  for (int64_t offset = 0; offset < size_B; offset = offset + buffer_B, ++i) {
    size_t size = min(size_B - offset, (int64_t) buffer_B);
    int read_ret = file->Read(data, size, offset);

    if (read_ret < 0) {
      return read_ret;
    }
    ret = ret + read_ret;

    if (clock && times_per_buffer)
      (*times_per_buffer)[i] = clock->elapsed();
  }

  return ret;
}

int main(int argc, char* argv[]) {
  xtreemfs::pbrpc::UserCredentials user_credentials;
  user_credentials.set_username("benchmark_libxtreemfs");
  user_credentials.add_groups("benchmark_libxtreemfs");

  // Options
  int runs;
  int size_MiB;
  int buffer_KiB;
  string raw_log;
  string path;
  string xtreemfs_url;

  po::options_description desc("Benchmark Options");
  desc.add_options()
    ("raw_log", po::value<string>(&raw_log), "Path-prefix where raw results will be logged (path_prefix-{in,out}")
    ("xtreemfs_url", po::value(&xtreemfs_url)->required(), "Volume to use (proto://dirhost:port/volume)")
    ("runs", po::value<int>(&runs)->required(), "Times to run the benchmarks.")
    ("size", po::value<int>(&size_MiB)->required(), "Number of MiB to write")
    ("buffer", po::value<int>(&buffer_KiB)->required(), "Size of the buffer to read/write at once")
    ("path",  po::value<string>(&path)->required(), "Path to file to write and read" )
  ;

  po::positional_options_description p;
  p.add("xtreemfs_url", 1);
  p.add("runs", 1);
  p.add("size", 1);
  p.add("buffer", 1);
  p.add("path", 1);

  po::variables_map vm;

  // Class which allows to change options of the library.
  xtreemfs::Options options;

  try {
    // Parse general options and retrieve unregistered options for own parsing.
    vector<string> rem_args = options.ParseCommandLine(argc, argv);

    po::store(po::command_line_parser(rem_args).
              options(desc).positional(p).run(), vm);
    po::notify(vm);
  } catch (const std::exception& e) {
    cout << "Invalid parameters found, error: " << e.what() << endl << endl
        << desc << endl << endl
        << options.ShowCommandLineHelp() << endl << endl;
    return 1;
  }

  try {
    options.xtreemfs_url = xtreemfs_url;
    options.ParseURL(xtreemfs::kDIR);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "ParseURL, error: " << e.what() << endl << endl;
  }

  // Check for required parameters.
  if (options.service_addresses.empty()) {
    throw xtreemfs::InvalidCommandLineParametersException("missing DIR host.");
  }
  if (options.volume_name.empty()) {
    throw xtreemfs::InvalidCommandLineParametersException("missing volume name.");
  }

  // Allocate memory to write/read
  int64_t size_B = 1024 * 1024 * (int64_t) size_MiB;
  size_t buffer_B = 1024 * (size_t) buffer_KiB;
  char* data = new char[buffer_B];

  TimeAverageVariance in_time(runs);
  TimeAverageVariance out_time(runs);

  xtreemfs::Client* client = NULL;
  xtreemfs::FileHandle* in_file = NULL;
  xtreemfs::FileHandle* out_file = NULL;

  int return_code = 0;
  try {
    client = xtreemfs::Client::CreateClient(options.service_addresses,
        user_credentials, NULL, options);

    // Start the client (a connection to the DIR service will be setup).
    client->Start();

    // Open the volume
    xtreemfs::Volume *volume = NULL;
    volume = client->OpenVolume(options.volume_name, NULL, options);

    // Open in file.
    in_file = volume->OpenFile(user_credentials,
                            path,
                            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_WRONLY),
                            511);  // = 777 Octal.

    // Do 5 warmup write runs
    for (int i = 0; i < 5; ++i) {
      writeBuffered(in_file, data, size_B, buffer_B);
    }
    in_file->Flush();


    {
      // Initialize stats
      size_t tpf_size = (size_B / buffer_B) + ((size_B % buffer_B != 0) ? 1 : 0); // add one buffer if not evenly dividable
      std::vector<Clock::TimeT> times_per_buffer(tpf_size, 0.0); // times after each buffer, presized and 0-initialised
      TimeAverageVariance ta_alloc(runs);
      std::vector<TimeAverageVariance> stats_per_buffer(tpf_size, ta_alloc);
//      std::vector<TimeAverageVariance> stats_per_buffer;
//      stats_per_buffer.reserve(tpf_size);
//      for (size_t j=0; j < tpf_size; ++j)
//        stats_per_buffer.push_back(TimeAverageVariance(runs));


      // Run the write benchmarks
      for (int i = 0; i < runs; ++i)
      {
          WallClock clock;
          writeBuffered(in_file, data, size_B, buffer_B, &clock, &times_per_buffer);
          in_file->Flush();
          in_time.add(clock);

          // times_per_buffer contains times from start until writing to end of the n-th buffer
          for (size_t k = 0, j = (times_per_buffer.size() - 1); k < times_per_buffer.size(); ++k, --j)
          {
            if (j > 0)
              times_per_buffer[j] = times_per_buffer[j] - times_per_buffer[j - 1];
            stats_per_buffer[j].add(times_per_buffer[j]);
          }
      }

      // Write per buffer stats
      if (vm.count("raw_log")) {
        string per_buf_log_file(raw_log + "-per_buf_log-read.csv");
        ofstream per_buf_log(per_buf_log_file.c_str(), ios::trunc);
        per_buf_log << "block\toffset\t" << stats_per_buffer.begin()->getHeaderString() << endl;

        for (size_t j = 0; j < stats_per_buffer.size(); ++j) {
          per_buf_log << j << "\t" << ((j+1) * buffer_KiB) << "\t" << stats_per_buffer[j].toString() << endl;
        }
        per_buf_log.close();
      }
    }

    // Open out file.
    out_file =
        volume->OpenFile(user_credentials, path,
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDONLY));

    // Do 5 warmup read runs
    for (int i = 0; i < 5; ++i) {
      readBuffered(out_file, data, size_B, buffer_B);
    }
    out_file->Flush();

    {
      // Initialize stats
      size_t tpf_size = (size_B / buffer_B) + ((size_B % buffer_B != 0) ? 1 : 0); // add one buffer if not evenly dividable
      std::vector<Clock::TimeT> times_per_buffer(tpf_size, 0.0); // times after each buffer, presized and 0-initialised
      TimeAverageVariance ta_alloc(runs);
      std::vector<TimeAverageVariance> stats_per_buffer(tpf_size, ta_alloc);
//      std::vector<TimeAverageVariance> stats_per_buffer;
//      stats_per_buffer.reserve(tpf_size);
//      for (size_t j=0; j < tpf_size; ++j)
//        stats_per_buffer.push_back(TimeAverageVariance(runs));

      // Run the read benchmarks
      for (int i = 0; i < runs; ++i)
      {
          WallClock clock;
          readBuffered(out_file, data, size_B, buffer_B, &clock, &times_per_buffer);
          out_file->Flush();
          out_time.add(clock);

          // times_per_buffer contains times from start until writing to end of the n-th buffer
          for (size_t k = 0, j = (times_per_buffer.size() - 1); k < times_per_buffer.size(); ++k, --j)
          {
            if (j > 0)
              times_per_buffer[j] = times_per_buffer[j] - times_per_buffer[j - 1];
            stats_per_buffer[j].add(times_per_buffer[j]);
          }
      }

      // Write per buffer stats
      if (vm.count("raw_log")) {
        string per_buf_log_file(raw_log + "-per_buf_log-read.csv");
        ofstream per_buf_log(per_buf_log_file.c_str(), ios::trunc);
        per_buf_log << "block\toffset\t" << stats_per_buffer.begin()->getHeaderString() << endl;

        for (size_t j = 0; j < stats_per_buffer.size(); ++j) {
          per_buf_log << j << "\t" << ((j+1) * buffer_KiB) << "\t" << stats_per_buffer[j].toString() << endl;
        }
        per_buf_log.close();
      }
    }


  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "An error occurred:\n" << e.what() << endl;
    return_code = 1;
  }

  if (in_file != NULL) {
    in_file->Close();
  }

  if (out_file != NULL) {
    out_file->Close();
  }

  if (client != NULL) {
    // Shutdown() does also invoke a volume->Close().
    client->Shutdown();
    delete client;
  }


  // Return the results
  cout << "access\t" << "benchmark\t" << TimeAverageVariance::getHeaderString() << "\tsize (MiB)" << "\tbufsize (KiB)" << "\tthroughput (MiB/s)" << endl
     << "libxtreemfs\t" << "write\t" << in_time.toString() << "\t" << size_MiB << "\t" << buffer_KiB << "\t" << (size_MiB / (in_time.average() * 0.000001)) << endl
     << "libxtreemfs\t" << "read\t" << out_time.toString() << "\t" << size_MiB << "\t" << buffer_KiB << "\t" << (size_MiB / (out_time.average() * 0.000001)) << endl;

  // Write raw results if log prefix is set
  if (vm.count("raw_log")) {
    int raw_log_len = raw_log.length();
    raw_log.append("-write");
    in_time.toFile(raw_log);

    raw_log.resize(raw_log_len);
    raw_log.append("-read");
    out_time.toFile(raw_log);
  }

  return return_code;
}
