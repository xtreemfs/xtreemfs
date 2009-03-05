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

package org.xtreemfs.new_osd.storage;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author bjko
 */
public class FileInfo {

    private Map<Long, Integer> objVersions;

    private Map<Long, String>  objChecksums;

    private long               filesize;

    private long               lastObjectNumber;

    private boolean            incVersionOnWrite;

    private long               truncateEpoch;

    /** Creates a new instance of FileInfo */
    public FileInfo() {
        objVersions = new HashMap<Long, Integer>();
        objChecksums = new HashMap<Long, String>();
    }

    public Map<Long, Integer> getObjVersions() {
        return objVersions;
    }

    public Map<Long, String> getObjChecksums() {
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

    public int getObjectVersion(long objId) {
        Integer v = objVersions.get(objId);
        return (v == null) ? 0 : v;
    }

    public String getObjectChecksum(long objId) {
        String c = objChecksums.get(objId);
        return c;
    }

    public boolean isIncVersionOnWrite() {
        return incVersionOnWrite;
    }

    public void setIncVersionOnWrite(boolean incVersionOnWrite) {
        this.incVersionOnWrite = incVersionOnWrite;
    }

    public void deleteObject(long objId) {
        objVersions.remove(objId);
        objChecksums.remove(objId);
    }

    public String toString() {
        return "fileSize=" + filesize + ", lastObjNo=" + lastObjectNumber + ", incVersionOnWrite="
            + incVersionOnWrite;
    }

    public long getTruncateEpoch() {
        return truncateEpoch;
    }

    public void setTruncateEpoch(long truncateEpoch) {
        this.truncateEpoch = truncateEpoch;
    }
}
