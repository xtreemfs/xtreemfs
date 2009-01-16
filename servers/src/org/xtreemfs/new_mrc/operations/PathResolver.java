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

package org.xtreemfs.new_mrc.operations;

import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
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
    
    private final Path         path;
    
    private StorageManager     sMan;
    
    private final long         parentsParentId;
    
    private final FileMetadata parentDir;
    
    private String             pathPrefix;
    
    private String             parentDirName;
    
    private String             fileName;
    
    private FileMetadata       file;
    
    /**
     * Creates a new resolver for the given path.
     * 
     * @param sMan
     * @param path
     * @throws DatabaseException
     */
    public PathResolver(StorageManager sMan, Path path) throws DatabaseException {
        
        this.path = path;
        this.sMan = sMan;
        
        // resolve the path
        parentsParentId = path.getCompCount() == 1 ? -1 : path.getCompCount() == 2 ? 0 : sMan
                .resolvePath(path.getComps(1, path.getCompCount() - 3));
        parentDir = sMan.getMetadata(parentsParentId, getParentDirName());
    }
    
    public String getFileName() {
        if (fileName == null)
            fileName = path.getLastComp(0);
        return fileName;
    }
    
    public FileMetadata getFile() throws DatabaseException {
        if (file == null)
            file = sMan.getMetadata(getParentDirId(), getFileName());
        return file;
    }
    
    public void checkIfFileExistsAlready() throws DatabaseException, UserException {
        if (getFile() != null)
            throw new UserException(ErrNo.EEXIST, "file or directory '" + path + "' exists already");
    }
    
    public void checkIfFileDoesNotExist() throws DatabaseException, UserException {
        if (getFile() == null)
            throw new UserException(ErrNo.EEXIST, "file or directory '" + path + "' does not exist");
    }
    
    public String getParentDirName() {
        if (parentDirName == null)
            parentDirName = path.getCompCount() == 1 ? "" : path.getLastComp(1);
        return parentDirName;
    }
    
    public long getParentDirId() throws DatabaseException {
        return parentDir == null ? 0 : parentDir.getId();
    }
    
    public FileMetadata getParentDir() throws DatabaseException {
        return parentDir;
    }
    
    public long getParentsParentId() throws DatabaseException {
        return parentsParentId;
    }
    
    public String getPathPrefix() {
        if (pathPrefix == null)
            pathPrefix = path.getComps(1, path.getCompCount() - 2);
        return pathPrefix;
    }
    
    public String toString() {
        return path.toString();
    }
    
}
