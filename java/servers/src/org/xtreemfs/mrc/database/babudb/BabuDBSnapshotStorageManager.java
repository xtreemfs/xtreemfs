/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.SnapshotManager;
import org.xtreemfs.babudb.api.database.DatabaseRO;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.ACLIterator;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.XAttrIterator;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.DBAdminHelper;
import org.xtreemfs.mrc.utils.Path;

public class BabuDBSnapshotStorageManager implements StorageManager {
    
    public static final int                FILE_INDEX                 = 0;
    
    public static final int                XATTRS_INDEX               = 1;
    
    public static final int                ACL_INDEX                  = 2;
    
    public static final int                FILE_ID_INDEX              = 3;
    
    public static final int                VOLUME_INDEX               = 4;
    
    public static final byte[]             LAST_ID_KEY                = { 'i' };
    
    public static final byte[]             VOL_SIZE_KEY               = { 's' };
    
    public static final byte[]             NUM_FILES_KEY              = { 'f' };
    
    public static final byte[]             NUM_DIRS_KEY               = { 'd' };
    
    private static final String            DEFAULT_SP_ATTR_NAME       = "sp";
    
    private static final String            DEFAULT_RP_ATTR_NAME       = "rp";
    
    private static final String            LINK_TARGET_ATTR_NAME      = "lt";
    
    protected static final String          OSD_POL_ATTR_NAME          = "osdPol";
    
    protected static final String          REPL_POL_ATTR_NAME         = "replPol";
    
    protected static final String          AC_POL_ATTR_NAME           = "acPol";
    
    protected static final String          AUTO_REPL_FACTOR_ATTR_NAME = "replFactor";
    
    protected static final String          AUTO_REPL_FULL_ATTR_NAME   = "replFull";
    
    protected static final String          VOL_ID_ATTR_NAME           = "volId";
    
    protected static final String          VOL_QUOTA                  = "quota";
    
    protected static final int[]           ALL_INDICES                = { FILE_INDEX, XATTRS_INDEX,
        ACL_INDEX, FILE_ID_INDEX, VOLUME_INDEX                       };
    
    private final DatabaseRO               database;
    
    private final BabuDBSnapshotVolumeInfo volume;
    
    private final String                   volumeName;
    
    private final String                   rootDirName;
    
    private final long                     rootParentId;
    
