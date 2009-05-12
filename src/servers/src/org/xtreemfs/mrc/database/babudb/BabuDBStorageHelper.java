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
package org.xtreemfs.mrc.database.babudb;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.mrc.metadata.BufferBackedRCMetadata;
import org.xtreemfs.mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XAttr;

public class BabuDBStorageHelper {
    
    static class ChildrenIterator implements Iterator<FileMetadata> {
        
        private final BabuDB                          database;
        
        private final String                          dbName;
        
        private final Iterator<Entry<byte[], byte[]>> it;
        
        private Entry<byte[], byte[]>                 next;
        
        private String                                prevFileName;
        
        private byte[][]                              keyBufs;
        
        private byte[][]                              valBufs;
        
        public ChildrenIterator(BabuDB database, String dbName, Iterator<Entry<byte[], byte[]>> it) {
            
            this.database = database;
            this.dbName = dbName;
            this.it = it;
            
            this.keyBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            this.valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
        }
        
        @Override
        public boolean hasNext() {
            return next != null || it.hasNext();
        }
        
        @Override
        public FileMetadata next() {
            
            while (next != null || it.hasNext()) {
                
                if (next == null)
                    next = it.next();
                
                final String currFileName = new String(next.getKey(), 8, next.getKey().length - 9);
                
                if (prevFileName != null && !prevFileName.equals(currFileName)) {
                    assert (valBufs[FileMetadata.RC_METADATA] != null) : "*** DATABASE CORRUPTED *** incomplete file metadata";
                    break;
                }
                
                final byte currType = next.getKey()[next.getKey().length - 1];
                
                keyBufs[currType] = next.getKey();
                valBufs[currType] = next.getValue();
                next = null;
                
                prevFileName = currFileName;
            }
            
            byte[][] tmpKeys = keyBufs;
            byte[][] tmpVals = valBufs;
            keyBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            prevFileName = null;
            
            // in case of a hardlink ...
            if (tmpVals[FileMetadata.RC_METADATA][0] == 2)
                try {
                    return BabuDBStorageHelper.resolveLink(database, dbName,
                        tmpVals[FileMetadata.RC_METADATA]);
                } catch (BabuDBException exc) {
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.db, this, "could not resolve hard link");
                    return null;
                }
            
            else
                return new BufferBackedFileMetadata(tmpKeys, tmpVals, BabuDBStorageManager.FILE_INDEX);
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    static class XAttrIterator implements Iterator<XAttr> {
        
        private Iterator<Entry<byte[], byte[]>> it;
        
        private String                          owner;
        
        private BufferBackedXAttr               next;
        
        public XAttrIterator(Iterator<Entry<byte[], byte[]>> it, String owner) {
            this.it = it;
            this.owner = owner;
        }
        
        @Override
        public boolean hasNext() {
            
            if (owner == null)
                return it.hasNext();
            
            if (next != null)
                return true;
            
            if (!it.hasNext())
                return false;
            
            while (it.hasNext()) {
                
                Entry<byte[], byte[]> tmp = it.next();
                next = new BufferBackedXAttr(tmp.getKey(), tmp.getValue());
                if (!owner.equals(next.getOwner()))
                    continue;
                
                return true;
            }
            
            return false;
        }
        
        @Override
        public XAttr next() {
            
            if (next != null) {
                XAttr tmp = next;
                next = null;
                return tmp;
            }
            
            for (;;) {
                
                Entry<byte[], byte[]> tmp = it.next();
                
                next = new BufferBackedXAttr(tmp.getKey(), tmp.getValue());
                if (owner != null && !owner.equals(next.getOwner()))
                    continue;
                
                BufferBackedXAttr tmp2 = next;
                next = null;
                return tmp2;
            }
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    static class ACLIterator implements Iterator<ACLEntry> {
        
        private Iterator<Entry<byte[], byte[]>> it;
        
        public ACLIterator(Iterator<Entry<byte[], byte[]>> it) {
            this.it = it;
        }
        
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }
        
        @Override
        public ACLEntry next() {
            Entry<byte[], byte[]> entry = it.next();
            return new BufferBackedACLEntry(entry.getKey(), entry.getValue());
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    public static byte[] getLastAssignedFileId(BabuDB database, String dbName) throws BabuDBException {
        
        byte[] bytes = database.directLookup(dbName, BabuDBStorageManager.LAST_ID_INDEX,
            BabuDBStorageManager.LAST_ID_KEY);
        
        if (bytes == null) {
            bytes = new byte[8];
            ByteBuffer tmp = ByteBuffer.wrap(bytes);
            tmp.putLong(0);
        }
        
        return bytes;
    }
    
    /**
     * Returns the collision number assigned to an extended attribute, -1 if the
     * attribute does not exist.
     * 
     * @param database
     * @param fileId
     * @param owner
     * @param attrKey
     * @return
     * @throws BabuDBException
     */
    public static short findXAttrCollisionNumber(BabuDB database, String dbName, long fileId, String owner,
        String attrKey) throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createXAttrPrefixKey(fileId, owner, attrKey);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
            BabuDBStorageManager.XATTRS_INDEX, prefix);
        
        Entry<byte[], byte[]> next = null;
        while (it.hasNext()) {
            
            Entry<byte[], byte[]> curr = it.next();
            BufferBackedXAttr attr = new BufferBackedXAttr(curr.getKey(), curr.getValue());
            
            if (owner.equals(attr.getOwner()) && attrKey.equals(attr.getKey())) {
                next = curr;
                break;
            }
        }
        
        if (next == null)
            return -1;
        
        return getXAttrCollisionNumber(next.getKey());
    }
    
    /**
     * Returns the collision number assigned to an extended attribute, or the
     * next largest unused number if the attribute does not exist.
     * 
     * @param database
     * @param fileId
     * @param owner
     * @param attrKey
     * @return
     * @throws BabuDBException
     */
    public static short findUsedOrNextFreeXAttrCollisionNumber(BabuDB database, String dbName, long fileId,
        String owner, String attrKey) throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createXAttrPrefixKey(fileId, owner, attrKey);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
            BabuDBStorageManager.XATTRS_INDEX, prefix);
        
