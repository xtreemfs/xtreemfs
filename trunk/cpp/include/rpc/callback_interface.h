/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef  CPP_INCLUDE_RPC_CALLBACK_INTERFACE_H_
#define  CPP_INCLUDE_RPC_CALLBACK_INTERFACE_H_

#include <boost/cstdint.hpp>

#include "pbrpc/RPC.pb.h"
#include "rpc/client_request.h"

namespace xtreemfs {
namespace rpc {

template <class ReturnMessageType> class CallbackInterface
    : public ClientRequestCallbackInterface {
 public:
  virtual ~CallbackInterface();

  /** To be implemented callback function which will be called by
   *  RequestCompleted() as the response was received.
   *
   * @param response_message    Pointer to the response message.
   * @param data                Response data or NULL.
   * @param data_length         Length of response data.
   * @param error               Error message or NULL if no error occurred.
   *
   * @remark Ownership of response_message, data and error is transferred to
   *         the caller.
   */
  virtual void CallFinished(ReturnMessageType* response_message,
                            char* data,
                            boost::uint32_t data_length,
                            xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
                            void* context) = 0;

  /** Executes CallFinished(), internal use only. */
  virtual void RequestCompleted(ClientRequest* request);
};

template <class ReturnMessageType>
    CallbackInterface<ReturnMessageType>::~CallbackInterface() {}

template <class ReturnMessageType>
    void CallbackInterface<ReturnMessageType>::RequestCompleted(
        ClientRequest* request) {
  assert(request->resp_message() != NULL || request->error() != NULL);
  CallFinished(dynamic_cast<ReturnMessageType*>(request->resp_message()),
               request->resp_data(),
               request->resp_data_len(),
               request->error(),
               request->context());

  delete request;
}

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CALLBACK_INTERFACE_H_
