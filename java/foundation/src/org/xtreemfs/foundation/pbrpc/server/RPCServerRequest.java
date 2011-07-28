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
import java.net.SocketAddress;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 *
 * @author bjko
 */
public class RPCServerRequest {

    private RPC.RPCHeader  header;
    private ReusableBuffer message;
    private ReusableBuffer data;
    private final RPCServerConnectionInterface connection;

    public RPCServerRequest(RPCServerConnectionInterface connection, ReusableBuffer headerBuffer, ReusableBuffer message, ReusableBuffer data) throws IOException {
        try {
            ReusableBufferInputStream rbis = new ReusableBufferInputStream(headerBuffer);
            header = RPC.RPCHeader.parseFrom(rbis);
            this.message = message;
            this.data = data;
            this.connection = connection;
        } finally {
            BufferPool.free(headerBuffer);
        }
    }

    public RPCServerRequest(RPCServerConnectionInterface connection, RPC.RPCHeader header, ReusableBuffer message) {
        this.header = header;
        this.message = message;
        this.data = null;
        this.connection = connection;
    }

    public RPC.RPCHeader getHeader() {
        return header;
    }

    /**
     * @return the message
     */
    public ReusableBuffer getMessage() {
        return message;
    }

    /**
     * @return the data
     */
    public ReusableBuffer getData() {
        return data;
    }

    public void freeBuffers() {
        BufferPool.free(message);
        BufferPool.free(data);
    }

    public void sendError(RPC.ErrorType type, RPC.POSIXErrno errno, String message, String debugInfo) {
        RPC.RPCHeader.ErrorResponse resp = RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(type).setPosixErrno(errno).setErrorMessage(message).setDebugInfo(debugInfo).build();
        sendError(resp);
    }

    public void sendError(RPC.ErrorType type, RPC.POSIXErrno errno, String message) {
        RPC.RPCHeader.ErrorResponse resp = RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(type).setPosixErrno(errno).setErrorMessage(message).build();
        sendError(resp);
    }

    public void sendRedirect(String target_uuid) {
        RPC.RPCHeader.ErrorResponse resp = RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.REDIRECT).setPosixErrno(RPC.POSIXErrno.POSIX_ERROR_NONE).setRedirectToServerUuid(target_uuid).build();
        sendError(resp);
    }

    public void sendError(RPC.RPCHeader.ErrorResponse error) {
        try {
            RPC.RPCHeader rqHdr = getHeader();
            RPC.RPCHeader respHdr = RPC.RPCHeader.newBuilder().setCallId(rqHdr.getCallId()).setMessageType(RPC.MessageType.RPC_RESPONSE_ERROR).setErrorResponse(error).build();
            RPCServerResponse response = new RPCServerResponse(respHdr, null, null);
            getConnection().getServer().sendResponse(this, response);
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void sendResponse(Message message, ReusableBuffer data) throws IOException {
        RPC.RPCHeader rqHdr = getHeader();
        RPC.RPCHeader respHdr = RPC.RPCHeader.newBuilder().setCallId(rqHdr.getCallId()).setMessageType(RPC.MessageType.RPC_RESPONSE_SUCCESS).build();
        RPCServerResponse response = new RPCServerResponse(respHdr, message, data);
        getConnection().getServer().sendResponse(this, response);
    }

    public SocketAddress getSenderAddress() {
        return connection.getSender();
    }

    /**
     * @return the connection
     */
    public RPCServerConnectionInterface getConnection() {
        return connection;
    }

    public String toString() {
        try {
            RPC.RPCHeader hdr = getHeader();
            String headerContent = "";
            if (hdr.getMessageType() == RPC.MessageType.RPC_REQUEST) {
                headerContent = ",proc="+hdr.getRequestHeader().getProcId()+",interf="+hdr.getRequestHeader().getInterfaceId();
            }
            return this.getClass().getCanonicalName()+": callid="+hdr.getCallId()+", type="+hdr.getMessageType()+headerContent;
        } catch (Exception ex) {
            return this.getClass().getCanonicalName()+": unparseable data: "+ex;
        }
    }


}

