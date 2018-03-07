/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.flease.comm.tcp;

import java.net.InetSocketAddress;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class NIOConnection {

    private TCPConnection connection;

    private Object        context;

    public NIOConnection(TCPConnection connection) {
        this.connection = connection;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public Object getContext() {
        return context;
    }

    /**
     * READ MUST ONLY BE USED FROM A NIOServer callback!
     * @param buffer
     */
    public void read(ReusableBuffer buffer) {
        connection.setReceiveBuffer(buffer);
    }

    /**
     * Write is thread safe
     * @param buffer
     */
    public void write(ReusableBuffer buffer, Object context) {
        connection.getServer().write(connection, buffer, context);
        //FIXME:wakeup selector, change interest set
    }

    public void close() {
        connection.getServer().closeConnection(connection);
    }

    public InetSocketAddress getEndpoint() {
        return connection.getEndpoint();
    }

    public String toString() {
        return "TCP connection (from "+connection.getChannel().socket().getRemoteSocketAddress()+" to local server @ "+connection.getServer().getPort()+")";
    }

}
