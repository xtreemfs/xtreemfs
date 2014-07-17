/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Christian Lorenz,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDConfig;

/**
 * Abstracts object data access from underlying on-disk storage layout.
 * 
 * @author bjko
 */
public abstract class FileStorageLayout extends StorageLayout {

    /**
     * file to store the layout and version used to create on disk data
     */
    public static final String    VERSION_FILENAME = ".version";

    /**
     * true, if we are on a windows platform
     */
    public final static boolean   WIN              = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * base directory in which to store files
     */
    protected final String        storageDir;

    protected FileStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
        super(cache);

        // initialize the storage directory
        String tmp = config.getObjDir();
        if (!tmp.endsWith("/"))
            tmp = tmp + "/";
        storageDir = tmp;
        File stdir = new File(storageDir);
        stdir.mkdirs();

        // check the data version
        File versionMetaFile = new File(storageDir, VERSION_FILENAME);
        if (versionMetaFile.exists()) {
            FileReader in = new FileReader(versionMetaFile);
            char[] text = new char[(int) versionMetaFile.length()];
            in.read(text);
            in.close();
            int versionOnDisk = Integer.valueOf(new String(text));
            if (!isCompatibleVersion(versionOnDisk)) {
                throw new IOException("the OSD storage layout used to create the data on disk (" + versionOnDisk
                        + ") is not compatible with the storage layout loaded: " + this.getClass().getSimpleName());
            }
        }

        final File tmpFile = new File(versionMetaFile + ".tmp");

        FileWriter out = new FileWriter(tmpFile);
        out.write(Integer.toString(getLayoutVersionTag()));
        out.close();

        tmpFile.renameTo(versionMetaFile);

    }

    @Override
    public void closeFile(FileMetadata metadata) {
        // do nothing
    };

    protected ReusableBuffer unwrapObjectData(String fileId, FileMetadata md, long objNo, long oldVersion)
            throws IOException {
        ReusableBuffer data;
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        final boolean isLastObj = (md.getLastObjectNumber() == objNo)
                || ((objNo == 0) && (md.getLastObjectNumber() == -1));
        ObjectInformation obj = readObject(fileId, md, objNo, 0, FULL_OBJECT_LENGTH, oldVersion);
        InternalObjectData oldObject = obj.getObjectData(isLastObj, 0, stripeSize);
        if (obj.getData() == null) {
            if (oldObject.getZero_padding() > 0) {
                // create a zero padded object
                data = BufferPool.allocate(oldObject.getZero_padding());
                for (int i = 0; i < oldObject.getZero_padding(); i++) {
                    data.put((byte) 0);
                }
                data.position(0);
            } else {
                data = BufferPool.allocate(0);
            }
        } else {
            if (oldObject.getZero_padding() > 0) {
                data = BufferPool.allocate(obj.getData().capacity() + oldObject.getZero_padding());
                data.put(obj.getData());
                for (int i = 0; i < oldObject.getZero_padding(); i++) {
                    data.put((byte) 0);
                }
            } else {
                data = obj.getData();
            }
        }
        return data;
    }

    protected ReusableBuffer cow(String fileId, FileMetadata md, long objNo, ReusableBuffer data, int offset,
            long oldVersion) throws IOException {
        ReusableBuffer writeData = null;
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        ObjectInformation obj = readObject(fileId, md, objNo, 0, FULL_OBJECT_LENGTH, oldVersion);
        final boolean isLastObj = (md.getLastObjectNumber() == objNo)
                || ((objNo == 0) && (md.getLastObjectNumber() == -1));
        InternalObjectData oldObject = obj.getObjectData(isLastObj, 0, stripeSize);
        if (obj.getData() == null) {
            if (oldObject.getZero_padding() > 0) {
                // create a zero padded object
                writeData = BufferPool.allocate(stripeSize);
                for (int i = 0; i < stripeSize; i++) {
                    writeData.put((byte) 0);
                }
                writeData.position(offset);
                writeData.put(data);
                writeData.position(0);
                BufferPool.free(data);
            } else {
                // write beyond EOF
                if (offset > 0) {
                    writeData = BufferPool.allocate(offset + data.capacity());
                    for (int i = 0; i < offset; i++) {
                        writeData.put((byte) 0);
                    }
                    writeData.put(data);
                    writeData.position(0);
                    BufferPool.free(data);
                } else {
                    writeData = data;
                }
            }
        } else {
            // object data exists on disk
            if (obj.getData().capacity() >= offset + data.capacity()) {
                // old object is large enough
                writeData = obj.getData();
                writeData.position(offset);
                writeData.put(data);
                BufferPool.free(data);
            } else {
                // copy old data and then new data
                writeData = BufferPool.allocate(offset + data.capacity());
                writeData.put(obj.getData());
                BufferPool.free(obj.getData());
                writeData.position(offset);
                writeData.put(data);
                BufferPool.free(data);
            }
        }
        return writeData;
    }
}
