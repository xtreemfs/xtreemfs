/*
 * Copyright (c) 2008 by Nele Andersen, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.io;

public interface ByteMapper {

    /**
     * reads data from file.
     * @param data a buffer of length (length+offset) in which the data is stored
     * @param offset offset within buffer to write to
     * @param length number of bytes to read
     * @param filePosition offset within file
     * @return the number of bytes read
     * @throws java.lang.Exception
     */
    public int read(byte[] data, int offset, int length, long filePosition) throws Exception;

    /**
     * writes data to a file.
     * @param data the data to write (buffer must be length+offset bytes long).
     * @param offset the position within the buffer to start at.
     * @param length number of bytes to write
     * @param filePosition the offset within the file
     * @return the number of bytes written
     * @throws java.lang.Exception
     */
    public int write(byte[] data, int offset, int length, long filePosition) throws Exception;
}
