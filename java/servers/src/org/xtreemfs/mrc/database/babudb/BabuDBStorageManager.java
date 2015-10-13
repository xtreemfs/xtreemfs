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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.babudb.api.DatabaseManager;
import org.xtreemfs.babudb.api.SnapshotManager;
import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.index.DefaultByteRangeComparator;
import org.xtreemfs.babudb.snapshots.DefaultSnapshotConfig;
import org.xtreemfs.babudb.snapshots.SnapshotConfig;
import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.ACLIterator;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.FileVoucherClientInfoIterator;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.OwnerType;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.QuotaInfo;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.XAttrIterator;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.mrc.metadata.BufferBackedFileVoucherClientInfo;
import org.xtreemfs.mrc.metadata.BufferBackedFileVoucherInfo;
import org.xtreemfs.mrc.metadata.BufferBackedStripingPolicy;
import org.xtreemfs.mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.mrc.metadata.BufferBackedXLoc;
import org.xtreemfs.mrc.metadata.BufferBackedXLocList;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.FileVoucherClientInfo;
import org.xtreemfs.mrc.metadata.FileVoucherInfo;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.quota.VolumeQuotaManager;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.DBAdminHelper;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;

public class BabuDBStorageManager implements StorageManager {
    
    public static final int                  FILE_INDEX                 = 0;
    
    public static final int                  XATTRS_INDEX               = 1;
    
    public static final int                  ACL_INDEX                  = 2;
    
    public static final int                  FILE_ID_INDEX              = 3;
    
    public static final int                  VOLUME_INDEX               = 4;
    
    public static final byte[]               LAST_ID_KEY                = { 'i' };
    
    public static final byte[]               VOL_SIZE_KEY               = { 's' };
    
    public static final byte[]               NUM_FILES_KEY              = { 'f' };
    
    public static final byte[]               NUM_DIRS_KEY               = { 'd' };
    
    public static final String               FILE_VOUCHER_KEY_IDENTIFER = "v";           // TODO(baerhold) copy to
                                                                                          // Snapshot

    public static final String               QUOTA_KEY_IDENTIFIER       = "q";
    
    private static final String              DEFAULT_SP_ATTR_NAME       = "sp";
    
    private static final String              DEFAULT_RP_ATTR_NAME       = "rp";
    
    private static final String              LINK_TARGET_ATTR_NAME      = "lt";
    
    protected static final String            OSD_POL_ATTR_NAME          = "osdPol";
    
    protected static final String            REPL_POL_ATTR_NAME         = "replPol";
    
    protected static final String            AC_POL_ATTR_NAME           = "acPol";
    
    protected static final String            AUTO_REPL_FACTOR_ATTR_NAME = "replFactor";
    
    protected static final String            AUTO_REPL_FULL_ATTR_NAME   = "replFull";
    
    protected static final String            ALLOW_SNAPS_ATTR_NAME      = "allowSnaps";
    
    protected static final String            VOL_ID_ATTR_NAME           = "volId";
    
    protected static final String            VOL_QUOTA_ATTR_NAME        = "quota";
    
    protected static final String            VOL_BSPACE_ATTR_NAME       = "blockedSpace";

    protected static final String            VOL_USPACE_ATTR_NAME       = "usedSpace";

    protected static final String            VOL_VOUCHERSIZE_ATTR_NAME  = "voucherSize";

    protected static final String            VOL_DEFAULT_U_QUOTA_ATTR_NAME = "defaultUserQuota";

    protected static final String            VOL_DEFAULT_G_QUOTA_ATTR_NAME = "defaultGroupQuota";

    protected static final int[]             ALL_INDICES                = { FILE_INDEX, XATTRS_INDEX, ACL_INDEX,
            FILE_ID_INDEX, VOLUME_INDEX                                };
    
    private final DatabaseManager            dbMan;
    
    private final SnapshotManager            snapMan;
    
    private final Database                   database;
    
    private final List<VolumeChangeListener> vcListeners;
    
    private final BabuDBVolumeInfo           volume;
    
    /**
     * Instantiates a storage manager by loading an existing volume database.
     * 
     * @param dbs
     *            the database system
     * @param db
     *            the database
     */
    public BabuDBStorageManager(BabuDB dbs, Database db) throws DatabaseException {
        
        this.dbMan = dbs.getDatabaseManager();
        this.snapMan = dbs.getSnapshotManager();
        this.database = db;
        this.vcListeners = new LinkedList<VolumeChangeListener>();
        
        volume = new BabuDBVolumeInfo();
        volume.init(this);
    }
    
    /**
     * Instantiates a storage manager by loading an existing volume database.
     * 
     * @param dbMan
     *            the database manager
     * @param sMan
     *            the snapshot manager
     * @param db
     *            the database
     */
    public BabuDBStorageManager(DatabaseManager dbMan, SnapshotManager sMan, Database db) throws DatabaseException {
        
        this.dbMan = dbMan;
        this.snapMan = sMan;
        this.database = db;
        this.vcListeners = new LinkedList<VolumeChangeListener>();
        
        volume = new BabuDBVolumeInfo();
        volume.init(this);
    }
    
