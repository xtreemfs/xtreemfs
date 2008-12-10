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

package org.xtreemfs.mrc.brain.storage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.DirEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileAttributeEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;
import org.xtreemfs.mrc.brain.storage.entities.StripingPolicy;
import org.xtreemfs.mrc.brain.storage.entities.XLocation;
import org.xtreemfs.mrc.brain.storage.entities.XLocationsList;
import org.xtreemfs.mrc.utils.Converter;

/**
 * This class is responsible for holding information about file system content.
 * It is used by the default brain implementation in order to manage files and
 * directories.
 * 
 * @author stender
 */
public class StorageManager {
    
    private String         dbDirectory;
    
    private StorageBackend backend;
    
    private SliceID        sliceId;
    
    public StorageManager(String dbDirectory, SliceID sliceId) throws UserException {
        this.dbDirectory = dbDirectory;
        this.sliceId = sliceId;
    }
    
    public void startup() throws BackendException {
        // backend = new DBStorageBackend(dbDirectory, volume.getOwnerId(),
        // volume.getGroupId());
        backend = new JavaStorageBackend(dbDirectory);
    }
    
    public void addXAttributes(long fileId, Map<String, Object> attrs) throws BackendException {
        
        if (attrs != null) {
            
            for (String key : attrs.keySet()) {
                
                FileAttributeEntity<String> data = new FileAttributeEntity<String>(key, String
                        .valueOf(attrs.get(key)), FileAttributeEntity.TYPE_USER, fileId, null);
                
                // add the new key-value pair to the index
                backend.put(data);
            }
        }
    }
    
    public long createFile(String ref, String userId, String groupId,
        Map<String, Object> stripingPolicy, boolean directory, Map<String, Object> aclMap)
        throws BackendException {
        
        long time = TimeSync.getGlobalTime() / 1000;
        
        // convert the access control map to a
        ACLEntry[] acl = Converter.mapToACL(aclMap);
        
        AbstractFileEntity file = null;
        if (directory) {
            DirEntity d = new DirEntity(0, userId, groupId, time, time, time, acl, 0);
            backend.put(d);
            file = d;
        } else {
            FileEntity f = new FileEntity(0, userId, groupId, time, time, time, 0, null, acl, 0, 0,
                0);
            backend.put(f);
            file = f;
        }
        
        if (ref != null)
            backend.put(new FileAttributeEntity<String>("ref", ref,
                FileAttributeEntity.TYPE_SYSTEM, file.getId(), userId));
        
        StripingPolicy spol = stripingPolicy == null ? null : Converter
                .mapToStripingPolicy(stripingPolicy);
        
        if (spol != null)
            backend.put(new FileAttributeEntity<StripingPolicy>("spol", spol,
                FileAttributeEntity.TYPE_SYSTEM, file.getId(), userId));
        
        return file.getId();
    }
    
    public long createFile(AbstractFileEntity file, List<FileAttributeEntity> attrs)
        throws BackendException {
        
        if (file instanceof FileEntity)
            backend.put((FileEntity) file);
        else
            backend.put((DirEntity) file);
        
        if (attrs != null)
            for (FileAttributeEntity attr : attrs) {
                attr.setFileId(file.getId());
                backend.put(attr);
            }
        
        return file.getId();
    }
    
    public long linkFile(String fileName, long fileId, long parentDirId) throws BackendException {
        return backend.link(fileId, fileName, parentDirId);
    }
    
    public long unlinkFile(String fileName, long fileId, long parentDirId) throws BackendException {
        return backend.unlink(fileId, fileName, parentDirId);
    }
    
    public void deleteXAttributes(long fileId, List<Object> attrKeys) throws BackendException {
        
        // if attrKeys == null, delete all attributes
        if (attrKeys == null) {
            
            Collection<FileAttributeEntity> list = backend.getAttrsByFileId(fileId,
                FileAttributeEntity.TYPE_USER);
            
            for (FileAttributeEntity att : list)
                backend.deleteAttribute(att.getFileId(), att.getKey());
        }

        else {
            
            for (Object key : attrKeys) {
                
                Collection<FileAttributeEntity> list = backend.getAttrsByFileId(fileId,
                    FileAttributeEntity.TYPE_USER);
                
                for (FileAttributeEntity att : list) {
                    if (att.getKey().equals(key))
                        backend.deleteAttribute(att.getFileId(), att.getKey());
                }
            }
        }
    }
    
