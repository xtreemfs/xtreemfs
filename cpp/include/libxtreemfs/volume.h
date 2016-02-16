/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_VOLUME_H_
#define CPP_INCLUDE_LIBXTREEMFS_VOLUME_H_

#include <stdint.h>

#include <list>
#include <string>

#include "pbrpc/RPC.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

class FileHandle;

/*
 * A Volume object corresponds to a mounted XtreemFS volume and defines
 * the available functions to access the file system.
 */
class Volume {
 public:
  virtual ~Volume() {}

  /** Closes the Volume.
   *
   * @throws OpenFileHandlesLeftException
   */
  virtual void Close() = 0;

  /** Returns information about the volume (e.g. used/free space).
   *
   * @param user_credentials    Name and Groups of the user.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::StatVFS* StatFS(
      const xtreemfs::pbrpc::UserCredentials& user_credentials) = 0;

  /** Resolves the symbolic link at "path" and returns it in "link_target_path".
   *
   * @param user_credentials        Name and Groups of the user.
   * @param path                    Path to the symbolic link.
   * @param link_target_path[out]   String where to store the result.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void ReadLink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      std::string* link_target_path) = 0;

  /** Creates a symbolic link pointing to "target_path" at "link_path".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param target_path         Path to the target.
   * @param link_path           Path to the symbolic link.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Symlink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& target_path,
      const std::string& link_path) = 0;

  /** Creates a hard link pointing to "target_path" at "link_path".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param target_path         Path to the target.
   * @param link_path           Path to the hard link.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Link(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& target_path,
      const std::string& link_path) = 0;

  /** Tests if the subject described by "user_credentials" is allowed to access
   *  "path" as specified by "flags". "flags" is a bit mask which may contain
   *  the values ACCESS_FLAGS_{F_OK,R_OK,W_OK,X_OK}.
   *
   *  Throws a PosixErrorException if not allowed.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path                Path to the file/directory.
   * @param flags   Open flags as specified in xtreemfs::pbrpc::SYSTEM_V_FCNTL.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Access(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::ACCESS_FLAGS flags) = 0;

  /** Opens a file and returns the pointer to a FileHandle object.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file.
   * @param flags   Open flags as specified in xtreemfs::pbrpc::SYSTEM_V_FCNTL.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark Ownership is NOT transferred to the caller. Instead
   *         FileHandle->Close() has to be called to destroy the object.
   */
  virtual FileHandle* OpenFile(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags) = 0;

  /** Same as previous OpenFile() except for the additional mode parameter,
   *  which sets the permissions for the file in case SYSTEM_V_FCNTL_H_O_CREAT
   *  is specified as flag and the file will be created.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual FileHandle* OpenFile(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
      uint32_t mode) = 0;

  /** Same as previous OpenFile() except for the additional parameter
   *  "attributes" which also stores Windows FileAttributes on the MRC
   *  when creating a file. See the MSDN article "File Attribute Constants" for
   *  the list of possible  *  values e.g., here: http://msdn.microsoft.com/en-us/library/windows/desktop/gg258117%28v=vs.85%29.aspx  // NOLINT
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual FileHandle* OpenFile(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
      uint32_t mode,
      uint32_t attributes) = 0;

  /** Truncates the file to "new_file_size_ bytes.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path            Path to the file.
   * @param new_file_size   New size of file.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Truncate(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      off_t new_file_size) = 0;

  /** Retrieve the attributes of a file and writes the result in "stat".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param stat[out]   Result of the operation will be stored here.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      xtreemfs::pbrpc::Stat* stat) = 0;

  /** Retrieve the attributes of a file and writes the result in "stat".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param ignore_metadata_cache   If true, do not use the cached value.
   *                                The cache will be updated, though.
   * @param stat[out]   Result of the operation will be stored here.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      bool ignore_metadata_cache,
      xtreemfs::pbrpc::Stat* stat) = 0;

  /** Sets the attributes given by "stat" and specified in "to_set".
   *
   * @note  If the mode, uid or gid is changed, the ctime of the file will be
   *        updated according to POSIX semantics.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param stat    Stat object with attributes which will be set.
   * @param to_set  Bitmask which defines which attributes to set.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void SetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::Stat& stat,
      xtreemfs::pbrpc::Setattrs to_set) = 0;

  /** Remove the file at "path" (deletes the entry at the MRC and all objects
   *  on one OSD).
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path                Path to the file.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void Unlink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path) = 0;

  /** Rename a file or directory "path" to "new_path".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path                Old path.
   * @param new_path            New path.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   * */
  virtual void Rename(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& new_path) = 0;

