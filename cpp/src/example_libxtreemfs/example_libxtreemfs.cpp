/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <iostream>
#include <string>

#include "util/logging.h"

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;

#include "xtreemfs/MRC.pb.h"

int main() {
  // Every operation is executed in the context of a given user and his groups.
  // The UserCredentials object does store this information.
  xtreemfs::pbrpc::UserCredentials user_credentials;
  user_credentials.set_username("mberlin");
  user_credentials.add_groups("mberlin");
  const xtreemfs::Options options;

  // Create a new instance of a client using the DIR service at 'localhost'
  // at port 32638 using the default implementation.
  xtreemfs::Client* client = xtreemfs::Client::CreateClient(
      "localhost:32638",
      user_credentials,
      NULL,  // No SSL options.
      options);

  // Start the client (a connection to the DIR service will be setup).
  client->Start();

  // Open a volume named 'test'.
  xtreemfs::Volume *volume = NULL;
  try {
    volume = client->OpenVolume(
        "test",
        NULL,  // No SSL options.
        options);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << e.what() << endl;
  }

  // Open a file.
  xtreemfs::FileHandle* file = volume->OpenFile(
      user_credentials,
      "/test.txt",
      xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDONLY);  // Flags.

  // Get file attributes.
  xtreemfs::pbrpc::Stat stat;
  volume->GetAttr(user_credentials, "/test.txt", &stat);
  cout << stat.size() << endl;

  // Write to file.
  char write_buf[] = "Testing :-).";
  file->Write(user_credentials,
              reinterpret_cast<const char*>(&write_buf),
              sizeof(write_buf),
              0);

  // Once again, now hopefully from the Cache.
  volume->GetAttr(user_credentials, "/test.txt", &stat);
  cout << stat.size() << endl;

  // Read from the file.
  char read_buf[128*1024];
  file->Read(user_credentials,
             reinterpret_cast<char*>(&read_buf),
             128 * 1024,
             0);
  cout << "Read from file: " << read_buf << endl;

  // Close the file.
  file->Close();

  // Shutdown() does also invoke a volume->Close().
  client->Shutdown();
  delete client;

  return 0;
}
