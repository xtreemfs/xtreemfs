/*
 * Copyright (c) 2010-2011 by Jan Stender,
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 * This class implements a version table for a file. The version table maps time
 * stamps to lists of object versions.
 * 
 * @author stender
 */
public class VersionTable {
    
    private static final long D_MAX = 2000; // 2s
                                         
    public static class Version {
        
        protected static final Version EMPTY_VERSION = new Version(new int[0], 0);
        
        private int[]                  objVersions;
        
        private long                   fileSize;
        
        public Version(int[] objVersions, long fileSize) {
            this.objVersions = objVersions;
            this.fileSize = fileSize;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public int getObjCount() {
            return objVersions.length;
        }
        
        public int getObjVersion(long objNo) {
            assert (objNo <= Integer.MAX_VALUE);
            return objNo >= objVersions.length ? 0 : objVersions[(int) objNo];
        }
        
    }
    
    /**
     * internal table with file-to-object-list mappings
     */
    private SortedMap<Long, Version> vt;
    
    private File                     vtFile;
    
    /**
     * Creates a new empty version table.
     * 
     * @param vtFile
     *            the file that persistently stores the table
     */
    public VersionTable(File vtFile) {
        vt = new TreeMap<Long, Version>();
        this.vtFile = vtFile;
    }
    
    /**
     * Loads a version table from a file. Previous content in the table is
     * discarded.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public synchronized void load() throws IOException {
        
        if (vtFile == null)
            throw new IOException("no source file specified");
        
        vt.clear();
        
        FileInputStream fi = new FileInputStream(vtFile);
        ReusableBuffer buf = BufferPool.allocate((int) vtFile.length());
        fi.getChannel().read(buf.getBuffer());
        buf.position(0);
        
        while (buf.position() < buf.limit()) {
            
            final long timestamp = buf.getLong();
            final long fileSize = buf.getLong();
            final long numObjs = buf.getLong();
            
            assert (numObjs <= Integer.MAX_VALUE) : "number of objects: " + numObjs + ", current limit = "
                + Integer.MAX_VALUE;
            // TODO: solve this problem for files with more than
            // Integer.MAX_VALUE objects
            
            final int[] objVersions = new int[(int) numObjs];
            for (int i = 0; i < objVersions.length; i++)
                objVersions[i] = buf.getInt();
            
            addVersion(timestamp, objVersions, fileSize);
        }
        
        BufferPool.free(buf);
        fi.close();
        
    }
    
    /**
     * Stores the current content of the version table in a file.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public synchronized void save() throws IOException {
        
        if (vtFile == null)
            throw new IOException("no target file specified");
        
        FileOutputStream fo = new FileOutputStream(vtFile);
        
        for (Entry<Long, Version> entry : vt.entrySet()) {
            
            ReusableBuffer buf = BufferPool.allocate((Long.SIZE / 8) * 3 + entry.getValue().getObjCount()
                * Integer.SIZE / 8);
            buf.putLong(entry.getKey());
            buf.putLong(entry.getValue().getFileSize());
            buf.putLong(entry.getValue().getObjCount());
            for (int i = 0; i < entry.getValue().getObjCount(); i++)
                buf.putInt(entry.getValue().getObjVersion(i));
            
            fo.write(buf.array());
            BufferPool.free(buf);
        }
        
        fo.close();
    }
    
    /**
     * Returns the latest version of a file stored in the table before the given
     * timestamp.
     * 
     * @param timestamp
     *            the time stamp
     * @return the latest file version before <code>timestamp</code>.
     */
    public Version getLatestVersionBefore(long timestamp) {
        try {
            return vt.get(vt.headMap(timestamp).lastKey());
        } catch (NoSuchElementException exc) {
            // if there is no file version before the given timestamp, return an
            // empty file
            return Version.EMPTY_VERSION;
        }
    }
    
    /**
     * Adds a new file version to the table.
     * 
     * @param timestamp
     *            the time stamp attached to the file version
     * @param objVersions
     *            the set of object versions attached to the file version
     */
    public void addVersion(long timestamp, int[] objVersions, long fileSize) {
        vt.put(timestamp, new Version(objVersions, fileSize));
    }
    
    /**
     * Deletes a file version from the table.
     * 
     * @param timestamp
     *            the timestamp attached to the file version
     */
    public void deleteVersion(long timestamp) {
        vt.remove(timestamp);
    }
    
    /**
     * Determines the set of object versions that can be cleaned up, under the
     * assumption that snapshots with the given timestamps exist. The
     * corresponding file versions are removed from the version table.
     * 
     * @param timestamps
     *            a list of timestamps for which the object versions need to be
     *            preserved
     * @return a mapping from object numbers to sets of object versions that
     *         are obsolete and may be deleted
     */
    public synchronized Map<Integer, Set<Integer>> cleanup(long[] timestamps) {
        
        SortedMap<Long, Version> cleanedTable = new TreeMap<Long, Version>(vt);
        
        Map<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
        
        // first, determine all file versions that are superseeded
        
        Long[] tsArray = cleanedTable.keySet().toArray(new Long[cleanedTable.size()]);
        
        // no object versions to delete if no file versions exist
        if (tsArray.length == 0)
            return result;
        
        // otherwise, check which versions are superseeded
        SortedMap<Long, Version> superseeded = new TreeMap<Long, Version>();
        for (int i = 0; i < tsArray.length; i++) {
            
            long currentTs = tsArray[i];
            long nextTs = i == tsArray.length - 1? Long.MAX_VALUE: tsArray[i + 1];
            
            // check if there is a timestamp t in 'timestamps' with currentTs -
            // d_max < t < nextTs + d_max
            boolean isSuperseeded = true;
            for (long t : timestamps)
                if (currentTs - D_MAX < t && t < nextTs + D_MAX) {
                    isSuperseeded = false;
                    break;
                }
            
            if (isSuperseeded)
                superseeded.put(currentTs, cleanedTable.get(currentTs));
            
        }
        
        // remove all superseeded versions
        for (long ts : superseeded.keySet())
            cleanedTable.remove(ts);
        
        // for all superseeded versions, check which objects do not occur in
        // non-superseeded versions
        for (Version v: superseeded.values()) {
            
            for (int i = 0; i < v.objVersions.length; i++) {
                
                int version = v.objVersions[i];
                if (!isContained(i, version, cleanedTable)) {
                    Set<Integer> versions = result.get(i);
                    if (versions == null) {
                        versions = new HashSet<Integer>();
                        result.put(i, versions);
                    }
                    versions.add(version);
                }
            }
        }
        
        vt = cleanedTable;
        
        return result;
    }
    
    /**
     * Returns the total number of versions stored in the table.
     * 
     * @return the number of versions
     */
    public long getVersionCount() {
        return vt.size();
    }
    
    /**
     * Checks if the given version of the given object is contained in any of
     * the file versions.
     * 
     * @param objNo
     *            the object number
     * @param objVer
     *            the object version
     * @return <code>true</code>, if it is contained, <code>false</code>,
     *         otherwise
     */
    public boolean isContained(long objNo, long objVer) {
        return isContained(objNo, objVer, vt);
    }
    
    private static boolean isContained(long objNo, long objVer, SortedMap<Long, Version> vtable) {
        
        assert (objNo <= Integer.MAX_VALUE);
        
        if (objVer == 0)
            return false;
        
        for (Version versions : vtable.values())
            if (objNo < versions.getObjCount() && objVer == versions.getObjVersion((int) objNo))
                return true;
        
        return false;
    }
    
}
