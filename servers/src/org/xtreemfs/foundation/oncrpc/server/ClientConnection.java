/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.server;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.interfaces.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.ONCRPCRequestHeader;

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

    public ClientConnection(ChannelIO channel) {
        this.channel = channel;
        this.openRequests = new AtomicInteger(0);
        this.pendingResponses = new ConcurrentLinkedQueue();
        this.connectionClosed = false;
        this.receiveFragHdr = ByteBuffer.allocate(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        this.sendFragHdr = ByteBuffer.allocate(ONCRPCRecordFragmentHeader.getFragmentHeaderSize());
        this.sendFragHdr.position(sendFragHdr.capacity());
        this.sendingFragmentHeader = true;
    }

    public void freeBuffers() {
        if (getReceive() != null)
            getReceive().freeBuffers();
        if (getSend() != null)
            getSend().freeBuffers();
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
    

}