    public Map<String, Object> getXAttributes(long fileId) throws BackendException {
        
        Map<String, Object> map = new HashMap<String, Object>();
        
        Collection<FileAttributeEntity> list = backend.getAttrsByFileId(fileId,
            FileAttributeEntity.TYPE_USER);
        for (FileAttributeEntity att : list)
            map.put(att.getKey(), att.getValue());
        
        return map;
    }
    
    public List<FileAttributeEntity> getAllAttributes(long fileId) throws BackendException {
        return backend.getAttrsByFileId(fileId);
    }
    
    public AbstractFileEntity getFileEntity(String path) throws UserException, BackendException {
        
        AbstractFileEntity file = getFile(backend.getFileById(1), path);
        if (file == null)
            throw new UserException(ErrNo.ENOENT, "could not find file or directory '" + path + "'");
        
        return file;
    }
    
    public AbstractFileEntity getFileEntity(String path, boolean directory)
        throws BackendException, UserException {
        
        AbstractFileEntity file = getFileEntity(path);
        
        if (file.getId() == 1 || file.isDirectory() == directory)
            return file;
        
        throw new UserException(directory ? ErrNo.ENOTDIR : ErrNo.EISDIR, "'" + path
            + "' is not a " + (directory ? "directory" : "file"));
    }
    
    public AbstractFileEntity getFileEntity(long fileId) throws BackendException {
        return backend.getFileById(fileId);
    }
    
    public XLocationsList getXLocationsList(long fileId) throws BackendException, UserException {
        
        AbstractFileEntity file = getFileEntity(fileId);
        if (file == null || !(file instanceof FileEntity))
            throw new UserException(ErrNo.ENOENT, "file does not exist or is a directory");
        
        return ((FileEntity) file).getXLocationsList();
    }
    
    public StripingPolicy getStripingPolicy(long fileId) throws BackendException {
        StripingPolicy sp = (StripingPolicy) backend.getSystemAttrByFileId(fileId, "spol");
        return sp == null ? null : new StripingPolicy(sp.getPolicy(), sp.getStripeSize(), sp
                .getWidth());
    }
    
    public StripingPolicy getVolumeStripingPolicy() throws BackendException {
        StripingPolicy sp = (StripingPolicy) backend.getSystemAttrByFileId(1, "spol");
        return sp == null ? null : new StripingPolicy(sp.getPolicy(), sp.getStripeSize(), sp
                .getWidth());
    }
    
    public boolean isReadOnly(long fileId) throws BackendException {
        Boolean ro = (Boolean) backend.getSystemAttrByFileId(fileId, "ro");
        return ro == null ? false : ro;
    }
    
    public String getFileReference(long fileId) throws BackendException {
        return (String) backend.getSystemAttrByFileId(fileId, "ref");
    }
    
    public boolean hasChildren(long fileId) throws BackendException {
        return !(backend.getFilesByParent(fileId).isEmpty() && backend.getDirsByParent(fileId)
                .isEmpty());
    }
    
    public AbstractFileEntity getChild(String fileName, long parentDirId) throws BackendException {
        return backend.getChild(fileName, parentDirId);
    }
    
    public List<String> getChildren(long fileId) throws BackendException {
        
        List<String> list = new LinkedList<String>();
        
        Map<String, FileEntity> files = backend.getFilesByParent(fileId);
        list.addAll(files.keySet());
        
        Map<String, DirEntity> dirs = backend.getDirsByParent(fileId);
        list.addAll(dirs.keySet());
        
        return list;
    }
    
    public Map<String, AbstractFileEntity> getChildData(long fileId) throws BackendException {
        
        Map<String, AbstractFileEntity> map = new HashMap<String, AbstractFileEntity>();
        
        Map<String, FileEntity> files = backend.getFilesByParent(fileId);
        map.putAll(files);
        
        Map<String, DirEntity> dirs = backend.getDirsByParent(fileId);
        map.putAll(dirs);
        
        return map;
    }
    
    public ACLEntry[] getVolumeACL() throws BackendException {
        AbstractFileEntity file = backend.getFileById(1);
        return file.getAcl();
    }
    
    public boolean fileExists(long parentDir, String file) throws BackendException {
        return backend.getChild(file, parentDir) != null;
    }
    
