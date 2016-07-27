/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;
import org.xtreemfs.foundation.pbrpc.utils.RecordMarker;

/**
 *
 * @author bjko
 */
public class RPCNIOSocketServerConnection implements RPCServerConnectionInterface {

    public enum ReceiveState {
        RECORD_MARKER,
        RPC_HEADER,
        RPC_MESSAGE,
        DATA
    };

    private final AtomicInteger openRequests;

    private Queue<RPCServerResponse>   pendingResponses;

    private final ChannelIO     channel;

    private final ByteBuffer    receiveRecordMarker;

    private final ByteBuffer    sendFragHdr;

    private ReusableBuffer[]    receiveBuffers;

    private ReceiveState        receiveState;

    private ByteBuffer[]        sendBuffers;

    private volatile boolean    connectionClosed;

    private SocketAddress       clientAddress;

    private RPCServerInterface  server;
    
    private long                bytesSent;
    
    private int                 expectedRecordSize;

    public RPCNIOSocketServerConnection(RPCServerInterface server, ChannelIO channel) {
        assert(server != null);
        assert(channel != null);
        this.channel = channel;
        this.openRequests = new AtomicInteger(0);
        this.pendingResponses = new ConcurrentLinkedQueue<RPCServerResponse>();
        this.connectionClosed = false;
        this.receiveRecordMarker = ByteBuffer.allocate(RecordMarker.HDR_SIZE);
        this.sendFragHdr = ByteBuffer.allocate(RecordMarker.HDR_SIZE);
        this.receiveState = ReceiveState.RECORD_MARKER;
        this.server = server;
        try {
            this.clientAddress = channel.socket().getRemoteSocketAddress();
        } catch (Exception ex) {
        }
    }

    /**
     * @return the receiveState
     */
    public ReceiveState getReceiveState() {
        return receiveState;
    }

    /**
     * @param receiveState the receiveState to set
     */
    public void setReceiveState(ReceiveState receiveState) {
        this.receiveState = receiveState;
    }

    @Override
    public RPCServerInterface getServer() {
        return server;
    }

    @Override
    public SocketAddress getSender() {
        return clientAddress;
    }

    public void freeBuffers() {
        if (receiveBuffers != null) {
            for (ReusableBuffer buffer : receiveBuffers)
                BufferPool.free(buffer);
        }
        for (RPCServerResponse r : pendingResponses) {
            r.freeBuffers();
        }
    }

    /**
     * @return the openRequests
     */
    public AtomicInteger getOpenRequests() {
        return openRequests;
    }

    /**
     * @return the channel
     */
    @Override
    public ChannelIO getChannel() {
        return channel;
    }


    /**
     * @return the connectionClosed
     */
    public boolean isConnectionClosed() {
        return connectionClosed;
    }

    /**
     * @param connectionClosed the connectionClosed to set
     */
    public void setConnectionClosed(boolean connectionClosed) {
        this.connectionClosed = connectionClosed;
    }

    /**
     * @return the pendingResponses
     */
    public Queue<RPCServerResponse> getPendingResponses() {
        return pendingResponses;
    }

    public void addPendingResponse(RPCServerResponse rq) {
        this.pendingResponses.add(rq);
    }

    /**
     * @return the fragmentHeader
     */
    ByteBuffer getReceiveRecordMarker() {
        return receiveRecordMarker;
    }

    /**
     * @return the fragmentHeader
     */
    ByteBuffer getSendFragHdr() {
        return sendFragHdr;
    }

    /**
     * @return the receive
     */
    ReusableBuffer[] getReceiveBuffers() {
        return receiveBuffers;
    }

    /**
     * @param receive the receive to set
     */
    void setReceiveBuffers(ReusableBuffer[] receive) {
        this.receiveBuffers = receive;
    }

    /**
     * @return the send
     */
    ByteBuffer[] getSendBuffers() {
        return sendBuffers;
    }

    /**
     * @param send the send to set
     */
    void setSendBuffers(ByteBuffer[] send) {
        this.sendBuffers = send;
    }


    /**
     * @return the clientAddress
     */
    public SocketAddress getClientAddress() {
        return clientAddress;
    }
    
    public void setExpectedRecordSize(int expectedRecordSize) {
        this.expectedRecordSize = expectedRecordSize;
        bytesSent = 0;
    }
    
    public void recordBytesSent(long bytesSent) {
        this.bytesSent += bytesSent;
        if (this.bytesSent > expectedRecordSize) {
            String errorMessage = "Too many bytes written (expected: "
                  + expectedRecordSize
                  + ", actual: "
                  + this.bytesSent
                  + ") in connection to "
                  + clientAddress;
            Logging.logMessage(Logging.LEVEL_ERROR, this, errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    public void checkEnoughBytesSent() {
        if (bytesSent != expectedRecordSize) {
            String errorMessage = "Incorrect record length sent (expected: "
                  + expectedRecordSize
                  + ", actual: "
                  + bytesSent
                  + ") in connection to "
                  + clientAddress;
            Logging.logMessage(Logging.LEVEL_ERROR, this, errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }
}
