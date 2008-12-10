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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.common.clients.mrc;

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.brain.BrainException;
import org.xtreemfs.mrc.brain.UserException;

/**
 * The interface to the MRC backend.
 * 
 * @author stender, bjko
 * 
 */
public interface MRCClientInterface {

    /**
     * Sets up a new file system. The following steps are taken:
     * <ol>
     * <li>all local volumes are deregistered from the directory service
     * <li>the database containing local metadata is stopped and all contents
     * are deleted
     * <li>an empty database for local metadata is started, representing an
     * empty file system
     * </ol>
     * 
     * @throws BrainException
     *             if the deregistration at the Directory Service of existing
     *             volumes failed or if an error occured in the storage backend
     */
    public void initFileSystem() throws BrainException;

    /**
     * Locally creates a new volume with the default OSD, striping and file
     * access policy without an ACL and registers the volume at the Directory
     * Service.<br/> This method is equivalent to
     * <code>createVolume(volumeName, null, userId, SimpleSelectionPolicy.POLICY_ID)</code>.
     * 
     * @param volumeName
     *            the name for the new volume
     * @param userId
     *            the user id
     * @throws UserException
     *             if the volume already exists
     * @throws BrainException
     *             if an error occured in the storage backend
     * @see #createVolume(String, Map, long, long, long, long)
     */
    public void createVolume(String volumeName, long userId)
            throws BrainException, UserException;

    /**
     * Locally creates a new volume and registers the volume at the Directory
     * Service.<br/> The ACL is provided as described in
     * <code>setVolumeACL(...)</code>.
     * 
     * 
     * @param volumeName
     *            the name for the new volume
     * @param volumeACL
     *            the ACL for the volume
     * @param userId
     *            the user id
     * @param osdPolicyId
     *            the id of the OSD policy to use with this volume
     * @param stripingPolicyId
     *            the id of the default striping policy used for files stored in
     *            the volume
     * @param fileAccessPolicyId
     *            the id of the access policy used for files in the volume
     * @throws UserException
     *             if the volume already exists
     * @throws BrainException
     *             if an error occured in the storage backend
     * @see #setVolumeACL(String, Map)
     */
    public void createVolume(MRCRequest request, String volumeName,
            long osdPolicyId, long stripingPolicyId, long fileAccessPolicyId,
            long uid, Map<String, Object> volumeACL) throws BrainException,
            UserException;

    /**
     * Sets an ACL for the volume with the given name.<br/> The ACL is provided
     * as an access control list of the form {user:long=rights:long, user2=...,
     * ...}.
     * 
     * <ul>
     * <li><code>rights</code>: the rights which the user has on the file.
     * <code>rights & 1</code> checks for read access and
     * <code>rights & 2</code> checks for write access.
     * <code>rights & 4</code> checks for execution access.
     * </ul>
     * 
     * @param volumeName
     *            the name of the volume
     * @param volumeACL
     *            the ACL
     * @throws UserException
     *             if the volume is invalid or the local MRC is not responsible
     *             for the volume
     * @throws BrainException
     *             if an error occured in the storage backend
     * 
     * @see #setVolumeACL(String, Map)
     */
    public void setVolumeACL(String volumeName, Map<String, Object> volumeACL)
            throws BrainException, UserException;

    /**
     * Returns the ACL of the volume with the given name.<br/> The ACL is
     * provided as described in {@link #setVolumeACL(String, Map)}.
     * 
     * @param volumeName
     *            the name of the volume
     * @return the ACL of the volume
     * @throws UserException
     *             if the volume is invalid or the local MRC is not responsible
     *             for the volume
     * @throws BrainException
     *             if an error occured in the storage backend
     * 
     * @see #setVolumeACL(String, Map)
     */
    public Map<String, Object> getVolumeACL(String volumeName)
            throws BrainException, UserException;

    /**
     * Deletes an existing volume held by the local MRC. All associated
     * directories and files are removed as well.
     * 
     * @param name
     *            the name of the volume to remove
     * @throws UserException
     *             if the volume is invalid or the local MRC is not responsible
     *             for the volume
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void deleteVolume(String name) throws BrainException, UserException;

    /**
     * Creates a new file without user attributes and striping policy. This
     * method is equivalent to <code>createFile(path, null, userId)</code>.
     * 
     * @param path
     *            the path to the file
     * @param userId
     *            the id of the user on behalf of whom the file is created
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     * @see #createFile(String, Map, long, long)
     */
    public void createFile(String path, long userId) throws BrainException,
            UserException;

