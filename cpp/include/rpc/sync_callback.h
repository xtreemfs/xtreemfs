/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_SYNC_CALLBACK_H_
#define CPP_INCLUDE_RPC_SYNC_CALLBACK_H_

#include <boost/thread.hpp>
#include <stdint.h>

#include "pbrpc/RPC.pb.h"
#include "rpc/client_request_callback_interface.h"

namespace xtreemfs {
namespace rpc {

class ClientRequest;

class SyncCallbackBase : public ClientRequestCallbackInterface {
 public:
  SyncCallbackBase();
  virtual ~SyncCallbackBase();

  /**
   * Returns if the rpc has finished (response was received or error).
   * This operation does not block.
   * @return true, if the RPC has finished
   */
  bool HasFinished();

  /**
   * Returns true if the request has failed. Blocks until
   * response is available.
   * @return true if an error occurred
   */
  bool HasFailed();

  /**
   * Returns a pointer to the error or NULL if the request was successful.
   * Blocks until response is available.
   * @return pointer to ErrorResponse, caller is responsible for deleting
   * the object or calling deleteBuffers
   */
  xtreemfs::pbrpc::RPCHeader::ErrorResponse* error();

  /**
   * Returns a pointer to the response message. Blocks until response is
   * available.
   * @return pointer to response message, caller is responsible for
   * deleting the object or calling deleteBuffers
   */
  ::google::protobuf::Message* response();

  /**
   * Returns the length of the response data or 0.
   * Blocks until response is available.
   * @return ength of the response data or 0
   */
  uint32_t data_length();

  /**
   * Returns a pointer to the response data. Blocks until response
   * is available.
   * @return pointer to response data, caller is responsible for
   * deleting[] the data or calling deleteBuffers
   */
  char* data();

  /**
   * Deletes the response objects (message, response, data)
   * This is not done automatically when the SyncCallback is deleted!
   */
  void DeleteBuffers();

  /** internal callback, ignore */
  virtual void RequestCompleted(ClientRequest* rq);

 private:
  boost::mutex cond_lock_;
  boost::condition_variable response_avail_;
  ClientRequest* request_;

  void WaitForResponse();
};

// TODO(hupfeld): update pbrpcgen to emit dynamic types.
template <class ReturnMessageType>
class SyncCallback : public SyncCallbackBase {};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_SYNC_CALLBACK_H_
