/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;

/**
 * 
 * <br>
 * Sep 2, 2011
 */
public abstract class Volume {
    public abstract void internalShutdown();

    /**
     * Start this volume, e.g. initialize all required things.
     */
    public abstract void start() throws Exception;

    /**
     * Close the volume.
     */
    public abstract void close();

    /**
     * Returns information about the volume (e.g. used/free space)
     * 
     * @param userCredentials
     *            Name and groups of the User.
     * @return {@link StatVFS}
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * @throws UnknownAddressSchemeException
     */
    // TODO: Create other Exceptions.
    public abstract StatVFS statFS(UserCredentials userCredentials) throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    // TODO: Create other Exceptions.
    public abstract String readLink(UserCredentials userCredentials, String path) throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    // TODO: Create other Exceptions.
    public abstract void symlink(UserCredentials userCredentials, String targetPath, String linkPath)
            throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void link(UserCredentials userCredentials, String targetPath, String linkPath)
            throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    // TODO: Create other Exceptions.
    public abstract void access(UserCredentials userCredentials, String path, int flags) throws IOException,
            PosixErrorException;

    /**
     * Opens a file and returns the pointer to a {@link FileHandle} object.
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
     * @throws UnknownAddressSchemeException
     * 
     * @remark Ownership is NOT transferred to the caller. Instead FileHandle.close() has to be called to
     *         destroy the object.
     */
    // TODO: Create other Exceptions.
    public abstract FileHandle openFile(UserCredentials userCredentials, String path, int flags)
            throws IOException, PosixErrorException;

    /**
     * Same as previous openFile() except for the additional mode parameter, which sets the permissions for
     * the file in case SYSTEM_V_FCNTL_H_O_CREAT is specified as flag and the file will be created.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws UnknownAddressSchemeException
     */
    public abstract FileHandle openFile(UserCredentials userCredentials, String path, int flags, int mode)
            throws IOException, PosixErrorException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void truncate(UserCredentials userCredentials, String path, int newFileSize)
            throws IOException, PosixErrorException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract Stat getAttr(UserCredentials userCredentials, String path) throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void setAttr(UserCredentials userCredentials, String path, Stat stat, int toSet)
            throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void unlink(UserCredentials userCredentials, String path) throws IOException;

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
     * @throws UnknownAddressSchemeException
     * */
    public abstract void rename(UserCredentials userCredentials, String path, String newPath)
            throws IOException;

    /**
     * Creates a directory with the modes "mode".
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
     * @throws UnknownAddressSchemeException
     */
    public abstract void createDirectory(UserCredentials userCredentials, String path, int mode)
            throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void removeDirectory(UserCredentials userCredentials, String path) throws IOException;

    /**
     * Appends the list of requested directory entries to "dirEntries".
     * 
     * There does not exist something like openDir and closeDir. Instead one can limit the number of requested
     * entries (count) and specify the offset.
     * 
     * {@link DirectoryEntries} will contain the names of the entries and, if not disabled by "namesOnly", a
     * {@link Stat} object for every entry.
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
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws UnknownAddressSchemeException
     * 
     * @remark Ownership is transferred to the caller.
     */
    public abstract DirectoryEntries readDir(UserCredentials userCredentials, String path, int offset,
            int count, boolean namesOnly) throws IOException;

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
     * @throws UnknownAddressSchemeException
     * 
     * @return {@link listxattrResponse}
     * 
     * @remark Ownership is transferred to the caller.
     */
    public abstract listxattrResponse listXAttrs(UserCredentials userCredentials, String path)
            throws IOException;

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
     * @throws UnknownAddressSchemeException
     * 
     * @remark Ownership is transferred to the caller.
     */
    public abstract listxattrResponse listXAttrs(UserCredentials userCredentials, String path,
            boolean useCache) throws IOException;

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
     *            May be 1 (= XATTR_CREATE) or 2 (= XATTR_REPLACE).
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws UnknownAddressSchemeException
     */
    public abstract void setXAttr(UserCredentials userCredentials, String path, String name, String value,
            int flags) throws IOException;

    /**
     * Writes value for an XAttribute with "name" stored for "path" in "value".
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
     * @throws UnknownAddressSchemeException
     * 
     */
    public abstract String getXAttr(UserCredentials userCredentials, String path, String name)
            throws IOException;

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
     * @param int Size of the extended attribute. -1 if the attribute was not found.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws UnknownAddressSchemeException
     * 
     * @return true if the attribute was found.
     */
    public abstract int getXAttrSize(UserCredentials userCredentials, String path, String name)
            throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void removeXAttr(UserCredentials userCredentials, String path, String name)
            throws IOException;

    /**
     * Adds a new replica for the file at "path" and triggers the replication of this replica if it's a full
     * replica.
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
    public abstract void addReplica(UserCredentials userCredentials, String path, Replica newReplica)
            throws IOException, PosixErrorException;

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
    public abstract Replicas listReplicas(UserCredentials userCredentials, String path) throws IOException;

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
     * @throws UnknownAddressSchemeException
     */
    public abstract void removeReplica(UserCredentials userCredentials, String path, String osdUuid)
            throws IOException;

    /**
     * Adds to "listOfOsdUuids" up to "numberOfOsds" UUIDs of all available OSDs where the file (described by
     * "path") can be placed.
     * 
     * @param userCredentials
     *            Username and groups of the user.
     * @param path
     *            Path to the file.
     * @param numberOfOsds
     *            Maximum number of OSDs which will be returned.
     * @param listOfOsdUuids
     *            [out] List of strings to which the UUIDs will be appended.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     */
    public abstract List<String> getSuitableOSDs(UserCredentials userCredentials, String path,
            int numberOfOsds) throws IOException;
}
