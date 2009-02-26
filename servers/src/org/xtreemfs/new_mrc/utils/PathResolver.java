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

package org.xtreemfs.new_mrc.utils;

import org.xtreemfs.new_mrc.ErrNo;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.database.DatabaseException;
import org.xtreemfs.new_mrc.database.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;

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
            throw new UserException(ErrNo.ENOENT, "path '" + path + "' does not exist");
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
            throw new UserException(ErrNo.EEXIST, "file or directory '" + path + "' exists already");
    }
    
    public void checkIfFileDoesNotExist() throws DatabaseException, UserException {
        if (getFile() == null)
            throw new UserException(ErrNo.ENOENT, "file or directory '" + path + "' does not exist");
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
