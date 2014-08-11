/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.stages.StorageStage;
import org.xtreemfs.osd.storage.VersionTable.Version;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;

// TODO(jdillmann): Implement checksums.
// TODO(jdillmann): Use minimal ReusableBuffers instead of StripeSized ones. 
// TODO(jdillmann): Write test cases for the layout and the version table.

/** Performance oriented StorageLayout, which keeps everything in memory. */
public class InMemoryStorageLayout extends StorageLayout {

    final static int   FILE_INIT_CAP   = 1024;
    final static float FILE_LOAD_FAC   = 0.75f;

    final static int   OBJECT_INIT_CAP = 1024; // 218500
    final static float OBJECT_LOAD_FAC = 0.75f;

    // =================================================================== =============
    // In-memory storage structures
    // ================================================================================

    private final static class InMemoryData {
        /** Metadata **/
        // TODO(jdillmann): move members to this and provide get/set.
        private final InMemoryMetadata              metadata              = new InMemoryMetadata();

        /** Mapping from ObjectId(objNo, objVersion) -> object **/
        private final Map<ObjectId, InMemoryObject> objects               = new HashMap<ObjectId, InMemoryObject>(
                                                                                  OBJECT_INIT_CAP, OBJECT_LOAD_FAC);

        /**
         * Mapping from objNo -> currentObject <br>
         * Contains information which version of a object is currently used.
         **/
        private Map<Long, InMemoryObject>           currentObjectVersions = null;

        InMemoryObject getObject(long objNo, long objVersion) {
            return objects.get(new ObjectId(objNo, objVersion));
        }

        void putObject(InMemoryObject object) {
            objects.put(new ObjectId(object), object);
        }

        InMemoryObject removeObject(long objNo, long objVersion) {
            return objects.remove(new ObjectId(objNo, objVersion));
        }

        Collection<InMemoryObject> getAllObjects() {
            return objects.values();
        }

        int objectSize() {
            return objects.size();
        }

        /**
         * Required to find objects in the map.
         * 
         */
        private final static class ObjectId {
            final long objNo;
            final long objVersion;

            public ObjectId(long objNo, long objVersion) {
                this.objNo = objNo;
                this.objVersion = objVersion;
            }

            public ObjectId(InMemoryObject object) {
                super();
                this.objNo = object.objNo;
                this.objVersion = object.objVersion;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + (int) (objNo ^ (objNo >>> 32));
                result = prime * result + (int) (objVersion ^ (objVersion >>> 32));
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                ObjectId other = (ObjectId) obj;
                if (objNo != other.objNo)
                    return false;
                if (objVersion != other.objVersion)
                    return false;
                return true;
            }
        }
    }

    private final static class InMemoryObject {
        long           objNo      = 0L;
        long           objVersion = 0L;
        ReusableBuffer data       = null;

        public InMemoryObject(long objNo, long objVersion) {
            this.objNo = objNo;
            this.objVersion = objVersion;
        }

        public int getLength() {
            return (data != null ? data.capacity() : 0);
        }

        /**
         * Set the data buffer to the new length.<br>
         * If the present length of the buffer is greater than the newLength argument then the buffer will be limited to
         * newLength. <br>
         * If the present length of the buffer is smaller than the newLength argument then the buffer will be enlarged.
         * In this case, the contents of the extended portion of the buffer are not defined.
         * 
         * @param length
         * @return true if the buffer could be be resized.
         */
        // TODO(jdillmann): Set length could allocate new buffers.
        public void setLength(int newLength) throws IOException {
            if (data == null) {
                if (newLength == 0) {
                    // Non existing objects are always of length 0.
                    return;
                } else {
                    throw new IOException(String.format(
                            "Empty object data buffers can't be resized (objNo='%s', objVersion='%s').", objNo,
                            objVersion));
                }
            }

            if (data.capacity() < newLength) {
                // Try to enlarge the buffer. Fails if the underlying buffer is too small.
                if (!data.enlarge(newLength)) {
                    throw new IOException(
                            String.format(
                                    "Object data buffer can't be resized to %d, because the underlying buffer is too small (objNo='%s', objVersion='%s').",
                                    newLength, objNo, objVersion));
                }
            }

            if (data.capacity() > newLength) {
                // Shrink the buffer.
                data.shrink(newLength);
            }
        }
    }

