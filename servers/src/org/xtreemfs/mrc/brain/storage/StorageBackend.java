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

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.DirEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileAttributeEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;

public interface StorageBackend {

    /**
     * Returns a the metadata object of a file with a given file ID.
     *
     * @param fileId
     * @return
     * @throws BackendException
     */
    public AbstractFileEntity getFileById(long fileId) throws BackendException;

    /**
     * Returns all attributes of a given type associated with a file.
     *
     * @param fileId
     * @param attrType
     * @return
     * @throws BackendException
     */
    public List<FileAttributeEntity> getAttrsByFileId(long fileId, long attrType)
        throws BackendException;

    /**
     * Returns all attributes associated with a file.
     *
     * @param fileId
     * @return
     * @throws BackendException
     */
    public List<FileAttributeEntity> getAttrsByFileId(long fileId)
        throws BackendException;

    /**
     * Returns all user-defined attributes associated with a file.
     *
     * @param fileId
     * @param attr
     * @return
     * @throws BackendException
     */
    public String getUserAttrByFileId(long fileId, String attr)
        throws BackendException;

    /**
     * Returns all system internal attributes associated with a file.
     *
     * @param fileId
     * @param attr
     * @return
     * @throws BackendException
     */
    public Object getSystemAttrByFileId(long fileId, String attr)
        throws BackendException;

    /**
     * Returns mapping from names to metadata objects of files nested in the
     * given directory.
     *
     * @param parentId
     * @return
     * @throws BackendException
     */
    public Map<String, FileEntity> getFilesByParent(long parentId)
        throws BackendException;

    /**
     * Returns a mapping from names to metadata objects of directories nested in
     * the given directory.
     *
     * @param parentId
     * @return
     * @throws BackendException
     */
    public Map<String, DirEntity> getDirsByParent(long parentId)
        throws BackendException;

    /**
     * Returns the metadata object of the child with the given name in the given
     * directory.
     *
     * @param name
     * @param parentId
     * @return
     * @throws BackendException
     */
    public AbstractFileEntity getChild(String name, long parentId)
        throws BackendException;

    /**
     * Creates a new link to a metadata object with a given name from within a
     * given directory.
     *
     * @param fileId
     * @param fileName
     * @param parentId
     * @return the new number of links to the file
     * @throws BackendException
     */
    public long link(long fileId, String fileName, long parentId)
        throws BackendException;

    /**
     * Removes a link to a metadata object from a given directory. If no more
     * links exist, the metadata object itself should also be removed.
     *
     * @param fileId
     * @param name
     * @param parentId
     * @return the new number of links to the file
     * @throws BackendException
     */
    public long unlink(long fileId, String name, long parentId)
        throws BackendException;

    /**
     * Removes an attribute associated with a given file from the internal
     * database.
     *
     * @param fileId
     * @param key
     * @throws BackendException
     */
    public void deleteAttribute(long fileId, String key)
        throws BackendException;

    /**
     * Adds a new attribute to the internal database. If an attribute with the
     * given ID already exists, it is replaced.
     *
     * @param data
     * @throws BackendException
     */
    public void put(FileAttributeEntity data) throws BackendException;

    /**
     * Adds a new file metadata object to the internal database. If a file
     * metadata object with the given ID already exists, it is replaced.
     *
     * @param data
     * @throws BackendException
     */
    public void put(FileEntity data) throws BackendException;

    /**
     * Adds a new directory metadata object to the internal database. If a
     * directory metadata object with the given ID already exists, it is
     * replaced.
     *
     * @param data
     * @throws BackendException
     */
    public void put(DirEntity data) throws BackendException;

    /**
     * Writes the in-memory representation of the internal database to disk.
     *
     * @throws BackendException
     */
    public void sync() throws BackendException;

    /**
     * Shuts down the internal database.
     *
     * @throws BackendException
     */
    public void close() throws BackendException;

    /**
     * Removes all data from the internal database and shuts it down.
     */
    public void destroy();

    /**
     * Returns the current size of the database state file.
     *
     * @return the current DB file size
     */
    public long getDBFileSize();

    /**
     * Returns the current number of files stored in the database.
     *
     * @return the current number of files
     */
    public long getNumberOfFiles();

    /**
     * Returns the current number of directories stored in the database.
     *
     * @return the current number of directories
     */
    public long getNumberOfDirs();

    /**
     * Dumps the content currently held in the interal database.
     *
     * @throws BackendException
     */
    public void dumpDB() throws BackendException;

}
