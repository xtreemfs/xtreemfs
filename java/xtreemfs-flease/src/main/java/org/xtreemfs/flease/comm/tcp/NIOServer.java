/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.flease.comm.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public interface NIOServer {

    /**
     * called upon incoming connections.
     * @param connection
     */
    public void onAccept(NIOConnection connection);

    /**
     * called when a connection was established
     * make sure to issue the first write
     * @param connection
     */
    public void onConnect(NIOConnection connection);

    /**
     * called when new data is available
     * @param connection
     * @param buffer
     */
    public void onRead(NIOConnection connection, ReusableBuffer buffer);

    /**
     * called when a connection is closed
     * @param connection
     */
    public void onClose(NIOConnection connection);


    public void onWriteFailed(IOException exception, Object context);

    public void onConnectFailed(InetSocketAddress endpoint, IOException exception, Object context);

}