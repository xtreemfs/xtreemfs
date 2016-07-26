/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;

/**
 * Caches metadata for libxtreemfs.
 */
public class MetadataCache {

    private final long                                maxNumberOfEntries;

    private final long                                ttlS;

    private boolean                                   enabled;

    /**
     * A map containing all {@link MetadataCacheEntry} in insertion order. Also it is possible to access an
     * entry in a hashmap like way in time O(1).
     */
    private final LinkedHashMap<String, MetadataCacheEntry> cache;

    /**
     * A map sorted by the path. This is used to iterate recursively over a path when a directory is
     * invalidated to delete all subdirectories and files belonging to the invalidated directory from the
     * cache.
     */
    private final SortedSet<String>                         pathIndex;

    private final ReadWriteLock                             readWriteLock;

    /**
     * Lock that is used when writing to the "pathIndex" and "cache" together.
     */
    private final Lock                                      writeLock;

    /**
     * Lock that is used when reading "pathIndex" and "cache" together.
     */
    private final Lock                                      readLock;

    /**
     * MetadataCache for Stat, listxattrResponse and XAttr objects per path.
     * 
     */
    protected MetadataCache(long maxNumberOfEntries, long ttlS) {
        this.maxNumberOfEntries = maxNumberOfEntries;
        this.ttlS = ttlS;

        enabled = maxNumberOfEntries > 0 ? true : false;

        cache = new LinkedHashMap<String, MetadataCacheEntry>();
        pathIndex = new TreeSet<String>();

        readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
    }

    /**
     * Removes MetadataCacheEntry for path from cache
     * 
     * @param path
     *            The path for the entry which should be invalidated.
     */
    protected void invalidate(String path) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            cache.remove(path);
            pathIndex.remove(path);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes MetadataCacheEntry for path and any objects matching path+"/".
     * 
     * @param path
     *            Path which should be invalidated.
     */
    protected void invalidatePrefix(String path) {
        if (path.isEmpty() || !enabled) {
            return;
        }
        // collect all entries that should be removed from pathIndex to remove them at the end.
        List<String> indicesToRemove = new ArrayList<String>();

        writeLock.lock();
        try {
            // At first, delete "path" from "cache" and "pathIndex"
            cache.remove(path);
            pathIndex.remove(path);

            // At second, remove all entries which have "path" respectively "path+'/'"
            if (!path.endsWith("/")) {
                path = path + "/";
            }

            for (String deletePath : pathIndex.tailSet(path)) {
                // if the we reach the first element which don't have "path" as
                // prefix we are finished.
                if (!deletePath.startsWith(path)) {
                    break;
                }

                // else delete this element from "pathIndex"and "cache"
                cache.remove(deletePath);
                indicesToRemove.add(deletePath);
            }
            // do the removing after looping over "pathIndex" to avoid ConcurrentModificationException
            pathIndex.removeAll(indicesToRemove);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Renames "path" to "newPath" and any object's path matching path+"/".
     * 
     * @param path
     *            Path which should be renamed to "newPath".
     * @param newPath
     *            New name of "path".
     */
    protected void renamePrefix(String path, String newPath) {
        if (path.isEmpty() || !enabled) {
            return;
        }
        // Temporary TreeSet to store the new paths
        TreeSet<String> tempPaths = new TreeSet<String>();

        writeLock.lock();
        try {
            // At first, rename the directory itself.
            MetadataCacheEntry entry = cache.remove(path);
            cache.put(newPath, entry);

            // Seconde, rename all entries with prefix that matches "path" respectively "path+'/'"
            if (!path.endsWith("/"))
                path = path + "/";
            if (!newPath.endsWith("/"))
                newPath = newPath + "/";

            for (String renamePath : pathIndex.tailSet(path)) {
                // if the we reach the first element which don't have "path" as
                // prefix we are finished.
                if (!renamePath.startsWith(path)) {
                    break;
                }

                // delete object from cache and insert it with new path
                entry = cache.remove(renamePath);
                cache.put(renamePath.replaceFirst(path, newPath), entry);

                // add the oldPath to the "tempPaths" in order to remove them later from "pathIndex"
                // and add them with the new path
                tempPaths.add(renamePath.replaceFirst(path, newPath));
            }

            // remove all collected tempPaths to the "pathIndex" and readd them with the correct new
            // path
            for (String tmpPath : tempPaths) {
                pathIndex.remove(tmpPath);
                pathIndex.add(tmpPath.replace(path, newPath));
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the Stat object if its exists. null otherwise.
     * 
     * @param path
     *            Path of the cached object.
     * @return {@link Stat} object or null.
     */
    protected Stat getStat(String path) {
        if (path.isEmpty() || !enabled) {
            return null;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry != null) { // cache hit
                // We must never have cached a hard link.
                assert (entry.getStat() == null || entry.getStat().getNlink() == 1);

                long currentTimeS = System.currentTimeMillis() / 1000;
                if (entry.getStatTimeoutS() >= currentTimeS) { // Stat object is still valid
                    return entry.getStat();
                } else { // Stat object is expired => delete it from cache

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache getStat expired: %s", path);
                    }

                    // Only delete object, if the maximum timeout of all three objects is
                    // reached.
                    if (entry.getTimeoutS() < currentTimeS) {
                        // Free MetadataCacheEntry and delete from Index. This increases the
                        // run time of GetStat() roughly by factor 3.
                        cache.remove(path);
                        pathIndex.remove(path);
                    }
                }
            } else { // cache miss
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "MetadataCache getStat miss: ", cache.size());
                }
            }
        } finally {
            writeLock.unlock();
        }
        return null;
    }

    /**
     * Stores/updates stat in cache for path.
     * 
     * @param path
     *            Path related to the {@link Stat} object.
     * @param stat
     *            The {@link Stat} object which should be cached under "path".
     */
    protected void updateStat(String path, Stat stat) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            // remove entry if it exists in the cache
            MetadataCacheEntry entry = cache.remove(path);
            if (entry == null) { // cache miss. entry has to be created

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "MetadataCache: registering %s", path);
                }
                entry = new MetadataCacheEntry();