    private final static class InMemoryMetadata {
        long                     truncateEpoch = 0L;
        int                      masterEpoch   = 0;
        // TODO(jdillmann): Try to save more efficient. Use Buffer and serialize?
        TruncateLog              truncateLog   = TruncateLog.getDefaultInstance();
        // TODO(jdillmann): Save more efficient.
        SortedMap<Long, Version> versionTable  = null;
    }

    // ================================================================================
    // Member Variables
    // ================================================================================

    /**
     * Each StorageThread is responsible for a certain subset of fileIds, which will be stored in different containers
     * to allow concurrent access. Every storage container contains mappings from fileIds to InMemoryData objects.
     */
    private final Map<String, InMemoryData>[] storage;

    /** Number of StorageThreads used to by the OSD **/
    private final int                         numStorageThreads;

    // ================================================================================
    // Constructors
    // ================================================================================

    @SuppressWarnings("unchecked")
    public InMemoryStorageLayout(OSDConfig config, MetadataCache cache) {
        super(cache);

        if (config.isUseChecksums()) {
            throw new UnsupportedOperationException("Checksums are not available with the In-Memory storage.");
        }

        if (config.getStorageThreads() == 0) {
            // If 0 is configured, the StorageStage is using 5 threads as a default.
            numStorageThreads = 5;
        } else {
            numStorageThreads = config.getStorageThreads();
        }

        storage = new Map[numStorageThreads];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = new HashMap<String, InMemoryData>(FILE_INIT_CAP, FILE_LOAD_FAC);
        }
    }

    // ================================================================================
    // Helper methods for accessing the in-memory storage.
    // ================================================================================

    /**
     * Returns the {@link InMemoryData} from the suitable storage container for fileId.
     * 
     * @see #storage
     * 
     * @param fileId
     * @return {@link InMemoryData} stored at the container or null.
     */
    private InMemoryData getData(String fileId) {
        int taskId = StorageStage.getTaskId(fileId, numStorageThreads);
        return storage[taskId].get(fileId);
    }

    /**
     * Associates the {@link InMemoryData} with the fileId in the suitable storage container.
     * 
     * @see #storage
     * @see Map#put(Object, Object)
     * 
     * @param fileId
     *            Must not be null.
     * @param data
     *            Must not be null.
     * @return the previous InMemoryData associated with fileId, or null if there was no mapping.
     */
    private InMemoryData putData(String fileId, InMemoryData data) {
        assert (fileId != null);
        assert (data != null);

        int taskId = StorageStage.getTaskId(fileId, numStorageThreads);
        return storage[taskId].put(fileId, data);
    }

    /**
     * Removes the mapping for the fileId from the suitable storage container if it is present.
     * 
     * @see #storage
     * @see Map#remove(Object, Object)
     * 
     * @param fileId
     * @return the previous InMemoryData associated with fileId, or null if there was no mapping.
     */
    private InMemoryData removeData(String fileId) {
        int taskId = StorageStage.getTaskId(fileId, numStorageThreads);
        return storage[taskId].remove(fileId);
    }

    /**
     * Check if there is in-memory data stored for fileId and return it or throw an IOException.
     * 
     * @param fileId
     * @return
     * @throws IOException
     */
    private InMemoryData getDataOrThrow(String fileId) throws IOException {
        InMemoryData f = getData(fileId);
        if (f == null) {
            throw new IOException("No data stored in memory for fileId: " + fileId);
        }

        return f;
    }

    /**
     * Get the in-memory data for fileId. Creates the entry in memory if it doesn't exist yet.
     * 
     * @param fileId
     * @return
     */
    private InMemoryData getOrCreateData(String fileId) {
        InMemoryData f = getData(fileId);
        if (f == null) {
            // Create a new data object with default values.
            f = new InMemoryData();

            // Store it to the data map.
            putData(fileId, f);
        }

        return f;
    }

    /**
     * Check if there is InMemoryMetadata stored for fileId and return it or throw an IOException.
     * 
     * @param fileId
     * @return
     * @throws IOException
     */
    private InMemoryMetadata getMetadataOrThrow(String fileId) throws IOException {
        return getDataOrThrow(fileId).metadata;
    }

    /**
     * Get the in-memory metadata for fileId. Creates an in-memory data entry with default metadata for fileId if it
     * doesn't exist.
     * 
     * @param fileId
     * @return
     */
    private InMemoryMetadata getOrCreateMetadata(String fileId) {
        return getOrCreateData(fileId).metadata;
    }

    // ================================================================================
    // Implementation of abstract StorageLayout methods
    // ================================================================================

    @Override
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
        FileMetadata info = new FileMetadata(sp);

        InMemoryData file = getData(fileId);

        if (file != null) {
            InMemoryMetadata m = file.metadata;

            Map<Long, Long> largestObjVersions = new HashMap<Long, Long>();
            Map<Long, Long> latestObjVersions = null;

            InMemoryObject lastObject = null;

            boolean multiVersionSupport = (file.currentObjectVersions != null);

            if (multiVersionSupport) {
                // Retrieve the latest object versions and the last object.
                latestObjVersions = new HashMap<Long, Long>();
                for (InMemoryObject o : file.currentObjectVersions.values()) {

                    if (o.objVersion > 0) {
                        latestObjVersions.put(o.objNo, o.objVersion);
                    }

                    if (lastObject == null || o.objNo > lastObject.objNo) {
                        lastObject = o;
                    }
                }
            }

            // Determine the largest object versions, as well as all checksums.
            for (InMemoryObject o : file.getAllObjects()) {

                if (!multiVersionSupport) {
                    if (lastObject == null || lastObject.objNo < o.objNo) {
                        lastObject = o;
                    }
                }

                // Determine the largest object version.
                Long oldver = largestObjVersions.get(o.objNo);
                if ((oldver == null) || (oldver < o.objVersion)) {
                    largestObjVersions.put(o.objNo, o.objVersion);
                }
            }

            if (multiVersionSupport) {
                // If multi-file-version support is enabled, it is necessary to keep track
                // of the largest files versions as well as the lates file versions
                info.initLargestObjectVersions(largestObjVersions);
                info.initLatestObjectVersions(latestObjVersions);
            } else {
                // If no multi-version support is enabled, the file version consists
                // of the set of objects with the largest version numbers.
                info.initLargestObjectVersions(largestObjVersions);
                info.initLatestObjectVersions(largestObjVersions);
            }

            // TODO(jdillmann): Implement checksums.
            info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());

            // Determine the filesize from the lastObject.
            if (lastObject != null) {
                long lastObjSize = lastObject.getLength();

                // check for empty padding file
                if (lastObjSize == 0) {
                    lastObjSize = sp.getStripeSizeForObject(0);
                }

                long fsize = lastObjSize;
                if (lastObject.objNo > 0) {
                    fsize += sp.getObjectEndOffset(lastObject.objNo - 1) + 1;
                }

                assert (fsize >= 0);
                info.setFilesize(fsize);
                info.setLastObjectNumber(lastObject.objNo);
            } else {
                // Empty file!
                info.setFilesize(0l);
                info.setLastObjectNumber(-1);
            }

            // Set the truncateEpoch.
            info.setTruncateEpoch(m.truncateEpoch);

            // Initialize the VersionTable.
            VersionTable vt = new InMemoryVersionTable(fileId, this);
            vt.load();
            info.initVersionTable(vt);
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Trying to load metadata for inexisting file '%s'",
                        fileId);
            }

            // FileMetadata does not exist: create it.
            info.setFilesize(0);
            info.setLastObjectNumber(-1);
            info.initLatestObjectVersions(new HashMap<Long, Long>());
            info.initLargestObjectVersions(new HashMap<Long, Long>());
            info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
            info.initVersionTable(new InMemoryVersionTable(fileId, this));
        }

        info.setGlobalLastObjectNumber(-1);
        return info;
    }

    @Override
    public void closeFile(FileMetadata metadata) {
        // do nothing
    }

    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length, long version)
            throws IOException {

        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "fetching object %s-%d from disk", fileId,
                    objNo);
        }

        ReusableBuffer dataOut = null;

        if (length == FULL_OBJECT_LENGTH) {
            assert (offset == 0) : "if length is FULL_OBJECT_LENGTH offset must be 0 but is " + offset;
            length = stripeSize;
        }

        if (version == 0) {
            // Object does not exist according to the MetadataCache.
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                        "object does not exist (according to md cache)");
            }
            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize);
        }

        // Check if there is data stored for the fileId.
        InMemoryData fileData = getData(fileId);
        if (fileData == null) {
            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize);
        }

        // Check if the requested object exists.
        // TODO(jdillmann): checksum
        InMemoryObject object = fileData.getObject(objNo, version);
        if (object == null || object.data == null) {
            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize);
        }

        // Check if the object data buffers capacity is an empty Buffer: this will be padding objects.
        // TODO(jdillmann): not supported yet!

        // Return an empty buffer, if the offset is beyond the objects length.
        if (object.getLength() <= offset) {

            dataOut = BufferPool.allocate(0);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                        "object %d is read at an offset beyond its size", objNo);
            }

            return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, dataOut, stripeSize);
        }

        // Copy requested slice from the in memory object.
        int lastoffset = offset + length;
        assert (lastoffset <= stripeSize);

        if (lastoffset > object.getLength()) {
            dataOut = BufferPool.allocate(object.getLength() - offset);
        } else {
            dataOut = BufferPool.allocate(length);
        }

        // Copy bytes from the in memory to the output buffer.
        dataOut.position(0);
        object.data.position(offset);
        while (dataOut.hasRemaining()) {
            dataOut.put(object.data.get());
        }

        // Reset the output buffer and return the ObjectInformation.
        dataOut.position(0);
        ObjectInformation oInfo = new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, dataOut, stripeSize);
        return oInfo;
    }

    @Override
    public void writeObject(String fileId, FileMetadata md, ReusableBuffer dataIn, long objNo, int offset,
            long newVersion, boolean sync, boolean cow) throws IOException {

        assert (newVersion > 0) : "object version must be > 0";

        if (dataIn.capacity() == 0) {
            return;
        }

        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        int lastOffset = offset + dataIn.capacity();
        assert (lastOffset <= stripeSize);

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "writing object %s-%d to memory", fileId,
                    objNo);
        }

        // Save information required to cleanup on versionchanges.
        final long oldVersion = md.getLatestObjectVersion(objNo);

        final boolean isRangeWrite = (offset > 0) || (dataIn.capacity() < stripeSize);

        // Get the in memory file data.
        InMemoryData file = getOrCreateData(fileId);
        InMemoryObject object;

        if (isRangeWrite) {
            if (cow) {
                object = new InMemoryObject(objNo, newVersion);

                // Create a copy of the current object.
                InMemoryObject oldObject = file.getObject(objNo, oldVersion);
                if (oldObject.data != null && oldObject.getLength() > 0) {
                    // Allocate a new buffer.
                    // TODO(jdillmann): stripeSize or oldObject.data.capacity()
                    object.data = BufferPool.allocate(stripeSize);

                    // Copy the buffer.
                    // Could be optimized by copying from 0->offset, offset+length->capacity
                    object.data.position(0);
                    oldObject.data.position(0);
                    object.data.put(oldObject.data);
                }
            } else {
                // Reuse the old buffer, but remove it from the map.
                object = file.removeObject(objNo, oldVersion);
            }
        } else {
            if (cow && (oldVersion != newVersion)) {
                // Allocate a new buffer if copy on write is enabled and the version or the checksum changed.
                object = new InMemoryObject(objNo, newVersion);
            } else {
                // Reuse the old buffer, but remove it from the map.
                object = file.removeObject(objNo, oldVersion);
            }
        }

        // TODO(jdillmann): meld into cases above (which should be simplified)
        if (object == null) {
            object = new InMemoryObject(objNo, newVersion);
        }

        // Allocate a new ByteBuffer if none exists yet.
        if (object.data == null) {
            // TODO(jdillmann): Only use required size (which is <= stripeSize)
            object.data = BufferPool.allocate(stripeSize);
        }

        // Update the object length. Throws an Exception if the underlying buffer is to small.
        object.setLength(lastOffset);

        // Copy bytes from the input to the in memory buffer.
        dataIn.position(0);
        object.data.position(offset);
        object.data.put(dataIn);

        // Free the input buffer.
        BufferPool.free(dataIn);

        // Assign the data object to the correct index.
        object.objNo = objNo;
        object.objVersion = newVersion;
        file.putObject(object);

        // Update the current versions.
        md.updateObjectVersion(objNo, newVersion);
    }

    @Override
    public void truncateObject(String fileId, FileMetadata md, long objNo, int newLength, long newVersion, boolean cow)
            throws IOException {

        final long oldVersion = md.getLatestObjectVersion(objNo);
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);

        assert (newLength <= stripeSize);

        InMemoryData file = getOrCreateData(fileId);
        InMemoryObject object = file.getObject(objNo, oldVersion);

        if (object == null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "Truncating nonexistent object fileId='%s', objNo='%s', objVersion='%s'.", fileId, objNo,
                        oldVersion);
            }
            object = new InMemoryObject(objNo, newVersion);
        }

        // Truncating to the same length has no effect.
        int oldLength = object.getLength();
        if (newLength == oldLength) {
            return;
        }

        // if (cow || checksumsEnabled) {
        if (cow) {
            InMemoryObject oldObject = object;
            object = new InMemoryObject(objNo, newVersion);

            if (oldObject.data != null && oldLength > 0 && newLength > 0) { // objectLength > 0

                // Allocate a new buffer.
                // TODO(jdillmann): stripeSize or oldObject.data.capacity()/limit()
                object.data = BufferPool.allocate(stripeSize);
                object.setLength(oldLength);

                // Copy the buffer.
                // Could be optimized by copying from 0->newSize
                object.data.position(0);
                oldObject.data.position(0);
                object.data.put(oldObject.data);
            }

        } else {
            if (newVersion != oldVersion) {
                // Remove the old object from the index.
                file.removeObject(objNo, oldVersion);
            }
        }

        if (newLength == 0) {
            // Free the buffer.
            if (object.data != null) {
                BufferPool.free(object.data);
                object.data = null;
            }

        } else if (newLength < oldLength) {
            // Keep the whole buffer, but limit it to the new length.
            object.setLength(newLength);

        } else if (newLength > oldLength) {
            // Keep the buffer, increase its limit and fill with zeros.
            if (object.data == null) {
                // TODO(jdillmann): or newSize?
                object.data = BufferPool.allocate(stripeSize);
            }

            // Fill with zeros.
            object.setLength(newLength);
            object.data.position(oldLength);
            while (object.data.hasRemaining()) {
                object.data.put((byte) 0);
            }
        }

        if (newVersion != oldVersion) {
            object.objVersion = newVersion;
            md.updateObjectVersion(objNo, newVersion);
            file.putObject(object);
        }

    }

    @Override
    public void deleteFile(String fileId, boolean deleteMetadata) throws IOException {
        InMemoryData file = getData(fileId);

        if (file == null) {
            // If no data is stored, deletion is unnecessary.
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Requested deletion of non existing file '%s'.", fileId);
            }

            return;
        }

        // Free the buffers.
        for (InMemoryObject object : file.getAllObjects()) {
            if (object.data != null) {
                BufferPool.free(object.data);
            }
        }

        // Clear the objects.
        file.objects.clear();

        // Delete the metadata, if requested.
        if (deleteMetadata) {
            removeData(fileId);
        }
    }

    @Override
    public void deleteObject(String fileId, FileMetadata md, long objNo, long version) throws IOException {
        InMemoryData file = getData(fileId);
        if (file == null) {
            // If no data is stored, deletion is unnecessary.
            return;
        }

        final long verToDel = (version == LATEST_VERSION) ? md.getLatestObjectVersion(objNo) : version;
        InMemoryObject object = file.removeObject(objNo, verToDel);
        if (object.data != null) {
            BufferPool.free(object.data);
        }
    }

    @Override
    public void createPaddingObject(String fileId, FileMetadata md, long objNo, long version, int size)
            throws IOException {
        // TODO(jdillmann): not really correct yet because InMemoryObject doesn't allow for paddings...
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);

        InMemoryData file = getOrCreateData(fileId);
        InMemoryObject object = file.getObject(objNo, version);

        if (object == null) {
            object = new InMemoryObject(objNo, version);
            file.putObject(object);
        }

        if (object.data == null) {
            object.data = BufferPool.allocate(stripeSize);
        }

        // Try to set the length of the object.
        object.setLength(size);

        // Set the fileSize and ensure it is filled with zeros.
        // TODO(jdillmann): is this really necessary, HashStorageLayout doesn't care.
        // object.data.position(0);
        // while (object.data.hasRemaining()) {
        // object.data.put((byte) 0);
        // }

        md.updateObjectVersion(objNo, version);
    }

    @Override
    public boolean fileExists(String fileId) {
        return (getData(fileId) != null);
    }

    @Override
    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
        getOrCreateMetadata(fileId).truncateEpoch = newTruncateEpoch;
    }

    @Override
    public void setTruncateLog(String fileId, TruncateLog log) throws IOException {
        getOrCreateMetadata(fileId).truncateLog = log;
    }

    @Override
    public TruncateLog getTruncateLog(String fileId) throws IOException {
        return getMetadataOrThrow(fileId).truncateLog;
    }

    @Override
    public void setMasterEpoch(String fileId, int masterEpoch) throws IOException {
        getOrCreateMetadata(fileId).masterEpoch = masterEpoch;
    }

    @Override
    public int getMasterEpoch(String fileId) throws IOException {
        return getMetadataOrThrow(fileId).masterEpoch;
    }

    @Override
    public void updateCurrentObjVersion(String fileId, long objNo, long newVersion) throws IOException {
        InMemoryData file = getDataOrThrow(fileId);
        InMemoryObject object = file.getObject(objNo, newVersion);

        if (file.currentObjectVersions == null) {
            file.currentObjectVersions = new HashMap<Long, InMemoryObject>();
        }

        file.currentObjectVersions.put(objNo, object);
    }

    /**
     * Remove objects from the files currentObjectsVersions mapping, if the objectNo is greater then the newLastObject.
     */
    @Override
    public void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException {
        InMemoryData file = getDataOrThrow(fileId);

        if (file.currentObjectVersions == null) {
            // TODO(jdillmann): use getter with generator.
            file.currentObjectVersions = new HashMap<Long, InMemoryObject>();
        }

        // If the newLastObject is < 0 the file has been deleted, but there are still older versions around.
        // Thus the currentObjectVersions can be truncated.
        if (newLastObject < 0) {
            file.currentObjectVersions.clear();
        }

        // Remove objectVersions for objects with a number higher than the newLastObject's one.
        for (Iterator<Long> it = file.currentObjectVersions.keySet().iterator(); it.hasNext();) {
            Long objNo = it.next();
            if (objNo > newLastObject) {
                it.remove();
            }
        }
    }

    @Override
    public int getLayoutVersionTag() {
        return 0;
    }

    @Override
    public boolean isCompatibleVersion(int layoutVersionTag) {
        // Always true, because with every restart the in-memory layout will be destroyed.
        return true;
    }

    /**
     * Warning: This is not thread-safe! <br>
     * Used from OSDDrain (GetFileIDListOperation).
     */
    @Override
    public ArrayList<String> getFileIDList() {
        ArrayList<String> fileIdList = new ArrayList<String>();

        for (int i = 0; i < storage.length; i++) {
            fileIdList.addAll(storage[i].keySet());
        }

        return fileIdList;
    }

    @Override
    public ObjectSet getObjectSet(String fileId, FileMetadata md) {
        InMemoryData fileData = getData(fileId);

        // If no data is stored, return an empty ObjectSet.
        if (fileData == null) {
            return new ObjectSet(0);
        }

        // Otherwise add each ObjectNo to the ObjectSet.
        ObjectSet objectSet = new ObjectSet(fileData.objectSize());
        for (InMemoryObject object : fileData.getAllObjects()) {
            objectSet.add(object.objNo);
        }
        return objectSet;
    }

    @Override
    public FileList getFileList(FileList l, int maxNumEntries) {
        // TODO(jdillmann): Implement (see CleanupThread).
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public long getFileInfoLoadCount() {
        // TODO(jdillmann): Implement (even if it is not used anywhere).
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Return the versionTable for fileId.
     * 
     * @param fileId
     * @return the versionTable stored for fileId.
     * @throws IOException
     *             if no metadata for fileId exists.
     */
    SortedMap<Long, Version> getVersionTable(String fileId) throws IOException {
        InMemoryMetadata metadata = getMetadataOrThrow(fileId);
        return metadata.versionTable;
    }

    /**
     * Store the versionTable for fileId in the in-memory metadata.
     * 
     * @param fileId
     * @param versionTable
     * @throws IOException
     *             if no metadata for fileId exists.
     */
    void setVersionTable(String fileId, SortedMap<Long, Version> versionTable) throws IOException {
        InMemoryMetadata metadata = getMetadataOrThrow(fileId);
        metadata.versionTable = versionTable;
    }

}
