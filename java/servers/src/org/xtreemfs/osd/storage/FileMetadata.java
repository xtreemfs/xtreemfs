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
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xtreemfs.common.xloc.StripingPolicyImpl;

/**
 * 
 * @author bjko
 */
public class FileMetadata {
    
    private Map<Long, Long>            latestObjVersions;
    
    private Map<Long, Long>            largestObjVersions;
    
    private Map<Long, SortedSet<Long>> existingObjVersions;

    private Map<Long, Map<Long, Long>> objChecksums;
    
    private long                       filesize;
    
    private long                       lastObjectNumber;
    
    private long                       globalLastObjectNumber;
    
    private long                       truncateEpoch;
    
    private final StripingPolicyImpl   stripingPolicy;
    
    private VersionTable               versionTable;

    private RandomAccessFile[]       handles;

    private long                     mdFileLength;
    
    /** Creates a new instance of FileInfo */
    public FileMetadata(StripingPolicyImpl sp) {
        stripingPolicy = sp;
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
    
    public long getLargestObjectVersion(long objId) {
        Long v = largestObjVersions.get(objId);
        return (v == null) ? 0 : v;
    }
    
    public long getLatestObjectVersion(long objId) {
        Long v = latestObjVersions.get(objId);
        return (v == null) ? 0 : v;
    }
    
    public Long getObjectChecksum(long objId, long objVer) {
        
        Map<Long, Long> checksums = objChecksums.get(objId);
        if (checksums == null)
            return 0L;
        
        Long c = checksums.get(objVer);
        return (c == null) ? 0 : c;
    }
    
    public Set<Entry<Long, Long>> getLatestObjectVersions() {
        return latestObjVersions.entrySet();
    }
    
    public SortedSet<Long> getExistingObjectVersions(long objId) {
        SortedSet<Long> v = existingObjVersions.get(objId);
        return (v == null) ? new TreeSet<Long>() : v;
    }

    public void clearLatestObjectVersions() {
        latestObjVersions.clear();
    }

    public void clearExistingObjectVersions() {
        existingObjVersions.clear();
    }
    
    public void initLargestObjectVersions(Map<Long, Long> largestObjVersions) {
        assert (this.largestObjVersions == null);
        this.largestObjVersions = largestObjVersions;
    }
    
    public void initLatestObjectVersions(Map<Long, Long> latestObjVersions) {
        assert (this.latestObjVersions == null);
        this.latestObjVersions = latestObjVersions;
    }
    
    public void initExistingObjectVersions(Map<Long, SortedSet<Long>> existingObjVersions) {
        assert (this.existingObjVersions == null);
        this.existingObjVersions = existingObjVersions;
    }

    public void initObjectChecksums(Map<Long, Map<Long, Long>> objChecksums) {
        assert (this.objChecksums == null);
        this.objChecksums = objChecksums;
    }
    
    public void initVersionTable(VersionTable versionTable) {
        assert (this.versionTable == null);
        this.versionTable = versionTable;
    }
    
    public void updateObjectVersion(long objId, long newVersion) {
        
        latestObjVersions.put(objId, newVersion);
        if (newVersion != 0 && newVersion > getLargestObjectVersion(objId))
            largestObjVersions.put(objId, newVersion);
        SortedSet<Long> versions = existingObjVersions.get(objId);
        if (versions == null) {
            versions = new TreeSet<Long>();
            existingObjVersions.put(objId, versions);
        }
        versions.add(newVersion);
        
    }
    
    public void updateObjectChecksum(long objId, long objVer, long newChecksum) {
        
        Map<Long, Long> checksums = objChecksums.get(objId);
        if (checksums == null) {
            checksums = new HashMap<Long, Long>();
            objChecksums.put(objId, checksums);
        }
        
        checksums.put(objVer, newChecksum);
    }
    
    public void discardObject(long objId, long objVer) {
        latestObjVersions.remove(objId);
        objChecksums.remove(objId + "." + objVer);
        if (existingObjVersions.containsKey(objId))
            existingObjVersions.get(objId).remove(objVer);
    }
    
    public String toString() {
        return "(fileSize=" + filesize + ", lastObjNo=" + lastObjectNumber + ")";
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
    
    public VersionTable getVersionTable() {
        return versionTable;
    }

    /**
     * @return the handles
     */
    public RandomAccessFile[] getHandles() {
        return handles;
    }

    /**
     * @param handles the handles to set
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
     * @param mdFileLength the mdFileLength to set
     */
    public void setMdFileLength(long mdFileLength) {
        this.mdFileLength = mdFileLength;
    }
    
}
