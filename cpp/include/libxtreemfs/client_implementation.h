/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_CLIENT_IMPLEMENTATION_H_
#define CPP_INCLUDE_LIBXTREEMFS_CLIENT_IMPLEMENTATION_H_

#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <list>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/uuid_cache.h"
#include "libxtreemfs/uuid_resolver.h"

namespace boost {
class thread;
}  // namespace boost

namespace xtreemfs {

class Options;
class Volume;
class VolumeImplementation;

namespace pbrpc {
class DIRServiceClient;
}  // namespace pbrpc

namespace rpc {
class Client;
class SSLOptions;
}  // namespace rpc

/**
 * Default Implementation of the XtreemFS C++ client interfaces.
 */
class ClientImplementation : public Client, public UUIDResolver {
 public:
  ClientImplementation(
      const std::string& dir_service_address,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options);
  virtual ~ClientImplementation();

  virtual void Start();
  virtual void Shutdown();

  virtual Volume* OpenVolume(
      const std::string& volume_name,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options);
  virtual void CloseVolume(xtreemfs::Volume* volume);

  virtual void CreateVolume(
      const std::string& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name,
      int mode,
      const std::string& owner_username,
      const std::string& owner_groupname,
      const xtreemfs::pbrpc::AccessControlPolicyType& access_policy,
      const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
      int default_stripe_size,
      int default_stripe_width,
      const std::list<xtreemfs::pbrpc::KeyValuePair*>& volume_attributes);

  virtual void DeleteVolume(
      const std::string& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name);

  virtual xtreemfs::pbrpc::Volumes* ListVolumes(
      const std::string& mrc_address);

  virtual UUIDResolver* GetUUIDResolver();

  virtual void UUIDToAddress(const std::string& uuid, std::string* address);
  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   std::string* uuid);

 private:
  /** Auth of type AUTH_NONE which is required for most operations which do not
   *  check the authentication data (except Create, Delete, ListVolume(s)). */
  xtreemfs::pbrpc::Auth auth_bogus_;

  const std::string dir_service_address_;

  /** The auth_type of this object will always be set to AUTH_NONE. */
  // TODO(mberlin): change this when the DIR service supports real auth.
  xtreemfs::pbrpc::Auth dir_service_auth_;

  /** These credentials will be used for messages to the DIR service. */
  const xtreemfs::pbrpc::UserCredentials dir_service_user_credentials_;

  const xtreemfs::rpc::SSLOptions* dir_service_ssl_options_;

  /** Options class which contains the log_level string and logfile path. */
  const xtreemfs::Options& options_;

  std::list<VolumeImplementation*> list_open_volumes_;
  boost::mutex list_open_volumes_mutex_;

  /** Caches service UUIDs -> (address, port, TTL). */
  UUIDCache uuid_cache_;

  /** The RPC Client processes requests from a queue and executes callbacks in
   * its thread. */
  boost::scoped_ptr<xtreemfs::rpc::Client> network_client_;
  boost::scoped_ptr<boost::thread> network_client_thread_;

  /** A DIRServiceClient is a wrapper for an RPC Client. */
  boost::scoped_ptr<xtreemfs::pbrpc::DIRServiceClient> dir_service_client_;

  /** Random, non-persistent UUID to distinguish locks of different clients. */
  std::string client_uuid_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CLIENT_IMPLEMENTATION_H_
