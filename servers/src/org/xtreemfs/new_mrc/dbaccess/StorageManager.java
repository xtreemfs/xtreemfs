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
package org.xtreemfs.new_mrc.dbaccess;

import java.util.Iterator;
import java.util.Map;

import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.StripingPolicy;
import org.xtreemfs.new_mrc.metadata.XAttr;
import org.xtreemfs.new_mrc.metadata.XLoc;
import org.xtreemfs.new_mrc.metadata.XLocList;

public interface StorageManager {
    
    /**
     * userID for system attributes; can be used w/ <code>getXAttr()</code> and
     * <code>getXAttrs()</code> to retrieve extended attributes assigned by the
     * system
     */
    public static final String SYSTEM_UID = "";
    
    /**
     * userID for global attributes; can be used w/ <code>getXAttr()</code> and
     * <code>getXAttrs()</code> to retrieve extended attributes visible to any user
     */
    public static final String GLOBAL_ID = "*";
    
    // misc
    
    public void init(String ownerId, String owningGroupId, short perms,
        Map<String, Object> rootDirDefSp, AtomicDBUpdate update) throws DatabaseException;
    
    public AtomicDBUpdate createAtomicDBUpdate(DBAccessResultListener listener, Object context)
        throws DatabaseException;
    
    public ACLEntry createACLEntry(long fileId, String entity, short rights);
    
    public XLoc createXLoc(StripingPolicy stripingPolicy, String[] osds);
    
    public XLocList createXLocList(XLoc[] replicas, int version);
    
    public StripingPolicy createStripingPolicy(String pattern, int stripeSize, int width);
    
    public XAttr createXAttr(long fileId, String owner, String key, String value);
    
    // XAttrs
    
    public void setXAttr(long fileId, String uid, String key, String value, AtomicDBUpdate update)
        throws DatabaseException;
    
    public String getXAttr(long fileId, String uid, String key) throws DatabaseException;
    
    public Iterator<XAttr> getXAttrs(long fileId) throws DatabaseException;
    
    public Iterator<XAttr> getXAttrs(long fileId, String uid) throws DatabaseException;
    
    // ACLs
    
    public void setACLEntry(long fileId, String entity, Integer rights, AtomicDBUpdate update)
        throws DatabaseException;
    
    public int getACLEntry(long fileId, String entity) throws DatabaseException;
    
    public Iterator<ACLEntry> getACL(long fileId) throws DatabaseException;
    
    // file creation and linking
    
    public BufferBackedFileMetadata create(long parentId, String fileName, String userId,
        String groupId, Map<String, Object> stripingPolicy, short perms, String ref,
        boolean directory, AtomicDBUpdate update) throws DatabaseException;
    
    public long link(long parentId, String fileName, long newParentId, String newFileName,
        AtomicDBUpdate update) throws DatabaseException;
    
    public void delete(long parentId, String fileName, AtomicDBUpdate update)
        throws DatabaseException;
    
    public long resolvePath(String path) throws DatabaseException;
    
    // getting metadata
    
    public Object[] getParentIdAndFileName(long fileId) throws DatabaseException;
    
    public FileMetadata getMetadata(long parentId, String fileName) throws DatabaseException;
    
    public StripingPolicy getDefaultStripingPolicy(long fileId) throws DatabaseException;
    
    public String getSoftlinkTarget(long fileId) throws DatabaseException;
    
    public Iterator<FileMetadata> getChildren(long parentId) throws DatabaseException;
    
    // setting metadata
    
    public void setMetadata(long parentId, String fileName, FileMetadata metadata, int type,
        AtomicDBUpdate update) throws DatabaseException;
    
    public void setDefaultStripingPolicy(long fileId, StripingPolicy defaultSp,
        AtomicDBUpdate update) throws DatabaseException;
    
    // X-Locations list operation
    
    public void addReplica(long parentId, String fileName, XLoc replica, AtomicDBUpdate update)
        throws DatabaseException;
    
    public void deleteReplica(long parentId, String fileName, int indexl, AtomicDBUpdate update)
        throws DatabaseException;
    
}