    /**
     * Instantiates a storage manager by creating a new database.
     * 
     * @param dbs
     *            the database system
     * @param volumeId
     *            the volume ID
     */
    public BabuDBStorageManager(BabuDB dbs, String volumeId, String volumeName, short fileAccessPolicyId,
            short[] osdPolicy, short[] replPolicy, String ownerId, String owningGroupId, int perms, ACLEntry[] acl,
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy rootDirDefSp, boolean allowSnaps,
            long volumeQuota, Map<String, String> attrs) throws DatabaseException {
        
        this.dbMan = dbs.getDatabaseManager();
        this.snapMan = dbs.getSnapshotManager();
        this.vcListeners = new LinkedList<VolumeChangeListener>();
        this.volume = new BabuDBVolumeInfo();
        
        TransactionalBabuDBUpdate update = new TransactionalBabuDBUpdate(dbMan);
        update.createDatabase(volumeId, 5);
        
        // atime, ctime, mtime
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        
        // create the root directory; the name is the database name
        createDir(1, 0, volumeName, time, time, time, ownerId, owningGroupId, perms, 0, true, update);
        setLastFileId(1, update);
        
        volume.init(this, update.getDatabaseName(), volumeName, osdPolicy, replPolicy, fileAccessPolicyId, allowSnaps,
                volumeQuota, update);
        
        // set the default striping policy
        if (rootDirDefSp != null)
            setDefaultStripingPolicy(1, rootDirDefSp, true, update);
        
        if (acl != null)
            for (ACLEntry entry : acl)
                setACLEntry(1L, entry.getEntity(), entry.getRights(), update);
        
        if (attrs != null)
            for (Entry<String, String> attr : attrs.entrySet())
                setXAttr(1L, SYSTEM_UID, "xtreemfs.volattr." + attr.getKey(), attr.getValue().getBytes(), true, update);
        
        update.execute();
        
        try {
            database = dbMan.getDatabase(update.getDatabaseName());
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
        
        notifyVolumeChange(volume);
    }
    
    @Override
    public VolumeInfo getVolumeInfo() {
        return volume;
    }
    
    @Override
    public void deleteDatabase() throws DatabaseException {
        try {
            dbMan.deleteDatabase(database.getName());
            notifyVolumeDelete(volume.getId());
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void addVolumeChangeListener(VolumeChangeListener listener) {
        vcListeners.add(listener);
        notifyVolumeChange(volume);
    }
    
    @Override
    public AtomicDBUpdate createAtomicDBUpdate(DBAccessResultListener<Object> listener, Object context)
            throws DatabaseException {
        try {
            return new AtomicBabuDBUpdate(database, listener == null ? null : new BabuDBRequestListenerWrapper<Object>(
                    listener), context);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public ACLEntry createACLEntry(long fileId, String entity, short rights) {
        return new BufferBackedACLEntry(fileId, entity, rights);
    }
    
    @Override
    public XLoc createXLoc(StripingPolicy stripingPolicy, String[] osds, int replFlags) {
        assert (stripingPolicy instanceof BufferBackedStripingPolicy);
        return new BufferBackedXLoc((BufferBackedStripingPolicy) stripingPolicy, osds, replFlags);
    }
    
    @Override
    public XLocList createXLocList(XLoc[] replicas, String replUpdatePolicy, int version) {
        BufferBackedXLoc[] tmp = new BufferBackedXLoc[replicas.length];
        for (int i = 0; i < replicas.length; i++)
            tmp[i] = (BufferBackedXLoc) replicas[i];
        return new BufferBackedXLocList(tmp, replUpdatePolicy, version);
    }
    
    @Override
    public StripingPolicy createStripingPolicy(String pattern, int stripeSize, int width) {
        return new BufferBackedStripingPolicy(pattern, stripeSize, width);
    }
    
    @Override
    public XAttr createXAttr(long fileId, String owner, String key, byte[] value) {
        return new BufferBackedXAttr(fileId, owner, key, value, (short) 0);
    }
    
    @Override
    public long getNextFileId() throws DatabaseException {
        
        try {
            // get the file ID assigned to the last created file or
            // directory
            byte[] idBytes = BabuDBStorageHelper.getLastAssignedFileId(database);
            
            // calculate the new file ID
            ByteBuffer tmp = ByteBuffer.wrap(idBytes);
            long id = tmp.getLong(0) + 1;
            
            return id;
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void setLastFileId(long fileId, AtomicDBUpdate update) throws DatabaseException {
        
        byte[] idBytes = new byte[8];
        ByteBuffer.wrap(idBytes).putLong(0, fileId);
        
        update.addUpdate(VOLUME_INDEX, LAST_ID_KEY, idBytes);
    }
    
    @Override
    public FileMetadata createDir(long fileId, long parentId, String fileName, int atime, int ctime, int mtime,
            String userId, String groupId, int perms, long w32Attrs, AtomicDBUpdate update) throws DatabaseException {
        
        return createDir(fileId, parentId, fileName, atime, ctime, mtime, userId, groupId, perms, w32Attrs, false,
                update);
    }
    
    public FileMetadata createDir(long fileId, long parentId, String fileName, int atime, int ctime, int mtime,
            String userId, String groupId, int perms, long w32Attrs, boolean initCount, AtomicDBUpdate update)
            throws DatabaseException {
        
        // create metadata
        BufferBackedFileMetadata fileMetadata = new BufferBackedFileMetadata(parentId, fileName, userId, groupId,
                fileId, atime, ctime, mtime, perms, w32Attrs, (short) 1);
        
        // update main metadata in the file index
        update.addUpdate(FILE_INDEX, fileMetadata.getFCMetadataKey(), fileMetadata.getFCMetadataValue());
        update.addUpdate(FILE_INDEX, fileMetadata.getRCMetadata().getKey(), fileMetadata.getRCMetadata().getValue());
        
        // add an entry to the file ID index
        update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(fileId, (byte) 3),
                BabuDBStorageHelper.createFileIdIndexValue(parentId, fileName));
        
        if (initCount)
            initCount(NUM_DIRS_KEY, update);
        else
            updateCount(NUM_DIRS_KEY, true, update);
        
        return fileMetadata;
    }
    
    @Override
    public FileMetadata createFile(long fileId, long parentId, String fileName, int atime, int ctime, int mtime,
            String userId, String groupId, int perms, long w32Attrs, long size, boolean readOnly, int epoch,
            int issEpoch, AtomicDBUpdate update) throws DatabaseException {
        
        // create metadata
        BufferBackedFileMetadata fileMetadata = new BufferBackedFileMetadata(parentId, fileName, userId, groupId,
                fileId, atime, ctime, mtime, size, perms, w32Attrs, (short) 1, epoch, issEpoch, readOnly);
        
        // update main metadata in the file index
        update.addUpdate(FILE_INDEX, fileMetadata.getFCMetadataKey(), fileMetadata.getFCMetadataValue());
        update.addUpdate(FILE_INDEX, fileMetadata.getRCMetadata().getKey(), fileMetadata.getRCMetadata().getValue());
        
        // add an entry to the file ID index
        update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(fileId, (byte) 3),
                BabuDBStorageHelper.createFileIdIndexValue(parentId, fileName));
        
        volume.updateVolumeSize(size, update);
        updateCount(NUM_FILES_KEY, true, update);
        
        return fileMetadata;
    }
    
    @Override
    public FileMetadata createSymLink(long fileId, long parentId, String fileName, int atime, int ctime, int mtime,
            String userId, String groupId, String ref, AtomicDBUpdate update) {
        
        // create metadata
        BufferBackedFileMetadata fileMetadata = new BufferBackedFileMetadata(parentId, fileName, userId, groupId,
                fileId, atime, ctime, mtime, ref.length(), 0777, 0, (short) 1, 0, 0, false);
        
        // create link target (XAttr)
        BufferBackedXAttr lt = new BufferBackedXAttr(fileId, SYSTEM_UID, LINK_TARGET_ATTR_NAME, ref.getBytes(),
                (short) 0);
        update.addUpdate(XATTRS_INDEX, lt.getKeyBuf(), lt.getValBuf());
        
        // update main metadata in the file index
        update.addUpdate(FILE_INDEX, fileMetadata.getFCMetadataKey(), fileMetadata.getFCMetadataValue());
        update.addUpdate(FILE_INDEX, fileMetadata.getRCMetadata().getKey(), fileMetadata.getRCMetadata().getValue());
        
        // add an entry to the file ID index
        update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(fileId, (byte) 3),
                BabuDBStorageHelper.createFileIdIndexValue(parentId, fileName));
        
        return fileMetadata;
    }
    
    @Override
    public short unlink(final long parentId, final String fileName, final AtomicDBUpdate update)
            throws DatabaseException {
        
        try {
            
            // retrieve the file metadata
            BufferBackedFileMetadata file = BabuDBStorageHelper.getMetadata(database, parentId, fileName);
            
            // determine and set the new link count
            short newLinkCount = (short) (file.getLinkCount() - 1);
            file.setLinkCount(newLinkCount);
            
            // if there will be links remaining after the deletion, update the
            // link count; it must be in the FILE_ID_INDEX, because there have
            // been at least two links
            if (newLinkCount > 0)
                update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(file.getId(),
                        FileMetadata.RC_METADATA), file.getRCMetadata().getValue());
            
            // remove all entries from the file index
            update.addUpdate(FILE_INDEX,
                    BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.FC_METADATA), null);
            update.addUpdate(FILE_INDEX,
                    BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.RC_METADATA), null);
            
            return newLinkCount;
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public short delete(final long parentId, final String fileName, final AtomicDBUpdate update)
            throws DatabaseException {
        
        try {
            
            // retrieve the file metadata
            BufferBackedFileMetadata file = BabuDBStorageHelper.getMetadata(database, parentId, fileName);
            
            // check whether there is only one link remaining
            short newLinkCount = (short) (file.getLinkCount() - 1);
            assert (newLinkCount >= 0);
            
            // decrement the link count
            file.setLinkCount(newLinkCount);
            
            // if there will be links remaining after the deletion, update the
            // link count
            if (newLinkCount > 0)
                update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(file.getId(),
                        FileMetadata.RC_METADATA), file.getRCMetadata().getValue());
            
            // delete all keys ...
            
            // remove all content from the file index
            update.addUpdate(BabuDBStorageManager.FILE_INDEX,
                    BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.FC_METADATA), null);
            update.addUpdate(BabuDBStorageManager.FILE_INDEX,
                    BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.RC_METADATA), null);
            
            // if the last link to the file is supposed to be deleted, remove
            // the remaining metadata, including ACLs and XAttrs
            if (newLinkCount == 0) {
                
                // remove the back link from the file ID index
                update.addUpdate(BabuDBStorageManager.FILE_ID_INDEX,
                        BabuDBStorageHelper.createFileIdIndexKey(file.getId(), (byte) 3), null);
                
                // remove potentially existing metadata from the file ID index
                update.addUpdate(BabuDBStorageManager.FILE_ID_INDEX,
                        BabuDBStorageHelper.createFileIdIndexKey(file.getId(), FileMetadata.FC_METADATA), null);
                update.addUpdate(BabuDBStorageManager.FILE_ID_INDEX,
                        BabuDBStorageHelper.createFileIdIndexKey(file.getId(), FileMetadata.RC_METADATA), null);
                
                byte[] idBytes = new byte[8];
                ByteBuffer.wrap(idBytes).putLong(file.getId());
                
                // remove all ACLs
                ResultSet<byte[], byte[]> it = database.prefixLookup(BabuDBStorageManager.ACL_INDEX, idBytes,
                        null).get();
                while (it.hasNext())
                    update.addUpdate(BabuDBStorageManager.ACL_INDEX, it.next().getKey(), null);
                it.free();
                
                // remove all extended attributes
                it = database.prefixLookup(BabuDBStorageManager.XATTRS_INDEX, idBytes, null).get();
                while (it.hasNext())
                    update.addUpdate(BabuDBStorageManager.XATTRS_INDEX, it.next().getKey(), null);
                it.free();
                
                // if a file is deleted, update file count and volume size
                if (file.isDirectory()) {
                    updateCount(NUM_DIRS_KEY, false, update);
                }
                
                else if (file.getXLocList() != null) {
                    volume.updateVolumeSize(-file.getSize(), update);
                    updateCount(NUM_FILES_KEY, false, update);
                }
                
            }
            
            return file.getLinkCount();
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
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
    public DatabaseResultSet<FileMetadata> getChildren(long parentId, int seen, int num) throws DatabaseException {
        
        try {
            return BabuDBStorageHelper.getChildren(database, parentId, seen, num);
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public StripingPolicy getDefaultStripingPolicy(long fileId) throws DatabaseException {
        
        try {
            byte[] sp = getXAttr(fileId, SYSTEM_UID, DEFAULT_SP_ATTR_NAME);
            if (sp == null)
                return null;
            
            return Converter.stringToStripingPolicy(this, new String(sp));
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public ReplicationPolicy getDefaultReplicationPolicy(long fileId) throws DatabaseException {
        
        try {
            byte[] rp = getXAttr(fileId, SYSTEM_UID, DEFAULT_RP_ATTR_NAME);
            if (rp == null)
                return null;
            
            return Converter.stringToReplicationPolicy(this, new String(rp));
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public FileVoucherInfo getFileVoucherInfo(long fileId) throws DatabaseException {
        try {
            byte[] key = BabuDBStorageHelper.createFileVoucherInfoKey(fileId);
            byte[] value = database.lookup(VOLUME_INDEX, key, null).get();

            return value == null ? null : new BufferBackedFileVoucherInfo(key, value);

        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public FileVoucherClientInfo getFileVoucherClientInfo(long fileId, String clientId) throws DatabaseException {
        try {
            byte[] key = BabuDBStorageHelper.createFileVoucherClientInfoKey(fileId, clientId);
            byte[] value = database.lookup(VOLUME_INDEX, key, null).get();

            return value == null ? null : new BufferBackedFileVoucherClientInfo(key, value);

        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public DatabaseResultSet<FileVoucherClientInfo> getAllFileVoucherClientInfo(long fileId) throws DatabaseException {
        try {
            byte[] prefixKey = BabuDBStorageHelper.createFileVoucherClientInfoKey(fileId, "");
            ResultSet<byte[], byte[]> it = database.prefixLookup(VOLUME_INDEX, prefixKey, null).get();

            return new FileVoucherClientInfoIterator(it);

        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getVolumeQuota() throws DatabaseException {
        try {
            byte[] quota = getXAttr(1, SYSTEM_UID, VOL_QUOTA_ATTR_NAME);
            if (quota == null)
                return 0;
            else {
                return Long.valueOf(new String(quota));
            }

        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getVolumeBlockedSpace() throws DatabaseException {
        try {
            byte[] blockedSpace = getXAttr(1, SYSTEM_UID, VOL_BSPACE_ATTR_NAME);
            if (blockedSpace == null)
                return 0;
            else {
                return Long.valueOf(new String(blockedSpace));
            }

        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getVolumeUsedSpace() throws DatabaseException {
        try {
            byte[] usedSpace = getXAttr(1, SYSTEM_UID, VOL_USPACE_ATTR_NAME);
            if (usedSpace == null)
                return 0;
            else {
                return Long.valueOf(new String(usedSpace));
            }

        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getVoucherSize() throws DatabaseException {
        try {
            byte[] voucherSize = getXAttr(1, SYSTEM_UID, VOL_VOUCHERSIZE_ATTR_NAME);
            if (voucherSize == null)
                return 0;
            else {
                return Long.valueOf(new String(voucherSize));
            }

        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getDefaultGroupQuota() throws DatabaseException {
        try {
            byte[] defaultGroupQuota = getXAttr(1, SYSTEM_UID, VOL_DEFAULT_G_QUOTA_ATTR_NAME);
            if (defaultGroupQuota == null)
                return VolumeQuotaManager.DEFAULT_GROUP_QUOTA;
            else {
                return Long.valueOf(new String(defaultGroupQuota));
            }

        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getDefaultUserQuota() throws DatabaseException {
        try {
            byte[] defaultUserQuota = getXAttr(1, SYSTEM_UID, VOL_DEFAULT_U_QUOTA_ATTR_NAME);
            if (defaultUserQuota == null)
                return VolumeQuotaManager.DEFAULT_USER_QUOTA;
            else {
                return Long.valueOf(new String(defaultUserQuota));
            }

        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public long getGroupQuota(String groupId) throws DatabaseException {
        return getOwnerQuotaInfo(OwnerType.GROUP, QuotaInfo.QUOTA, groupId, QuotaConstants.NO_QUOTA);
    }

    @Override
    public long getGroupBlockedSpace(String groupId) throws DatabaseException {
        return getOwnerQuotaInfo(OwnerType.GROUP, QuotaInfo.BLOCKED, groupId, 0);
    }

    @Override
    public long getGroupUsedSpace(String groupId) throws DatabaseException {
        return getOwnerQuotaInfo(OwnerType.GROUP, QuotaInfo.USED, groupId, 0);
    }

    @Override
    public long getUserQuota(String userId) throws DatabaseException {
        return getOwnerQuotaInfo(OwnerType.USER, QuotaInfo.QUOTA, userId, QuotaConstants.NO_QUOTA);
    }

    @Override
    public long getUserBlockedSpace(String userId) throws DatabaseException {
        return getOwnerQuotaInfo(OwnerType.USER, QuotaInfo.BLOCKED, userId, 0);
    }

    @Override
    public long getUserUsedSpace(String userId) throws DatabaseException {
        return getOwnerQuotaInfo(OwnerType.USER, QuotaInfo.USED, userId, 0);
    }

    private long getOwnerQuotaInfo(OwnerType ownerType, QuotaInfo quotaInfo, String id, long defaultValue)
            throws DatabaseException {
        try {
            byte[] key = BabuDBStorageHelper.createOwnerQuotaInfoKey(ownerType, quotaInfo, id);
            byte[] value = database.lookup(VOLUME_INDEX, key, null).get();

            if (value == null)
                return defaultValue;
            else {
                return Long.valueOf(new String(value));
            }

        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    @Override
    public FileMetadata getMetadata(long fileId) throws DatabaseException {
        
        try {
            
            // create the key for the file ID index lookup
            byte[] key = BabuDBStorageHelper.createFileIdIndexKey(fileId, (byte) -1);
            ByteBuffer.wrap(key).putLong(fileId);
            
            byte[][] valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            
            // retrieve the metadata from the link index
            ResultSet<byte[], byte[]> it = database.prefixLookup(BabuDBStorageManager.FILE_ID_INDEX, key, null)
                    .get();
            
            while (it.hasNext()) {
                
                Entry<byte[], byte[]> curr = it.next();
                
                int type = BabuDBStorageHelper.getType(curr.getKey(), BabuDBStorageManager.FILE_ID_INDEX);
                
                // if the value is a back link, resolve it
                if (type == 3) {
                    
                    long parentId = ByteBuffer.wrap(curr.getValue()).getLong();
                    String fileName = new String(curr.getValue(), 8, curr.getValue().length - 8);
                    
                    return getMetadata(parentId, fileName);
                }
                
                valBufs[type] = curr.getValue();
            }
            
            it.free();
            
            // if not metadata was found for the file ID, return null
            if (valBufs[FileMetadata.RC_METADATA] == null)
                return null;
            
            byte[][] keyBufs = new byte[][] { null, BabuDBStorageHelper.createFileKey(0, "", FileMetadata.RC_METADATA) };
            
            // otherwise, a hard link target is contained in the index; create a
            // new metadata object in this case
            return new BufferBackedFileMetadata(keyBufs, valBufs, BabuDBStorageManager.FILE_ID_INDEX);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
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
            return target == null ? null : new String(target);
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
    public void link(final FileMetadata metadata, final long newParentId, final String newFileName,
            final AtomicDBUpdate update) {
        
        // get the link source
        BufferBackedFileMetadata md = (BufferBackedFileMetadata) metadata;
        
        // increment the link count
        short links = metadata.getLinkCount();
        md.setLinkCount((short) (links + 1));
        
        // insert the whole metadata of the original file in the file ID
        // index
        update.addUpdate(FILE_ID_INDEX,
                BabuDBStorageHelper.createFileIdIndexKey(metadata.getId(), FileMetadata.FC_METADATA),
                md.getFCMetadataValue());
        update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(metadata.getId(),
                FileMetadata.RC_METADATA), md.getRCMetadata().getValue());
        
        // remove the back link
        update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(metadata.getId(), (byte) 3), null);
        
        // if the metadata was retrieved from the FILE_INDEX and hasn't
        // been deleted before (i.e. links == 0), ensure that the original
        // file in the file index now points to the file ID index, and
        // remove the FC and XLoc metadata entries
        if (links != 0 && md.getIndexId() == FILE_INDEX) {
            
            update.addUpdate(FILE_INDEX, md.getRCMetadata().getKey(),
                    BabuDBStorageHelper.createLinkTarget(metadata.getId()));
            update.addUpdate(FILE_INDEX, md.getFCMetadataKey(), null);
        }
        
        // create an entry for the new link to the metadata in the file
        // index
        update.addUpdate(FILE_INDEX,
                BabuDBStorageHelper.createFileKey(newParentId, newFileName, FileMetadata.RC_METADATA),
                BabuDBStorageHelper.createLinkTarget(metadata.getId()));
        
    }
    
    @Override
    public FileMetadata[] resolvePath(final Path path) throws DatabaseException {
        
        try {
            FileMetadata[] md = new FileMetadata[path.getCompCount()];
            
            long parentId = 0;
            for (int i = 0; i < md.length; i++) {
                md[i] = BabuDBStorageHelper.getMetadata(database, parentId, path.getComp(i));
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
    
    @Override
    public void setACLEntry(long fileId, String entity, Short rights, AtomicDBUpdate update) throws DatabaseException {
        
        BufferBackedACLEntry entry = new BufferBackedACLEntry(fileId, entity, rights == null ? 0 : rights);
        update.addUpdate(ACL_INDEX, entry.getKeyBuf(), rights == null ? null : entry.getValBuf());
    }
    
    @Override
    public void setDefaultStripingPolicy(long fileId,
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy defaultSp, AtomicDBUpdate update)
            throws DatabaseException {
        
        setDefaultStripingPolicy(fileId, defaultSp, false, update);
    }
    
    public void setDefaultStripingPolicy(long fileId,
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy defaultSp, boolean init,
            AtomicDBUpdate update) throws DatabaseException {
        
        setXAttr(fileId, SYSTEM_UID, DEFAULT_SP_ATTR_NAME, Converter.stripingPolicyToString(defaultSp).getBytes(),
                init, update);
    }
    
    @Override
    public void setDefaultReplicationPolicy(long fileId, ReplicationPolicy defaultRp, AtomicDBUpdate update)
            throws DatabaseException {
        
        setXAttr(fileId, SYSTEM_UID, DEFAULT_RP_ATTR_NAME, Converter.replicationPolicyToString(defaultRp).getBytes(),
                update);
    }
    
    @Override
    public void setFileVoucherInfo(FileVoucherInfo fileVoucherInfo, AtomicDBUpdate update) throws DatabaseException {

        assert (fileVoucherInfo instanceof BufferBackedFileVoucherInfo);
        BufferBackedFileVoucherInfo bufferBackedInfo = (BufferBackedFileVoucherInfo) fileVoucherInfo;

        update.addUpdate(VOLUME_INDEX, bufferBackedInfo.getKeyBuf(), bufferBackedInfo.getValBuf());

    }

    @Override
    public void setFileVoucherClientInfo(FileVoucherClientInfo fileVoucherClientInfo, AtomicDBUpdate update)
            throws DatabaseException {

        assert (fileVoucherClientInfo instanceof BufferBackedFileVoucherClientInfo);
        BufferBackedFileVoucherClientInfo bufferBackedInfo = (BufferBackedFileVoucherClientInfo) fileVoucherClientInfo;

        update.addUpdate(VOLUME_INDEX, bufferBackedInfo.getKeyBuf(), bufferBackedInfo.getValBuf());
    }

    @Override
    public void setVolumeQuota(long quota, AtomicDBUpdate update) throws DatabaseException {
        setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_QUOTA_ATTR_NAME, String.valueOf(quota).getBytes(), update);
    }

    @Override
    public void setVolumeBlockedSpace(long blockedSpace, AtomicDBUpdate update) throws DatabaseException {
        setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_BSPACE_ATTR_NAME,
                String.valueOf(blockedSpace).getBytes(), update);
    }

    @Override
    public void setVolumeUsedSpace(long usedSpace, AtomicDBUpdate update) throws DatabaseException {
        setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_USPACE_ATTR_NAME, String.valueOf(usedSpace)
                .getBytes(), update);
    }

    @Override
    public void setVoucherSize(long voucherSize, AtomicDBUpdate update) throws DatabaseException {
        setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_VOUCHERSIZE_ATTR_NAME,
                String.valueOf(voucherSize).getBytes(), update);
    }

    @Override
    public void setDefaultGroupQuota(long defaultGroupQuota, AtomicDBUpdate update) throws DatabaseException {
        setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_DEFAULT_G_QUOTA_ATTR_NAME,
                String.valueOf(defaultGroupQuota).getBytes(), update);
    }

    @Override
    public void setDefaultUserQuota(long defaultUserQuota, AtomicDBUpdate update) throws DatabaseException {
        setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_DEFAULT_U_QUOTA_ATTR_NAME,
                String.valueOf(defaultUserQuota).getBytes(), update);
    }

    @Override
    public void setGroupQuota(String groupId, Long quota, AtomicDBUpdate update) throws DatabaseException {

        assert (groupId != null && !groupId.isEmpty());

        setOwnerQuotaInfo(groupId, quota, OwnerType.GROUP, QuotaInfo.QUOTA, update);
    }

    @Override
    public void setGroupBlockedSpace(String groupId, Long blockedSpace, AtomicDBUpdate update) throws DatabaseException {

        assert (groupId != null && !groupId.isEmpty());

        setOwnerQuotaInfo(groupId, blockedSpace, OwnerType.GROUP, QuotaInfo.BLOCKED, update);
    }

    @Override
    public void setGroupUsedSpace(String groupId, Long usedSpace, AtomicDBUpdate update) throws DatabaseException {

        assert (groupId != null && !groupId.isEmpty());

        setOwnerQuotaInfo(groupId, usedSpace, OwnerType.GROUP, QuotaInfo.USED, update);
    }

    @Override
    public void setUserQuota(String userId, Long quota, AtomicDBUpdate update) throws DatabaseException {

        assert (userId != null && !userId.isEmpty());

        setOwnerQuotaInfo(userId, quota, OwnerType.USER, QuotaInfo.QUOTA, update);
    }

    @Override
    public void setUserBlockedSpace(String userId, Long blockedSpace, AtomicDBUpdate update) throws DatabaseException {

        assert (userId != null && !userId.isEmpty());

        setOwnerQuotaInfo(userId, blockedSpace, OwnerType.USER, QuotaInfo.BLOCKED, update);
    }

    @Override
    public void setUserUsedSpace(String userId, Long usedSpace, AtomicDBUpdate update) throws DatabaseException {

        assert (userId != null && !userId.isEmpty());

        setOwnerQuotaInfo(userId, usedSpace, OwnerType.USER, QuotaInfo.USED, update);
    }

    private void setOwnerQuotaInfo(String id, Long value, OwnerType ownerType, QuotaInfo quotaInfo,
            AtomicDBUpdate update) throws DatabaseException {

        byte[] keyBuf = BabuDBStorageHelper.createOwnerQuotaInfoKey(ownerType, quotaInfo, id);
        byte[] valueBuf = value == null ? null : String.valueOf(value).getBytes();

        update.addUpdate(VOLUME_INDEX, keyBuf, valueBuf);
    }

    @Override
    public void setMetadata(FileMetadata metadata, byte type, AtomicDBUpdate update) throws DatabaseException {
        
        assert (metadata instanceof BufferBackedFileMetadata);
        BufferBackedFileMetadata md = (BufferBackedFileMetadata) metadata;
        
        int index = md.getIndexId();
        if (type == -1)
            for (byte i = 0; i < BufferBackedFileMetadata.NUM_BUFFERS; i++) {
                byte[] valBuf = md.getValueBuffer(i);
                assert (valBuf != null);
                update.addUpdate(
                        index,
                        index == FILE_ID_INDEX ? BabuDBStorageHelper.createFileIdIndexKey(metadata.getId(), i) : md
                                .getKeyBuffer(i), valBuf);
            }
        
        else {
            byte[] valBuf = md.getValueBuffer(type);
            assert (valBuf != null);
            update.addUpdate(
                    index,
                    index == FILE_ID_INDEX ? BabuDBStorageHelper.createFileIdIndexKey(metadata.getId(), type) : md
                            .getKeyBuffer(type), valBuf);
        }
    }
    
    @Override
    public void setXAttr(long fileId, String uid, String key, byte[] value, AtomicDBUpdate update)
            throws DatabaseException {
        
        setXAttr(fileId, uid, key, value, false, update);
    }
    
    public void setXAttr(long fileId, String uid, String key, byte[] value, boolean init, AtomicDBUpdate update)
            throws DatabaseException {
        
        try {
            short collNumber = init ? -1 : BabuDBStorageHelper.findXAttrCollisionNumber(database, fileId, uid, key);
            
            BufferBackedXAttr xattr = new BufferBackedXAttr(fileId, uid, key, value, collNumber);
            update.addUpdate(XATTRS_INDEX, xattr.getKeyBuf(), value == null ? null : xattr.getValBuf());
            
            if (key.startsWith(SYS_ATTR_KEY_PREFIX + MRCHelper.POLICY_ATTR_PREFIX))
                notifyAttributeSet(volume.getId(), key, value == null ? null : new String(value));
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public void createSnapshot(String snapName, long parentId, String dirName, boolean recursive)
            throws DatabaseException {
        
        try {
            
            // determine the prefixes for the snapshot
            byte[][][] prefixes = null;
            
            FileMetadata snapDir = getMetadata(parentId, dirName);
            
            // for a full volume snapshot, simply use a 'null' prefix (full:
            // dirID == 1 && recursive)
            if (snapDir.getId() != 1 || !recursive) {
                
                // get the IDs of all files and directories contained in the
                // given directory; if recursive == true, include subdirectories
                List<FileMetadata> nestedFiles = new LinkedList<FileMetadata>();
                BabuDBStorageHelper.getNestedFiles(nestedFiles, database, snapDir.getId(), recursive);
                
                List<byte[]> dirEntryPrefixes = new ArrayList<byte[]>(nestedFiles.size());
                List<byte[]> filePrefixes = new ArrayList<byte[]>(nestedFiles.size());
                
                // include the extended attributes of the volume's root
                // directory if it's not the snapshot directory - they are
                // needed to access volume-wide parameters in the snapshot, such
                // as the access control policy
                if (snapDir.getId() != 1)
                    filePrefixes.add(ByteBuffer.wrap(new byte[8]).putLong(1).array());
                
                // include all metadata of the snapshot (i.e. top level) dir
                byte[] idxKey = BabuDBStorageHelper.createFileKey(parentId, dirName, (byte) -1);
                byte[] fileKey = BabuDBStorageHelper.createFilePrefixKey(snapDir.getId());
                dirEntryPrefixes.add(idxKey);
                filePrefixes.add(fileKey);
                
                // include the snapshot directory content
                idxKey = BabuDBStorageHelper.createFilePrefixKey(snapDir.getId());
                dirEntryPrefixes.add(idxKey);
                
                // determine the key prefixes of all nested files to include and
                // exclude
                for (FileMetadata file : nestedFiles) {
                    
                    // create a prefix key for the nested file
                    byte[] key = BabuDBStorageHelper.createFilePrefixKey(file.getId());
                    
                    // if the nested file is a directory, ...
                    if (file.isDirectory()) {
                        
                        // include the directory in the file prefixes
                        // and the directory prefix in the dir entry prefixes
                        filePrefixes.add(key);
                        dirEntryPrefixes.add(key);
                    }
                    
                    // if the nested file is a file, ...
                    else
                        filePrefixes.add(key);
                }
                
                byte[][] dirEntryPrefixesA = dirEntryPrefixes.toArray(new byte[dirEntryPrefixes.size()][]);
                byte[][] filePrefixesA = filePrefixes.toArray(new byte[filePrefixes.size()][]);
                
                Arrays.sort(dirEntryPrefixesA, DefaultByteRangeComparator.getInstance());
                Arrays.sort(filePrefixesA, DefaultByteRangeComparator.getInstance());
                
                // FILE_INDEX, XATTRS_INDEX, ACL_INDEX, FILE_ID_INDEX,
                // VOLUME_INDEX
                prefixes = new byte[][][] { dirEntryPrefixesA, filePrefixesA, filePrefixesA, filePrefixesA, null };
            }
            
            // create the snapshot
            SnapshotConfig snap = new DefaultSnapshotConfig(snapName, ALL_INDICES, prefixes, null);
            snapMan.createPersistentSnapshot(database.getName(), snap);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public void deleteSnapshot(String snapName) throws DatabaseException {
        
        try {
            
            // check if the snapshot exists; if not, throw an exception
            snapMan.getSnapshotDB(database.getName(), snapName);
            
            // delete the snapshot
            snapMan.deletePersistentSnapshot(database.getName(), snapName);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String[] getAllSnapshots() {
        return snapMan.getAllSnapshots(volume.getId());
    }
    
    public void dump() throws BabuDBException {
        
        System.out.println("FILE_ID_INDEX");
        
        ResultSet<byte[], byte[]> it = database.prefixLookup(FILE_ID_INDEX, new byte[0], null).get();
        while (it.hasNext()) {
            Entry<byte[], byte[]> next = it.next();
            System.out.println(Arrays.toString(next.getKey()) + " = " + Arrays.toString(next.getValue()));
        }
        it.free();
        
        System.out.println("\nFILE_INDEX");
        
        it = database.prefixLookup(FILE_INDEX, new byte[0], null).get();
        while (it.hasNext()) {
            Entry<byte[], byte[]> next = it.next();
            System.out.println(Arrays.toString(next.getKey()) + " = " + Arrays.toString(next.getValue()));
        }
        it.free();
    }
    
    @Override
    public void dumpDB(BufferedWriter xmlWriter) throws DatabaseException, IOException {
        DBAdminHelper.dumpVolume(xmlWriter, this);
    }
    
    protected void updateVolumeSize(long diff, AtomicDBUpdate update) throws DatabaseException {
        
        long newSize = getVolumeSize() + diff;
        
        byte[] sizeBytes = new byte[8];
        ByteBuffer.wrap(sizeBytes).putLong(0, newSize);
        
        update.addUpdate(VOLUME_INDEX, VOL_SIZE_KEY, sizeBytes);
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

    protected void notifyVolumeChange(VolumeInfo vol) {
        for (VolumeChangeListener listener : vcListeners)
            listener.volumeChanged(vol);
    }
    
    protected void notifyVolumeDelete(String volId) {
        for (VolumeChangeListener listener : vcListeners)
            listener.volumeDeleted(volId);
    }
    
    protected void notifyAttributeSet(String volId, String key, String value) {
        for (VolumeChangeListener listener : vcListeners)
            listener.attributeSet(volId, key, value);
    }
    
    private void updateCount(byte[] key, boolean increment, AtomicDBUpdate update) throws DatabaseException {
        
        try {
            byte[] countBytes = BabuDBStorageHelper.getVolumeMetadata(database, key);
            ByteBuffer countBuf = ByteBuffer.wrap(countBytes);
            countBuf.putLong(0, countBuf.getLong() + (increment ? 1 : -1));
            
            update.addUpdate(VOLUME_INDEX, key, countBytes);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    private void initCount(byte[] key, AtomicDBUpdate update) {
        
        byte[] countBytes = new byte[Long.SIZE / 8];
        ByteBuffer countBuf = ByteBuffer.wrap(countBytes);
        countBuf.putLong(0, 1);
        
        update.addUpdate(VOLUME_INDEX, key, countBytes);
    }
}