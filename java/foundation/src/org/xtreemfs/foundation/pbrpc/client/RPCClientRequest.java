/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import com.google.protobuf.Message;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferOutputStream;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.utils.RecordMarker;


/**
 *
 * @author bjko
 */
public class RPCClientRequest<ReturnType extends Message> {

    private final RPC.RPCHeader    requestHeader;
    private RPC.RPCHeader          responseHeader;
    final ReusableBuffer[] buffers;
    final int              hdrLen;
    final int              msgLen;
    final int              dataLen;

    private final RPCResponse response;

    private long              timeQueued;
    private long              bytesWritten;


    RPCClientRequest(Auth authHeader, UserCredentials uCreds, int callId, int interfaceId, int procId, Message message, ReusableBuffer data, RPCResponse<ReturnType> response) throws IOException {
        if (uCreds == null) {
            throw new IOException("No UserCredentials object given (null). Make sure it's set.");
        }
        if (authHeader == null) {
            throw new IOException("No Auth object given (null). Make sure it's set.");
        }

        RPC.RPCHeader.RequestHeader rqHdr = RPC.RPCHeader.RequestHeader.newBuilder().setAuthData(authHeader).setUserCreds(uCreds).
                setInterfaceId(interfaceId).setProcId(procId).build();
        requestHeader = RPC.RPCHeader.newBuilder().setCallId(callId).setMessageType(RPC.MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();
        this.response = response;

        ReusableBufferOutputStream os = new ReusableBufferOutputStream(ReusableBufferOutputStream.BUFF_SIZE);
        requestHeader.writeTo(os);
        hdrLen = os.length();
        if (message != null) {
            message.writeTo(os);
            msgLen = os.length()-hdrLen;
        } else {
            msgLen = 0;
        }
        if (data != null) {
            os.appendBuffer(data);
            dataLen = data.limit();
        } else {
            dataLen = 0;
        }
        assert(hdrLen > 0);
        assert(msgLen >= 0);
        assert(dataLen >= 0);
        os.flip();
        buffers = os.getBuffers();
    }

    public ReusableBuffer[] getBuffers() {
        return buffers;
    }

    public ByteBuffer[] packBuffers(ByteBuffer recordMarker) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending record marker: %d/%d/%d", hdrLen,msgLen,dataLen);
        }
        recordMarker.putInt(hdrLen);
        recordMarker.putInt(msgLen);
        recordMarker.putInt(dataLen);
        recordMarker.flip();
        ByteBuffer[] arr = new ByteBuffer[buffers.length+1];
        arr[0] = recordMarker;
        for (int i = 0; i < buffers.length; i++) {
            arr[i+1] = buffers[i].getBuffer();
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "send buffer #%d: %s", i+1,buffers[i]);
            }
        }
        return arr;
    }

    public void freeBuffers() {
        for (int i = 0; i < buffers.length; i++) {
            BufferPool.free(buffers[i]);
            buffers[i] = null;
        }
    }

    /**
     * duration of request from sending the request until the response
     * was received completeley.
     * @return duration in ns
     */
    public long getDuration() {
        if (RPCNIOSocketClient.ENABLE_STATISTICS) {
            return 0;
        } else {
            return 0l;
        }
    }

    /**
     * @return the requestHeader
     */
    public RPC.RPCHeader getRequestHeader() {
        return requestHeader;
    }

    void queued() {
        this.timeQueued = TimeSync.getLocalSystemTime();
    }

    long getTimeQueued() {
        return this.timeQueued;
    }

    /**
     * @return the responseHeader
     */
    public RPC.RPCHeader getResponseHeader() {
        return responseHeader;
    }

    /**
     * @param responseHeader the responseHeader to set
     */
    public void setResponseHeader(RPC.RPCHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public RPCResponse<ReturnType> getResponse() {
        return response;
    }
    
    public void recordBytesWritten(long bytesWritten) {
        this.bytesWritten += bytesWritten;
        if (this.bytesWritten > RecordMarker.HDR_SIZE + hdrLen + dataLen + msgLen) {
            String errorMessage = "Too many bytes written (expected: "
                    + (RecordMarker.HDR_SIZE + hdrLen + dataLen + msgLen)
                    + ", actual: "
                    + this.bytesWritten
                    + ") for message "
                    + requestHeader;
            Logging.logMessage(Logging.LEVEL_ERROR, this, errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    public void checkEnoughBytesSent() {
        if (bytesWritten != RecordMarker.HDR_SIZE + hdrLen + dataLen + msgLen) {
            String errorMessage = "Not enough bytes written (expected: "
                    + (RecordMarker.HDR_SIZE + hdrLen + dataLen + msgLen)
                    + ", actual: "
                    + bytesWritten
                    + ") for message "
                    + requestHeader;
            Logging.logMessage(Logging.LEVEL_ERROR, this, errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }
}
