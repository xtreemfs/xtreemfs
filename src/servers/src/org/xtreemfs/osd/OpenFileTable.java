/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 and Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 *          Eugenio Cesario (CNR)
 */

package org.xtreemfs.osd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.xtreemfs.foundation.ClientLease;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * This class models an OpenFileTable, storing the set of files in an 'open'
 * state; it makes available a 'clean' method that cleans the table by deleting
 * entries whose expiration time is expired
 * 
 * @author Eugenio Cesario
 */
public final class OpenFileTable {
    
    private HashMap<String, OpenFileTableEntry> openFiles;
    
    private PriorityQueue<OpenFileTableEntry>   expTimes;
    
    private PriorityQueue<OpenFileTableEntry>   expTimesWrite;
    
    // constructor
    public OpenFileTable() {
        openFiles = new HashMap<String, OpenFileTableEntry>();
        expTimes = new PriorityQueue<OpenFileTableEntry>();
        expTimesWrite = new PriorityQueue<OpenFileTableEntry>();
    }
    
    /**
     * Insert a new entry in the table
     * 
     * @param fId
     *            fileId
     * @param expTime
     *            expiration time
     */
    public CowPolicy refresh(String fId, long expTime, boolean write) {
        OpenFileTableEntry currEntry = openFiles.get(fId);
        
        if (currEntry != null) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "refreshing for %s", fId);
            
