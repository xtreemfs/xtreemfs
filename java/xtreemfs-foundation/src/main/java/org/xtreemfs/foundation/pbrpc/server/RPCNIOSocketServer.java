/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;
import org.xtreemfs.foundation.pbrpc.channels.SSLChannelIO;
import org.xtreemfs.foundation.pbrpc.channels.SSLHandshakeOnlyChannelIO;
import org.xtreemfs.foundation.util.OutputUtils;

/**
 * @author bjko
 */
public class RPCNIOSocketServer extends LifeCycleThread implements RPCServerInterface {

    /**
     * Maximum fragment size to accept. If the size is larger, the connection is
     * closed.
     */
    public static final int MAX_FRAGMENT_SIZE = 1024 * 1024 * 32;

    /**
     * the server socket
     */
    private final ServerSocketChannel socket;

    /**
     * Selector for server socket
     */
    private final Selector selector;

    /**
     * If set to true the main loop will exit upon next invocation
     */
    private volatile boolean quit;

    /**
     * The receiver that gets all incoming requests.
     */
    private volatile RPCServerRequestListener receiver;

    /**
     * sslOptions if SSL is enabled, null otherwise
     */
    private final SSLOptions sslOptions;

    /**
     * Connection count
     */
    private final AtomicInteger numConnections;

    /**
     * Number of requests received but not answered
     */
    private long pendingRequests;

    /**
     * Port on which the server listens for incoming connections.
     */
    private final int bindPort;

    private final List<RPCNIOSocketServerConnection> connections;

    /**
     * maximum number of pending client requests to allow
     */
    private final int maxClientQLength;

    /**
     * if the Q was full we need at least CLIENT_Q_THR spaces before we start
     * reading from the client again. This is to prevent it from oscillating
     */
    private final int clientQThreshold;

    public static final int DEFAULT_MAX_CLIENT_Q_LENGTH = 100;

    public RPCNIOSocketServer(int bindPort, InetAddress bindAddr, RPCServerRequestListener rl,
                              SSLOptions sslOptions) throws IOException {
        this(bindPort, bindAddr, rl, sslOptions, 0, -1);
    }

    public RPCNIOSocketServer(int bindPort, InetAddress bindAddr, RPCServerRequestListener rl,
                              SSLOptions sslOptions, int bindRetries) throws IOException {
        this(bindPort, bindAddr, rl, sslOptions, bindRetries, -1);
    }

    public RPCNIOSocketServer(int bindPort, InetAddress bindAddr, RPCServerRequestListener rl,
                              SSLOptions sslOptions, int bindRetries, int receiveBufferSize) throws IOException {
        this(bindPort, bindAddr, rl, sslOptions, bindRetries, receiveBufferSize, DEFAULT_MAX_CLIENT_Q_LENGTH);
    }

