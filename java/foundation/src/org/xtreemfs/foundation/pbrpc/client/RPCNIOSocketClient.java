/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 * Copyright (c) 2013 by Bjoern Kolbeck.
 * Copyright (c) 2014 by Quobyte Inc.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;
import org.xtreemfs.foundation.pbrpc.channels.SSLChannelIO;
import org.xtreemfs.foundation.pbrpc.channels.SSLHandshakeOnlyChannelIO;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServerConnection;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.util.OutputUtils;

import com.google.protobuf.Message;
/**
 * 
 * @author bjko
 */
public class RPCNIOSocketClient extends LifeCycleThread {

    public static boolean ENABLE_STATISTICS = false;

    
    /**
     * Maximum tries to reconnect to the server
     */
    public static final int                                   MAX_RECONNECT       = 4;
    
    /**
     * milliseconds between two timeout checks
     */
    public static final int                                   TIMEOUT_GRANULARITY = 250;
    
    private final Map<InetSocketAddress, RPCClientConnection> connections;
    
    private final int                                         requestTimeout;
    
    private final int                                         connectionTimeout;

    private AtomicLong                                        lastCheck;
    
    private final Selector                                    selector;
    
    private volatile boolean                                  quit;
    
    private final SSLOptions                                  sslOptions;
    
    private final AtomicInteger                               transactionId;
    
    private final ConcurrentLinkedQueue<RPCClientConnection>  toBeEstablished;
    
    private final int                                         sendBufferSize;
    
    private final int                                         receiveBufferSize;
    
    private final SocketAddress                               localBindPoint;


