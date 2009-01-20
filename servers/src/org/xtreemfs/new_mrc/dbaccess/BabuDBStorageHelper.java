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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.new_mrc.metadata.BufferBackedRCMetadata;
import org.xtreemfs.new_mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XAttr;

public class BabuDBStorageHelper {
    
    static class ChildrenIterator implements Iterator<FileMetadata> {
        
        private Iterator<Entry<byte[], byte[]>> it;
        
        private Entry<byte[], byte[]>           next;
        
        private Map<Short, byte[][]>            keyMap;
        
        private Map<Short, byte[][]>            valMap;
        
        public ChildrenIterator(Iterator<Entry<byte[], byte[]>> it) {
            this.it = it;
            this.keyMap = new HashMap<Short, byte[][]>();
            this.valMap = new HashMap<Short, byte[][]>();
        }
        
        @Override
        public boolean hasNext() {
            return next != null || !this.keyMap.isEmpty() ? true : it.hasNext();
        }
        
        @Override
        public FileMetadata next() {
            
            int prevHash = -1;
            
            if (keyMap.isEmpty())
                while (it.hasNext()) {
                    
                    if (next == null)
                        next = it.next();
                    
                    final int currFileNameHash = ByteBuffer.wrap(next.getKey(), 8, 4).getInt();
                    if (prevHash != -1 && prevHash != currFileNameHash)
                        break;
                    
                    final byte currType = next.getKey()[12];
                    final short currCollNumber = next.getKey().length <= 13 ? 0 : ByteBuffer.wrap(
                        next.getKey(), 13, 2).getShort();
                    
                    byte[][] keyBufs = keyMap.get(currCollNumber);
                    byte[][] valBufs = valMap.get(currCollNumber);
                    if (keyBufs == null) {
                        
                        keyBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
                        keyMap.put(currCollNumber, keyBufs);
                        
                        valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
                        valMap.put(currCollNumber, valBufs);
                    }
                    
                    keyBufs[currType] = next.getKey();
                    valBufs[currType] = next.getValue();
                    next = null;
                    
                    prevHash = currFileNameHash;
                }
            
            Short key = keyMap.entrySet().iterator().next().getKey();
            byte[][] keyBufs = keyMap.remove(key);
            byte[][] valBufs = valMap.remove(key);
            
            return new BufferBackedFileMetadata(keyBufs, valBufs);
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
    
    public static byte[] getLastAssignedFileId(BabuDB database, String dbName)
        throws BabuDBException {
        
        byte[] bytes = database.syncLookup(dbName, BabuDBStorageManager.LAST_ID_INDEX,
            BabuDBStorageManager.LAST_ID_KEY);
        
        if (bytes == null) {
            bytes = new byte[8];
            ByteBuffer tmp = ByteBuffer.wrap(bytes);
            tmp.putLong(0);
        }
        
        return bytes;
    }
    
    public static List<byte[]>[] getMetadataKeys(BabuDB database, String dbName, long parentId,
        String fileName) throws BabuDBException {
        
        List<byte[]>[] lists = new LinkedList[4];
        
        short collNumber = findFileCollisionNumber(database, dbName, parentId, fileName);
        
        // metadata index keys
        lists[BabuDBStorageManager.FILE_INDEX] = new LinkedList<byte[]>();
        byte[] prefix = BabuDBStorageHelper.createFilePrefixKey(parentId, fileName, (byte) -1);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
            BabuDBStorageManager.FILE_INDEX, prefix);
        
        while (it.hasNext()) {
            byte[] key = it.next().getKey();
            if (getFileCollisionNumber(key) == collNumber)
                lists[BabuDBStorageManager.FILE_INDEX].add(key);
        }
        
        // file ID index key
        lists[BabuDBStorageManager.FILE_ID_INDEX] = new LinkedList<byte[]>();
        long fileId = getId(database, dbName, parentId, fileName);
        byte[] idBytes = new byte[8];
        ByteBuffer.wrap(idBytes).putLong(fileId);
        
        lists[BabuDBStorageManager.FILE_ID_INDEX].add(idBytes);
        
        // ACL index
        lists[BabuDBStorageManager.ACL_INDEX] = new LinkedList<byte[]>();
        it = database.syncPrefixLookup(dbName, BabuDBStorageManager.ACL_INDEX, idBytes);
        
        while (it.hasNext())
            lists[BabuDBStorageManager.ACL_INDEX].add(it.next().getKey());
        
        // XAttrIndex
        lists[BabuDBStorageManager.XATTRS_INDEX] = new LinkedList<byte[]>();
        it = database.syncPrefixLookup(dbName, BabuDBStorageManager.XATTRS_INDEX, idBytes);
        
        while (it.hasNext())
            lists[BabuDBStorageManager.XATTRS_INDEX].add(it.next().getKey());
        
        return lists;
    }
    
