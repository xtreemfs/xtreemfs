/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_CLIENT_IMPLEMENTATION_H_
#define CPP_INCLUDE_LIBXTREEMFS_CLIENT_IMPLEMENTATION_H_

#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/uuid_cache.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/typedefs.h"
#include "libxtreemfs/uuid_resolver.h"
#include "util/synchronized_queue.h"
#include "libxtreemfs/async_write_handler.h"

#include "xtreemfs/DIR.pb.h"

namespace boost {
class thread;
}  // namespace boost

namespace xtreemfs {

class Options;
class UUIDIterator;
class Vivaldi;
class Volume;
class VolumeImplementation;

namespace pbrpc {
class DIRServiceClient;
class OSDServiceClient;
}  // namespace pbrpc

namespace rpc {
class Client;
class SSLOptions;
class ClientTestFastLingerTimeout_LingerTests_Test;  // see FRIEND_TEST @bottom.
class ClientTestFastLingerTimeoutConnectTimeout_LingerTests_Test;
}  // namespace rpc

class DIRUUIDResolver : public UUIDResolver {
 public:
  DIRUUIDResolver(
      SimpleUUIDIterator& dir_uuid_iterator,
      const pbrpc::UserCredentials& user_credentials,
      const Options& options);

  void Initialize(rpc::Client* network_client);

  virtual void UUIDToAddress(const std::string& uuid, std::string* address);
  virtual void UUIDToAddressWithOptions(const std::string& uuid,
                                        std::string* address,
                                        const RPCOptions& options);
  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   std::string* uuid);
  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   SimpleUUIDIterator* uuid_iterator);
  virtual std::vector<std::string> VolumeNameToMRCUUIDs(const std::string& volume_name);

 private:
  SimpleUUIDIterator& dir_uuid_iterator_;
  /** The auth_type of this object will always be set to AUTH_NONE. */

  // TODO(mberlin): change this when the DIR service supports real auth.
  pbrpc::Auth dir_service_auth_;

  /** These credentials will be used for messages to the DIR service. */
  const pbrpc::UserCredentials dir_service_user_credentials_;

  /** A DIRServiceClient is a wrapper for a RPC Client. */
  boost::scoped_ptr<pbrpc::DIRServiceClient> dir_service_client_;

  /** Caches service UUIDs -> (address, port, TTL). */
  UUIDCache uuid_cache_;

  /** Options class which contains the log_level string and logfile path. */
  const Options& options_;

  pbrpc::ServiceSet* GetServicesByName(const std::string& volume_name);
};

/**
 * Default Implementation of the XtreemFS C++ client interfaces.
 */
class ClientImplementation : public Client {
 public:
  ClientImplementation(
      const ServiceAddresses& dir_service_addresses,
      const pbrpc::UserCredentials& user_credentials,
      const rpc::SSLOptions* ssl_options,
      const Options& options);
  virtual ~ClientImplementation();

  virtual void Start();
  virtual void Shutdown();

  virtual Volume* OpenVolume(
      const std::string& volume_name,
      const rpc::SSLOptions* ssl_options,
      const Options& options);
  virtual void CloseVolume(xtreemfs::Volume* volume);

  virtual void CreateVolume(
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name,
      int mode,
      const std::string& owner_username,
      const std::string& owner_groupname,
      const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
      long volume_quota,
      const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
      int default_stripe_size,
      int default_stripe_width,
      const std::map<std::string, std::string>& volume_attributes);

  virtual void CreateVolume(
      const ServiceAddresses& mrc_address,
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name,
      int mode,
      const std::string& owner_username,
      const std::string& owner_groupname,
      const pbrpc::AccessControlPolicyType& access_policy_type,
      long volume_quota,
      const pbrpc::StripingPolicyType& default_striping_policy_type,
      int default_stripe_size,
      int default_stripe_width,
      int default_parity_width,
      const std::list<pbrpc::KeyValuePair*>& volume_attributes);

