/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cstring>

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

int main(int argc, char* argv[]) {
  // Every operation is executed in the context of a given user and his groups.
  // The UserCredentials object does store this information and is currently
  // (08/2011) *only* evaluated by the MRC (although the protocol requires to
  // send user_credentials to DIR and OSD, too).
  xtreemfs::pbrpc::UserCredentials user_credentials;
  user_credentials.set_username("example_libxtreemfs");
  user_credentials.add_groups("example_libxtreemfs");

  // Class which allows to change options of the library.
  xtreemfs::Options options;

  try {
    options.ParseCommandLine(argc, argv);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "Invalid parameters found, error: " << e.what() << endl << endl;
    return 1;
  }

  xtreemfs::Client* client = NULL;
  xtreemfs::FileHandle* file = NULL;
  int return_code = 0;
  try {
    // Create a new instance of a client using the DIR service at
    // 'demo.xtreemfs.org' (default port 32638).
    client = xtreemfs::Client::CreateClient(
        "demo.xtreemfs.org:32638",
        user_credentials,
        NULL,  // No SSL options.
        options);

    // Start the client (a connection to the DIR service will be setup).
    client->Start();

    // Open a volume named 'demo'.
    xtreemfs::Volume *volume = NULL;
    volume = client->OpenVolume("demo",
                                NULL,  // No SSL options.
                                options);

    // Open a file.
    file = volume->OpenFile(user_credentials,
                            "/example_libxtreemfs.txt",
                            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
                            511);  // = 777 Octal.

    // Write to file.
    cout << "Writing the string\n"
            "\n"
            "\t\"Accessed XtreemFS through the C++ libxtreemfs.\"\n"
            "\n"
            "to the file example_libxtreemfs.txt..." << endl;
    char write_buf[] = "Accessed XtreemFS through the C++ libxtreemfs.";
    file->Write(reinterpret_cast<const char*>(&write_buf),
                sizeof(write_buf),
                0);

    // Get file attributes.
    xtreemfs::pbrpc::Stat stat;
    volume->GetAttr(user_credentials, "/example_libxtreemfs.txt", &stat);
    cout << "\nNew file size of example_libxtreemfs.txt: "
         << stat.size() << " Bytes." << endl;
    // Once again, now hopefully from the Cache.
    volume->GetAttr(user_credentials, "/example_libxtreemfs.txt", &stat);
    cout << "\nFile size of example_libxtreemfs.txt again (this time retrieved"
            " from the enabled metadata cache): " << stat.size() << endl;

    // Read from the file.
    const size_t buffer_size = 128 * 1024;  // 128kB, default object size.
    char read_buf[buffer_size];
    memset(&read_buf, 0, buffer_size);
    file->Read(reinterpret_cast<char*>(&read_buf),
               buffer_size,  // Length.
               0);  // Offset.
    cout << "\nReading the content of the file example_libxtreemfs.txt:\n\n"
         << read_buf << endl;
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "An error occurred:\n" << e.what() << endl;
    return_code = 1;
  }

  if (file != NULL) {
    // Close the file (no need to delete it, see documentation volume.h).
    file->Close();
  }

  if (client != NULL) {
    // Shutdown() does also invoke a volume->Close().
    client->Shutdown();
    delete client;
  }

  return return_code;
}
