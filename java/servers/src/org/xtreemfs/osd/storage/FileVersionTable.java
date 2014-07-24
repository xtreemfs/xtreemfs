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
import java.util.Map.Entry;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 * This class implements a version table for a file. The version table maps time stamps to lists of object versions.
 * 
 * @author stender
 */
public class FileVersionTable extends VersionTable {

    private File vtFile;

    /**
     * Creates a new empty version table.
     * 
     * @param vtFile
     *            the file that persistently stores the table
     */
    public FileVersionTable(File vtFile) {
        super();
        this.vtFile = vtFile;
    }

    /**
     * Loads a version table from a file. Previous content in the table is discarded.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void doLoad() throws IOException {

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
    protected void doSave() throws IOException {

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
}
