/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Christian Lorenz,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;

/**
 * Abstracts object data access from underlying on-disk or in-memory storage layout. <br>
 * Handles the metadata caching which is common to every StorageLayout.
 **/
public abstract class StorageLayout {

    /**
     * Pass this constant to {@link #readObject()} instead of an actual length to read the the full object (all
     * available data).
     */
    public static final int       FULL_OBJECT_LENGTH = -1;

    /**
     * Pass this constant to {@link #deleteObject()} instead of an actual version to delete the latest version.
     */
    public static final int       LATEST_VERSION     = -1;

    /**
     * File metadata cache, which is used in other stages (see {@link OSDRequestDispatcher#OSDRequestDispatcher()}).
     */
    protected final MetadataCache cache;

    /**
     * Initialize the MetadataCache.
     * 
     * @param cache
     */
    protected StorageLayout(MetadataCache cache) {
        this.cache = cache;
    }

    /**
     * Returns cached file metadata, or loads and caches it if it is not cached.
     * 
     * @param fileId
     *            the file ID
     * @param sp
     *            the striping policy assigned to the file
     * @return a {@link FileMetadata} object comprising all metadata associated with the file
     * @throws IOException
     *             if an error occurred while trying to read the metadata
     */
    public FileMetadata getFileMetadata(final StripingPolicyImpl sp, final String fileId) throws IOException {
        // Try to retrieve metadata from cache.
        FileMetadata fi = cache.getFileInfo(fileId);

        // If metadata is not cached ...
        if (fi == null) {
            // ... load metadata from disk
            fi = loadFileMetadata(fileId, sp);

            // ... cache metadata to speed up further accesses.
            cache.setFileInfo(fileId, fi);
        }

        return fi;
    }

    /**
     * Returns cached file metadata, or loads it if it is not cached.
     * 
     * @param fileId
     *            the file ID
     * @param sp
     *            the striping policy assigned to the file
     * @return a {@link FileMetadata} object comprising all metadata associated with the file
     * @throws IOException
     *             if an error occurred while trying to read the metadata
     */
    public FileMetadata getFileMetadataNoCaching(final StripingPolicyImpl sp, final String fileId) throws IOException {
        // Try to retrieve metadata from cache
        FileMetadata fi = cache.getFileInfo(fileId);

        // if metadata is not cached, load it
        if (fi == null) {
            fi = loadFileMetadata(fileId, sp);
        }

        return fi;
    }

    /**
     * Loads all metadata associated with a file on the OSD from the storage device. Amongst others, such metadata may
     * comprise object version numbers and checksums.
     * 
     * @param fileId
     *            the file ID
     * @param sp
     *            the striping policy assigned to the file
     * @return a {@link FileMetadata} object comprising all metadata associated with the file
     * @throws IOException
     *             if an error occurred while trying to read the metadata
     */
    protected abstract FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException;

    /**
     * must be called when a file is closed
     * 
     * @param metadata
     */
    public abstract void closeFile(FileMetadata metadata);

    /**
     * Reads a complete object from the storage device.
     * 
     * @param fileId
     *            fileId of the object
     * @param objNo
     *            object number
     * @param version
     *            version to be read
     * @param checksum
     *            the checksum currently stored with the object
     * @param sp
     *            the striping policy assigned to the file
     * @param osdNumber
     *            the number of the OSD assigned to the object
     * @throws java.io.IOException
     *             when the object cannot be read
     * @return a buffer containing the object, or a <code>null</code> if the object does not exist
     */

