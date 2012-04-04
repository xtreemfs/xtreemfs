/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_environment.h"

#include "common/test_rpc_server_dir.h"
#include "libxtreemfs/client.h"

using namespace xtreemfs::rpc;

namespace xtreemfs {

TestEnvironment::TestEnvironment() : options(), user_credentials() {
  user_credentials.set_username("ClientTest");
  user_credentials.add_groups("ClientTest");

  dir.reset(new TestRPCServerDIR());
}

void TestEnvironment::Start() {
  dir->Start();
  
  // If the DIR server address was not explicitly overridden, set it to the
  // started test DIR server.
  if (options.service_address.empty()) {
    options.service_address = dir->GetAddress();
  }

  client.reset(Client::CreateClient(options.service_address,
                                    user_credentials,
                                    NULL,  // No SSL options.
                                    options));

  // Start the client (a connection to the DIR service will be setup).
  client->Start();
}

void TestEnvironment::Stop() {
  if (client.get()) {
    client->Shutdown();
  }

  if (dir.get()) {
    dir->Stop();
  }
}

}  // namespace xtreemfs
