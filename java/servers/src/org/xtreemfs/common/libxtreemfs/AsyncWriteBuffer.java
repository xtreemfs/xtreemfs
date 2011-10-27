/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;

/**
 * 
 * <br>
 * Oct 27, 2011
 */
public class AsyncWriteBuffer {

    /**
     * @remark Ownership of "writeRequest" is transferred to this object.
     */
    protected AsyncWriteBuffer(writeRequest writeRequest, char[] data, int dataLength,
            FileHandleImplementation fileHandle) {
    }

    /**
     * @remark Ownership of write_request is transferred to this object.
     */
    protected AsyncWriteBuffer(writeRequest writeRequest, char[] data, int dataLengt,
            FileHandleImplementation fileHandle, String osdUuid) {

        this.writeRequest = writeRequest;
        this.data = data;
        this.dataLength = dataLengt;
        this.fileHandle = fileHandle;
        this.osdUuid = osdUuid;

    }

    /**
     * Additional information of the write request.
     */
    private writeRequest             writeRequest;

    /**
     * Actual payload of the write request.
     */
    private char[]                   data;

    /**
     * Length of the payload.
     */
    private int                      dataLength;

    /**
     * FileHandle which did receive the write() command.
     */
    private FileHandleImplementation fileHandle;

    /**
     * Set to false if the member "osdUuid" is used instead of the FileInfo's osdUuidIterator in order to
     * determine the OSD to be used.
     */
    private boolean                  useUuidIterator;

    /**
     * UUID of the OSD which was used for the last retry or if useUuidIterator is false, this variable is
     * initialized to the OSD to be used.
     */
    private String                   osdUuid;

    protected writeRequest getWriteRequest() {
        return writeRequest;
    }

    protected char[] getData() {
        return data;
    }

    protected int getDataLength() {
        return dataLength;
    }

    protected FileHandleImplementation getFileHandle() {
        return fileHandle;
    }

    protected boolean isUseUuidIterator() {
        return useUuidIterator;
    }

    protected String getOsdUuid() {
        return osdUuid;
    }
};
