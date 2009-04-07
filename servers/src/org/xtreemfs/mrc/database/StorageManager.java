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
package org.xtreemfs.mrc.database;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Path;

public interface StorageManager {
    
    /**
     * userID for system attributes; can be used w/ <code>getXAttr()</code> and
     * <code>getXAttrs()</code> to retrieve extended attributes assigned by the
     * system
     */
    public static final String SYSTEM_UID = "";
    
    /**
     * userID for global attributes; can be used w/ <code>getXAttr()</code> and
     * <code>getXAttrs()</code> to retrieve extended attributes visible to any
     * user
     */
    public static final String GLOBAL_ID  = "*";
    
    // initialization
    
    public void init(String ownerId, String owningGroupId, int perms, ACLEntry[] acl,
        org.xtreemfs.interfaces.StripingPolicy rootDirDefSp, AtomicDBUpdate update) throws DatabaseException;
    
    // file ID counter operations
    
    public long getNextFileId() throws DatabaseException;
    
    public void setLastFileId(long fileId, AtomicDBUpdate update) throws DatabaseException;
    
    // entity generators
    
    public AtomicDBUpdate createAtomicDBUpdate(DBAccessResultListener listener, Object context)
        throws DatabaseException;
    
    public ACLEntry createACLEntry(long fileId, String entity, short rights);
    
    public XLoc createXLoc(StripingPolicy stripingPolicy, String[] osds);
    
    public XLocList createXLocList(XLoc[] replicas, String replUpdatePolicy, int version);
    
    public StripingPolicy createStripingPolicy(String pattern, int stripeSize, int width);
    
    public XAttr createXAttr(long fileId, String owner, String key, String value);
    
    public void dumpDB(BufferedWriter xmlWriter) throws DatabaseException, IOException;
    
    // XAttrs
    
    public void setXAttr(long fileId, String uid, String key, String value, AtomicDBUpdate update)
        throws DatabaseException;
    
    public String getXAttr(long fileId, String uid, String key) throws DatabaseException;
    
    public Iterator<XAttr> getXAttrs(long fileId) throws DatabaseException;
    
    public Iterator<XAttr> getXAttrs(long fileId, String uid) throws DatabaseException;
    
    // ACLs
    
    public void setACLEntry(long fileId, String entity, Short rights, AtomicDBUpdate update)
        throws DatabaseException;
    
    public ACLEntry getACLEntry(long fileId, String entity) throws DatabaseException;
    
    public Iterator<ACLEntry> getACL(long fileId) throws DatabaseException;
    
    // creating, linking, modifying and deleting files/directories
    
    public FileMetadata createDir(long fileId, long parentId, String fileName, int atime, int ctime,
        int mtime, String userId, String groupId, int perms, long w32Attrs, AtomicDBUpdate update)
        throws DatabaseException;
    
    public FileMetadata createFile(long fileId, long parentId, String fileName, int atime, int ctime,
        int mtime, String userId, String groupId, int perms, long w32Attrs, long size, boolean readOnly,
        int epoch, int issEpoch, AtomicDBUpdate update) throws DatabaseException;
    
    public FileMetadata createSymLink(long fileId, long parentId, String fileName, int atime, int ctime,
        int mtime, String userId, String groupId, String ref, AtomicDBUpdate update) throws DatabaseException;
    
    public void link(FileMetadata metadata, long newParentId, String newFileName, AtomicDBUpdate update)
        throws DatabaseException;
    
    public void setMetadata(FileMetadata metadata, byte type, AtomicDBUpdate update) throws DatabaseException;
    
    public void setDefaultStripingPolicy(long fileId, org.xtreemfs.interfaces.StripingPolicy defaultSp,
        AtomicDBUpdate update) throws DatabaseException;
    
    public short unlink(long parentId, String fileName, AtomicDBUpdate update) throws DatabaseException;
    
    public short delete(long parentId, String fileName, AtomicDBUpdate update) throws DatabaseException;
    
    // getting metadata
    
    public FileMetadata[] resolvePath(Path path) throws DatabaseException;
    
    public String getVolumeId();
    
    public String getVolumeName();
    
    public FileMetadata getMetadata(long fileId) throws DatabaseException;
    
    public FileMetadata getMetadata(long parentId, String fileName) throws DatabaseException;
    
    public StripingPolicy getDefaultStripingPolicy(long fileId) throws DatabaseException;
    
    public String getSoftlinkTarget(long fileId) throws DatabaseException;
    
    public Iterator<FileMetadata> getChildren(long parentId) throws DatabaseException;
    
}
