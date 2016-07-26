/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;

/**
 *
 * @author bjko
 */
public class UDPMessage implements RPCServerConnectionInterface {
    private final ReusableBuffer buffer;
    private final InetSocketAddress address;
    final RPCUDPSocketServer server;

    public UDPMessage(ReusableBuffer buffer, InetSocketAddress address, RPCUDPSocketServer server) {
        this.buffer = buffer;
        this.address = address;
        this.server = server;
    }

    @Override
    public RPCServerInterface getServer() {
        return server;
    }

    /**
     * @return the buffer
     */
    public ReusableBuffer getBuffer() {
        return buffer;
    }

    /**
     * @return the address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public SocketAddress getSender() {
        return address;
    }
    
    @Override
    public ChannelIO getChannel() {
        return null;
    }

}