    /**
     * Returns the collision number assigned to a file metadata entry, or -1 if
     * the file does not exist.
     * 
     * @param database
     * @param parentId
     * @param fileName
     * @return
     * @throws BabuDBException
     */
    public static short findFileCollisionNumber(BabuDB database, String dbName, long parentId,
        String fileName) throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createFilePrefixKey(parentId, fileName, BufferBackedRCMetadata.TYPE_ID);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
            BabuDBStorageManager.FILE_INDEX, prefix);
        
        Entry<byte[], byte[]> next = null;
        while (it.hasNext()) {
            
            Entry<byte[], byte[]> curr = it.next();
            BufferBackedRCMetadata md = new BufferBackedRCMetadata(curr.getKey(), curr.getValue());
            
            if (md.getFileName().equals(fileName)) {
                next = curr;
                break;
            }
        }
        
        if (next == null)
            return -1;
        
        return getFileCollisionNumber(next.getKey());
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
    public static short findXAttrCollisionNumber(BabuDB database, String dbName, long fileId,
        String owner, String attrKey) throws BabuDBException {
        
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
    public static short findUsedOrNextFreeXAttrCollisionNumber(BabuDB database, String dbName,
        long fileId, String owner, String attrKey) throws BabuDBException {
        
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
    
    /**
     * Returns the next free collision number that can be assigned to a new file
     * metadata entry, or -1 if the file exists already.
     * 
     * @param database
     * @param parentId
     * @param fileName
     * @return
     * @throws BabuDBException
     */
    public static short findNextFreeFileCollisionNumber(BabuDB database, String dbName,
        long parentId, String fileName) throws BabuDBException {
        
        // determine the key prefix
        byte[] prefix = createFilePrefixKey(parentId, fileName, BufferBackedRCMetadata.TYPE_ID);
        
        // perform a prefix lookup
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
            BabuDBStorageManager.FILE_INDEX, prefix);
        
        // find the last entry in which the file name hash is included
        Entry<byte[], byte[]> next = null;
        while (it.hasNext()) {
            
            next = it.next();
            BufferBackedRCMetadata md = new BufferBackedRCMetadata(next.getKey(), next.getValue());
            
            // check whether file exists, i.e.\ the file name stored in the
            // value is the same as the given file name; if so, return -1 to
            // indicate that no free key exists
            if (md.getFileName().equals(fileName))
                return -1;
        }
        
        // if no key w/ the given name hash exists, return 0 to indicate that no
        // collision counter is needed
        if (next == null)
            return 0;
        
        // if the file does not exist, determine the next free collision number
        return (short) (getFileCollisionNumber(next.getKey()) + 1);
    }
    
    public static byte[] createFilePrefixKey(long parentId, String fileName, byte type) {
        
        byte[] prefix = new byte[type >= 0 ? 13 : 12];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(parentId).putInt(fileName.hashCode());
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
    
    public static short getFileCollisionNumber(byte[] key) {
        
        short collNum = 0;
        ByteBuffer tmp = ByteBuffer.wrap(key);
        if (key.length == 13)
            collNum = 0;
        else
            collNum = tmp.getShort(13);
        
        return collNum;
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
    
    public static long getId(BabuDB database, String dbName, long parentId, String fileName)
        throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createFilePrefixKey(parentId, fileName, BufferBackedRCMetadata.TYPE_ID);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(dbName,
            BabuDBStorageManager.FILE_INDEX, prefix);
        
        long id = -1;
        while (it.hasNext()) {
            
            Entry<byte[], byte[]> curr = it.next();
            BufferBackedRCMetadata md = new BufferBackedRCMetadata(curr.getKey(), curr.getValue());
            
            if (md.getFileName().equals(fileName)) {
                id = md.getId();
                break;
            }
        }
        
        return id;
    }
    
    public static byte getType(byte[] key) {
        return key[12];
    }
    
}