        Entry<byte[], byte[]> next = null;
        Entry<byte[], byte[]> curr = null;
        while (it.hasNext()) {
            
            curr = it.next();
            BufferBackedXAttr attr = new BufferBackedXAttr(curr.getKey(), curr.getValue());
            
            if (owner.equals(attr.getOwner()) && attrKey.equals(attr.getKey())) {
                next = curr;
                break;
            }
        }
        
        if (next == null)
            return curr == null ? 0 : (short) (getXAttrCollisionNumber(curr.getKey()) + 1);
        
        return getXAttrCollisionNumber(next.getKey());
    }
    
    public static byte[] createFileKey(long parentId, String fileName, byte type) {
        
        byte[] bytes = fileName.getBytes();
        
        byte[] prefix = new byte[(type >= 0 ? 9 : 8) + bytes.length];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(parentId).put(bytes);
        if (type >= 0)
            buf.put(type);
        
        return prefix;
    }
    
    public static byte[] createXAttrPrefixKey(long fileId, String owner, String attrKey) {
        
        byte[] prefix = new byte[owner == null ? 8 : attrKey == null ? 12 : 16];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(fileId);
        if (owner != null)
            buf.putInt(owner.hashCode());
        if (attrKey != null)
            buf.putInt(attrKey.hashCode());
        
        return prefix;
    }
    
    public static byte[] createFilePrefixKey(long parentId) {
        
        byte[] prefix = new byte[8];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(parentId);
        
        return prefix;
    }
    
    public static byte[] createACLPrefixKey(long fileId, String entityName) {
        
        byte[] entityBytes = entityName == null ? new byte[0] : entityName.getBytes();
        byte[] prefix = new byte[8 + entityBytes.length];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(fileId).put(entityBytes);
        
        return prefix;
    }
    
    public static byte[] createFileIdIndexValue(long parentId, String fileName) {
        
        byte[] nameBytes = fileName.getBytes();
        
        byte[] buf = new byte[8 + nameBytes.length];
        ByteBuffer tmp = ByteBuffer.wrap(buf);
        tmp.putLong(parentId).put(nameBytes);
        
        return buf;
    }
    
    public static byte[] createFileIdIndexKey(long fileId, byte type) {
        
        byte[] buf = new byte[type == -1 ? 8 : 9];
        ByteBuffer tmp = ByteBuffer.wrap(buf);
        tmp.putLong(fileId);
        if (type != -1)
            tmp.put(type);
        
        return buf;
    }
    
    public static byte[] createLinkTarget(long fileId) {
                
        byte[] buf = new byte[9];
        ByteBuffer tmp = ByteBuffer.wrap(buf);
        tmp.put((byte) 2).putLong(fileId);
        
        return buf;
    }
    
