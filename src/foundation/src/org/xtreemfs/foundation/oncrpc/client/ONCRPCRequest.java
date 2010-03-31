/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.client;

import java.util.List;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;


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

    long startT, endT;

    ONCRPCRequest(RPCResponseListener listener, int xid, int programId, int versionId, int procedureId, yidl.runtime.Object response, Object attachment,
            yidl.runtime.Object credentials) {
        ONCRPCRequestHeader hdr = new ONCRPCRequestHeader(xid, programId, versionId,procedureId,credentials);
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        hdr.marshal(writer);
        response.marshal(writer);
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

    public void deserializeResponse(yidl.runtime.Object msg) {
        msg.unmarshal(new XDRUnmarshaller(responseFragments.get(0)));
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

    /**
     * duration of request from sending the request until the response
     * was received completeley.
     * @return duration in ns
     */
    public long getDuration() {
        if (RPCNIOSocketClient.ENABLE_STATISTICS) {
            return endT-startT;
        } else {
            return 0l;
        }
    }
}
