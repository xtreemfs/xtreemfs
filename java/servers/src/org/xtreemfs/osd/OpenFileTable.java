/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *                            Eugenio Cesario,
 *                            Zuse Institute Berlin,
 *                            Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.operations.EventCreateFileVersion;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * This class models an OpenFileTable, storing the set of files in an 'open' state; it makes available a 'clean' method
 * that cleans the table by deleting entries whose expiration time is expired
 * 
 * @author Eugenio Cesario
 */
public final class OpenFileTable {

    private final HashMap<String, OpenFileTableEntry> openFiles;

    private final SortedSet<OpenFileTableEntry>       expTimes;

    private final SortedSet<OpenFileTableEntry>       expTimesWrite;

    // constructor
    public OpenFileTable() {
        openFiles = new HashMap<String, OpenFileTableEntry>();
        expTimes = new TreeSet<OpenFileTableEntry>();
        expTimesWrite = new TreeSet<OpenFileTableEntry>(OpenFileTableEntry.createWriteExpComparator());
    }

    /**
     * Insert a new entry in the table
     * 
     * @param fId
     *            fileId
     * @param expTime
     *            expiration time
     * @param write
     *            true if this file should be copied on write
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

                currEntry.setExpirationTime(expTime);
                openFiles.put(fId, currEntry);
                boolean success = expTimes.add(currEntry);
                assert (success);

                if (write) {
                    expTimesWrite.remove(currEntry);
                    currEntry.setWriteExpirationTime(expTime);
                    currEntry.setWrite();
                    expTimesWrite.add(currEntry);
                }
            }
            return currEntry.getCowPolicy();
        } else {
            Logging.logMessage(Logging.LEVEL_WARN, this,
                    "attempt to keep file %s open failed because no open state exists anymore", fId);
            // Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new entry for %s", fId);
            // // 'currEntry' is a new entry, so
            // // insert it in the table
            // OpenFileTableEntry newEntry = new OpenFileTableEntry(fId, expTime);
            // openFiles.put(fId, newEntry);
            // expTimes.add(newEntry);
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
     * @param write
     *            true if this file should be copied on write
     */
    public void openFile(String fId, long expTime, CowPolicy policy, boolean write) {
        assert (openFiles.containsKey(fId) == false);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new entry for %s", fId);
        // 'currEntry' is a new entry, so
        // insert it in the table
        OpenFileTableEntry newEntry = new OpenFileTableEntry(fId, expTime, policy);
        openFiles.put(fId, newEntry);
        expTimes.add(newEntry);

        if (write) {
            newEntry.setWriteExpirationTime(expTime);
            newEntry.setWrite();
            expTimesWrite.add(newEntry);
        }
    }

    /**
     * Returns 'true' if this table contains the specified file, 'false' otherwise
     */
    public boolean contains(String fId) {
        return openFiles.containsKey(fId);
    }

    /**
     * Delete all the entries whose expiration time is strictly less than 'toTime'.
     */
    public List<OpenFileTableEntry> clean(long toTime) {

        LinkedList<OpenFileTableEntry> closedFiles = new LinkedList<OpenFileTableEntry>();

        OpenFileTableEntry dummyEntry = new OpenFileTableEntry(null, toTime);

        Iterator<OpenFileTableEntry> it = expTimes.iterator();

        // since entries in 'expTimes' are sorted w.r.t. their expiration time
        // (ascending order), 'expTimes' has to be scanned until there is an
        // entry with its 'expTimes' > 'toTime'

        while (it.hasNext()) {

            OpenFileTableEntry currEntry = (OpenFileTableEntry) it.next();

            if (currEntry.compareTo(dummyEntry) < 0) {
                String fId = currEntry.fileId;
                openFiles.remove(fId);
                it.remove();
                currEntry.setClosed();
                closedFiles.add(currEntry);
            } else {
                break;
            }
        }
        return closedFiles;
    }

    /**
     * Deletes all entries of files that were opened for writing whose expiration time is strictly less than 'toTime'.
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
     * @return true if the file with the given id is set to be deleted on close, false otherwise.
     */
    public boolean isDeleteOnClose(String fileId) {
        OpenFileTableEntry fileEntry = openFiles.get(fileId);

        if (fileEntry != null) {
            return fileEntry.isDeleteOnClose();
        }

        return false;
    }

    /**
     * Remove the entries for fileId from the OpenFileTable.
     * 
     * @param fileId
     * @return the removed {@link OpenFileTableEntry} or null if it did not exist.
     */
    public OpenFileTableEntry close(String fileId) {

        OpenFileTableEntry currEntry = openFiles.get(fileId);

        if (currEntry != null) {
            expTimes.remove(currEntry);
            expTimesWrite.remove(currEntry);
            openFiles.remove(fileId);
            currEntry.setClosed();
        }

        return currEntry;
    }

    public int getNumOpenFiles() {
        return this.openFiles.size();
    }

