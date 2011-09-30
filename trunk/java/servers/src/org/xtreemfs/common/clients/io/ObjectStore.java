/*
 * Copyright (c) 2008 by Nele Andersen,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.io;

import java.io.IOException;

import org.xtreemfs.foundation.buffer.ReusableBuffer;

public interface ObjectStore {

    /**
     * read an object from an OSD.
     * @param offset offset within the object
     * @param objectNo object number (0 is the first object in a file)
     * @param length number of bytes to read
     * @return the data read. In case of an EOF the buffer's length will be smaller than requested!
     * @throws java.io.IOException
     * @throws org.xtreemfs.foundation.json.JSONException
     * @throws java.lang.InterruptedException
     * @throws org.xtreemfs.common.clients.HttpErrorException
     */
    ReusableBuffer readObject(long objectNo, int offset, int length) throws IOException,
    InterruptedException;

    void writeObject(long offset, long objectNo, ReusableBuffer buffer) throws IOException,
     InterruptedException;
}
