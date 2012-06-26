/*
 * Copyright (c) 2010-2012 by Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xtreemfs.osd.storage.FileVersionLog.FileVersion;
import org.xtreemfs.osd.storage.VersionManager.ObjectVersionInfo;

/**
 * This class implements a record-keeping component for versions of files and objects, as well as object
 * checksums. It monitors two kinds of versions, which are <it>version numbers</it> and <it>timestamps</it>.
 * The former are used for replication; the latter for snapshots.
 * 
 * @author stender
 */
public class VersionManager {

    public static class ObjectVersionInfo implements Comparable<ObjectVersionInfo> {

        public static ObjectVersionInfo MISSING = new ObjectVersionInfo(0, 0, 0);
        public static ObjectVersionInfo PADDED  = new ObjectVersionInfo(-1, -1, -1);

        public final long               version;
        public final long               timestamp;
        public final long               checksum;

        public ObjectVersionInfo(long version, long timestamp, long checksum) {
            this.version = version;
            this.timestamp = timestamp;
            this.checksum = checksum;
        }

        @Override
        public int compareTo(ObjectVersionInfo o) {
            return version > o.version ? 1 : version < o.version ? -1 : timestamp > o.timestamp ? 1
                    : timestamp < o.timestamp ? -1 : 0;
        }

        @Override
        public String toString() {
            return "(" + version + ", " + timestamp + ", " + checksum + ")";
        }

    }

    /**
     * reference to the file version log
     */
    private final FileVersionLog                          fileVersionLog;

    /**
     * map of object versions: object ID -> sorted set of object versions in ascending order
     */
    private final Map<Long, SortedSet<ObjectVersionInfo>> objectVersionMap;

    /**
     * flag indicating whether versioning is generally enabled
     */
    private boolean                                       versioningEnabled;

    /**
     * ID of the largest known object
     */
    private long                                          largestObjectId;

    /**
     * Creates a new version manager.
     * 
     * @param fileVersionLog
     *            the file version log
     */
    public VersionManager(FileVersionLog fileVersionLog, boolean versioningEnabled) {
        this.fileVersionLog = fileVersionLog;
        this.objectVersionMap = new TreeMap<Long, SortedSet<ObjectVersionInfo>>();
        this.versioningEnabled = versioningEnabled;
        this.largestObjectId = -1;
    }

    /**
     * Adds a new object version or timestamp.
     * 
     * @param objId
     *            the object identifier
     * @param version
     *            the version identifier
     * @param timestamp
     *            the timestamp
     */
    public void addObjectVersionInfo(long objId, long version, long timestamp, long checksum) {

        // implicitly enable versioning as soon as a timestamped version is added
        if (timestamp != -1)
            versioningEnabled = true;

        SortedSet<ObjectVersionInfo> versions = objectVersionMap.get(objId);
        if (versions == null) {
            versions = new TreeSet<ObjectVersionInfo>();
            objectVersionMap.put(objId, versions);
        }

        // replace version (needs to be removed first, because versions w/ the same version and timestamp will
        // be considered equal otherwise)
        ObjectVersionInfo v = new ObjectVersionInfo(version, timestamp, checksum);
        versions.remove(v);
        versions.add(v);

        if (objId > largestObjectId)
            largestObjectId = objId;
    }

    /**
     * Removes an existing object version.
     * 
     * @param objId
     *            the object identifier
     * @param version
     *            the version identifier
     * @param timestamp
     *            the timestamp
     */
    public void removeObjectVersionInfo(long objId, long version, long timestamp) {

        SortedSet<ObjectVersionInfo> versions = objectVersionMap.get(objId);
        if (versions != null)
            versions.remove(new ObjectVersionInfo(version, timestamp, 0));
    }

    /**
     * Returns information about a specific version of the given object.
     * 
     * @param objId
     *            the object ID
     * @param version
     *            the version
     * @param timestamp
     *            the timestamp
     * @return the object version, or {@link ObjectVersionInfo.MISSING} if no such version is available
     */
    public ObjectVersionInfo getObjectVersionInfo(long objId, long version, long timestamp) {

        ObjectVersionInfo v = ObjectVersionInfo.MISSING;

        // retrieve object version
        SortedSet<ObjectVersionInfo> versions = objectVersionMap.get(objId);
        if (versions != null && versions.size() > 0) {
            SortedSet<ObjectVersionInfo> set = versions.tailSet(new ObjectVersionInfo(version, timestamp, 0));
            if (set.size() > 0)
                v = set.first();
        }

        return v;
    }

