/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;


/**
 * 
 * Represents a read operation for the StripeTranslator.
 */
public class ReadOperation {

    private long    objNumber;

    private int    osdOffset;

    private int    reqSize;

    private int    reqOffset;

    /**
     *  The position where the result of this request should put into the global Buffer.
     */
    private int    bufferStart;

    protected ReadOperation(long objNumber, int osdOffset, int reqSize, int reqOffset, int bufferStart) {

        this.objNumber = objNumber;
        this.osdOffset = osdOffset;
        this.reqSize = reqSize;
        this.reqOffset = reqOffset;
        this.bufferStart = bufferStart ;

    }

    protected long getObjNumber() {
        return objNumber;
    }

    public int getOsdOffset() {
        return osdOffset;
    }

    protected int getReqSize() {
        return reqSize;
    }

    protected int getReqOffset() {
        return reqOffset;
    }

    protected int getBufferStart() {
        return bufferStart;
    }

}
