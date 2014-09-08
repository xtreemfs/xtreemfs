/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_CALLBACK_EXECUTE_SYNC_REQUEST_H_
#define CPP_INCLUDE_LIBXTREEMFS_CALLBACK_EXECUTE_SYNC_REQUEST_H_

#include <boost/function.hpp>
#include <string>

namespace xtreemfs {

namespace rpc {
class ClientRequestCallbackInterface;
class SyncCallbackBase;
}  // namespace rpc

namespace pbrpc {
class XCap;
class XLocSet;
}  // namespace pbrpc

class UUIDIterator;
class UUIDResolver;
class Options;
class XCapHandler;
class XLocSetHandler;

class RPCOptions {
 public: 
  typedef boost::function0<int> WasInterruptedCallback;

  RPCOptions(int max_retries, 
             int retry_delay_s,
             bool delay_last_attempt,
             WasInterruptedCallback was_interrupted_cb)
     : max_retries_(max_retries), 
       retry_delay_s_(retry_delay_s),
       delay_last_attempt_(delay_last_attempt),
       was_interrupted_cb_(was_interrupted_cb) {}

  RPCOptions(int max_retries,
             int retry_delay_s,
             WasInterruptedCallback was_interrupted_cb)
     : max_retries_(max_retries), 
       retry_delay_s_(retry_delay_s),
       delay_last_attempt_(false),
       was_interrupted_cb_(was_interrupted_cb) {}

  int max_retries() const {
    return max_retries_;
  }

  int retry_delay_s() const {
    return retry_delay_s_;
  }

  bool delay_last_attempt() const {
    return delay_last_attempt_;
  }

  WasInterruptedCallback was_interrupted_cb() const {
    return was_interrupted_cb_;
  }

 private:
  int max_retries_;
  int retry_delay_s_;
  bool delay_last_attempt_;
  WasInterruptedCallback was_interrupted_cb_;
};

/** Retries to execute the synchronous request "sync_function" up to "options.
 *  max_tries" times and may get interrupted. The "uuid_iterator" object is used
 *  to retrieve UUIDs or mark them as failed.
 *  If uuid_iterator_has_addresses=true, the resolving of the UUID is skipped
 *  and the string retrieved by uuid_iterator->GetUUID() is used as address.
 *  (in this case uuid_resolver may be NULL).
 *
 *  The parameter delay_last_attempt should be set true, if this method is
 *  called with max_tries = 1 and one does the looping over the retries on its
 *  own (for instance in FileHandleImplementation::AcquireLock). If set to false
 *  this method would return immediately after the _last_ try and the caller would
 *  have to ensure the delay of options.retry_delay_s on its own.
 *
 *  Ownership of arguments is NOT transferred.
 *
 */

rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    bool uuid_iterator_has_addresses,
    XCapHandler* xcap_handler,
    xtreemfs::pbrpc::XCap* xcap_in_req,
    XLocSetHandler* xlocset_handler,
    xtreemfs::pbrpc::XLocSet* xlocset_in_req);

/** Executes the request without a xlocset handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    bool uuid_iterator_has_addresses,
    XCapHandler* xcap_handler,
    xtreemfs::pbrpc::XCap* xcap_in_req);


/** Executes the request without delaying the last try and no xcap handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    XLocSetHandler* xlocset_handler,
    xtreemfs::pbrpc::XLocSet* xlocset_in_req);

/** Executes the request without delaying the last try, no xcap and no xlocset handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options);

/** Executes the request without a xcap handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    bool uuid_iterator_has_addresses,
    XLocSetHandler* xlocset_handler,
    xtreemfs::pbrpc::XLocSet* xlocset_in_req);

/** Executes the request without a xcap and a xlocset handler. */
rpc::SyncCallbackBase* ExecuteSyncRequest(
    boost::function<rpc::SyncCallbackBase* (const std::string&)> sync_function,
    UUIDIterator* uuid_iterator,
    UUIDResolver* uuid_resolver,
    const RPCOptions& options,
    bool uuid_iterator_has_addresses);
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CALLBACK_EXECUTE_SYNC_REQUEST_H_
