///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package org.xtreemfs.osd.storage;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.nio.ByteBuffer;
//import java.nio.channels.FileChannel;
//import org.xtreemfs.common.buffer.BufferPool;
//import org.xtreemfs.common.buffer.ReusableBuffer;
//import org.xtreemfs.common.logging.Logging;
//import org.xtreemfs.common.xloc.StripingPolicyImpl;
//import org.xtreemfs.osd.OSDConfig;
//import org.xtreemfs.osd.replication.ObjectSet;
//
///**
// *
// * @author bjko
// */
//public class SingleFileStorageLayout extends StorageLayout {
//
//    public static final int   SL_TAG = 0x00020001;
//
//    private static final String MD_SUFFIX = ".md";
//
//    private static final String DATA_SUFFIX = ".data";
//
//
//    public final static boolean WIN = System.getProperty("os.name").toLowerCase().contains("win");
//
//    private static final String TEPOCH_SUFFIX = ".te";
//
//    private static final int    MDRECORD_SIZE = Long.SIZE/8 * 2;
//
//
//    public SingleFileStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
//        super(config, cache);
//        Logging.logMessage(Logging.LEVEL_ERROR, this,"this storage layout is still under development and should not be used except for testing!");
//    }
//    @Override
//    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
//        FileMetadata fi = new FileMetadata();
//        File f = new File(storageDir+fileId+DATA_SUFFIX);
//        if (f.exists()) {
//            RandomAccessFile ofile = new RandomAccessFile(storageDir+fileId+DATA_SUFFIX, "r");
//
//            try {
//                final int stripeSize = sp.getStripeSizeForObject(0);
//                final long fileSize = ofile.length();
//
//                final long numObjs = (long) Math.ceil((double)fileSize/(double)stripeSize);
//
//                for (long i = 0; i < numObjs; i++) {
//                    long globalON = sp.getGloablObjectNumber(i);
//                    fi.getObjChecksums().put(globalON, 0l);
//                    fi.getObjVersions().put(globalON, 1l);
//                }
//                fi.setLastObjectNumber(sp.getGloablObjectNumber(numObjs-1));
//                fi.setFilesize((fi.getGlobalLastObjectNumber()-1)*stripeSize+fileSize%stripeSize);
//                File tepoch = new File(storageDir+fileId+TEPOCH_SUFFIX);
//                if (tepoch.exists()) {
//                    RandomAccessFile rf = new RandomAccessFile(tepoch, "r");
//                    fi.setTruncateEpoch(rf.readLong());
//                    rf.close();
//                }
//            } finally {
//                ofile.close();
//            }
//        } else {
//            fi.setLastObjectNumber(-1);
//            fi.setFilesize(0);
//            fi.setTruncateEpoch(0);
//        }
//        fi.setGlobalLastObjectNumber(-1);
//        return fi;
//    }
//
//    @Override
//    public ObjectInformation readObject(String fileId, long objNo, long version, long checksum, StripingPolicyImpl sp) throws IOException {
//        return readObject(fileId, objNo, version, checksum, sp, 0, -1);
//    }
//
//    @Override
//    public ObjectInformation readObject(String fileId, long objNo, long objVer, long objChksm, StripingPolicyImpl sp, int offset, int length) throws IOException {
//        RandomAccessFile ofile = new RandomAccessFile(storageDir+fileId+DATA_SUFFIX, "r");
//
//        try {
//            final int stripeSize = sp.getStripeSizeForObject(objNo);
//            final long objFileOffset = sp.getRow(objNo)*stripeSize+offset;
//            final long fileSize = ofile.length();
//
//
//            if (fileSize <= objFileOffset) {
//                //EOF
//                return new ObjectInformation(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, null, stripeSize );
//            } else {
//                long objSize = fileSize-objFileOffset;
//                if (objSize > stripeSize)
//                    objSize = stripeSize-offset;
//                if ((length > 0) && (objSize > length)) {
//                    objSize = length;
//                }
//                ReusableBuffer obj = BufferPool.allocate((int)objSize);
//                final FileChannel c = ofile.getChannel();
//                ofile.seek(objFileOffset);
//                c.read(obj.getBuffer());
//                obj.flip();
//                return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, obj, stripeSize);
//            }
//        } finally {
//            ofile.close();
//        }
//    }
//
//    @Override
//    public boolean checkObject(ReusableBuffer obj, long checksum) {
//        return true;
//    }
//
//    @Override
//    public void writeObject(String fileId, long objNo, ReusableBuffer data, long version, int offset, long checksum, StripingPolicyImpl sp, boolean sync) throws IOException {
//        RandomAccessFile ofile = new RandomAccessFile(storageDir+fileId+DATA_SUFFIX, "rw");
//        RandomAccessFile mdfile = new RandomAccessFile(storageDir+fileId+MD_SUFFIX, "rw");
//
//        try {
//            final int stripeSize = sp.getStripeSizeForObject(objNo);
//            final long objFileOffset = sp.getRow(objNo)*stripeSize+offset;
//            final long fileSize = ofile.length();
//
//            final FileChannel c = ofile.getChannel();
//            ofile.seek(objFileOffset);
//            data.position(0);
//            c.write(data.getBuffer());
//
//            writeMDRecord(mdfile, objNo, version, checksum);
//
//        } finally {
//            ofile.close();
//            mdfile.close();
//        }
//    }
//
//    @Override
//    public long createChecksum(String fileId, long objNo, ReusableBuffer data, long version, long currentChecksum) throws IOException {
//        return 0;
//    }
//
//    private void writeMDRecord(RandomAccessFile mdfile, long objNo, long version, long checksum) throws IOException {
//        final FileChannel mc = mdfile.getChannel();
//        mdfile.seek(objNo*MDRECORD_SIZE);
//        ByteBuffer mdata = ByteBuffer.allocate(MDRECORD_SIZE);
//        mdata.putLong(checksum);
//        mdata.putLong(version);
//        mdata.flip();
//        mc.write(mdata);
//    }
//
//    @Override
//    public void deleteFile(String fileId) throws IOException {
//        File f = new File(storageDir+fileId+MD_SUFFIX);
//        f.delete();
//        f = new File(storageDir+fileId+DATA_SUFFIX);
//        f.delete();
//    }
//
//    @Override
//    public void deleteAllObjects(String fileId) throws IOException {
//        deleteFile(fileId);
//    }
//
//    @Override
//    public void deleteObject(String fileId, long objNo, long version, StripingPolicyImpl sp) throws IOException {
//        //can only pad with zeros or cut file length
//        RandomAccessFile ofile = new RandomAccessFile(storageDir+fileId+DATA_SUFFIX, "w");
//        RandomAccessFile mdfile = new RandomAccessFile(storageDir+fileId+MD_SUFFIX, "w");
//
//        try {
//            final int stripeSize = sp.getStripeSizeForObject(objNo);
//            final long objFileOffset = sp.getRow(objNo)*stripeSize;
//            final long fileSize = ofile.length();
//
//            final int lastObj = (int) Math.ceil((double)fileSize/(double)stripeSize);
//
//            if (fileSize > 0){
//                if (lastObj == objNo) {
//                    if (sp.getRow(objNo) == 0) {
//                        ofile.setLength(0);
//                        mdfile.setLength(0);
//                    } else {
//                        ofile.setLength((sp.getRow(objNo)-1)*stripeSize);
//                        mdfile.setLength((lastObj+1)*MDRECORD_SIZE);
//                    }
//                } else if (objNo < lastObj) {
//                    //fill with 0s
//                    ReusableBuffer buf = BufferPool.allocate(stripeSize);
//                    while (buf.hasRemaining())
//                        buf.put((byte)0);
//                    buf.flip();
//                    final FileChannel c = ofile.getChannel();
//                    c.position(objFileOffset);
//                    c.write(buf.getBuffer());
//
//                }
//                //no else here, if objNo > lastObj, it does not exist
//            }
//
//        } finally {
//            ofile.close();
//            mdfile.close();
//        }
//    }
//
//    @Override
//    public long createPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, long version, long size) throws IOException {
//        RandomAccessFile ofile = new RandomAccessFile(storageDir+fileId+DATA_SUFFIX, "w");
//        RandomAccessFile mdfile = new RandomAccessFile(storageDir+fileId+MD_SUFFIX, "w");
//
//        try {
//            final int stripeSize = sp.getStripeSizeForObject(objNo);
//            final long objFileOffset = sp.getRow(objNo)*stripeSize;
//            final long fileSize = ofile.length();
//
//
//            if (fileSize < objFileOffset + size) {
//               //we need to create the object
//                ofile.setLength(objFileOffset + size);
//                mdfile.setLength((objNo+1)*MDRECORD_SIZE);
//            }
//
//        } finally {
//            ofile.close();
//        }
//        return 0l;
//    }
//
//    @Override
//    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
//        RandomAccessFile rf = new RandomAccessFile(storageDir+fileId+TEPOCH_SUFFIX, "rw");
//        rf.writeLong(newTruncateEpoch);
//        rf.close();
//    }
//
//    @Override
//    public boolean fileExists(String fileId) {
//        File f = new File(storageDir+fileId+DATA_SUFFIX);
//        return f.exists();
//    }
//
//    @Override
//    public long getFileInfoLoadCount() {
//        return 0;
//    }
//
//    @Override
//    public ObjectSet getObjectSet(String fileId) {
//        return new ObjectSet();
//    }
//
//    @Override
//    public FileList getFileList(FileList l, int maxNumEntries) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public int getLayoutVersionTag() {
//        return SL_TAG;
//    }
//
//    @Override
//    public boolean isCompatibleVersion(int layoutVersionTag) {
//        return layoutVersionTag == SL_TAG;
//    }
//
//}
