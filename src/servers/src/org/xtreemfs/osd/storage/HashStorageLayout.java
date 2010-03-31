/*  Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 and 2008 Consiglio Nazionale delle Ricerche

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

import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.checksums.ChecksumAlgorithm;
import org.xtreemfs.foundation.checksums.ChecksumFactory;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;

/**
 * 
 * @author clorenz
 */
public class HashStorageLayout extends StorageLayout {
    
    /**
     * file to store the truncate epoch in (metadata)
     */
    public static final String             TEPOCH_FILENAME       = ".tepoch";
    
    /**
     * file that stores the mapping between file and object versions
     */
    public static final String             VTABLE_FILENAME       = ".vtable";
    
    /**
     * file that stores the mapping between file and object versions
     */
    public static final String             CURRENT_VER_FILENAME  = ".curr_file_ver";
    
    public static final int                SL_TAG                = 0x00000002;
    
    /** 32bit algorithm */
    public static final String             JAVA_HASH             = "Java-Hash";
    
    /** 64bit algorithm */
    public static final String             SDBM_HASH             = "SDBM";
    
    public static final int                SUBDIRS_16            = 15;
    
    public static final int                SUBDIRS_256           = 255;
    
    public static final int                SUBDIRS_4096          = 4095;
    
    public static final int                SUBDIRS_65535         = 65534;
    
    public static final int                SUBDIRS_1048576       = 1048575;
    
    public static final int                SUBDIRS_16777216      = 16777215;
    
    public static final String             DEFAULT_HASH          = JAVA_HASH;
    
    private static final int               DEFAULT_SUBDIRS       = SUBDIRS_256;
    
    private static final int               DEFAULT_MAX_DIR_DEPTH = 4;
    
    private int                            prefixLength;
    
    private int                            hashCutLength;
    
    private ChecksumAlgorithm              checksumAlgo;
    
    private long                           _stat_fileInfoLoads;
    
    private final boolean                  checksumsEnabled;
    
    private final LRUCache<String, String> hashedPathCache;
    
    private static final boolean           USE_PATH_CACHE        = true;
    
