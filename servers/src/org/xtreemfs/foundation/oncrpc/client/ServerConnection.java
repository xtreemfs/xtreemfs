/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

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

    private final Map<Integer,RPCRequest>  requests;

    private final Queue<RPCRequest>        sendQueue;

    private long lastUsed;

    private long nextReconnectTime;

    private int  numConnectAttempts;

    private ReusableBuffer requestHeaders;
    private ReusableBuffer requestPayload;

    private ReusableBuffer responseHeaders;
    private ReusableBuffer responsePayload;

    private ONCRPCResponseHeader rpcResponseHeader;
    

    public ServerConnection() {
        requests = new HashMap();
        lastUsed = TimeSync.getLocalSystemTime();
        numConnectAttempts = 0;
        nextReconnectTime = 0;
        sendQueue = new ConcurrentLinkedQueue();
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
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"next reconnect possible after "+(waitt/1000)+" s, "+this.numConnectAttempts);
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

    void setRpcResponseHeader(ONCRPCResponseHeader hdr) {
        rpcResponseHeader = hdr;
    }

    ONCRPCResponseHeader getRpcResponseHeader() {
        return rpcResponseHeader;
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

    RPCRequest getRequest(int rqTransId) {
        return requests.get(rqTransId);
    }

    void addRequest(int rqTransId, RPCRequest rq) {
        requests.put(rqTransId,rq);
    }

    void removeRequest(int rqTransId) {
        requests.remove(rqTransId);
    }

    Map<Integer,RPCRequest> getRequests() {
        return this.requests;
    }

    Queue<RPCRequest> getSendQueue() {
        return sendQueue;
    }

    /**
     * @return the requestHeaders
     */
    ReusableBuffer getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * @param requestHeaders the requestHeaders to set
     */
    void setRequestHeaders(ReusableBuffer requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /**
     * @return the requestPayload
     */
    ReusableBuffer getRequestPayload() {
        return requestPayload;
    }

    /**
     * @param requestPayload the requestPayload to set
     */
    void setRequestPayload(ReusableBuffer requestPayload) {
        this.requestPayload = requestPayload;
    }

    /**
     * @return the responseHeaders
     */
    ReusableBuffer getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * @param responseHeaders the responseHeaders to set
     */
    void setResponseHeaders(ReusableBuffer responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * @return the responsePayload
     */
    ReusableBuffer getResponsePayload() {
        return responsePayload;
    }

    /**
     * @param responsePayload the responsePayload to set
     */
    void setResponsePayload(ReusableBuffer responsePayload) {
        this.responsePayload = responsePayload;
    }

}
