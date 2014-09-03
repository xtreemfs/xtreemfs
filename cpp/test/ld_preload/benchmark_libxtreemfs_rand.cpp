/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cstring>
#include <cstdlib>

#include <iostream>
#include <string>

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

// Args: runs, log file prefix, filename,  [volume address (://dir:port/volumename)]

int main(int argc, char* argv[]) {
  xtreemfs::pbrpc::UserCredentials user_credentials;
  user_credentials.set_username("benchmark_libxtreemfs");
  user_credentials.add_groups("benchmark_libxtreemfs");

  // Options
  unsigned seed = 12345;
  int runs;
  int size_MiB;
  string raw_log;
  string path;
  string xtreemfs_url;

  po::options_description desc("Benchmark Options");
  desc.add_options()
    ("raw_log", po::value<string>(&raw_log), "Path-prefix where raw results will be logged (path_prefix-{in,out}")
    ("xtreemfs_url", po::value(&xtreemfs_url)->required(), "Volume to use (proto://dirhost:port/volume)")
    ("runs", po::value<int>(&runs)->required(), "Times to run the benchmarks.")
    ("size", po::value<int>(&size_MiB)->required(), "Number of MiB to write")
    ("path",  po::value<string>(&path)->required(), "Path to file to write and read" )
  ;

  po::positional_options_description p;
  p.add("xtreemfs_url", 1);
  p.add("runs", 1);
  p.add("size", 1);
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
  unsigned int size_B = 1024 * 1024 * size_MiB;
  unsigned int blocks = size_B / 4096;
  char* data = new char[size_B];
  char buffer[4096];

  // Seed pseudorandom generator
  std::srand(seed);

  TimeAverageVariance write_time(runs);
  TimeAverageVariance read_time(runs);

  xtreemfs::Client* client = NULL;
  xtreemfs::FileHandle* write_file = NULL;
  xtreemfs::FileHandle* read_file = NULL;

  int return_code = 0;
  try {
    client = xtreemfs::Client::CreateClient(options.service_addresses,
        user_credentials, NULL, options);

    // Start the client (a connection to the DIR service will be setup).
    client->Start();

    // Open the volume
    xtreemfs::Volume *volume = NULL;
    volume = client->OpenVolume(options.volume_name, NULL, options);

    // Create the file
    write_file = volume->OpenFile(user_credentials,
                            path,
                            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_WRONLY),
                            511);  // = 777 Octal.
    write_file->Write(data, size_B, 0);
    write_file->Flush();
    write_file->Close();


    // Run the write benchmarks
    write_file = volume->OpenFile(user_credentials, path,
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_WRONLY));

    for (int i = 0; i < runs; ++i)
    {
      // Calculate offset
      int r = rand();
      int b = r % blocks;
      int o = b * 4096;


      WallClock clock;
      write_file->Write(data, 4096, o);
      write_file->Flush();
      write_time.add(clock);
    }



    // Open out file.
    read_file = volume->OpenFile(user_credentials, path,
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDONLY));

    // Run the read benchmarks
    for (int i = 0; i < runs; ++i)
    {
      // Calculate offset
      int r = rand();
      int b = r % blocks;
      int o = b * 4096;

      WallClock clock;
      read_file->Read(buffer, 4069, o);
      read_file->Flush();
      read_time.add(clock);
    }


  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "An error occurred:\n" << e.what() << endl;
    return_code = 1;
  }

  if (write_file != NULL) {
    write_file->Close();
  }

  if (read_file != NULL) {
    read_file->Close();
  }

  if (client != NULL) {
    // Shutdown() does also invoke a volume->Close().
    client->Shutdown();
    delete client;
  }


  // Return the results
  const double block_MiB = 4096.0 / (1024 * 1024);
  const double throughput_write = (block_MiB / (write_time.average() * 0.000001));
  const double throughput_read = (block_MiB / (read_time.average() * 0.000001));
  cout << "access\t" << "benchmark\t" << TimeAverageVariance::getHeaderString() << "\tsize (MiB)" << "\tthroughput (MiB/s)" << endl
     << "libxtreemfs\t" << "in\t" << write_time.toString() << "\t" << block_MiB << "\t" << throughput_write << endl
     << "libxtreemfs\t" << "out\t" << read_time.toString() << "\t" << block_MiB << "\t" << throughput_read << endl;

  // Write raw results if log prefix is set
  if (vm.count("raw_log")) {
    int raw_log_len = raw_log.length();
    raw_log.append("-in");
    write_time.toFile(raw_log);

    raw_log.resize(raw_log_len);
    raw_log.append("-out");
    read_time.toFile(raw_log);
  }

  return return_code;
}
