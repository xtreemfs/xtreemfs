/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;

/**
 * This StorageLayout does neither write nor read data. It is intended for benchmarking purposes only.
 */
public class NullStorageLayout extends StorageLayout {

    public static class NullVersionTable extends VersionTable {
        @Override
        protected void doLoad() throws IOException { }

        @Override
        protected void doSave() throws IOException { }
    }

    public NullStorageLayout(OSDConfig config, MetadataCache cache) {
        super(cache);
    }

    @Override
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
        FileMetadata info = new FileMetadata(sp);
        info.setFilesize(0);
        info.setLastObjectNumber(-1);
        info.initLatestObjectVersions(new HashMap<Long, Long>());
        info.initLargestObjectVersions(new HashMap<Long, Long>());
        info.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
        info.initVersionTable(new NullVersionTable());
        info.setGlobalLastObjectNumber(-1);

        return info;
    }

    @Override
    public void closeFile(FileMetadata metadata) {
    }

    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length, long version)
            throws IOException {
        final int stripeSize = md.getStripingPolicy().getStripeSizeForObject(objNo);
        ReusableBuffer buffer = BufferPool.allocate(length);

        return new ObjectInformation(ObjectInformation.ObjectStatus.EXISTS, buffer, stripeSize);
    }

    @Override
    public void writeObject(String fileId, FileMetadata md, ReusableBuffer data, long objNo, int offset,
            long newVersion, boolean sync, boolean cow) throws IOException {
        md.updateObjectVersion(objNo, newVersion);
        BufferPool.free(data);
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
        return 0;
    }

    @Override
    public boolean isCompatibleVersion(int layoutVersionTag) {
        return true;
    }

    @Override
    public int getMasterEpoch(String fileId) throws IOException {
        return 0;
    }

    @Override
    public void setMasterEpoch(String fileId, int masterEpoch) throws IOException {
    }

    @Override
    public TruncateLog getTruncateLog(String fileId) throws IOException {
        return TruncateLog.getDefaultInstance();
    }

    @Override
    public void setTruncateLog(String fileId, TruncateLog log) throws IOException {
    }

    @Override
    public ArrayList<String> getFileIDList() {
        return new ArrayList<String>();
    }

}
