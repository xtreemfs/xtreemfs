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
 * <br>
 * Nov 2, 2011
 */
public class WriteOperation {

    private int            objNumber;

    private int            osdOffset;

    private int            reqSize;

    private int            reqOffset;
    
    private ReusableBuffer buf;

    protected WriteOperation(int objNumber, int osdOffset, int reqSize, int reqOffset, ReusableBuffer buf) {
        this.objNumber = objNumber;
        this.osdOffset = osdOffset;
        this.reqSize = reqSize;
        this.reqOffset = reqOffset;
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
    
    protected byte[] getReqData() {
        byte[] data = new byte[reqSize];
        buf.get(data, reqOffset, reqSize);
        return data;
    }
}