    public RPCNIOSocketServer(int bindPort, InetAddress bindAddr, RPCServerRequestListener rl,
                              SSLOptions sslOptions, int bindRetries, int receiveBufferSize,
                              int maxClientQLength) throws IOException {
        super("PBRPCSrv@" + bindPort);

        // open server socket
        socket = ServerSocketChannel.open();
        socket.configureBlocking(false);

        if (receiveBufferSize != -1) {
            socket.socket().setReceiveBufferSize(receiveBufferSize);
            try {
                if (socket.socket().getReceiveBufferSize() != receiveBufferSize) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "could not set socket receive buffer size to " + receiveBufferSize
                                    + ", using default size of " + socket.socket().getReceiveBufferSize());
                }
            } catch (SocketException exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                        "could not check whether receive buffer size was successfully set to %d bytes", receiveBufferSize);
            }
        } else {
            socket.socket().setReceiveBufferSize(256 * 1024);
        }

        socket.socket().setReuseAddress(true);

        int bindTry = 0;
        long waitTime = 1000;
        do {
            try {
                ++bindTry;
                socket.socket().bind(
                        bindAddr == null ? new InetSocketAddress(bindPort) : new InetSocketAddress(bindAddr, bindPort));
            } catch (BindException e) {
                if (bindTry > bindRetries) {
                    // Rethrow exception with the failed port number.
                    throw new BindException("Failed to bind to port " + bindPort + " after " + bindTry + " attempt(s)"
                            + " (" + e.getMessage() + ").");
                } else {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "Failed to bind to port " + bindPort + ", waiting " + waitTime + "ms for it to become free ("
                            + (bindRetries - bindTry) + " attempt(s) left).");
                    if (bindTry == 1) {
                        Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                "You can configure the number of attempts using the 'listen.port.bind_retries' parameter."
                                + " Current value: " + bindRetries + ".");
                    }
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e1) {
                        throw new RuntimeException("Interrupted while waiting for port " + bindPort + " to become free", e1);
                    }
                    waitTime *= 2;
                }
            }
        } while (!socket.socket().isBound());

        this.bindPort = bindPort;
        if (bindTry > 1) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "Successfully bound to port " + bindPort
                    + " after " + bindTry + " attempts");
        }

        // create a selector and register socket
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_ACCEPT);

        // server is ready to accept connections now

        this.receiver = rl;

        this.sslOptions = sslOptions;

        this.numConnections = new AtomicInteger(0);

        this.connections = new LinkedList<RPCNIOSocketServerConnection>();

        this.maxClientQLength = maxClientQLength;
        this.clientQThreshold = (maxClientQLength / 2 >= 0) ? maxClientQLength / 2 : 0;
        if (maxClientQLength <= 1) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "max client queue length is 1, pipe lining is disabled.");
        }
    }

    /**
     * Stop the server and close all connections.
     */
    @Override
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

    /**
     * sends a response.
     */
    @Override
    public void sendResponse(RPCServerRequest request, RPCServerResponse response) {
        assert (response != null);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "response sent");
        final RPCNIOSocketServerConnection connection = (RPCNIOSocketServerConnection) request.getConnection();
        try {
            request.freeBuffers();
        } catch (AssertionError ex) {
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "Caught an AssertionError while trying to free buffers:");
                Logging.logError(Logging.LEVEL_INFO, this, ex);
            }
        }
        assert (connection.getServer() == this);

        if (!connection.isConnectionClosed()) {
            synchronized (connection) {
                boolean isEmpty = connection.getPendingResponses().isEmpty();
                connection.addPendingResponse(response);
                if (isEmpty) {
                    final SelectionKey key = connection.getChannel().keyFor(selector);
                    if (key != null) {
                        try {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        } catch (CancelledKeyException e) {
                            // Ignore it since the timeout mechanism will deal with it.
                        }
                    }
                    selector.wakeup();
                }
            }
        } else {
            // ignore and free buffers
            response.freeBuffers();
        }
    }

    @Override
    public void run() {

        notifyStarted();

        if (Logging.isInfo()) {
            String sslMode = "";
            if (sslOptions != null) {
                if (sslOptions.isFakeSSLMode()) {
                    sslMode = "GRID SSL mode enabled (SSL handshake only)";
                } else {
                    sslMode = "SSL enabled (" + sslOptions.getSSLProtocol() + ")";
                }
            }
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "PBRPC Srv %d ready %s", bindPort, sslMode);
        }

        try {
            while (!quit) {
                // try to select events...
                int numKeys = 0;
                try {
                    numKeys = selector.select();
                } catch (CancelledKeyException ex) {
                    // who cares
                } catch (IOException ex) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "Exception while selecting: %s", ex.toString());
                    continue;
                }

                if (numKeys > 0) {
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
                                acceptConnection();
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
            }

            for (RPCNIOSocketServerConnection con : connections) {
                try {
                    con.getChannel().close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // close socket
            selector.close();
            socket.close();

            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "PBRPC Server %d shutdown complete", bindPort);

            notifyStopped();
        } catch (Throwable thr) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "PBRPC Server %d CRASHED!", bindPort);
            notifyCrashed(thr);
        }

    }

    /**
     * read data from a readable connection
     *
     * @param key a readable key
     */
    private void readConnection(SelectionKey key) {

        final RPCNIOSocketServerConnection con = (RPCNIOSocketServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {

            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {
                    while (true) {
                        if (con.getOpenRequests().get() > maxClientQLength) {
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                            Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                    "client sent too many requests... not accepting new requests from %s, q=%d", con
                                            .getChannel().socket().getRemoteSocketAddress().toString(), con.getOpenRequests().get());
                            return;
                        }

                        ByteBuffer buf = null;
                        switch (con.getReceiveState()) {
                            case RECORD_MARKER: {
                                buf = con.getReceiveRecordMarker();
                                break;
                            }
                            case RPC_HEADER: {
                                buf = con.getReceiveBuffers()[0].getBuffer();
                                break;
                            }
                            case RPC_MESSAGE: {
                                buf = con.getReceiveBuffers()[1].getBuffer();
                                break;
                            }
                            case DATA: {
                                buf = con.getReceiveBuffers()[2].getBuffer();
                                break;
                            }
                        }

                        // read fragment header
                        final int numBytesRead = readData(key, channel, buf);
                        if (numBytesRead == -1) {
                            // connection closed
                            if (Logging.isInfo()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                        "client closed connection (EOF): %s", channel.socket()
                                                .getRemoteSocketAddress().toString());
                            }
                            closeConnection(key);
                            return;
                        }
                        if (buf.hasRemaining()) {
                            // not enough data...
                            break;
                        }

                        switch (con.getReceiveState()) {
                            case RECORD_MARKER: {
                                buf.position(0);
                                final int hdrLen = buf.getInt();
                                final int msgLen = buf.getInt();
                                final int dataLen = buf.getInt();

                                if ((hdrLen <= 0) || (hdrLen >= MAX_FRAGMENT_SIZE)
                                        || (msgLen < 0) || (msgLen >= MAX_FRAGMENT_SIZE)
                                        || (dataLen < 0) || (dataLen >= MAX_FRAGMENT_SIZE)) {
                                    Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this,
                                            "invalid record marker size (%d/%d/%d) received, closing connection to client %s",
                                            hdrLen, msgLen, dataLen, channel.socket()
                                                    .getRemoteSocketAddress().toString());
                                    closeConnection(key);
                                    return;
                                }
                                final ReusableBuffer[] buffers = new ReusableBuffer[]{BufferPool.allocate(hdrLen),
                                        ((msgLen > 0) ? BufferPool.allocate(msgLen) : null),
                                        ((dataLen > 0) ? BufferPool.allocate(dataLen) : null)};
                                con.setReceiveBuffers(buffers);
                                con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.RPC_HEADER);
                                continue;
                            }

                            case RPC_HEADER: {
                                if (con.getReceiveBuffers()[1] != null) {
                                    con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.RPC_MESSAGE);
                                    continue;
                                } else {
                                    if (con.getReceiveBuffers()[2] != null) {
                                        con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.DATA);
                                        continue;
                                    } else {
                                        break;
                                    }
                                }
                            }
                            case RPC_MESSAGE: {
                                if (con.getReceiveBuffers()[2] != null) {
                                    con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.DATA);
                                    continue;
                                } else {
                                    break;
                                }

                            }
                        }

                        //assemble ServerRequest
                        con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.RECORD_MARKER);
                        con.getReceiveRecordMarker().clear();

                        ReusableBuffer[] receiveBuffers = con.getReceiveBuffers();
                        receiveBuffers[0].flip();
                        if (receiveBuffers[1] != null)
                            receiveBuffers[1].flip();
                        if (receiveBuffers[2] != null)
                            receiveBuffers[2].flip();
                        con.setReceiveBuffers(null);

                        RPCServerRequest rq = null;
                        try {
                            rq = new RPCServerRequest(con, receiveBuffers[0], receiveBuffers[1], receiveBuffers[2]);
                        } catch (IOException ex) {
                            // close connection if the header cannot be parsed
                            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "invalid PBRPC header received: " + ex);
                            if (Logging.isDebug()) {
                                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                            }
                            closeConnection(key);
                            BufferPool.free(receiveBuffers[1]);
                            BufferPool.free(receiveBuffers[2]);
                            return;
                        }
                        // request is 
                        // complete... send to receiver
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, rq
                                    .toString());
                        }
                        con.getOpenRequests().incrementAndGet();
                        if (Logging.isDebug())
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "request received");
                        pendingRequests++;
                        if (!receiveRequest(rq, con)) {
                            closeConnection(key);
                            return;
                        }
                    }
                }
            }
        } catch (CancelledKeyException ex) {
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "client closed connection (CancelledKeyException): %s", channel.socket().getRemoteSocketAddress()
                                .toString());
            }
            closeConnection(key);
        } catch (ClosedByInterruptException ex) {
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "client closed connection (EOF): %s", channel.socket().getRemoteSocketAddress()
                                .toString());
            }
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "connection to %s closed by remote peer", con.getChannel().socket()
                                .getRemoteSocketAddress().toString());
            }
            closeConnection(key);
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key);
        }
    }

    /**
     * write data to a writeable connection
     *
     * @param key the writable key
     */
    private void writeConnection(SelectionKey key) {

        final RPCNIOSocketServerConnection con = (RPCNIOSocketServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {

            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {

                    while (true) {

                        // final ByteBuffer fragmentHeader =
                        // con.getSendFragHdr();

                        ByteBuffer[] response = con.getSendBuffers();
                        if (response == null) {
                            synchronized (con) {
                                RPCServerResponse rq = con.getPendingResponses().peek();
                                if (rq == null) {
                                    // no more responses, stop writing...
                                    con.setSendBuffers(null);
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    break;
                                }
                                response = rq.packBuffers(con.getSendFragHdr());
                                con.setSendBuffers(response);
                                con.setExpectedRecordSize(rq.getRpcMessageSize());
                            }
                        }

                        /*
                         * if (fragmentHeader.hasRemaining()) { final int
                         * numBytesWritten = writeData(key, channel,
                         * fragmentHeader); if (numBytesWritten == -1) {
                         * //connection closed closeConnection(key); return; }
                         * if (fragmentHeader.hasRemaining()) { //not enough
                         * data... break; } //finished sending... send fragment
                         * data now... } else {
                         */
                        // send fragment data
                        assert (response != null);
                        final long numBytesWritten = channel.write(response);
                        if (numBytesWritten == -1) {
                            if (Logging.isInfo()) {
                                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                                        "client closed connection (EOF): %s", channel.socket()
                                                .getRemoteSocketAddress().toString());
                            }
                            // connection closed
                            closeConnection(key);
                            return;
                        }
                        con.recordBytesSent(numBytesWritten);

                        if (response[response.length - 1].hasRemaining()) {
                            // not enough data...
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            break;
                        }
                        con.checkEnoughBytesSent();
                        // finished sending fragment
                        // clean up :-) request finished
                        pendingRequests--;
                        RPCServerResponse rq = con.getPendingResponses().poll();
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "sent response for %s", rq.toString());
                        }
                        rq.freeBuffers();
                        con.setSendBuffers(null);
                        con.getSendFragHdr().clear();
                        int numRq = con.getOpenRequests().decrementAndGet();

                        if ((key.interestOps() & SelectionKey.OP_READ) == 0) {
                            if (numRq < clientQThreshold) {
                                // read from client again
                                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                        "client allowed to send data again: %s, q=%d", con.getChannel().socket()
                                                .getRemoteSocketAddress().toString(), numRq);
                            }
                        }
                    }
                }
            }
        } catch (CancelledKeyException ex) {
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "client closed connection (CancelledKeyException): %s", channel.socket().getRemoteSocketAddress()
                                .toString());
            }
            closeConnection(key);
        } catch (ClosedByInterruptException ex) {
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "client closed connection (EOF): %s", channel.socket().getRemoteSocketAddress()
                                .toString());
            }
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "connection to %s closed by remote peer", con.getChannel().socket()
                                .getRemoteSocketAddress().toString());
            }
            closeConnection(key);
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key);
        }
    }

    /**
     * Reads data from the socket, ensures that SSL connection is ready
     *
     * @param key     the SelectionKey
     * @param channel the channel to read from
     * @param buf     the buffer to read to
     * @return number of bytes read, -1 on EOF
     * @throws java.io.IOException
     */
    public static int readData(SelectionKey key, ChannelIO channel, ByteBuffer buf) throws IOException {
        return channel.read(buf);
        /*
         * if (!channel.isShutdownInProgress()) { if (channel.doHandshake(key))
         * { return channel.read(buf); } else { return 0; } } else { return 0; }
         */
    }

    public static int writeData(SelectionKey key, ChannelIO channel, ByteBuffer buf) throws IOException {
        return channel.write(buf);
        /*
         * if (!channel.isShutdownInProgress()) { if (channel.doHandshake(key))
         * { return channel.write(buf); } else { return 0; } } else { return 0;
         * }
         */
    }

    /**
     * close a connection
     *
     * @param key matching key
     */
    private void closeConnection(SelectionKey key) {
        final RPCNIOSocketServerConnection con = (RPCNIOSocketServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        // remove the connection from the selector and close socket
        try {
            connections.remove(con);
            con.setConnectionClosed(true);
            key.cancel();
            channel.close();
        } catch (Exception ex) {
        } finally {
            // adjust connection count and make sure buffers are freed
            numConnections.decrementAndGet();
            con.freeBuffers();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "closing connection to %s", channel
                    .socket().getRemoteSocketAddress().toString());
        }
    }

    /**
     * accept a new incoming connection
     */
    private void acceptConnection() throws IOException {
        SocketChannel client = null;
        RPCNIOSocketServerConnection con = null;
        ChannelIO channelIO = null;
        // FIXME: Better exception handling!

        try {

            // accept that connection
            client = socket.accept();

            if (sslOptions == null) {
                channelIO = new ChannelIO(client);
            } else {
                if (sslOptions.isFakeSSLMode()) {
                    channelIO = new SSLHandshakeOnlyChannelIO(client, sslOptions, false);
                } else {
                    channelIO = new SSLChannelIO(client, sslOptions, false);
                }
            }
            con = new RPCNIOSocketServerConnection(this, channelIO);

            // and configure it to be non blocking
            // IMPORTANT!
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, con);
            client.socket().setTcpNoDelay(true);

            numConnections.incrementAndGet();

            this.connections.add(con);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connect from client at %s",
                        client.socket().getRemoteSocketAddress().toString());
            }

        } catch (ClosedChannelException ex) {
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                        "client closed connection during accept");
            }
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "cannot establish connection: %s", ex.toString());
            if (channelIO != null) {
                channelIO.close();
            }
        } catch (IOException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "cannot establish connection: %s", ex.toString());
            if (channelIO != null) {
                channelIO.close();
            }
        }
    }

    /**
     * @param request
     * @param con
     * @return true on success, false on error
     */
    private boolean receiveRequest(RPCServerRequest request, RPCNIOSocketServerConnection con) {
        try {
            request.getHeader();

            receiver.receiveRecord(request);
            return true;
        } catch (IllegalArgumentException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "invalid PBRPC header received: " + ex);
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            return false;
        }
    }

    public int getNumConnections() {
        return this.numConnections.get();
    }

    public long getPendingRequests() {
        return this.pendingRequests;
    }
}