    /**
     * Creates a new file.
     * 
     * @param path
     *            the path to the file
     * @param attrs
     *            a map containing the file attributes as (key/value) pairs
     * @param stripingPolicyId
     *            the id of the striping policy used with this file. If
     *            <code>0</code> is specified, the volume striping policy will
     *            be used.
     * @param userId
     *            the id of the user on behalf of whom the file is created
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void createFile(String path, Map<String, Object> attrs,
            long stripingPolicyId, long userId, Map<String, Object> acl)
            throws BrainException, UserException;

    /**
     * Adds a user attribute to an existing file. If the attribute already
     * exists for the given user, it will be overwritten.
     * 
     * @param path
     *            the path to the file
     * @param key
     *            the attribute key
     * @param value
     *            the attribute value
     * @param userId
     *            the user id associated with the attribute. If <code>0</code>
     *            is provided, the attribute will be regarded as global, i.e. it
     *            will be visible to any user.
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void addUserAttribute(String path, String key, String value,
            long userId) throws BrainException, UserException;

    /**
     * Adds multiple user attributes to an existing file. If the attribute
     * already exists for the given user, it will be overwritten.
     * 
     * @param path
     *            the path to the file
     * @param attrs
     *            a map containing the file attributes as (key/value) pairs
     * @param userId
     *            the user id associated with the attributes. If <code>0</code>
     *            is provided, the attributes will be regarded as global, i.e.
     *            they will be visible to any user.
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void addUserAttributes(String path, Map<String, Object> attrs,
            long userId) throws BrainException, UserException;

    /**
     * Assigns a new replica to an existing file. Each replica of a file
     * represents the entire file content. Since different replicas may be
     * striped over multiple OSDs in different ways, each replica is described
     * by a string containing striping information. The striping information
     * string will only be stored but not evaluated by the MRC.
     * 
     * @param globalFileId
     *            the global ID of the file in the form of "volumeId":"fileId"
     * @param stripingInfo
     *            an opaque string containing striping information about the
     *            replica
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend if an error
     *             occured in t
     */
    public void addReplica(String globalFileId, String stripingInfo)
            throws BrainException, UserException;

    /**
     * Removes a user attribute from an existing file.
     * 
     * @param path
     *            the path to the file
     * @param key
     *            the key of the attribute
     * @param userId
     *            the id of the user who defined the attribute, or
     *            <code>0</code> for a global attribute
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void removeUserAttribute(String path, String key, long userId)
            throws BrainException, UserException;

    /**
     * Removes multiple user attributes from an existing file.
     * 
     * @param path
     *            the path to the file
     * @param attrKeys
     *            a list containing all keys of the attribute
     * @param userId
     *            the id of the user who defined the attributes, or
     *            <code>0</code> for a global attribute
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void removeUserAttributes(String path, List<Object> attrKeys,
            long userId) throws BrainException, UserException;

    /**
     * Returns a map containing all user-defined attribute/value pairs of a
     * file. In case of a directory, <code>null</code> will be returned.
     * 
     * @param path
     *            the path to the file
     * @param userId
     *            the user id associated with the attributes
     * @return a map containing the attributes
     * 
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public Map<String, Object> getUserAttributes(String path, long userId)
            throws BrainException, UserException;

    /**
     * Deletes a file or directory including all user attributes. In case of a
     * directory, the directory is required to be empty, i.e. it must neither
     * contain files nor subdirectories.
     * 
     * @param path
     *            the path to the file
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void delete(String path) throws BrainException, UserException;

    /**
     * Creates a new directory without user attributes. The operation will fail
     * unless the first n-1 of n components in <code>path</code> refer to an
     * existing directory. This method is equivalent to
     * <code>createDir(path, null, userId)</code>.
     * 
     * @param path
     *            complete path including the volume name
     * @param userId
     *            the id of the user on behalf of whom the directory is created
     * @throws UserException
     *             if the parent path does not exist or the local MRC is not
     *             responsible for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void createDir(String path, long userId) throws BrainException,
            UserException;

    /**
     * Creates a new directory with user attributes. The operation will fail
     * unless the first n-1 of n components in <code>path</code> refer to an
     * existing directory.
     * 
     * @param path
     *            complete path including the volume name
     * @param attrs
     *            a map containing the directory attributes as (key/value) pairs
     * @param userId
     *            the id of the user on behalf of whom the directory is created
     * @throws UserException
     *             if the parent path does not exist or the local MRC is not
     *             responsible for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void createDir(String path, Map<String, Object> attrs, long userId)
            throws BrainException, UserException;

    /**
     * Lists the contents of a directory. Note that no guarantees are given
     * about the order in which elements are listed.
     * 
     * @param path
     *            the complete path including the volume
     * @return a list of strings of the subdirectorie and files in the directory
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public List<String> readDir(String path) throws BrainException,
            UserException;

    /**
     * Returns the result of a 'readdir' combined with a 'stat' for each
     * directory entry. It is returned in the form of a map which maps the entry
     * names to maps containing the stat infos as provided by the 'stat' method.
     * 
     * @param path
     *            the directory of which the contents are returned
     * @param userId
     *            the id of the user on behalf of whom the stat is returned.
     *            This is necessary in order to properly translate the POSIX
     *            access rights.
     * @return a list of stats for the directory contents
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public Map<String, Map<String, Object>> readDirAndStat(String path,
            long userId) throws BrainException, UserException;

    /**
     * Returns information about a single file or directory. The data returned
     * has the following shape:<br/><br/>
     * 
     * {volId=long, sliceId=long, fileId=long, type=int, userId=long, size=long,
     * atime=long, mtime=long, ctime=long, posixAccessMode=int}.
     * 
     * <ul>
     * <li><code>volId</code>: the id of the volume holding the file or
     * directory
     * <li><code>sliceId</code>: the id of the slice holding the file or
     * directory
     * <li><code>fileId</code>: the id of the file or directory
     * <li><code>type</code>: an integer between 0 and 2 describing the type
     * (0=directory, 1=file, 2=symlink)
     * <li><code>userId</code>: the user id of the file owner
     * <li><code>size</code>: the file size
     * <li><code>atime</code>: the access timestamp
     * <li><code>mtime</code>: the modification timestamp
     * <li><code>ctime</code>: the change timestamp
     * <li><code>posixAccessMode</code>: the posix access rights (rwx) for
     * the owner, the VO and the rest
     * </ul>
     * 
     * @param path
     *            the path of the file in the file system
     * @param userId
     *            the id of the user on behalf of whom the stat is returned.
     *            This is necessary in order to properly translate the POSIX
     *            access rights.
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     * @return the stats for the file
     */
    public Map<String, Object> stat(String path, long userId)
            throws BrainException, UserException;

