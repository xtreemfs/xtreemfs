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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.UserDefinedLookup;
import org.xtreemfs.babudb.lsmdb.LSMLookupInterface;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.new_mrc.dbaccess.BabuDBStorageHelper.ACLIterator;
import org.xtreemfs.new_mrc.dbaccess.BabuDBStorageHelper.ChildrenIterator;
import org.xtreemfs.new_mrc.dbaccess.BabuDBStorageHelper.XAttrIterator;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException.ExceptionType;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.new_mrc.metadata.BufferBackedStripingPolicy;
import org.xtreemfs.new_mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.new_mrc.metadata.BufferBackedXLoc;
import org.xtreemfs.new_mrc.metadata.BufferBackedXLocList;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.StripingPolicy;
import org.xtreemfs.new_mrc.metadata.XAttr;
import org.xtreemfs.new_mrc.metadata.XLoc;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.operations.Path;
import org.xtreemfs.new_mrc.utils.Converter;

public class BabuDBStorageManager implements StorageManager {
    
    public static final int     FILE_INDEX            = 0;
    
    public static final int     XATTRS_INDEX          = 1;
    
    public static final int     ACL_INDEX             = 2;
    
    public static final int     FILE_ID_INDEX         = 3;
    
    public static final int     LAST_ID_INDEX         = 5;
    
    public static final byte[]  LAST_ID_KEY           = { '*' };
    
    private static final String DEFAULT_SP_ATTR_NAME  = "sp";
    
    private static final String LINK_TARGET_ATTR_NAME = "lt";
    
    private BabuDB              database;
    
    private String              dbName;
    
    private String              volumeName;
    
    public BabuDBStorageManager(BabuDB database, String volumeName, String volumeId) {
        
        this.database = database;
        this.dbName = volumeId;
        this.volumeName = volumeName;
        try {
            // first, try to create a new database; if it already exists, an
            // exception will be thrown
            database.createDatabase(dbName, 6);
            
        } catch (BabuDBException e) {
            // database already exists
            Logging.logMessage(Logging.LEVEL_TRACE, this, "database '" + dbName + "' loaded");
        }
    }
    