    /**
     * on some platforms (e.g. FreeBSD 7.2 with openjdk6) Selector.select(int timeout)
     * returns immediately. If this problem is detected, the thread waits 25ms after each
     * invocation to avoid excessive CPU consumption. See also issue #75
     */
    private boolean                                           brokenSelect;

    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout)
        throws IOException {
        this(sslOptions, requestTimeout, connectionTimeout, -1, -1, null, "", false);
    }

    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout, String threadName)
        throws IOException {
        this(sslOptions, requestTimeout, connectionTimeout, -1, -1, null, threadName, false);
    }
    
    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout,
        int sendBufferSize, int receiveBufferSize, SocketAddress localBindPoint) throws IOException {
        this(sslOptions, requestTimeout, connectionTimeout, sendBufferSize, receiveBufferSize, localBindPoint, "", false);
    }
    
    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout, String threadName, boolean startAsDaemon) throws IOException {
        this(sslOptions, requestTimeout, connectionTimeout, -1, -1, null, threadName, startAsDaemon);
    }
    
    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout,
            int sendBufferSize, int receiveBufferSize, SocketAddress localBindPoint, String threadName) throws IOException {
    	this(sslOptions, requestTimeout, connectionTimeout, sendBufferSize, receiveBufferSize, localBindPoint, threadName, false);
    }
    
    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout,
        int sendBufferSize, int receiveBufferSize, SocketAddress localBindPoint, String threadName, boolean startAsDaemon) throws IOException {
        super(threadName);
        setDaemon(startAsDaemon);
        if (requestTimeout >= connectionTimeout - TIMEOUT_GRANULARITY * 2) {
            throw new IllegalArgumentException(
                "request timeout must be smaller than connection timeout less " + TIMEOUT_GRANULARITY * 2
                    + "ms");
        }
        this.lastCheck = new AtomicLong();
        this.requestTimeout = requestTimeout;
        this.connectionTimeout = connectionTimeout;
        this.sendBufferSize = sendBufferSize;
        this.receiveBufferSize = receiveBufferSize;
        this.localBindPoint = localBindPoint;
        connections = new HashMap<InetSocketAddress, RPCClientConnection>();
        selector = Selector.open();
        this.sslOptions = sslOptions;
        quit = false;
        transactionId = new AtomicInteger((int) (Math.random() * 1e6 + 1.0));
        toBeEstablished = new ConcurrentLinkedQueue<RPCClientConnection>();
        
        if (this.localBindPoint != null && Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "RPC Client '%s': Using the following address for outgoing connections: %s", threadName, this.localBindPoint);
        }
    }
    
    
    public void sendRequest(InetSocketAddress server, Auth auth, UserCredentials uCred, int interface_id, int proc_id, Message message, ReusableBuffer data,
            RPCResponse response, boolean highPriority) {
        try {
            RPCClientRequest rq = new RPCClientRequest(auth, uCred, transactionId.incrementAndGet(), interface_id, proc_id, message, data, response);
            internalSendRequest(server, rq, highPriority);
        } catch (Throwable e) { // CancelledKeyException, RuntimeException (caused by missing TimeSyncThread)
            //e.printStackTrace();
            response.requestFailed(e.toString());
        } 
    }
    
    private void internalSendRequest(InetSocketAddress server, RPCClientRequest request, boolean highPriority) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "sending request %s no %d", request
                    .toString(), transactionId.get());
        }
        // get connection
        RPCClientConnection con = null;
        synchronized (connections) {
            con = connections.get(server);
            if (con == null) {
                con = new RPCClientConnection(server);
                connections.put(server, con);
            }
        }
        synchronized (con) {
            boolean isEmpty = con.getSendQueue().isEmpty();
            request.queued();
            con.useConnection();
            if (highPriority)
                con.getSendQueue().add(0, request);
            else
                con.getSendQueue().add(request);
            
            if (!con.isConnected()) {
                establishConnection(server, con);

            } else {
                if (isEmpty) {
                    final SelectionKey key = con.getChannel().keyFor(selector);
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
        }
    }
    
    @Override
    public void run() {

        brokenSelect = false;
        // Doesn't work properly, should be a replaced with a better way to detect
        // a broken selector on FreeBSD.
        /*try {
            long now = System.currentTimeMillis();
            int numKeys = selector.select(100);
            long duration = System.currentTimeMillis()-now;
            if ((duration < 10) && (numKeys == 0)) {
                Logging.logMessage(Logging.LEVEL_WARN, this,"detected broken select(int timeout)!");
                brokenSelect = true;
            }
        } catch (Throwable th) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"could not check Selector for broken select(int timeout): "+th);
        }*/
        
        notifyStarted();
        lastCheck.set(System.currentTimeMillis());

        try {
            while (!quit) {
                if (!toBeEstablished.isEmpty()) {
                    while (true) {
                        RPCClientConnection con = toBeEstablished.poll();
                        if (con == null) {
                            break;
                        }
                        try {
                            con.getChannel().register(selector,
                                SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, con);
                        } catch (ClosedChannelException ex) {
                            closeConnection(con.getChannel().keyFor(selector), ex.toString());
                        }
                    }
                    toBeEstablished.clear();
                }
                
                int numKeys = 0;
                try {
                    numKeys = selector.select(TIMEOUT_GRANULARITY);
                } catch (CancelledKeyException ex) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this, "Exception while selecting: %s",
                        ex.toString());
                    continue;
                } catch (IOException ex) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this, "Exception while selecting: %s",
                        ex.toString());
                    continue;
                }
                if (numKeys > 0) {
                    // fetch events
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    
                    // process all events
                    while (iter.hasNext()) {
                        try {
                            SelectionKey key = iter.next();
                            
                            // remove key from the list
                            iter.remove();
                            
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
                            continue;
                        }
                    }
                }
    
                if (numKeys == 0 && brokenSelect) {
    
                    try {
                        sleep(25);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
                try {
                    checkForTimers();
                } catch (ConcurrentModificationException ce) {
                    Logging.logMessage(Logging.LEVEL_CRIT, this, 
                            OutputUtils.getThreadDump());
                }
            }
        } catch (Throwable thr) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "PBRPC Client CRASHED!");
            notifyCrashed(thr);
        }            
        
        synchronized (connections) {
            for (RPCClientConnection con : connections.values()) {
                synchronized (con) {
                    for (RPCClientRequest rq : con.getSendQueue()) {
                        rq.getResponse().requestFailed("RPC cancelled due to client shutdown");
                        rq.freeBuffers();
                    }
                    for (RPCClientRequest rq : con.getRequests().values()) {
                        rq.getResponse().requestFailed("RPC cancelled due to client shutdown");
                        rq.freeBuffers();
                    }
                    try {
                        if (con.getChannel() != null)
                            con.getChannel().close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        
        notifyStopped();
    }
    
    private void establishConnection(InetSocketAddress server, RPCClientConnection con) {
        
        if (con.canReconnect()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connect to %s", server
                        .toString());
            }
            ChannelIO channel;
            try {
                if (sslOptions == null) { // no SSL
                    channel = new ChannelIO(SocketChannel.open());
                } else {
                    if (sslOptions.isFakeSSLMode()) {
                        channel = new SSLHandshakeOnlyChannelIO(SocketChannel.open(), sslOptions, true);
                    } else {
                        channel = new SSLChannelIO(SocketChannel.open(), sslOptions, true);
                    }
                }
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(true);
                if (localBindPoint != null) {
                    channel.socket().bind(localBindPoint);
                }
                
                if (sendBufferSize != -1) {
                    channel.socket().setSendBufferSize(sendBufferSize);
                    if (channel.socket().getSendBufferSize() != sendBufferSize) {
                        Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "could not set socket send buffer size to " + sendBufferSize
                                + ", using default size of " + channel.socket().getSendBufferSize());
                    }
                }
                
                if (receiveBufferSize != -1) {
                    channel.socket().setReceiveBufferSize(receiveBufferSize);
                    if (channel.socket().getReceiveBufferSize() != receiveBufferSize) {
                        Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                            "could not set socket receive buffer size to " + receiveBufferSize
                                + ", using default size of " + channel.socket().getReceiveBufferSize());
                    }
                } else {
                    channel.socket().setReceiveBufferSize(256 * 1024);
                }
                
                channel.connect(server);
                con.setChannel(channel);
                toBeEstablished.add(con);
                selector.wakeup();
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connection created");
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "socket send buffer size: %d",
                        channel.socket().getSendBufferSize());
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "socket receive buffer size: %d",
                        channel.socket().getReceiveBufferSize());
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "local bind point: %s", channel
                                .socket().getLocalAddress());
                }
                
            } catch (Exception ex) {
                if (ex.getClass() == java.net.SocketException.class && ex.getMessage().equals("Invalid argument")) {
                    Logging.logMessage(
                            Logging.LEVEL_ERROR,
                            Category.net,
                            this,
                            "FAILED TO USE THE FOLLOWING ADDRESS FOR OUTGOING REQUESTS: %s. Make sure that the hostname is correctly spelled in the configuration and it resolves to the correct IP.",
                            localBindPoint);
                }
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "cannot contact server %s",
                        con.getEndpointString());
                }
                con.connectFailed();
                for (RPCClientRequest rq : con.getSendQueue()) {
                    rq.getResponse().requestFailed("sending RPC failed: server '"+con.getEndpointString()+"' not reachable ("+ex+")");
                    rq.freeBuffers();
                }
                con.getSendQueue().clear();
                
            }
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "reconnect to server still blocked locally to avoid flooding (server: %s)", con.getEndpointString());
            }
            synchronized (con) {
                for (RPCClientRequest rq : con.getSendQueue()) {
                    rq.getResponse().requestFailed("sending RPC failed: reconnecting to the server '"+con.getEndpointString()+"' was blocked locally to avoid flooding");
                    rq.freeBuffers();
                }
                con.getSendQueue().clear();
            }
        }
        
    }
    
    private void readConnection(SelectionKey key) {
        final RPCClientConnection con = (RPCClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {

            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {

                    while (true) {
                        ByteBuffer buf = null;
                        switch (con.getReceiveState()) {
                            case RECORD_MARKER: {
                                buf = con.getResponseRecordMarker(); break;
                            }
                            case RPC_MESSAGE: {
                                buf = con.getResponseBuffers()[1].getBuffer(); break;
                            }
                            case RPC_HEADER: {
                                buf = con.getResponseBuffers()[0].getBuffer(); break;
                            }
                            case DATA: {
                                buf = con.getResponseBuffers()[2].getBuffer(); break;
                            }
                        }

                        // read fragment header
                        final int numBytesRead = RPCNIOSocketServer.readData(key, channel, buf);
                        if (numBytesRead == -1) {
                            // connection closed
                            if (Logging.isInfo()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "client closed connection (EOF): %s", channel.socket()
                                            .getRemoteSocketAddress().toString());
                            }
                            closeConnection(key,"server ("+channel.socket()
                                            .getRemoteSocketAddress().toString()+") closed connection");
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

                                if ((hdrLen <= 0) || (hdrLen >= RPCNIOSocketServer.MAX_FRAGMENT_SIZE)
                                     || (msgLen < 0) || (msgLen >= RPCNIOSocketServer.MAX_FRAGMENT_SIZE)
                                     || (dataLen < 0) || (dataLen >= RPCNIOSocketServer.MAX_FRAGMENT_SIZE)) {
                                    Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this,
                                        "invalid record marker size (%d/%d/%d) received, closing connection to client %s",
                                        hdrLen,msgLen,dataLen,channel.socket()
                                            .getRemoteSocketAddress().toString());
                                    closeConnection(key,"received invalid record marker from server ("+channel.socket()
                                            .getRemoteSocketAddress().toString()+"), closed connection");
                                    return;
                                }
                                final ReusableBuffer[] buffers = new ReusableBuffer[]{BufferPool.allocate(hdrLen),
                                        ((msgLen > 0) ? BufferPool.allocate(msgLen) : null),
                                        ((dataLen > 0) ? BufferPool.allocate(dataLen) : null) };
                                con.setResponseBuffers(buffers);
                                con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.RPC_HEADER);
                                continue;
                            }

                            case RPC_HEADER: {
                                if (con.getResponseBuffers()[1] != null) {
                                    con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.RPC_MESSAGE);
                                    continue;
                                } else if (con.getResponseBuffers()[2] != null) {
                                    // this is necessary, because we may receive some default 
                                    // instance of a message (empty) with data attached BUG #188
                                    con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.DATA);
                                    continue;
                                } else {
                                    break;
                                }
                            }
                            case RPC_MESSAGE: {
                                if (con.getResponseBuffers()[2] != null) {
                                    con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.DATA);
                                    continue;
                                } else {
                                    break;
                                }

                            }
                        }

                        //assemble ServerRequest
                        con.setReceiveState(RPCNIOSocketServerConnection.ReceiveState.RECORD_MARKER);
                        con.getResponseRecordMarker().clear();

                        //assemble response...
                        assembleResponse(key, con);
                    }
                }
            }
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key, "server closed connection ("+ex+")");
        } catch (NotYetConnectedException e) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(e));
            }
            closeConnection(key, "server closed connection: "+e);
        }
    }

    private void assembleResponse(SelectionKey key, RPCClientConnection con) throws IOException {

        try {
            ReusableBuffer[] receiveBuffers = con.getResponseBuffers();
            receiveBuffers[0].flip();
            if (receiveBuffers[1] != null)
                receiveBuffers[1].flip();
            if (receiveBuffers[2] != null)
                receiveBuffers[2].flip();
            

            ReusableBufferInputStream rbis = new ReusableBufferInputStream(receiveBuffers[0]);
            final RPC.RPCHeader header = RPC.RPCHeader.parseFrom(rbis);
            BufferPool.free(receiveBuffers[0]);

            RPCClientRequest rq = con.getRequest(header.getCallId());
            if (rq == null) {
                // Might happen when a request timed out before a response was
                // sent.
                BufferPool.free(receiveBuffers[1]);
                BufferPool.free(receiveBuffers[2]);
                con.setResponseBuffers(null);
                Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                    "received response for unknown request callId=%d",
                                    header.getCallId());
                return;
            }
            RPCResponse response = rq.getResponse();
            rq.setResponseHeader(header);
            con.setResponseBuffers(null);

            response.responseAvailable(rq, receiveBuffers[1], receiveBuffers[2]);
        } catch (IOException ex) {
            closeConnection(key,"invalid response received: "+ex);
        }

    }
    
    private void writeConnection(SelectionKey key) {
        final RPCClientConnection con = (RPCClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        try {
            
            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {
                    
                    while (true) {
                        ByteBuffer[] buffers = con.getRequestBuffers();
                        RPCClientRequest send = con.getPendingRequest();
                        if (buffers == null) {
                            assert(send == null);
                            synchronized (con) {
                                if (con.getSendQueue().isEmpty()) {
                                    // no more responses, stop writing...
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    break;
                                }
                                send = con.getSendQueue().remove(0);
                            }
                            assert(send != null);
                            con.getRequestRecordMarker().clear();
                            buffers = send.packBuffers(con.getRequestRecordMarker());
                            con.setRequestBuffers(buffers);
                            con.setPendingRequest(send);
                        }

                        assert(buffers != null);
                        final long numBytesWritten = channel.write(buffers);
                        if (numBytesWritten == -1) {
                            if (Logging.isInfo()) {
                                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                                    "client closed connection (EOF): %s", channel.socket()
                                            .getRemoteSocketAddress().toString());
                            }
                            // connection closed
                            closeConnection(key, "server unexpectedly closed connection (EOF)");
                            return;
                        }
                        // Detect if the client writes outside of the fragment.
                        send.recordBytesWritten(numBytesWritten);

                        if (buffers[buffers.length-1].hasRemaining()) {
                            // not enough data...
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            break;
                        }

                        //remove from queue
                        synchronized (con) {
                            con.addRequest(send.getRequestHeader().getCallId(), send);
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "sent request %d to %s", send.getRequestHeader().getCallId(), con.getEndpointString());
                            }
                        }
                        send.checkEnoughBytesSent();
                        con.setRequestBuffers(null);
                        con.setPendingRequest(null);
                    }
                }
            }
        } catch (CancelledKeyException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key, "server closed connection: "+ex);
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key, "server closed connection: "+ex);
        } catch (NotYetConnectedException e) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(e));
            }
            closeConnection(key, "server closed connection: "+e);
        }
    }
    
    private void connectConnection(SelectionKey key) {
        final RPCClientConnection con = (RPCClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        try {
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            synchronized (con) {
                if (!con.getSendQueue().isEmpty()) {
                    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                }
            }
            con.connected();
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connected from %s to %s", con
                        .getChannel().socket().getLocalSocketAddress().toString(), con.getEndpointString());
            }
        } catch (CancelledKeyException ex) {
            con.connectFailed();
            closeConnection(key, "server '" + con.getEndpointString() + "' not reachable ("+ex+")");
        } catch (IOException ex) {
            con.connectFailed();
            closeConnection(key, "server '" + con.getEndpointString() + "' not reachable ("+ex+")");
        }
        
    }
    
    private void closeConnection(SelectionKey key, String errorMessage) {
        final RPCClientConnection con = (RPCClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        List<RPCClientRequest> cancelRq = new LinkedList<RPCClientRequest>();
        synchronized (con) {
            // remove the connection from the selector and close socket
            try {
                key.cancel();
                channel.close();
            } catch (Exception ex) {
            }
            cancelRq.addAll(con.getRequests().values());
            cancelRq.addAll(con.getSendQueue());
            con.getRequests().clear();
            con.getSendQueue().clear();
            con.setChannel(null);
        }
        
        // notify listeners
        for (RPCClientRequest rq : cancelRq) {
            rq.getResponse().requestFailed("sending RPC failed: "+errorMessage);
            rq.freeBuffers();
        }
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "closing connection to %s", con
                    .getEndpointString());
        }
    }
    
    private void checkForTimers() {
        // poor man's timer
        long now = System.currentTimeMillis();
        if (now >= lastCheck.get() + TIMEOUT_GRANULARITY) {
            // check for timed out requests
            synchronized (connections) {
                Iterator<RPCClientConnection> conIter = connections.values().iterator();
                while (conIter.hasNext()) {
                    final RPCClientConnection con = conIter.next();
                    
                    if (con.getLastUsed() < (now - connectionTimeout)) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                "removing idle connection");
                        }
                        try {
                            conIter.remove();
                            closeConnection(con.getChannel().keyFor(selector), null);
                        } catch (Exception ex) {
                        }
                    } else {
                        // check for request timeout
                        List<RPCClientRequest> cancelRq = new LinkedList<RPCClientRequest>();
                        synchronized (con) {
                            Iterator<RPCClientRequest> iter = con.getRequests().values().iterator();
                            while (iter.hasNext()) {
                                final RPCClientRequest rq = iter.next();
                                if (rq.getTimeQueued() + requestTimeout < now) {
                                    cancelRq.add(rq);
                                    iter.remove();
                                }
                            }
                            iter = con.getSendQueue().iterator();
                            while (iter.hasNext()) {
                                final RPCClientRequest rq = iter.next();
                                if (rq.getTimeQueued() + requestTimeout < now) {
                                    cancelRq.add(rq);
                                    iter.remove();
                                } else {
                                    // requests are ordered :-)
                                    break;
                                }
                            }
                        }
                        for (RPCClientRequest rq : cancelRq) {
                            rq.getResponse().requestFailed("sending RPC failed: request timed out");
                            rq.freeBuffers();
                        }
                        
                    }
                }
                
                lastCheck.set(now);
            }
        }
    }
    
    @Override
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

    /**
     * Returns the number of bytes received and transferred from/to a server.
     * @param server
     * @return an array with the number of bytes received [0] and sent [1]
     */
    public long[] getTransferStats(InetSocketAddress server) {
        RPCClientConnection con = null;
         synchronized (connections) {
             con = connections.get(server);
         }
        if (con == null)
            return null;
        else
            return new long[]{con.bytesRX,con.bytesTX};
    }
}
