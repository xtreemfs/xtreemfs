/*
 * Copyright (c) 2010-2012 by Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 * This class implements a file version log, which records a timestamp and size for each file version.
 * 
 * @author stender
 */
public class FileVersionLog {

    public static class FileVersion implements Comparable<Long> {

        protected static final FileVersion EMPTY_VERSION = new FileVersion(0, 0, 0);

        private long                       timestamp;
        private long                       fileSize;
        private long                       numObjects;

        public FileVersion(long timestamp, long fileSize, long numObjects) {
            this.timestamp = timestamp;
            this.fileSize = fileSize;
            this.numObjects = numObjects;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getFileSize() {
            return fileSize;
        }

        public long getNumObjects() {
            return numObjects;
        }

        @Override
        public int compareTo(Long timestamp) {
            return this.timestamp < timestamp ? -1 : this.timestamp > timestamp ? 1 : 0;
        }

        @Override
        public String toString() {
            return "ts=" + timestamp + ", fs=" + fileSize + ", #objs=" + numObjects;
        }

    }

    /**
     * memory-resident representation of the truncate log
     */
    private List<FileVersion>       versionList;

    /**
     * memory-resident representation of all truncate operations performed since the last file version was
     * appended; the list is only used to record additional versions that have to be written to disk as well
     * when adding a file version
     */
    private final List<FileVersion> truncateList;

    private final File              logFile;

    private static final long       D_MAX = 2000; // 2s

    /**
     * Creates a new empty file version log.
     * 
     * @param logFile
     *            the file that persistently stores the log
     */
    public FileVersionLog(File logFile) {
        this.logFile = logFile;
        this.versionList = new ArrayList<FileVersion>();
        this.truncateList = new LinkedList<FileVersion>();
    }

    /**
     * Loads a file version log from a file into a memory-resident representation.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public synchronized void load() throws IOException {

        if (logFile == null)
            throw new IOException("no source file specified");

        versionList = new ArrayList<FileVersion>((int) (logFile.length() / 24));

        FileInputStream fi = new FileInputStream(logFile);
        ReusableBuffer logBuffer = BufferPool.allocate((int) logFile.length());
        logBuffer.position(0);
        fi.getChannel().read(logBuffer.getBuffer());
        fi.close();

        logBuffer.position(0);
        while (logBuffer.position() < logBuffer.limit()) {

            final long timestamp = logBuffer.getLong();
            final long fileSize = logBuffer.getLong();
            final long numObjects = logBuffer.getLong();

            assert (numObjects <= Integer.MAX_VALUE) : "number of objects: " + numObjects + ", current limit = "
                    + Integer.MAX_VALUE;
            // TODO: solve this problem for files with more than
            // Integer.MAX_VALUE objects

            // if no objects exist, the file size must be zero
            assert (numObjects != 0 || fileSize == 0);

            versionList.add(new FileVersion(timestamp, fileSize, numObjects));
        }

        BufferPool.free(logBuffer);
    }

    /**
     * Purges the log. This method effectively creates a new log that contains all remaining file versions
     * after having removed all file versions that are not bound to a snapshot according to the given array of
     * snapshot timestamps.
     * 
     * @param snapshotTimestamps
     *            an array of snapshot timestamps
     * @return an array containing all timestamps of the remaining file version after having purged the log
     */
    public synchronized long[] purge(long[] snapshotTimestamps) throws IOException {

        List<FileVersion> newVersionList = new LinkedList<FileVersion>();

        // for each file version, check if it is superseded
        // only run from 0 to length - 1 in order to omit the latest version, which it is never superseded
        for (int i = 0; i < versionList.size() - 1; i++) {

            FileVersion currentVersion = versionList.get(i);

            long currentTs = currentVersion.timestamp;
            long nextTs = versionList.get(i + 1).timestamp;

            // check if there is a timestamp t in 'timestamps' with currentTs -
            // d_max <= t <= nextTs + d_max
            boolean isSuperseeded = true;
            for (long t : snapshotTimestamps)
                if (currentTs - D_MAX <= t && t <= nextTs + D_MAX) {
                    isSuperseeded = false;
                    break;
                }

            if (!isSuperseeded)
                newVersionList.add(currentVersion);
        }

        // explicitly add the latest version, as it cannot be superseded by definition
        newVersionList.add(versionList.get(versionList.size() - 1));

        // rewrite the file version log on disk
        FileOutputStream fout = new FileOutputStream(logFile, false);
        for (FileVersion fv : newVersionList) {

            ReusableBuffer versionBuf = BufferPool.allocate(24);
            versionBuf.position(0);
            versionBuf.putLong(fv.timestamp).putLong(fv.fileSize).putLong(fv.numObjects);
            versionBuf.position(0);

            int bytesWritten = fout.getChannel().write(versionBuf.getBuffer());
            if (bytesWritten != 24)
                throw new IOException("could not append file version; only " + bytesWritten
                        + " bytes written; should be 24");

            BufferPool.free(versionBuf);
        }

        fout.close();

        // create an array of remaining file version timestamps
        long[] remainingTimestamps = new long[newVersionList.size()];
        for (int i = 0; i < remainingTimestamps.length; i++)
            remainingTimestamps[i] = newVersionList.get(i).timestamp;

        // replace the in-memory representation of the log
        versionList = newVersionList;

        return remainingTimestamps;
    }

