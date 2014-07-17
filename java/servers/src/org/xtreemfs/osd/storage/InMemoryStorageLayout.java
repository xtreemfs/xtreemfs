/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.ArrayList;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;

/** Performance oriented StorageLayout, which keeps everything in memory. */
public class InMemoryStorageLayout extends StorageLayout {

    /**
     * @param cache
     */
    public InMemoryStorageLayout(MetadataCache cache) {
        super(cache);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected FileMetadata loadFileMetadata(String fileId, StripingPolicyImpl sp) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void closeFile(FileMetadata metadata) {
        // TODO Auto-generated method stub

    }

    @Override
    public ObjectInformation readObject(String fileId, FileMetadata md, long objNo, int offset, int length, long version)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeObject(String fileId, FileMetadata md, ReusableBuffer data, long objNo, int offset,
            long newVersion, boolean sync, boolean cow) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void truncateObject(String fileId, FileMetadata md, long objNo, int newLength, long newVersion, boolean cow)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteFile(String fileId, boolean deleteMetadata) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteObject(String fileId, FileMetadata md, long objNo, long version) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void createPaddingObject(String fileId, FileMetadata md, long objNo, long version, int size)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTruncateEpoch(String fileId, long newTruncateEpoch) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean fileExists(String fileId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateCurrentObjVersion(String fileId, long objNo, long newVersion) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCurrentVersionSize(String fileId, long newLastObject) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public long getFileInfoLoadCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ObjectSet getObjectSet(String fileId, FileMetadata md) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileList getFileList(FileList l, int maxNumEntries) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLayoutVersionTag() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isCompatibleVersion(int layoutVersionTag) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getMasterEpoch(String fileId) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setMasterEpoch(String fileId, int masterEpoch) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public TruncateLog getTruncateLog(String fileId) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTruncateLog(String fileId, TruncateLog log) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public ArrayList<String> getFileIDList() {
        // TODO Auto-generated method stub
        return null;
    }

}
