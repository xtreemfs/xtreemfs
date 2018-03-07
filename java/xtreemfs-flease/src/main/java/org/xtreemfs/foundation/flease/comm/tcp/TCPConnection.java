/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease.comm.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class TCPConnection {

    private SocketChannel channel;

    private Queue<SendRequest> sendQueue;

    private ReusableBuffer        receiveBuffer;

    private final NIOConnection         nioCon;

    private final TCPCommunicator       myServer;

    private final AtomicBoolean         closed;

    private final InetSocketAddress endpoint;

    public TCPConnection(SocketChannel channel, TCPCommunicator myServer, InetSocketAddress endpoint) {
        this.channel = channel;
        sendQueue = new ConcurrentLinkedQueue<SendRequest>();
        nioCon = new NIOConnection(this);
        this.myServer = myServer;
        closed = new AtomicBoolean(false);
        this.endpoint = endpoint;
    }


    public TCPCommunicator getServer() {
        return myServer;
    }

    public NIOConnection getNIOConnection() {
        return nioCon;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public InetSocketAddress getEndpoint() {
        return this.endpoint;
    }

    public SendRequest getSendBuffer() {
        return sendQueue.peek();
    }

    public void nextSendBuffer() {
        sendQueue.poll();
    }

    public void addToSendQueue(SendRequest buffer) {
        sendQueue.add(buffer);
    }
    
    public boolean sendQueueIsEmpty() {
        return sendQueue.isEmpty();
    }

    public ReusableBuffer getReceiveBuffer() {
        return receiveBuffer;
    }

    public void setReceiveBuffer(ReusableBuffer buffer) {
        receiveBuffer = buffer;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void setClosed() {
        closed.set(true);
    }

    public void close(NIOServer implementation, IOException error) {
        try {
            BufferPool.free(receiveBuffer);
            channel.close();
        } catch (IOException ex) {
            //ignore
        } finally {
            for (SendRequest rq : sendQueue) {
                BufferPool.free(rq.data);
            }
            if (implementation != null) {
                for (SendRequest rq : sendQueue) {
                    if (rq.getContext() != null)
                        implementation.onWriteFailed(error, rq.getContext());
                }
            }
        }
    }

    public static class SendRequest {
        private final ReusableBuffer data;
        private final Object context;

        public SendRequest(ReusableBuffer data, Object context) {
            this.data = data;
            this.context = context;
        }

        /**
         * @return the data
         */
        public ReusableBuffer getData() {
            return data;
        }

        /**
         * @return the context
         */
        public Object getContext() {
            return context;
        }
    }

}
