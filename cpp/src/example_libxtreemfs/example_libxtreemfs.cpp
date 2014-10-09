/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cstring>

#include <iostream>
#include <list>
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

    xtreemfs::pbrpc::Auth auth = xtreemfs::pbrpc::Auth::default_instance();
    auth.set_auth_type(xtreemfs::pbrpc::AUTH_NONE);

    // Create a new volume named 'demo'.
    xtreemfs::pbrpc::Volumes *volumes = client->ListVolumes("localhost:32636", auth);
    bool has_volume = false;
    for(int i = 0; i < volumes->volumes_size() && !has_volume; ++i) {
        has_volume = volumes->volumes(i).name().compare("demo") == 0;
    }

    if(has_volume) {
        client->DeleteVolume("localhost:32636", auth, user_credentials, "demo");
    }
	client->CreateVolume("localhost:32636", auth, user_credentials, "demo");

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

    cout << endl << "Closing /example_libxtreemfs.txt... ";
	file->Close();
	file = NULL;
	cout << "ok!" << endl;

    // mark the file as read-only
	cout << endl << "Marking /example_libxtreemfs.txt read only... ";
    volume->SetXAttr(user_credentials, "/example_libxtreemfs.txt", "xtreemfs.read_only", "true", xtreemfs::pbrpc::XATTR_FLAGS_CREATE);
    cout << "ok!" << endl;

    // list replica(s) and their OSD(s)
    // we expect one replica and one OSD here because we created a new volume above
    xtreemfs::pbrpc::Replicas* replicas = volume->ListReplicas(user_credentials, "/example_libxtreemfs.txt");
    const int repls = replicas->replicas_size();
    cout << endl << repls << " replica(s) for /example_libxtreemfs.txt:" << endl;
    for(int i = 0; i < repls; ++i) {
        xtreemfs::pbrpc::Replica replica = replicas->replicas(i);
        const int osds = replica.osd_uuids_size();
        cout << "\t" << osds << " OSD(s) for replica " << i << ":";
        for(int j = 0; j < osds; ++j) {
            cout << " " << replica.osd_uuids(j);
        }
        cout << endl;
    }

    // grab one suitable OSD which we can use for manual replication of the file
    list<string> osd_uuids;
    volume->GetSuitableOSDs(user_credentials, "/example_libxtreemfs.txt", 1, &osd_uuids);

    // replicate to second OSD if available
    if(osd_uuids.size() > 0) {
        string osd_uuid = osd_uuids.front();
        cout << endl << "Replicating to suitable OSD " << osd_uuid << "... ";

        // add replication
		xtreemfs::pbrpc::Replica replica;
		replica.add_osd_uuids(osd_uuid);

		// read-only files have partial replication by default, we want full
		replica.set_replication_flags(xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA | xtreemfs::pbrpc::REPL_FLAG_STRATEGY_RAREST_FIRST);
		xtreemfs::pbrpc::StripingPolicy *striping = new xtreemfs::pbrpc::StripingPolicy;
		striping->set_type(xtreemfs::pbrpc::STRIPING_POLICY_RAID0);
		striping->set_stripe_size(128);
		striping->set_width(1);
		replica.set_allocated_striping_policy(striping);

		volume->AddReplica(user_credentials, "/example_libxtreemfs.txt", replica);
		cout << "ok!" << endl;
    } else {
        cout << endl << "No second OSD found for replication." << endl;
    }

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