    /**
     * Creates a symbolic link to the given target.<br/> The link itself
     * behaves like an independent file with its own metadata. When file
     * contents are read, however, the read request will be redirected to the
     * given target path. No guarantees are given that the target path is valid,
     * nor will the softlink be updated when the referenced file is moved or
     * renamed.
     * 
     * @param linkPath
     *            the path for the link itself
     * @param targetPath
     *            the path to the link's target
     * @param userId
     *            the id of the user on behalf of whom the file is created
     * @throws UserException
     *             if the link path is invalid or the local MRC is not
     *             responsible for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void createSymLink(String linkPath, String targetPath, long userId)
            throws BrainException, UserException;

    /**
     * Returns the path to which the symbolic link referenced by the given path
     * points to.
     * 
     * @param path
     *            the path to the symbolic link
     * @return the path which the symbolic link points to
     * @throws UserException
     *             if the path does not point to a symbolic link or the local
     *             MRC is not responsible for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public String getSymlinkTarget(String path) throws BrainException,
            UserException;

    /**
     * Opens an existing file.
     * 
     * <p>
     * If the capability is successfully issued, a map of the following form
     * will be returned:<br/><br/>
     * 
     * result = {storageLocs:StorageLocList, stripingPolicy:long,
     * capablity:string}.<br/> storageLocs = [feasibleHost_1:string, ... ,
     * feasibleHost_n:string]
     * 
     * <ul>
     * <li><code>storageLocs</code>: a list of strings 'hostname:port'
     * describing the locations of feasible OSDs
     * <li><code>stripingPolicy</code>: the id of the striping policy used
     * with the given path
     * <li><code>capability</code>: the string containing the encrypted
     * capability
     * </ul>
     * </p>
     * 
     * <br/> In case the capability could not be issued, <code>null</code> is
     * returned.
     * 
     * @param path
     *            the path for which to generate the capability
     * @param accessMode
     *            the access for the file/directory. Possible attributes are
     *            "rwx".
     * @param userId
     *            the id of the user on behalf of whom the capability is issued
     * @return a map of the form described above
     * 
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public Map<String, Object> open(String path, String accessMode, long userId)
            throws BrainException, UserException;

    /**
     * Checks whether the given path refers to a directory.
     * 
     * @param path
     *            the path
     * @return <code>true</code> if the path refers to a directory,
     *         <code>false</code> otherwise
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public boolean isDir(String path) throws BrainException, UserException;

    /**
     * Checks whether the given path refers to a symbolic link.
     * 
     * @param path
     *            the path
     * @return <code>true</code> if the path refers to a symbolic link,
     *         <code>false</code> otherwise
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public boolean isSymLink(String path) throws BrainException, UserException;

    /**
     * Moves the file or directory referenced by the source path to the given
     * target path.
     * <p>
     * The behavior of this method depends on what both paths are pointing to.
     * The source path must point to a valid file or directory which is managed
     * by the local MRC.
     * 
     * The behavior is a follows:
     * <ul>
     * <li> source points to a file:
     * <li>
     * <ul>
     * <li> target is a file or does not exist: the source file will be moved to
     * the target's parent directory where the old file (if exists) is removed
     * <li> target is a directory: the source file will be moved to the target
     * directory
     * </ul>
     * <li> source points to a directory:
     * <li>
     * <ul>
     * <li> target is a file: an exception is thrown
     * <li> target is a directory: the source directory tree will be moved to
     * the target directory
     * <li> target does not exist: the source directory will be moved to the
     * target's parent directory and renamed
     * </ul>
     * </ul>
     * </p>
     * 
     * @param sourcePath
     *            the path pointing to the source file or directory
     * @param targetPath
     *            the path pointing to the target file or directory
     * @throws UserException
     *             if the source or target path is invalid or the local MRC is
     *             not responsible for the source path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void move(String sourcePath, String targetPath)
            throws BrainException, UserException;

    /**
     * Submits a query. A list of files matching the given query string is
     * returned in the form of path names.
     * 
     * @param path
     *            the path from which the query is executed. Query results will
     *            be restricted to paths that are contained by the given path.
     * @param queryString
     *            a string representing the query
     * @param userId
     *            the id of the user on behalf of whom the query is executed
     * @return if the path or query string is invalid or the local MRC is not
     *         responsible for the path or an I/O error occured
     * @throws UserException
     *             if the query is invalid
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public List<String> query(String path, String queryString, long userId)
            throws BrainException, UserException;

    /**
     * Sets the size of a file.
     * 
     * @param path
     *            the path of the file
     * @param fileSize
     *            the new size of the file
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void updateFileSize(String path, long fileSize)
            throws BrainException, UserException;

    /**
     * Terminates the Brain instance. All connections to remote hosts will be
     * closed, and unconfirmed writes will be flushed to disk.
     * 
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public void shutdown() throws BrainException;

    /**
     * Returns the list containing striping information about replicas for the
     * file with the given path.
     * 
     * @param globalFileId
     *            the global ID of the file in the form of "volumeId":"fileId"
     * @return a list of strings containing striping information
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     * @throws BrainException
     *             if an error occured in the storage backend
     */
    public List<String> getReplicas(String globalFileId) throws BrainException,
            UserException;