    /**
     * Returns the latest known version of the given object (regardless of existing file versions).
     * 
     * @param objId
     *            the object ID
     * @return the largest object version
     */
    public ObjectVersionInfo getLargestObjectVersion(long objId) {

        ObjectVersionInfo version = ObjectVersionInfo.MISSING;

        // retrieve latest object version
        SortedSet<ObjectVersionInfo> versions = objectVersionMap.get(objId);
        if (versions != null && versions.size() > 0)
            version = versions.last();

        return version;
    }

    /**
     * Returns the latest known version of the given object and version number (regardless of existing file
     * versions).
     * 
     * @param objId
     *            the object ID
     * @param v
     *            the version number
     * @return the largest object version
     */
    public ObjectVersionInfo getLargestObjectVersionBefore(long objId, long v) {

        ObjectVersionInfo version = ObjectVersionInfo.MISSING;

        // retrieve latest object version
        SortedSet<ObjectVersionInfo> versions = objectVersionMap.get(objId);
        if (versions != null && versions.size() > 0) {
            SortedSet<ObjectVersionInfo> subset = versions.subSet(new ObjectVersionInfo(v, 0, 0),
                    new ObjectVersionInfo(v, Long.MAX_VALUE, 0));
            if (subset != null && subset.size() > 0)
                version = subset.last();
        }

        return version;
    }

    /**
     * Returns the latest object version that is bound to a file version that has been created not later than
     * the given timestamp.
     * 
     * @param objId
     *            the object ID
     * @param timestamp
     *            the timestamp; if <code>Long.MAX_VALUE</code> is provided, the current object version will
     *            be retrieved
     * @return the identifier of the latest object version up to the timestamp;
     *         <code>ObjectVersionInfo.MISSING</code>, if no such version exists;
     *         <code>ObjectVersionInfo.PADDED</code>, if version needs to be zero-padded
     */
    public ObjectVersionInfo getLatestObjectVersionBefore(long objId, long timestamp, long currentObjCount) {

        // get the latest file version up to the given timestamp; return an
        // empty object version if no such file version exists
        FileVersion fv = timestamp == Long.MAX_VALUE ? new FileVersion(timestamp - 1, 0, currentObjCount)
                : fileVersionLog.getLatestVersionBefore(timestamp);
        if (fv.getTimestamp() == 0)
            return ObjectVersionInfo.MISSING;

        long length = fv.getNumObjects();
        ObjectVersionInfo versionInfo = ObjectVersionInfo.MISSING;

        // retrieve the latest object version up to the given timestamp; add 1
        // to version timestamp to ensure that equal timestamps are returned if
        // contained
        SortedSet<ObjectVersionInfo> versions = objectVersionMap.get(objId);
        if (versions != null && !versions.isEmpty()) {
            SortedSet<ObjectVersionInfo> headSet = versions.headSet(new ObjectVersionInfo(versions.last().version, fv
                    .getTimestamp() + 1, 0));
            if (!headSet.isEmpty())
                versionInfo = headSet.last();
        }

        // if such an object version exists, check all remaining versions
        // between
        while (fv.getTimestamp() >= versionInfo.timestamp) {

            if (objId > fv.getNumObjects() - 1)
                return objId > length - 1 ? ObjectVersionInfo.MISSING : ObjectVersionInfo.PADDED;

            // retrieve next earlier file version
            fv = fileVersionLog.getLatestVersionBefore(fv.getTimestamp() - 1);
        }

        // return object version
        return versionInfo;
    }

    /**
     * Removes all information about existing object versions.
     */
    public void clearObjectVersions() {
        objectVersionMap.clear();
    }

    /**
     * Checks if versioning is enabled.
     * 
     * @return <code>true</code>, if versioning is enabled; <code>false</code>, otherwise
     */
    public boolean isVersioningEnabled() {
        return versioningEnabled;
    }