    public void setFileACL(long fileId, Map<String, Object> acl) throws BackendException {
        setFileACL(fileId, Converter.mapToACL(acl));
    }
    
    public void setFileACL(long fileId, ACLEntry[] acl) throws BackendException {
        
        AbstractFileEntity file = backend.getFileById(fileId);
        file.setAcl(acl);
        
        if (file instanceof FileEntity)
            backend.put((FileEntity) file);
        else
            backend.put((DirEntity) file);
    }
    
    public void setFileSize(long fileId, long fileSize, long epoch, long issuedEpoch)
        throws BackendException {
        
        FileEntity file = (FileEntity) backend.getFileById(fileId);
        file.setSize(fileSize);
        file.setEpoch(epoch);
        file.setIssuedEpoch(issuedEpoch);
        backend.put(file);
    }
    
    public void setFileOwner(long fileId, String owner) throws BackendException {
        
        AbstractFileEntity file = backend.getFileById(fileId);
        file.setUserId(owner);
        
        if (file instanceof FileEntity)
            backend.put((FileEntity) file);
        else
            backend.put((DirEntity) file);
    }
    
    public void setFileGroup(long fileId, String group) throws BackendException {
        
        AbstractFileEntity file = backend.getFileById(fileId);
        file.setGroupId(group);
        
        if (file instanceof FileEntity)
            backend.put((FileEntity) file);
        else
            backend.put((DirEntity) file);
    }
    
    public void setXLocationsList(long fileId, XLocationsList xLocList) throws BackendException {
        
        FileEntity file = (FileEntity) backend.getFileById(fileId);
        file.setXLocationsList(xLocList);
        backend.put(file);
    }
    
    public void setVolumeACL(Map<String, Object> acl) throws BackendException {
        setFileACL(1, acl);
    }
    
    public void setVolumeStripingPolicy(Map<String, Object> stripingPolicy) throws BackendException {
        setStripingPolicy(1, stripingPolicy);
    }
    
    public void setStripingPolicy(long fileId, Map<String, Object> stripingPolicy)
        throws BackendException {
        
        if (stripingPolicy != null)
            backend.put(new FileAttributeEntity<StripingPolicy>("spol", Converter
                    .mapToStripingPolicy(stripingPolicy), FileAttributeEntity.TYPE_SYSTEM, fileId,
                ""));
        else
            backend.deleteAttribute(fileId, "spol");
    }
    
    public void setReadOnly(long fileId, boolean readOnly) throws BackendException {
        backend.put(new FileAttributeEntity<Boolean>("ro", readOnly,
            FileAttributeEntity.TYPE_SYSTEM, fileId, ""));
    }
    
    public List<String> submitQuery(String contextPath, String queryString)
        throws BackendException, UserException {
        
        // TODO
        
        return null;
    }
    
    public void sync() throws BackendException {
        backend.sync();
    }
    
    public void shutdown() throws BackendException {
        backend.close();
        backend = null;
    }
    
    public void cleanup() {
        if (backend != null)
            backend.destroy();
        backend = null;
    }
    
    private AbstractFileEntity getFile(AbstractFileEntity parent, String path)
        throws BackendException, UserException {
        
        if (path.equals(""))
            return parent;
        
        int i = path.indexOf('/');
        String first = i == -1 ? path : path.substring(0, i);
        String remainder = i == -1 ? "" : path.substring(i + 1);
        
        // check if there is a subdirectory with the name of the topmost path
        // component
        AbstractFileEntity child = backend.getChild(first, parent.getId());
        
        if (child == null)
            throw new UserException(ErrNo.ENOENT, "path component '" + first + "' does not exist");
        
        if (!child.isDirectory() && remainder.length() > 0)
            throw new UserException(ErrNo.ENOTDIR, "inner path component '" + first
                + "' is not a directory");
        
        return getFile(child, remainder);
    }
    
    public void updateFileTimes(long fileId, boolean setATime, boolean setCTime, boolean setMTime)
        throws BackendException {
        
        AbstractFileEntity file = backend.getFileById(fileId);
        updateFileTimes(file, setATime, setCTime, setMTime);
    }
    
