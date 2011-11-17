/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 * 
 * Represents a read operation for the StripeTranslator.
 */
public class ReadOperation {

    private int    objNumber;

    private int    osdOffset;

    private int    reqSize;

    private int    reqOffset;

    /**
     *  The position where the result of this request should put into the global Buffer.
     */
    private int    bufferStart;

    protected ReadOperation(int objNumber, int osdOffset, int reqSize, int reqOffset, int bufferStart) {

        this.objNumber = objNumber;
        this.osdOffset = osdOffset;
        this.reqSize = reqSize;
        this.reqOffset = reqOffset;
        this.bufferStart = bufferStart ;

    }

    protected int getObjNumber() {
        return objNumber;
    }

    protected void setObjNumber(int objNumber) {
        this.objNumber = objNumber;
    }

    public int getOsdOffset() {
        return osdOffset;
    }

    public void setOsdOffset(int osdOffset) {
        this.osdOffset = osdOffset;
    }

    protected int getReqSize() {
        return reqSize;
    }

    protected void setReqSize(int reqSize) {
        this.reqSize = reqSize;
    }

    protected int getReqOffset() {
        return reqOffset;
    }

    protected void setReqOffset(int reqOffset) {
        this.reqOffset = reqOffset;
    }

    protected int getBufferStart() {
        return bufferStart;
    }

    protected void setData(int bufferStart) {
        this.bufferStart  = bufferStart;
    }
}
