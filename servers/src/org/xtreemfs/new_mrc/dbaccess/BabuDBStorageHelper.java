package org.xtreemfs.new_mrc.dbaccess;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.new_mrc.metadata.BufferBackedRCMetadata;
import org.xtreemfs.new_mrc.metadata.BufferBackedXAttr;

public class BabuDBStorageHelper {
    
    public static byte[] getLastAssignedFileId(BabuDB database) throws BabuDBException {
        
        byte[] bytes = database.syncLookup(BabuDBStorageManager.DB_NAME,
            BabuDBStorageManager.LAST_ID_INDEX, BabuDBStorageManager.LAST_ID_KEY);
        
        if (bytes == null) {
            bytes = new byte[8];
            ByteBuffer tmp = ByteBuffer.wrap(bytes);
            tmp.putLong(0);
        }
        
        return bytes;
    }
    
    public static List<byte[]>[] getMetadataKeys(BabuDB database, long parentId, String fileName)
        throws BabuDBException {
        
        List<byte[]>[] lists = new LinkedList[4];
        
        short collNumber = findFileCollisionNumber(database, parentId, fileName);
        
        // metadata index keys
        lists[BabuDBStorageManager.FILE_INDEX] = new LinkedList<byte[]>();
        byte[] prefix = BabuDBStorageHelper.createFilePrefixKey(parentId, fileName, (byte) -1);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(
            BabuDBStorageManager.DB_NAME, BabuDBStorageManager.FILE_INDEX, prefix);
        
        while (it.hasNext()) {
            byte[] key = it.next().getKey();
            if (getFileCollisionNumber(key) == collNumber)
                lists[BabuDBStorageManager.FILE_INDEX].add(key);
        }
        
        // file ID index key
        lists[BabuDBStorageManager.FILE_ID_INDEX] = new LinkedList<byte[]>();
        long fileId = getId(database, parentId, fileName);
        byte[] idBytes = new byte[8];
        ByteBuffer.wrap(idBytes).putLong(fileId);
        
        lists[BabuDBStorageManager.FILE_ID_INDEX].add(idBytes);
        
        // ACL index
        lists[BabuDBStorageManager.ACL_INDEX] = new LinkedList<byte[]>();
        it = database.syncPrefixLookup(BabuDBStorageManager.DB_NAME,
            BabuDBStorageManager.ACL_INDEX, idBytes);
        
        while (it.hasNext())
            lists[BabuDBStorageManager.ACL_INDEX].add(it.next().getKey());
        
        // XAttrIndex
        lists[BabuDBStorageManager.XATTRS_INDEX] = new LinkedList<byte[]>();
        it = database.syncPrefixLookup(BabuDBStorageManager.DB_NAME,
            BabuDBStorageManager.XATTRS_INDEX, idBytes);
        
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
    public static short findFileCollisionNumber(BabuDB database, long parentId, String fileName)
        throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createFilePrefixKey(parentId, fileName, BufferBackedRCMetadata.TYPE_ID);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(
            BabuDBStorageManager.DB_NAME, BabuDBStorageManager.FILE_INDEX, prefix);
        
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
    public static short findXAttrCollisionNumber(BabuDB database, long fileId, String owner,
        String attrKey) throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createXAttrPrefixKey(fileId, owner, attrKey);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(
            BabuDBStorageManager.DB_NAME, BabuDBStorageManager.XATTRS_INDEX, prefix);
        
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
    public static short findNextFreeFileCollisionNumber(BabuDB database, long parentId,
        String fileName) throws BabuDBException {
        
        // determine the key prefix
        byte[] prefix = createFilePrefixKey(parentId, fileName, BufferBackedRCMetadata.TYPE_ID);
        
        // perform a prefix lookup
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(
            BabuDBStorageManager.DB_NAME, BabuDBStorageManager.FILE_INDEX, prefix);
        
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
        
        byte[] prefix = new byte[16];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(fileId).putInt(owner.hashCode()).putInt(attrKey.hashCode());
        
        return prefix;
    }
    
    public static byte[] createPrefix(long parentId) {
        
        byte[] prefix = new byte[8];
        ByteBuffer buf = ByteBuffer.wrap(prefix);
        buf.putLong(parentId);
        
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
    
    public static long getId(BabuDB database, long parentId, String fileName)
        throws BabuDBException {
        
        // first, determine the collision number
        byte[] prefix = createFilePrefixKey(parentId, fileName, BufferBackedRCMetadata.TYPE_ID);
        Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(
            BabuDBStorageManager.DB_NAME, BabuDBStorageManager.FILE_INDEX, prefix);
        
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