    /**
     * Instantiates a storage manager by creating a new database.
     * 
     * @param sMan
     *            the snapshot manager
     * @param volumeId
     *            the volume ID
     */
    public BabuDBSnapshotStorageManager(SnapshotManager sMan, String volumeName, String volumeId, String snapName,
        long timestamp, long rootParentId) throws DatabaseException {
        
        this.volumeName = volumeName;
        
        try {
            database = sMan.getSnapshotDB(volumeId, snapName);
        } catch (BabuDBException e) {
            throw new DatabaseException("could not retrieve database for snapshot '" + snapName + "'", e);
        }
        
        try {
            this.rootParentId = rootParentId;
            this.rootDirName = BabuDBStorageHelper.getRootDirName(database, rootParentId);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        if (this.rootParentId == -1)
            throw new DatabaseException("no root directory found", ExceptionType.INTERNAL_DB_ERROR);
        
        this.volume = new BabuDBSnapshotVolumeInfo(timestamp);
        volume.init(this);
    }
    
    @Override
    public VolumeInfo getVolumeInfo() {
        return volume;
    }
    
    @Override
    public DatabaseResultSet<ACLEntry> getACL(long fileId) throws DatabaseException {
        
        try {
            
            byte[] prefix = BabuDBStorageHelper.createACLPrefixKey(fileId, null);
            ResultSet<byte[], byte[]> it = database.prefixLookup(ACL_INDEX, prefix, null).get();
            
            return new ACLIterator(it);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public ACLEntry getACLEntry(long fileId, String entity) throws DatabaseException {
        
        try {
            
            byte[] key = BabuDBStorageHelper.createACLPrefixKey(fileId, entity);
            byte[] value = database.lookup(ACL_INDEX, key, null).get();
            
            return value == null ? null : new BufferBackedACLEntry(key, value);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public DatabaseResultSet<FileMetadata> getChildren(long parentId, int from, int num) throws DatabaseException {
        
        try {
            return BabuDBStorageHelper.getChildren(database, parentId, from, num);
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public StripingPolicy getDefaultStripingPolicy(long fileId) throws DatabaseException {
        
        try {
            byte[] spBytes = getXAttr(fileId, SYSTEM_UID, DEFAULT_SP_ATTR_NAME);
            if (spBytes == null)
                return null;
            
            return Converter.stringToStripingPolicy(this, new String(spBytes));
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public ReplicationPolicy getDefaultReplicationPolicy(long fileId) throws DatabaseException {
        
        try {
            byte[] rpBytes = getXAttr(fileId, SYSTEM_UID, DEFAULT_RP_ATTR_NAME);
            if (rpBytes == null)
                return null;
            
            return Converter.stringToReplicationPolicy(this, new String(rpBytes));
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public long getVolumeQuota() throws DatabaseException {
        try {
            byte[] quotaBytes = getXAttr(1, SYSTEM_UID, VOL_QUOTA);
            if (quotaBytes == null) {
                return 0;
            } else {
                return Long.valueOf(new String(quotaBytes));
            }
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public FileMetadata getMetadata(long fileId) throws DatabaseException {
        
        ResultSet<byte[], byte[]> it = null;
        try {
            
            // create the key for the file ID index lookup
            byte[] key = BabuDBStorageHelper.createFileIdIndexKey(fileId, (byte) -1);
            ByteBuffer.wrap(key).putLong(fileId);
            
            byte[][] valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            
            // retrieve the metadata from the link index
            it = database.prefixLookup(
                BabuDBSnapshotStorageManager.FILE_ID_INDEX, key, null).get();
            
            while (it.hasNext()) {
                
                Entry<byte[], byte[]> curr = it.next();
                
                int type = BabuDBStorageHelper.getType(curr.getKey(),
                    BabuDBSnapshotStorageManager.FILE_ID_INDEX);
                
                // if the value is a back link, resolve it
                if (type == 3) {
                    
                    long parentId = ByteBuffer.wrap(curr.getValue()).getLong();
                    String fileName = new String(curr.getValue(), 8, curr.getValue().length - 8);
                    
                    return getMetadata(parentId, fileName);
                }
                
                valBufs[type] = curr.getValue();
            }
            
            // if not metadata was found for the file ID, return null
            if (valBufs[FileMetadata.RC_METADATA] == null)
                return null;
            
            byte[][] keyBufs = new byte[][] { null,
                BabuDBStorageHelper.createFileKey(0, "", FileMetadata.RC_METADATA) };
            
            // otherwise, a hard link target is contained in the index; create a
            // new metadata object in this case
            return new BufferBackedFileMetadata(keyBufs, valBufs, BabuDBSnapshotStorageManager.FILE_ID_INDEX);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        } finally {
            if (it != null)
                it.free();
        }
    }
    
    @Override
    public FileMetadata getMetadata(final long parentId, final String fileName) throws DatabaseException {
        
        try {
            return BabuDBStorageHelper.getMetadata(database, parentId, fileName);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String getSoftlinkTarget(long fileId) throws DatabaseException {
        
        try {
            byte[] target = getXAttr(fileId, SYSTEM_UID, LINK_TARGET_ATTR_NAME);
            return target == null? null: new String(target);
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public byte[] getXAttr(long fileId, String uid, String key) throws DatabaseException {
        
        ResultSet<byte[], byte[]> it = null;
        try {
            
            // peform a prefix lookup
            byte[] prefix = BabuDBStorageHelper.createXAttrPrefixKey(fileId, uid, key);
            it = database.prefixLookup(XATTRS_INDEX, prefix, null).get();
            
            // check whether the entry is the correct one
            while (it.hasNext()) {
                
                Entry<byte[], byte[]> curr = it.next();
                BufferBackedXAttr xattr = new BufferBackedXAttr(curr.getKey(), curr.getValue());
                if (uid.equals(xattr.getOwner()) && key.equals(xattr.getKey()))
                    return xattr.getValue();
            }
            
            return null;
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        } finally {
            if (it != null)
                it.free();
        }
    }
    
    @Override
    public DatabaseResultSet<XAttr> getXAttrs(long fileId) throws DatabaseException {
        
        try {
            
            // peform a prefix lookup
            byte[] prefix = BabuDBStorageHelper.createXAttrPrefixKey(fileId, null, null);
            ResultSet<byte[], byte[]> it = database.prefixLookup(XATTRS_INDEX, prefix, null).get();
            
            return new XAttrIterator(it, null);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public DatabaseResultSet<XAttr> getXAttrs(long fileId, String uid) throws DatabaseException {
        
        try {
            
            // peform a prefix lookup
            byte[] prefix = BabuDBStorageHelper.createXAttrPrefixKey(fileId, uid, null);
            ResultSet<byte[], byte[]> it = database.prefixLookup(XATTRS_INDEX, prefix, null).get();
            
            return new XAttrIterator(it, uid);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public FileMetadata[] resolvePath(final Path path) throws DatabaseException {
        
        try {
            FileMetadata[] md = new FileMetadata[path.getCompCount()];
            
            long parentId = rootParentId;
            for (int i = 0; i < md.length; i++) {
                md[i] = BabuDBStorageHelper.getMetadata(database, parentId, i == 0 ? rootDirName : path
                        .getComp(i));
                if (md[i] == null || i < md.length - 1 && !md[i].isDirectory()) {
                    md[i] = null;
                    return md;
                }
                parentId = md[i].getId();
            }
            
            return md;
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    protected long getVolumeSize() throws DatabaseException {
        try {
            byte[] sizeBytes = BabuDBStorageHelper.getVolumeMetadata(database, VOL_SIZE_KEY);
            return ByteBuffer.wrap(sizeBytes).getLong(0);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    protected long getNumFiles() throws DatabaseException {
        try {
            byte[] sizeBytes = BabuDBStorageHelper.getVolumeMetadata(database, NUM_FILES_KEY);
            return ByteBuffer.wrap(sizeBytes).getLong(0);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    protected long getNumDirs() throws DatabaseException {
        try {
            byte[] sizeBytes = BabuDBStorageHelper.getVolumeMetadata(database, NUM_DIRS_KEY);
            return ByteBuffer.wrap(sizeBytes).getLong(0);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void addVolumeChangeListener(VolumeChangeListener listener) {
    }
    
    @Override
    public ACLEntry createACLEntry(long fileId, String entity, short rights) {
        return null;
    }
    
    @Override
    public AtomicDBUpdate createAtomicDBUpdate(DBAccessResultListener<Object> listener, Object context)
        throws DatabaseException {
        
        try {
            return new AtomicBabuDBSnapshotUpdate(listener == null ? null
                : new BabuDBRequestListenerWrapper<Object>(listener), context);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public FileMetadata createDir(long fileId, long parentId, String fileName, int atime, int ctime,
        int mtime, String userId, String groupId, int perms, long w32Attrs, AtomicDBUpdate update)
        throws DatabaseException {
        throwException();
        return null;
    }
    
    @Override
    public FileMetadata createFile(long fileId, long parentId, String fileName, int atime, int ctime,
        int mtime, String userId, String groupId, int perms, long w32Attrs, long size, boolean readOnly,
        int epoch, int issEpoch, AtomicDBUpdate update) throws DatabaseException {
        throwException();
        return null;
    }
    
    @Override
    public void createSnapshot(String snapName, long parentId, String dirName, boolean recursive)
        throws DatabaseException {
        throwException();
    }
    
    @Override
    public StripingPolicy createStripingPolicy(String pattern, int stripeSize, int width, int parityWidth) {
        return null;
    }
    
    @Override
    public FileMetadata createSymLink(long fileId, long parentId, String fileName, int atime, int ctime,
        int mtime, String userId, String groupId, String ref, AtomicDBUpdate update) throws DatabaseException {
        throwException();
        return null;
    }
    
    @Override
    public XAttr createXAttr(long fileId, String owner, String key, byte[] value) {
        return null;
    }
    
    @Override
    public XLoc createXLoc(StripingPolicy stripingPolicy, String[] osds, int replFlags) {
        return null;
    }
    
    @Override
    public XLocList createXLocList(XLoc[] replicas, String replUpdatePolicy, int version) {
        return null;
    }
    
    @Override
    public short delete(long parentId, String fileName, AtomicDBUpdate update) throws DatabaseException {
        throwException();
        return -1;
    }
    
    @Override
    public void deleteDatabase() throws DatabaseException {
        throwException();
    }
    
    @Override
    public void deleteSnapshot(String snapName) throws DatabaseException {
        throwException();
    }
    
    @Override
    public void dumpDB(BufferedWriter xmlWriter) throws DatabaseException, IOException {
        DBAdminHelper.dumpVolume(xmlWriter, this);
    }
    
    @Override
    public String[] getAllSnapshots() throws DatabaseException {
        throwException();
        return null;
    }
    
    @Override
    public long getNextFileId() throws DatabaseException {
        throwException();
        return -1;
    }
    
    @Override
    public void link(FileMetadata metadata, long newParentId, String newFileName, AtomicDBUpdate update)
        throws DatabaseException {
        throwException();
    }
    
    @Override
    public void setACLEntry(long fileId, String entity, Short rights, AtomicDBUpdate update)
        throws DatabaseException {
        throwException();
    }
    
    @Override
    public void setDefaultStripingPolicy(long fileId,
        org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy defaultSp, AtomicDBUpdate update)
        throws DatabaseException {
        throwException();
    }
    
    @Override
    public void setDefaultReplicationPolicy(long fileId, ReplicationPolicy defaultRp, AtomicDBUpdate update)
        throws DatabaseException {
        throwException();
    }
    
    @Override
    public void setVolumeQuota(long quota, AtomicDBUpdate update) throws DatabaseException {
        throwException();
    }

    @Override
    public void setLastFileId(long fileId, AtomicDBUpdate update) throws DatabaseException {
        throwException();
    }
    
    @Override
    public void setMetadata(FileMetadata metadata, byte type, AtomicDBUpdate update) throws DatabaseException {
        throwException();
    }
    
    @Override
    public void setXAttr(long fileId, String uid, String key, byte[] value, AtomicDBUpdate update)
        throws DatabaseException {
        throwException();
    }
    
    @Override
    public short unlink(long parentId, String fileName, AtomicDBUpdate update) throws DatabaseException {
        throwException();
        return -1;
    }
    
    protected String getVolumeName() {
        return volumeName;
    }
    
    protected void throwException() throws DatabaseException {
        throw new DatabaseException("cannot invoke this operation on a snapshot", ExceptionType.NOT_ALLOWED);
    }
}
