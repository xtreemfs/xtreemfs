/*
 * Copyright (c) 2009 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.internal;

import java.util.List;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;

/**
 *
 * @author bjko
 */
public abstract class ObjectMapper {

    private StripingPolicy fileSP;

    protected ObjectMapper(StripingPolicy fileSP) {
        this.fileSP = fileSP;
    }

    public static ObjectMapper getMapper(StripingPolicy fileSP) {
        if (fileSP.getType() == StripingPolicyType.STRIPING_POLICY_RAID0)
            return new RAID0ObjectMapper(fileSP);
        else
            throw new RuntimeException("unknown striping policy type: "+fileSP.getType());
    }

    public abstract List<ObjectRequest> readRequest(int length, long fileOffset, Replica replica);

    public abstract List<ObjectRequest> writeRequest(ReusableBuffer data, long fileOffset, Replica replica);

    public static class ObjectRequest {
        private final long objNo;
        private final int offset;
        private final int length;
        private ReusableBuffer data;
        private final String osdUUID;

        public ObjectRequest(long objNo, int offset, int length, String osdUUID, ReusableBuffer data) {
            this.objNo = objNo;
            this.offset = offset;
            this.data = data;
            this.length = length;
            this.osdUUID = osdUUID;
        }

        /**
         * @return the objNo
         */
        public long getObjNo() {
            return objNo;
        }

        /**
         * @return the offset
         */
        public int getOffset() {
            return offset;
        }

        /**
         * @return the data
         */
        public ReusableBuffer getData() {
            return data;
        }

        /**
         * @return the length
         */
        public int getLength() {
            return length;
        }

        /**
         * @return the osdUUID
         */
        public String getOsdUUID() {
            return osdUUID;
        }

        /**
         * @param data the data to set
         */
        public void setData(ReusableBuffer data) {
            this.data = data;
        }

    }

}
