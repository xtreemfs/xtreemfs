/*
 * Copyright (c)  2009 Juan Gonzalez de Benito,
 *                2011 Bjoern Kolbeck (Zuse Institute Berlin),
 *                2012 Matthias Noack (Zuse Institute Berlin)
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_VIVALDI_H_
#define CPP_INCLUDE_LIBXTREEMFS_VIVALDI_H_

#include <list>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/vivaldi_node.h"
#include "xtreemfs/DIRServiceClient.h"

namespace xtreemfs {

class KnownOSD {
 public:
  KnownOSD(const std::string uuid,
           const xtreemfs::pbrpc::VivaldiCoordinates& coordinates)
      : uuid(uuid),
        coordinates(coordinates) {
  }

  pbrpc::VivaldiCoordinates* get_coordinates() {
    return &this->coordinates;
  }

  std::string get_uuid() {
    return this->uuid;
  }

  void set_coordinates(xtreemfs::pbrpc::VivaldiCoordinates new_coords) {
    this->coordinates = new_coords;
  }
 private:
  std::string uuid;
  xtreemfs::pbrpc::VivaldiCoordinates coordinates;
};


class Vivaldi {
 public:
  Vivaldi(xtreemfs::rpc::Client* rpc_client,
          xtreemfs::pbrpc::DIRServiceClient* dir_client,
          UUIDIterator* dir_service_addresses,
          UUIDResolver* uuid_resolver,
          const Options& options);

  void Run();

 private:
  bool update_known_osds(std::list<KnownOSD>* updated_osds,
                         const VivaldiNode& own_node);

  xtreemfs::rpc::Client* rpc_client_;
  xtreemfs::pbrpc::DIRServiceClient* dir_client_;
  UUIDIterator* dir_service_addresses_;
  UUIDResolver* uuid_resolver_;

  /** libxtreemfs Options object which includes all program options */
  const xtreemfs::Options& options_;

  /** The PBRPC protocol requires an Auth & UserCredentials object in every
   *  request. However there are many operations which do not check the content
   *  of this operation and therefore we use bogus objects then.
   *  auth_bogus_ will always be set to the type AUTH_NONE.
   *
   *  @remark Cannot be set to const because it's modified inside the
   *          constructor VolumeImplementation(). */
  xtreemfs::pbrpc::Auth auth_bogus_;

  /** The PBRPC protocol requires an Auth & UserCredentials object in every
   *  request. However there are many operations which do not check the content
   *  of this operation and therefore we use bogus objects then.
   *  user_credentials_bogus will only contain a user "xtreems".
   *
   *  @remark Cannot be set to const because it's modified inside the
   *          constructor VolumeImplementation(). */
  xtreemfs::pbrpc::UserCredentials user_credentials_bogus_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_VIVALDI_H_
