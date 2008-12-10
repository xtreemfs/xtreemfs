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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.DirEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileAttributeEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;

public class JavaStorageBackend implements StorageBackend {

    private static final String                         DB_FILE_PREFIX = "mrcdb";

    private static final String                         DB_FILE_NAME   = DB_FILE_PREFIX
                                                                           + "."
                                                                           + VersionManagement
                                                                                   .getMrcDataVersion();

    /* file ID (file) -> file */
    private Map<Long, FileEntity>                       fileMap;

    /* file ID (dir) -> dir */
    private Map<Long, DirEntity>                        dirMap;

    /* file ID (dir) -> maps of references to nested files */
    private Map<Long, Map<Object, Object>[]>            fileChildrenMap;

    /* file ID (dir) -> maps of references to nested directories */
    private Map<Long, Map<Object, Object>[]>            dirChildrenMap;

    /* file ID -> (attr name -> attr entity) */
    private Map<Long, Map<String, FileAttributeEntity>> fileAttributeMap;

    private long                                        nextFileId;

    private DirEntity                                   rootDir;

    private File                                        dbDir;

    public JavaStorageBackend(String dbDirectory) throws BackendException {

        dbDir = new File(dbDirectory);
        dbDir.mkdir();
        File dbFile = new File(dbDir, DB_FILE_NAME);

        ObjectInputStream ois = null;

        if (dbFile.exists()) {

            try {
                long startTime = 0;
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "loading database " + dbDirectory
                        + " ...");
                    startTime = System.currentTimeMillis();
                }

                ois = new ObjectInputStream(new FileInputStream(dbFile));
                fileMap = (TreeMap<Long, FileEntity>) ois.readObject();
                dirMap = (TreeMap<Long, DirEntity>) ois.readObject();
                fileChildrenMap = (TreeMap<Long, Map<Object, Object>[]>) ois.readObject();
                dirChildrenMap = (TreeMap<Long, Map<Object, Object>[]>) ois.readObject();
                fileAttributeMap = (TreeMap<Long, Map<String, FileAttributeEntity>>) ois
                        .readObject();
                nextFileId = ois.readLong();
                rootDir = (DirEntity) ois.readObject();

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "loading database '"
                        + dbDirectory + "' finished after "
                        + (System.currentTimeMillis() - startTime) + " ms");

            } catch (Exception exc) {
                throw new BackendException(exc);

            } finally {
                try {
                    if (ois != null)
                        ois.close();
                } catch (IOException exc) {
                    throw new BackendException(exc);
                }
            }

        } else {

            String[] files = dbDir.list();
            if (files != null) {
                for (String file : files) {
                    if (file.startsWith(DB_FILE_PREFIX + ".")) {
                        try {
                            int version = Integer.parseInt(file
                                    .substring(DB_FILE_PREFIX.length() + 1));
                            if (version < VersionManagement.getMrcDataVersion()) {

                                String msg1 = "outdated version of MRC database detected: "
                                    + version + " (current version: "
                                    + VersionManagement.getMrcDataVersion() + ")";
                                String msg2 = "please convert or delete file '"
                                    + new File(dbDir, file).getAbsolutePath() + "' on server";

                                Logging.logMessage(Logging.LEVEL_ERROR, this, msg1);
                                Logging.logMessage(Logging.LEVEL_ERROR, this, msg2);
                                throw new BackendException(msg1 + "\n" + msg2);
                            }
                        } catch (NumberFormatException exc) {
                            // ignore
                        }
                    }
                }
            }

            fileMap = new TreeMap<Long, FileEntity>();
            dirMap = new TreeMap<Long, DirEntity>();
            fileChildrenMap = new TreeMap<Long, Map<Object, Object>[]>();
            dirChildrenMap = new TreeMap<Long, Map<Object, Object>[]>();
            fileAttributeMap = new TreeMap<Long, Map<String, FileAttributeEntity>>();
            nextFileId = 1;

            // create the root element
            long time = TimeSync.getGlobalTime() / 1000;
            rootDir = new DirEntity(1, null, null, time, time, time, null, 1);
        }
    }

    public void deleteAttribute(long fileId, String name) throws BackendException {

        Map<String, FileAttributeEntity> attrMap = fileAttributeMap.get(fileId);
        if (attrMap != null)
            attrMap.remove(name);
    }

    public long link(long fileId, String fileName, long parentId) throws BackendException {

        AbstractFileEntity abstractFile = fileMap.get(fileId);
        if (abstractFile == null)
            abstractFile = dirMap.get(fileId);
        assert (abstractFile != null);

        // first, increment the link count in the metadata object
        abstractFile.setLinkCount(abstractFile.getLinkCount() + 1);

        // then, create a new link in the parent directory

        if (abstractFile instanceof FileEntity) {

            FileEntity file = (FileEntity) abstractFile;

            // retrieve the hashes containing the parent directory children
            Map<Object, Object>[] childHashes = fileChildrenMap.get(parentId);

            // if no file hashes have yet been created for the directory, create
            // one for mapping the file ID and one for mapping the file name to
            // the entity
            if (childHashes == null) {
                childHashes = new HashMap[2];
                childHashes[0] = new HashMap<Object, Object>();
                childHashes[1] = new HashMap<Object, Object>();

                fileChildrenMap.put(parentId, childHashes);
            }

            Object obj = childHashes[0].get(fileId);
            if (obj == null) {
                obj = new Object[] { 1, file };
                childHashes[0].put(fileId, obj);
            } else {
                Object[] objArray = (Object[]) obj;
                objArray[0] = (Integer) objArray[0] + 1;
                objArray[1] = file;
            }

            childHashes[0].put(fileId, obj);
            childHashes[1].put(fileName, file);

        } else {

            DirEntity dir = (DirEntity) abstractFile;

            // retrieve the hashes containing the parent directory children
            Map<Object, Object>[] childHashes = dirChildrenMap.get(parentId);

            // if no directory hashes have yet been created for the directory,
            // one for mapping the directory ID and one for mapping the
            // directory name to the entity
            if (childHashes == null) {
                childHashes = new HashMap[2];
                childHashes[0] = new HashMap<Object, Object>();
                childHashes[1] = new HashMap<Object, Object>();

                dirChildrenMap.put(parentId, childHashes);
            }

            Object obj = childHashes[0].get(fileId);
            if (obj == null) {
                obj = new Object[] { 1, dir };
                childHashes[0].put(fileId, obj);
            } else {
                Object[] objArray = (Object[]) obj;
                objArray[0] = (Integer) objArray[0] + 1;
                objArray[1] = dir;
            }

            childHashes[0].put(fileId, obj);
            childHashes[1].put(fileName, dir);
        }

        return abstractFile.getLinkCount();

    }

    public long unlink(long fileId, String name, long parentId) throws BackendException {

        AbstractFileEntity abstractFile = fileMap.get(fileId);
        if (abstractFile == null)
            abstractFile = dirMap.get(fileId);
        assert (abstractFile != null);

        // find the link and remove it from the children map

        if (abstractFile instanceof FileEntity) {

            // retrieve the hashes containing the parent directory children
            Map<Object, Object>[] childHashes = fileChildrenMap.get(parentId);

            // if an entity is linked to a directory more than once, decrement
            // the directory link counter in the 'by ID' map;
            // otherwise, remove the entry from the 'by ID' map
            Object[] entity1 = (Object[]) childHashes[0].get(fileId);
            if (((Integer) entity1[0]).equals(1))
                childHashes[0].remove(fileId);
            else
                entity1[0] = (Integer) entity1[0] - 1;

            // remove the entry from the 'by name' map
            FileEntity entity2 = (FileEntity) childHashes[1].remove(name);
            assert (entity1 != null) : "file not found in directory table";
            assert (entity1[1] == entity2) : "inconsistent directory entry";

            // remove the file children map entry if child map is empty
            if (childHashes[0].size() == 0)
                fileChildrenMap.remove(parentId);

            // check whether the metadata object can be deleted, as no more
            // links will exist after the unlink operation
            if (abstractFile.getLinkCount() == 1)
                fileMap.remove(fileId);

        } else {

            // retrieve the hashes containing the parent directory children
            Map<Object, Object>[] childHashes = dirChildrenMap.get(parentId);

            // if an entity is linked to a directory more than once, decrement
            // the directory link counter in the 'by ID' map;
            // otherwise, remove the entry from the 'by ID' map
            Object[] entity1 = (Object[]) childHashes[0].get(fileId);
            if (((Integer) entity1[0]).equals(1))
                childHashes[0].remove(fileId);
            else
                entity1[0] = (Integer) entity1[0] - 1;

            // remove the entry from the 'by name' map
            DirEntity entity2 = (DirEntity) childHashes[1].remove(name);
            assert (entity1 != null) : "directory not found in directory table";
            assert (entity1[1] == entity2) : "inconsistent directory entry";

            // remove the dir children map entry if child map is empty
            if (childHashes[0].size() == 0)
                dirChildrenMap.remove(parentId);

            // check whether the metadata object can be deleted, as no more
            // links will exist after the unlink operation
            if (abstractFile.getLinkCount() == 1)
                dirMap.remove(fileId);
        }

        // when the last link to the file has been removed, remove all related
        // metadata from other indices as well
        if (abstractFile.getLinkCount() == 1)
            fileAttributeMap.remove(fileId);

        abstractFile.setLinkCount(abstractFile.getLinkCount() - 1);
        return abstractFile.getLinkCount();
    }

    public List<FileAttributeEntity> getAttrsByFileId(long fileId, long attrType)
        throws BackendException {

        List<FileAttributeEntity> result = new LinkedList<FileAttributeEntity>();

        if (fileAttributeMap.get(fileId) == null)
            return result;

        for (FileAttributeEntity entity : fileAttributeMap.get(fileId).values())
            if (entity.getType() == attrType)
                result.add(new FileAttributeEntity(entity));

        return result;
    }

    public List<FileAttributeEntity> getAttrsByFileId(long fileId) throws BackendException {

        if (fileAttributeMap.get(fileId) == null)
            return new ArrayList<FileAttributeEntity>(0);

        LinkedList<FileAttributeEntity> list = new LinkedList<FileAttributeEntity>();
        for (FileAttributeEntity entity : fileAttributeMap.get(fileId).values())
            list.add(new FileAttributeEntity(entity));

        return list;
    }

    public AbstractFileEntity getChild(String name, long parentId) throws BackendException {

        if (parentId == rootDir.getId() && name.length() == 0)
            return new DirEntity(rootDir);

        Map<Object, Object>[] dirs = dirChildrenMap.get(parentId);
        if (dirs != null) {
            DirEntity dir = (DirEntity) dirs[1].get(name);
            if (dir != null)
                return new DirEntity(dir);
        }

        Map<Object, Object>[] files = fileChildrenMap.get(parentId);
        if (files != null) {
            FileEntity file = (FileEntity) files[1].get(name);
            if (file != null)
                return new FileEntity(file);
        }

        return null;
    }

    public AbstractFileEntity getFileById(long fileId) throws BackendException {

        if (fileId == 1)
            return new DirEntity(rootDir);

        DirEntity dir = dirMap.get(fileId);
        if (dir != null)
            return new DirEntity(dir);

        FileEntity file = fileMap.get(fileId);
        if (file != null)
            return new FileEntity(file);

        return null;
    }

    public Map<String, FileEntity> getFilesByParent(long parentId) throws BackendException {

        Map<String, FileEntity> result = new HashMap<String, FileEntity>();

        Map<Object, Object>[] files = fileChildrenMap.get(parentId);
        if (files != null)
            for (Object fileName : files[1].keySet())
                result.put((String) fileName, new FileEntity((FileEntity) files[1].get(fileName)));

        return result;
    }

    public Map<String, DirEntity> getDirsByParent(long parentId) throws BackendException {

        Map<String, DirEntity> result = new HashMap<String, DirEntity>();

        Map<Object, Object>[] dirs = dirChildrenMap.get(parentId);
        if (dirs != null)
            for (Object dirName : dirs[1].keySet())
                result.put((String) dirName, new DirEntity((DirEntity) dirs[1].get(dirName)));

        return result;
    }

    public String getUserAttrByFileId(long fileId, String attr) {

        Map<String, FileAttributeEntity> attrs = fileAttributeMap.get(fileId);
        if (attrs == null)
            return null;

        FileAttributeEntity ae = attrs.get(attr);

        return ae == null || ae.getType() != FileAttributeEntity.TYPE_USER ? null : ae.getValue()
                .toString();
    }

    public Object getSystemAttrByFileId(long fileId, String attr) throws BackendException {

        // TODO: distinguish between system and user attributes

        Map<String, FileAttributeEntity> attrs = fileAttributeMap.get(fileId);
        if (attrs == null)
            return null;

        FileAttributeEntity ae = attrs.get(attr);

        return ae == null || ae.getType() != FileAttributeEntity.TYPE_SYSTEM ? null : ae.getValue();
    }

    public void put(FileAttributeEntity data) throws BackendException {

        Map<String, FileAttributeEntity> attrs = fileAttributeMap.get(data.getFileId());
        if (attrs == null) {
            attrs = new TreeMap<String, FileAttributeEntity>();
            fileAttributeMap.put(data.getFileId(), attrs);
        }

        attrs.put(data.getKey(), data);
    }

    public void put(FileEntity data) throws BackendException {

        // if no file ID has been assigned to the file yet, assign it now
        if (data.getId() == 0)
            data.setId(++nextFileId);
        else if (data.getId() > nextFileId)
            nextFileId = data.getId();

        FileEntity file = fileMap.get(data.getId());

        // if no file with the given ID exists yet, create a new one and add it
        // to the file map
        if (file == null) {
            file = new FileEntity(data);
            fileMap.put(data.getId(), file);
        }

        // otherwise, replace the content of the existing metadata object with
        // the given content
        else
            file.setContent(data);

    }

    public void put(DirEntity data) throws BackendException {

        // first, check if the entity refers to the root directory; if so
        // directly set it and return
        if (data.getId() == 1) {
            rootDir.setContent(data);
            return;
        }

        // if no file ID has been assigned to the directory yet, assign it now
        if (data.getId() == 0)
            data.setId(++nextFileId);
        else if (data.getId() > nextFileId)
            nextFileId = data.getId();

        DirEntity dir = dirMap.get(data.getId());

        // if no directory with the given ID exists yet, create a new one and
        // add it to the directory map
        if (dir == null) {
            dir = new DirEntity(data);
            dirMap.put(data.getId(), dir);
        }

        // otherwise, replace the content of the existing metadata object with
        // the given content
        else
            dir.setContent(data);

    }

    public void sync() throws BackendException {

        try {

            // flush the entire database state to disk

            dbDir.mkdirs();
            File dbFile = new File(dbDir, DB_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(dbFile);

            ObjectOutput oos = new ObjectOutputStream(fos);
            oos.writeObject(fileMap);
            oos.writeObject(dirMap);
            oos.writeObject(fileChildrenMap);
            oos.writeObject(dirChildrenMap);
            oos.writeObject(fileAttributeMap);
            oos.writeLong(nextFileId);
            oos.writeObject(rootDir);

            fos.getFD().sync();

            oos.close();

        } catch (IOException exc) {
            throw new BackendException(exc);
        }
    }

    public void close() throws BackendException {
    }

    public void destroy() {
        FSUtils.delTree(dbDir);
        fileMap = null;
        dirMap = null;
        fileChildrenMap = null;
        dirChildrenMap = null;
        fileAttributeMap = null;
    }

    public long getDBFileSize() {
        File dbFile = new File(dbDir, DB_FILE_NAME);
        return dbFile.length();
    }

    public long getNumberOfFiles() {
        return fileMap.size();
    }

    public long getNumberOfDirs() {
        return dirMap.size();
    }

    public void dumpDB() throws BackendException {
        System.out.println("files: " + fileChildrenMap);
        System.out.println();
        System.out.println("dirs: " + dirChildrenMap);
    }

}
