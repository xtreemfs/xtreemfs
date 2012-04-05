/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_mrc.h"

#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/MRCServiceConstants.h"

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {
namespace rpc {

TestRPCServerMRC::TestRPCServerMRC() {
  interface_id_ = INTERFACE_ID_MRC;
  // Register available operations.
}

}  // namespace rpc
}  // namespace xtreemfs
