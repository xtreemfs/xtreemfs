/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_osd.h"

#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/OSDServiceConstants.h"

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {
namespace rpc {

TestRPCServerOSD::TestRPCServerOSD() {
  interface_id_ = INTERFACE_ID_OSD;
  // Register available operations.

}

}  // namespace rpc
}  // namespace xtreemfs