  /** Creates a directory with the modes "mode".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path                Path to the new directory.
   * @param mode                Permissions of the new directory.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void MakeDirectory(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      unsigned int mode) = 0;

  /** Removes the directory at "path" which has to be empty.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the directory to be removed.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void DeleteDirectory(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path) = 0;

  /** Appends the list of requested directory entries to "dir_entries".
   *
   * There does not exist something like OpenDir and CloseDir. Instead one can
   * limit the number of requested entries (count) and specify the offset.
   *
   * DirectoryEntries will contain the names of the entries and, if not disabled
   * by "names_only", a Stat object for every entry.
   *
   * @remark Even if names_only is set to false, an entry does _not_ need to
   *         contain a stat buffer. Always check with entries(i).has_stbuf()
   *         if the i'th entry does have a stat buffer before accessing it.
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the directory.
   * @param offset  Index of first requested entry.
   * @param count   Number of requested entries.
   * @param names_only If set to true, the Stat object of every entry will be
   *                   omitted.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark    Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::DirectoryEntries* ReadDir(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      uint64_t offset,
      uint32_t count,
      bool names_only) = 0;

  /** Returns the list of extended attributes stored for "path" (Entries may
   *  be cached).
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark    Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::listxattrResponse* ListXAttrs(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path) = 0;

  /** Returns the list of extended attributes stored for "path" (Set "use_cache"
   *  to false to make sure no cached entries are returned).
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path        Path to the file/directory.
   * @param use_cache   Set to false to fetch the attributes from the MRC.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @remark    Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::listxattrResponse* ListXAttrs(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      bool use_cache) = 0;

  /** Sets the extended attribute "name" of "path" to "value".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param name    Name of the extended attribute.
   * @param value   Value of the extended attribute.
   * @param flags   May be 1 (= XATTR_CREATE) or 2 (= XATTR_REPLACE).
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void SetXAttr(
        const xtreemfs::pbrpc::UserCredentials& user_credentials,
        const std::string& path,
        const std::string& name,
        const std::string& value,
        xtreemfs::pbrpc::XATTR_FLAGS flags) = 0;

  /** Writes value for an XAttribute with "name" stored for "path" in "value".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param name    Name of the extended attribute.
   * @param value[out]  Will contain the content of the extended attribute.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @return    true if the attribute was found.
   */
  virtual bool GetXAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name,
      std::string* value) = 0;

  /** Writes the size of a value (string size without null-termination) of an
   *  XAttribute "name" stored for "path" in "size".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param name    Name of the extended attribute.
   * @param size[out]   Will contain the size of the extended attribute.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   *
   * @return    true if the attribute was found.
   */
  virtual bool GetXAttrSize(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name,
      int* size) = 0;

  /** Removes the extended attribute "name", stored for "path".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file/directory.
   * @param name    Name of the extended attribute.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void RemoveXAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name) = 0;

  /** Adds a new replica for the file at "path" and triggers the replication of
   *  this replica if it's a full replica.
   *
   * @param user_credentials    Username and groups of the user.
   * @param path            Path to the file.
   * @param new_replica     Description of the new replica to be added.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * */
  virtual void AddReplica(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::Replica& new_replica) = 0;

  /** Return the list of replicas of the file at "path".
   *
   * @param user_credentials    Username and groups of the user.
   * @param path                Path to the file.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   *
   * @remark Ownership is transferred to the caller.
   */
  virtual xtreemfs::pbrpc::Replicas* ListReplicas(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path) = 0;

  /** Removes the replica of file at "path" located on the OSD with the UUID
   *  "osd_uuid" (which has to be the head OSD in case of striping).
   *
   * @param user_credentials    Username and groups of the user.
   * @param path                Path to the file.
   * @param osd_uuid            UUID of the OSD from which the replica will be
   *                            deleted.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void RemoveReplica(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& osd_uuid) = 0;

  /** Adds all available OSDs where the file (described by "path") can be
   *  placed to "list_of_osd_uuids"
   *
   * @param user_credentials    Username and groups of the user.
   * @param path                Path to the file.
   * @param number_of_osds      Number of OSDs required in a valid group. This
   *                            is only relevant for grouping and will be
   *                            ignored by filtering and sorting policies.
   * @param list_of_osd_uuids[out]  List of strings to which the UUIDs will be
   *                                appended.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   */
  virtual void GetSuitableOSDs(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      int number_of_osds,
      std::list<std::string>* list_of_osd_uuids) = 0;

  /** Sets the replica update policy of "path" to "policy".
   *
   * @param user_credentials    Name and Groups of the user.
   * @param path    Path to the file.
   * @param policy  Policy to set for the file
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  virtual void SetReplicaUpdatePolicy(
        const xtreemfs::pbrpc::UserCredentials& user_credentials,
        const std::string& path,
        const std::string& policy) = 0;

};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_VOLUME_H_
