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

import java.util.HashMap;
import java.util.Map;
import org.xtreemfs.common.xloc.StripingPolicyImpl;

/**
 *
 * @author bjko
 */
public class FileMetadata {

    private Map<Long, Long>     objVersions;

    private Map<Long, Long>     objChecksums;

    private long               filesize;

    private long               lastObjectNumber;

    private long               globalLastObjectNumber;

    private long               truncateEpoch;

    private final StripingPolicyImpl stripingPolicy;
    
    private Map<Long, int[]> fileVersions;

    /** Creates a new instance of FileInfo */
    public FileMetadata(StripingPolicyImpl sp) {
        objVersions = new HashMap<Long, Long>();
        objChecksums = new HashMap<Long, Long>();
        fileVersions = new HashMap<Long, int[]>();
        stripingPolicy = sp;
    }

    public Map<Long, Long> getObjVersions() {
        return objVersions;
    }

    public Map<Long, Long> getObjChecksums() {
        return objChecksums;
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

    public long getObjectVersion(long objId) {
        Long v = objVersions.get(objId);
        return (v == null) ? 0 : v;
    }

    public Long getObjectChecksum(long objId) {
        Long c = objChecksums.get(objId);
        return (c == null) ? 0 : c;
    }

    public void deleteObject(long objId) {
        objVersions.remove(objId);
        objChecksums.remove(objId);
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
     * @param globalLastObjectNumber the globalLastObjectNumber to set
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

    public Map<Long, int[]> getFileVersions() {
        return fileVersions;
    }

    public void setFileVersions(Map<Long, int[]> fileVersions) {
        this.fileVersions = fileVersions;
    }
    
}
