/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.OwnerType;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.FileVoucherClientInfo;
import org.xtreemfs.mrc.metadata.FileVoucherInfo;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
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
    public static final String SYSTEM_UID          = "";
    
    /**
     * userID for global attributes; can be used w/ <code>getXAttr()</code> and
     * <code>getXAttrs()</code> to retrieve extended attributes visible to any
     * user
     */
    public static final String GLOBAL_ID           = "*";
    
    /**
     * key prefix for XtreemFS system attributes
     */
    public static final String SYS_ATTR_KEY_PREFIX = "xtreemfs.";
    
    // file ID counter operations
    
    public long getNextFileId() throws DatabaseException;
    
    public void setLastFileId(long fileId, AtomicDBUpdate update) throws DatabaseException;
    
    // entity generators
    
    public AtomicDBUpdate createAtomicDBUpdate(DBAccessResultListener<Object> listener, Object context)
        throws DatabaseException;
    
    public ACLEntry createACLEntry(long fileId, String entity, short rights);
    
    public XLoc createXLoc(StripingPolicy stripingPolicy, String[] osds, int replFlags);
    
    public XLocList createXLocList(XLoc[] replicas, String replUpdatePolicy, int version);
    
    public StripingPolicy createStripingPolicy(String pattern, int stripeSize, int width, int parity);

    public XAttr createXAttr(long fileId, String owner, String key, byte[] value);
    
    public void dumpDB(BufferedWriter xmlWriter) throws DatabaseException, IOException;
    
    // handling volumes
    
    public VolumeInfo getVolumeInfo();
    
    public void addVolumeChangeListener(VolumeChangeListener listener);
    
    public void deleteDatabase() throws DatabaseException;
    
    // handling XAttrs
    
    public void setXAttr(long fileId, String uid, String key, byte[] value, AtomicDBUpdate update)
        throws DatabaseException;
    
    public byte[] getXAttr(long fileId, String uid, String key) throws DatabaseException;
    
    public DatabaseResultSet<XAttr> getXAttrs(long fileId) throws DatabaseException;
    
    public DatabaseResultSet<XAttr> getXAttrs(long fileId, String uid) throws DatabaseException;
    
    // handling ACLs
    
    public void setACLEntry(long fileId, String entity, Short rights, AtomicDBUpdate update)
        throws DatabaseException;
    
    public ACLEntry getACLEntry(long fileId, String entity) throws DatabaseException;
    
    public DatabaseResultSet<ACLEntry> getACL(long fileId) throws DatabaseException;
    
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
    
    public void setDefaultStripingPolicy(long fileId, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy defaultSp,
        AtomicDBUpdate update) throws DatabaseException;

    public void setFileVoucherInfo(FileVoucherInfo fileVoucherInfo, AtomicDBUpdate update) throws DatabaseException;

    public void setFileVoucherClientInfo(FileVoucherClientInfo fileVoucherClientInfo, AtomicDBUpdate update)
            throws DatabaseException;

    public void setVolumeQuota(long quota, AtomicDBUpdate update) throws DatabaseException;

    public void setVolumeBlockedSpace(long blockedSpace, AtomicDBUpdate update) throws DatabaseException;

    public void setVolumeUsedSpace(long usedSpace, AtomicDBUpdate update) throws DatabaseException;

    public void setVoucherSize(long voucherSize, AtomicDBUpdate update) throws DatabaseException;

    public void setDefaultUserQuota(long defaultUserQuota, AtomicDBUpdate update) throws DatabaseException;

    public void setDefaultGroupQuota(long defaultGroupQuota, AtomicDBUpdate update) throws DatabaseException;

    public void setGroupQuota(String groupId, Long quota, AtomicDBUpdate update) throws DatabaseException;

    public void setGroupBlockedSpace(String groupId, Long blockedSpace, AtomicDBUpdate update) throws DatabaseException;

    public void setGroupUsedSpace(String groupId, Long usedSpace, AtomicDBUpdate update) throws DatabaseException;

    public void setUserQuota(String userId, Long quota, AtomicDBUpdate update) throws DatabaseException;

    public void setUserBlockedSpace(String userId, Long blockedSpace, AtomicDBUpdate update) throws DatabaseException;

    public void setUserUsedSpace(String userId, Long usedSpace, AtomicDBUpdate update) throws DatabaseException;

    public void setDefaultReplicationPolicy(long fileId, ReplicationPolicy defaultRp,
        AtomicDBUpdate update) throws DatabaseException;
    
    public short unlink(long parentId, String fileName, AtomicDBUpdate update) throws DatabaseException;
    
    public short delete(long parentId, String fileName, AtomicDBUpdate update) throws DatabaseException;
    
    // retrieving metadata
    
    public FileMetadata[] resolvePath(Path path) throws DatabaseException;
    
    public FileMetadata getMetadata(long fileId) throws DatabaseException;
    
    public FileMetadata getMetadata(long parentId, String fileName) throws DatabaseException;
    
    public StripingPolicy getDefaultStripingPolicy(long fileId) throws DatabaseException;
    
    public ReplicationPolicy getDefaultReplicationPolicy(long fileId) throws DatabaseException;

    public FileVoucherInfo getFileVoucherInfo(long fileId) throws DatabaseException;

    public FileVoucherClientInfo getFileVoucherClientInfo(long fileId, String clientId) throws DatabaseException;

    public DatabaseResultSet<FileVoucherClientInfo> getAllFileVoucherClientInfo(long fileId) throws DatabaseException;

    public long getVolumeQuota() throws DatabaseException;

    public long getVolumeBlockedSpace() throws DatabaseException;

    public long getVolumeUsedSpace() throws DatabaseException;

    public long getVoucherSize() throws DatabaseException;

    public long getDefaultUserQuota() throws DatabaseException;

    public long getDefaultGroupQuota() throws DatabaseException;

    public long getUserQuota(String userId) throws DatabaseException;

    public long getUserUsedSpace(String userId) throws DatabaseException;

    public long getUserBlockedSpace(String userId) throws DatabaseException;

    public long getGroupQuota(String groupId) throws DatabaseException;

    public long getGroupUsedSpace(String groupId) throws DatabaseException;

    public long getGroupBlockedSpace(String groupId) throws DatabaseException;

    public Map<String, Map<String, Long>> getAllOwnerQuotaInfo(OwnerType ownerType, String id) throws DatabaseException;

    public String getSoftlinkTarget(long fileId) throws DatabaseException;
    
    public DatabaseResultSet<FileMetadata> getChildren(long parentId, int seen, int num) throws DatabaseException;
    
    // handling snapshots
    
    public void createSnapshot(String snapName, long parentId, String dirName, boolean recursive)
        throws DatabaseException;
    
    public void deleteSnapshot(String snapName) throws DatabaseException;
    
    public String[] getAllSnapshots() throws DatabaseException;
}
