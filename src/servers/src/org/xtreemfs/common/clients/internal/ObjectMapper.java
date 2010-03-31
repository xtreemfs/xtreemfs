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
package org.xtreemfs.common.clients.internal;

import java.util.List;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;

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
            throw new RuntimeException("unknown striping policy type: "+fileSP.getTypeName());
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
