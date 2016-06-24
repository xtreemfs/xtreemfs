/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.checksums.ChecksumAlgorithm;
import org.xtreemfs.foundation.checksums.ChecksumFactory;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.XLocSetVersionState;

/**
 *
 * @author bjko
 */
@Deprecated
public class SingleFileStorageLayout extends StorageLayout {

    public static final int   SL_TAG = 0x00020001;

    private static final String MD_SUFFIX = ".md";

    private static final String DATA_SUFFIX = ".data";

    private static final String TEPOCH_SUFFIX = ".te";

    private static final int    MDRECORD_SIZE = Long.SIZE/8 * 2;

    private static final int    MD_HANDLE = 1;

    private static final int    DATA_HANDLE = 0;

    private final boolean checksumsEnabled;

    private ChecksumAlgorithm              checksumAlgo;

    private final LRUCache<String, String> hashedPathCache;
    private static final int HASH_CUTOFF = 4;

    private final ByteBuffer mdata;

    private static final char[] hexTab = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};


    public SingleFileStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
        super(config, cache);
        checksumsEnabled = config.isUseChecksums();
        mdata = ByteBuffer.allocate(MDRECORD_SIZE);
        if (config.isUseChecksums()) {

            // get the algorithm from the factory
            try {
                checksumAlgo = ChecksumFactory.getInstance().getAlgorithm(config.getChecksumProvider());
                if (checksumAlgo == null)
                    throw new NoSuchAlgorithmException("algo is null");
            } catch (NoSuchAlgorithmException e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.all, this,
                    "could not instantiate checksum algorithm '%s'", config.getChecksumProvider());
                Logging.logMessage(Logging.LEVEL_ERROR, Category.all, this,
                    "OSD checksums will be switched off");
            }
        }

        hashedPathCache = new LRUCache<String, String>(2048);

        Logging.logMessage(Logging.LEVEL_ERROR, this,"this storage layout is still under development and should not be used except for testing!");
    }

    @Override
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
        FileMetadata fi = new FileMetadata(sp);
        Map<Long,Long> tmp = new HashMap();
        fi.initLatestObjectVersions(tmp);
        fi.initLargestObjectVersions(tmp);
        if (checksumsEnabled)
            fi.initObjectChecksums(new HashMap());

        File f = new File(getFilePath(fileId)+DATA_SUFFIX);
        
        if (f.exists()) {
            openHandles(fi,fileId);
            RandomAccessFile ofile = fi.getHandles()[DATA_HANDLE];
            RandomAccessFile mdfile = fi.getHandles()[MD_HANDLE];
            mdfile.seek(0);
            FileChannel mdChannel = mdfile.getChannel();

            final int stripeSize = sp.getStripeSizeForObject(0);
            final long fileSize = ofile.length();

            final long numObjs = (long) Math.ceil((double)fileSize/(double)stripeSize);

            for (long i = 0; i < numObjs; i++) {
                long globalON = sp.getGloablObjectNumber(i);

                long chkSum = mdfile.readLong();
                long version = mdfile.readLong();
                if (version == 0)
                    continue;
                if (checksumsEnabled)
                    fi.updateObjectChecksum(globalON, chkSum, version);
                fi.updateObjectVersion(globalON, version);
            }
            fi.setLastObjectNumber(sp.getGloablObjectNumber(numObjs-1));
            fi.setFilesize((fi.getGlobalLastObjectNumber()-1)*stripeSize+fileSize%stripeSize);
            File tepoch = new File(getFilePath(fileId)+TEPOCH_SUFFIX);
            if (tepoch.exists()) {
                RandomAccessFile rf = new RandomAccessFile(tepoch, "r");
                fi.setTruncateEpoch(rf.readLong());
                rf.close();
            }
        } else {
            fi.setLastObjectNumber(-1);
            fi.setFilesize(0);
            fi.setTruncateEpoch(0);
        }
        fi.setGlobalLastObjectNumber(-1);
        return fi;
    }

    private void openHandles(FileMetadata md, String fileId) throws IOException{
        if (md.getHandles() == null) {
            String filename = getFilePath(fileId);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "open file: %s",
                            filename);
            }
            RandomAccessFile ofile = new RandomAccessFile(filename+DATA_SUFFIX, "rw");
            RandomAccessFile mdfile = new RandomAccessFile(filename+MD_SUFFIX, "rw");
            RandomAccessFile[] handles = new RandomAccessFile[]{ofile,mdfile};
            md.setHandles(handles);
        }
    }

    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length,
        long version) throws IOException {

        openHandles(md,fileId);
        final RandomAccessFile ofile = md.getHandles()[DATA_HANDLE];

        final StripingPolicyImpl sp = md.getStripingPolicy();

        final int stripeSize = sp.getStripeSizeForObject(objNo);
        final long objFileOffset = sp.getRow(objNo)*stripeSize+offset;
        final long fileSize = ofile.length();


        if (fileSize <= objFileOffset) {
            //EOF
            return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize );
        } else {
            long objSize = fileSize-objFileOffset;
            if (objSize > stripeSize)
                objSize = stripeSize-offset;
            if ((length > 0) && (objSize > length)) {
                objSize = length;
            }
            assert(objSize <= stripeSize);
            ReusableBuffer obj = BufferPool.allocate((int)objSize);
            final FileChannel c = ofile.getChannel();
            ofile.seek(objFileOffset);
            c.read(obj.getBuffer());
            obj.flip();

            ObjectInformation oInfo = new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, obj, stripeSize);
            if (checksumsEnabled) {
                //FIXME: optimize the read away
                ReusableBuffer data = BufferPool.allocate(stripeSize);
                ofile.seek(sp.getRow(objNo)*stripeSize);
                c.read(data.getBuffer());
                checksumAlgo.reset();
                checksumAlgo.update(data.getBuffer());
                BufferPool.free(data);
                long newChecksum = checksumAlgo.getValue();
                oInfo.setChecksumInvalidOnOSD(newChecksum != md.getObjectChecksum(objNo, md.getLatestObjectVersion(objNo)));
            }

            return oInfo;
        }
        
    }

    @Override
    public void writeObject(String fileId, FileMetadata md, ReusableBuffer data, long objNo, int offset,
        long newVersion, boolean sync, boolean cow) throws IOException {

        openHandles(md,fileId);
        final RandomAccessFile ofile = md.getHandles()[DATA_HANDLE];
        final RandomAccessFile mdfile = md.getHandles()[MD_HANDLE];

        final StripingPolicyImpl sp = md.getStripingPolicy();

        final int stripeSize = sp.getStripeSizeForObject(objNo);
        final long objFileOffset = sp.getRow(objNo)*stripeSize+offset;

        final FileChannel c = ofile.getChannel();
        ofile.seek(objFileOffset);
        data.position(0);
        c.write(data.getBuffer());

        //do calc checksum
        long newChecksum = 0l;

        if (checksumsEnabled) {
            data.position(0);
            checksumAlgo.reset();
            if (data.remaining() < stripeSize) {
                final long objOffset = sp.getRow(objNo)*stripeSize;
                ReusableBuffer csumData = BufferPool.allocate(stripeSize);
                ofile.seek(objOffset);
                c.read(csumData.getBuffer());
                checksumAlgo.update(csumData.getBuffer());
                BufferPool.free(csumData);
            } else {
                checksumAlgo.update(data.getBuffer());
            }
            newChecksum = checksumAlgo.getValue();
            md.updateObjectChecksum(objNo, newVersion, newChecksum);
        }
        BufferPool.free(data);

        writeMDRecord(md, mdfile, objNo, newVersion, newChecksum);
        md.updateObjectVersion(objNo, newVersion);     
    }

    private void writeMDRecord(FileMetadata md, RandomAccessFile mdfile, long objNo, long version, long checksum) throws IOException {
        long seekPos = objNo*MDRECORD_SIZE;
        if (md.getMdFileLength() <= seekPos) {
            long newSize = seekPos+4096;
            mdfile.setLength(newSize);
            md.setMdFileLength(newSize);
        }
        mdfile.seek(seekPos);
        mdfile.writeLong(checksum);
        mdfile.writeLong(version);

    }

    @Override
    public void deleteFile(String fileId, boolean deleteMetadata) throws IOException {
        File f = new File(getFilePath(fileId)+MD_SUFFIX);
        f.delete();
        f = new File(getFilePath(fileId)+DATA_SUFFIX);
        f.delete();
        if (deleteMetadata) {
            //fixme
        }
    }

    @Override
    public void deleteObject(String fileId, FileMetadata md, final long objNo, long version) throws IOException {
        //can only pad with zeros or cut file length

        openHandles(md,fileId);
        final RandomAccessFile ofile = md.getHandles()[DATA_HANDLE];
        final RandomAccessFile mdfile = md.getHandles()[MD_HANDLE];

        final StripingPolicyImpl sp = md.getStripingPolicy();

        final int stripeSize = sp.getStripeSizeForObject(objNo);
        final long objFileOffset = sp.getRow(objNo)*stripeSize;
        final long fileSize = ofile.length();

        final int lastObj = (int) Math.ceil((double)fileSize/(double)stripeSize);

        if (fileSize > 0){
            if (lastObj == objNo) {
                if (sp.getRow(objNo) == 0) {
                    ofile.setLength(0);
                    mdfile.setLength(0);
                } else {
                    ofile.setLength((sp.getRow(objNo)-1)*stripeSize);
                    mdfile.setLength((lastObj+1)*MDRECORD_SIZE);
                    md.setMdFileLength((lastObj+1)*MDRECORD_SIZE);
                }
            } else if (objNo < lastObj) {
                //fill with 0s
                ReusableBuffer buf = BufferPool.allocate(stripeSize);
                while (buf.hasRemaining())
                    buf.put((byte)0);
                buf.flip();
                final FileChannel c = ofile.getChannel();
                c.position(objFileOffset);
                c.write(buf.getBuffer());

            }
            //no else here, if objNo > lastObj, it does not exist
        }
    }

    @Override
    public void createPaddingObject(String fileId, FileMetadata md, long objNo, long version, int size)
        throws IOException {

        openHandles(md,fileId);
        final RandomAccessFile ofile = md.getHandles()[DATA_HANDLE];
        final RandomAccessFile mdfile = md.getHandles()[MD_HANDLE];

        final StripingPolicyImpl sp = md.getStripingPolicy();

        final int stripeSize = sp.getStripeSizeForObject(objNo);
        final long objFileOffset = sp.getRow(objNo)*stripeSize;
        final long fileSize = ofile.length();


        if (fileSize < objFileOffset + size) {
           //we need to create the object
            ofile.setLength(objFileOffset + size);
            mdfile.setLength((objNo+1)*MDRECORD_SIZE);
            md.setMdFileLength((objNo+1)*MDRECORD_SIZE);
        }


    }

    @Override
    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
        RandomAccessFile rf = new RandomAccessFile(getFilePath(fileId)+TEPOCH_SUFFIX, "rw");
        rf.writeLong(newTruncateEpoch);
        rf.close();
    }

    @Override
    public boolean fileExists(String fileId) {
        File f = new File(getFilePath(fileId)+DATA_SUFFIX);
        return f.exists();
    }

    @Override
    public long getFileInfoLoadCount() {
        return 0;
    }

    @Override
    public ObjectSet getObjectSet(String fileId, FileMetadata md) {
        ObjectSet objectSet;

        objectSet = new ObjectSet(md.getLatestObjectVersions().size());
        for (Entry<Long,Long> e : md.getLatestObjectVersions()) {
            objectSet.add(e.getKey());
        }

        return objectSet;
    }

    @Override
    public FileList getFileList(FileList l, int maxNumEntries) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLayoutVersionTag() {
        return SL_TAG;
    }

    @Override
    public boolean isCompatibleVersion(int layoutVersionTag) {
        return layoutVersionTag == SL_TAG;
    }

    @Override
    public void truncateObject(String fileId, FileMetadata md, long objNo, int newLength, long newVersion, boolean cow) throws IOException {

        openHandles(md,fileId);
        final RandomAccessFile ofile = md.getHandles()[DATA_HANDLE];
        final RandomAccessFile mdfile = md.getHandles()[MD_HANDLE];

        final StripingPolicyImpl sp = md.getStripingPolicy();

        final int stripeSize = sp.getStripeSizeForObject(objNo);
        final long objFileOffset = sp.getRow(objNo)*stripeSize;
        final long fileSize = ofile.length();
        final long lastObj = sp.getObjectNoForOffset(fileSize);

        if (lastObj == objNo) {
            // System.out.println("truncating: "+(objFileOffset + newLength));
            ofile.setLength(objFileOffset + newLength);

            long newChecksum = 0l;
            if (checksumsEnabled) {
                checksumAlgo.reset();
                final long objOffset = sp.getRow(objNo)*stripeSize;
                ReusableBuffer csumData = BufferPool.allocate(stripeSize);
                ofile.seek(objOffset);
                ofile.getChannel().read(csumData.getBuffer());
                checksumAlgo.update(csumData.getBuffer());
                BufferPool.free(csumData);

                newChecksum = checksumAlgo.getValue();
                md.updateObjectChecksum(objNo, newVersion, newChecksum);
            }
            writeMDRecord(md, mdfile, objNo, newVersion, newChecksum);
        }


    }

    /**
     * generates the path for the file with an "/" at the end
     *
     * @param fileId
     * @return
     */
    private String getFilePath(String fileId) {
        //format /dir/xx/xx/fileID

        String path = hashedPathCache.get(fileId);
        if (path == null) {
            StringBuilder hashPath = new StringBuilder(this.storageDir.length()+fileId.length()+2+4);
            hashPath.append(this.storageDir);
            int hash = fileId.hashCode();

            hashPath.append(OutputUtils.trHex[(hash & 0x0F)]);
            hashPath.append(OutputUtils.trHex[((hash >> 4) & 0x0F)]);
            hashPath.append("/");
            hashPath.append(OutputUtils.trHex[((hash >> 8) & 0x0F)]);
            hashPath.append(OutputUtils.trHex[((hash >> 12) & 0x0F)]);
            hashPath.append("/");

            String dirs = hashPath.toString();

            File f = new File(dirs);
            f.mkdirs();

            hashPath.append(fileId);
            path = hashPath.toString();
            hashedPathCache.put(fileId, path);

        }
        return path;


    }

    @Override
    public void updateCurrentObjVersion(String fileId, long objNo, long newVersion) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ArrayList<String> getFileIDList() {
        
        ArrayList<String> fileList = new ArrayList<String>();
        
        Stack<String> directories = new Stack<String>();
        directories.push(storageDir);
        
        File currentFile;
        
        while (!directories.empty()) {
            currentFile = new File(directories.pop());
            for (File f : currentFile.listFiles()) {
                if(f != null && f.isDirectory()) {
                    directories.push(f.getAbsolutePath());
                } else {
                    if (f != null && f.isFile() &&  !f.getName().endsWith(".ser") && f.getName().contains(":")) {
                        String fName = f.getName().replace(DATA_SUFFIX, "").replace(MD_SUFFIX, "")
                        .replace(TEPOCH_SUFFIX, "");
         
                        if (fileList.indexOf(fName) == -1) { 
                            fileList.add(fName);    
                        }
                        
                    }
                } 
            }
        }
        
        return fileList;
    }

    @Override
    public int getMasterEpoch(String fileId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMasterEpoch(String fileId, int masterEpoch) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TruncateLog getTruncateLog(String fileId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTruncateLog(String fileId, TruncateLog log) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XLocSetVersionState getXLocSetVersionState(String fileId) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setXLocSetVersionState(String fileId, XLocSetVersionState versionState) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getInvalidClientExpireTimeSet(String fileId) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setInvalidClientExpireTimeSet(String fileId, Set<String> invalidClientExpireTimeSet) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setECIntervalVector(String fileId, List<Interval> intervals, boolean next, boolean append)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getECIntervalVector(String fileId, boolean next, IntervalVector vector) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
