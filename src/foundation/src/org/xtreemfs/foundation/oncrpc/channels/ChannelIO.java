/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.foundation.oncrpc.channels;

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
