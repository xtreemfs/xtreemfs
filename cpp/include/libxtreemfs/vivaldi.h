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

#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <list>
#include <string>

#include "libxtreemfs/options.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/vivaldi_node.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

namespace pbrpc {
class DIRServiceClient;
class OSDServiceClient;
}  // namespace pbrpc

namespace rpc {
class Client;
}  // namespace rpc

class SimpleUUIDIterator;
class UUIDResolver;

class KnownOSD {
 public:
  KnownOSD(const std::string& uuid,
           const xtreemfs::pbrpc::VivaldiCoordinates& coordinates)
      : uuid(uuid),
        coordinates(coordinates) {
  }

  bool operator==(const KnownOSD& other) {
    return (uuid == other.uuid) &&
           (coordinates.local_error() == other.coordinates.local_error()) &&
           (coordinates.x_coordinate() == other.coordinates.x_coordinate()) &&
           (coordinates.y_coordinate() == other.coordinates.y_coordinate());
  }

  pbrpc::VivaldiCoordinates* GetCoordinates() {
    return &coordinates;
  }

  const std::string& GetUUID() {
    return uuid;
  }

  void SetCoordinates(const pbrpc::VivaldiCoordinates& new_coords) {
    coordinates = new_coords;
  }
 private:
  std::string uuid;
  pbrpc::VivaldiCoordinates coordinates;
};

class Vivaldi {
 public:
  /**
   * @remarks   Ownership is not transferred.
   */
  Vivaldi(SimpleUUIDIterator& dir_uuid_iterator,
          UUIDResolver* uuid_resolver,
          const Options& options);
  
  void Initialize(rpc::Client* network_client);
  void Run();

  const xtreemfs::pbrpc::VivaldiCoordinates& GetVivaldiCoordinates() const;

 private:
  bool UpdateKnownOSDs(std::list<KnownOSD>* updated_osds,
                       const VivaldiNode& own_node);

  boost::scoped_ptr<pbrpc::DIRServiceClient> dir_client_;
  boost::scoped_ptr<pbrpc::OSDServiceClient> osd_client_;
  SimpleUUIDIterator& dir_uuid_iterator_;
  UUIDResolver* uuid_resolver_;

  /** Shallow copy of the Client's options, with disabled retry and interrupt
   *  functionality. */
  Options vivaldi_options_;

  /** The PBRPC protocol requires an Auth & UserCredentials object in every
   *  request. However there are many operations which do not check the content
   *  of this operation and therefore we use bogus objects then.
   *  auth_bogus_ will always be set to the type AUTH_NONE.
   *
   *  @remark Cannot be set to const because it's modified inside the
   *          constructor VolumeImplementation(). */
  pbrpc::Auth auth_bogus_;

  /** The PBRPC protocol requires an Auth & UserCredentials object in every
   *  request. However there are many operations which do not check the content
   *  of this operation and therefore we use bogus objects then.
   *  user_credentials_bogus will only contain a user "xtreems".
   *
   *  @remark Cannot be set to const because it's modified inside the
   *          constructor VolumeImplementation(). */
  pbrpc::UserCredentials user_credentials_bogus_;

  /** Mutex to serialise concurrent read and write access to
   *  my_vivaldi_coordinates_. */
  mutable boost::mutex coordinate_mutex_;

  pbrpc::VivaldiCoordinates my_vivaldi_coordinates_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_VIVALDI_H_
