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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.channels.ChannelIO;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCRecordFragmentHeader;

/**
 *
 * @author bjko
 */
public class ServerConnection {

    public static final int RETRY_RESET_IN_MS = 500;

    /** max wait is one hour
     */
    public static final int MAX_RETRY_WAIT = 1000*60*60;

    private ChannelIO  channel;

    private final Map<Integer,ONCRPCRequest>  requests;

    private final Queue<ONCRPCRequest>        sendQueue;

    private long lastUsed;

    private long nextReconnectTime;

    private int  numConnectAttempts;

    private final ByteBuffer     requestFragHdr;

    private final ByteBuffer     responseFragHdr;

    private List<ReusableBuffer> responseFragments;

    private boolean              lastResponseFragReceived;

    private ONCRPCRequest   sendRequest;

    private final InetSocketAddress    endpoint;

    volatile long bytesRX, bytesTX;
    

    public ServerConnection(InetSocketAddress endpoint) {
        requests = new HashMap<Integer, ONCRPCRequest>();
        lastUsed = TimeSync.getLocalSystemTime();
        numConnectAttempts = 0;
        nextReconnectTime = 0;
        sendQueue = new ConcurrentLinkedQueue<ONCRPCRequest>();
        requestFragHdr = ByteBuffer.allocateDirect(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        responseFragHdr = ByteBuffer.allocateDirect(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        clearResponseFragments();
        this.endpoint = endpoint;
        bytesTX = 0;
        bytesRX = 0;
    }

    boolean isConnected() {
        return channel != null;
    }

    void connectFailed() {
        numConnectAttempts++;
        long waitt = Math.round(RETRY_RESET_IN_MS*Math.pow(2,this.numConnectAttempts));
        if (waitt > MAX_RETRY_WAIT)
            waitt = MAX_RETRY_WAIT;
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                "next reconnect possible after %d s, %d", (waitt / 1000), this.numConnectAttempts);
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

    ONCRPCRequest getRequest(int rqTransId) {
        return requests.remove(rqTransId);
    }

    void addRequest(int rqTransId, ONCRPCRequest rq) {
        requests.put(rqTransId,rq);
    }

    void removeRequest(int rqTransId) {
        requests.remove(rqTransId);
    }

    Map<Integer,ONCRPCRequest> getRequests() {
        return this.requests;
    }

    Queue<ONCRPCRequest> getSendQueue() {
        return sendQueue;
    }

    
    /**
     * @return the requestFragHdr
     */
    ByteBuffer getRequestFragHdr() {
        return requestFragHdr;
    }

    /**
     * @return the responseFragHdr
     */
    ByteBuffer getResponseFragHdr() {
        return responseFragHdr;
    }


    /**
     * @return the sendRequest
     */
    ONCRPCRequest getSendRequest() {
        return sendRequest;
    }

    /**
     * @param sendRequest the sendRequest to set
     */
    void setSendRequest(ONCRPCRequest sendRequest) {
        this.sendRequest = sendRequest;
    }

    /**
     * @return the responseFragments
     */
    List<ReusableBuffer> getResponseFragments() {
        return responseFragments;
    }

    void clearResponseFragments() {
        responseFragments = new ArrayList<ReusableBuffer>(RPCNIOSocketServer.MAX_FRAGMENTS);
    }

    ReusableBuffer getCurrentResponseFragment() {
        return responseFragments.get(responseFragments.size()-1);
    }

    void addResponseFragment(ReusableBuffer buf) {
        responseFragments.add(buf);
    }

    /**
     * @return the lastResponseFragReceived
     */
    boolean isLastResponseFragReceived() {
        return lastResponseFragReceived;
    }

    /**
     * @param lastResponseFragReceived the lastResponseFragReceived to set
     */
    void setLastResponseFragReceived(boolean lastResponseFragReceived) {
        this.lastResponseFragReceived = lastResponseFragReceived;
    }

    /**
     * @return the endpoint
     */
    public InetSocketAddress getEndpoint() {
        return endpoint;
    }

}
