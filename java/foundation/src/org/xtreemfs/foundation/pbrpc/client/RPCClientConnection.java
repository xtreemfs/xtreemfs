/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServerConnection.ReceiveState;
import org.xtreemfs.foundation.pbrpc.utils.RecordMarker;

/**
 *
 * @author bjko
 */
public class RPCClientConnection {

    public static final int RETRY_RESET_IN_MS = 500;

    /** max wait is one minute */
    public static final int MAX_RETRY_WAIT_MS = 1000*60;

    private ChannelIO  channel;

    private final Map<Integer,RPCClientRequest>  requests;

    private final List<RPCClientRequest>         sendQueue;

    private long lastUsed;

    private long nextReconnectTime;

    private int  numConnectAttempts;

    private final ByteBuffer     requestRecordMarker;

    private final ByteBuffer     responseRecordMarker;

    private ReusableBuffer[]     responseBuffers;

    private ByteBuffer[]         requestBuffers;

    private RPCClientRequest	 pendingRequest;
    
    private ReceiveState         receiveState;

    private final InetSocketAddress    endpoint;

    volatile long bytesRX, bytesTX;
    

    public RPCClientConnection(InetSocketAddress endpoint) {
        requests = new HashMap<Integer, RPCClientRequest>();
        lastUsed = TimeSync.getLocalSystemTime();
        numConnectAttempts = 0;
        nextReconnectTime = 0;
        sendQueue = new LinkedList<RPCClientRequest>();
        requestRecordMarker = ByteBuffer.allocateDirect(RecordMarker.HDR_SIZE);
        responseRecordMarker = ByteBuffer.allocateDirect(RecordMarker.HDR_SIZE);
        this.endpoint = endpoint;
        receiveState = ReceiveState.RECORD_MARKER;
        bytesTX = 0;
        bytesRX = 0;
    }

    public void freeBuffers() {
        if (responseBuffers != null) {
            for (ReusableBuffer buf: responseBuffers)
                BufferPool.free(buf);
        }
        for (RPCClientRequest rq : sendQueue) {
            rq.freeBuffers();
        }
        for (RPCClientRequest rq : requests.values()) {
            rq.freeBuffers();
        }
    }

    boolean isConnected() {
        return channel != null;
    }

    void connectFailed() {
        numConnectAttempts++;
        long waitt = Math.round(RETRY_RESET_IN_MS*Math.pow(2,this.numConnectAttempts));
        if (waitt > MAX_RETRY_WAIT_MS) {
            waitt = MAX_RETRY_WAIT_MS;
        }
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                "next reconnect possible after %d s, attempt = %d", (waitt / 1000), this.numConnectAttempts);
        this.nextReconnectTime = System.currentTimeMillis()+waitt;
    }

    boolean canReconnect() {
        return (this.nextReconnectTime < System.currentTimeMillis());
    }

    void setChannel(ChannelIO channel) {
        this.channel = channel;
    }

    void connected() {
        numConnectAttempts = 0;
        lastUsed = TimeSync.getLocalSystemTime();
    }

    void useConnection() {
        lastUsed = TimeSync.getLocalSystemTime();
    }

    long getLastUsed() {
        return lastUsed;
    }

    ChannelIO getChannel() {
        return channel;
    }

    RPCClientRequest getRequest(int callId) {
        return requests.remove(callId);
    }

    void addRequest(int callId, RPCClientRequest rq) {
        requests.put(callId,rq);
    }

    void removeRequest(int callId) {
        requests.remove(callId);
    }

    Map<Integer,RPCClientRequest> getRequests() {
        return this.requests;
    }

    List<RPCClientRequest> getSendQueue() {
        return sendQueue;
    }

    
    /**
     * @return the requestFragHdr
     */
    ByteBuffer getRequestRecordMarker() {
        return requestRecordMarker;
    }

    /**
     * @return the responseFragHdr
     */
    ByteBuffer getResponseRecordMarker() {
        return responseRecordMarker;
    }

    public String getEndpointString() {
        String endpointString = "unknown";
        try {
            endpointString = endpoint.toString();
        } catch (Exception ex2) {
        }
        
        return endpointString;
    }

    /**
     * @return the requestBuffers
     */
    public ByteBuffer[] getRequestBuffers() {
        return requestBuffers;
    }

    /**
     * @param requestBuffers the requestBuffers to set
     */
    public void setRequestBuffers(ByteBuffer[] requestBuffers) {
        this.requestBuffers = requestBuffers;
    }

    public RPCClientRequest getPendingRequest() {
        return pendingRequest;
    }
    
    public void setPendingRequest(RPCClientRequest pendingRequest) {
    	this.pendingRequest = pendingRequest;
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

    /**
     * @return the responseBuffers
     */
    public ReusableBuffer[] getResponseBuffers() {
        return responseBuffers;
    }

    /**
     * @param responseBuffers the responseBuffers to set
     */
    public void setResponseBuffers(ReusableBuffer[] responseBuffers) {
        this.responseBuffers = responseBuffers;
    }

}
