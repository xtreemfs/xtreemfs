/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.foundation.pinky.channels;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;

/**
 * A abstraction of the SocketChannel
 *
 * @author clorenz
 */
public class ChannelIO {

    protected final SocketChannel channel;

    protected Certificate[] certs;
    
    protected Object        attachment;

    public ChannelIO(SocketChannel channel) {
        super();
        this.channel = channel;
        this.certs = null;
        attachment = null;
    }

    public SelectableChannel configureBlocking(boolean block)
                    throws IOException {
            return channel.configureBlocking(block);
    }

    public boolean connect(SocketAddress remote) throws IOException {
            return this.channel.connect(remote);
    }

    public void close() throws IOException {
            channel.socket().close();
            channel.close();
    }

    public boolean isBlocking() {
            return channel.isBlocking();
    }

    public boolean isOpen() {
            return channel.isOpen();
    }

    public SelectionKey keyFor(Selector sel) {
            return channel.keyFor(sel);
    }

    public int read(ByteBuffer dst) throws IOException {
            return channel.read(dst);
    }

    public SelectionKey register(Selector sel, int ops, Object att)
                    throws ClosedChannelException {
            return channel.register(sel, ops, att);
    }

    public Socket socket() {
            return channel.socket();
    }

    public String toString() {
            return channel.toString();
    }

    public int validOps() {
            return channel.validOps();
    }

    public int write(ByteBuffer src) throws IOException {
            return channel.write(src);
    }

    public long write(ByteBuffer[] src) throws IOException {
            return channel.write(src);
    }

    public boolean finishConnect() throws IOException {
            return this.channel.finishConnect();
    }

    public boolean isConnectionPending() {
            return this.channel.isConnectionPending();
    }

    /**
     * does the handshake if needed
     * @param key
     * @return true, if handshake is completed
     * @throws IOException
     */
    public boolean doHandshake(SelectionKey key) throws IOException {
            return true;
    }

    /**
     * prepares the channel for closing
     * this can take more than 1 call
     * @param key
     * @return true, if channel is ready for closing
     * @throws IOException
     */
    public boolean shutdown(SelectionKey key) throws IOException {
        return true;
    }

    /**
     * is channel in closing-procedure?
     * @return
     */
    public boolean isShutdownInProgress() {
        return false;
    }

    /**
     * is there remaining data in channel-buffers, which must be flushed?
     * @return
     */
    public boolean isFlushed() {
        return true;
    }

    public Certificate[] getCerts() {
        return certs;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }
}
