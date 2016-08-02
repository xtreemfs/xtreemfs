/*
 * Copyright (c) 2009-2011 by Christian Lorenz, Bjoern Kolbeck,
                              Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.checksums.ChecksumAlgorithm;
import org.xtreemfs.foundation.checksums.ChecksumFactory;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.ec.ECStorage;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.XLocSetVersionState;

import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;

/**
 * 
 * @author clorenz
 */
public class HashStorageLayout extends StorageLayout {

    /**
     * file to store the truncate epoch in (metadata)
     */
    public static final String             TEPOCH_FILENAME               = ".tepoch";

    /**
     * File to store the master epoch.
     */
    public static final String             MASTER_EPOCH_FILENAME         = ".mepoch";

    public static final String             TRUNCATE_LOG_FILENAME         = ".tlog";

    /**
     * file that stores the mapping between file and object versions
     */
    public static final String             VTABLE_FILENAME               = ".vtable";

    /**
     * file that stores the mapping between file and object versions
     */
    public static final String             CURRENT_VER_FILENAME          = ".curr_file_ver";

    /**
     * file that stores the latest XLocSet version this replica belonged to and if it is invalidated
     */
    public static final String             XLOC_VERSION_STATE_FILENAME   = ".version_state";

    /*
     * files that store the IntervalVersionTreea used for erasure coding.
     */
    public static final String             EC_VERSIONS_CUR               = ".ec_versions_cur";
    public static final String             EC_VERSIONS_NEXT              = ".ec_versions_next";

    /**
     * file that stores the invalid expire times for a file
     */
    public static final String             QUOTA_INVALID_EXPIRE_TIMES_FILENAME = ".invalid_expire_times";

    public static final int                SL_TAG                        = 0x00000002;

    /** 32bit algorithm */
    public static final String             JAVA_HASH                     = "Java-Hash";

    /** 64bit algorithm */
    public static final String             SDBM_HASH                     = "SDBM";

    public static final int                SUBDIRS_16                    = 15;

    public static final int                SUBDIRS_256                   = 255;

    public static final int                SUBDIRS_4096                  = 4095;

    public static final int                SUBDIRS_65535                 = 65534;

    public static final int                SUBDIRS_1048576               = 1048575;

    public static final int                SUBDIRS_16777216              = 16777215;

    public static final String             DEFAULT_HASH                  = JAVA_HASH;

    private static final int               DEFAULT_SUBDIRS               = SUBDIRS_256;

    private static final int               DEFAULT_MAX_DIR_DEPTH         = 4;

    private int                            prefixLength;

    private int                            hashCutLength;

    private ChecksumAlgorithm              checksumAlgo;

    private long                           _stat_fileInfoLoads;

    private final boolean                  checksumsEnabled;

    private final LRUCache<String, String> hashedPathCache;

    private static final boolean           USE_PATH_CACHE                = true;

    /**
     * An incomplete read may be caused by a bad sector. This parameter defines how often the OSD should retry
     * to read the data as the disk firmware might remap the sector in the meantime / recover the data.
     */
    private static final int               RETRIES_INCOMPLETE_READ       = 2;

    private static final String            ERROR_MESSAGE_INCOMPLETE_READ = "Failed to read the requested number of bytes from the file on disk. Maybe there's a media error or the file was modified outside the scope of the OSD by another process?";

    private final LRUCache<String, XLocSetVersionState> xLocSetVSCache;

