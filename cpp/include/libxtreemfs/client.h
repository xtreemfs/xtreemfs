/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_CLIENT_H_
#define CPP_INCLUDE_LIBXTREEMFS_CLIENT_H_

#include <list>
#include <string>

#include "pbrpc/RPC.pb.h"
#include "rpc/ssl_options.h"
#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

class Options;
class UUIDIterator;
class UUIDResolver;
class Volume;

/**
 * Provides methods to open, close, create, delete and list volumes and to
 * instantiate a new client object, to start and shutdown a Client object.
 */
class Client {
 public:
  /** Available client implementations which are allowed by CreateClient(). */
  enum ClientImplementationType {
    kDefaultClient
  };

  /** Returns an instance of the default Client implementation.
   * @param dir_service_address Address of the DIR service
   *                            (Format: ip-addr:port, e.g. localhost:32638)
   * @param user_credentials    Name and Groups of the user.
   * @param ssl_options         NULL if no SSL is used.
   * @param options             Has to contain loglevel string and logfile path.
   */
  static Client* CreateClient(
      const std::string& dir_service_address,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options);

  /** Returns an instance of the chosen Client implementation. */
  static Client* CreateClient(
      const std::string& dir_service_address,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options,
      ClientImplementationType type);

  virtual ~Client() {}

  /** Initialize a client.
   *
   * @remark Make sure initialize_logger was called before. */
  virtual void Start() = 0;

  /** A shutdown of a client will close all open volumes and block until all
   *  threads have exited.
   *
   *  @throws OpenFileHandlesLeftException
   */
  virtual void Shutdown() = 0;

  /** Open a volume and use the returned class to access it.
   * @remark Ownership is NOT transferred to the caller. Instead
   *         Volume->Close() has to be called to destroy the object.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws UnknownAddressSchemeException
   * @throws VolumeNotFoundException
   */
  virtual xtreemfs::Volume* OpenVolume(
      const std::string& volume_name,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options) = 0;

  // TODO(mberlin): Also provide a method which accepts a list of MRC addresses
  //                or an UUID Iterator object which contains all addresses.
  /** Creates a volume on the MRC at mrc_address using certain default values (
   *  POSIX access policy type, striping size = 128k and width = 1 (i.e. no
   *  striping), mode = 777 and owner username and groupname retrieved from the
   *  user_credentials.
   *
   * @param mrc_address     String of the form "hostname:port".
   * @param auth            Authentication data, e.g. of type AUTH_PASSWORD.
   * @param user_credentials    Username and groups of the user who executes
   *                        CreateVolume(). Not checked so far?
   * @param volume_name     Name of the new volume.
   *
   * @throws IOException
   * @throws PosixErrorException
   */
  void CreateVolume(
      const std::string& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name);

  // TODO(mberlin): Also provide a method which accepts a list of MRC addresses
  //                or an UUID Iterator object which contains all addresses.
  /** Creates a volume on the MRC at mrc_address.
   *
   * @param mrc_address     String of the form "hostname:port".
   * @param auth            Authentication data, e.g. of type AUTH_PASSWORD.
   * @param user_credentials    Username and groups of the user who executes
   *                        CreateVolume().
   * @param volume_name     Name of the new volume.
   * @param mode            Mode of the volume's root directory (in octal
   *                        representation (e.g. 511), not decimal (777)).
   * @param owner_username  Name of the owner user.
   * @param owner_groupname Name of the owner group.
   * @param access_policy_type  Access policy type (Null, Posix, Volume, ...).
   * @param default_striping_policy_type    Only RAID0 so far.
   * @param default_stripe_size     Size of an object on the OSD (in kBytes).
   * @param default_stripe_width    Number of OSDs objects of a file are striped
   *                                across.
   * @param volume_attributes   Reference to a list of key-value pairs of volume
   *                            attributes which will bet set at creation time
   *                            of the volume.
   *
   * @throws IOException
   * @throws PosixErrorException
   */
  virtual void CreateVolume(
      const std::string& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name,
      int mode,
      const std::string& owner_username,
      const std::string& owner_groupname,
      const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
      const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
      int default_stripe_size,
      int default_stripe_width,
      const std::list<xtreemfs::pbrpc::KeyValuePair*>& volume_attributes) = 0;

  // TODO(mberlin): Also provide a method which accepts a list of MRC addresses
  //                or an UUID Iterator object which contains all addresses.
  /** Deletes the volume "volume_name" at the MRC "mrc_address".
   *
   * @param mrc_address     String of the form "hostname:port".
   * @param auth            Authentication data, e.g. of type AUTH_PASSWORD.
   * @param user_credentials    Username and groups of the user who executes
   *                        CreateVolume().
   * @param volume_name     Name of the volume to be deleted.
   *
   * @throws IOException
   * @throws PosixErrorException
   */
  virtual void DeleteVolume(
      const std::string& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name) = 0;

  /** Returns the available volumes on a MRC.
   *
   * @param mrc_address     String of the form "hostname:port".
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   *
   * @remark Ownership is transferred to the caller. */
  virtual xtreemfs::pbrpc::Volumes* ListVolumes(
      const std::string& mrc_address) = 0;

  /** Returns the available volumes on a MRC.
   *
   * @param uuid_iterator_with_mrc_addresses    UUIDIterator object which
   *                                            contains MRC addresses of the
   *                                            form "hostname:port".
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   *
   * @remark Ownership of the return value is transferred to the caller. */
  virtual xtreemfs::pbrpc::Volumes* ListVolumes(
      UUIDIterator* uuid_iterator_with_mrc_addresses) = 0;

  /** Returns a pointer to a UUIDResolver object, which provides functions to
   *  resolve UUIDs to IP-Addresses and Ports.
   *
   *  @remark Ownership is NOT transferred to the caller.
   */
  virtual UUIDResolver* GetUUIDResolver() = 0;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CLIENT_H_