    /** Creates a new instance of HashStorageLayout */
    public HashStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
        this(config, cache, DEFAULT_HASH, DEFAULT_SUBDIRS, DEFAULT_MAX_DIR_DEPTH);
    }
    
    /**
     * Creates a new instance of HashStorageLayout. If some value is incorrect,
     * the default value will be used.
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
         * if (hashAlgo.equals(JAVA_HASH)) { this.hashAlgo = new JavaHash();
         * }else if (hashAlgo.equals(SDBM_HASH)) { this.hashAlgo = new SDBM(); }
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
    }
    
    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length,
        long version) throws IOException {
        
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "fetching object %s-%d from disk",
                fileId, objNo);
        }
        
        ReusableBuffer bbuf = null;
        
        if (length == -1) {
            assert (offset == 0) : "if length is -1 offset must be 0 but is " + offset;
            length = stripeSize;
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
            Logging
                    .logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "path to object on disk: %s",
                        fileName);
        }
        
        File file = new File(fileName);
        
        if (file.exists()) {
            
            RandomAccessFile f = new RandomAccessFile(fileName, "r");
            
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
                    
                    f.getChannel().position(offset);
                    f.getChannel().read(bbuf.getBuffer());
                    
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                            "object %d is read at offset %d, %d bytes read", objNo, offset, bbuf.limit());
                    }
                    
                    assert (!bbuf.hasRemaining());
                    assert (bbuf.remaining() <= length);
                    bbuf.position(0);
                    f.close();
                    return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, bbuf, stripeSize);
                }
            } finally {
                f.close();
            }
            
        } else {
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "object %d does not exist", objNo);
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
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "writing object %s-%d to disk: %s",
                fileId, objNo,relPath);
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
        int offset, long objNo, long newVersion, boolean sync, boolean deleteOldVersion) throws IOException {
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
        RandomAccessFile f = new RandomAccessFile(file, mode);
        
        fullObj.position(0);
        f.getChannel().write(fullObj.getBuffer());
        f.close();
        
        if (deleteOldVersion) {
            String oldFilename = generateAbsoluteObjectPathFromRelPath(relativePath, objNo, oldVersion,
                oldChecksum);
            File oldFile = new File(oldFilename);
            oldFile.delete();
        }
        
        md.updateObjectVersion(objNo, newVersion);
        md.updateObjectChecksum(objNo, newVersion, newChecksum);
        BufferPool.free(fullObj);
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
        RandomAccessFile f = new RandomAccessFile(file, mode);
        
        data.position(0);
        f.seek(offset);
        f.getChannel().write(data.getBuffer());
        f.close();
        BufferPool.free(data);
        
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
        RandomAccessFile f = new RandomAccessFile(file, mode);
        
        data.position(0);
        f.getChannel().write(data.getBuffer());
        f.close();
        BufferPool.free(data);
        
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
        
        RandomAccessFile versionFile = new RandomAccessFile(file, "rw");
        versionFile.seek(objNo * Long.SIZE / 8);
        versionFile.writeLong(newVersion);
        versionFile.close();
    }
    
    @Override
    public void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException {
        
        File file = new File(generateAbsoluteFilePath(fileId), CURRENT_VER_FILENAME);
        if (!file.exists())
            file.createNewFile();
        
        RandomAccessFile versionFile = new RandomAccessFile(file, "rw");
        versionFile.setLength((newLastObject + 1) * Long.SIZE / 8);
        versionFile.close();
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
            RandomAccessFile raf = new RandomAccessFile(newFilename, mode);
            raf.getChannel().write(oldData.getBuffer());
            raf.close();
            BufferPool.free(oldData);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                    "truncate object %d, wrote new version %d: %s", objNo, newVersion, newFilename);
            }
            
            md.updateObjectVersion(objNo, newVersion);
            if (checksumsEnabled)
                md.updateObjectChecksum(objNo, newVersion, newChecksum);
            
        } else {
            // just make the object shorter
            RandomAccessFile raf = new RandomAccessFile(oldFile, mode);
            raf.setLength(newLength);
            raf.close();
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
            byte[] content = new byte[(int) size];
            checksumAlgo.update(ByteBuffer.wrap(content));
            checksum = checksumAlgo.getValue();
        }
        
        // write file
        String filename = generateAbsoluteObjectPathFromRelPath(relPath, objNo, version, checksum);
        RandomAccessFile raf = new RandomAccessFile(filename, "rw");
        raf.setLength(size);
        raf.close();
        
        md.updateObjectVersion(objNo, version);
        
        if (checksumsEnabled)
            md.updateObjectChecksum(objNo, version, checksum);
    }
    
    @Override
    public void deleteFile(String fileId, boolean deleteMetadata) throws IOException {
        
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles();
        
        if (objs == null) {
            return;
        }

        // otherwise, delete the file including its metadata
        else {
            
            for (File obj : objs) {
                obj.delete();
            }
            
            // delete all empty dirs along the path
            if (deleteMetadata) {
                del(fileDir);
            }
            
        }
    }
    
    private void del(File parent) {
        if (parent.list().length > 1 || (parent.getAbsolutePath() + "/").equals(this.storageDir)) {
            return;
        } else {
            parent.delete();
            del(parent.getParentFile());
        }
    }
    
    @Override
    public void deleteObject(String fileId, FileMetadata md, final long objNo, long version)
        throws IOException {
        final long verToDel = (version == LATEST_VERSION) ? md.getLatestObjectVersion(objNo) : version;
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles(new FileFilter() {
            
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
    
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
        
        _stat_fileInfoLoads = 0;
        
        FileMetadata info = new FileMetadata(sp);
        
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        
        // file exists already ...
        if (fileDir.exists()) {
            
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
                    if(checksums == null) {
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
                    largestObjVersions.put((long) ofd.objNo, ofd.objVersion);
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
            
            // determine filesize from lastObjectNumber
            if (lastObjNum > -1) {
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
            
            // read truncate epoch from file
            File tepoch = new File(fileDir, TEPOCH_FILENAME);
            if (tepoch.exists()) {
                RandomAccessFile rf = new RandomAccessFile(tepoch, "r");
                info.setTruncateEpoch(rf.readLong());
                rf.close();
            }
            
            // initialize version table
            File vtFile = new File(fileDir, VTABLE_FILENAME);
            VersionTable vt = new VersionTable(vtFile);
            if (vtFile.exists())
                vt.load();
            
            info.initVersionTable(vt);
            
        }

        // file does not exist
        else {
            info.setFilesize(0);
            info.setLastObjectNumber(-1);
            info.initLatestObjectVersions(new HashMap<Long, Long>());
            info.initLargestObjectVersions(new HashMap<Long, Long>());
            info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
            info.initVersionTable(new VersionTable(new File(fileDir, VTABLE_FILENAME)));
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
        RandomAccessFile rf = new RandomAccessFile(tepoch, "rw");
        rf.writeLong(newTruncateEpoch);
        rf.close();
    }
    
    @Override
    public ObjectSet getObjectSet(String fileId, FileMetadata md) {
        ObjectSet objectSet;
        
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (fileDir.exists()) {
            String[] objs = fileDir.list(new FilenameFilter() {
                
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith(".")) // ignore special files (metadata,
                    // .tepoch)
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
    
    private String generateAbsoluteFilePath(String fileId) {
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
                int PREVIEW_LENGTH = 15;
                String currentDir = l.status.pop();
                File dir = new File(storageDir + currentDir);
                assert (dir.listFiles() != null) : storageDir + currentDir + " is not a valid directory!";
                FileReader fReader;
                
                File newestFirst = null;
                File newestLast = null;
                Long objectSize = 0L;
                
                for (File ch : dir.listFiles()) {
                    // handle the directories (hash and fileName)
                    if (ch != null && ch.isDirectory()) {
                        l.status.push(currentDir + "/" + ch.getName());
                        // get informations from the objects
                    } else if (ch != null && ch.isFile() && !ch.getName().startsWith(".")
                        && !ch.getName().endsWith(".ser")) {
                        // get the file metadata
                        try {
                            if (newestFirst == null) {
                                newestFirst = newestLast = ch;
                                objectSize = ch.length();
                            } else if (getVersion(ch) > getVersion(newestFirst)) {
                                newestFirst = newestLast = ch;
                                objectSize = (objectSize >= ch.length()) ? objectSize : ch.length();
                            } else if (getVersion(ch) == getVersion(newestFirst)) {
                                if (getObjectNo(ch) < getObjectNo(newestFirst)) {
                                    newestFirst = ch;
                                } else if (getObjectNo(ch) > getObjectNo(newestLast)) {
                                    newestLast = ch;
                                }
                                objectSize = (objectSize >= ch.length()) ? objectSize : ch.length();
                            }
                        } catch (NumberFormatException ne) {
                            Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                                "CleanUp: an illegal file was discovered and ignored.");
                        }
                    }
                }
                
                // dir is a fileName-directory
                if (newestFirst != null) {
                    // get a preview from the file
                    char[] preview = null;
                    try {
                        fReader = new FileReader(newestFirst);
                        preview = new char[PREVIEW_LENGTH];
                        fReader.read(preview);
                        fReader.close();
                    } catch (Exception e) {
                        assert (false);
                    }
                    
                    // get the metaInfo from the root-directory
                    long stripCount = getObjectNo(newestLast);
                    long fileSize = (stripCount == 1) ? newestFirst.length() : (objectSize * stripCount)
                        + newestLast.length();
                    
                    // insert the data into the FileList
                    l.files.put((WIN) ? dir.getName().replace('_', ':') : dir.getName(), new FileData(
                        fileSize, (int) (objectSize / 1024)));
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
    
}
