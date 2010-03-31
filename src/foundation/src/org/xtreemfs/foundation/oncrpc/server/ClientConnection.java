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

package org.xtreemfs.foundation.oncrpc.server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.oncrpc.channels.ChannelIO;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;

/**
 *
 * @author bjko
 */
public class ClientConnection {

    private final AtomicInteger openRequests;

    private Queue<ONCRPCRecord>   pendingResponses;

    private final ChannelIO     channel;

    private final ByteBuffer    receiveFragHdr;

    private final ByteBuffer    sendFragHdr;

    private ONCRPCRecord          receive;

    private ONCRPCRecord          send;

    private volatile boolean    connectionClosed;

    private boolean               sendingFragmentHeader;

    private SocketAddress         clientAddress;

    public ClientConnection(ChannelIO channel) {
        this.channel = channel;
        this.openRequests = new AtomicInteger(0);
        this.pendingResponses = new ConcurrentLinkedQueue<ONCRPCRecord>();
        this.connectionClosed = false;
        this.receiveFragHdr = ByteBuffer.allocate(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        this.sendFragHdr = ByteBuffer.allocate(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        this.sendFragHdr.position(sendFragHdr.capacity());
        this.sendingFragmentHeader = true;
        try {
            this.clientAddress = channel.socket().getRemoteSocketAddress();
        } catch (Exception ex) {
        }
    }

    public void freeBuffers() {
        if (getReceive() != null)
            getReceive().freeBuffers();
        if (getSend() != null)
            getSend().freeBuffers();
        for (ONCRPCRecord r : pendingResponses) {
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
    public Queue<ONCRPCRecord> getPendingResponses() {
        return pendingResponses;
    }

    public void addPendingResponse(ONCRPCRecord rq) {
        this.pendingResponses.add(rq);
    }

    /**
     * @return the fragmentHeader
     */
    ByteBuffer getReceiveFragHdr() {
        return receiveFragHdr;
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
    ONCRPCRecord getReceive() {
        return receive;
    }

    /**
     * @param receive the receive to set
     */
    void setReceive(ONCRPCRecord receive) {
        this.receive = receive;
    }

    /**
     * @return the send
     */
    ONCRPCRecord getSend() {
        return send;
    }

    /**
     * @param send the send to set
     */
    void setSend(ONCRPCRecord send) {
        this.send = send;
    }

    /**
     * @return the sendingFragmentHeader
     */
    boolean isSendingFragmentHeader() {
        return sendingFragmentHeader;
    }

    /**
     * @param sendingFragmentHeader the sendingFragmentHeader to set
     */
    void setSendingFragmentHeader(boolean sendingFragmentHeader) {
        this.sendingFragmentHeader = sendingFragmentHeader;
    }

    /**
     * @return the clientAddress
     */
    public SocketAddress getClientAddress() {
        return clientAddress;
    }
    

}
