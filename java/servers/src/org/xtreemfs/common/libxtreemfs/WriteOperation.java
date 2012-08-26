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

    private long            objNumber;

    private int            osdOffset;

    private int            reqSize;

    private int            reqOffset;
    
    private ReusableBuffer buf;

    protected WriteOperation(long objNumber, int osdOffset, int reqSize, int reqOffset, ReusableBuffer buf) {
        this.objNumber = objNumber;
        this.osdOffset = osdOffset;
        this.reqSize = reqSize;
        this.reqOffset = reqOffset;
        this.buf = buf;
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

    
    protected ReusableBuffer getReqData() {
        return buf;
    }
}
