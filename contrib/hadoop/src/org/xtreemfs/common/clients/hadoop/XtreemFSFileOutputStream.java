/*
 * Copyright (c) 2012 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.IOException;
import java.io.OutputStream;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * 
 * Represents an Outputstream used when writing a file with HDFS.
 */
public class XtreemFSFileOutputStream extends OutputStream {
    private int             position = 0;

    private UserCredentials userCredentials;

    private FileHandle      fileHandle;

    private String          fileName;

    public XtreemFSFileOutputStream(UserCredentials userCredentials, FileHandle fileHandle, String fileName) {
        this.userCredentials = userCredentials;
        this.fileHandle = fileHandle;
        this.fileName = fileName;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        byte[] data = new byte[1];
        data[0] = (byte) b;
        int writtenBytes = fileHandle.write(userCredentials, data, 1, position);
        position += writtenBytes;
    }

    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
                || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        int writtenBytes = fileHandle.write(userCredentials, b, off, len, position);
        position += writtenBytes;
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
