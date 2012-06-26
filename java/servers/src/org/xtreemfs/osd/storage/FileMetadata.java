/*
 * Copyright (c) 2008-2011 by Eugenio Cesario, Bjoern Kolbeck, Christian Lorenz
 *               Zuse Institute Berlin, Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.osd.storage.VersionManager.ObjectVersionInfo;

/**
 * 
 * @author bjko
 */
public class FileMetadata {

    private VersionManager           versionManager;

    /**
     * contains the current size of the file in bytes
     */
    private long                     filesize;

    /**
     * contains the last local object number of the file
     */
    private long                     lastObjectNumber;

    /**
     * contains the last object number of the file that is locally known
     */
    private long                     globalLastObjectNumber;

    /**
     * contains the current truncate epoch of the file
     */
    private long                     truncateEpoch;

    /**
     * reference to the striping policy of the file
     */
    private final StripingPolicyImpl stripingPolicy;

    private RandomAccessFile[]       handles;

    private long                     mdFileLength;

    /** Creates a new instance of FileInfo */
    public FileMetadata(StripingPolicyImpl sp, FileVersionLog fileVersionLog, boolean versioningEnabled) {
        stripingPolicy = sp;
        versionManager = new VersionManager(fileVersionLog, versioningEnabled);
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public long getLastObjectNumber() {
        return lastObjectNumber;
    }

    public void setLastObjectNumber(long lastObjectNumber) {
        this.lastObjectNumber = lastObjectNumber;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public String toString() {
        return "fileSize=" + filesize + ", lastObjNo=" + lastObjectNumber;
    }

    public long getTruncateEpoch() {
        return truncateEpoch;
    }

    public void setTruncateEpoch(long truncateEpoch) {
        this.truncateEpoch = truncateEpoch;
    }

    /**
     * @return the globalLastObjectNumber
     */
    public long getGlobalLastObjectNumber() {
        return globalLastObjectNumber;
    }

    /**
     * @param globalLastObjectNumber
     *            the globalLastObjectNumber to set
     */
    public void setGlobalLastObjectNumber(long globalLastObjectNumber) {
        this.globalLastObjectNumber = globalLastObjectNumber;
    }

    /**
     * @return the stripingPolicy
     */
    public StripingPolicyImpl getStripingPolicy() {
        return stripingPolicy;
    }

    /**
     * @return the handles
     */
    public RandomAccessFile[] getHandles() {
        return handles;
    }

    /**
     * @param handles
     *            the handles to set
     */
    public void setHandles(RandomAccessFile[] handles) {
        this.handles = handles;
    }

    /**
     * @return the mdFileLength
     */
    public long getMdFileLength() {
        return mdFileLength;
    }

    /**
     * @param mdFileLength
     *            the mdFileLength to set
     */
    public void setMdFileLength(long mdFileLength) {
        this.mdFileLength = mdFileLength;
    }

}
