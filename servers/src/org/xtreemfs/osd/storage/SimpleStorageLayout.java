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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.osd.ConcurrentFileMap;
import org.xtreemfs.common.striping.StripeInfo;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.osd.OSDConfig;

/**
 * A very simple storage layout implementation. It stores one object per
 * physical file. Objects are stored in files following the FILEID_OBJECTNO
 * pattern
 *
 * @author bjko
 */

@Deprecated
public class SimpleStorageLayout extends StorageLayout {

    private long _stat_fileInfoLoads;

    /** Creates a new instance of SimpleStorageLayout */
    public SimpleStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
        super(config, cache);
        _stat_fileInfoLoads = 0;
    }

    public ReusableBuffer readObject(String fileId, long objNo, int version, String checksum,
        StripingPolicy sp, long osdNumber) throws IOException {
        ReusableBuffer bbuf = null;

        String fileName = storageDir + generateFilename(fileId, objNo, version);

        File file = new File(fileName);

        if (file.exists()) {

            RandomAccessFile f = new RandomAccessFile(fileName, "r");

            if (f.length() > 0) {
                // read object data
                bbuf = BufferPool.allocate((int) f.length());

                f.getChannel().read(bbuf.getBuffer());
            } else {
                // zero padding...
                bbuf = BufferPool.allocate((int) sp.getStripeSize(objNo));
                for (int i = 0; i < sp.getStripeSize(objNo); i++) {
                    bbuf.put((byte) 0);
                }
            }

            f.close();
            bbuf.position(0);
        } else {
            // It handles the POSIX behavior of read beyond EOF
            bbuf = BufferPool.allocate(0);
            bbuf.position(0);
        }

        return bbuf;
    }

    public boolean checkObject(ReusableBuffer obj, String checksum) {
        return true; // no checksum support
    }

    public void writeObject(String fileId, long objNo, ReusableBuffer data, int version,
        int offset, String currentChecksum, StripingPolicy sp, long osdNumber) throws IOException {

        // ignore empty writes
        if (data.capacity() > 0) {

            assert (offset + data.capacity() <= sp.getStripeSize(objNo));

            String fileName = storageDir + generateFilename(fileId, objNo, version);

            File parentDir = new File(storageDir + generateParentDir(fileId));
            if (!parentDir.exists())
                parentDir.mkdir();

            try {
                // File file = new File(fileName);

                RandomAccessFile f = new RandomAccessFile(fileName, "rw");

                data.position(0);
                f.seek(offset);
                f.getChannel().write(data.getBuffer());
                f.close();

                if (version > 0) {
                    // delete old file
                    String fileNameOld = storageDir + generateFilename(fileId, objNo, version - 1);
                    File oldFile = new File(fileNameOld);
                    oldFile.delete();
                }
            } catch (FileNotFoundException ex) {
                throw new IOException("unable to create file directory or object: "
                    + ex.getMessage());
            }
        }
    }

    public String createChecksum(String fileId, long objNo, ReusableBuffer data, int version,
        String currentChecksum) throws IOException {
        return null;
    }

    public void deleteFile(final String fileId) throws IOException {
        File fileDir = new File(storageDir + generateParentDir(fileId));
        File[] objs = fileDir.listFiles();
        assert (objs != null);
        for (File obj : objs) {
            obj.delete();
        }
        fileDir.delete();
        new File(generateMetadataName(fileId)).delete();
    }

    public void deleteAllObjects(final String fileId) throws IOException {
        File fileDir = new File(storageDir + generateParentDir(fileId));
        File[] objs = fileDir.listFiles();
        for (File obj : objs) {
            obj.delete();
        }
    }

    public void deleteObject(String fileId, long objNo, int version) throws IOException {
        File f = new File(storageDir + generateFilename(fileId, objNo, version));
        f.delete();
    }

    private String generateFilename(String fileId, long objNo, int version) {
        return generateParentDir(fileId) + "/" + objNo + "." + version;
    }

    private String generateParentDir(String fileId) {
        return fileId;
    }

    private String generateMetadataName(String fileId) {
        return storageDir + generateParentDir(fileId) + ".metadata";
    }

    public FileInfo loadFileInfo(String fileId, StripingPolicy sp) throws IOException {

        _stat_fileInfoLoads++;

        FileInfo info = new FileInfo();

        File fileDir = new File(storageDir + generateParentDir(fileId));
        if (fileDir.exists()) {

            String[] objs = fileDir.list();
            String lastObject = null;
            long lastObjNum = -1;
            // long lastObjNumVer = -1;

            for (String obj : objs) {
                if (obj.startsWith("."))
                    continue; // ignore special files (metadata, .tepoch)
                int cpos = obj.indexOf('.');
                String tmp = obj.substring(0, cpos);
                long objNum = Long.valueOf(tmp);
                tmp = obj.substring(cpos + 1);
                int objVer = Integer.valueOf(tmp);
                if (objNum > lastObjNum) {
                    lastObject = obj;
                    lastObjNum = objNum;
                }
                Integer oldver = info.getObjVersions().get(objNum);
                if ((oldver == null) || (oldver < objVer)) {
                    info.getObjVersions().put(objNum, objVer);
                }
            }
            if (lastObjNum > -1) {
                File lastObjFile = new File(storageDir + generateParentDir(fileId) + "/"
                    + lastObject);
                long lastObjSize = lastObjFile.length();
                // check for empty padding file
                if (lastObjSize == 0)
                    lastObjSize = sp.getStripeSize(lastObjSize);
                long fsize = lastObjSize;
                if (lastObjNum > 0) {
                    fsize += sp.getLastByte(lastObjNum -1 ) + 1;
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
            fileDir.mkdir();
            info.setFilesize(0);
            info.setLastObjectNumber(-1);
        }

        // info.setMetadata(new Metadata(generateMetadataName(fileId)));

        return info;
    }

    public boolean fileExists(String fileId) {
        File dir = new File(storageDir + generateParentDir(fileId));
        return dir.exists();
    }

    public String createPaddingObject(String fileId, long objId, StripingPolicy sp, int version,
        long size) throws IOException {

        if (size == sp.getStripeSize(objId)) {
            File f = new File(storageDir + generateFilename(fileId, objId, version));
            f.createNewFile();
        } else {
            RandomAccessFile raf = new RandomAccessFile(storageDir
                + generateFilename(fileId, objId, version), "rw");
            raf.setLength(size);
            raf.close();
        }

        return null;
    }

    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
        File parent = new File(storageDir + generateParentDir(fileId));
        if (!parent.exists())
            parent.mkdirs();
        File tepoch = new File(parent, TEPOCH_FILENAME);
        RandomAccessFile rf = new RandomAccessFile(tepoch, "rw");
        rf.writeLong(newTruncateEpoch);
        rf.close();
    }

    public long getFileInfoLoadCount() {
        return _stat_fileInfoLoads;
    }
    
    public ConcurrentFileMap getAllFiles() throws IOException{
        throw new IOException("This function is not available for the deprecated SimpleStorageLayout!");
    }
}