            // 'currEntry' isn't a new entry, so update it
            // if its expiration time is renewed
            if (expTime > currEntry.expTime) {
                
                // openFiles.remove(fId);
                expTimes.remove(currEntry);
                
                if(write)
                    expTimesWrite.remove(currEntry);

                currEntry.setExpirationTime(expTime);
                openFiles.put(fId, currEntry);
                expTimes.add(currEntry);

                if(write)
                    expTimesWrite.add(currEntry);
            }
            return currEntry.getCowPolicy();
        } else {
            assert (false) : "should never get here!";
            Logging.logMessage(Logging.LEVEL_ERROR, this,
                "ARGH!!!! SHOULD NOT REFRESH FOR NON-OPEN FILE ANYMORE!!!!!");
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new entry for %s", fId);
            // 'currEntry' is a new entry, so
            // insert it in the table
            OpenFileTableEntry newEntry = new OpenFileTableEntry(fId, expTime);
            openFiles.put(fId, newEntry);
            expTimes.add(newEntry);
            return null;
        }
    }
    
    /**
     * Insert a new entry in the table
     * 
     * @param fId
     *            fileId
     * @param expTime
     *            expiration time
     */
    public void openFile(String fId, long expTime, CowPolicy policy, boolean write) {
        assert (openFiles.containsKey(fId) == false);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new entry for %s", fId);
        // 'currEntry' is a new entry, so
        // insert it in the table
        OpenFileTableEntry newEntry = new OpenFileTableEntry(fId, expTime, policy);
        OpenFileTableEntry newWriteEntry = new OpenFileTableEntry(fId, expTime, policy);
        openFiles.put(fId, newEntry);
        expTimes.add(newEntry);
        if(write)
            expTimesWrite.add(newWriteEntry);
    }
    
    /**
     * Returns 'true' if this table contains the specified file, 'false'
     * otherwise
     */
    public boolean contains(String fId) {
        return openFiles.containsKey(fId);
    }
    
    /**
     * Delete all the entries whose expiration time is strictly less than
     * 'toTime'.
     */
    public List<OpenFileTableEntry> clean(long toTime) {
        
        LinkedList<OpenFileTableEntry> closedFiles = new LinkedList<OpenFileTableEntry>();
        
        OpenFileTableEntry dummyEntry = new OpenFileTableEntry(null, toTime);
        
        Iterator it = expTimes.iterator();
        
        // since entries in 'expTimes' are sorted w.r.t. their expiration time
        // (ascending order), 'expTimes' has to be scanned until there is an
        // entry
        // with its 'expTimes' > 'toTime'
        
        while (it.hasNext()) {
            
            OpenFileTableEntry currEntry = (OpenFileTableEntry) it.next();
            
            if (currEntry.compareTo(dummyEntry) < 0) {
                String fId = currEntry.fileId;
                openFiles.remove(fId);
                it.remove();
                closedFiles.add(currEntry);
            } else {
                break;
            }
        }
        return closedFiles;
    }
    
    /**
     * Deletes all entries of files that were opened for writing whose
     * expiration time is strictly less than 'toTime'.
     * 
     * @param toTime
     * @return
     */
    public List<OpenFileTableEntry> cleanWritten(long toTime) {
        
        List<OpenFileTableEntry> closedFiles = new LinkedList<OpenFileTableEntry>();
        
        OpenFileTableEntry dummyEntry = new OpenFileTableEntry(null, toTime);
        
        Iterator<OpenFileTableEntry> it = expTimesWrite.iterator();
        
        while (it.hasNext()) {
            
            OpenFileTableEntry currEntry = it.next();
            
            if (currEntry.compareTo(dummyEntry) < 0) {
                String fId = currEntry.fileId;
                openFiles.remove(fId);
                it.remove();
                closedFiles.add(currEntry);
            } else {
                break;
            }
        }
        
        return closedFiles;
        
    }
    
    /**
     * It tells if a file was closed
     * 
     * @param fileId
     *            File to consult
     * @return true if the file is closed
     */
    public boolean isClosed(String fileId) {
        OpenFileTableEntry fileEntry = openFiles.get(fileId);
        
        if (fileEntry != null) {
            if (fileEntry.expTime < (System.currentTimeMillis() / 1000)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
    
    public void setDeleteOnClose(String fileId) {
        OpenFileTableEntry fileEntry = openFiles.get(fileId);
        
        if (fileEntry != null) {
            fileEntry.setDeleteOnClose();
        }
    }
    
    /**
     * 
     * @param fileId
     * @return true if the file with the given id is set to be deleted on close,
     *         false otherwise.
     */
    public boolean isDeleteOnClose(String fileId) {
        OpenFileTableEntry fileEntry = openFiles.get(fileId);
        
        if (fileEntry != null) {
            return fileEntry.isDeleteOnClose();
        }
        
        return false;
    }
    
    public void close(String fileId) {
        OpenFileTableEntry currEntry = openFiles.get(fileId);
        
        if (currEntry != null) {
            expTimes.remove(currEntry);
            openFiles.remove(fileId);
        }
    }
    
    public int getNumOpenFiles() {
        return this.openFiles.size();
    }

    public OpenFileTableEntry getEntry(String fileId) {
        return this.openFiles.get(fileId);
    }
    
    /*public List<ClientLease> getLeases(String fileId) {
        OpenFileTableEntry e = openFiles.get(fileId);
        assert (e != null);
        return e.getClientLeases();
    }*/
    
    /**
     * Class used to model an entry in the OpenFileTable
     * 
     * @author Eugenio Cesario
     * 
     */
    public static class OpenFileTableEntry implements Comparable {
        
        private final String      fileId;
        
        private long              expTime;
        
        private boolean           deleteOnClose;
        
        //private List<ClientLease> clientLeases;

        private Map<String,AdvisoryLock> advisoryLocks;
        
        private CowPolicy         fileCowPolicy;
        
        public OpenFileTableEntry(String fid, long et) {
            this(fid, et, null);
        }
        
        public OpenFileTableEntry(String fid, long et, CowPolicy cow) {
            fileId = fid;
            expTime = et;
            deleteOnClose = false;
            //clientLeases = new LinkedList();
            
            if (cow != null)
                fileCowPolicy = cow;
            else
                fileCowPolicy = new CowPolicy(CowPolicy.cowMode.NO_COW);
        }
        
        public int compareTo(Object o) {
            int res = 0;
            final OpenFileTableEntry e = (OpenFileTableEntry) o;
            if (this.expTime < e.expTime) {
                res = -1;
            } else if (this.expTime == e.expTime) {
                res = 0;
            } else {
                res = 1;
            }
            return res;
        }
        
        public boolean equals(Object o) {
            try {
                final OpenFileTableEntry e = (OpenFileTableEntry) o;
                if (fileId.equals(e.fileId)) {
                    return true;
                } else {
                    return false;
                }
            } catch (ClassCastException ex) {
                return false;
            }
        }
        
        public String toString() {
            return "(" + fileId + "," + expTime + ")";
        }
        
        public void setExpirationTime(long newExpTime) {
            this.expTime = newExpTime;
        }
        
        public void setDeleteOnClose() {
            deleteOnClose = true;
        }
        
        public boolean isDeleteOnClose() {
            return deleteOnClose;
        }
        
        public String getFileId() {
            return this.fileId;
        }
        
        /*public List<ClientLease> getClientLeases() {
            return this.clientLeases;
        }*/
        
        public CowPolicy getCowPolicy() {
            return this.fileCowPolicy;
        }

        public AdvisoryLock acquireLock(String client_uuid, int pid, long offset, long length, boolean exclusive) {
            final String procId = getProcId(client_uuid, pid);
            final AdvisoryLock l = new AdvisoryLock(offset, length, exclusive, client_uuid, pid);
            if (advisoryLocks == null) {
                advisoryLocks = new HashMap();
                //don't need to look for new locks here
                advisoryLocks.put(procId, l);

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG,Category.all,this,"acquired lock for file %s by %s (%d-%d)",fileId,procId,offset,length);
                }

                return l;
            } else {

                for (Entry<String,AdvisoryLock> e : advisoryLocks.entrySet()) {
                    if (e.getKey().equals(procId))
                        continue;
                    if (e.getValue().isConflicting(l)) {
                        assert( (e.getValue().getClientPid() != l.getClientPid())
                                || (!e.getValue().getClientUuid().equals(l.getClientUuid()))) : procId+" vs "+e.getKey();
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG,Category.all,this,"acquired for file %s by %s failed, conflickting lock by",fileId,procId,e.getKey());
                        }
                        return null;
                    }
                }
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG,Category.all,this,"acquired lock for file %s by %s (%d-%d)",fileId,procId,offset,length);
                }
                advisoryLocks.put(procId, l);
                return l;
            }
        }

        public AdvisoryLock checkLock(String client_uuid, int pid, long offset, long length, boolean exclusive) {
            final String procId = getProcId(client_uuid, pid);
            final AdvisoryLock l = new AdvisoryLock(offset, length, exclusive, client_uuid, pid);
            if (advisoryLocks == null) {
                return l;
            } else {

                for (Entry<String,AdvisoryLock> e : advisoryLocks.entrySet()) {
                    if (e.getKey().equals(procId))
                        return l;
                    if (e.getValue().isConflicting(l)) {
                        return e.getValue();
                    }
                }
                return l;
            }
        }

        public void unlock(String client_uuid, int pid) {
            if (advisoryLocks == null)
                return;
            final String procId = getProcId(client_uuid, pid);
            advisoryLocks.remove(procId);
        }


        private static String getProcId(String clientUuid, int pid) {
            return String.format("%010d%s",pid,clientUuid);
        }
        
    }
}
