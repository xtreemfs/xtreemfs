/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.storage;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.ObjectData;

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

    private final ReusableBuffer data;

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

    public ObjectData getObjectData(boolean isLastObject) {
        if (isLastObject) {
            switch (status) {
                case EXISTS: return new ObjectData(data, 0, 0, checksumInvalidOnOSD);
                case DOES_NOT_EXIST: return new ObjectData(null,0,0,checksumInvalidOnOSD);
                case PADDING_OBJECT: throw new RuntimeException("padding object must not be last object!");
            }
        } else {
            switch (status) {
                case EXISTS: {
                    final int paddingZeros = getStripeSize()-data.capacity();
                    System.out.println("capacity: "+data.capacity()+"/"+data.remaining());
                    return new ObjectData(data,0,paddingZeros,checksumInvalidOnOSD);
                }
                case DOES_NOT_EXIST:
                case PADDING_OBJECT: {
                    return new ObjectData(data, 0, getStripeSize(), checksumInvalidOnOSD);
                }
            }
        }
        assert(false) : "should be unreachable";
        return null;
    }

    public ObjectData getObjectData(boolean isLastObject, int offset, int length) {
        if (offset+length > getStripeSize())
            throw new IllegalArgumentException("offset+length must be less than the stripe size");

        ObjectData tmp = getObjectData(isLastObject);

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
        System.out.println("zero padding: "+tmp.getZero_padding());
        return tmp;


    }
    
        /**
     * @return the data
     */
    public ReusableBuffer getData() {
        return data;
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