    /**
     * Locally creates a file tree from the given tree data. The tree is
     * inserted at the given target path.<br/> The purpose of this method is to
     * allow remote MRC instances to transfer directory trees to the local file
     * system, e.g.\ in connection with a 'move' operation.
     * 
     * The tree data is provided as follows:
     * 
     * treeData:TreeData = [fileData:FileData, attrs:AttributeList,
     * osdData:OSDEndpointList, stripingPolicyId: long, ref:string,
     * subElements:TreeData] <br/><br/> FileData = {name:string, atime:long,
     * ctime:long, mtime:long, size:long, userId:long, isDirectory:boolean}
     * <br/><br/> AttributeList = [{key:string, value:string, type:long,
     * userId:long}, {...}] <br/><br/> OSDEndpointList = [endpoint1:string,
     * ...]
     * 
     * @param treeData
     *            the data representing the subtree to add
     * @param targetPath
     *            the path where to add the subtree
     * @throws BrainException
     *             if an error occured in the storage backend
     * @throws UserException
     *             if the path is invalid or the local MRC is not responsible
     *             for the path
     */
    public void createFileTree(List<Object> treeData, String targetPath)
            throws BrainException, UserException;

    // -- MONITORING ROUTINES

    public Map<String, Object> getPerVolumeOSDs();

    /**
     * Returns a map of volumes held by the local MRCs. The result is returned
     * in the form of a mapping from volume ids to volume names.
     * 
     * @return a map volumeId -> volumeName of all volumes on the local server
     */
    public Map<String, String> getLocalVolumes() throws Exception;

}