    public abstract ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length,
            long version) throws IOException;

    /**
     * Writes a partial object to the storage device.
     * 
     * @param fileId
     *            the file Id the object belongs to
     * @param objNo
     *            object number
     * @param data
     *            buffer with the data to be written
     * @param version
     *            the version to be written
     * @param offset
     *            the relative offset in the object at which to write the buffer
     * @param currentChecksum
     *            the checksum currently assigned to the object; if OSD checksums are disabled, <code>null</code> can be
     *            used
     * @param sp
     *            the striping policy assigned to the file
     * @param osdNumber
     *            the number of the OSD responsible for the object
     * @throws java.io.IOException
     *             when the object cannot be written
     */
    public abstract void writeObject(String fileId, FileMetadata md, ReusableBuffer data, long objNo, int offset,
            long newVersion, boolean sync, boolean cow) throws IOException;

    /**
     * Truncates an object on the storage device.
     * 
     * @param fileId
     * @param md
     * @param objNo
     * @param newLength
     * @param cow
     * @throws IOException
     */
    public abstract void truncateObject(String fileId, FileMetadata md, long objNo, int newLength, long newVersion,
            boolean cow) throws IOException;

    /**
     * Deletes all versions of all objects of a file.
     * 
     * @param fileId
     *            the ID of the file
     * @throws IOException
     *             if an error occurred while deleting the objects
     */
    public abstract void deleteFile(String fileId, boolean deleteMetadata) throws IOException;

    /**
     * Deletes a single version of a single object of a file.
     * 
     * @param fileId
     *            the ID of the file
     * @param objNo
     *            the number of the object to delete
     * @param version
     *            the version number of the object to delete
     * @throws IOException
     *             if an error occurred while deleting the object
     */
    public abstract void deleteObject(String fileId, FileMetadata md, long objNo, long version) throws IOException;

    /**
     * Creates and stores a zero-padded object.
     * 
     * @param fileId
     *            the ID of the file
     * @param objNo
     *            the number of the object to create
     * @param sp
     *            the striping policy assigned to the file
     * @param version
     *            the version of the object to create
     * @param size
     *            the size of the object to create
     * @return if OSD checksums are enabled, the newly calculated checksum; <code>null</code>, otherwise
     * @throws IOException
     *             if an error occurred when storing the object
     */
    public abstract void createPaddingObject(String fileId, FileMetadata md, long objNo, long version, int size)
            throws IOException;

    /**
     * Persistently stores a new truncate epoch for a file.
     * 
     * @param fileId
     *            the file ID
     * @param newTruncateEpoch
     *            the new truncate epoch
     * @throws IOException
     *             if an error occurred while storing the new epoch number
     */
    public abstract void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException;

    /**
     * Checks whether the file with the given ID exists.
     * 
     * @param fileId
     *            the ID of the file
     * @return <code>true</code>, if the file exists, <code>false</code>, otherwise
     */
    public abstract boolean fileExists(String fileId);

    /**
     * Updates a single object in the current version of the file. If copy-on-write is enabled, this method has to be
     * invoked when a new object version is written.
     * 
     * @param fileId
     * @param objNo
     * @param newVersion
     * @throws IOException
     */
    public abstract void updateCurrentObjVersion(String fileId, long objNo, long newVersion) throws IOException;

    /**
     * Updates the size of current version of the file. If copy-on-write is enabled, this method has to be invoked when
     * the file is truncated.
     * 
     * @param fileId
     * @param newLastObject
     * @throws IOException
     */
    public abstract void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException;

    // TODO(jdillmann): doc
    public abstract long getFileInfoLoadCount();

    /**
     * returns a list of all local saved objects of this file
     * 
     * @param fileId
     * @return null, if file does not exist, otherwise objectList
     */
    public abstract ObjectSet getObjectSet(String fileId, FileMetadata md);

    // TODO(jdillmann): doc
    public abstract FileList getFileList(FileList l, int maxNumEntries);

    // TODO(jdillmann): doc
    public abstract int getLayoutVersionTag();

    // TODO(jdillmann): doc
    public abstract boolean isCompatibleVersion(int layoutVersionTag);

    /**
     * Retrieves the FleaseM master epoch stored for a file.
     * 
     * @param fileId
     * @return current master epoch stored on disk.
     */
    public abstract int getMasterEpoch(String fileId) throws IOException;

    /**
     * Stores the master epoch for a file on stable storage.
     * 
     * @param fileId
     * @param masterEpoch
     */
    public abstract void setMasterEpoch(String fileId, int masterEpoch) throws IOException;

    // TODO(jdillmann): doc
    public abstract TruncateLog getTruncateLog(String fileId) throws IOException;

    // TODO(jdillmann): doc
    public abstract void setTruncateLog(String fileId, TruncateLog log) throws IOException;

    /**
     * returns a list of all files on OSD as fileID
     * 
     * @return {@link HashMap}<String, String>
     */
    // TODO(jdillmann): return set and fix doc
    public abstract ArrayList<String> getFileIDList();

    // TODO(jdillmann): doc
    public static final class FileList {
        // directories to scan
        final Stack<String>         status;

        // fileName->fileDetails
        final Map<String, FileData> files;

        boolean                     hasMore;

        public FileList(Stack<String> status, Map<String, FileData> files) {
            this.status = status;
            this.files = files;
        }
    }

    // TODO(jdillmann): doc
    public static final class FileData {
        final long size;

        final int  objectSize;

        FileData(long size, int objectSize) {
            this.size = size;
            this.objectSize = objectSize;
        }
    }
}
