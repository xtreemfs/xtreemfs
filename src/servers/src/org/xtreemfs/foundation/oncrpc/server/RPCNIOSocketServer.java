/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */
package org.xtreemfs.foundation.oncrpc.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
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

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.channels.ChannelIO;
import org.xtreemfs.foundation.oncrpc.channels.SSLChannelIO;
import org.xtreemfs.foundation.oncrpc.channels.SSLHandshakeOnlyChannelIO;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

/**
 * 
 * @author bjko
 */
public class RPCNIOSocketServer extends LifeCycleThread {
    
    /**
     * Maximum number of record fragments supported.
     */
    public static final int                MAX_FRAGMENTS     = 1;
    
    /**
     * Maximum fragment size to accept. If the size is larger, the connection is
     * closed.
     */
    public static final int                MAX_FRAGMENT_SIZE = 1024 * 1024 * 32;
    
    /**
     * the server socket
     */
    private final ServerSocketChannel      socket;
    
    /**
     * Selector for server socket
     */
    private final Selector                 selector;
    
    /**
     * If set to true thei main loop will exit upon next invocation
     */
    private volatile boolean               quit;
    
    /**
     * The receiver that gets all incomming requests.
     */
    private final RPCServerRequestListener receiver;
    
    /**
     * sslOptions if SSL is enabled, null otherwise
     */
    private final SSLOptions               sslOptions;
    
    /**
     * Connection count
     */
    private final AtomicInteger            numConnections;
    
    /**
     * Number of requests received but not answered
     */
    private long                           pendingRequests;
    
    /**
     * Port on which the server listens for incomming connections.
     */
    private final int                      bindPort;
    
    private final List<ClientConnection>   connections;
    
    /**
     * maximum number of pending client requests to allow
     */
    public static int                      MAX_CLIENT_QUEUE  = 20000;
    
    /**
     * if the Q was full we need at least CLIENT_Q_THR spaces before we start
     * reading from the client again. This is to prevent it from oscillating
     */
    public static int                      CLIENT_Q_THR      = 5000;

    private final AuthFlavorProvider       authProvider;
    
    public RPCNIOSocketServer(int bindPort, InetAddress bindAddr, RPCServerRequestListener rl,
        SSLOptions sslOptions, AuthFlavorProvider authProvider) throws IOException {
        super("ONCRPCSrv@" + bindPort);
        
        // open server socket
        socket = ServerSocketChannel.open();
        socket.configureBlocking(false);
        socket.socket().setReceiveBufferSize(256 * 1024);
        socket.socket().setReuseAddress(true);
        socket.socket().bind(
            bindAddr == null ? new InetSocketAddress(bindPort) : new InetSocketAddress(bindAddr, bindPort));
        this.bindPort = bindPort;
        
        // create a selector and register socket
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_ACCEPT);
        
        // server is ready to accept connections now
        
        this.receiver = rl;
        
        this.sslOptions = sslOptions;
        
        this.numConnections = new AtomicInteger(0);
        
        this.connections = new LinkedList();