  virtual void CreateVolume(
      const ServiceAddresses& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name,
      int mode,
      const std::string& owner_username,
      const std::string& owner_groupname,
      const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
      long volume_quota,
      const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
      int default_stripe_size,
      int default_stripe_width,
      int default_parity_width,
      const std::map<std::string, std::string>& volume_attributes);

  virtual void DeleteVolume(
      const ServiceAddresses& mrc_address,
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name);

  virtual void DeleteVolume(
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name);

  virtual pbrpc::Volumes* ListVolumes(
      const ServiceAddresses& mrc_addresses,
      const pbrpc::Auth& auth);

  virtual std::vector<std::string> ListVolumeNames();

  virtual UUIDResolver* GetUUIDResolver();

  virtual std::string UUIDToAddress(const std::string& uuid);

  /** Returns a ServiceSet with all services of the given type.
   *
   * @param serviceType Type of the Service
   *
   * @throws IOException
   * @throws PosixErrorException
   *
   * @remark Ownership of the return value is transferred to the caller. */
  pbrpc::ServiceSet* GetServicesByType(const xtreemfs::pbrpc::ServiceType service_type);

  /** Returns a ServiceSet with all services of the given name
   *
   * @param string Name of the Service
   *
   * @throws IOException
   * @throws PosixErrorException
   *
   * @remark Ownership of the return value is transferred to the caller. */
  pbrpc::ServiceSet* GetServicesByName(const std::string service_name);

  const pbrpc::VivaldiCoordinates& GetVivaldiCoordinates() const;

  util::SynchronizedQueue<AsyncWriteHandler::CallbackEntry>& GetAsyncWriteCallbackQueue();

 private:
  /** True if Shutdown() was executed. */
  bool was_shutdown_;

  /** Auth of type AUTH_NONE which is required for most operations which do not
   *  check the authentication data (except Create, Delete, ListVolume(s)). */
  xtreemfs::pbrpc::Auth auth_bogus_;

  /** The auth_type of this object will always be set to AUTH_NONE. */
  // TODO(mberlin): change this when the DIR service supports real auth.
  xtreemfs::pbrpc::Auth dir_service_auth_;

  /** These credentials will be used for messages to the DIR service. */
  xtreemfs::pbrpc::UserCredentials dir_service_user_credentials_;

  /** Options class which contains the log_level string and logfile path. */
  const xtreemfs::Options& options_;

  std::list<VolumeImplementation*> list_open_volumes_;
  boost::mutex list_open_volumes_mutex_;

  const rpc::SSLOptions* dir_service_ssl_options_;

  /** The RPC Client processes requests from a queue and executes callbacks in
   * its thread. */
  boost::scoped_ptr<rpc::Client> network_client_;
  boost::scoped_ptr<boost::thread> network_client_thread_;

  /** A DIRServiceClient is a wrapper for a RPC Client. */
  boost::scoped_ptr<pbrpc::DIRServiceClient> dir_service_client_;


  SimpleUUIDIterator dir_uuid_iterator_;
  DIRUUIDResolver uuid_resolver_;

  /** Random, non-persistent UUID to distinguish locks of different clients. */
  std::string client_uuid_;

  /** Vivaldi thread, periodically updates vivaldi-coordinates. */
  boost::scoped_ptr<boost::thread> vivaldi_thread_;
  boost::scoped_ptr<Vivaldi> vivaldi_;
  boost::scoped_ptr<pbrpc::OSDServiceClient> osd_service_client_;

  /** Thread that handles the callbacks for asynchronous writes. */
  boost::scoped_ptr<boost::thread> async_write_callback_thread_;
  /** Holds the Callbacks enqueued be CallFinished() (producer). They are
   *  processed by ProcessCallbacks(consumer), running in its own thread. */
  util::SynchronizedQueue<AsyncWriteHandler::CallbackEntry> async_write_callback_queue_;

  FRIEND_TEST(rpc::ClientTestFastLingerTimeout, LingerTests);
  FRIEND_TEST(rpc::ClientTestFastLingerTimeoutConnectTimeout, LingerTests);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CLIENT_IMPLEMENTATION_H_