    private void updateFileTimes(AbstractFileEntity file, boolean setATime, boolean setCTime,
        boolean setMTime) throws BackendException {
        
        long currentTime = TimeSync.getGlobalTime() / 1000;
        
        if (setATime)
            file.setAtime(currentTime);
        if (setCTime)
            file.setCtime(currentTime);
        if (setMTime)
            file.setMtime(currentTime);
        
        if (file instanceof FileEntity)
            backend.put((FileEntity) file);
        else
            backend.put((DirEntity) file);
    }
    
    public SliceID getSliceId() {
        return sliceId;
    }
    
    public long getDBFileSize() {
        return backend.getDBFileSize();
    }
    
    public long getNumberOfFiles() {
        return backend.getNumberOfFiles();
    }
    
    public long getNumberOfDirs() {
        return backend.getNumberOfDirs();
    }
    
    public void dumpDB(BufferedWriter xmlWriter) throws BackendException, IOException {
        
        DirEntity dir = (DirEntity) backend.getFileById(1);
        
        // serialize the root directory
        xmlWriter.write("<dir id=\"" + dir.getId() + "\" name=\"\" uid=\"" + dir.getUserId()
            + "\" gid=\"" + dir.getGroupId() + "\" atime=\"" + dir.getAtime() + "\" ctime=\""
            + dir.getCtime() + "\" mtime=\"" + dir.getMtime() + "\">\n");
        
        // serialize the root directory's ACL
        ACLEntry[] acl = dir.getAcl();
        dumpACL(xmlWriter, acl);
        
        // serialize the root directory's attributes
        List<FileAttributeEntity> attrs = backend.getAttrsByFileId(dir.getId());
        dumpAttrs(xmlWriter, attrs);
        
        dumpDB(xmlWriter, 1);
        xmlWriter.write("</dir>\n");
    }
    
    private void dumpDB(BufferedWriter xmlWriter, long parentId) throws BackendException,
        IOException {
        
        // serialize all directories
        Map<String, DirEntity> dirs = backend.getDirsByParent(parentId);
        for (String name : dirs.keySet()) {
            
            DirEntity dir = dirs.get(name);
            
            // serialize the directory
            xmlWriter.write("<dir id=\"" + dir.getId() + "\" name=\""
                + OutputUtils.escapeToXML(name) + "\" uid=\"" + dir.getUserId() + "\" gid=\""
                + dir.getGroupId() + "\" atime=\"" + dir.getAtime() + "\" ctime=\""
                + dir.getCtime() + "\" mtime=\"" + dir.getMtime() + "\">\n");
            
            // serialize the directory's ACL
            ACLEntry[] acl = dir.getAcl();
            dumpACL(xmlWriter, acl);
            
            // serialize the directory's attributes
            List<FileAttributeEntity> attrs = backend.getAttrsByFileId(dir.getId());
            dumpAttrs(xmlWriter, attrs);
            
            dumpDB(xmlWriter, dir.getId());
            xmlWriter.write("</dir>\n");
        }
        
        // serialize all files
        Map<String, FileEntity> files = backend.getFilesByParent(parentId);
        for (String name : files.keySet()) {
            
            FileEntity file = files.get(name);
            
            // serialize the file
            xmlWriter.write("<file id=\"" + file.getId() + "\" name=\""
                + OutputUtils.escapeToXML(name) + "\" size=\"" + file.getSize() + "\" epoch=\""
                + file.getEpoch() + "\" issuedEpoch=\"" + file.getIssuedEpoch() + "\" uid=\""
                + file.getUserId() + "\" gid=\"" + file.getGroupId() + "\" atime=\""
                + file.getAtime() + "\" ctime=\"" + file.getCtime() + "\" mtime=\""
                + file.getMtime() + "\">\n");
            
            // serialize the file's xLoc list
            XLocationsList xloc = file.getXLocationsList();
            if (xloc != null) {
                xmlWriter.write("<xlocList version=\"" + xloc.getVersion() + "\">\n");
                for (XLocation replica : xloc.getReplicas()) {
                    xmlWriter.write("<xloc pattern=\"" + replica.getStripingPolicy() + "\">\n");
                    for (String osd : replica.getOsdList())
                        xmlWriter.write("<osd location=\"" + osd + "\"/>\n");
                }
                xmlWriter.write("</xloc>\n");
                xmlWriter.write("</xlocList>\n");
            }
            
            // serialize the file's ACL
            ACLEntry[] acl = file.getAcl();
            dumpACL(xmlWriter, acl);
            
            // serialize the file's attributes
            List<FileAttributeEntity> attrs = backend.getAttrsByFileId(file.getId());
            dumpAttrs(xmlWriter, attrs);
            
            xmlWriter.write("</file>\n");
        }
    }
    
