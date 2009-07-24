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

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.checksums.ChecksumAlgorithm;
import org.xtreemfs.common.checksums.ChecksumFactory;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;

/**
 * 
 * @author clorenz
 */
public class HashStorageLayout extends StorageLayout {
    
    /** 32bit algorithm */
    public static final String JAVA_HASH             = "Java-Hash";
    
    /** 64bit algorithm */
    public static final String SDBM_HASH             = "SDBM";
    
    public static final int    SUBDIRS_16            = 15;
    
    public static final int    SUBDIRS_256           = 255;
    
    public static final int    SUBDIRS_4096          = 4095;
    
    public static final int    SUBDIRS_65535         = 65534;
    
    public static final int    SUBDIRS_1048576       = 1048575;
    
    public static final int    SUBDIRS_16777216      = 16777215;
    
    public static final String DEFAULT_HASH          = JAVA_HASH;
    
    private static final int   DEFAULT_SUBDIRS       = SUBDIRS_256;
    
    private static final int   DEFAULT_MAX_DIR_DEPTH = 4;
    
    @Deprecated
    private static final char  VERSION_SEPARATOR     = '.';
    
    @Deprecated
    private static final char  CHECKSUM_SEPARATOR    = '-';
    
    private int                prefixLength;
    
    // private StringChecksumAlgorithm hashAlgo;
    
    private int                hashCutLength;
    
    private ChecksumAlgorithm  checksumAlgo;
    
    private long               _stat_fileInfoLoads;
    
