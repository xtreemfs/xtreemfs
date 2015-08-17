/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cstring>

#include <csignal>
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

using namespace std;

xtreemfs::Client* client = NULL;
xtreemfs::FileHandle* file = NULL;
xtreemfs::Volume *volume = NULL;

xtreemfs::pbrpc::UserCredentials user_credentials;
xtreemfs::pbrpc::Auth auth;

string mrc = "localhost:32636";
string dir = "localhost:32638";

int default_stripe_width = 4;
int default_parity_width = 2;

void clean_up(int signum){

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
  if(signum){
    cout << "caught Ctrl-C...deleting volume" << endl;
    exit(signum);
  }
}

int foo(size_t buf_size, size_t offset, size_t over_read){
  user_credentials.set_username("jan");
  user_credentials.add_groups("users");
  auth.set_auth_type(xtreemfs::pbrpc::AUTH_NONE);
  xtreemfs::Options options;
  std::list<xtreemfs::pbrpc::KeyValuePair*> volume_attributes;
  options.max_tries = 1;
  options.max_read_tries = 1;

  string file_name = "test_file";
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

    cout << "trying to create volume \"test\"" << endl;
    client->CreateVolume(mrc, //options.mrc_service_address,
                         auth,
                         user_credentials,
                         "test", //options.volume_name,
                         511,
                         "jan",
                         "users",
                         xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX,
                         0,
                         xtreemfs::pbrpc::STRIPING_POLICY_REED_SOL_VAN, //options.default_striping_policy_type,
                         1, //options.default_stripe_size,
                         default_stripe_width,
                         default_parity_width,
                         volume_attributes);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "Volume \"test\" exists already...deleting..." << endl;
    client->DeleteVolume(mrc,
                         auth,
                         user_credentials,
                         "test");
    cout << "...and recreating" << endl;
    client->CreateVolume(mrc, //options.mrc_service_address,
                         auth,
                         user_credentials,
                         "test", //options.volume_name,
                         511,
                         "jan",
                         "users",
                         xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX,
                         0,
                         xtreemfs::pbrpc::STRIPING_POLICY_REED_SOL_VAN, //options.default_striping_policy_type,
                         1, //options.default_stripe_size,
                         default_stripe_width,
                         default_parity_width,
                         volume_attributes);
  }
  try {
    volume = client->OpenVolume("test",
                                NULL,  // No SSL options.
                                options);
    // volume->SetXAttr(user_credentials, "/", "xtreemfs.default_sp", "{\"pattern\":\"STRIPING_POLICY_ERASURECODE\",\"width\":3,\"parity_width\":1,\"size\":1}", xtreemfs::pbrpc::XATTR_FLAGS() );
    // xtreemfs::pbrpc::listxattrResponse* xattrlist = volume->ListXAttrs(user_credentials, "/", false);
    // cout << "Volume created...xattrs:" << endl;
    // for(int i = 0; i < xattrlist->xattrs_size(); i++) {
    //   cout << "\t" << xattrlist->xattrs(i).name() << ": " << xattrlist->xattrs(i).value() << endl;
    // }

    // Open a file.
    file = volume->OpenFile(user_credentials,
                            file_name,
                            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC |
                                xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
                            511);  // = 777 Octal.

    // Write to file.
    char write_buf[buf_size];

    for(size_t i = 0; i < buf_size; i++)
      write_buf[i] = rand() % 256;

    cout << "Writing " << buf_size << " Bytes\n"
            "to the file " << file_name << "..." << endl;

    file->Write(reinterpret_cast<const char*>(&write_buf),
            buf_size,
            0);

    file->Flush();

    // file->Write(reinterpret_cast<const char*>(&write_buf) + sizeof(write_buf) / 2,
    //             sizeof(write_buf) / 2,
    //             sizeof(write_buf) / 2);

    // Get file attributes.
    cout << "Getting file stats..." << endl;
    xtreemfs::pbrpc::Stat stat;
    volume->GetAttr(user_credentials, file_name, &stat);
    cout << "\nNew file size of " << file_name << ": "
         << stat.size() << " Bytes." << endl;

    // cout << endl << "killing osd1..." << endl;
    // system("kill $(cat /tmp/xtreemfs-test/xtreemfs_osd1.pid)");
    // cout << "trying to read with one crashed osd..." << endl;

    // Read from the file.
    char read_buf[buf_size - offset + over_read];
    memset(&read_buf, 0, buf_size - offset);
    int read = file->Read(reinterpret_cast<char*>(&read_buf),
               buf_size - offset + over_read, // Length.
               offset);  // Offset.
    // file->Read(reinterpret_cast<char*>(&read_buf) + buf_size / 2,
    //            buf_size / 2,  // Length.
    //            buf_size / 2);  // Offset.
    cout << read << " bytes have been read, " << (buf_size - offset + over_read) << " were requested, expected " << (buf_size - offset) << endl;
    if (read != buf_size - offset) {
      return_code = 2;
      cout << "#######\t\tread bytes and expected bytes differ!!!!!!!!!!!!!" << endl;
    }
    cout << "comparing " << (buf_size - offset) << " bytes of read and write buffers with offset " << offset  << endl;
    char* write_loc = write_buf;
    if(memcmp(write_loc + offset, &read_buf, buf_size - offset) == 0){
      cout << "buffers match" << endl;
    } else {
      cout << "## buffers differ ##" << endl;
      return_code = 1;
      bool uneq = false;
      for (int k = offset; k < buf_size; k++) {
        if(read_buf[k - offset] == write_buf[k]){
          if (uneq) {
            uneq = false;
            cout << (k - offset -1 ) << " are unequal" << endl;
          }
        } else {
          if (!uneq) {
            uneq = true;
            cout << "bytes "<< (k - offset) << "...";
          }
        }

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

int main(int argc, char* argv[]) {

  signal(SIGINT, clean_up);

  int vals[] = {
    // TODO add short reads too
    // reads from the start
    4096, 0, 0,
    5000, 0, 0,
    5500, 0, 0,
    8000, 0, 0,
    8192, 0, 0,
    9000, 0, 0,
    9500, 0, 0,
    10240, 0, 0,
    11000, 0, 0,
    12000, 0, 0,
    // reads with offset into first object
    4096, 1020, 0,
    5000, 1020, 0,
    5500, 1020, 0,
    8000, 1020, 0,
    10240, 1020, 0,
    11000, 1020, 0,
    12000, 1020, 0,
    10240, 1020, 0,
    11000, 1020, 0,
    12000, 1020, 0,
    // reads with offset into second object
    4096, 2020, 0,
    5000, 2020, 0,
    5500, 2020, 0,
    8000, 2020, 0,
    10240, 2020, 0,
    11000, 2020, 0,
    12000, 2020, 0,
    10240, 2020, 0,
    11000, 2020, 0,
    12000, 2020, 0,
    // reads with offset into second line
    4096, 2100, 0,
    5000, 2100, 0,
    5500, 2100, 0,
    8000, 2100, 0,
    10240, 2100, 0,
    11000, 2100, 0,
    12000, 2100, 0,
    10240, 2100, 0,
    11000, 2100, 0,
    12000, 2100, 0,
    // over long reads (less than one object) without offset
    4096, 0, 1020,
    5000, 0, 1020,
    5500, 0, 1020,
    8000, 0, 1020,
    10240, 0, 1020,
    11000, 0, 1020,
    12000, 0, 1020,
    10240, 0, 1020,
    11000, 0, 1020,
    12000, 0, 1020,
    // over long reads (more than one object) without offset
    4096, 0, 2020,
    5000, 0, 2020,
    5500, 0, 2020,
    8000, 0, 2020,
    10240, 0, 2020,
    11000, 0, 2020,
    12000, 0, 2020,
    10240, 0, 2020,
    11000, 0, 2020,
    12000, 0, 2020,
    // various reads
    4096, 1050, 100,
    4096, 1020, 100,
    8000, 1020, 10,
    8000, 1020, 100,
    8000, 1020, 200,
    8192, 1020, 100,
    10240, 1050, 200,
    11000, 1050, 200,
    12000, 1050, 200,
    10240, 2020, 0,
    11000, 2020, 0,
    12000, 2020, 0,
    10240, 2050, 0,
    11000, 2050, 0,
    12000, 2050, 0,
    10240, 2050, 250,
    11000, 2050, 250,
    12000, 2050, 250,
    4096, 0, 2048,
    262144, 0, 2000
  };

  vector<int> wrong_fs;
  int runs = 0;
  for (int i = 0; i < sizeof(vals) / sizeof(int); i += 3) {
    cout << endl << "###################################################################" << endl;
    cout << "Testing with filesize " << vals[i] << " offset " << vals [i + 1] << " over_read " << vals[i + 2] << endl << endl;
    int res = foo(vals[i], vals[i + 1], vals[i + 2]);
    if (res == 1) {
      cout << "###### test case failed: filesize " << vals[i] << " offset " << vals [i + 1] << " over_read " << vals[i + 2] << endl << endl;
      for (int i = 0; i < wrong_fs.size(); i += 3) {
          cout << "in run " << wrong_fs[i] << " " << wrong_fs[i + 1] << " " << wrong_fs[i + 2] << " a wrong filesize was reported" << endl;
      }
      return 1;
    }
    if (res == 2) {
      wrong_fs.push_back(vals[i]);
      wrong_fs.push_back(vals[i + 1]);
      wrong_fs.push_back(vals[i + 2]);
    }
    runs++;
  }

  for (int i = 0; i < wrong_fs.size(); i += 3) {
      cout << "in run " << wrong_fs[i] << " " << wrong_fs[i + 1] << " " << wrong_fs[i + 2] << " a wrong filesize was reported" << endl;
  }
}
