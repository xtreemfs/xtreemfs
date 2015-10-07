/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/client.h"

#include <string>

#include "libxtreemfs/client_implementation.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

Client* Client::CreateClient(
    const ServiceAddresses& dir_service_addresses,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const xtreemfs::rpc::SSLOptions* ssl_options,
    const Options& options) {
  return CreateClient(dir_service_addresses,
                      user_credentials,
                      ssl_options,
                      options,
                      kDefaultClient);
}

Client* Client::CreateClient(
    const ServiceAddresses& dir_service_addresses,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const xtreemfs::rpc::SSLOptions* ssl_options,
    const Options& options,
    ClientImplementationType type) {
  switch (type) {
    case kDefaultClient:
      return new ClientImplementation(dir_service_addresses,
                                      user_credentials,
                                      ssl_options,
                                      options);
    default:
      return NULL;
  }
}

void Client::CreateVolume(
      const ServiceAddresses& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name) {
  std::list<xtreemfs::pbrpc::KeyValuePair*> volume_attributes;  // Empty.

  CreateVolume(mrc_address, auth, user_credentials, volume_name, 511,
               "", "", xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX, 0,
               xtreemfs::pbrpc::STRIPING_POLICY_RAID0, 128, 1, volume_attributes);
}

void Client::CreateVolume(
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
    const std::list<xtreemfs::pbrpc::KeyValuePair*>& volume_attributes) {

  CreateVolume(mrc_address, auth, user_credentials, volume_name, mode,
      owner_username, owner_groupname, access_policy_type, volume_quota,
      default_striping_policy_type, default_stripe_size,
      default_stripe_width, 0, volume_attributes);
}

}  // namespace xtreemfs
