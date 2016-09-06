package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.ArrayList;
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
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.XLocSetVersionState;

public class NullStorageLayout extends StorageLayout {
    final static int               CACHE_SIZE  = 50;

    LRUCache<String, FileMetadata> fileCache   = new LRUCache<String, FileMetadata>(CACHE_SIZE);
    LRUCache<String, Integer>      mepochCache = new LRUCache<String, Integer>(CACHE_SIZE);


    public NullStorageLayout(OSDConfig config, MetadataCache cache) throws IOException {
        super(config, cache);
    }

    @Override
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
        FileMetadata info = fileCache.get(fileId);
        if (info == null) {
            info = new FileMetadata(sp);
            // File fileDir = new File(HashStorageLayout.generateAbsoluteFilePath(fileId));

            info.setFilesize(0);
            info.setLastObjectNumber(-1);
            info.initLatestObjectVersions(new HashMap<Long, Long>());
            info.initLargestObjectVersions(new HashMap<Long, Long>());
            info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
            // info.initVersionTable(new VersionTable(new File(fileDir, VTABLE_FILENAME)));
            info.setECCurVector(new AVLTreeIntervalVector());
            info.setECNextVector(new AVLTreeIntervalVector());


            info.setGlobalLastObjectNumber(-1);
            fileCache.put(fileId, info);
        }
        return info;
    }

    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length,
            long version) throws IOException {
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        ReusableBuffer bbuf = BufferPool.allocate(length);
        ObjectInformation oInfo = new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, bbuf, stripeSize);
        return oInfo;
    }

    @Override
    public void writeObject(String fileId, FileMetadata md, ReusableBuffer data, long objNo, int offset,
            long newVersion, boolean sync, boolean cow) throws IOException {
        BufferPool.free(data);
        md.updateObjectVersion(objNo, newVersion);
    }

    @Override
    public void truncateObject(String fileId, FileMetadata md, long objNo, int newLength, long newVersion, boolean cow)
            throws IOException {
        md.updateObjectVersion(objNo, newVersion);
    }

    @Override
    public void deleteFile(String fileId, boolean deleteMetadata) throws IOException {
    }

    @Override
    public void deleteObject(String fileId, FileMetadata md, long objNo, long version) throws IOException {
    }

    @Override
    public void createPaddingObject(String fileId, FileMetadata md, long objNo, long version, int size)
            throws IOException {
        md.updateObjectVersion(objNo, version);
    }

    @Override
    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
    }

    @Override
    public boolean fileExists(String fileId) {
        return true;
    }

    @Override
    public void updateCurrentObjVersion(String fileId, long objNo, long newVersion) throws IOException {
    }

    @Override
    public void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException {
    }

    @Override
    public long getFileInfoLoadCount() {
        return 0;
    }

    @Override
    public ObjectSet getObjectSet(String fileId, FileMetadata md) {
        return new ObjectSet(0);
    }

    @Override
    public FileList getFileList(FileList l, int maxNumEntries) {
        return new FileList(new Stack<String>(), new HashMap<String, FileData>());
    }

    @Override
    public int getLayoutVersionTag() {
        return 4;
    }

    @Override
    public boolean isCompatibleVersion(int layoutVersionTag) {
        return (layoutVersionTag == 4);
    }

    @Override
    public int getMasterEpoch(String fileId) throws IOException {
        Integer epoch = mepochCache.get(fileId);
        if (epoch != null)
            return epoch;
        return 0;
    }

    @Override
    public void setMasterEpoch(String fileId, int masterEpoch) throws IOException {
        mepochCache.put(fileId, masterEpoch);
    }

    @Override
    public TruncateLog getTruncateLog(String fileId) throws IOException {
        return TruncateLog.newBuilder().build();
    }

    @Override
    public void setTruncateLog(String fileId, TruncateLog log) throws IOException {
    }

    @Override
    public ArrayList<String> getFileIDList() {
        return new ArrayList<String>();
    }

    @Override
    public XLocSetVersionState getXLocSetVersionState(String fileId) throws IOException {
        return XLocSetVersionState.newBuilder().setInvalidated(false).setVersion(0).build();
    }

    @Override
    public void setXLocSetVersionState(String fileId, XLocSetVersionState versionState) throws IOException {
    }

    @Override
    public Set<String> getInvalidClientExpireTimeSet(String fileId) throws IOException {
        return new TreeSet<String>();
    }

    @Override
    public void setInvalidClientExpireTimeSet(String fileId, Set<String> invalidClientExpireTimeSet)
            throws IOException {
    }

    @Override
    public void setECIntervalVector(String fileId, List<Interval> intervals, boolean next, boolean append)
            throws IOException {
    }

    @Override
    public boolean getECIntervalVector(String fileId, boolean next, IntervalVector vector) throws IOException {
        return true;
    }

}
