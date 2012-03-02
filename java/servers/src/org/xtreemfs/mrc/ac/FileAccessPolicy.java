/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.ac;

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.PathResolver;

/**
 * An interface for a policy defining file access.
 * 
 * @author stender
 */
public interface FileAccessPolicy {
    
    /**
     * Returns a string representing the policy-specific translation of the
     * given POSIX access mode. This method can be used to obtain a valid access
     * mode string to pass with <code>checkAccess</code>.
     * 
     * @param accessMode
     *            the POSIX access mode, see {@link FileAccessManager} constants
     * @return a policy-specific string describing the access mode
     */
    public String translateAccessFlags(int accessMode);
    
    /**
     * Returns a string representing the policy-specific translation of the
     * given permissions into the 'rwx' scheme.
     * 
     * @param permissions
     *            an integer representing the permissions for a specific user or
     *            group
     * @return an 'rwx' string describing the permissions
     */
    public String translatePermissions(int permissions);
    
    /**
     * Checks whether the user with the given ID is allowed to perform
     * operations for the given file with the given access mode.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file
     * @param parentId
     *            the file's parent ID - note that 0 is provided unless the
     *            check refers to an entity being added, deleted or moved in a
     *            directory
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @param accessMode
     *            the access mode. How the access mode has to be interpreted
     *            depends on the policy implementation.
     * @throws UserException
     *             if the permission was denied
     * @throws MRCException
     *             if an error occurs while trying to get permissions
     */
    public void checkPermission(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, String accessMode) throws UserException, MRCException;
    
    /**
     * Checks whether search permission is granted on the given path. The method
     * should return without throwing an exception if
     * <code>checkPermission()</code> for an access mode implying the right to
     * switch to the directory returns <code>true</code>.
     * 
     * POSIX-compliant implementations might have to check permissions
     * recursively for each path component. Since there might not be an explicit
     * access mode for searching directories, the framework will invoke this
     * method instead of using <code>checkPermission()</code> when checking
     * search access on directories.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param res
     *            the path resolver
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @throws UserException
     *             if the permission was denied
     * @throws MRCException
     *             if an error occurs at the backend
     */
    public void checkSearchPermission(StorageManager sMan, PathResolver res, String userId,
        List<String> groupIds) throws UserException, MRCException;
    
    /**
     * Checks whether permission is granted to change the owner of the file with
     * the given ID.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file in the volume
     * @param userId
     *            the ID of the user on behalf of whom the access check is
     *            performed
     * @param groupIds
     *            a list of group IDs
     * @throws UserException
     *             if the permissoin was denied
     * @throws MRCException
     *             if an error occurs at the backend
     */
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws UserException, MRCException;
    
    /**
     * Returns a POSIX access mode bit mask for the given file and user in the
     * form of a long. As specified in POSIX, the first three bits represent
     * read, write and execute access for the user, the next three bits do the
     * same for the group, and the last three bits for the rest of the world.
     * Any other bits may be used in a policy-specific manner.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @return the POSIX access rights
     * @throws MRCException
     *             if an error occurs when trying to tranlate access rights
     */
    public int getPosixAccessRights(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws MRCException;
    
    /**
     * Modifies the file ACL by means of a POSIX access mode bit mask.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file
     * @param parentId
     *            the file's parent ID
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @param posixAccessRights
     *            a long value describing the POSIX access rights
     * @param update
     *            the database update
     * @throws MRCException
     *             if an error occurs when trying to tranlate access rights
     * @throws UserException
     *             if access is denied
     */
    public void setPosixAccessRights(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, int posixAccessRights, boolean superUser, AtomicDBUpdate update) throws MRCException,
        UserException;
    
    /**
     * Creates or changes a set of entries the current ACL of a file.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file
     * @param parentId
     *            the file's parent ID
     * @param entries
     *            a mapping from entity names (ac entities) to long values
     *            (rights masks) representing the ACL entries to add/modify
     * @param update
     *            the database update
     * @throws MRCException
     *             if an error occurs when trying to change the ACL
     * @throws UserException
     *             if access is denied
     */
    public void updateACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        Map<String, Object> entries, AtomicDBUpdate update) throws MRCException, UserException;
    
    /**
     * Returns the ACL of a file.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file
     * @return the ACL of the given file
     */
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file) throws MRCException;
    
    /**
     * Creates or changes an entry in the current ACL of a file.
     * 
     * @param sMan
     *            the volume's Storage Manager
     * @param file
     *            the file
     * @param parentId
     *            the file's parent ID
     * @param entities
     *            a list of access control entity names to delete from the ACL
     * @param update
     *            the database update
     * @throws MRCException
     *             if an error occurs when trying to change the ACL
     * @throws UserException
     *             if access is denied
     */
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        List<Object> entities, AtomicDBUpdate update) throws MRCException, UserException;
    
    /**
     * Returns the default ACL for the root directory. The method is invoked in
     * order to assign an initial ACL to a the root directory of a newly-created
     * volume.
     * 
     * @return the default root ACL
     */
    public ACLEntry[] getDefaultRootACL();
}