    public OpenFileTableEntry getEntry(String fileId) {
        return this.openFiles.get(fileId);
    }

    /**
     * Class used to model an entry in the OpenFileTable
     * 
     * @author Eugenio Cesario
     * 
     */
    public static class OpenFileTableEntry implements Comparable<OpenFileTableEntry> {

        private final String              fileId;

        private final CowPolicy           fileCowPolicy;

        private long                      expTime;

        private long                      writeExpTime;

        private Map<String, AdvisoryLock> advisoryLocks;

        private boolean                   closed;

        private boolean                   write;

        private boolean                   deleteOnClose;

        private OpenFileTableEntry(String fid, long et) {
            this(fid, et, null);
        }

        private OpenFileTableEntry(String fid, long et, CowPolicy cow) {
            fileId = fid;
            expTime = et;
            writeExpTime = 0;

            closed = false;
            deleteOnClose = false;
            write = false;

            if (cow != null)
                fileCowPolicy = cow;
            else
                fileCowPolicy = new CowPolicy(CowPolicy.cowMode.NO_COW);
        }

        private void setExpirationTime(long newExpTime) {
            this.expTime = newExpTime;
        }

        private void setWriteExpirationTime(long newExpTime) {
            this.writeExpTime = newExpTime;
        }

        /**
         * Thread-safe method (access to final field).
         * 
         * @return the fileId of this entry.
         */
        public String getFileId() {
            return this.fileId;
        }

        /**
         * Thread-safe method (access to final field).
         * 
         * @return the cowPolicy of this entry.
         */
        public CowPolicy getCowPolicy() {
            return this.fileCowPolicy;
        }

        /**
         * @return if the entry has been removed from the OpenFileTable.
         */
        public boolean isClosed() {
            return closed;
        }

        /**
         * Mark that the entry has been removed from the OpenFileTable.
         */
        private void setClosed() {
            closed = true;
        }

        /**
         * Mark the file for deletion on close.<br>
         */
        private void setDeleteOnClose() {
            deleteOnClose = true;
        }

        /**
         * @return true if the file should be deleted on close.
         */
        public boolean isDeleteOnClose() {
            return deleteOnClose;
        }

        /**
         * Mark pending copies on write for the file. <br>
         */
        private void setWrite() {
            write = true;
        }

        /**
         * Clear the write flag once a {@link EventCreateFileVersion} has been triggered.
         */
        public void clearWrite() {
            write = false;
        }

        /**
         * @return true if the file should be copied after a write.
         */
        public boolean isWrite() {
            return write;
        }

        public AdvisoryLock acquireLock(String client_uuid, int pid, long offset, long length, boolean exclusive) {
            final String procId = getProcId(client_uuid, pid);
            final AdvisoryLock l = new AdvisoryLock(offset, length, exclusive, client_uuid, pid);
            if (advisoryLocks == null) {
                advisoryLocks = new HashMap();
                // don't need to look for new locks here
                advisoryLocks.put(procId, l);

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.all, this,
                            "acquired lock for file %s by %s (%d-%d)", fileId, procId, offset, length);
                }

                return l;
            } else {

                for (Entry<String, AdvisoryLock> e : advisoryLocks.entrySet()) {
                    if (e.getKey().equals(procId))
                        continue;
                    if (e.getValue().isConflicting(l)) {
                        assert ((e.getValue().getClientPid() != l.getClientPid()) || (!e.getValue().getClientUuid()
                                .equals(l.getClientUuid()))) : procId + " vs " + e.getKey();
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.all, this,
                                    "acquired for file %s by %s failed, conflickting lock by", fileId, procId,
                                    e.getKey());
                        }
                        return null;
                    }
                }
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.all, this,
                            "acquired lock for file %s by %s (%d-%d)", fileId, procId, offset, length);
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

                for (Entry<String, AdvisoryLock> e : advisoryLocks.entrySet()) {
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
            return String.format("%010d%s", pid, clientUuid);
        }

        @Override
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

        @Override
        public int compareTo(OpenFileTableEntry e) {
            int res = 0;
            if (this.expTime < e.expTime) {
                res = -1;
            } else if (this.expTime == e.expTime) {
                res = e.fileId == null ? -1 : this.fileId.compareTo(e.fileId);
            } else {
                res = 1;
            }
            return res;
        }

        public static Comparator<OpenFileTableEntry> createWriteExpComparator() {
            return new Comparator<OpenFileTableEntry>() {
                @Override
                public int compare(OpenFileTableEntry e1, OpenFileTableEntry e2) {

                    int res = 0;
                    if (e1.writeExpTime < e2.writeExpTime) {
                        res = -1;
                    } else if (e1.writeExpTime == e2.writeExpTime) {
                        res = e1.fileId == null ? -1 : e2.fileId.compareTo(e1.fileId);
                    } else {
                        res = 1;
                    }
                    return res;
                }
            };
        }

        @Override
        public String toString() {
            return "(" + fileId + "," + expTime + "ms)";
        }

    }
}
