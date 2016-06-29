/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.utils;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;

/**
 * Provides convenience methods for accessing a path to a file, and resolves
 * frequently-needed pieces of metadata, such as the parent directory of the
 * file described by the given path. This functionality is needed by most
 * XtreemFS operations.
 * 
 * @author stender
 * 
 */
public class PathResolver {
    
    private String               fileName;
    
    private String               parentDirName;
    
    private final Path           path;
    
    private final FileMetadata[] resolvedPath;
    
    /**
     * Creates a new resolver for the given path.
     * 
     * @param sMan
     * @param path
     * @throws DatabaseException
     */
    public PathResolver(StorageManager sMan, Path path) throws DatabaseException, UserException {
        
        this.path = path;
        this.resolvedPath = sMan.resolvePath(path);
        
        // check if the resolved path prefix exists
        if (resolvedPath.length > 1 && resolvedPath[resolvedPath.length - 2] == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "path '" + path + "' does not exist");
    }
    
    public PathResolver(Path path, FileMetadata... files) {
        this.path = path;
        this.resolvedPath = files;
    }
    
    public String getFileName() {
        if (fileName == null)
            fileName = path.getLastComp(0);
        return fileName;
    }
    
    public FileMetadata getFile() throws DatabaseException {
        return resolvedPath[resolvedPath.length - 1];
    }
    
    public void checkIfFileExistsAlready() throws DatabaseException, UserException {
        if (getFile() != null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EEXIST, "file or directory '" + path
                + "' exists already");
    }
    
    public void checkIfFileDoesNotExist() throws DatabaseException, UserException {
        if (getFile() == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file or directory '" + path
                + "' does not exist");
    }
    
    public String getParentDirName() {
        if (parentDirName == null)
            parentDirName = path.getCompCount() == 1 ? "" : path.getLastComp(1);
        return parentDirName;
    }
    
    public long getParentDirId() throws DatabaseException {
        return resolvedPath.length == 1 ? 0 : resolvedPath[resolvedPath.length - 2].getId();
    }
    
    public FileMetadata getParentDir() throws DatabaseException {
        return resolvedPath.length == 1 ? null : resolvedPath[resolvedPath.length - 2];
    }
    
    public long getParentsParentId() throws DatabaseException {
        return resolvedPath.length == 1 ? -1 : resolvedPath.length == 2 ? 0
            : resolvedPath[resolvedPath.length - 3].getId();
    }
    
    public FileMetadata[] getResolvedPath() {
        return resolvedPath;
    }
    
    public String toString() {
        return path.toString();
    }
    
}
