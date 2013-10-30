/*
 * Copyright (c) 2012 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.FileSystem.Statistics;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Represents an InputStream user when reading a file with HDFS.
 */
public class XtreemFSInputStream extends FSInputStream {

    private long position = 0;

    private UserCredentials userCredentials;
    
    private String fileName;
    
    private FileHandle fileHandle;
    
    private Statistics statistics;
    
    public XtreemFSInputStream(UserCredentials userCredentials, FileHandle fileHandle, String fileName, Statistics statistics) {
        this.userCredentials = userCredentials;
        this.fileHandle = fileHandle;
        this.fileName = fileName;
        this.statistics = statistics;
    }

    @Override
    public synchronized void seek(long l) throws IOException {
        this.position = l;
    }

    @Override
    public synchronized long getPos() throws IOException {
        return position;
    }

    @Override
    public synchronized boolean seekToNewSource(long l) throws IOException {
        return false;
    }

    @Override
    public synchronized int read() throws IOException {
        byte[] buf = new byte[1];
        int numRead = fileHandle.read(userCredentials, buf, 1, position);
        if (numRead == 0) {
            return -1;
        }
        seek(getPos() + 1);
        statistics.incrementBytesRead(1);
        return (int) (buf[0] & 0xFF);
    }

    @Override
    public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
        int bytesRead = fileHandle.read(userCredentials, bytes, offset, length, getPos());
        if ((bytesRead == 0) && (length > 0)) {
            return -1;
        }
        seek(getPos() + bytesRead);
        statistics.incrementBytesRead(bytesRead);
        return bytesRead;
    }

    @Override
    public synchronized int read(long position, byte[] bytes, int offset, int length) throws IOException {
        int bytesRead = fileHandle.read(userCredentials, bytes, offset, length, position);
        if ((bytesRead == 0) && (length > 0)) {
            return -1;
        }
        statistics.incrementBytesRead(bytesRead);
        return bytesRead;
    }

    @Override
    public synchronized int read(byte[] bytes) throws IOException {
        return read(position, bytes, 0, bytes.length);
    }

    @Override
    public synchronized void close() throws IOException {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Closing file %s", fileName);
        }
        super.close();
        fileHandle.close();
    }
}