                // entry doesn't exists yet in cache => there is no entry "pathIndex" for it. We
                // have to create one.
                pathIndex.add(path);
            }

            // set net stat object and update timeouts
            entry.setStat(stat);
            entry.setStatTimeoutS(System.currentTimeMillis() / 1000 + ttlS);
            entry.setTimeoutS(entry.getStatTimeoutS());

            // (re-)add entry to "cache"
            evictUnmutexed(1);
            cache.put(path, entry);

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates timestamp of the cached stat object.
     * 
     * @param path
     *            Path of the cached object.
     * @param timestampS
     *            timestamp in seconds which should be set
     * @param toSet
     *            int value of attributes that should be set as defined in {@link Setattrs}. Possible values:
     *            SETATTR_ATIME, SETATTR_MTIME, SETATTR_CTIME
     * 
     * 
     */
    protected void updateStatTime(String path, long timestampS, int toSet) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);

            if (entry != null) {
                if (entry.getStat() == null) {
                    return;
                }

                // build new Stat object and update (a,c,m)time value if toSet says so.
                Stat.Builder newStat = entry.getStat().toBuilder();
                long timeNS = timestampS * 1000000000;

                if ((toSet & Setattrs.SETATTR_ATIME.getNumber()) > 0
                        && (timeNS > entry.getStat().getAtimeNs())) {
                    newStat.setAtimeNs(timeNS);
                }
                if ((toSet & Setattrs.SETATTR_MTIME.getNumber()) > 0
                        && (timeNS > entry.getStat().getMtimeNs())) {
                    newStat.setMtimeNs(timeNS);
                }
                if ((toSet & Setattrs.SETATTR_CTIME.getNumber()) > 0
                        && (timeNS > entry.getStat().getCtimeNs())) {
                    newStat.setCtimeNs(timeNS);
                }

                // set new Stat object and update timeouts
                entry.setStat(newStat.build());
                entry.setStatTimeoutS(System.currentTimeMillis() / 1000 + ttlS);
                entry.setTimeoutS(entry.getStatTimeoutS());

                // readd the entry in the cache
                cache.remove(path);
                cache.put(path, entry);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates the attributes given in "stat" and selected by "toSet".
     * 
     * @param path
     *            Path of the cached object.
     * @param stat
     *            The {@link Stat} object cotaining the new values.
     * @param toSet
     *            Bitmap describing which values should be updated. See {@link Setattrs}.
     */
    protected void updateStatAttributes(String path, Stat stat, int toSet) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);

            if (entry != null) {
                if (entry.getStat() == null) {
                    return;
                }

                // Update Stat object regarding to "toSet" and "stat"
                Stat.Builder statBuilder = entry.getStat().toBuilder();

                if ((toSet & Setattrs.SETATTR_ATTRIBUTES.getNumber()) > 0) {
                    statBuilder.setAttributes(stat.getAttributes());
                }

                if ((toSet & Setattrs.SETATTR_MODE.getNumber()) > 0) {
                    // Modify only the last 12 Bits (3 bits for sticky bit, set GID and
                    // set UID and 3 * 3 bits for the file access mode).
                    statBuilder.setMode((statBuilder.getMode() & 0xFFFFF000) | (stat.getMode() & 0x00000FFF));
                }

                if ((toSet & Setattrs.SETATTR_UID.getNumber()) > 0) {
                    statBuilder.setUserId(stat.getUserId());
                }

                if ((toSet & Setattrs.SETATTR_GID.getNumber()) > 0) {
                    statBuilder.setGroupId(stat.getGroupId());
                }

                if ((toSet & Setattrs.SETATTR_SIZE.getNumber()) > 0) {

                    if (stat.hasTruncateEpoch() && stat.getTruncateEpoch() > statBuilder.getTruncateEpoch()) {
                        statBuilder.setSize(stat.getSize());
                        statBuilder.setTruncateEpoch(stat.getTruncateEpoch());
                    } else {
                        if (stat.hasTruncateEpoch()
                                && stat.getTruncateEpoch() == statBuilder.getTruncateEpoch()
                                && stat.getSize() > statBuilder.getSize()) {
                            statBuilder.setSize(stat.getSize());
                        }
                    }

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache UpdateStatAttributes SETATTR_SIZE: new size: %s "
                                        + "truncate epoch: %s", statBuilder.getSize(),
                                statBuilder.getTruncateEpoch());
                    }
                }

                if ((toSet & Setattrs.SETATTR_ATIME.getNumber()) > 0) {
                    statBuilder.setAtimeNs(stat.getAtimeNs());
                }

                if ((toSet & Setattrs.SETATTR_MTIME.getNumber()) > 0) {
                    statBuilder.setMtimeNs(stat.getMtimeNs());
                }

                if ((toSet & Setattrs.SETATTR_CTIME.getNumber()) > 0) {
                    statBuilder.setCtimeNs(stat.getCtimeNs());
                }

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "MetadataCache UpdateStatAttributes: %s toSet: %s", path, toSet);
                }

                // update stat, timeouts and then re-add "entry" to the cache
                entry.setStat(statBuilder.build());
                entry.setStatTimeoutS(System.currentTimeMillis() / 1000 + ttlS);
                entry.setTimeoutS(entry.getStatTimeoutS());

                cache.remove(path);
                cache.put(path, entry);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates file size and truncate epoch from an OSDWriteResponse.
     * 
     * @param path
     *            Path of the cached object.
     * @param response
     *            {@link OSDWriteResponse} with new file size and truncate epoch.
     */
    protected void updateStatFromOSDWriteResponse(String path, OSDWriteResponse response) {
        if (path.isEmpty() || !enabled || !response.hasSizeInBytes() || !response.hasTruncateEpoch()) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry != null) {
                if (entry.getStat() == null) {
                    return;
                }

                Stat.Builder statBuilder = entry.getStat().toBuilder();
                if (response.getTruncateEpoch() > statBuilder.getTruncateEpoch()
                        || (response.getTruncateEpoch() == statBuilder.getTruncateEpoch() //
                                && response.getSizeInBytes() > statBuilder.getSize())) {

                    statBuilder.setSize(response.getSizeInBytes());
                    statBuilder.setTruncateEpoch(response.getTruncateEpoch());
                    entry.setStat(statBuilder.build());
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns a {@link DirectoryEntries} object (if it's found for "path") limited to entries starting from
     * "offset" up to "count" (or the maximum)S.
     * 
     * @param path
     *            Path of the cache object.
     * @param offset
     *            Index where to begin with retrieving DiretoryEntries.
     * @param count
     *            Number of entries which should be return.
     * @return {@link DirectoryEntries}
     */
    protected DirectoryEntries getDirEntries(String path, int offset, int count) {
        if (path.isEmpty() || !enabled) {
            return null;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry != null && entry.getDirectoryEntries() != null) {
                long currentTimeS = System.currentTimeMillis() / 1000;
                if (entry.getDirectoryEntriesTimeoutS() >= currentTimeS) { // entry is valid => use it

                    DirectoryEntries.Builder result;

                    // copy all entries from cache
                    if (offset == 0 && count >= entry.getDirectoryEntries().getEntriesCount()) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                    "MetadataCache getDirEntries() hit: %s [%s]", path, cache.size());
                        }
                        result = entry.getDirectoryEntries().toBuilder();
                    } else { // copy just the selected entries from cache
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                    "MetadataCache getDirectoryEntries() hit (partial copy): "
                                            + "%s [%s] offset: %s", path, cache.size(), offset);
                        }
                        result = DirectoryEntries.newBuilder();
                        for (int i = offset; i < offset + count; i++) {
                            result.addEntries(entry.getDirectoryEntries().getEntries(i));
                        }
                    }
                    return result.build();
                } else { // Expired! => remove from cache
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache entry expired: %s", path);
                    }

                    // Only delete object, if the maximum timeout is reached.
                    if (entry.getTimeoutS() < currentTimeS) {
                        cache.remove(path);
                        pathIndex.remove(path);
                    }
                    return null;
                }
            }
        } finally {
            writeLock.unlock();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "MetadataCache getDirectoryEntries() miss: %s", path);
        }
        return null;
    }

    /**
     * Invalidates the stat entry stored for "path".
     * 
     * @param path
     *            Path of the cache object.
     */
    protected void invalidateStat(String path) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry != null) {
                entry.setStat(null);
                entry.setStatTimeoutS(0);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Stores/updates DirectoryEntries in cache for path.
     * 
     * @param path
     *            Path where the object should be stored.
     * @param dirEntries
     *            {@link DirectoryEntries} object which should be cached.
     * 
     * @note This implementation assumes that "dirEntries" is always complete, i.e. it must be guaranteed that
     *       it contains all entries.
     */
    protected void updateDirEntries(String path, DirectoryEntries dirEntries) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            boolean created = false;

            if (entry == null) { // entry does not exist, create new one
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "MetadataCache updateDirEntries(): new entry for path %s", path);
                }
                entry = new MetadataCacheEntry();
                entry.setPath(path);
                created = true;
            }

            entry.setDirectoryEntries(dirEntries);
            entry.setDirectoryEntriesTimeoutS(System.currentTimeMillis() / 1000 + ttlS);
            entry.setTimeoutS(entry.getDirectoryEntriesTimeoutS());

            if (created) {
                evictUnmutexed(1);
                pathIndex.add(path);
                cache.put(path, entry);
            } else {
                cache.remove(path);
                cache.put(path, entry);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes "entryName" from the cached directory "pathToDirectory".
     * 
     * @param pathToDirectory
     *            Path of the cached directory.
     * @param entryName
     *            Name from the entry that should be removed from the cache {@link DirectoryEntries}.
     */
    protected void invalidateDirEntry(String pathToDirectory, String entryName) {
        if (pathToDirectory.isEmpty() || entryName.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(pathToDirectory);
            if (entry != null) {
                if (entry.getDirectoryEntries() == null) {
                    return;
                }

                DirectoryEntries.Builder dirEntriesBuilder = DirectoryEntries.newBuilder();

                // Copy DirectoryEntries to "dirEntriesBuilder" except entries with "entryName".
                for (int i = 0; i < entry.getDirectoryEntries().getEntriesCount(); i++) {
                    if (!entry.getDirectoryEntries().getEntries(i).getName().equals(entryName)) {
                        dirEntriesBuilder.addEntries(entry.getDirectoryEntries().getEntries(i));
                    }
                }
                entry.setDirectoryEntries(dirEntriesBuilder.build());
            }

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Remove cached DirectoryEntries in cache for path.
     * 
     * @param path
     *            Path of the cached object.
     */
    protected void invalidateDirEntries(String path) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry != null) {
                entry.setDirectoryEntries(null);
                entry.setDirectoryEntriesTimeoutS(0);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns a {@link Tupel} with and Xattr and a boolean value. Xattr may be null if it is not found. The
     * boolean value determines if the Xattrs where cached. (Xattrs can be cached but the Xattr "name" can not
     * be in Xattrs)
     * 
     * 
     * @param path
     *            Path of the cached object.
     * @param name
     *            Name of the {@link XAttr}.
     * 
     * @return {@link Tupel} with Xattr value or null as first value and boolean as second.
     */
    protected Tupel<String, Boolean> getXAttr(String path, String name) {
        boolean xattrsCached = false;

        if (path.isEmpty() || !enabled) {
            return new Tupel<String, Boolean>(null, xattrsCached);
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            long currentTimeMS = System.currentTimeMillis() / 1000;

            if (entry != null && entry.getXattrs() != null) {
                // Entry found with valid Xattrs. Check timeout Xattrs
                if (entry.getXattrTimeoutS() >= currentTimeMS) {
                    xattrsCached = true;

                    for (XAttr xattr : entry.getXattrs().getXattrsList()) {
                        if (xattr.getName().equals(name)) {
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                        "MetadataCache getXattr() hit: %s [%s]", path, cache.size());
                            }
                            return new Tupel<String, Boolean>(xattr.getValue(), xattrsCached);
                        }
                    }
                    // the Xattr "name" was not found;
                    return new Tupel<String, Boolean>(null, xattrsCached);
                } else { // Cache entry is expired => remove from cache.

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache getXattr() expired: %s", path);
                    }
                    // Only delete object if maximum timeout is reached.
                    if (entry.getTimeoutS() < currentTimeMS) {
                        cache.remove(path);
                        pathIndex.remove(path);
                    }
                    return new Tupel<String, Boolean>(null, xattrsCached);
                }
            }

        } finally {
            writeLock.unlock();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "MetadataCache getXattr() miss: %s [%s]", path, cache.size());
        }
        return new Tupel<String, Boolean>(null, xattrsCached);
    }

    /**
     * Returns a Tupel the size of a value (string length) of an XAttribute "name" cached for "path" if it
     * exists and is still valid as first item. 0 otherwhise. And true as second item if the Xattrs where
     * cached for path.
     * 
     * 
     * @param path
     *            Path of the cached object.
     * @param name
     *            Name of the {@link XAttr}.
     * 
     * @return A {@link Tupel} with size of the value of the Xattr or 0 as first and Boolean as second item.
     */
    protected Tupel<Integer, Boolean> getXAttrSize(String path, String name) {
        boolean xattrCached = false;

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            long currentTimeS = System.currentTimeMillis() / 1000;

            if (entry != null && entry.getXattrs() != null) {
                if (entry.getXattrTimeoutS() >= currentTimeS) { // entry is still valid
                    xattrCached = true;
                    for (XAttr xattr : entry.getXattrs().getXattrsList()) {
                        if (xattr.getName().equals(name)) {

                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                        "MetadataCache getXattrSize() hit: %s [%s]", path, cache.size());
                            }
                            return new Tupel<Integer, Boolean>(xattr.getValue().length(), xattrCached);
                        }
                    }

                } else { // cache entry is expired => remove from cache
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache getXattrSize() expired: %s", path);
                    }

                    // only delete entry when overall timeout is expired
                    if (entry.getTimeoutS() < currentTimeS) {
                        cache.remove(path);
                        pathIndex.remove(path);
                    }
                }
            }

        } finally {
            writeLock.unlock();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "MetadataCache getXattrSize() miss: %s [%s]", path, cache.size());
        }
        return new Tupel<Integer, Boolean>(0, xattrCached);
    }

    /**
     * Get all extended attributes cached for "path". Return null if not cached Xattrs exists for that path.
     * 
     * @param path
     *            Path of the cached object.
     * @return {@link listxattrResponse} or null.
     */
    protected listxattrResponse getXAttrs(String path) {
        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            long currentTimeS = System.currentTimeMillis() / 1000;

            if (entry != null && entry.getXattrs() != null) {
                if (entry.getXattrTimeoutS() >= currentTimeS) { // cache entry is still valid; hit

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache getXattrs() hit: %s [%s]", path, cache.size());
                    }
                    return entry.getXattrs();

                } else { // entry is expired => remove it
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "MetadataCache getXattrs() expired: %s", path, cache.size());
                    }

                    // only delete object when overall timeout is expired
                    if (entry.getTimeoutS() < currentTimeS) {
                        cache.remove(path);
                        pathIndex.remove(path);
                    }
                    return null;
                }
            }
        } finally {
            writeLock.unlock();
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "MetadataCache getXattrs() miss %s [%s]", path, cache.size());
        }
        return null;
    }

    /**
     * Updates the "value" for the attribute "name" of "path" if the list of attributes for "path" is already
     * cached.
     * 
     * @param path
     *            Path of the cached object.
     * @param name
     *            Name of the {@link XAttr}.
     * @param value
     *            New value that should be set.
     */
    protected void updateXAttr(String path, String name, String value) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            // check if there is already an entry for "path"
            MetadataCacheEntry entry = cache.get(path);
            if (entry == null) {
                // Don't create a new entry with an incomplete xattr list.
                return;
            }

            if (entry.getXattrs() == null) {
                // Don't create a new xattr list for an existing cache entry.
                return;
            }

            long currentTimeS = System.currentTimeMillis() / 1000;
            if (entry.getXattrTimeoutS() < currentTimeS) {
                // Don't update expired xattrs.
                return;
            }

            listxattrResponse.Builder newXattrs = entry.getXattrs().toBuilder();
            boolean nameFound = false;
            for (int i = 0; i < newXattrs.getXattrsList().size(); i++) {

                // if Xattr with "name" found, update it, set "nameFound" to true und stop the loop
                if (newXattrs.getXattrs(i).getName().equals(name)) {
                    nameFound = true;
                    XAttr.Builder xattr = newXattrs.getXattrs(i).toBuilder();
                    xattr.setValue(value);
                    newXattrs.setXattrs(i, xattr.build());
                    break;
                }

            }
            // if Xattr with "name" don't exists, add it to teh Xattr list.
            if (!nameFound) {
                newXattrs.addXattrs(XAttr.newBuilder().setName(name).setValue(value).build());
            }

            // Replace the existing entry in cache - do not update TTL
            entry.setXattrs(newXattrs.build());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Stores/updates XAttrs in cache for path.
     * 
     * @note This implementation assumes that the list of extended attributes is always complete.
     * 
     * @param path
     *            Path of the cached object.
     * @param xattrs
     *            New {@link listxattrResponse} that should be cached.
     */
    protected void updateXAttrs(String path, listxattrResponse xattrs) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            // create a new entry if there isn't one in cache
            if (entry == null) {

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "MetadataCache updateXattrs: new entry for path %s", path);
                }
                entry = new MetadataCacheEntry();
                entry.setPath(path);
            }

            entry.setXattrs(xattrs);
            entry.setXattrTimeoutS(System.currentTimeMillis() / 1000 + ttlS);
            entry.setTimeoutS(entry.getXattrTimeoutS());

            // we have to remove and readd the entry. If it was not in cache we are dealing with a
            // new entry and we have to insert in "pathIndex" too
            if (cache.remove(path) == null) {
                pathIndex.add(path);
            }
            cache.put(path, entry);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes "name" from the list of extended attributes cached for "path".
     * 
     * @param path
     *            Path of the cached object.
     * @param name
     *            Name of the {@link XAttr} that should be removed from cached Xattrs.
     */
    protected void invalidateXAttr(String path, String name) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry == null) {
                // there is no entry in the cache => nothing to do
                return;
            }

            if (entry.getXattrs() == null) {
                // don't create a new incomplete Xattr list.
                return;
            }

            long currentTimeS = System.currentTimeMillis() / 1000;
            if (entry.getXattrTimeoutS() < currentTimeS) {
                // Don't alter expired Xattr.
                return;
            }

            // Copy old Xattr without entry "name"
            listxattrResponse.Builder xattrs = listxattrResponse.newBuilder();
            for (XAttr xattr : entry.getXattrs().getXattrsList()) {
                if (!xattr.getName().equals(name)) {
                    xattrs.addXattrs(xattr);
                }
            }

            entry.setXattrs(xattrs.build());

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Remove cached XAttrs in cache for path.
     * 
     * @param path
     *            Path of the cached object.
     */
    protected void invalidateXAttrs(String path) {
        if (path.isEmpty() || !enabled) {
            return;
        }

        writeLock.lock();
        try {
            MetadataCacheEntry entry = cache.get(path);
            if (entry != null) {
                entry.setXattrs(null);
                entry.setXattrTimeoutS(0);
            }

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the current number of elements.
     * 
     * @return long
     */
    protected long size() {
        readLock.lock();
        try {
            return cache.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the maximum number of elements.
     * 
     * @return long
     */
    protected long capacity() {
        return maxNumberOfEntries;
    }

    /**
     * Evicts first n oldest entries from cache.
     * 
     * @param n
     *            Number of elements that should be evicted.
     */
    private void evictUnmutexed(int n) {
        while (cache.size() > maxNumberOfEntries - n) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "MetadataCache evictUnmutexed: Deleting one entry from cache; "
                                + "entries in total: %s", cache.size());
            }
            // get first element of keys of the LinkedHashMap "cache".
            String path = cache.keySet().iterator().next();
            cache.remove(path);
            pathIndex.remove(path);
        }
    }
}
