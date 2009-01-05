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

package org.xtreemfs.new_mrc.dbaccess;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBInsertGroup;
import org.xtreemfs.babudb.BabuDBRequestListener;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.brain.metadata.StripingPolicy;
import org.xtreemfs.mrc.brain.metadata.XLoc;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException.ExceptionType;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.new_mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.new_mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XAttr;

public class BabuDBStorageManager implements StorageManager {
    
    static class BabuDBRequestListenerWrapper implements BabuDBRequestListener {
        
        private DBAccessResultListener listener;
        
        public BabuDBRequestListenerWrapper(DBAccessResultListener listener) {
            this.listener = listener;
        }
        
        @Override
        public void insertFinished(Object context) {
            listener.insertFinished(context);
        }
        
        @Override
        public void lookupFinished(Object context, byte[] value) {
            listener.lookupFinished(context, value);
        }
        
        @Override
        public void prefixLookupFinished(Object context, Iterator<Entry<byte[], byte[]>> iterator) {
            listener.prefixLookupFinished(context, iterator);
        }
        
        @Override
        public void requestFailed(Object context, BabuDBException error) {
            listener.requestFailed(context, error);
        }
        
    }
    
    static class ChildrenIterator implements Iterator<FileMetadata> {
        
        private Iterator<Entry<byte[], byte[]>> it;
        