    private void dumpAttrs(BufferedWriter xmlWriter, List<FileAttributeEntity> attrs)
        throws IOException {
        
        if (attrs != null && attrs.size() != 0) {
            xmlWriter.write("<attrs>\n");
            for (FileAttributeEntity attr : attrs) {
                
                String key = attr.getKey().toString();
                String val = attr.getValue().toString();
                
                // create byte array for user attributes, as they might
                // contain binary content
                if (attr.getType() != FileAttributeEntity.TYPE_SYSTEM) {
                    key = OutputUtils.byteArrayToHexString(key.getBytes());
                    val = OutputUtils.byteArrayToHexString(val.getBytes());
                }
                
                xmlWriter.write("<attr key=\"" + key + "\" value=\"" + val + "\" type=\""
                    + attr.getType() + "\" uid=\"" + attr.getUserId() + "\"/>\n");
            }
            xmlWriter.write("</attrs>\n");
        }
    }
    
    private void dumpACL(BufferedWriter xmlWriter, ACLEntry[] acl) throws IOException {
        
        if (acl != null && acl.length != 0) {
            xmlWriter.write("<acl>\n");
            for (ACLEntry entry : acl)
                xmlWriter.write("<entry entity=\"" + entry.getEntity() + "\" rights=\""
                    + entry.getRights() + "\"/>\n");
            xmlWriter.write("</acl>\n");
        }
    }
    