    /**
     * Creates a new file version.
     * 
     * @param timestamp
     *            the timestamp
     * @param fileSize
     *            the file size
     * @param numObjects
     *            the number of objects
     */
    public void createFileVersion(long timestamp, long fileSize, long numObjects) throws IOException {
        versioningEnabled = true;
        fileVersionLog.appendVersion(timestamp, fileSize, numObjects);
    }

    /**
     * Returns the latest file version up to the given timestamp.
     * 
     * @param timestamp
     *            the timestamp
     * @return the file version
     */
    public FileVersion getLatestFileVersionBefore(long timestamp) {
        assert (versioningEnabled) : "attempted to get latest file version of an unversioned file";
        return fileVersionLog.getLatestVersionBefore(timestamp);
    }

    /**
     * Checks if the given version of the given object is contained in any prior version of the file.
     * 
     * @param objId
     *            the object identifier
     * @param version
     *            the object version info
     * @return <code>true</code>, if the given object version is contained in at least one prior version of
     *         the file; <code>false</code>, otherwise
     */
    public boolean isContained(ObjectVersionInfo version) {
        // check if a file version exists that was created not earlier than the object version
        FileVersion fv = fileVersionLog.getLatestVersionBefore(version.timestamp);
        return fv.getTimestamp() >= version.timestamp;
    }

    /**
     * Returns information object versions that are neither bound to any file verision with one of the given
     * timestamps, nor the current version of the file.
     * 
     * @param timestamps
     *            the list of file version timestamps
     * @return a map: object ID -> set of object version information containing information about object
     *         versions that may be safely deleted
     */
    public Map<Long, Set<ObjectVersionInfo>> getUnboundObjectVersions(long[] timestamps) {

        Map<Long, Set<ObjectVersionInfo>> unboundVersions = new HashMap<Long, Set<ObjectVersionInfo>>();

        for (Entry<Long, SortedSet<ObjectVersionInfo>> objVerEntry : objectVersionMap.entrySet()) {

            long objId = objVerEntry.getKey();
            SortedSet<ObjectVersionInfo> versions = objVerEntry.getValue();

            // for each object version, check if it is superseded
            ObjectVersionInfo[] versionArray = versions.toArray(new ObjectVersionInfo[versions.size()]);

            // only run from 0 to length - 1 in order to omit the latest version, which it is never superseded
            for (int i = 0; i < versionArray.length - 1; i++) {

                ObjectVersionInfo currentVersion = versionArray[i];
                long currentTs = currentVersion.timestamp;
                long nextTs = versionArray[i + 1].timestamp;

                // check if there is a timestamp t in 'timestamps' with currentTs <= t <= nextTs
                boolean isSuperseeded = true;
                for (long t : timestamps)
                    if (currentTs <= t && t <= nextTs) {
                        isSuperseeded = false;
                        break;
                    }

                if (isSuperseeded) {

                    Set<ObjectVersionInfo> unboundVersionsSet = unboundVersions.get(objId);
                    if (unboundVersionsSet == null) {
                        unboundVersionsSet = new HashSet<VersionManager.ObjectVersionInfo>();
                        unboundVersions.put(objId, unboundVersionsSet);
                    }

                    unboundVersionsSet.add(currentVersion);
                }
            }

        }

        return unboundVersions;
    }

    /**
     * Purges the file version log by creating a new log that does not contain any file versions that are not
     * bound to any snapshot with one of the given timestamps.
     * 
     * @param unboundVersions
     *            information about undbound versions
     * @return an array of remaining file version timestamps after having purged the unbound ones
     */
    public long[] purgeUnboundFileVersions(long[] snapshotTimestamps) throws IOException {
        return fileVersionLog.purge(snapshotTimestamps);
    }

    /**
     * Returns the current number of registered file versions.
     * 
     * @return the current count of file versions
     */
    public long getFileVersionCount() {
        return versioningEnabled ? fileVersionLog.getVersionCount() : 0;
    }

    /**
     * Returns the largest object identifier for which a version has been added to this manager.
     * 
     * @return the largest object identifier
     */
    public long getLastObjectId() {
        return largestObjectId;
    }

}