    private final boolean      checksumsEnabled;
    
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
            } catch (NoSuchAlgorithmException e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.db, this,
                    "could not instantiate checksum algorithm '%s'", config.getChecksumProvider());
                Logging.logMessage(Logging.LEVEL_ERROR, Category.db, this,
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
        
        _stat_fileInfoLoads = 0;
    }
    
    public ObjectInformation readObject(String fileId, long objNo, int version, long checksum,
        StripingPolicyImpl sp) throws IOException {
        return readObject(fileId, objNo, version, checksum, sp, 0, -1);
    }
    
    public ObjectInformation readObject(String fileId, long objNo, int version, long checksum,
        StripingPolicyImpl sp, int offset, int length) throws IOException {
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this, "fetching object %s-%d from disk",
                fileId, objNo);
        
        ReusableBuffer bbuf = null;
        
        if (length == -1) {
            assert (offset == 0) : "if length is -1 offset must be 0 but is " + offset;
            length = sp.getStripeSizeForObject(objNo);
        }
        
        String fileName = generateAbsolutObjectPath(fileId, objNo, version, checksum);
        
        if (Logging.isDebug())
            Logging
                    .logMessage(Logging.LEVEL_DEBUG, Category.db, this, "path to object on disk: %s",
                        fileName);
        
        File file = new File(fileName);
        
        if (file.exists()) {
            
            RandomAccessFile f = new RandomAccessFile(fileName, "r");
            
            final int flength = (int) f.length();
            
            try {
                if (flength == 0) {
                    
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this,
                            "object %d is a padding object", objNo);
                    
                    return new ObjectInformation(ObjectInformation.ObjectStatus.PADDING_OBJECT, null, sp
                            .getStripeSizeForObject(objNo));
                    
                } else if (flength <= offset) {
                    
                    bbuf = BufferPool.allocate(0);
                    
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this,
                            "object %d is read at an offset beyond its size", objNo);
                    
                    return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, bbuf, sp
                            .getStripeSizeForObject(objNo));
                    
                } else {
                    
                    // read object data
                    int lastoffset = offset + length;
                    assert (lastoffset <= sp.getStripeSizeForObject(objNo));
                    
                    if (lastoffset > flength) {
                        assert (flength - offset > 0);
                        bbuf = BufferPool.allocate(flength - offset);
                    } else {
                        bbuf = BufferPool.allocate(length);
                    }
                    
                    f.getChannel().position(offset);
                    f.getChannel().read(bbuf.getBuffer());
                    
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this,
                            "object %d is read at offset %d, %d bytes read", objNo, offset, bbuf.limit());
                    
                    assert (!bbuf.hasRemaining());
                    assert (bbuf.remaining() <= length);
                    bbuf.position(0);
                    f.close();
                    return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, bbuf, sp
                            .getStripeSizeForObject(objNo));
                }
            } finally {
                f.close();
            }
            
        } else {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this, "object %d does not exist", objNo);
            
            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, sp
                    .getStripeSizeForObject(objNo));
        }
    }
    
    public boolean checkObject(ReusableBuffer obj, long checksum) {
        
        // calculate and compare the checksum if checksumming is enabled
        if (checksumsEnabled) {
            
            // calculate the checksum
            checksumAlgo.update(obj.getBuffer());
            long calcedChecksum = checksumAlgo.getValue();
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this,
                    "calc'ed checksum: %s, stored checksum: %s", +calcedChecksum, checksum);
            
            // test the checksum
            return calcedChecksum == checksum;
        }
        
        return true;
    }
    
    public void writeObject(String fileId, long objNo, ReusableBuffer data, int version, int offset,
        long checksum, StripingPolicyImpl sp, boolean sync) throws IOException {
        
        // ignore empty writes
        if (data.capacity() > 0) {
            
            String relPath = generateRelativeFilePath(fileId);
            new File(this.storageDir + relPath).mkdirs();
            
            try {
                // write file
                String filename = generateAbsoluteObjectPath(relPath, objNo, version, checksum);
                File file = new File(filename);
                String mode = sync ? "rwd" : "rw";
                RandomAccessFile f = new RandomAccessFile(file, mode);
                
                data.position(0);
                f.seek(offset);
                f.getChannel().write(data.getBuffer());
                f.close();
                
                if (version > 0) {
                    // delete old file
                    String fileNameOld = generateAbsoluteObjectPath(relPath, objNo, version - 1, checksum);
                    File oldFile = new File(fileNameOld);
                    oldFile.delete();
                }
                
            } catch (FileNotFoundException ex) {
                throw new IOException("unable to create file directory or object: " + ex.getMessage());
            }
        }
    }
    
    public long createChecksum(String fileId, long objNo, ReusableBuffer data, int version,
        long currentChecksum) throws IOException {
        
        String relPath = generateRelativeFilePath(fileId);
        new File(this.storageDir + relPath).mkdirs();
        
        try {
            
            // if OSD checksums are enabled, calculate the checksum
            if (checksumAlgo != null) {
                
                String filename = generateAbsoluteObjectPath(relPath, objNo, version, currentChecksum);
                File file = new File(filename);
                
                // if not data is provided, fetch the object from disk
                if (data == null) {
                    
                    RandomAccessFile f = new RandomAccessFile(file, "rw");
                    data = BufferPool.allocate((int) f.length());
                    f.getChannel().read(data.getBuffer());
                    f.close();
                    
                    checksumAlgo.update(data.getBuffer());
                    BufferPool.free(data);
                }

                // otherwise, calculate the checksum directly from the given
                // buffer
                else
                    checksumAlgo.update(data.getBuffer());
                
                // calculate the checksum and wrap it into a buffer, in
                // order to write it to the object file
                long checksum = checksumAlgo.getValue();
                
                // encode the checksum in the file name by renaming the file
                // accordingly
                file.renameTo(new File(generateAbsoluteObjectPath(relPath, objNo, version, checksum)));
                
                return checksum;
            }
            
        } catch (FileNotFoundException ex) {
            throw new IOException("unable to create file directory or object: " + ex.getMessage());
        }
        
        return 0;
    }
    
    public long createPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, int version, long size)
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
        String filename = generateAbsoluteObjectPath(relPath, objNo, version, checksum);
        RandomAccessFile raf = new RandomAccessFile(filename, "rw");
        raf.setLength(size);
        raf.close();
        
        return checksum;
        
    }
    
    public void deleteAllObjects(String fileId) throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles();
        if (objs == null)
            return;
        for (File obj : objs) {
            obj.delete();
        }
    }
    
    public void deleteFile(String fileId) throws IOException {
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles();
        if (objs == null)
            return;
        for (File obj : objs) {
            obj.delete();
        }
        
        // delete all empty dirs along the path
        del(fileDir);
    }
    
    private void del(File parent) {
        if (parent.list().length > 1 || (parent.getAbsolutePath() + "/").equals(this.storageDir)) {
            return;
        } else {
            parent.delete();
            del(parent.getParentFile());
        }
    }
    
    public void deleteObject(String fileId, final long objNo) throws IOException {
        final String prefix = createFileName(objNo, 0, 0).substring(0, 8);
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles(new FileFilter() {
            
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(prefix);
            }
        });
        for (File obj : objs) {
            obj.delete();
        }
    }
    
    public void deleteObject(String fileId, final long objNo, final int version) throws IOException {
        final String prefix = createFileName(objNo, 0, 0).substring(0, 8 + 4);
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        File[] objs = fileDir.listFiles(new FileFilter() {
            
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(prefix);
            }
        });
        for (File obj : objs) {
            obj.delete();
        }
    }
    
    public boolean fileExists(String fileId) {
        File dir = new File(generateAbsoluteFilePath(fileId));
        return dir.exists();
    }
    
    protected FileInfo loadFileInfo(String fileId, StripingPolicyImpl sp) throws IOException {
        
        _stat_fileInfoLoads = 0;
        
        FileInfo info = new FileInfo();
        
        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (fileDir.exists()) {
            
            String[] objs = fileDir.list();
            String lastObject = null;
            long lastObjNum = -1;
            // long lastObjNumVer = -1;
            
            for (String obj : objs) {
                if (obj.startsWith("."))
                    continue; // ignore special files (metadata, .tepoch)
                ObjFileData ofd = parseFileName(obj);
                if (ofd.objNo > lastObjNum) {
                    lastObject = obj;
                    lastObjNum = ofd.objNo;
                }
                
                Integer oldver = info.getObjVersions().get(ofd.objNo);
                if ((oldver == null) || (oldver < ofd.objVersion)) {
                    info.getObjVersions().put((long) ofd.objNo, ofd.objVersion);
                    info.getObjChecksums().put((long) ofd.objNo, ofd.checksum);
                }
            }
            
            // generate filesize from lastObjectNumber
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
            
        } else {
            info.setFilesize(0);
            info.setLastObjectNumber(-1);
        }
        info.setGlobalLastObjectNumber(-1);
        return info;
    }
    
    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
        File parent = new File(generateAbsoluteFilePath(fileId));
        if (!parent.exists())
            parent.mkdirs();
        File tepoch = new File(parent, TEPOCH_FILENAME);
        RandomAccessFile rf = new RandomAccessFile(tepoch, "rw");
        rf.writeLong(newTruncateEpoch);
        rf.close();
    }

    @Override
    public ObjectSet getObjectSet(String fileId) {
        ObjectSet objectSet;

        File fileDir = new File(generateAbsoluteFilePath(fileId));
        if (fileDir.exists()) {
            String[] objs = fileDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith(".")) // ignore special files (metadata, .tepoch)
                        return false;
                    else
                        return true;
                }
            });
            objectSet = new ObjectSet(objs.length);

            for (int i = 0; i < objs.length; i++) {
                objectSet.add(parseFileName(objs[i]).objNo);
            }
        } else
            objectSet = new ObjectSet(0);
            
        return objectSet;
    }

    private String generateAbsoluteFilePath(String fileId) {
        return this.storageDir + generateRelativeFilePath(fileId);
    }
    
    private String generateAbsolutObjectPath(String fileId, long objNo, int version, long checksum) {
        StringBuilder path = new StringBuilder(generateAbsoluteFilePath(fileId));
        path.append(createFileName(objNo, version, checksum));
        return path.toString();
    }
    
    private String generateAbsoluteObjectPath(String relativeFilePath, long objNo, int version, long checksum) {
        StringBuilder path = new StringBuilder(this.storageDir);
        path.append(relativeFilePath);
        path.append(createFileName(objNo, version, checksum));
        return path.toString();
    }
    
    private String generateRelativeFilePath(String fileId) {
        StringBuilder path = generateHashPath(fileId);
        path.append(fileId);
        path.append("/");
        return path.toString();
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
        } else
            return sb.toString();
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
    private int getVersion(File f) throws NumberFormatException {
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
    private int getObjectNo(File f) throws NumberFormatException {
        final String name = f.getName();
        ObjFileData ofd = parseFileName(name);
        return (int) ofd.objNo;
    }
    
    public static String createFileName(long objNo, int objVersion, long checksum) {
        final StringBuffer sb = new StringBuffer(Integer.SIZE / 8 * 3 + Long.SIZE / 8);
        OutputUtils.writeHexLong(sb, objNo);
        OutputUtils.writeHexInt(sb, objVersion);
        OutputUtils.writeHexLong(sb, checksum);
        return sb.toString();
    }
    
    public static ObjFileData parseFileName(String filename) {
        final long objNo = OutputUtils.readHexLong(filename, 0);
        final int objVersion = OutputUtils.readHexInt(filename, 16);
        final long checksum = OutputUtils.readHexLong(filename, 24);
        return new ObjFileData(objNo, objVersion, checksum);
    }
    
    public static final class ObjFileData {
        final long objNo;
        
        final int  objVersion;
        
        final long checksum;
        
        public ObjFileData(long objNo, int objVersion, long checksum) {
            this.objNo = objNo;
            this.objVersion = objVersion;
            this.checksum = checksum;
        }
    }
    
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
                            Logging.logMessage(Logging.LEVEL_ERROR, Category.db, this,
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
                    int stripCount = getObjectNo(newestLast);
                    long fileSize = (stripCount == 1) ? newestFirst.length() : (objectSize * stripCount)
                        + newestLast.length();
                    
                    // insert the data into the FileList
                    l.files.put(dir.getName(), new FileData(fileSize, (int) (objectSize / 1024)));
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
    
    final class FileData {
        final long size;
        
        final int  objectSize;
        
        FileData(long size, int objectSize) {
            this.size = size;
            this.objectSize = objectSize;
        }
    }
}
