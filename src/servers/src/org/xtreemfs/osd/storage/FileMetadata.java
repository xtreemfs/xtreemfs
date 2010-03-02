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

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.xloc.StripingPolicyImpl;

/**
 * 
 * @author bjko
 */
public class FileMetadata {
    
    private Map<Long, Long>          latestObjVersions;
    
    private Map<Long, Long>          largestObjVersions;
    
    private Map<String, Long>        objChecksums;
    
    private long                     filesize;
    
    private long                     lastObjectNumber;
    
    private long                     globalLastObjectNumber;
    
    private long                     truncateEpoch;
    
    private final StripingPolicyImpl stripingPolicy;
    
    private VersionTable             versionTable;
    
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
        Long c = objChecksums.get(objId + "." + objVer);
        return (c == null) ? 0 : c;
    }
    
    public Set<Entry<Long, Long>> getLatestObjectVersions() {
        return latestObjVersions.entrySet();
    }
    
    public void clearLatestObjectVersions() {
        latestObjVersions.clear();
    }
    
    public void initLargestObjectVersions(Map<Long, Long> largestObjVersions) {
        assert (this.largestObjVersions == null);
        this.largestObjVersions = largestObjVersions;
    }
    
    public void initLatestObjectVersions(Map<Long, Long> latestObjVersions) {
        assert (this.latestObjVersions == null);
        this.latestObjVersions = latestObjVersions;
    }
    
    public void initObjectChecksums(Map<String, Long> objChecksums) {
        assert (this.objChecksums == null);
        this.objChecksums = objChecksums;
    }
    
    public void initVersionTable(VersionTable versionTable) {
        assert (this.versionTable == null);
        this.versionTable = versionTable;
    }
    
    public void updateObjectVersion(long objId, long newVersion) {
        
        latestObjVersions.put(objId, newVersion);
        
        if (largestObjVersions != latestObjVersions && newVersion != 0)
            largestObjVersions.put(objId, newVersion);
        
    }
    
    public void updateObjectChecksum(long objId, long objVer, long newChecksum) {
        objChecksums.put(objId + "." + objVer, newChecksum);
    }
    
    public void discardObject(long objId, long objVer) {
        latestObjVersions.remove(objId);
        objChecksums.remove(objId + "." + objVer);
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
    
    public VersionTable getVersionTable() {
        return versionTable;
    }
    
}
