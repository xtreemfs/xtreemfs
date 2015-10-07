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

  CreateVolume(mrc_address,
               auth,
               user_credentials,
               volume_name,
               511,
               "",
               "",
               xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX,
               0,
               0,
               xtreemfs::pbrpc::STRIPING_POLICY_RAID0,
               128,
               1,
               volume_attributes);
}


}  // namespace xtreemfs
