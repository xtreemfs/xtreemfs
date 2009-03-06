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

package org.xtreemfs.foundation.oncrpc.client;

import java.util.List;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.Serializable;


/**
 *
 * @author bjko
 */
public class ONCRPCRequest {

    private final List<ReusableBuffer>  requestBuffers;

    private List<ReusableBuffer>      responseFragments;

    /**
     * fragment which is currently sent
     */
    private int                       requestBufSendCount;

    private final RPCResponseListener listener;

    private long                      timeQueued;

    private final int                 xid;

    private final Object              attachment;

    ONCRPCRequest(RPCResponseListener listener, int xid, int programId, int versionId, int procedureId, Serializable response, Object attachment) {
        ONCRPCRequestHeader hdr = new ONCRPCRequestHeader(xid, programId, versionId,procedureId);
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        hdr.serialize(writer);
        response.serialize(writer);
        writer.flip();

        this.requestBuffers = writer.getBuffers();
        this.listener = listener;
        this.requestBufSendCount = 0;
        this.xid = xid;
        this.attachment = attachment;
    }

    ONCRPCRequest(RPCResponseListener listener, int xid, List<ReusableBuffer> requestBuffers, Object attachment) {
        this.requestBuffers = requestBuffers;
        this.listener = listener;
        this.requestBufSendCount = 0;
        this.xid = xid;
        this.attachment = attachment;
    }

    public Object getAttachment() {
        return attachment;
    }

    int getXID() {
        return this.xid;
    }

    int getRequestSize() {
        int size = 0;
        for (ReusableBuffer buf : requestBuffers) {
            size += buf.remaining();
        }
        return size;
    }

    ReusableBuffer getCurrentRequestBuffer() {
        return this.requestBuffers.get(requestBufSendCount);
    }

    boolean isLastRequestBuffer() {
        return requestBuffers.size() == requestBufSendCount+1;
    }

    void nextRequestBuffer() {
        assert(requestBuffers.size() > requestBufSendCount);
        requestBufSendCount++;
    }

    void queued() {
        this.timeQueued = TimeSync.getLocalSystemTime();
    }
    
    long getTimeQueued() {
        return this.timeQueued;
    }

    RPCResponseListener getListener() {
        return listener;
    }

    /**
     * @return the responseFragments
     */
    List<ReusableBuffer> getResponseFragments() {
        return responseFragments;
    }

    /**
     * @param responseFragments the responseFragments to set
     */
    void setResponseFragments(List<ReusableBuffer> responseFragments) {
        this.responseFragments = responseFragments;
    }

    public void deserializeResponse(Serializable msg) {
        msg.deserialize(responseFragments.get(0));
    }

    public void freeBuffers() {
        for (ReusableBuffer buf : requestBuffers) {
            BufferPool.free(buf);
        }
        if (responseFragments != null) {
            for (ReusableBuffer buf : responseFragments) {
                BufferPool.free(buf);
            }
        }
    }

    

    
    

}