    /**
     * Returns the latest version of a file stored in the table up to the given timestamp.
     * 
     * @param timestamp
     *            the timestamp
     * @return the latest file version up to <code>timestamp</code>, or EMPTY_VERSION if no such version
     *         exists.
     */
    public FileVersion getLatestVersionBefore(long timestamp) {

        // do a binary search
        int position = Collections.binarySearch(versionList, timestamp);

        // version does not exist
        if (position < 0) {

            // get the position of the latest prior version
            position = -position - 2;

            // return this version if it exists; otherwise, return an empty
            // file version
            return position >= 0 ? versionList.get(position) : FileVersion.EMPTY_VERSION;
        }

        // version exists
        else
            return versionList.get(position);
    }

    /**
     * Append a new file version to the log.
     * 
     * @param timestamp
     *            the timestamp attached to the file version
     * @param fileSize
     *            the file size attached to the file version
     * @param numObjects
     *            the number of objects attached to the file version
     */
    public synchronized void appendVersion(long timestamp, long fileSize, long numObjects) throws IOException {

        // if no objects exist, the file size must be zero
        assert (numObjects != 0 || fileSize == 0);

        // perform append writes to the log
        FileOutputStream fout = new FileOutputStream(logFile, true);

        // append an individual file version for each entry in the truncate record; this is necessary to
        // ensure correct results when reading data from truncated files
        for (FileVersion ver : truncateList)
            writeVersion(fout, ver.getTimestamp(), ver.getFileSize(), ver.getNumObjects());

        // clear the truncate record
        truncateList.clear();

        // write the actual file version
        writeVersion(fout, timestamp, fileSize, numObjects);

        // insert the version in the memory-resident data structure
        versionList.add(new FileVersion(timestamp, fileSize, numObjects));

        fout.close();
    }

    /**
     * Records a truncate operation in the memory-resident log.
     * 
     * @param timestamp
     *            the timestamp attached to the file version
     * @param fileSize
     *            the file size attached to the file version
     * @param numObjects
     *            the number of objects attached to the file version
     */
    public void recordTruncate(long timestamp, long fileSize, long numObjects) {

        FileVersion fv = new FileVersion(timestamp, fileSize, numObjects);

        versionList.add(fv);
        truncateList.add(fv);
    }

    /**
     * Returns the total number of versions stored in the log.
     * 
     * @return the number of versions
     */
    public long getVersionCount() {
        return versionList.size();
    }

    private void writeVersion(FileOutputStream fout, long timestamp, long fileSize, long numObjects) throws IOException {

        ReusableBuffer versionBuf = BufferPool.allocate(24);
        versionBuf.position(0);
        versionBuf.putLong(timestamp).putLong(fileSize).putLong(numObjects);
        versionBuf.position(0);

        int bytesWritten = fout.getChannel().write(versionBuf.getBuffer());
        if (bytesWritten != 24)
            throw new IOException("could not append file version; only " + bytesWritten
                    + " bytes written; should be 24");

        BufferPool.free(versionBuf);
    }

}
