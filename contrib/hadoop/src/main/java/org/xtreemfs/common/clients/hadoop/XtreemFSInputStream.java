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

    private long            position           = 0;

    // Used by the buffer to determine the position in the file.
    private long            bufferFilePosition = 0;

    private UserCredentials userCredentials;

    private String          fileName;

    private FileHandle      fileHandle;

    private Statistics      statistics;

    private boolean         useBuffer;

    private ByteBuffer      buffer;

    private boolean         EOF                = false;
    
    private boolean         closed;

    public XtreemFSInputStream(UserCredentials userCredentials, FileHandle fileHandle, String fileName,
            boolean useBuffer, int bufferSize, Statistics statistics) throws IOException {
        this.userCredentials = userCredentials;
        this.fileHandle = fileHandle;
        this.fileName = fileName;
        this.statistics = statistics;
        this.useBuffer = useBuffer;
        this.closed = false;
        
        if (useBuffer) {
            this.buffer = ByteBuffer.allocateDirect(bufferSize);
            buffer.position(buffer.capacity());
        }
    }

    @Override
    public synchronized void seek(long l) throws IOException {
        this.position = l;
        if (useBuffer) {
            this.bufferFilePosition = l;
            buffer.position(buffer.limit());
        }
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
        byte[] data = new byte[1];
        int bytesRead = 0;
        if (useBuffer) {
            bytesRead = readFromBuffer(data, 0, 1);
        } else {
            bytesRead = fileHandle.read(userCredentials, data, 1, position);
        }

        if (bytesRead == 0) {
            return -1;
        }
        position += 1;
        statistics.incrementBytesRead(1);
        return (int) (data[0] & 0xFF);
    }

    @Override
    public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
        int bytesRead = 0;
        if (useBuffer) {
            bytesRead = readFromBuffer(bytes, offset, length);
        } else {
            bytesRead = fileHandle.read(userCredentials, bytes, offset, length, position);
        }
        if ((bytesRead == 0) && (length > 0)) {
            return -1;
        }
        position += bytesRead;
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
        return read(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            Logging.logMessage(Logging.LEVEL_WARN, this,
                    "Ignoring attempt to close already closed file %s", fileName);
            return;
        }
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Closing file %s", fileName);
        }
        super.close();
        fileHandle.close();
        closed = true;
    }
    
    // For use by external systems, such as the StatisticsFileSystem
    // https://github.com/robert-schmidtke/hdfs-statistics-adapter
    public String getCurrentDatanodeHostName() {
        return fileHandle.getLastOSDAddress();
    }

    private int readFromBuffer(byte[] bytes, int offset, int length) throws IOException {
        if (EOF || length == 0) {
            return 0;
        }

        if (buffer.remaining() >= length) {
            // Read from buffer.
            buffer.get(bytes, offset, length);
            return length;
        } else {
            int bytesLeftToRead = length;
            int newBytesOffset = offset;
            int bytesReadFromBuffer = 0;

            if (buffer.hasRemaining()) {
                // Read remaining bytes from buffer.
                bytesReadFromBuffer = buffer.remaining();
                buffer.get(bytes, offset, buffer.remaining());
                bytesLeftToRead -= bytesReadFromBuffer;
                newBytesOffset += bytesReadFromBuffer;
            }

            // Fill buffer.
            byte[] tmp = new byte[buffer.capacity()];
            int bytesRead = fileHandle.read(userCredentials, tmp, 0, tmp.length, bufferFilePosition);

            if (bytesRead == 0) {
                EOF = true;
                return bytesReadFromBuffer;
            } else {
                bufferFilePosition += bytesRead;

                // Put file content in buffer.
                buffer.clear();
                buffer.put(tmp, 0, bytesRead);
                buffer.position(0);
                buffer.limit(bytesRead);

                // Read left bytes from buffer.
                return bytesReadFromBuffer + readFromBuffer(bytes, newBytesOffset, bytesLeftToRead);
            }
        }
    }
}