    @Override
    public AtomicDBUpdate createAtomicDBUpdate(DBAccessResultListener listener, Object context)
        throws DatabaseException {
        try {
            return new AtomicBabuDBUpdate(database, dbName, listener == null ? null
                : new BabuDBRequestListenerWrapper(listener), context);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public ACLEntry createACLEntry(long fileId, String entity, short rights) {
        return new BufferBackedACLEntry(fileId, entity, rights);
    }
    
    @Override
    public XLoc createXLoc(StripingPolicy stripingPolicy, String[] osds) {
        assert (stripingPolicy instanceof BufferBackedStripingPolicy);
        return new BufferBackedXLoc((BufferBackedStripingPolicy) stripingPolicy, osds);
    }
    
    @Override
    public XLocList createXLocList(XLoc[] replicas, int version) {
        BufferBackedXLoc[] tmp = new BufferBackedXLoc[replicas.length];
        for (int i = 0; i < replicas.length; i++)
            tmp[i] = (BufferBackedXLoc) replicas[i];
        return new BufferBackedXLocList(tmp, version);
    }
    
    @Override
    public StripingPolicy createStripingPolicy(String pattern, int stripeSize, int width) {
        return new BufferBackedStripingPolicy(pattern, stripeSize, width);
    }
    
    @Override
    public XAttr createXAttr(long fileId, String owner, String key, String value) {
        return new BufferBackedXAttr(fileId, owner, key, value, (short) 0);
    }
    
    @Override
    public void init(String ownerId, String owningGroupId, short perms, ACLEntry[] acl,
        Map<String, Object> rootDirDefSp, AtomicDBUpdate update) throws DatabaseException {
        
        // create the root directory; the name is the database name
        create(0L, volumeName, ownerId, owningGroupId, rootDirDefSp, perms, null, true, update);
        if (acl != null)
            for (ACLEntry entry : acl)
                setACLEntry(1L, entry.getEntity(), entry.getRights(), update);
    }
    
    @Override
    public FileMetadata create(final long parentId, final String fileName, final String userId,
        final String groupId, final Map<String, Object> stripingPolicy, final short perms,
        final String ref, final boolean directory, final AtomicDBUpdate update)
        throws DatabaseException {
        
        UserDefinedLookup udl = new UserDefinedLookup() {
            
            @Override
            public Object execute(LSMLookupInterface database) throws BabuDBException {
                
                try {
                    
                    // atime, ctime, mtime
                    int time = (int) (TimeSync.getGlobalTime() / 1000);
                    
                    // get the next collision number
                    short collCount = BabuDBStorageHelper.findNextFreeFileCollisionNumber(database,
                        parentId, fileName);
                    
                    // if the file exists already, throw an exception
                    if (collCount == -1)
                        return new DatabaseException(ExceptionType.FILE_EXISTS);
                    
                    // get the file ID assigned to the last created file or
                    // directory
                    byte[] idBytes = BabuDBStorageHelper.getLastAssignedFileId(database);
                    
                    // calculate the new file ID
                    ByteBuffer tmp = ByteBuffer.wrap(idBytes);
                    long id = tmp.getLong(0) + 1;
                    
                    tmp.putLong(0, id);
                    update.addUpdate(LAST_ID_INDEX, LAST_ID_KEY, idBytes);
                    
                    // create metadata
                    BufferBackedFileMetadata fileMetadata = directory ? new BufferBackedFileMetadata(
                        parentId, fileName, userId, groupId, id, time, time, time, perms,
                        (short) 1, collCount)
                        : new BufferBackedFileMetadata(parentId, fileName, userId, groupId, id,
                            time, time, time, 0L, perms, (short) 1, 0, 0, false, collCount);
                    
                    // update main metadata in the file index
                    update.addUpdate(FILE_INDEX, fileMetadata.getFCMetadataKey(), fileMetadata
                            .getFCMetadataValue());
                    update.addUpdate(FILE_INDEX, fileMetadata.getRCMetadata().getKey(),
                        fileMetadata.getRCMetadata().getValue());
                    
                    // add an entry to the file ID index
                    update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(id,
                        (byte) 3), BabuDBStorageHelper.createFileIdIndexValue(parentId, fileName));
                    
                    // create default striping policy (XAttr)
                    if (stripingPolicy != null && stripingPolicy.size() != 0) {
                        
                        BufferBackedXAttr sp = new BufferBackedXAttr(id, SYSTEM_UID,
                            DEFAULT_SP_ATTR_NAME, JSONParser.writeJSON(stripingPolicy), (short) 0);
                        
                        update.addUpdate(XATTRS_INDEX, sp.getKeyBuf(), sp.getValBuf());
                    }
                    
                    // create link target (XAttr)
                    if (ref != null) {
                        
                        BufferBackedXAttr lt = new BufferBackedXAttr(id, SYSTEM_UID,
                            LINK_TARGET_ATTR_NAME, ref, (short) 0);
                        
                        update.addUpdate(XATTRS_INDEX, lt.getKeyBuf(), lt.getValBuf());
                    }
                    
                    return fileMetadata;
                    
                } catch (JSONException exc) {
                    return new DatabaseException(ExceptionType.INTERNAL_DB_ERROR);
                }
            }
        };
        
        try {
            
            Object result = database.syncUserDefinedLookup(dbName, udl);
            if (result instanceof DatabaseException)
                throw (DatabaseException) result;
            
            return (BufferBackedFileMetadata) result;
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public short delete(final long parentId, final String fileName, final AtomicDBUpdate update)
        throws DatabaseException {
        
        UserDefinedLookup udl = new UserDefinedLookup() {
            
            @Override
            public Object execute(LSMLookupInterface database) throws BabuDBException {
                
                // check whether the file refers to a hard link
                BufferBackedFileMetadata file = BabuDBStorageHelper.getMetadata(database, parentId,
                    fileName);
                
                boolean lastLink = file.getLinkCount() == 1;
                
                // decrement the link count
                file.setLinkCount((short) (file.getLinkCount() - 1));
                
                // get all keys to delete
                List<byte[]>[] fileKeys = BabuDBStorageHelper.getKeysToDelete(database, parentId,
                    fileName, lastLink);
                
                // if there are links remaining after the deletion, update
                // the link count
                if (!lastLink)
                    update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(file
                            .getId(), FileMetadata.RC_METADATA), file.getRCMetadata().getValue());
                
                // add delete requests for each key
                for (int i = 0; i < fileKeys.length; i++)
                    if (fileKeys[i] != null)
                        for (byte[] key : fileKeys[i])
                            update.addUpdate(i, key, null);
                
                return file.getLinkCount();
            }
        };
        
        try {
            return (Short) database.syncUserDefinedLookup(dbName, udl);
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public Iterator<ACLEntry> getACL(long fileId) throws DatabaseException {
        
        try {
            
            byte[] prefix = BabuDBStorageHelper.createACLPrefixKey(fileId, null);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName, ACL_INDEX,
                prefix);
            
            return new ACLIterator(it);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public ACLEntry getACLEntry(long fileId, String entity) throws DatabaseException {
        
        try {
            
            byte[] key = BabuDBStorageHelper.createACLPrefixKey(fileId, entity);
            byte[] value = database.syncLookup(dbName, ACL_INDEX, key);
            
            return value == null ? null : new BufferBackedACLEntry(key, value);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public Iterator<FileMetadata> getChildren(long parentId) throws DatabaseException {
        
        try {
            
            byte[] prefix = BabuDBStorageHelper.createFilePrefixKey(parentId);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName, FILE_INDEX,
                prefix);
            
            return new ChildrenIterator(database, dbName, it);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public StripingPolicy getDefaultStripingPolicy(long fileId) throws DatabaseException {
        
        try {
            String spString = getXAttr(fileId, SYSTEM_UID, DEFAULT_SP_ATTR_NAME);
            if (spString == null)
                return null;
            
            Map<String, Object> spMap = (Map<String, Object>) JSONParser.parseJSON(new JSONString(
                spString));
            return Converter.mapToStripingPolicy(this, spMap);
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String getVolumeId() {
        return this.dbName;
    }
    
    @Override
    public String getVolumeName() {
        return this.volumeName;
    }
    
    @Override
    public FileMetadata getMetadata(long fileId) throws DatabaseException {
        
        try {
            
            // create the key for the file ID index lookup
            byte[] key = BabuDBStorageHelper.createFileIdIndexKey(fileId, (byte) -1);
            ByteBuffer.wrap(key).putLong(fileId);
            
            byte[][] valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            
            // retrieve the metadata from the link index
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
                BabuDBStorageManager.FILE_ID_INDEX, key);
            
            while (it.hasNext()) {
                
                Entry<byte[], byte[]> curr = it.next();
                
                int type = BabuDBStorageHelper.getType(curr.getKey(),
                    BabuDBStorageManager.FILE_ID_INDEX);
                
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
            
            // otherwise, a hard link target is contained in the index; create a
            // new metadata object in this case
            return new BufferBackedFileMetadata(null, valBufs, BabuDBStorageManager.FILE_ID_INDEX);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public FileMetadata getMetadata(final long parentId, final String fileName)
        throws DatabaseException {
        
        UserDefinedLookup udl = new UserDefinedLookup() {
            
            @Override
            public Object execute(LSMLookupInterface database) throws BabuDBException {
                return BabuDBStorageHelper.getMetadata(database, parentId, fileName);
            }
        };
        
        try {
            return (FileMetadata) database.syncUserDefinedLookup(dbName, udl);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String getSoftlinkTarget(long fileId) throws DatabaseException {
        
        try {
            return getXAttr(fileId, SYSTEM_UID, LINK_TARGET_ATTR_NAME);
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String getXAttr(long fileId, String uid, String key) throws DatabaseException {
        
        try {
            
            // peform a prefix lookup
            byte[] prefix = BabuDBStorageHelper.createXAttrPrefixKey(fileId, uid, key);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName, XATTRS_INDEX,
                prefix);
            
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
        }
    }
    
    @Override
    public Iterator<XAttr> getXAttrs(long fileId) throws DatabaseException {
        
        try {
            
            // peform a prefix lookup
            byte[] prefix = BabuDBStorageHelper.createXAttrPrefixKey(fileId, null, null);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName, XATTRS_INDEX,
                prefix);
            
            return new XAttrIterator(it, null);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public Iterator<XAttr> getXAttrs(long fileId, String uid) throws DatabaseException {
        
        try {
            
            // peform a prefix lookup
            byte[] prefix = BabuDBStorageHelper.createXAttrPrefixKey(fileId, uid, null);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName, XATTRS_INDEX,
                prefix);
            
            return new XAttrIterator(it, uid);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public void link(final FileMetadata metadata, final long newParentId, final String newFileName,
        final AtomicDBUpdate update) throws DatabaseException {
        
        UserDefinedLookup udl = new UserDefinedLookup() {
            
            @Override
            public Object execute(LSMLookupInterface database) throws BabuDBException {
                
                // get the link source
                BufferBackedFileMetadata md = (BufferBackedFileMetadata) metadata;
                
                // increment the link count
                short links = metadata.getLinkCount();
                md.setLinkCount((short) (links + 1));
                
                // insert the whole metadata of the original file in the file ID
                // index
                update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(metadata
                        .getId(), FileMetadata.FC_METADATA), md.getFCMetadataValue());
                update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(metadata
                        .getId(), FileMetadata.RC_METADATA), md.getRCMetadata().getValue());
                update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(metadata
                        .getId(), FileMetadata.XLOC_METADATA), md.getXLocListValue());
                update.addUpdate(FILE_ID_INDEX, BabuDBStorageHelper.createFileIdIndexKey(metadata
                        .getId(), (byte) 3), null);
                
                // if the metadata was retrieved from the file index and hasn't
                // been
                // deleted before (i.e. links == 0), ensure that the original
                // file
                // in the file index now points to the file ID index, and remove
                // the
                // FC and XLoc metadata entries
                if (links != 0 && md.getIndexId() == FILE_INDEX) {
                    
                    update.addUpdate(FILE_INDEX, md.getRCMetadata().getKey(), BabuDBStorageHelper
                            .createLinkTarget(metadata.getId(), metadata.getFileName()));
                    update.addUpdate(FILE_INDEX, md.getFCMetadataKey(), null);
                    if (md.getXLocListKey() != null)
                        update.addUpdate(FILE_INDEX, md.getXLocListKey(), null);
                }
                
                // create an entry for the new link to the metadata in the file
                // index
                
                // retrieve the next free collision number for the new link
                short collCount = BabuDBStorageHelper.findNextFreeFileCollisionNumber(database,
                    newParentId, newFileName);
                
                // if a file with the same name exists already, replace it
                if (collCount == -1)
                    collCount = BabuDBStorageHelper.findFileCollisionNumber(database, newParentId,
                        newFileName);
                
                update.addUpdate(FILE_INDEX, BabuDBStorageHelper.createFileKey(newParentId,
                    newFileName, FileMetadata.RC_METADATA, collCount), BabuDBStorageHelper
                        .createLinkTarget(metadata.getId(), newFileName));
                
                return null;
            }
            
        };
        
        try {
            database.syncUserDefinedLookup(dbName, udl);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public FileMetadata[] resolvePath(final Path path) throws DatabaseException {
        
        UserDefinedLookup udl = new UserDefinedLookup() {
            
            @Override
            public Object execute(LSMLookupInterface database) throws BabuDBException {
                
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
                
            }
        };
        
        try {
            return (FileMetadata[]) database.syncUserDefinedLookup(dbName, udl);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void setACLEntry(long fileId, String entity, Short rights, AtomicDBUpdate update)
        throws DatabaseException {
        
        BufferBackedACLEntry entry = new BufferBackedACLEntry(fileId, entity, rights == null ? 0
            : rights);
        update.addUpdate(ACL_INDEX, entry.getKeyBuf(), rights == null ? null : entry.getValBuf());
    }
    
    @Override
    public void setDefaultStripingPolicy(long fileId, StripingPolicy defaultSp,
        AtomicDBUpdate update) throws DatabaseException {
        try {
            Map<String, Object> sp = Converter.stripingPolicyToMap(defaultSp);
            setXAttr(fileId, SYSTEM_UID, DEFAULT_SP_ATTR_NAME, JSONParser.writeJSON(sp), update);
        } catch (JSONException exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        }
    }
    
    @Override
    public void setMetadata(FileMetadata metadata, byte type, AtomicDBUpdate update)
        throws DatabaseException {
        
        assert (metadata instanceof BufferBackedFileMetadata);
        BufferBackedFileMetadata md = (BufferBackedFileMetadata) metadata;
        
        int index = md.getIndexId();
        if (type == -1)
            for (byte i = 0; i < BufferBackedFileMetadata.NUM_BUFFERS; i++) {
                update.addUpdate(index, index == FILE_ID_INDEX ? BabuDBStorageHelper
                        .createFileIdIndexKey(metadata.getId(), i) : md.getKeyBuffer(i), md
                        .getValueBuffer(i));
            }
        
        else
            update.addUpdate(index, index == FILE_ID_INDEX ? BabuDBStorageHelper
                    .createFileIdIndexKey(metadata.getId(), type) : md.getKeyBuffer(type), md
                    .getValueBuffer(type));
    }
    
    @Override
    public void setXAttr(long fileId, String uid, String key, String value, AtomicDBUpdate update)
        throws DatabaseException {
        
        try {
            short collNumber = BabuDBStorageHelper.findXAttrCollisionNumber(database, dbName,
                fileId, uid, key);
            
            BufferBackedXAttr xattr = new BufferBackedXAttr(fileId, uid, key, value, collNumber);
            update.addUpdate(XATTRS_INDEX, xattr.getKeyBuf(), value == null ? null : xattr
                    .getValBuf());
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    // public void dump() throws BabuDBException {
    //        
    // System.out.println("FILE_ID_INDEX");
    //        
    // Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
    // FILE_ID_INDEX,
    // new byte[0]);
    // while (it.hasNext()) {
    // Entry<byte[], byte[]> next = it.next();
    // System.out.println(Arrays.toString(next.getKey()) + " = "
    // + Arrays.toString(next.getValue()));
    // }
    //        
    // System.out.println("\nFILE_INDEX");
    //        
    // it = database.syncPrefixLookup(dbName, FILE_INDEX, new byte[0]);
    // while (it.hasNext()) {
    // Entry<byte[], byte[]> next = it.next();
    // System.out.println(Arrays.toString(next.getKey()) + " = "
    // + Arrays.toString(next.getValue()));
    // }
    // }
    
    public void dumpDB(BufferedWriter xmlWriter) throws DatabaseException, IOException {
        BabuDBAdminTool.dumpVolume(xmlWriter, this);
    }
    
}

//
// public long getDBFileSize() {
// return backend.getDBFileSize();
// }
//
// public long getNumberOfFiles() {
// return backend.getNumberOfFiles();
// }
//
// public long getNumberOfDirs() {
// return backend.getNumberOfDirs();
// }
//
//
// public void restoreDBFromDump(String entity, Attributes attrs, RestoreState
// state,
// boolean openTag) throws DatabaseException {
//
// if (entity.equals("dir")) {
//    
// if (openTag) {
//        
// Long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
// String name =
// OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
// String uid = attrs.getValue(attrs.getIndex("uid"));
// String gid = attrs.getValue(attrs.getIndex("gid"));
// long atime = Long.parseLong(attrs.getValue(attrs.getIndex("atime")));
// long ctime = Long.parseLong(attrs.getValue(attrs.getIndex("ctime")));
// long mtime = Long.parseLong(attrs.getValue(attrs.getIndex("mtime")));
//        
// DirEntity dir = new DirEntity(id, uid, gid, atime, ctime, mtime, null, 0);
//        
// createFile(dir, null);
// if (state.parentIds.size() != 0)
// linkFile(name, id, state.parentIds.get(0));
//        
// state.parentIds.add(0, id);
// state.currentEntity = backend.getFileById(id);
// }
//
// else
// state.parentIds.remove(0);
// }
//
// else if (entity.equals("file") && openTag) {
//    
// Long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
// String name =
// OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
//    
// // since files may be linked to multiple directories, create the
// // metadata object only if it does not exist yet
// FileEntity file = (FileEntity) backend.getFileById(id);
// if (file == null) {
//        
// String uid = attrs.getValue(attrs.getIndex("uid"));
// String gid = attrs.getValue(attrs.getIndex("gid"));
// long atime = Long.parseLong(attrs.getValue(attrs.getIndex("atime")));
// long ctime = Long.parseLong(attrs.getValue(attrs.getIndex("ctime")));
// long mtime = Long.parseLong(attrs.getValue(attrs.getIndex("mtime")));
// long size = Long.parseLong(attrs.getValue(attrs.getIndex("size")));
// String writeEpochStr = attrs.getValue(attrs.getIndex("epoch"));
// long writeEpoch = writeEpochStr == null ? 0 : Long.parseLong(writeEpochStr);
// String truncEpochStr = attrs.getValue(attrs.getIndex("issuedEpoch"));
// long truncEpoch = truncEpochStr == null ? 0 : Long.parseLong(truncEpochStr);
//        
// file = new FileEntity(id, uid, gid, atime, ctime, mtime, size, null, null, 0,
// writeEpoch, truncEpoch);
//        
// createFile(file, null);
// }
//    
// linkFile(name, id, state.parentIds.get(0));
//    
// state.currentEntity = backend.getFileById(id);
// }
//
// else if (entity.equals("xlocList") && openTag) {
//    
// long version = Long.parseLong(attrs.getValue(attrs.getIndex("version")));
// ((FileEntity) state.currentEntity).setXLocationsList(new XLocationsList(null,
// version));
// backend.put((FileEntity) state.currentEntity);
// }
//
// else if (entity.equals("xloc") && openTag) {
//    
// String pattern = attrs.getValue(attrs.getIndex("pattern"));
// StringTokenizer st = new StringTokenizer(pattern, " ,");
// String policy = st.nextToken();
// long size = Long.parseLong(st.nextToken());
// long width = Long.parseLong(st.nextToken());
//    
// XLocationsList xLocList = ((FileEntity)
// state.currentEntity).getXLocationsList();
//    
// state.currentXLoc = new XLocation(new StripingPolicy(policy, size, width),
// null);
// xLocList.addReplicaWithoutVersionChange(state.currentXLoc);
// }
//
// else if (entity.equals("osd") && openTag) {
//    
// String osd = attrs.getValue(attrs.getIndex("location"));
//    
// String[] osdList = state.currentXLoc.getOsdList();
// if (osdList == null)
// osdList = new String[] { osd };
// else {
// String[] newOSDList = new String[osdList.length + 1];
// System.arraycopy(osdList, 0, newOSDList, 0, osdList.length);
// newOSDList[newOSDList.length - 1] = osd;
// osdList = newOSDList;
// }
//    
// state.currentXLoc.setOsdList(osdList);
// }
//
// else if (entity.equals("entry") && openTag) {
//    
// String userId = attrs.getValue(attrs.getIndex("entity"));
// long rights = Long.parseLong(attrs.getValue(attrs.getIndex("rights")));
//    
// ACLEntry[] acl = state.currentEntity.getAcl();
// if (acl == null)
// acl = new ACLEntry[] { new ACLEntry(userId, rights) };
// else {
// ACLEntry[] newACL = new ACLEntry[acl.length + 1];
// System.arraycopy(acl, 0, newACL, 0, acl.length);
// newACL[newACL.length - 1] = new ACLEntry(userId, rights);
// acl = newACL;
// }
//    
// state.currentEntity.setAcl(acl);
// if (state.currentEntity instanceof FileEntity)
// backend.put((FileEntity) state.currentEntity);
// else
// backend.put((DirEntity) state.currentEntity);
// }
//
// else if (entity.equals("attr") && openTag) {
//    
// String key =
// OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("key")));
// Object value =
// OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("value")));
//    
// // if the value refers to a striping policy, parse it
// if (key.equals("spol")) {
// StringTokenizer st = new StringTokenizer(value.toString(), ", ");
// value = new StripingPolicy(st.nextToken(), Long.parseLong(st.nextToken()),
// Long
// .parseLong(st.nextToken()));
// } else if (key.equals("ro"))
// value = Boolean.valueOf((String) value);
//    
// long type = Long.parseLong(attrs.getValue(attrs.getIndex("type")));
// String uid = attrs.getValue(attrs.getIndex("uid"));
//    
// backend
// .put(new FileAttributeEntity(key, value, type, state.currentEntity.getId(),
// uid));
// }
// }
//
// public static class RestoreState {
//
// public List<Long> parentIds = new LinkedList<Long>();
//
// public AbstractFileEntity currentEntity;
//
// public XLocation currentXLoc;
//
// }
//
// /**
// *
// * @param fileID
// * @return true, if the file with the given ID exists, false otherwise
// * @throws DatabaseException
// */
// public boolean exists(String fileID) throws DatabaseException {
// try {
// return (getFileEntity(Long.parseLong(fileID)) != null);
// } catch (NumberFormatException e) {
// throw new
// DatabaseException("StorageManager.exists(fileID) : wrong fileID-format");
// }
// }
