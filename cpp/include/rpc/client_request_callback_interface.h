/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_CLIENT_REQUEST_CALLBACK_INTERFACE_H_
#define CPP_INCLUDE_RPC_CLIENT_REQUEST_CALLBACK_INTERFACE_H_

#include "rpc/client_request.h"

namespace xtreemfs {
namespace rpc {

class ClientRequest;

class ClientRequestCallbackInterface {
 public:
  virtual ~ClientRequestCallbackInterface() {}
  virtual void RequestCompleted(ClientRequest* request) = 0;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CLIENT_REQUEST_CALLBACK_INTERFACE_H_

