/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.server;

import com.google.protobuf.Message;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferOutputStream;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.utils.RecordMarker;

/**
 *
 * @author bjko
 */
public class RPCServerResponse {

    final int callId;

    final ReusableBuffer[] buffers;
    final int hdrLen;
    final int msgLen;
    final int dataLen;

    public RPCServerResponse(RPC.RPCHeader header, Message message, ReusableBuffer data) throws IOException {
        ReusableBufferOutputStream os = new ReusableBufferOutputStream(ReusableBufferOutputStream.BUFF_SIZE);
        callId = header.getCallId();

        hdrLen = header.getSerializedSize();
        msgLen = (message != null) ? message.getSerializedSize() : 0;
        dataLen = (data != null) ? data.capacity() : 0;

        assert(hdrLen > 0);
        assert(msgLen >= 0);
        assert(dataLen >= 0);

        RecordMarker rm = new RecordMarker(hdrLen, msgLen, dataLen);
        rm.writeFragmentHeader(os);
        header.writeTo(os);
        if (message != null) {
            message.writeTo(os);
        } 
        if (data != null) {
            data.position(data.limit());
            os.appendBuffer(data);
        } 
        os.flip();
        buffers = os.getBuffers();
    }

    public ReusableBuffer[] getBuffers() {
        return buffers;
    }

    public ByteBuffer[] packBuffers(ByteBuffer recordMarker) {
        ByteBuffer[] arr = new ByteBuffer[buffers.length];
        for (int i = 0; i < buffers.length; i++)
            arr[i] = buffers[i].getBuffer();
        return arr;
    }

    public void freeBuffers() {
        for (int i = 0; i < buffers.length; i++) {
            BufferPool.free(buffers[i]);
            buffers[i] = null;
        }
    }

    public String toString() {
        return this.getClass().getCanonicalName()+": callid="+callId;
    }
    
    public int getRpcMessageSize() {
        return RecordMarker.HDR_SIZE + hdrLen + dataLen + msgLen;
    }
}
