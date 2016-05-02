/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease.comm.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.comm.tcp.TCPConnection.SendRequest;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class TCPCommunicator extends LifeCycleThread {

    private final int port;

    /**
     * the server socket
     */
    private final ServerSocketChannel socket;

    /**
     * Selector for server socket
     */
    private final Selector selector;

    private final NIOServer implementation;

    private final List<TCPConnection> connections;

    private final Queue<TCPConnection> pendingCons;

    private final AtomicInteger        sendQueueSize;

    /**
     *
     * @param implementation
     * @param port, 0 to disable server mode
     * @param bindAddr
     * @throws IOException
     */
    public TCPCommunicator(NIOServer implementation, int port, InetAddress bindAddr) throws IOException {
        super("TCPcom@" + port);

        this.port = port;
        this.implementation = implementation;
        this.connections = new LinkedList();
        this.pendingCons = new ConcurrentLinkedQueue();
        sendQueueSize = new AtomicInteger();

        // open server socket
        if (port == 0) {
            socket = null;
        } else {
            socket = ServerSocketChannel.open();
            socket.configureBlocking(false);
            socket.socket().setReceiveBufferSize(256 * 1024);
            socket.socket().setReuseAddress(true);
            socket.socket().bind(
                    bindAddr == null ? new InetSocketAddress(port) : new InetSocketAddress(bindAddr, port));
        }

        // create a selector and register socket
        selector = Selector.open();
        if (socket != null)
            socket.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Stop the server and close all connections.
     */
    public void shutdown() {
        this.interrupt();
    }

    /**
     * sends a response.
     *
     * @param request
     *            the request
     */
    public void write(TCPConnection connection, ReusableBuffer buffer, Object context) {
        assert (buffer != null);
        synchronized (connection) {
            if (connection.getChannel().isConnected()) {
                if (connection.sendQueueIsEmpty()) {
                    try {
                        int bytesWritten = connection.getChannel().write(buffer.getBuffer());
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,
                                    "directly wrote %d bytes to %s", bytesWritten,
                                    connection.getChannel().socket().getRemoteSocketAddress().toString());
                        }
                        if (bytesWritten < 0) {
                            if (context != null)
                                implementation.onWriteFailed(new IOException("remote party closed connection while writing"), context);
                            abortConnection(connection,new IOException("remote party closed connection while writing"));
                            return;
                        }
                        if (!buffer.hasRemaining()) {
                            //we are done
                            BufferPool.free(buffer);
                            return;
                        }
                    } catch (ClosedChannelException ex) {
                        if (context != null)
                            implementation.onWriteFailed(ex, context);
                        abortConnection(connection,ex);
                    } catch (IOException ex) {
                        if (Logging.isDebug())
                            Logging.logError(Logging.LEVEL_DEBUG, this,ex);
                        if (context != null)
                            implementation.onWriteFailed(ex, context);
                        abortConnection(connection,ex);
                    }
                }

                synchronized (connection) {
                    boolean isEmpty = connection.sendQueueIsEmpty();
                    if (isEmpty) {
                        try {
                            SelectionKey key = connection.getChannel().keyFor(selector);
                            if (key != null) {
                                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            }
                        } catch (CancelledKeyException ex) {
                        }
                    }
                }

                sendQueueSize.incrementAndGet();
                connection.addToSendQueue(new TCPConnection.SendRequest(buffer, context));
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this, "enqueued write to %s",
                            connection.getEndpoint());
                }
                selector.wakeup();
            } else {
                // ignore and free bufers
                if (connection.getChannel().isConnectionPending()) {
                    sendQueueSize.incrementAndGet();
                    connection.addToSendQueue(new TCPConnection.SendRequest(buffer, context));
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this, "enqueued write to %s",
                                connection.getEndpoint());
                    }
                } else {
                    BufferPool.free(buffer);
                    if (context != null)
                        implementation.onWriteFailed(new IOException("Connection already closed"), context);
                }
            }
        }
    }

    public void run() {
        notifyStarted();

        if (Logging.isInfo()) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "TCP Server @%d ready", port);
        }

        try {
            while (!isInterrupted()) {
                // try to select events...
                try {
                    final int numKeys = selector.select();
                    if (!pendingCons.isEmpty()) {
                         while (true) {
                            TCPConnection con = pendingCons.poll();
                            if (con == null) {
                                break;
                            }
                            try {
                                assert(con.getChannel() != null);
                                con.getChannel().register(selector,
                                    SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, con);
                            } catch (ClosedChannelException ex) {
                                abortConnection(con,ex);
                            }
                        }
                    }
                    if (numKeys == 0) {
                        continue;
                    }
                } catch (CancelledKeyException ex) {
                    // who cares
                } catch (IOException ex) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "Exception while selecting: %s", ex.toString());
                    continue;
                }

                // fetch events
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                // process all events
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    // remove key from the list
                    iter.remove();
                    try {

                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        }
                        if (key.isConnectable()) {
                            connectConnection(key);
                        }
                        if (key.isReadable()) {
                            readConnection(key);
                        }
                        if (key.isWritable()) {
                            writeConnection(key);
                        }
                    } catch (CancelledKeyException ex) {
                        // nobody cares...
                        continue;
                    }
                }
            }

            for (TCPConnection con : connections) {
                try {
                    con.close(implementation,new IOException("server shutdown"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // close socket
            selector.close();
            if (socket != null)
                socket.close();

            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "TCP Server @%d shutdown complete", port);
            }

            notifyStopped();
        } catch (Throwable thr) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "TPC Server @%d CRASHED!", port);
            notifyCrashed(thr);
        }
    }

    private void connectConnection(SelectionKey key) {
        final TCPConnection con = (TCPConnection) key.attachment();
        final SocketChannel channel = con.getChannel();

        try {
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            synchronized (con) {
                if (con.getSendBuffer() != null) {
                    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                } else {
                    key.interestOps(SelectionKey.OP_READ);
                }
            }
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connected from %s to %s", con
                        .getChannel().socket().getLocalSocketAddress().toString(), channel.socket().getRemoteSocketAddress()
                        .toString());
            }
            implementation.onConnect(con.getNIOConnection());
        } catch (IOException ex) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this,ex);
            }
            implementation.onConnectFailed(con.getEndpoint(), ex, con.getNIOConnection().getContext());
            con.close(implementation,ex);
        }

    }

    public NIOConnection connect(InetSocketAddress server, Object context) throws IOException {
        TCPConnection con = openConnection(server,context);
        return con.getNIOConnection();
    }

    private TCPConnection openConnection(InetSocketAddress server, Object context) throws IOException {

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connect to %s", server
                    .toString());
        }
        SocketChannel channel = null;
        TCPConnection con = null;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            //channel.socket().setTcpNoDelay(true);
            channel.socket().setReceiveBufferSize(256 * 1024);
            channel.connect(server);
            con = new TCPConnection(channel, this, server);
            con.getNIOConnection().setContext(context);
            pendingCons.add(con);
            selector.wakeup();
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connection established");
            return con;
        } catch (IOException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "cannot contact server %s",
                    server);
            }
            if (con != null)
                con.close(implementation, ex);
            throw ex;
        }


    }


    /**
     * accept a new incomming connection
     *
     * @param key
     *            the acceptable key
     */
    private void acceptConnection(SelectionKey key) {
        SocketChannel client = null;
        TCPConnection connection = null;
        // FIXME: Better exception handling!

        try {
            // accept connection
            client = socket.accept();
            connection = new TCPConnection(client,this,(InetSocketAddress)client.socket().getRemoteSocketAddress());

            // and configure it to be non blocking
            // IMPORTANT!
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, connection);
            //client.socket().setTcpNoDelay(true);

            //numConnections.incrementAndGet();

            connections.add(connection);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connect from client at %s",
                        client.socket().getRemoteSocketAddress().toString());
            }
            implementation.onAccept(connection.getNIOConnection());

        } catch (ClosedChannelException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "cannot establish connection: %s", ex.toString());
            }
        } catch (IOException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "cannot establish connection: %s", ex.toString());
            }
        }
    }


    /**
     * read data from a readable connection
     *
     * @param key
     *            a readable key
     */
    private void readConnection(SelectionKey key) {
        final TCPConnection con = (TCPConnection) key.attachment();
        final SocketChannel channel = con.getChannel();

        try {

            while (true) {
                final ReusableBuffer readBuf = con.getReceiveBuffer();
                if (readBuf == null)
                    return;
                final int numBytesRead = channel.read(readBuf.getBuffer());
                if (numBytesRead == -1) {
                    // connection closed
                    if (Logging.isInfo()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                            "client closed connection (EOF): %s", channel.socket()
                                    .getRemoteSocketAddress().toString());
                    }
                    abortConnection(con,new IOException("remote end closed connection while reading data"));
                    return;
                } else if (numBytesRead == 0) {
                    return;
                }
                implementation.onRead(con.getNIOConnection(), readBuf);
            }
        } catch (ClosedChannelException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "connection to %s closed by remote peer", con.getChannel().socket()
                            .getRemoteSocketAddress().toString());
            }
            abortConnection(con,ex);
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            abortConnection(con,ex);
        }
    }

    /**
     * write data to a writeable connection
     *
     * @param key
     *            the writable key
     */
    private void writeConnection(SelectionKey key) {
        final TCPConnection con = (TCPConnection) key.attachment();
        final SocketChannel channel = con.getChannel();

        SendRequest srq = null;
        try {

            while (true) {
                srq = con.getSendBuffer();
                if (srq == null) {
                    synchronized (con) {
                        srq = con.getSendBuffer();
                        if (srq == null) {
                            // no more responses, stop writing...
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                            return;
                        }
                    }
                }
                // send data
                final long numBytesWritten = channel.write(srq.getData().getBuffer());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this, "wrote %d bytes to %s",
                            numBytesWritten, channel.socket().getRemoteSocketAddress().toString());
                }
                if (numBytesWritten == -1) {
                    if (Logging.isInfo()) {
                        Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                                "client closed connection (EOF): %s", channel.socket().getRemoteSocketAddress().toString());
                    }
                    // connection closed
                    
                    abortConnection(con, new IOException("remote end closed connection while writing data"));
                    return;
                }
                if (srq.getData().hasRemaining()) {
                    // not enough data...
                    break;
                }
                // finished sending fragment
                // clean up :-) request finished

                BufferPool.free(srq.getData());
                sendQueueSize.decrementAndGet();
                con.nextSendBuffer();
                
            }
        } catch (ClosedChannelException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "connection to %s closed by remote peer", con.getChannel().socket().getRemoteSocketAddress().toString());
            }
            abortConnection(con,ex);
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            abortConnection(con,ex);
        }
    }

    public int getSendQueueSize() {
        return sendQueueSize.get();
    }

    void closeConnection(TCPConnection con) {
        if (con.isClosed())
            return;
        con.setClosed();
        final SocketChannel channel = con.getChannel();

        // remove the connection from the selector and close socket
        try {
            synchronized (connections) {
                connections.remove(con);
            }
            final SelectionKey key = channel.keyFor(selector);
            if (key != null)
                key.cancel();
            con.close(null,null);
        } catch (Exception ex) {
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "closing connection to %s", channel
                    .socket().getRemoteSocketAddress().toString());
        }
    }

    void abortConnection(TCPConnection con, IOException exception) {
        if (con.isClosed())
            return;
        con.setClosed();
        final SocketChannel channel = con.getChannel();

        // remove the connection from the selector and close socket
        try {
            synchronized (connections) {
                connections.remove(con);
            }
            final SelectionKey key = channel.keyFor(selector);
            if (key != null)
                key.cancel();
            con.close(implementation,exception);
        } catch (Exception ex) {
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "closing connection to %s", channel
                    .socket().getRemoteSocketAddress().toString());
        }
        implementation.onClose(con.getNIOConnection());
    }

    int getPort() {
        return this.port;
    }
}