        this.authProvider = authProvider;
    }
    
    /**
     * Stop the server and close all connections.
     */
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }
    
    /**
     * sends a response.
     * 
     * @param request
     *            the request
     */
    public void sendResponse(ONCRPCRecord request) {
        assert (request.getResponseBuffers() != null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "response sent");
        final ClientConnection connection = request.getConnection();
        if (!connection.isConnectionClosed()) {
            synchronized (connection) {
                boolean isEmpty = connection.getPendingResponses().isEmpty();
                connection.addPendingResponse(request);
                if (isEmpty) {
                    SelectionKey key = connection.getChannel().keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
            }
            selector.wakeup();
        } else {
            // ignore and free bufers
            request.freeBuffers();
        }
    }
    
    public void run() {
        
        notifyStarted();
        
        if (Logging.isInfo()) {
            String sslMode = "";
            if (sslOptions != null) {
                if (sslOptions.isFakeSSLMode()) {
                    sslMode = "GRID SSL mode enabled (SSL handshake only)";
                } else {
                    sslMode = "SSL enabled";
                }
            }
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "ONCRPC Srv %d ready %s", bindPort,sslMode);
        }
        
        try {
            while (!quit) {
                // try to select events...
                try {
                    if (selector.select() == 0) {
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
            
            for (ClientConnection con : connections) {
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
                    "ONCRPC Server %d shutdown complete", bindPort);
            
            notifyStopped();
        } catch (Exception thr) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "ONRPC Server %d CRASHED!", bindPort);
            notifyCrashed(thr);
        }
        
    }
    
    /**
     * read data from a readable connection
     * 
     * @param key
     *            a readable key
     */
    private void readConnection(SelectionKey key) {
        final ClientConnection con = (ClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        try {
            
            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {
                    
                    if (con.getOpenRequests().get() > MAX_CLIENT_QUEUE) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                        Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "client sent too many requests... not accepting new requests: %s", con
                                    .getChannel().socket().getRemoteSocketAddress().toString());
                        return;
                    }
                    while (true) {
                        final ByteBuffer fragmentHeader = con.getReceiveFragHdr();
                        if (fragmentHeader.hasRemaining()) {
                            // read fragment header
                            final int numBytesRead = readData(key, channel, fragmentHeader);
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
                            if (fragmentHeader.hasRemaining()) {
                                // not enough data...
                                break;
                            } else {
                                // fragment header is complete...
                                fragmentHeader.position(0);
                                final int fragmentHeaderInt = fragmentHeader.getInt();
                                final int fragmentSize = ONCRPCRecordFragmentHeader
                                        .getFragmentLength(fragmentHeaderInt);
                                final boolean lastFragment = ONCRPCRecordFragmentHeader
                                        .isLastFragment(fragmentHeaderInt);
                                
                                if ((fragmentSize <= 0) || (fragmentSize >= MAX_FRAGMENT_SIZE)) {
                                    Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this,
                                        "invalid fragment size (%d) received, closing connection to client %s",
                                        fragmentSize,channel.socket()
                                            .getRemoteSocketAddress().toString());
                                    closeConnection(key);
                                    return;
                                }
                                final ReusableBuffer fragment = BufferPool.allocate(fragmentSize);
                                
                                ONCRPCRecord rq = con.getReceive();
                                if (rq == null) {
                                    rq = new ONCRPCRecord(this, con);
                                    con.setReceive(rq);
                                }
                                rq.addNewRequestFragment(fragment);
                                rq.setAllFragmentsReceived(lastFragment);
                            }
                        } else {
                            final ONCRPCRecord rq = con.getReceive();
                            final ReusableBuffer fragment = rq.getLastRequestFragment();
                            
                            final int numBytesRead = readData(key, channel, fragment.getBuffer());
                            if (numBytesRead == -1) {
                                // connection closed
                                closeConnection(key);
                                return;
                            }
                            if (fragment.hasRemaining()) {
                                // not enough data...
                                break;
                            } else {
                                // reset fragment header position to read next
                                // fragment
                                fragmentHeader.position(0);
                                
                                if (rq.isAllFragmentsReceived()) {
                                    con.setReceive(null);
                                    // request is complete... send to receiver
                                    if (Logging.isDebug()) {
                                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, rq
                                                .toString());
                                    }
                                    con.getOpenRequests().incrementAndGet();
                                    if (Logging.isDebug())
                                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                            "request received");
                                    pendingRequests++;
                                    if (!receiveRequest(key, rq, con)) {
                                        closeConnection(key);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
     * @param key
     *            the writable key
     */
    private void writeConnection(SelectionKey key) {
        final ClientConnection con = (ClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        try {
            
            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {
                    
                    while (true) {
                        
                        // final ByteBuffer fragmentHeader =
                        // con.getSendFragHdr();
                        
                        ONCRPCRecord rq = con.getSend();
                        if (rq == null) {
                            synchronized (con) {
                                rq = con.getPendingResponses().poll();
                                if (rq == null) {
                                    // no more responses, stop writing...
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    break;
                                }
                                con.setSend(rq);
                            }
                            // create fragment header
                            /*
                             * fragmentHeader.position(0); final int
                             * fragmentSize = rq.getResponseSize(); final
                             * boolean isLastFragment = true; final int
                             * fragmentHeaderInt =
                             * ONCRPCRecordFragmentHeader.getFragmentHeader
                             * (fragmentSize, isLastFragment);
                             * fragmentHeader.putInt(fragmentHeaderInt);
                             * fragmentHeader.position(0);
                             */
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
                        final long numBytesWritten = channel.write(rq.getResponseSendBuffers());
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
                        if (!rq.responseComplete()) {
                            // not enough data...
                            break;
                        }
                        // finished sending fragment
                        // clean up :-) request finished
                        pendingRequests--;
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                "sent response for %s", rq.toString());
                        }
                        rq.freeBuffers();
                        con.setSend(null);
                        int numRq = con.getOpenRequests().decrementAndGet();
                        
                        if ((key.interestOps() & SelectionKey.OP_READ) == 0) {
                            if (numRq < (MAX_CLIENT_QUEUE - CLIENT_Q_THR)) {
                                // read from client again
                                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                    "client allowed to send data again: %s", con.getChannel().socket()
                                            .getRemoteSocketAddress().toString());
                            }
                        }
                        
                        continue;
                    }
                }
            }
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
     * @param key
     *            the SelectionKey
     * @param channel
     *            the channel to read from
     * @param buf
     *            the buffer to read to
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
     * @param key
     *            matching key
     */
    private void closeConnection(SelectionKey key) {
        final ClientConnection con = (ClientConnection) key.attachment();
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
     * accept a new incomming connection
     * 
     * @param key
     *            the acceptable key
     */
    private void acceptConnection(SelectionKey key) {
        SocketChannel client = null;
        ClientConnection con = null;
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
            con = new ClientConnection(channelIO);
            
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
                try {
                    channelIO.close();
                } catch (IOException ex2) {
                }
            }
        } catch (IOException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "cannot establish connection: %s", ex.toString());
            if (channelIO != null) {
                try {
                    channelIO.close();
                } catch (IOException ex2) {
                }
            }
        }
    }
    
    /**
     *
     * @param key
     * @param record
     * @param con
     * @return true on success, false on error
     */
    private boolean receiveRequest(SelectionKey key, ONCRPCRecord record, ClientConnection con) {
        try {
            ONCRPCRequest rq = new ONCRPCRequest(record,authProvider.getNewAuthObject());
            
            final ONCRPCRequestHeader hdr = rq.getRequestHeader();

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"received RPC xid=%d type=%d proc=%d",hdr.getXID(),
                        hdr.getMessageType(),hdr.getProcedure());
            }

            if (hdr.getRpcVersion() != 2) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                    "Invalid RPC version: %d, expected 2", hdr.getRpcVersion());
                rq.sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH);
                return true;
            }
            if (hdr.getMessageType() != 0) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                    "Invalid message type: %d, expected 0", hdr.getRpcVersion());
                rq.sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS);
                return true;
            }
            
            receiver.receiveRecord(rq);
            return true;
        } catch (IllegalArgumentException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net,this,"invalid ONCRPC header received: "+ex);
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this,ex);
            }
            return false;
            //closeConnection(key);
        } catch (BufferUnderflowException ex) {
            // close connection if the header cannot be parsed
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net,this,"invalid ONCRPC header received: "+ex);
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this,ex);
            }
            return false;
            //closeConnection(key);
        }
    }
    
    public int getNumConnections() {
        return this.numConnections.get();
    }
    
    public long getPendingRequests() {
        return this.pendingRequests;
    }
}
