/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.ac;

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.brain.BrainException;
import org.xtreemfs.mrc.brain.UserException;

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
    public String translateAccessMode(int accessMode);
    
    /**
     * Checks whether the user with the given ID is allowed to perform
     * operations for the given file with the given access mode.
     * 
     * @param volumeId
     *            the volume ID of the file
     * @param fileId
     *            the file ID
     * @param parentId
     *            the ID of the file's parent - note that '0' is provided unless
     *            the check refers to an entity being added, deleted or moved in
     *            a directory
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @param accessMode
     *            the access mode. How the access mode has to be interpreted
     *            depends on the policy implementation.
     * @throws UserException
     *             if the permission was denied
     * @throws BrainException
     *             if an error occurs while trying to get permissions
     */
    public void checkPermission(String volumeId, long fileId, long parentId, String userId,
        List<String> groupIds, String accessMode) throws UserException, BrainException;
    
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
     * @param volumeId
     *            the volume ID of the directory
     * @param path
     *            the full path to the file or directory
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @throws UserException
     *             if the permission was denied
     * @throws BrainException
     *             if an error occurs at the backend
     */
    public void checkSearchPermission(String volumeId, String path, String userId,
        List<String> groupIds) throws UserException, BrainException;
    
    /**
     * Checks whether permission is granted to change the owner of the file with
     * the given ID.
     * 
     * @param volumeId
     *            the volume ID of the file
     * @param fileId
     *            the file ID in the volume
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @throws UserException
     *             if the permissoin was denied
     * @throws BrainException
     *             if an error occurs at the backend
     */
    public void checkPrivilegedPermissions(String volumeId, long fileId, String userId,
        List<String> groupIds) throws UserException, BrainException;
    
    /**
     * Returns an ACL in the form of a map that is used as the volume ACL in
     * case no volume ACL is explicitly specified when a new volume is created.
     * 
     * @param volumeId
     *            the volume ID
     * @return a mapping from String to Long representing the default volume ACL
     * @throws BrainException
     *             if an error occurs while determining the default volume ACLs
     */
    public Map<String, Object> createDefaultVolumeACL(String volumeId) throws BrainException;
    
    /**
     * Returns the access control list that is automatically assigned to a newly
     * created child.
     * 
     * The framework will invoke this method when a new file or directory is
     * created.
     * 
     * @param mode
     *            the access mode from which the initial ACL is calculated
     * @throws BrainException
     *             if an error occurs at the backend
     */
    public Map<String, Object> convertToACL(long mode) throws BrainException;
    
    /**
     * Returns a POSIX access mode bit mask for the given file and user in the
     * form of a long. As specified in POSIX, the first three bits represent
     * read, write and execute access for the user, the next three bits do the
     * same for the group, and the last three bits for the rest of the world.
     * Any other bits may be used in a policy-specific manner.
     * 
     * @param volumeId
     *            the volume ID of the file
     * @param fileId
     *            the file ID in the volume
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @return the POSIX access rights
     * @throws BrainException
     *             if an error occurs when trying to tranlate access rights
     */
    public long getPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds) throws BrainException;
    
    /**
     * Modifies the file ACL by means of a POSIX access mode bit mask.
     * 
     * @param volumeId
     *            the volume ID
     * @param fileId
     *            the file ID
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @param posixRights
     *            a long value describing the POSIX access rights
     * @throws BrainException
     *             if an error occurs when trying to tranlate access rights
     * @throws UserException
     *             if access is denied
     */
    public void setPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds, long posixRights) throws BrainException, UserException;
    
    /**
     * Creates or changes a set of entries the current ACL of a file.
     * 
     * @param volumeId
     *            the volume ID
     * @param fileId
     *            the file ID
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @param entries
     *            a mapping from entity names (ac entities) to long values
     *            (rights masks) representing the ACL entries to add/modify
     * @throws BrainException
     *             if an error occurs when trying to change the ACL
     * @throws UserException
     *             if access is denied
     */
    public void setACLEntries(String volumeId, long fileId, String userId, List<String> groupIds,
        Map<String, Object> entries) throws BrainException, UserException;
    
    /**
     * Creates or changes an entry in the current ACL of a file.
     * 
     * @param volumeId
     *            the volume ID
     * @param fileId
     *            the file ID
     * @param userId
     *            the user ID
     * @param groupIds
     *            a list of group IDs
     * @param entities
     *            a list of access control entity names to delete from the ACL
     * @throws BrainException
     *             if an error occurs when trying to change the ACL
     * @throws UserException
     *             if access is denied
     */
    public void removeACLEntries(String volumeId, long fileId, String userId,
        List<String> groupIds, List<Object> entities) throws BrainException, UserException;
    
}