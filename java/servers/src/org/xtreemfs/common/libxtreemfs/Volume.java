/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;

/**
 * Represents a volume. A volume object can be obtain by opening a volume with a client.
 */
public interface Volume {

    public void internalShutdown();

    /**
     * Start this volume, e.g. initialize all required things.
     */
    void start() throws Exception;
    
    
    /**
     * Same as start(), but add option to start threads as daemons. Daemon threads are only used by the XtreemFSHadoopClient.
     * 
     * @param startThreadsAsDaemons if true, all threads are daemons.
     */
    void start(boolean startThreadsAsDaemons) throws Exception;

    /**
     * Close the volume.
     */
    public void close();

    /**
     * Returns information about the volume (e.g. used/free space).
     * 
     * @param userCredentials
     *            Name and groups of the User.
     * @return {@link StatVFS}
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public StatVFS statFS(UserCredentials userCredentials) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * 
     * Resolves the symbolic link at "path" and returns it in "linkTargetPath".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the symbolic link.
     * @return String where to store the result.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public String readLink(UserCredentials userCredentials, String path) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * 
     * Creates a symbolic link pointing to "targetPath" at "linkPath".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param targetPath
     *            Path to the target.
     * @param linkPath
     *            Path to the symbolic link.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void symlink(UserCredentials userCredentials, String targetPath, String linkPath)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Creates a hard link pointing to "targetPath" at "linkPath".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param targetPath
     *            Path to the target.
     * @param linkPath
     *            Path to the hard link.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void link(UserCredentials userCredentials, String targetPath, String linkPath)
            throws IOException, PosixErrorException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Tests if the subject described by "userCredentials" is allowed to access "path" as specified by
     * "flags". "flags" is a bit mask which may contain the values ACCESS_FLAGS_{F_OK,R_OK,W_OK,X_OK}.
     * 
     * Throws a PosixErrorException if not allowed.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param flags
     *            Open flags as specified in xtreemfs::pbrpc::SYSTEM_V_FCNTL.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void access(UserCredentials userCredentials, String path, int flags) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Opens a file and returns the pointer to a {@link FileHandle} object.
     * 
     * When creating files, use the function {@link #openFile(UserCredentials, String, int, int)} with the
     * additional mode parameter instead.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file.
     * @param flags
     *            Open flags as specified in xtreemfs::pbrpc::SYSTEM_V_FCNTL.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * 
     * @remark Ownership is NOT transferred to the caller. Instead FileHandle.close() has to be called to
     *         destroy the object.
     */
    public FileHandle openFile(UserCredentials userCredentials, String path, int flags)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Same as previous openFile() except for the additional mode parameter, which sets the permissions for
     * the file in case SYSTEM_V_FCNTL_H_O_CREAT is specified as flag and the file will be created.
     * 
     * Please note that the mode parameter requires octal values, i.e. use 0777 instead of 777 for the
     * permissions.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public FileHandle openFile(UserCredentials userCredentials, String path, int flags, int mode)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Truncates the file to "newFileSize" bytes.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file.
     * @param newFileSize
     *            New size of file.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void truncate(UserCredentials userCredentials, String path, int newFileSize)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Retrieve the attributes of a file and writes the result in "stat".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @return stat Result of the operation will be stored here.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public Stat getAttr(UserCredentials userCredentials, String path) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Sets the attributes given by "stat" and specified in "toSet".
     * 
     * @note If the mode, uid or gid is changed, the ctime of the file will be updated according to POSIX
     *       semantics.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param stat
     *            Stat object with attributes which will be set.
     * @param toSet
     *            Bitmask which defines which attributes to set.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void setAttr(UserCredentials userCredentials, String path, Stat stat, int toSet)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Remove the file at "path" (deletes the entry at the MRC and all objects on one OSD).
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void unlink(UserCredentials userCredentials, String path) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Rename a file or directory "path" to "newPath".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Old path.
     * @param newPath
     *            New path.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * */
    public void rename(UserCredentials userCredentials, String path, String newPath)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Creates a directory with the modes "mode". Creates missing parent directories if and only if recursive
     * is set to true. Results in an error otherwise.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the new directory.
     * @param mode
     *            Permissions of the new directory.
     * @param recursive
     *            Whether or not non existing parent directories should be created.
     * 
     * @throws IOException
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public void createDirectory(UserCredentials userCredentials, String path, int mode,
            boolean recursive) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Creates a directory with the modes "mode". Results in an error when parent directory doesn't exist.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the new directory.
     * @param mode
     *            Permissions of the new directory.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void createDirectory(UserCredentials userCredentials, String path, int mode)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Removes the directory at "path" which has to be empty.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the directory to be removed.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void removeDirectory(UserCredentials userCredentials, String path) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Returns a list of "count" directories/files contained in the directory "path" beginning by "offset". If
     * count equals 0 all entries beginning by "offset" will be in the list.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the directory.
     * @param offset
     *            Index of first requested entry.
     * @param count
     *            Number of requested entries.
     * @param namesOnly
     *            If set to true, the {@link Stat} object of every entry will be omitted.
     * 
     * @return {@link DirectoryEntries} will contain the names of the entries and, if not disabled by
     *         "namesOnly", a {@link Stat} object for every entry.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public DirectoryEntries readDir(UserCredentials userCredentials, String path, int offset,
            int count, boolean namesOnly) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * Returns the list of extended attributes stored for "path" (Entries may be cached).
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * 
     * @return {@link listxattrResponse}
     * 
     * @remark Ownership is transferred to the caller.
     */
    public listxattrResponse listXAttrs(UserCredentials userCredentials, String path)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Returns the list of extended attributes stored for "path" (Set "useCache" to false to make sure no
     * cached entries are returned).
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param useCache
     *            Set to false to fetch the attributes from the MRC.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * 
     * @remark Ownership is transferred to the caller.
     */
    public listxattrResponse listXAttrs(UserCredentials userCredentials, String path,
            boolean useCache) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Sets the extended attribute "name" of "path" to "value".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param name
     *            Name of the extended attribute.
     * @param value
     *            Value of the extended attribute.
     * @param flags
     *            May be XATTR_CREATE or XATTR_REPLACE.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void setXAttr(UserCredentials userCredentials, String path, String name, String value,
            XATTR_FLAGS flags) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    
    /**
     * Sets the extended attribute "name" of "path" to "value".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param path
     *            Path to the file/directory.
     * @param name
     *            Name of the extended attribute.
     * @param value
     *            Value of the extended attribute.
     * @param flags
     *            May be XATTR_CREATE or XATTR_REPLACE.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void setXAttr(UserCredentials userCredentials, Auth auth, String path, String name, String value,
            XATTR_FLAGS flags) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    
    /**
     * Returns value for an XAttribute with "name" stored for "path" in "value".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param name
     *            Name of the extended attribute.
     * @return String Will contain the content of the extended attribute. NULL if attribute was not found.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * 
     */
    public String getXAttr(UserCredentials userCredentials, String path, String name)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Writes the size of a value (string size without null-termination) of an XAttribute "name" stored for
     * "path" in "size".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param name
     *            Name of the extended attribute.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * 
     * @return true if the attribute was found.
     */
    public int getXAttrSize(UserCredentials userCredentials, String path, String name)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Removes the extended attribute "name", stored for "path".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file/directory.
     * @param name
     *            Name of the extended attribute.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void removeXAttr(UserCredentials userCredentials, String path, String name)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Adds a new replica for the file at "path" and triggers the replication of this replica if it's a full
     * replica.
     * 
     * Please note, in case of a read-only replica the replication flags of newReplica must contain a
     * replication strategy flag.
     * For a partial replica, use {@link REPL_FLAG.REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING}.
     * If you create a full replica (with the flag {@link REPL_FLAG.REPL_FLAG_FULL_REPLICA}),
     * use the strategy {@link REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST}.
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param path
     *            Path to the file.
     * @param newReplica
     *            Description of the new replica to be added.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * */
    public void addReplica(UserCredentials userCredentials, String path, Replica newReplica)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Return the list of replicas of the file at "path".
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param path
     *            Path to the file.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * 
     * @remark Ownership is transferred to the caller.
     */
    public Replicas listReplicas(UserCredentials userCredentials, String path) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Removes the replica of file at "path" located on the OSD with the UUID "osdUuid" (which has to be the
     * head OSD in case of striping).
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param path
     *            Path to the file.
     * @param osdUuid
     *            UUID of the OSD from which the replica will be deleted.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void removeReplica(UserCredentials userCredentials, String path, String osdUuid)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Returns a list of all available OSDs where the file (described by "path") can be placed.
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param path
     *            Path to the file.
     * @param numberOfOsds
     *            Number of OSDs required in a valid group. This is only relevant for grouping and will be ignored by
     *            filtering and sorting policies.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public List<String> getSuitableOSDs(UserCredentials userCredentials, String path,
            int numberOfOsds) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Sets the default replication policy for "directory".
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param directory
     *            Path of the directory.
     * @param replicationPolicy
     *            Replication policy which is defined in {@link ReplicaUpdatePolicies}
     * @param replicationFactor
     *            Number of replicas that should be assigned to new files.
     * @param replicationFlags
     *            Replication flags as number. Defined in {@link REPL_FLAG}.
     *            Use the helper functions available in {@link ReplicationFlags}.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public void setDefaultReplicationPolicy(UserCredentials userCredentials, String directory,
            String replicationPolicy, int replicationFactor, int replicationFlags) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Gets the default replication policy for "directory".
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param directory
     *            Path of the directory.
     * @return {@link ReplicationPolicy}
     * @throws IOException
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public ReplicationPolicy getDefaultReplicationPolicy(UserCredentials userCredentials, String directory)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Sets the replica update policy of "path" to "policy".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file.
     * @param policy
     *            Policy to set for the file
     * @throws IOException
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public void setReplicaUpdatePolicy(UserCredentials userCredentials, String path, String policy)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Returns a list of {@link StripeLocation} where each stripe of the file is located. To determine where the a
     * particular stripe is located the UUIDs of all replicas which have a copy of this stripe will be collected and
     * resolved to hostnames. If a uuid can't be resolved it will be deleted from the list because HDFS can't handle IP
     * addresses.
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param path
     *            Path of the file.
     * @param startSize
     *            Size in byte where to start collecting the {@link StripeLocation}s.
     * @param length
     *            The length of the part of the file where the {@link StripeLocation}s should be collected in
     *            byte.
     * @return {@link List} of {@link StripeLocation}
     * 
     * @throws IOException
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */

    public List<StripeLocation> getStripeLocations(UserCredentials userCredentials, String path,
            long startSize, long length) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    
    /**
     * Removes the user from the ACL stored in path 
     * 
     * @param userCreds 
     *          Username and groups of the user. 
     * @param path 
     *          The path on the volume where the ACL is stored
     * @param user 
     *          The user to remove access rights
     * @throws IOException
     */
    public void removeACL(UserCredentials userCreds, String path, String user) throws IOException;
    
    /**
     * Removes all provided users from the ACL stored in path 
     * 
     * @param userCreds 
     *          Username and groups of the user. 
     * @param path 
     *          The path on the volume where the ACL is stored
     * @param aclEntries
     *          Set of acl entries to remove
     * @throws IOException
     */    
    public void removeACL(UserCredentials userCreds, String path, Set<String> aclEntries) throws IOException;
    
    /**
     * Adds the user to the ACL for the provided path 
     * 
     * @param userCreds 
     *          Username and groups of the user. 
     * @param path 
     *          The path on the volume where the ACL is stored
     * @param user 
     *          The user to remove access rights
     * @param accessrights 
     *          The accessrights to be set for the user. I.e. rwx, rx, rw, ...
     * @throws IOException
     */ 
    public void setACL(UserCredentials userCreds, String path, String user, String accessrights)
            throws IOException;
    
