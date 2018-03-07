/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.osd.InternalObjectData;

/**
 *
 * @author bjko
 */
public class ObjectInformation {

    /**
     * @return the stripeSize
     */
    public int getStripeSize() {
        return stripeSize;
    }

    /**
     * @return the checksumInvalidOnOSD
     */
    public boolean isChecksumInvalidOnOSD() {
        return checksumInvalidOnOSD;
    }

    /**
     * @param checksumInvalidOnOSD the checksumInvalidOnOSD to set
     */
    public void setChecksumInvalidOnOSD(boolean checksumInvalidOnOSD) {
        this.checksumInvalidOnOSD = checksumInvalidOnOSD;
    }

    public static enum ObjectStatus {
        /**
         * object exists on disk, data is available
         */
        EXISTS,
        /**
         * object does not exist on disk
         */
        DOES_NOT_EXIST,
        /**
         * object is an empty (file size == 0) placeholder
         * for a fully zero padded object
         */
        PADDING_OBJECT
    };

    private ReusableBuffer data;

    private final ObjectStatus   status;

    private final int            stripeSize;

    private long                 lastLocalObjectNo;

    private long                 globalLastObjectNo;

    private boolean              checksumInvalidOnOSD;

    public ObjectInformation(ObjectStatus status, ReusableBuffer data, int stripeSize) {
        this.data = data;
        this.status = status;
        this.stripeSize = stripeSize;
    }

    public InternalObjectData getObjectData(boolean isLastObject, int offset, int length) {
        assert(length >= 0);
        if (isLastObject) {
            switch (status) {
                case EXISTS: return new InternalObjectData(0, checksumInvalidOnOSD, 0, data);
                case DOES_NOT_EXIST: return new InternalObjectData(0,checksumInvalidOnOSD, 0, null);
                case PADDING_OBJECT: throw new RuntimeException("padding object must not be last object!");
            }
        } else {
            switch (status) {
                case EXISTS: {
                    final int paddingZeros = length-data.remaining();
                    assert(paddingZeros >= 0) : "offset: "+offset+" length: "+length+" remaining: "+data.remaining();
                    return new InternalObjectData(0,checksumInvalidOnOSD, paddingZeros, data);
                }
                case DOES_NOT_EXIST:
                case PADDING_OBJECT: {
                    return new InternalObjectData(0, checksumInvalidOnOSD, length, data);
                }
            }
        }
        assert(false) : "should be unreachable";
        return null;
    }

    /*public ObjectData getObjectData(boolean isLastObject, int offset, int length) {
        if (offset+length > getStripeSize())
            throw new IllegalArgumentException("offset+length must be less than the stripe size");

        ObjectData tmp = getObjectData(isLastObject,length);




        final int dataLength = tmp.getZero_padding() + ((tmp.getData() != null) ? tmp.getData().remaining() : 0);

        if (dataLength < getStripeSize()) {
            //this is an EOF
            assert(tmp.getZero_padding() == 0);
            if (tmp.getData() != null) {
                final int bufLength = tmp.getData().remaining();
                if (offset > bufLength) {
                    //no data to be sent
                    BufferPool.free(tmp.getData());
                    tmp.setData(null);
                } else {
                    //create a range
                    final int newLength = (bufLength > offset+length) ? length : bufLength-offset;
                    tmp.getData().range(offset,newLength);
                }
            }
        } else {
            //full object
            if (tmp.getData() == null) {
                //padding object
                tmp.setZero_padding(length);
            } else {
                final int bufLength = tmp.getData().remaining();
                int newBufLen;
                if (offset >= bufLength) {
                    BufferPool.free(tmp.getData());
                    tmp.setData(null);
                    //adapt zeros
                    //object is a full object, so length is all zeros
                    tmp.setZero_padding(length);
                } else {
                    //buffer stays
                    if (offset+length > bufLength) {
                        //goes beyond buffer
                        tmp.getData().range(offset,bufLength-offset);
                    } else {
                        tmp.getData().range(offset,length);
                    }
                    //fill up the rest with zeros
                    tmp.setZero_padding(length-tmp.getData().remaining());
                }
            }
        }
        return tmp;


    }*/
    
        /**
     * @return the data
     */
    public ReusableBuffer getData() {
        return data;
    }

    public void setData(ReusableBuffer data) {
        this.data = data;
    }

    /**
     * @return the status
     */
    public ObjectStatus getStatus() {
        return status;
    }

        /**
     * @return the lastLocalObjectNo
     */
    public long getLastLocalObjectNo() {
        return lastLocalObjectNo;
    }

    /**
     * @param lastLocalObjectNo the lastLocalObjectNo to set
     */
    public void setLastLocalObjectNo(long lastLocalObjectNo) {
        this.lastLocalObjectNo = lastLocalObjectNo;
    }

    /**
     * @return the globalLastObjectNo
     */
    public long getGlobalLastObjectNo() {
        return globalLastObjectNo;
    }

    /**
     * @param globalLastObjectNo the globalLastObjectNo to set
     */
    public void setGlobalLastObjectNo(long globalLastObjectNo) {
        this.globalLastObjectNo = globalLastObjectNo;
    }

}
