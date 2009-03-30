/*  Copyright (c) 2008 Consiglio Nazionale delle Ricerche and
 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Eugenio Cesario (CNR), Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.osd.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.osd.ConcurrentFileMap;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.osd.OSDConfig;

/**
 * Abstracts object data access from underlying on-disk storage layout.
 *
 * @author bjko
 */
public abstract class StorageLayout {

    public static final String    TEPOCH_FILENAME  = ".tepoch";

    public static final String    VERSION_FILENAME = ".version";

    public static final String    LOCAL_KNOWN_FILESIZE_FILENAME = ".filesize";

    protected final String        storageDir;

    protected final MetadataCache cache;

    protected StorageLayout(OSDConfig config, MetadataCache cache) throws IOException {

        this.cache = cache;

        // initialize the storage directory
        String tmp = config.getObjDir();
        if (!tmp.endsWith("/"))
            tmp = tmp + "/";
        storageDir = tmp;
        File stdir = new File(storageDir);
        stdir.mkdirs();

        // check the data version
        File versionMetaFile = new File(storageDir, VERSION_FILENAME);

        if (!versionMetaFile.exists()) {
            FileWriter out = new FileWriter(versionMetaFile);
            out.write(Long.toString(VersionManagement.getOsdDataVersion()));
            out.close();
        }

        else {
            BufferedReader in = new BufferedReader(new FileReader(versionMetaFile));
            String version = in.readLine();
            in.close();

            if (!version.equals(Long.toString(VersionManagement.getOsdDataVersion())))
                throw new IOException("wrong OSD data version: " + version
                    + ", required version is: " + VersionManagement.getOsdDataVersion());
        }
    }

    /**
     * Returns cached file metadata, or loads and caches it if it is not cached.
     *
     * @param sp
     * @param fileId
     * @return
     * @throws IOException
     */
    public FileInfo getFileInfo(final StripingPolicyImpl sp, final String fileId) throws IOException {

        // try to retrieve metadata from cache
        FileInfo fi = cache.getFileInfo(fileId);

        // if metadata is not cached ...
        if (fi == null) {

            // ... load metadata from disk
            fi = loadFileInfo(fileId, sp);

            // ... cache metadata to speed up further accesses
            cache.setFileInfo(fileId, fi);
        }

        return fi;
    }

    /**
     * Loads all metadata associated with a file on the OSD from the storage
     * device. Amongst others, such metadata may comprise object version numbers
     * and checksums.
     *
     * @param fileId
     *            the file ID
     * @param sp
     *            the striping policy assigned to the file
     * @return a <code>FileInfo</code> object comprising all metadata
     *         associated with the file
     * @throws IOException
     *             if an error occurred while trying to read the metadata
     */
    protected abstract FileInfo loadFileInfo(String fileId, StripingPolicyImpl sp) throws IOException;

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
     * @return a buffer containing the object, or a <code>null</code> if the
     *         object does not exist
     */
    public abstract ObjectInformation readObject(String fileId, long objNo, int version,
	    long checksum, StripingPolicyImpl sp)
	    throws IOException;

    /**
     * Determines whether the given data has a correct checksum.
     *
     * @param obj
     *            the object data
     * @param checksum
     *            the correct checksum
     * @return <code>true</code>, if the given checksum matches the checksum
     *         of the data, <code>false</code>, otherwise
     */
    public abstract boolean checkObject(ReusableBuffer obj, long checksum);

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
     *            the checksum currently assigned to the object; if OSD
     *            checksums are disabled, <code>null</code> can be used
     * @param sp
     *            the striping policy assigned to the file
     * @param osdNumber
     *            the number of the OSD responsible for the object
     * @throws java.io.IOException
     *             when the object cannot be written
     */
    public abstract void writeObject(String fileId, long objNo, ReusableBuffer data, int version,
        int offset, long checksum, StripingPolicyImpl sp) throws IOException;

    /**
     * Calculates and stores the checksum for an object.
     *
     * @param fileId
     *            the file Id the object belongs to
     * @param objNo
     *            the object number
     * @param data
     *            a buffer to calculate the checksum from. If <code>null</code>
     *            is provided, the object will be rad from the storage device
     *            and checksummed.
     * @param version
     *            the version of the object
     * @param currentChecksum
     *            the checksum currently assigned to the object; if OSD
     *            checksums are disabled, <code>null</code> can be used
     * @return if OSD checksums are enabled, the newly calculated checksum;
     *         <code>null</code>, otherwise
     * @throws java.io.IOException
     *             if an I/O error occured
     */
    public abstract long createChecksum(String fileId, long objNo, ReusableBuffer data,
        int version, long currentChecksum) throws IOException;

    /**
     * Deletes all objects of a file.
     *
     * @param fileId
     *            the ID of the file
     * @throws IOException
     *             if an error occurred while deleting the objects
     */
    public abstract void deleteFile(String fileId) throws IOException;

    /**
     * Deletes all objects of a file.
     *
     * @param fileId
     *            the ID of the file
     * @throws IOException
     *             if an error occurred while deleting the objects
     */
    public abstract void deleteAllObjects(String fileId) throws IOException;

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
    public abstract void deleteObject(String fileId, long objNo, int version) throws IOException;

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
     * @return if OSD checksums are enabled, the newly calculated checksum;
     *         <code>null</code>, otherwise
     * @throws IOException
     *             if an error occurred when storing the object
     */
    public abstract long createPaddingObject(String fileId, long objNo, StripingPolicyImpl sp,
        int version, long size) throws IOException;

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
     * @return <code>true</code>, if the file exists, <code>false</code>,
     *         otherwise
     */
    public abstract boolean fileExists(String fileId);

    public abstract long getFileInfoLoadCount();

    /**
     * 
     * @return all available files on the OSD ordered by volume IDs
     * 
     * @throws IOException if an error occurred
     */
    public abstract ConcurrentFileMap getAllFiles() throws IOException;
    
    /**
     * sets the local known filesize
     * @param fileId
     * @param size
     */
    public void writeFilesize(String fileId, long size) throws IOException {
        // try to retrieve metadata from cache
        FileInfo fi = cache.getFileInfo(fileId);

        // if metadata is cached ...
        if (fi != null) {
            fi.setFilesize(size);
        }
    }
    
    /**
     * determines, if special disk-file for filesize exists
     * @return
     * @throws IOException
     */
    public abstract boolean isFilesizeWrittenToDisk(String fileId) throws IOException;
}
