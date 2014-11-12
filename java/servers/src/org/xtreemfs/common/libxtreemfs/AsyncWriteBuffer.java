/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;

/**
 * 
 * Stores all information needed for an asynchronous write.
 */
public class AsyncWriteBuffer {

    /**
     * Creates a new {@link AsyncWriteBuffer} which is using the osdUuidIterator from
     * {@link AsyncWriteHandler}
     * 
     */
    protected AsyncWriteBuffer(writeRequest writeRequest, ReusableBuffer data, int dataLength,
            FileHandleImplementation fileHandle) {
        this.writeRequest = writeRequest;
        this.data = data;
        this.dataLength = dataLength;
        this.fileHandle = fileHandle;

        this.osdUuid = null;
        this.useUuidIterator = true;
    }

    /**
     * Creates a new {@link AsyncWriteBuffer} with a own osdUuid
     */
    protected AsyncWriteBuffer(writeRequest writeRequest, ReusableBuffer data, int dataLength,
            FileHandleImplementation fileHandle, String osdUuid) {
        this.writeRequest = writeRequest;
        this.data = data;
        this.dataLength = dataLength;
        this.fileHandle = fileHandle;
        this.osdUuid = osdUuid;

        this.useUuidIterator = false;
    }

    /**
     * Additional information of the write request.
     */
    private writeRequest             writeRequest;

    /**
     * Actual payload of the write request.
     */
    private ReusableBuffer           data;

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

    protected ReusableBuffer getData() {
        return data;
    }

    protected int getDataLength() {
        return dataLength;
    }

    protected FileHandleImplementation getFileHandle() {
        return fileHandle;
    }

    protected boolean isUsingUuidIterator() {
        return useUuidIterator;
    }

    protected String getOsdUuid() {
        return osdUuid;
    }

    /**
     * @param osdUuid
     */
    protected void setOsdUuid(String osdUuid) {
        this.osdUuid = osdUuid;
    }
};
