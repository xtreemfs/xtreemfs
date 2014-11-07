/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cstring>

#include <csignal>
#include <iostream>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"  // xtreemfs::pbrpc::UserCredentials
#include "xtreemfs/MRC.pb.h"  // xtreemfs::pbrpc::Stat

using namespace std;

xtreemfs::Client* client = NULL;
xtreemfs::FileHandle* file = NULL;
xtreemfs::Volume *volume = NULL;

xtreemfs::pbrpc::UserCredentials user_credentials;
xtreemfs::pbrpc::Auth auth;

string mrc = "localhost:32636";
string dir = "localhost:32638";

void clean_up(int signum){
  if(signum){
    cout << "caught Ctrl-C...deleting volume" << endl;
  }

  if (file != NULL) {
    // Close the file (no need to delete it, see documentation volume.h).
    cout << "Closing file..." << endl;
    file->Close();
  }

  if (volume != NULL) {
    cout << "Closing volume..." << endl;
    volume->Close();
  }

  if (client != NULL) {
    cout << "Deleting volume..." << endl;
    client-> DeleteVolume(mrc,
        auth,
        user_credentials,
        "test");
    client->Shutdown();
    delete client;
  }
  cout << "cleanup completed...exiting" << endl;
  exit(signum);
}

int main(int argc, char* argv[]) {
  user_credentials.set_username("jan");
  user_credentials.add_groups("users");
  auth.set_auth_type(xtreemfs::pbrpc::AUTH_NONE);

  signal(SIGINT, clean_up);

  size_t buf_size = 4096;
  string file_name = "4kB";
  // Every operation is executed in the context of a given user and his groups.
  // The UserCredentials object does store this information and is currently
  // (08/2011) *only* evaluated by the MRC (although the protocol requires to
  // send user_credentials to DIR and OSD, too).
  // Class which allows to change options of the library.
  xtreemfs::Options options;

  try {
    options.ParseCommandLine(argc, argv);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "Invalid parameters found, error: " << e.what() << endl << endl;
    return 1;
  }

  int return_code = 0;
  try {
    // Create a new instance of a client using the DIR service at
    // 'demo.xtreemfs.org' (default port 32638).
    client = xtreemfs::Client::CreateClient(
        dir,
        user_credentials,
        NULL,  // No SSL options.
        options);

    // Start the client (a connection to the DIR service will be setup).
    client->Start();

    std::list<xtreemfs::pbrpc::KeyValuePair*> volume_attributes;
    cout << "auth is " << auth.auth_data() << endl;
    cout << "user_credentials are " << user_credentials.username() << " " << user_credentials.groups(0) << endl;
    client->CreateVolume(mrc, //options.mrc_service_address,
                         auth,
                         user_credentials,
                         "test", //options.volume_name,
                         511,
                         "jan",
                         "users",
                         xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX,
                         0,
                         xtreemfs::pbrpc::STRIPING_POLICY_ERASURECODE, //options.default_striping_policy_type,
                         1, //options.default_stripe_size,
                         3, //options.default_stripe_width,
                         1, // default parity width
                         volume_attributes);
    volume = client->OpenVolume("test",
                                NULL,  // No SSL options.
                                options);
    // Open a file.
    file = volume->OpenFile(user_credentials,
                            file_name,
                            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
                            511);  // = 777 Octal.

    // Write to file.
    cout << "Writing " << buf_size << " Bytes\n"
            "to the file " << file_name << "..." << endl;
    char write_buf[buf_size];

    for(size_t i = 0; i < buf_size; i++)
      write_buf[i] = rand() % 256;

    file->Write(reinterpret_cast<const char*>(&write_buf),
                sizeof(write_buf),
                0);

    // Get file attributes.
    cout << "Getting file stats..." << endl;
    xtreemfs::pbrpc::Stat stat;
    volume->GetAttr(user_credentials, file_name, &stat);
    cout << "\nNew file size of " << file_name << ": "
         << stat.size() << " Bytes." << endl;

    // Read from the file.
    char read_buf[stat.size()];
    memset(&read_buf, 0, stat.size());
    file->Read(reinterpret_cast<char*>(&read_buf),
               stat.size(),  // Length.
               0);  // Offset.
    // cout << "\nReading the content of the file example_libxtreemfs.txt:\n\n"
    //      << read_buf << endl;
    cout << "comparing " << stat.size() << " bytes of read and write buffers" << endl;
    for(int i = 0; i < buf_size; i++) {
      if(read_buf[i] == write_buf[i]){
        cout << "o";
      } else {
        cout << "x";
      }
    }
    cout << endl;
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "An error occurred:\n" << e.what() << endl;
    return_code = 1;
  }

  clean_up(0);

  return return_code;
}