    public static short getXAttrCollisionNumber(byte[] key) {
        
        short collNum = 0;
        ByteBuffer tmp = ByteBuffer.wrap(key);
        if (key.length == 16)
            collNum = 0;
        else
            collNum = tmp.getShort(16);
        
        return collNum;
    }
    
    public static BufferBackedFileMetadata getMetadata(BabuDB database, String dbName, long parentId,
        String fileName) throws BabuDBException {
        
        byte[] rcKey = BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.RC_METADATA);
        byte[] rcValue = database.directLookup(dbName, BabuDBStorageManager.FILE_INDEX, rcKey);
        
        if (rcValue != null) {
            
            // if the value refers to a link, resolve the link
            if (rcValue[0] == 2)
                return resolveLink(database, dbName, rcValue);
            
            byte[][] keyBufs = new byte[][] {
                BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.FC_METADATA), rcKey,
                BabuDBStorageHelper.createFileKey(parentId, fileName, FileMetadata.XLOC_METADATA) };
            byte[][] valBufs = new byte[][] {
                database.directLookup(dbName, BabuDBStorageManager.FILE_INDEX, keyBufs[0]), rcValue,
                database.directLookup(dbName, BabuDBStorageManager.FILE_INDEX, keyBufs[2]) };
            
            return new BufferBackedFileMetadata(keyBufs, valBufs, BabuDBStorageManager.FILE_INDEX);
        }

        else
            return null;
        
    }
    
    public static long getId(BabuDB database, String dbName, long parentId, String fileName, Boolean directory)
        throws BabuDBException {
        
        byte[] key = createFileKey(parentId, fileName, BufferBackedFileMetadata.RC_METADATA);
        byte[] value = database.directLookup(dbName, BabuDBStorageManager.FILE_INDEX, key);
        
        if (value == null)
            return -1;
        
        return ByteBuffer.wrap(value).getLong(1);
    }
    
    public static byte getType(byte[] key, int index) {
        return key[index == BabuDBStorageManager.FILE_ID_INDEX ? 8 : 12];
    }
    
    public static BufferBackedFileMetadata resolveLink(BabuDB database, String dbName, byte[] target)
        throws BabuDBException {
        
        // determine the key for the link index
        byte[] fileIdBytes = new byte[8];
        System.arraycopy(target, 1, fileIdBytes, 0, fileIdBytes.length);
        
        String fileName = new String(target, 9, target.length - 9);
        byte[][] valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
        
        // retrieve the metadata from the link index
        Iterator<Entry<byte[], byte[]>> it = database.directPrefixLookup(dbName,
            BabuDBStorageManager.FILE_ID_INDEX, fileIdBytes);
        
        while (it.hasNext()) {
            
            Entry<byte[], byte[]> curr = it.next();
            
            int type = getType(curr.getKey(), BabuDBStorageManager.FILE_ID_INDEX);
            if (type == 3) {
                long fileId = ByteBuffer.wrap(fileIdBytes).getLong();
                Logging.logMessage(Logging.LEVEL_WARN, Category.db, (Object) null,
                    "MRC database contains redundant data for file %d", fileId);
                continue;
            }
            valBufs[type] = curr.getValue();
        }
        
        if (valBufs[FileMetadata.RC_METADATA] == null)
            return null;
        
        // replace the file name with the link name
        BufferBackedRCMetadata tmp = new BufferBackedRCMetadata(null, valBufs[FileMetadata.RC_METADATA]);
        BufferBackedRCMetadata tmp2 = tmp.isDirectory() ? new BufferBackedRCMetadata(0, fileName, tmp
                .getOwnerId(), tmp.getOwningGroupId(), tmp.getId(), tmp.getPerms(), tmp.getW32Attrs(), tmp
                .getLinkCount()) : new BufferBackedRCMetadata(0, fileName, tmp.getOwnerId(), tmp
                .getOwningGroupId(), tmp.getId(), tmp.getPerms(), tmp.getW32Attrs(), tmp.getLinkCount(), tmp
                .getEpoch(), tmp.getIssuedEpoch(), tmp.isReadOnly());
        valBufs[FileMetadata.RC_METADATA] = tmp2.getValue();
        byte[][] keyBufs = new byte[][] { null, tmp2.getKey(), null };
        
        return new BufferBackedFileMetadata(keyBufs, valBufs, BabuDBStorageManager.FILE_ID_INDEX);
    }
    
}