    /**
     * Adds all users to the ACL for the provided path 
     * 
     * @param userCreds 
     *          Username and groups of the user. 
     * @param path 
     *          The path on the volume where the ACL is stored
     * @param aclEntries
     *          ACL entries to set
     * @throws IOException
     */ 
    public void setACL(UserCredentials userCreds, String path, Map<String, Object> aclEntries)
            throws IOException;
    
    /**
     * Returns all users in the ACL for the provided path 
     * 
     * @param userCreds 
     *          Username and groups of the user. 
     * @param path 
     *          The path on the volume where the ACL is stored
     * @throws IOException
     */ 
    public Map<String, Object> listACL(UserCredentials userCreds, String path) throws IOException;
    
    /** Get the OSD selection policies of the volume (replica placement).
     * 
     * @param userCreds Username and groups of the user.
     * 
     * @return List of policies as comma separated string.
     * @throws IOException
     */
    public String getOSDSelectionPolicy(UserCredentials userCreds) throws IOException;
    
    /** Set the OSD selection policies for the volume (replica placement).
     * 
     * @param userCreds Username and groups of the user.
     * @param policies List of policies as comma separated string.
     * @throws IOException
     */
    public void setOSDSelectionPolicy(UserCredentials userCreds, String policies) throws IOException;
    