    /** Creates a new instance of HashStorageLayout */
    public HashStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
        this(config, cache, DEFAULT_HASH, DEFAULT_SUBDIRS, DEFAULT_MAX_DIR_DEPTH);
    }

    /**
     * Creates a new instance of HashStorageLayout. If some value is incorrect, the default value will be
     * used.
     * 
     * @param config
     * @param hashAlgo
     * @param maxSubdirsPerDir
     * @param maxDirDepth
     * @throws IOException
     */
    public HashStorageLayout(OSDConfig config, MetadataCache cache, String hashAlgo, int maxSubdirsPerDir,
            int maxDirDepth) throws IOException {

        super(config, cache);

        /*
         * if (hashAlgo.equals(JAVA_HASH)) { this.hashAlgo = new JavaHash(); }else if
         * (hashAlgo.equals(SDBM_HASH)) { this.hashAlgo = new SDBM(); }
         */

        this.checksumsEnabled = config.isUseChecksums();
        if (config.isUseChecksums()) {

            // get the algorithm from the factory
            try {
                checksumAlgo = ChecksumFactory.getInstance().getAlgorithm(config.getChecksumProvider());
                if (checksumAlgo == null)
                    throw new NoSuchAlgorithmException("algo is null");
            } catch (NoSuchAlgorithmException e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                        "could not instantiate checksum algorithm '%s'", config.getChecksumProvider());
                Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                        "OSD checksums will be switched off");
            }
        }

        if (maxSubdirsPerDir != 0) {
            this.prefixLength = Integer.toHexString(maxSubdirsPerDir).length();
        } else {
            this.prefixLength = Integer.toHexString(DEFAULT_SUBDIRS).length();
        }

        if (maxDirDepth != 0) {
            this.hashCutLength = maxDirDepth * this.prefixLength;
        } else {
            this.hashCutLength = DEFAULT_MAX_DIR_DEPTH * this.prefixLength;
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "initialized with checksums=%s prefixLen=%d",
                    this.checksumsEnabled, this.prefixLength);
        }

        _stat_fileInfoLoads = 0;

        hashedPathCache = new LRUCache<String, String>(2048);

        xLocSetVSCache = new LRUCache<String, XLocSetVersionState>(2048);
    }

    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length,
            long version) throws IOException {

        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                    "fetching object %s-%d from disk", fileId, objNo);
        }

        ReusableBuffer bbuf = null;
        boolean checkChecksum = false;

        if (length == -1) {
            assert (offset == 0) : "if length is -1 offset must be 0 but is " + offset;
            length = stripeSize;
            if (checksumsEnabled) {
                // Check checksum only if -1 was supplied as length. Fortunately, only xtfs_scrub uses this
                // parameter effectively skipping the expensive checksum check for regular operations.
                checkChecksum = true;
            }
        }

        if (version == 0) {
            // object does not exist
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                        "object does not exist (according to md cache)");
            }
            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize);
        }

        final long oldChecksum = md.getObjectChecksum(objNo, version);
        String fileName = generateAbsoluteObjectPathFromFileId(fileId, objNo, version, oldChecksum);

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "path to object on disk: %s",
                    fileName);
        }

        File file = new File(fileName);

        if (file.exists()) {

            RandomAccessFile f = new RandomAccessFile(file, "r");

            final int flength = (int) f.length();

            try {
                if (flength == 0) {

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                                "object %d is a padding object", objNo);
                    }

                    return new ObjectInformation(ObjectInformation.ObjectStatus.PADDING_OBJECT, null,
                            stripeSize);

                } else if (flength <= offset) {

                    bbuf = BufferPool.allocate(0);

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                                "object %d is read at an offset beyond its size", objNo);
                    }

                    return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, bbuf, stripeSize);

                } else {

                    // read object data
                    int lastoffset = offset + length;
                    assert (lastoffset <= stripeSize);

                    if (lastoffset > flength) {
                        assert (flength - offset > 0);
                        bbuf = BufferPool.allocate(flength - offset);
                    } else {
                        bbuf = BufferPool.allocate(length);
                    }

                    for (int attempt = 0; attempt <= RETRIES_INCOMPLETE_READ; attempt++) {
                        if (attempt > 0) {
                            bbuf.position(0);
                            Logging.logMessage(
                                    Logging.LEVEL_INFO,
                                    Category.storage,
                                    this,
                                    "Retrying to read object from disk since it failed before (retry %d/%d). Path to the file on disk: %s",
                                    attempt, RETRIES_INCOMPLETE_READ, fileName);
                        }

                        f.getChannel().position(offset);
                        f.getChannel().read(bbuf.getBuffer());
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                                    "object %d is read at offset %d, %d bytes read, attempt: %d", objNo,
                                    offset, bbuf.limit(), attempt);
                        }

                        if (bbuf.hasRemaining()) {
                            if (attempt == 0) {
                                Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                                        "%s Path to the file on disk: %s", ERROR_MESSAGE_INCOMPLETE_READ,
                                        fileName);
                            }
                        } else {
                            if (attempt > 0) {
                                Logging.logMessage(
                                        Logging.LEVEL_ERROR,
                                        Category.storage,
                                        this,
                                        "Successfully read object from disk at retry %d after previous failures. Path to the file on disk: %s",
                                        attempt, fileName);
                            }
                            break;
                        }

                        if (attempt == RETRIES_INCOMPLETE_READ) {
                            throw new IOException(ERROR_MESSAGE_INCOMPLETE_READ);
                        }
                    }

                    f.close();

                    bbuf.position(0);
                    ObjectInformation oInfo = new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS,
                            bbuf, stripeSize);

                    if (checkChecksum) {
                        ReusableBuffer bbufCopy = bbuf.createViewBuffer();
                        checksumAlgo.reset();
                        checksumAlgo.update(bbufCopy.getBuffer());
                        BufferPool.free(bbufCopy);
                        long newChecksum = checksumAlgo.getValue();
                        oInfo.setChecksumInvalidOnOSD(newChecksum != oldChecksum);
                    }

                    return oInfo;
                }
            } catch (Exception e) {
                if (bbuf != null) {
                    BufferPool.free(bbuf);
                }

                if (e instanceof IOException) {
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                            "Failed to read object file from disk. Error: %s Path to the file on disk: %s",
                            e.getMessage(), fileName);
                    throw (IOException) e;
                } else {
                    throw new IOException(e);
                }
            } finally {
                f.close();
            }

        } else {

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "object %d does not exist",
                        objNo);
            }

            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize);
        }
    }

    @Override
    public void writeObject(String fileId, FileMetadata md, ReusableBuffer data, long objNo, int offset,
            long newVersion, boolean sync, boolean cow) throws IOException {

        assert (newVersion > 0) : "object version must be > 0";

        if (data.capacity() == 0) {
            return;
        }

        String relPath = generateRelativeFilePath(fileId);
        new File(this.storageDir + relPath).mkdirs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                    "writing object %s-%d to disk: %s", fileId, objNo, relPath);
        }

        try {

            final boolean isRangeWrite = (offset > 0)
                    || (data.capacity() < md.getStripingPolicy().getStripeSizeForObject(objNo));
            if (isRangeWrite) {
                if (cow || checksumsEnabled) {
                    partialWriteCOW(relPath, fileId, md, data, offset, objNo, newVersion, sync, !cow);
                } else {
                    partialWriteNoCOW(relPath, fileId, md, data, objNo, offset, newVersion, sync);
                }
            } else {
                completeWrite(relPath, fileId, md, data, objNo, newVersion, sync, !cow);
            }

        } catch (FileNotFoundException ex) {
            throw new IOException("unable to create file directory or object: " + ex.getMessage());
        }
    }

    private void partialWriteCOW(String relativePath, String fileId, FileMetadata md, ReusableBuffer data,
            int offset, long objNo, long newVersion, boolean sync, boolean deleteOldVersion)
            throws IOException {
        // write file

        assert (data != null);

        final long oldVersion = md.getLatestObjectVersion(objNo);
        final long oldChecksum = md.getObjectChecksum(objNo, oldVersion);

        ReusableBuffer fullObj = cow(fileId, md, objNo, data, offset, oldVersion);

        long newChecksum = 0;
        if (checksumsEnabled) {
            checksumAlgo.reset();
            checksumAlgo.update(fullObj.getBuffer());
            newChecksum = checksumAlgo.getValue();
        }
        final String newFilename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, newVersion,
                newChecksum);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "writing to file (COW): %s", newFilename);
        }
        File file = new File(newFilename);
        String mode = sync ? "rwd" : "rw";
        RandomAccessFile f = null;

        try {
            f = new RandomAccessFile(file, mode);
            fullObj.position(0);
            f.getChannel().write(fullObj.getBuffer());
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                    "Failed to write object file to disk. Error: %s Path to the file on disk: %s",
                    e.getMessage(), newFilename);
            throw e;
        } finally {
            if (f != null) {
                f.close();
            }
            BufferPool.free(fullObj);
        }

        if (deleteOldVersion) {
            String oldFilename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, oldVersion,
                    oldChecksum);
            File oldFile = new File(oldFilename);
            oldFile.delete();
        }

        md.updateObjectVersion(objNo, newVersion);
        md.updateObjectChecksum(objNo, newVersion, newChecksum);
    }

    private void partialWriteNoCOW(String relativePath, String fileId, FileMetadata md, ReusableBuffer data,
            long objNo, int offset, long newVersion, boolean sync) throws IOException {
        // write file
        assert (!checksumsEnabled);

        final long oldVersion = md.getLatestObjectVersion(objNo);
        final String filename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, oldVersion, 0l);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "writing to file: %s", filename);
        }
        File file = new File(filename);
        String mode = sync ? "rwd" : "rw";
        RandomAccessFile f = null;

        try {
            f = new RandomAccessFile(file, mode);
            data.position(0);
            f.seek(offset);
            f.getChannel().write(data.getBuffer());
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                    "Failed to write object file to disk. Error: %s Path to the file on disk: %s",
                    e.getMessage(), filename);
            throw e;
        } finally {
            if (f != null) {
                f.close();
            }
            BufferPool.free(data);
        }

        if (newVersion != oldVersion) {
            String newFilename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, newVersion, 0l);
            file.renameTo(new File(newFilename));
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "renamed to: %s", newFilename);
            }
            md.updateObjectVersion(objNo, newVersion);
        }
    }

    private void completeWrite(String relativePath, String fileId, FileMetadata md, ReusableBuffer data,
            long objNo, long newVersion, boolean sync, boolean deleteOldVersion) throws IOException {
        // write file

        final long oldVersion = md.getLatestObjectVersion(objNo);
        final long oldChecksum = md.getObjectChecksum(objNo, oldVersion);

        long newChecksum = 0;
        if (checksumsEnabled) {
            checksumAlgo.reset();
            checksumAlgo.update(data.getBuffer());
            newChecksum = checksumAlgo.getValue();
        }
        final String newFilename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, newVersion,
                newChecksum);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "writing to file: %s", newFilename);
        }
        File file = new File(newFilename);
        String mode = sync ? "rwd" : "rw";
        RandomAccessFile f = null;

        try {
            f = new RandomAccessFile(file, mode);
            data.position(0);
            f.getChannel().write(data.getBuffer());
        } finally {
            if (f != null) {
                f.close();
            }
            BufferPool.free(data);
        }

        if (((oldVersion != newVersion) || (newChecksum != oldChecksum)) && (deleteOldVersion)) {
            String oldFilename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, oldVersion,
                    oldChecksum);
            File oldFile = new File(oldFilename);
            oldFile.delete();
        }

        md.updateObjectVersion(objNo, newVersion);

        if (checksumsEnabled)
            md.updateObjectChecksum(objNo, newVersion, newChecksum);
    }

    @Override
    public void updateCurrentObjVersion(String fileId, long objNo, long newVersion) throws IOException {

        File file = new File(generateAbsoluteFilePath(fileId), CURRENT_VER_FILENAME);
        if (!file.exists())
            file.createNewFile();

        RandomAccessFile versionFile = null;
        try {
            versionFile = new RandomAccessFile(file, "rw");
            versionFile.seek(objNo * Long.SIZE / 8);
            versionFile.writeLong(newVersion);
        } finally {
            if (versionFile != null) {
                versionFile.close();
            }
        }
    }

    @Override
    public void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException {

        File file = new File(generateAbsoluteFilePath(fileId), CURRENT_VER_FILENAME);
        if (!file.exists())
            file.createNewFile();

        RandomAccessFile versionFile = null;
        try {
            versionFile = new RandomAccessFile(file, "rw");
            versionFile.setLength((newLastObject + 1) * Long.SIZE / 8);
        } finally {
            versionFile.close();
        }
    }

    @Override
    public void truncateObject(String fileId, FileMetadata md, long objNo, int newLength, long newVersion,
            boolean cow) throws IOException {

        final long oldVersion = md.getLatestObjectVersion(objNo);
        final long oldChecksum = md.getObjectChecksum(objNo, oldVersion);

        assert (newLength <= md.getStripingPolicy().getStripeSizeForObject(objNo));

        String oldFileName = generateAbsoluteObjectPathFromFileId(fileId, objNo, oldVersion, oldChecksum);
        File oldFile = new File(oldFileName);
        final long currentLength = oldFile.length();

        String mode = "rw";

        if (newLength == currentLength) {
            return;
        }

        if (cow || checksumsEnabled) {
            ReusableBuffer oldData = unwrapObjectData(fileId, md, objNo, oldVersion);

            if (newLength < oldData.capacity()) {
                oldData.range(0, newLength);
            } else {
                ReusableBuffer newData = BufferPool.allocate(newLength);
                newData.put(oldData);
                while (newData.hasRemaining()) {
                    newData.put((byte) 0);
                }
                BufferPool.free(oldData);
                oldData = newData;
            }
            oldData.position(0);

            long newChecksum = 0l;
            if (checksumsEnabled) {
                // calc checksum
                checksumAlgo.update(oldData.getBuffer());
                newChecksum = checksumAlgo.getValue();
            }

            if (!cow) {
                oldFile.delete();
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                            "truncate object %d, delete old version %d: %s", objNo, oldVersion, oldFileName);
                }
            }

            String newFilename = generateAbsoluteObjectPathFromFileId(fileId, objNo, newVersion, newChecksum);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(newFilename, mode);
                raf.getChannel().write(oldData.getBuffer());
            } finally {
                if (raf != null) {
                    raf.close();
                }
                BufferPool.free(oldData);
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                        "truncate object %d, wrote new version %d: %s", objNo, newVersion, newFilename);
            }

            md.updateObjectVersion(objNo, newVersion);
            if (checksumsEnabled)
                md.updateObjectChecksum(objNo, newVersion, newChecksum);

        } else {
            // just make the object shorter
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(oldFile, mode);
                raf.setLength(newLength);
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
            if (newVersion != oldVersion) {
                String newFilename = generateAbsoluteObjectPathFromFileId(fileId, objNo, newVersion, 0l);
                oldFile.renameTo(new File(newFilename));
                md.updateObjectVersion(objNo, newVersion);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                            "truncate object %d, renamed file for new version %d: %s", objNo, newVersion,
                            newFilename);
                }
            }
        }
    }

    @Override
    public void createPaddingObject(String fileId, FileMetadata md, long objNo, long version, int size)
            throws IOException {

        assert (size >= 0) : "size is " + size;

        String relPath = generateRelativeFilePath(fileId);
        new File(this.storageDir + relPath).mkdirs();

        // calculate the checksum for the padding object if necessary
        long checksum = 0;
        if (checksumAlgo != null) {
            byte[] content = new byte[size];
            checksumAlgo.update(ByteBuffer.wrap(content));
            checksum = checksumAlgo.getValue();
        }

        // write file
        String filename = generateAbsoluteObjectPathFromRelPath(relPath, objNo, version, checksum);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(filename, "rw");
            raf.setLength(size);
        } finally {
            if (raf != null) {
                raf.close();
            }
        }

        md.updateObjectVersion(objNo, version);

        if (checksumsEnabled)
            md.updateObjectChecksum(objNo, version, checksum);
    }

    @Override
    public void deleteFile(String fileId, final boolean deleteMetadata) throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));

        // Filter metadata from the fileList, if deleteMetadata is not set.
        File[] fileList = fileDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().startsWith(".ec_versions")) {
                    return true;
                }

                if (!deleteMetadata && pathname.getName().startsWith(".")) {
                    return false;
                }

                return true;
            }
        });

        // Stop the execution if the directory does not exist.
        if (fileList == null) {
            return;
        }

        // Delete the filtered files.
        for (File file : fileList) {
            file.delete();
        }

        // Try to delete the data directory if it is empty.
        if (deleteMetadata) {
            del(fileDir);
        }
        
        
        // Delete EC directories
        for (String suffix : 
            new String[] { ECStorage.FILEID_CODE_SUFFIX, ECStorage.FILEID_DELTA_SUFFIX, ECStorage.FILEID_NEXT_SUFFIX }) {
            File ecDir = new File(generateAbsoluteFilePath(fileId + suffix));

            if (ecDir != null && ecDir.exists()) {
                File[] ecFileList = ecDir.listFiles();
                if (ecFileList != null) {
                    for (File file : ecFileList) {
                        file.delete();
                    }
                }

                del(ecDir);
            }
        }
    }

    private void del(File parent) {
        File storageDirFile = new File(this.storageDir);
        for (File p = parent; 
                p != null && p.list().length <= 1 && !p.equals(storageDirFile); 
                p = p.getParentFile()) {
            p.delete();
        }
    }

    @Override
    public void deleteObject(String fileId, FileMetadata md, final long objNo, long version)
            throws IOException {
        final long verToDel = (version == LATEST_VERSION) ? md.getLatestObjectVersion(objNo) : version;
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().startsWith(".")) {
                    return false;
                }
                ObjFileData ofd = parseFileName(pathname.getName());
                return (ofd.objNo == objNo) && (ofd.objVersion == verToDel);
            }
        });
        for (File obj : objs) {
            obj.delete();
        }
    }

    @Override
    public boolean fileExists(String fileId) {
        File dir = new File(generateAbsoluteFilePath(fileId));
        return dir.exists();
    }

    @Override
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {

        _stat_fileInfoLoads = 0;

        FileMetadata info = new FileMetadata(sp);
        File fileDir = new File(generateAbsoluteFilePath(fileId));

        // file exists already ...
        if (fileDir.exists()) {
            // read truncate epoch from file
            File tepoch = new File(fileDir, TEPOCH_FILENAME);
            if (tepoch.exists()) {
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(tepoch, "r");
                    info.setTruncateEpoch(raf.readLong());
                } finally {
                    if (raf != null) {
                        raf.close();
                    }
                }
            }


            // Load the VersionTrees required for Erasure Coding
            if (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE) {
                // filesize could be restored from versionvector, but to stay consistent with the
                // other policies store only the local filesize to FileMetadat

                AVLTreeIntervalVector curVector = new AVLTreeIntervalVector();
                getECIntervalVector(fileId, false, curVector);
                info.setECCurVector(curVector);

                AVLTreeIntervalVector nextVector = new AVLTreeIntervalVector();
                getECIntervalVector(fileId, true, nextVector);
                info.setECNextVector(nextVector);

                // FIXME (jdillmann): Update on ECStorage operations?
                // long fileSize = Math.max(curVector.getEnd(), nextVector.getEnd());
                long fileSize = curVector.getEnd();
                info.setFilesize(fileSize);

                // The following values are not used for files with EC policy
                info.setLastObjectNumber(-1);
                info.initLatestObjectVersions(new HashMap<Long, Long>());
                info.initLargestObjectVersions(new HashMap<Long, Long>());
                info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
                info.initVersionTable(new VersionTable(new File(fileDir, VTABLE_FILENAME)));

            } else {

                info.setECCurVector(new AVLTreeIntervalVector());
                info.setECNextVector(new AVLTreeIntervalVector());

                Map<Long, Long> largestObjVersions = new HashMap<Long, Long>();
                Map<Long, Map<Long, Long>> objChecksums = new HashMap<Long, Map<Long, Long>>();
                Map<Long, Long> latestObjVersions = null;

                long lastObjNum = -1;
                String lastObject = null;

                File currVerFile = new File(fileDir, CURRENT_VER_FILENAME);
                boolean multiVersionSupport = currVerFile.exists();

                // if multi-file-version support is enabled, retrieve the object
                // versions for the current file version from the "latest versions"
                // file
                if (multiVersionSupport) {

                    latestObjVersions = new HashMap<Long, Long>();

                    RandomAccessFile rf = new RandomAccessFile(currVerFile, "r");
                    for (long l = 0;; l++) {
                        // read object numbers until the file ends
                        try {
                            long objVer = rf.readLong();
                            if (objVer != 0)
                                latestObjVersions.put(l, objVer);
                        } catch (EOFException exc) {
                            lastObjNum = l - 1;
                            break;
                        }
                    }

                    rf.close();
                }

                // determine the largest object versions, as well as all checksums
                String[] objs = fileDir.list();
                for (String obj : objs) {

                    if (obj.startsWith(".")) {
                        continue; // ignore special files (metadata, .tepoch)
                    }

                    ObjFileData ofd = parseFileName(obj);

                    // determine the checksum
                    if (ofd.checksum != 0) {

                        Map<Long, Long> checksums = objChecksums.get(ofd.objNo);
                        if (checksums == null) {
                            checksums = new HashMap<Long, Long>();
                            objChecksums.put(ofd.objNo, checksums);
                        }

                        checksums.put(ofd.objVersion, ofd.checksum);
                    }

                    // determine the last object
                    if (multiVersionSupport) {
                        Long latestObjVer = latestObjVersions.get(ofd.objNo);
                        if (ofd.objNo == lastObjNum && latestObjVer != null && ofd.objVersion == latestObjVer)
                            lastObject = obj;
                    }

                    else {
                        if (ofd.objNo > lastObjNum) {
                            lastObject = obj;
                            lastObjNum = ofd.objNo;
                        }
                    }

                    // determine the largest object version
                    Long oldver = largestObjVersions.get(ofd.objNo);
                    if ((oldver == null) || (oldver < ofd.objVersion))
                        largestObjVersions.put(ofd.objNo, ofd.objVersion);
                }

                if (multiVersionSupport) {

                    // set object versions and checksums of the latest file version
                    info.initLatestObjectVersions(latestObjVersions);

                    // if multi-file-version support is enabled, it is also
                    // necessary to keep track of the largest file versions
                    info.initLargestObjectVersions(largestObjVersions);
                }

                // if no multi-version support is enabled, the file version consists
                // of the set of objects with the latest version numbers
                else {
                    info.initLatestObjectVersions(largestObjVersions);
                    info.initLargestObjectVersions(largestObjVersions);
                }

                info.initObjectChecksums(objChecksums);

                if (lastObjNum > -1) {
                    // determine filesize from lastObjectNumber
                    File lastObjFile = new File(fileDir.getAbsolutePath() + "/" + lastObject);
                    long lastObjSize = lastObjFile.length();
                    // check for empty padding file
                    if (lastObjSize == 0) {
                        lastObjSize = sp.getStripeSizeForObject(lastObjSize);
                    }
                    long fsize = lastObjSize;
                    if (lastObjNum > 0) {
                        fsize += sp.getObjectEndOffset(lastObjNum - 1) + 1;
                    }
                    assert (fsize >= 0);
                    info.setFilesize(fsize);
                    info.setLastObjectNumber(lastObjNum);

                } else {
                    // empty file!
                    info.setFilesize(0l);
                    info.setLastObjectNumber(-1);
                }

                // initialize version table
                File vtFile = new File(fileDir, VTABLE_FILENAME);
                VersionTable vt = new VersionTable(vtFile);
                if (vtFile.exists())
                    vt.load();

                info.initVersionTable(vt);
            }

        }

        // file does not exist
        else {
            info.setFilesize(0);
            info.setLastObjectNumber(-1);
            info.initLatestObjectVersions(new HashMap<Long, Long>());
            info.initLargestObjectVersions(new HashMap<Long, Long>());
            info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
            info.initVersionTable(new VersionTable(new File(fileDir, VTABLE_FILENAME)));
            info.setECCurVector(new AVLTreeIntervalVector());
            info.setECNextVector(new AVLTreeIntervalVector());
        }

        info.setGlobalLastObjectNumber(-1);
        return info;
    }

    @Override
    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
        File parent = new File(generateAbsoluteFilePath(fileId));
        if (!parent.exists()) {
            parent.mkdirs();
        }
        File tepoch = new File(parent, TEPOCH_FILENAME);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(tepoch, "rw");
            raf.writeLong(newTruncateEpoch);
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }

    @Override
    public ObjectSet getObjectSet(String fileId, FileMetadata md) {
        ObjectSet objectSet;

        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (fileDir.exists()) {
            String[] objs = fileDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith(".")) // ignore special files (metadata, .tepoch)
                    {
                        return false;
                    } else {
                        return true;
                    }
                }
            });
            objectSet = new ObjectSet(objs.length);

            for (int i = 0; i < objs.length; i++) {
                objectSet.add(parseFileName(objs[i]).objNo);
            }
        } else {
            objectSet = new ObjectSet(0);
        }

        return objectSet;
    }

    public String generateAbsoluteFilePath(String fileId) {
        return this.storageDir + generateRelativeFilePath(fileId);
    }

    private String generateAbsoluteObjectPathFromFileId(String fileId, long objNo, long version, long checksum) {
        StringBuilder path = new StringBuilder(generateAbsoluteFilePath(fileId));
        path.append(createFileName(objNo, version, checksum));
        return path.toString();
    }

    private String generateAbsoluteObjectPathFromRelPath(String relativeFilePath, long objNo, long version,
            long checksum) {
        StringBuilder path = new StringBuilder(this.storageDir);
        path.append(relativeFilePath);
        path.append(createFileName(objNo, version, checksum));
        return path.toString();
    }

    private String generateRelativeFilePath(String fileId) {
        if (USE_PATH_CACHE) {
            String cached = hashedPathCache.get(fileId);
            if (cached != null)
                return cached;
        }
        String id = (WIN) ? fileId.replace(':', '_') : fileId;
        StringBuilder path = generateHashPath(id);
        path.append(id);
        path.append("/");
        final String pathStr = path.toString();
        if (USE_PATH_CACHE) {
            hashedPathCache.put(fileId, pathStr);
        }
        return pathStr;
    }

    /**
     * generates the path for the file with an "/" at the end
     * 
     * @param fileId
     * @return
     */
    private StringBuilder generateHashPath(String fileId) {
        StringBuilder hashPath = new StringBuilder(128);
        String hash = hash(fileId);
        int i = 0, j = prefixLength;

        while (j < hash.length()) {
            hashPath.append(hash.subSequence(i, j));
            hashPath.append("/");

            i += prefixLength;
            j += prefixLength;
        }
        if (j < hash.length() + prefixLength) {
            hashPath.append(hash.subSequence(i, hash.length()));
            hashPath.append("/");
        }
        return hashPath;
    }

    /**
     * computes the hash for the File
     * 
     * @param str
     * @return
     */
    private String hash(String str) {
        assert (str != null);
        // this.hashAlgo.digest(str);
        StringBuffer sb = new StringBuffer(16);
        // final long hashValue = this.hashAlgo.getValue();
        final long hashValue = str.hashCode();
        OutputUtils.writeHexLong(sb, hashValue);

        if (sb.length() > this.hashCutLength) {
            return sb.substring(0, this.hashCutLength);
        } else {
            return sb.toString();
        }
    }

    @Override
    public long getFileInfoLoadCount() {
        return _stat_fileInfoLoads;
    }

    /**
     * 
     * @param f
     * @return the VersionNo of the given File.
     * @throws NumberFormatException
     */
    private long getVersion(File f) throws NumberFormatException {
        final String name = f.getName();
        ObjFileData ofd = parseFileName(name);
        return ofd.objVersion;
    }

    /**
     * 
     * @param f
     * @return the ObjectNo of the given File.
     * @throws NumberFormatException
     */
    private long getObjectNo(File f) throws NumberFormatException {
        final String name = f.getName();
        ObjFileData ofd = parseFileName(name);
        return ofd.objNo;
    }

    public static String createFileName(long objNo, long objVersion, long checksum) {
        final StringBuffer sb = new StringBuffer(3 * Long.SIZE / 8);
        OutputUtils.writeHexLong(sb, objNo);
        OutputUtils.writeHexLong(sb, objVersion);
        OutputUtils.writeHexLong(sb, checksum);
        return sb.toString();
    }

    public static ObjFileData parseFileName(String filename) {
        if (filename.length() == 32) {
            // compatability mode
            final long objNo = OutputUtils.readHexLong(filename, 0);
            final int objVersion = OutputUtils.readHexInt(filename, 16);
            final long checksum = OutputUtils.readHexLong(filename, 24);
            return new ObjFileData(objNo, objVersion, checksum);
        } else {
            final long objNo = OutputUtils.readHexLong(filename, 0);
            final long objVersion = OutputUtils.readHexLong(filename, 16);
            final long checksum = OutputUtils.readHexLong(filename, 32);
            return new ObjFileData(objNo, objVersion, checksum);
        }
    }

    @Override
    public int getLayoutVersionTag() {
        return SL_TAG;
    }

    @Override
    public boolean isCompatibleVersion(int layoutVersionTag) {
        if (layoutVersionTag == SL_TAG) {
            return true;
        }
        // we are compatible with the old layout (version was an int)
        if (layoutVersionTag == 1) {
            return true;
        }
        return false;
    }

    public static final class ObjFileData {

        final long objNo;

        final long objVersion;

        final long checksum;

        public ObjFileData(long objNo, long objVersion, long checksum) {
            this.objNo = objNo;
            this.objVersion = objVersion;
            this.checksum = checksum;
        }
    }

    @Override
    public FileList getFileList(FileList l, int maxNumEntries) {

        if (l == null) {
            l = new FileList(new Stack<String>(), new HashMap<String, FileData>());
            l.status.push("");
        }
        l.files.clear();

        try {
            do {
                String currentDir = l.status.pop();
                File dir = new File(storageDir + currentDir);
                if (dir.listFiles() == null) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, storageDir + currentDir
                            + " is not a valid directory!");
                    continue;
                }

                File newestFirst = null;
                File newestLast = null;
                Long objectSize = 0L;
                boolean isFileNameDir = false;

                for (File ch : dir.listFiles()) {
                    // handle the directories (hash and fileName)
                    if (ch != null && ch.isDirectory()) {
                        // FIXME (jdillmann): Exclude EC Meta
                        l.status.push(currentDir + "/" + ch.getName());
                        // get information from the objects
                    } else if (ch != null && ch.isFile() && !ch.getName().contains(".")
                            && !ch.getName().endsWith(".ser")) {
                        // get the file metadata
                        try {
                            long version = getVersion(ch);
                            long objNum = getObjectNo(ch);

                            isFileNameDir = true;
                            if (newestFirst == null) {

                                newestFirst = newestLast = ch;
                                objectSize = ch.length();
                            } else if (version > getVersion(newestFirst)) {

                                newestFirst = newestLast = ch;
                                objectSize = (objectSize >= ch.length()) ? objectSize : ch.length();
                            } else if (version == getVersion(newestFirst)) {

                                if (objNum < getObjectNo(newestFirst)) {
                                    newestFirst = ch;
                                } else if (objNum > getObjectNo(newestLast)) {
                                    newestLast = ch;
                                }
                                objectSize = (objectSize >= ch.length()) ? objectSize : ch.length();
                            }
                        } catch (Exception e) {

                            Logging.logMessage(Logging.LEVEL_WARN, Category.storage, this,
                                    "CleanUp: an illegal file (" + ch.getAbsolutePath()
                                            + ") was discovered and ignored.");
                        }
                    } else if (ch != null && ch.isFile() && ch.getName().endsWith(XLOC_VERSION_STATE_FILENAME)) {
                        // If no data file exists, but a version_state file, the whole data folder can be deleted after
                        // a certain period.
                        isFileNameDir = true;
                    }
                }

                // FIXME (jdillmann): make sure "fake" EC file ids are handled correctly

                // dir is a fileName-directory
                if (isFileNameDir) {
                    if (newestFirst != null) {
                        // get the metaInfo from the root-directory
                        long stripCount = getObjectNo(newestLast);
                        long fileSize = (stripCount == 1) ? newestFirst.length() : (objectSize * stripCount)
                                + newestLast.length();

                        // insert the data into the FileList
                        l.files.put((WIN) ? dir.getName().replace('_', ':') : dir.getName(),
                                new FileData(fileSize, (int) (objectSize / 1024)));
                    } else {
                        // No data file exists, but the folders metadata is still in place.
                        l.files.put((WIN) ? dir.getName().replace('_', ':') : dir.getName(), 
                                new FileData(true));
                    }
                }
            } while (l.files.size() < maxNumEntries);
            l.hasMore = true;
            return l;

        } catch (EmptyStackException ex) {
            // done
            l.hasMore = false;
            return l;
        }
    }

    @Override
    public ArrayList<String> getFileIDList() {

        ArrayList<String> fileList = new ArrayList<String>();

        Stack<String> directories = new Stack<String>();
        directories.push(storageDir);

        File currentFile;
        // FIXME (jdillmann): make sure "fake" EC file ids are handled correctly

        while (!directories.empty()) {
            currentFile = new File(directories.pop());
            for (File f : currentFile.listFiles()) {
                if (f != null && f.isDirectory() && !f.getName().contains(":")) {
                    directories.push(f.getAbsolutePath());
                } else {
                    // Ignoring "." in names will ignore EC .next .code .delta directories as well
                    if (f != null && !f.getName().contains(".")) {
                        fileList.add(f.getName());
                    }
                }
            }
        }

        return fileList;
    }

    @Override
    public int getMasterEpoch(String fileId) throws IOException {
        int masterEpoch = 0;
        RandomAccessFile raf = null;
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File mepoch = new File(fileDir, MASTER_EPOCH_FILENAME);

        try {
            raf = new RandomAccessFile(mepoch, "r");

            masterEpoch = raf.readInt();
        } catch (FileNotFoundException ex) {
            // Before XtreemFS 1.4.1 the .mepoch was accidentally stored in the
            // wrong directory because a leading "/" was not removed from
            // fileId.
            String oldFileId = "/" + fileId;
            File oldFileDir = new File(generateAbsoluteFilePath(oldFileId));
            File oldMepoch = new File(oldFileDir, MASTER_EPOCH_FILENAME);

            if (oldMepoch.isFile()) {
                if (!fileDir.exists()) {
                    fileDir.mkdirs();
                }

                if (oldMepoch.renameTo(mepoch)) {
                    del(oldFileDir);

                    raf = new RandomAccessFile(mepoch, "r");
                } else {
                    Logging.logMessage(Logging.LEVEL_WARN, this, "Failed to move %s file from: %s to: %s",
                            MASTER_EPOCH_FILENAME, oldFileDir.getPath(), fileDir.getPath());

                    raf = new RandomAccessFile(oldMepoch, "r");
                }

                masterEpoch = raf.readInt();
            }
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
        return masterEpoch;
    }

    @Override
    public void setMasterEpoch(String fileId, int masterEpoch) throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File mepoch = new File(fileDir, MASTER_EPOCH_FILENAME);
        RandomAccessFile rf = new RandomAccessFile(mepoch, "rw");
        rf.writeInt(masterEpoch);
        rf.close();
    }

    @Override
    public TruncateLog getTruncateLog(String fileId) throws IOException {
        TruncateLog.Builder tlbuilder = TruncateLog.newBuilder();

        try {
            File fileDir = new File(generateAbsoluteFilePath(fileId));
            File tlog = new File(fileDir, TRUNCATE_LOG_FILENAME);
            FileInputStream input = null;
            try {
                input = new FileInputStream(tlog);
                tlbuilder.mergeDelimitedFrom(input);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (IOException ex) {
        }
        return tlbuilder.build();
    }

    @Override
    public void setTruncateLog(String fileId, TruncateLog log) throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File tlog = new File(fileDir, TRUNCATE_LOG_FILENAME);
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(tlog);
            log.writeDelimitedTo(output);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public XLocSetVersionState getXLocSetVersionState(String fileId) throws IOException {
        XLocSetVersionState state = xLocSetVSCache.get(fileId);

        if (state == null) {
            File fileDir = new File(generateAbsoluteFilePath(fileId));
            File vsFile = new File(fileDir, XLOC_VERSION_STATE_FILENAME);
    
            FileInputStream input = null;
            try {
                input = new FileInputStream(vsFile);

                XLocSetVersionState.Builder vsbuilder = XLocSetVersionState.newBuilder();
                vsbuilder.mergeDelimitedFrom(input);
                state = vsbuilder.build();
            } catch (FileNotFoundException e) {
                // If the file does not exist yet, set the initial state.
                state = XLocSetVersionState.newBuilder().setInvalidated(true).setVersion(-1).build();
            } catch (UninitializedMessageException e) {
                // If the parsed message did miss some required fields, set the initial state and log the error.
                Logging.logMessage(Logging.LEVEL_WARN, this,
                        "Version state file is corrupt. Using default values. FileId: %s", fileId);
                state = XLocSetVersionState.newBuilder().setInvalidated(true).setVersion(-1).build();
            } finally {
                if (input != null) {
                    input.close();
                }
            }

            // Cache the version state.
            xLocSetVSCache.put(fileId, state);
        }

        return state;
    }

    @Override
    public void setXLocSetVersionState(String fileId, XLocSetVersionState versionState) throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }

        File vsFile = new File(fileDir, XLOC_VERSION_STATE_FILENAME);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(vsFile);
            versionState.writeDelimitedTo(output);
            xLocSetVSCache.put(fileId, versionState);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public Set<String> getInvalidClientExpireTimeSet(String fileId) throws IOException {

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                    "get invalid client expire times for file id: %s", fileId);
        }

        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File invalidClientExpireTimeFile = new File(fileDir, QUOTA_INVALID_EXPIRE_TIMES_FILENAME);

        Set<String> invalidClientExpireTimeSet = new TreeSet<String>();

        if (!invalidClientExpireTimeFile.exists()) {
            return invalidClientExpireTimeSet;
        }

        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(new FileInputStream(invalidClientExpireTimeFile)));

            String line;
            while ((line = input.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                invalidClientExpireTimeSet.add(line);
            }
        } finally {
            if (input != null) {
                input.close();
            }
        }

        return invalidClientExpireTimeSet;
    }

    @Override
    public void setInvalidClientExpireTimeSet(String fileId, Set<String> invalidClientExpireTimeSet) throws IOException {

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                    "set invalid client expire times for file id %s and count of expire times %d", fileId,
                    invalidClientExpireTimeSet.size());
        }

        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }

        File invalidClientExpireTimeFile = new File(fileDir, QUOTA_INVALID_EXPIRE_TIMES_FILENAME);

        if (invalidClientExpireTimeSet == null || invalidClientExpireTimeSet.isEmpty()) {
            if (invalidClientExpireTimeFile.exists()) {
                invalidClientExpireTimeFile.delete();
            }
            return;
        }

        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(invalidClientExpireTimeFile)));

            for (String invalidClientExpireTime : invalidClientExpireTimeSet) {
                output.write(invalidClientExpireTime);
                output.newLine();
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public void setECIntervalVector(String fileId, List<Interval> intervals, boolean next, boolean append)
            throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        fileDir.mkdirs();

        File vectorFile = next ? new File(fileDir, EC_VERSIONS_NEXT) : new File(fileDir, EC_VERSIONS_CUR);
        if (!vectorFile.exists()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "IntervalVector logfile created (%s).",
                    vectorFile.toString());
            vectorFile.createNewFile();
        }

        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(vectorFile, append));
            for (Interval interval : intervals) {
                Message msg = ProtoInterval.toProto(interval);
                msg.writeDelimitedTo(output);
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public boolean getECIntervalVector(String fileId, boolean next, IntervalVector vector) throws IOException {

        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File vectorFile = next ? new File(fileDir, EC_VERSIONS_NEXT) : new File(fileDir, EC_VERSIONS_CUR);

        if (!fileDir.exists() || !vectorFile.exists()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Could not get %s IntervalVector for fileId %s",
                    (next ? "next" : "current"), fileId);
            return false;
            // throw new FileNotFoundException()
        }

        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(vectorFile));
            org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg.Builder msg;
            msg = org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg.newBuilder();

            boolean eof = false;
            while (!eof) {
                msg.clear();

                if (msg.mergeDelimitedFrom(input)) {
                    ProtoInterval interval = new ProtoInterval(msg.build());
                    vector.insert(interval);
                } else {
                    eof = true;
                }
            }

        } finally {
            if (input != null) {
                input.close();
            }
        }

        return true;
    }
}
