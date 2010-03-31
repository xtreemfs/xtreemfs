/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.osd.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.NoSuchElementException;
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
    
    public static class Version {
        
        protected static final Version EMPTY_VERSION = new Version(new int[0], 0);
        
        private int[]               objVersions;
        
        private long                fileSize;
        
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
            assert(objNo <= Integer.MAX_VALUE);
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
    public void load() throws IOException {
        
        if (vtFile == null)
            throw new IOException("no source file specified");
        
        vt.clear();
        
        FileInputStream fi = new FileInputStream(vtFile);
        MappedByteBuffer map = fi.getChannel().map(MapMode.READ_ONLY, 0, vtFile.length());
        
        while (map.position() < map.limit()) {
            
            final long timestamp = map.getLong();
            final long fileSize = map.getLong();
            final long numObjs = map.getLong();
            
            assert (numObjs <= Integer.MAX_VALUE) : "number of objects: " + numObjs + ", current limit = "
                + Integer.MAX_VALUE;
            // TODO: solve this problem for files with more than
            // Integer.MAX_VALUE objects
            
            final int[] objVersions = new int[(int) numObjs];
            for (int i = 0; i < objVersions.length; i++)
                objVersions[i] = map.getInt();
            
            addVersion(timestamp, objVersions, fileSize);
        }
        
        fi.close();
        
    }
    
    /**
     * Stores the current content of the version table in a file.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void save() throws IOException {
        
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
        
        assert (objNo <= Integer.MAX_VALUE);
        
        if (objVer == 0)
            return false;
        
        for (Version versions : vt.values())
            if (objNo < versions.getObjCount() && objVer == versions.getObjVersion((int) objNo))
                return true;
        
        return false;
    }
    
}