    /** Get the Replica selection policies of the volume (replica selection).
     * 
     * 
     * @param userCreds Username and groups of the user.
     * 
     * @return List of policies as comma separated string.
     * @throws IOException
     */
    public String getReplicaSelectionPolicy(UserCredentials userCreds) throws IOException;
    
    /** Set the Replica selection policies for the volume (replica selection).
     * 
     * @param userCreds Username and groups of the user.
     * @param policies List of policies as comma separated string.
     * @throws IOException
     */
    public void setReplicaSelectionPolicy(UserCredentials userCreds, String policies) throws IOException;
    
    
    /** Set attribute of a policy to further customize replica placement and
     *  selection. See the user guide for more information.
     * 
     * @param userCreds Username and groups of the user.
     * @param attribute Format: <policy id>.<attribute name> e.g., "1001.domains"
     * @param value Value of the attribute.
     * @throws IOException
     */
    public void setPolicyAttribute(UserCredentials userCreds, String attribute, String value) throws IOException;

    /**
     * Get the name of the volume.
     * @return The name of the volume.
     */
    public String getVolumeName();

    /**
     * Used only for Hadoop Interface.
     * 
     * Encapsulates information about one stripe, i.e. the size in kb where the stripe begins, the length of
     * the stripe and lists of hostnames and corresponding uuids where the stripe is located. Hostnames are
     * usually the ones which are configured through the "hostname = " option of the OSD. Otherwise it is the
     * resolved hostname of registred IP address at the DIR.
     * 
     */
    public class StripeLocation {
        private final long     startSize;
        private final long     length;
        private final String[] uuids;

        /**
         * The hostname as configured with "hostname = " parameter of the OSD or otherwise the resolved
         * hostname from the IP address registered at DIR.
         */
        private final String[] hostnames;

        protected StripeLocation(long startSize, long length, String[] uuids, String[] hostnames) {
            this.startSize = startSize;
            this.length = length;
            this.uuids = uuids;
            this.hostnames = hostnames;
        }

        public long getStartSize() {
            return startSize;
        }

        public long getLength() {
            return length;
        }

        public String[] getUuids() {
            return uuids;
        }

        public String[] getHostnames() {
            return hostnames;
        }
    }
}