    public void restoreDBFromDump(String entity, Attributes attrs, RestoreState state,
        boolean openTag, int dbVersion) throws BackendException {
        
        if (entity.equals("dir")) {
            
            if (openTag) {
                
                Long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
                String name = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
                String uid = attrs.getValue(attrs.getIndex("uid"));
                String gid = attrs.getValue(attrs.getIndex("gid"));
                long atime = Long.parseLong(attrs.getValue(attrs.getIndex("atime")));
                long ctime = Long.parseLong(attrs.getValue(attrs.getIndex("ctime")));
                long mtime = Long.parseLong(attrs.getValue(attrs.getIndex("mtime")));
                
                DirEntity dir = new DirEntity(id, uid, gid, atime, ctime, mtime, null, 0);
                
                createFile(dir, null);
                if (state.parentIds.size() != 0)
                    linkFile(name, id, state.parentIds.get(0));
                
                state.parentIds.add(0, id);
                state.currentEntity = backend.getFileById(id);
            }

            else
                state.parentIds.remove(0);
        }

        else if (entity.equals("file") && openTag) {
            
            Long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
            String name = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
            
            // since files may be linked to multiple directories, create the
            // metadata object only if it does not exist yet
            FileEntity file = (FileEntity) backend.getFileById(id);
            if (file == null) {
                
                String uid = attrs.getValue(attrs.getIndex("uid"));
                String gid = attrs.getValue(attrs.getIndex("gid"));
                long atime = Long.parseLong(attrs.getValue(attrs.getIndex("atime")));
                long ctime = Long.parseLong(attrs.getValue(attrs.getIndex("ctime")));
                long mtime = Long.parseLong(attrs.getValue(attrs.getIndex("mtime")));
                long size = Long.parseLong(attrs.getValue(attrs.getIndex("size")));
                String writeEpochStr = attrs.getValue(attrs.getIndex("epoch"));
                long writeEpoch = writeEpochStr == null ? 0 : Long.parseLong(writeEpochStr);
                String truncEpochStr = attrs.getValue(attrs.getIndex("issuedEpoch"));
                long truncEpoch = truncEpochStr == null ? 0 : Long.parseLong(truncEpochStr);
                
                file = new FileEntity(id, uid, gid, atime, ctime, mtime, size, null, null, 0,
                    writeEpoch, truncEpoch);
                
                createFile(file, null);
            }
            
            linkFile(name, id, state.parentIds.get(0));
            
            state.currentEntity = backend.getFileById(id);
        }

        else if (entity.equals("xlocList") && openTag) {
            
            long version = Long.parseLong(attrs.getValue(attrs.getIndex("version")));
            ((FileEntity) state.currentEntity).setXLocationsList(new XLocationsList(null, version));
            backend.put((FileEntity) state.currentEntity);
        }

        else if (entity.equals("xloc") && openTag) {
            
            String pattern = attrs.getValue(attrs.getIndex("pattern"));
            StringTokenizer st = new StringTokenizer(pattern, " ,");
            String policy = st.nextToken();
            long size = Long.parseLong(st.nextToken());
            long width = Long.parseLong(st.nextToken());
            
            XLocationsList xLocList = ((FileEntity) state.currentEntity).getXLocationsList();
            
            state.currentXLoc = new XLocation(new StripingPolicy(policy, size, width), null);
            xLocList.addReplicaWithoutVersionChange(state.currentXLoc);
        }

        else if (entity.equals("osd") && openTag) {
            
            String osd = attrs.getValue(attrs.getIndex("location"));
            
            String[] osdList = state.currentXLoc.getOsdList();
            if (osdList == null)
                osdList = new String[] { osd };
            else {
                String[] newOSDList = new String[osdList.length + 1];
                System.arraycopy(osdList, 0, newOSDList, 0, osdList.length);
                newOSDList[newOSDList.length - 1] = osd;
                osdList = newOSDList;
            }
            
            state.currentXLoc.setOsdList(osdList);
        }

        else if (entity.equals("entry") && openTag) {
            
            String userId = attrs.getValue(attrs.getIndex("entity"));
            long rights = Long.parseLong(attrs.getValue(attrs.getIndex("rights")));
            
            ACLEntry[] acl = state.currentEntity.getAcl();
            if (acl == null)
                acl = new ACLEntry[] { new ACLEntry(userId, rights) };
            else {
                ACLEntry[] newACL = new ACLEntry[acl.length + 1];
                System.arraycopy(acl, 0, newACL, 0, acl.length);
                newACL[newACL.length - 1] = new ACLEntry(userId, rights);
                acl = newACL;
            }
            
            state.currentEntity.setAcl(acl);
            if (state.currentEntity instanceof FileEntity)
                backend.put((FileEntity) state.currentEntity);
            else
                backend.put((DirEntity) state.currentEntity);
        }

        else if (entity.equals("attr") && openTag) {
            
            long type = Long.parseLong(attrs.getValue(attrs.getIndex("type")));
            String uid = attrs.getValue(attrs.getIndex("uid"));
            
            String key = null;
            Object value = null;
            
            // do not escape system attributes, as they only contain ASCII data
            key = attrs.getValue(attrs.getIndex("key"));
            if (type == FileAttributeEntity.TYPE_SYSTEM) {
                value = attrs.getValue(attrs.getIndex("value"));
            }

            else if (dbVersion < 2) {
                try {
                    key = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("key")));
                    value = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("value")));
                } catch (Exception exc) {
                    Logging.logMessage(Logging.LEVEL_WARN, this, "extended attribute");
                    return;
                }
                
            }

            else {
                key = new String(OutputUtils.hexStringToByteArray(attrs.getValue(attrs
                        .getIndex("key"))));
                value = new String(OutputUtils.hexStringToByteArray(attrs.getValue(attrs
                        .getIndex("value"))));
            }
            
            // if the value refers to a striping policy, parse it
            if (key.equals("spol")) {
                StringTokenizer st = new StringTokenizer(value.toString(), ", ");
                value = new StripingPolicy(st.nextToken(), Long.parseLong(st.nextToken()), Long
                        .parseLong(st.nextToken()));
            } else if (key.equals("ro"))
                value = Boolean.valueOf((String) value);
            
            backend
                    .put(new FileAttributeEntity(key, value, type, state.currentEntity.getId(), uid));
        }
    }
    
    /**
     * VERY EVIL OPERATION!
     */
    public StorageBackend getBackend() {
        return this.backend;
    }
    
    public static class RestoreState {
        
        public List<Long>         parentIds = new LinkedList<Long>();
        
        public AbstractFileEntity currentEntity;
        
        public XLocation          currentXLoc;
        
    }
    
    /**
     * 
     * @param fileID
     * @return true, if the file with the given ID exists, false otherwise
     * @throws BackendException
     */
    public boolean exists(String fileID) throws BackendException {
        try {
            return (getFileEntity(Long.parseLong(fileID)) != null);
        } catch (NumberFormatException e) {
            throw new BackendException("StorageManager.exists(fileID) : wrong fileID-format");
        }
    }
}