        public ChildrenIterator(Iterator<Entry<byte[], byte[]>> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public FileMetadata next() {
            
//            Entry<byte[], byte[]> next = it.next();
//            
//            FileMetadata metadata = new BufferBackedFileMetadata();
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void remove() {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    public static final String DB_NAME       = "V";
    
    public static final int    FILE_INDEX    = 0;
    
    public static final int    XATTRS_INDEX  = 1;
    
    public static final int    ACL_INDEX     = 2;
    
    public static final int    FILE_ID_INDEX = 3;
    
    public static final int    LAST_ID_INDEX = 4;
    
    public static final byte[] LAST_ID_KEY   = { '*' };
    
    private BabuDB             database;
    
    public BabuDBStorageManager(String dbDir, String dbLogDir) throws DatabaseException {
        try {
            database = new BabuDB(dbDir, dbLogDir, 2, 1024 * 1024 * 16, 5 * 60, false);
            try {
                database.createDatabase(DB_NAME, 5);
            } catch (BabuDBException e) {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "database loaded from '" + dbDir
                    + "'");
                // database already exists
            }
        } catch (BabuDBException e) {
            throw new DatabaseException(e);
        }
        
    }
    
    @Override
    public void shutdown() {
        database.shutdown();
    }
    
    @Override
    public void addReplica(long parentId, String fileName, XLoc replica) throws DatabaseException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public long create(long parentId, String fileName, String userId, String groupId,
        Map<String, Object> stripingPolicy, short perms, Map<String, Short> aclMap, String ref,
        boolean directory, DBAccessResultListener result, Object context) throws DatabaseException {
        
        try {
            
            // atime, ctime, mtime
            int time = (int) (TimeSync.getGlobalTime() / 1000);
            
            BabuDBInsertGroup ig = database.createInsertGroup(DB_NAME);
            
            // get the next collision number
            short collCount = BabuDBStorageHelper.findNextFreeCollisionNumber(database, DB_NAME,
                FILE_INDEX, parentId, fileName);
            
            // if the file exists already, throw an exception
            if (collCount == -1)
                throw new DatabaseException(ExceptionType.FILE_EXISTS);
            
            // get the file ID assigned to the last created file or directory
            byte[] idBytes = BabuDBStorageHelper.getLastAssignedFileId(database);
            
            // calculate the new file ID
            ByteBuffer tmp = ByteBuffer.wrap(idBytes);
            long id = tmp.getLong(0) + 1;
            
            tmp.putLong(0, id);
            ig.addInsert(LAST_ID_INDEX, LAST_ID_KEY, idBytes);
            
            // create metadata
            BufferBackedFileMetadata fileMetadata = directory ? new BufferBackedFileMetadata(
                parentId, fileName, userId, groupId, id, time, time, time, perms, collCount)
                : new BufferBackedFileMetadata(parentId, fileName, userId, groupId, id, time, time,
                    time, 0L, perms, (short) 1, 0, 0, false, collCount);
            ig.addInsert(FILE_INDEX, fileMetadata.getFCMetadataKey(), fileMetadata
                    .getFCMetadataValue());
            ig.addInsert(FILE_INDEX, fileMetadata.getRCMetadata().getKey(), fileMetadata
                    .getRCMetadata().getValue());
            
            ig.addInsert(FILE_ID_INDEX, idBytes, BabuDBStorageHelper.createFileIdIndexValue(
                parentId, fileName));
            
            // create default striping policy (XAttr)
            if (stripingPolicy != null) {
                
                collCount = 0; // TODO: check for collisions
                
                BufferBackedXAttr sp = new BufferBackedXAttr(id, "", "sp", JSONParser
                        .writeJSON(stripingPolicy), collCount);
                
                ig.addInsert(XATTRS_INDEX, sp.getKeyBuf(), sp.getValBuf());
            }
            
            // create link target (XAttr)
            if (ref != null) {
                
                collCount = 0; // TODO: check for collisions
                
                BufferBackedXAttr lt = new BufferBackedXAttr(id, "", "lt", ref, collCount);
                ig.addInsert(XATTRS_INDEX, lt.getKeyBuf(), lt.getValBuf());
            }
            
            // create ACL
            if (aclMap != null) {
                for (Entry<String, Short> entry : aclMap.entrySet()) {
                    
                    BufferBackedACLEntry aclEntry = new BufferBackedACLEntry(id, entry.getKey(),
                        entry.getValue());
                    ig.addInsert(ACL_INDEX, aclEntry.getKeyBuf(), aclEntry.getValBuf());
                }
            }
            
            // insert all data in the database
            database.asyncInsert(ig, new BabuDBRequestListenerWrapper(result), context);
            
            return id;
            
        } catch (DatabaseException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void delete(long parentId, String fileName, DBAccessResultListener result, Object context)
        throws DatabaseException {
        
        try {
            
            // get all keys to delete
            List<byte[]>[] fileKeys = BabuDBStorageHelper.getMetadataKeys(database, parentId,
                fileName);
            
            // add delete requests for each key
            BabuDBInsertGroup ig = database.createInsertGroup(DB_NAME);
            for (int i = 0; i < fileKeys.length; i++)
                for (byte[] key : fileKeys[i])
                    ig.addDelete(i, key);
            
            // execute delete requests
            database.asyncInsert(ig, new BabuDBRequestListenerWrapper(result), context);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void deleteReplica(long parentId, String fileName, int index) throws DatabaseException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Iterator<ACLEntry> getACL(long parentId, String fileName) throws DatabaseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getACLEntry(long parentId, String fileName, String entity) throws DatabaseException {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public Iterator<FileMetadata> getChildren(long parentId, String fileName)
        throws DatabaseException {
        
        try {
            
            byte[] prefix = BabuDBStorageHelper.createPrefix(parentId);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(DB_NAME, FILE_INDEX,
                prefix);
            
            return new ChildrenIterator(it);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    @Override
    public StripingPolicy getDefaultStripingPolicy(long parentId, String fileName)
        throws DatabaseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public FileMetadata getMetadata(long parentId, String fileName) throws DatabaseException {
        
        try {
            short collNumber = BabuDBStorageHelper.findFileCollisionNumber(database, parentId,
                fileName);
            
            // second, determine the value
            byte[] prefix = BabuDBStorageHelper.createFilePrefixKey(parentId, fileName, (byte) -1);
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(DB_NAME, FILE_INDEX,
                prefix);
            
            byte[][] keyBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            byte[][] valBufs = new byte[BufferBackedFileMetadata.NUM_BUFFERS][];
            
            while (it.hasNext()) {
                
                Entry<byte[], byte[]> curr = it.next();
                if (BabuDBStorageHelper.getCollisionNumber(curr.getKey()) == collNumber) {
                    int type = BabuDBStorageHelper.getType(curr.getKey());
                    keyBufs[type] = curr.getKey();
                    valBufs[type] = curr.getValue();
                }
            }
            
            if (keyBufs[0] == null)
                throw new DatabaseException(ExceptionType.NO_SUCH_FILE);
            
            return new BufferBackedFileMetadata(keyBufs, valBufs);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String getXAttr(long parentId, String fileName, String uid, String key)
        throws DatabaseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Iterator<XAttr> getXAttrs(long parentId, String fileName) throws DatabaseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Iterator<XAttr> getXAttrs(long parentId, String fileName, String uid)
        throws DatabaseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public long link(long parentId, String fileName, long newParentId, String newFileName)
        throws DatabaseException {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public long resolvePath(String path) throws DatabaseException {
        return resolvePath(1, path);
    }
    
    @Override
    public long resolvePath(long parentId, String path) throws DatabaseException {
        
        int index = path.indexOf('/');
        
        String nextComponent = index == -1 ? path : path.substring(0, index);
        String remainder = index == -1 ? null : index == path.length() - 1 ? null : path
                .substring(index + 1);
        
        long nextId = nextComponent.length() == 0 ? parentId : resolveComponent(parentId,
            nextComponent);
        if (remainder == null)
            return nextId;
        
        return resolvePath(nextId, remainder);
    }
    
    @Override
    public void setACLEntry(long parentId, String fileName, String entity, Integer rights)
        throws DatabaseException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void setDefaultStripingPolicy(long parentId, String fileName, StripingPolicy defaultSp)
        throws DatabaseException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void setMetadata(long parentId, String fileName, FileMetadata metadata)
        throws DatabaseException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void setXAttr(long parentId, String fileName, String uid, String key, String value)
        throws DatabaseException {
        // TODO Auto-generated method stub
        
    }
    
    private long resolveComponent(long parentId, String comp) throws DatabaseException {
        try {
            return BabuDBStorageHelper.getId(database, parentId, comp);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
}

// public void setXAttribute(long fileId, String key, String value, String uid)
// throws DatabaseException {
// try {
// database.createInsertGroup("V");
// } catch (BabuDBException e) {
// throw new DatabaseException(e);
// }
// }
//
// public long createFile(String ref, String userId, String groupId,
// Map<String, Object> stripingPolicy, boolean directory, Map<String, Object>
// aclMap)
// throws DatabaseException {
//
// long time = TimeSync.getGlobalTime() / 1000;
//
// // convert the access control map to a
// ACLEntry[] acl = Converter.mapToACL(aclMap);
//
// AbstractFileEntity file = null;
// if (directory) {
// DirEntity d = new DirEntity(0, userId, groupId, time, time, time, acl, 0);
// backend.put(d);
// file = d;
// } else {
// FileEntity f = new FileEntity(0, userId, groupId, time, time, time, 0, null,
// acl, 0, 0,
// 0);
// backend.put(f);
// file = f;
// }
//
// if (ref != null)
// backend.put(new FileAttributeEntity<String>("ref", ref,
// FileAttributeEntity.TYPE_SYSTEM, file.getId(), userId));
//
// StripingPolicy spol = stripingPolicy == null ? null : Converter
// .mapToStripingPolicy(stripingPolicy);
//
// if (spol != null)
// backend.put(new FileAttributeEntity<StripingPolicy>("spol", spol,
// FileAttributeEntity.TYPE_SYSTEM, file.getId(), userId));
//
// return file.getId();
// }
//
// public long createFile(AbstractFileEntity file, List<FileAttributeEntity>
// attrs)
// throws DatabaseException {
//
// if (file instanceof FileEntity)
// backend.put((FileEntity) file);
// else
// backend.put((DirEntity) file);
//
// if (attrs != null)
// for (FileAttributeEntity attr : attrs) {
// attr.setFileId(file.getId());
// backend.put(attr);
// }
//
// return file.getId();
// }
//
// public long linkFile(String fileName, long fileId, long parentDirId) throws
// DatabaseException {
// return backend.link(fileId, fileName, parentDirId);
// }
//
// public long unlinkFile(String fileName, long fileId, long parentDirId) throws
// DatabaseException {
// return backend.unlink(fileId, fileName, parentDirId);
// }
//
// public void deleteXAttributes(long fileId, List<Object> attrKeys) throws
// DatabaseException {
//
// // if attrKeys == null, delete all attributes
// if (attrKeys == null) {
//    
// Collection<FileAttributeEntity> list = backend.getAttrsByFileId(fileId,
// FileAttributeEntity.TYPE_USER);
//    
// for (FileAttributeEntity att : list)
// backend.deleteAttribute(att.getFileId(), att.getKey());
// }
//
// else {
//    
// for (Object key : attrKeys) {
//        
// Collection<FileAttributeEntity> list = backend.getAttrsByFileId(fileId,
// FileAttributeEntity.TYPE_USER);
//        
// for (FileAttributeEntity att : list) {
// if (att.getKey().equals(key))
// backend.deleteAttribute(att.getFileId(), att.getKey());
// }
// }
// }
// }
//
// public Iterator<Entry<byte[], byte[]>> getXAttributes(long fileId) throws
// DatabaseException {
//
// }
//
// public List<FileAttributeEntity> getAllAttributes(long fileId) throws
// DatabaseException {
// return backend.getAttrsByFileId(fileId);
// }
//
// public AbstractFileEntity getFileEntity(String path) throws UserException,
// DatabaseException {
//
// AbstractFileEntity file = getFile(backend.getFileById(1), path);
// if (file == null)
// throw new UserException(ErrNo.ENOENT, "could not find file or directory '" +
// path + "'");
//
// return file;
// }
//
// public AbstractFileEntity getFileEntity(String path, boolean directory)
// throws DatabaseException, UserException {
//
// AbstractFileEntity file = getFileEntity(path);
//
// if (file.getId() == 1 || file.isDirectory() == directory)
// return file;
//
// throw new UserException(directory ? ErrNo.ENOTDIR : ErrNo.EISDIR, "'" + path
// + "' is not a " + (directory ? "directory" : "file"));
// }
//
// public AbstractFileEntity getFileEntity(long fileId) throws DatabaseException
// {
// return backend.getFileById(fileId);
// }
//
// public XLocationsList getXLocationsList(long fileId) throws
// DatabaseException, UserException {
//
// AbstractFileEntity file = getFileEntity(fileId);
// if (file == null || !(file instanceof FileEntity))
// throw new UserException(ErrNo.ENOENT,
// "file does not exist or is a directory");
//
// return ((FileEntity) file).getXLocationsList();
// }
//
// public StripingPolicy getStripingPolicy(long fileId) throws DatabaseException
// {
// StripingPolicy sp = (StripingPolicy) backend.getSystemAttrByFileId(fileId,
// "spol");
// return sp == null ? null : new StripingPolicy(sp.getPolicy(),
// sp.getStripeSize(), sp
// .getWidth());
// }
//
// public StripingPolicy getVolumeStripingPolicy() throws DatabaseException {
// StripingPolicy sp = (StripingPolicy) backend.getSystemAttrByFileId(1,
// "spol");
// return sp == null ? null : new StripingPolicy(sp.getPolicy(),
// sp.getStripeSize(), sp
// .getWidth());
// }
//
// public boolean isReadOnly(long fileId) throws DatabaseException {
// Boolean ro = (Boolean) backend.getSystemAttrByFileId(fileId, "ro");
// return ro == null ? false : ro;
// }
//
// public String getFileReference(long fileId) throws DatabaseException {
// return (String) backend.getSystemAttrByFileId(fileId, "ref");
// }
//
// public boolean hasChildren(long fileId) throws DatabaseException {
// return !(backend.getFilesByParent(fileId).isEmpty() &&
// backend.getDirsByParent(fileId)
// .isEmpty());
// }
//
// public AbstractFileEntity getChild(String fileName, long parentDirId) throws
// DatabaseException {
// return backend.getChild(fileName, parentDirId);
// }
//
// public List<String> getChildren(long fileId) throws DatabaseException {
//
// List<String> list = new LinkedList<String>();
//
// Map<String, FileEntity> files = backend.getFilesByParent(fileId);
// list.addAll(files.keySet());
//
// Map<String, DirEntity> dirs = backend.getDirsByParent(fileId);
// list.addAll(dirs.keySet());
//
// return list;
// }
//
// public Map<String, AbstractFileEntity> getChildData(long fileId) throws
// DatabaseException {
//
// Map<String, AbstractFileEntity> map = new HashMap<String,
// AbstractFileEntity>();
//
// Map<String, FileEntity> files = backend.getFilesByParent(fileId);
// map.putAll(files);
//
// Map<String, DirEntity> dirs = backend.getDirsByParent(fileId);
// map.putAll(dirs);
//
// return map;
// }
//
// public ACLEntry[] getVolumeACL() throws DatabaseException {
// AbstractFileEntity file = backend.getFileById(1);
// return file.getAcl();
// }
//
// public boolean fileExists(long parentDir, String file) throws
// DatabaseException {
// return backend.getChild(file, parentDir) != null;
// }
//
// public void setFileACL(long fileId, Map<String, Object> acl) throws
// DatabaseException {
// setFileACL(fileId, Converter.mapToACL(acl));
// }
//
// public void setFileACL(long fileId, ACLEntry[] acl) throws DatabaseException
// {
//
// AbstractFileEntity file = backend.getFileById(fileId);
// file.setAcl(acl);
//
// if (file instanceof FileEntity)
// backend.put((FileEntity) file);
// else
// backend.put((DirEntity) file);
// }
//
// public void setFileSize(long fileId, long fileSize, long epoch, long
// issuedEpoch)
// throws DatabaseException {
//
// FileEntity file = (FileEntity) backend.getFileById(fileId);
// file.setSize(fileSize);
// file.setEpoch(epoch);
// file.setIssuedEpoch(issuedEpoch);
// backend.put(file);
// }
//
// public void setFileOwner(long fileId, String owner) throws DatabaseException
// {
//
// AbstractFileEntity file = backend.getFileById(fileId);
// file.setUserId(owner);
//
// if (file instanceof FileEntity)
// backend.put((FileEntity) file);
// else
// backend.put((DirEntity) file);
// }
//
// public void setFileGroup(long fileId, String group) throws DatabaseException
// {
//
// AbstractFileEntity file = backend.getFileById(fileId);
// file.setGroupId(group);
//
// if (file instanceof FileEntity)
// backend.put((FileEntity) file);
// else
// backend.put((DirEntity) file);
// }
//
// public void setXLocationsList(long fileId, XLocationsList xLocList) throws
// DatabaseException {
//
// FileEntity file = (FileEntity) backend.getFileById(fileId);
// file.setXLocationsList(xLocList);
// backend.put(file);
// }
//
// public void setVolumeACL(Map<String, Object> acl) throws DatabaseException {
// setFileACL(1, acl);
// }
//
// public void setVolumeStripingPolicy(Map<String, Object> stripingPolicy)
// throws DatabaseException {
// setStripingPolicy(1, stripingPolicy);
// }
//
// public void setStripingPolicy(long fileId, Map<String, Object>
// stripingPolicy)
// throws DatabaseException {
//
// if (stripingPolicy != null)
// backend.put(new FileAttributeEntity<StripingPolicy>("spol", Converter
// .mapToStripingPolicy(stripingPolicy), FileAttributeEntity.TYPE_SYSTEM,
// fileId,
// ""));
// else
// backend.deleteAttribute(fileId, "spol");
// }
//
// public void setReadOnly(long fileId, boolean readOnly) throws
// DatabaseException {
// backend.put(new FileAttributeEntity<Boolean>("ro", readOnly,
// FileAttributeEntity.TYPE_SYSTEM, fileId, ""));
// }
//
// public List<String> submitQuery(String contextPath, String queryString)
// throws DatabaseException, UserException {
//
// // TODO
//
// return null;
// }
//
// public void sync() throws DatabaseException {
// backend.sync();
// }
//
// public void shutdown() throws DatabaseException {
// backend.close();
// backend = null;
// }
//
// public void cleanup() {
// if (backend != null)
// backend.destroy();
// backend = null;
// }
//
// private AbstractFileEntity getFile(AbstractFileEntity parent, String path)
// throws DatabaseException, UserException {
//
// if (path.equals(""))
// return parent;
//
// int i = path.indexOf('/');
// String first = i == -1 ? path : path.substring(0, i);
// String remainder = i == -1 ? "" : path.substring(i + 1);
//
// // check if there is a subdirectory with the name of the topmost path
// // component
// AbstractFileEntity child = backend.getChild(first, parent.getId());
//
// if (child == null)
// throw new UserException(ErrNo.ENOENT, "path component '" + first +
// "' does not exist");
//
// if (!child.isDirectory() && remainder.length() > 0)
// throw new UserException(ErrNo.ENOTDIR, "inner path component '" + first
// + "' is not a directory");
//
// return getFile(child, remainder);
// }
//
// public void updateFileTimes(long fileId, boolean setATime, boolean setCTime,
// boolean setMTime)
// throws DatabaseException {
//
// AbstractFileEntity file = backend.getFileById(fileId);
// updateFileTimes(file, setATime, setCTime, setMTime);
// }
//
// private void updateFileTimes(AbstractFileEntity file, boolean setATime,
// boolean setCTime,
// boolean setMTime) throws DatabaseException {
//
// long currentTime = TimeSync.getGlobalTime() / 1000;
//
// if (setATime)
// file.setAtime(currentTime);
// if (setCTime)
// file.setCtime(currentTime);
// if (setMTime)
// file.setMtime(currentTime);
//
// if (file instanceof FileEntity)
// backend.put((FileEntity) file);
// else
// backend.put((DirEntity) file);
// }
//
// public SliceID getSliceId() {
// return sliceId;
// }
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
// public void dumpDB(BufferedWriter xmlWriter) throws DatabaseException,
// IOException {
//
// DirEntity dir = (DirEntity) backend.getFileById(1);
//
// // serialize the root directory
// xmlWriter.write("<dir id=\"" + dir.getId() + "\" name=\"\" uid=\"" +
// dir.getUserId()
// + "\" gid=\"" + dir.getGroupId() + "\" atime=\"" + dir.getAtime() +
// "\" ctime=\""
// + dir.getCtime() + "\" mtime=\"" + dir.getMtime() + "\">\n");
//
// // serialize the root directory's ACL
// ACLEntry[] acl = dir.getAcl();
// dumpACL(xmlWriter, acl);
//
// // serialize the root directory's attributes
// List<FileAttributeEntity> attrs = backend.getAttrsByFileId(dir.getId());
// dumpAttrs(xmlWriter, attrs);
//
// dumpDB(xmlWriter, 1);
// xmlWriter.write("</dir>\n");
// }
//
// private void dumpDB(BufferedWriter xmlWriter, long parentId) throws
// DatabaseException,
// IOException {
//
// // serialize all directories
// Map<String, DirEntity> dirs = backend.getDirsByParent(parentId);
// for (String name : dirs.keySet()) {
//    
// DirEntity dir = dirs.get(name);
//    
// // serialize the directory
// xmlWriter.write("<dir id=\"" + dir.getId() + "\" name=\""
// + OutputUtils.escapeToXML(name) + "\" uid=\"" + dir.getUserId() + "\" gid=\""
// + dir.getGroupId() + "\" atime=\"" + dir.getAtime() + "\" ctime=\""
// + dir.getCtime() + "\" mtime=\"" + dir.getMtime() + "\">\n");
//    
// // serialize the directory's ACL
// ACLEntry[] acl = dir.getAcl();
// dumpACL(xmlWriter, acl);
//    
// // serialize the directory's attributes
// List<FileAttributeEntity> attrs = backend.getAttrsByFileId(dir.getId());
// dumpAttrs(xmlWriter, attrs);
//    
// dumpDB(xmlWriter, dir.getId());
// xmlWriter.write("</dir>\n");
// }
//
// // serialize all files
// Map<String, FileEntity> files = backend.getFilesByParent(parentId);
// for (String name : files.keySet()) {
//    
// FileEntity file = files.get(name);
//    
// // serialize the file
// xmlWriter.write("<file id=\"" + file.getId() + "\" name=\""
// + OutputUtils.escapeToXML(name) + "\" size=\"" + file.getSize() +
// "\" epoch=\""
// + file.getEpoch() + "\" issuedEpoch=\"" + file.getIssuedEpoch() + "\" uid=\""
// + file.getUserId() + "\" gid=\"" + file.getGroupId() + "\" atime=\""
// + file.getAtime() + "\" ctime=\"" + file.getCtime() + "\" mtime=\""
// + file.getMtime() + "\">\n");
//    
// // serialize the file's xLoc list
// XLocationsList xloc = file.getXLocationsList();
// if (xloc != null) {
// xmlWriter.write("<xlocList version=\"" + xloc.getVersion() + "\">\n");
// for (XLocation replica : xloc.getReplicas()) {
// xmlWriter.write("<xloc pattern=\"" + replica.getStripingPolicy() + "\">\n");
// for (String osd : replica.getOsdList())
// xmlWriter.write("<osd location=\"" + osd + "\"/>\n");
// }
// xmlWriter.write("</xloc>\n");
// xmlWriter.write("</xlocList>\n");
// }
//    
// // serialize the file's ACL
// ACLEntry[] acl = file.getAcl();
// dumpACL(xmlWriter, acl);
//    
// // serialize the file's attributes
// List<FileAttributeEntity> attrs = backend.getAttrsByFileId(file.getId());
// dumpAttrs(xmlWriter, attrs);
//    
// xmlWriter.write("</file>\n");
// }
// }
//
// private void dumpAttrs(BufferedWriter xmlWriter, List<FileAttributeEntity>
// attrs)
// throws IOException {
//
// if (attrs != null && attrs.size() != 0) {
// xmlWriter.write("<attrs>\n");
// for (FileAttributeEntity attr : attrs)
// xmlWriter.write("<attr key=\"" + OutputUtils.escapeToXML(attr.getKey())
// + "\" value=\"" + OutputUtils.escapeToXML(attr.getValue().toString())
// + "\" type=\"" + attr.getType() + "\" uid=\"" + attr.getUserId() + "\"/>\n");
// xmlWriter.write("</attrs>\n");
// }
// }
//
// private void dumpACL(BufferedWriter xmlWriter, ACLEntry[] acl) throws
// IOException {
//
// if (acl != null && acl.length != 0) {
// xmlWriter.write("<acl>\n");
// for (ACLEntry entry : acl)
// xmlWriter.write("<entry entity=\"" + entry.getEntity() + "\" rights=\""
// + entry.getRights() + "\"/>\n");
// xmlWriter.write("</acl>\n");
// }
// }
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
// /**
// * VERY EVIL OPERATION!
// */
// public StorageBackend getBackend() {
// return this.backend;
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
