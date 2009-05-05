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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.channels.ChannelIO;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;

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
    

    public ServerConnection(InetSocketAddress endpoint) {
        requests = new HashMap();
        lastUsed = TimeSync.getLocalSystemTime();
        numConnectAttempts = 0;
        nextReconnectTime = 0;
        sendQueue = new ConcurrentLinkedQueue();
        requestFragHdr = ByteBuffer.allocateDirect(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        responseFragHdr = ByteBuffer.allocateDirect(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        clearResponseFragments();
        this.endpoint = endpoint;
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
        responseFragments = new ArrayList(RPCNIOSocketServer.MAX_FRAGMENTS);
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
