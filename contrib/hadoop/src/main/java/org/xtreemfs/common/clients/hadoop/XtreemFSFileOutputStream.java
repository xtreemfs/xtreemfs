/*
 * Copyright (c) 2012 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;

/**
 * 
 * Represents an Outputstream used when writing a file with HDFS.
 */
public class XtreemFSFileOutputStream extends OutputStream {
    private long            position = 0;

    private UserCredentials userCredentials;

    private FileHandle      fileHandle;

    private String          fileName;

    private boolean         useBuffer;

    private ByteBuffer      buffer;
    
    private boolean         closed;

    public XtreemFSFileOutputStream(UserCredentials userCredentials, FileHandle fileHandle, String fileName,
            boolean useBuffer, int bufferSize) throws IOException {
        this(userCredentials, fileHandle, fileName, useBuffer, bufferSize, false);
    }

    public XtreemFSFileOutputStream(UserCredentials userCredentials, FileHandle fileHandle, String fileName,
            boolean useBuffer, int bufferSize, boolean append) throws IOException {
        this.userCredentials = userCredentials;
        this.fileHandle = fileHandle;
        this.fileName = fileName;
        this.useBuffer = useBuffer;
        this.closed = false;
        
        if (useBuffer) {
            this.buffer = ByteBuffer.allocateDirect(bufferSize);
        }

        // Set position to end of file for append operation.
        if (append) {
            Stat stat = fileHandle.getAttr(userCredentials);
            if (stat == null) {
                throw new IOException("Cannot stat file '" + fileName + "'");
            }
            position = stat.getSize();
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        byte[] data = new byte[1];
        data[0] = (byte) b;
        if (useBuffer) {
            writeToBuffer(data, 0, 1);
        } else {
            int writtenBytes = fileHandle.write(userCredentials, data, 1, position);
            position += writtenBytes;
        }

    }

    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        
        if (useBuffer) {
            writeToBuffer(b, off, len);
        } else {
            int writtenBytes = fileHandle.write(userCredentials, b, off, len, position);
            position += writtenBytes;
        }
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

        if (useBuffer && buffer.position() > 0) {
            // If buffer has remaining content, write content to file.
            byte[] bytesWriteToFile = flushBuffer();
            fileHandle.write(userCredentials, bytesWriteToFile, bytesWriteToFile.length, position);
            buffer = null;
        }
        super.close();
        fileHandle.close();
        closed = true;
    }

    private synchronized void writeToBuffer(byte b[], int off, int len) throws IOException {
        if (buffer.remaining() > len) {
            // Write content to buffer.
            buffer.put(b, off, len);
        } else {
            // Flush buffer and write content + last write request to file.
            byte[] buffercontent = flushBuffer(); 
            byte[] bytesWriteToFile = new byte[buffercontent.length + len];
            System.arraycopy(buffercontent, 0, bytesWriteToFile, 0, buffercontent.length);
            System.arraycopy(b, off, bytesWriteToFile, buffercontent.length, len);
            int writtenBytes = fileHandle
                    .write(userCredentials, bytesWriteToFile, bytesWriteToFile.length, position);
            position += writtenBytes;
        }
    }
    
    private byte[] flushBuffer() {
        int bytesInBuffer = buffer.position();
        buffer.clear();
        byte[] buffercontent = new byte[bytesInBuffer];
        buffer.get(buffercontent, 0, bytesInBuffer);
        buffer.clear();
        return buffercontent;
    }

}
