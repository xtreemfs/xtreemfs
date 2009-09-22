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
package org.xtreemfs.foundation.oncrpc.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.ProtocolException;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.utils.ONCRPCError;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
/**
 * 
 * @author bjko
 */
public class RPCNIOSocketClient extends LifeCycleThread {

    public static boolean ENABLE_STATISTICS = false;

    
    /**
     * Maxmimum tries to reconnect to the server
     */
    public static final int                                MAX_RECONNECT       = 4;
    
    /**
     * milliseconds between two timeout checks
     */
    public static final int                                TIMEOUT_GRANULARITY = 250;
    
    private final Map<InetSocketAddress, ServerConnection> connections;
    
    private final int                                      requestTimeout;
    
    private final int                                      connectionTimeout;
    
    private long                                           lastCheck;
    
    private final Selector                                 selector;
    
    private volatile boolean                               quit;
    
    private final SSLOptions                               sslOptions;
    
    private final AtomicInteger                            transactionId;
    
    private final ConcurrentLinkedQueue<ServerConnection>  toBeEstablished;
    
    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout)
        throws IOException {
        super("RPC Client");
        if (requestTimeout >= connectionTimeout - TIMEOUT_GRANULARITY * 2) {
            throw new IllegalArgumentException(
                "request timeout must be smaller than connection timeout less " + TIMEOUT_GRANULARITY * 2
                    + "ms");
        }
        this.requestTimeout = requestTimeout;
        this.connectionTimeout = connectionTimeout;
        connections = new HashMap();
        selector = Selector.open();
        this.sslOptions = sslOptions;
        quit = false;
        transactionId = new AtomicInteger((int) (Math.random() * 1e6 + 1.0));
        toBeEstablished = new ConcurrentLinkedQueue<ServerConnection>();
    }
    
    public void sendRequest(RPCResponseListener listener, InetSocketAddress server, int programId,
        int versionId, int procedureId, yidl.runtime.Object message) {
        sendRequest(listener, server, programId, versionId, procedureId, message, null);
    }
    
    public void sendRequest(RPCResponseListener listener, InetSocketAddress server, int programId,
        int versionId, int procedureId, yidl.runtime.Object message, Object attachment) {
        sendRequest(listener, server, programId, versionId, procedureId, message, attachment, null);
    }
    
    public void sendRequest(RPCResponseListener listener, InetSocketAddress server, int programId,
        int versionId, int procedureId, yidl.runtime.Object message, Object attachment, UserCredentials credentials) {
        ONCRPCRequest rec = new ONCRPCRequest(listener, this.transactionId.getAndIncrement(), programId,
            versionId, procedureId, message, attachment, credentials);
        sendRequest(server, rec);
    }
    
    private void sendRequest(InetSocketAddress server, ONCRPCRequest request) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "sending request %s no %d", request
                    .toString(), transactionId.get());
        }
        // get connection
        ServerConnection con = null;
        synchronized (connections) {
            con = connections.get(server);
            if (con == null) {
                con = new ServerConnection(server);
                connections.put(server, con);
            }
        }
        synchronized (con) {
            boolean isEmpty = con.getSendQueue().isEmpty();
            request.queued();
            con.useConnection();
            con.getSendQueue().add(request);
            if (!con.isConnected()) {
                establishConnection(server, con);
                
            } else {
                if (isEmpty) {
                    final SelectionKey key = con.getChannel().keyFor(selector);
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
                selector.wakeup();
            }
        }
    }
    
    public void run() {
        
        notifyStarted();
        lastCheck = System.currentTimeMillis();
        
        while (!quit) {
            
            int numKeys = 0;
            
            try {
                numKeys = selector.select(TIMEOUT_GRANULARITY);
            } catch (CancelledKeyException ex) {
                // who cares
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.net, this, "Exception while selecting: %s",
                    ex.toString());
                continue;
            }
            
            if (!toBeEstablished.isEmpty()) {
                while (true) {
                    ServerConnection con = toBeEstablished.poll();
                    if (con == null) {
                        break;
                    }
                    try {
                        con.getChannel().register(selector,
                            SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, con);
                    } catch (ClosedChannelException ex) {
                        closeConnection(con.getChannel().keyFor(selector), ex);
                    }
                }
                toBeEstablished.clear();
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
            checkForTimers();
        }
        
        synchronized (connections) {
            for (ServerConnection con : connections.values()) {
                for (ONCRPCRequest rq : con.getSendQueue()) {
                    rq.getListener().requestFailed(rq, new IOException("client was shut down"));
                }
                for (ONCRPCRequest rq : con.getRequests().values()) {
                    rq.getListener().requestFailed(rq, new IOException("client was shut down"));
                }
                try {
                    if (con.getChannel() != null)
                        con.getChannel().close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        notifyStopped();
    }
    
    private void establishConnection(InetSocketAddress server, ServerConnection con) {
        
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
                    channel = new SSLChannelIO(SocketChannel.open(), sslOptions, true);
                }
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(true);
                channel.socket().setReceiveBufferSize(256 * 1024);
                channel.connect(server);
                con.setChannel(channel);
                toBeEstablished.add(con);
                selector.wakeup();
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "connection established");
            } catch (IOException ex) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "cannot contact server %s",
                        con.getEndpoint().toString());
                }
                con.connectFailed();
                for (ONCRPCRequest rq : con.getSendQueue()) {
                    rq.getListener().requestFailed(rq, new IOException("server not reachable", ex));
                }
                con.getSendQueue().clear();
            }
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "reconnect to server still blocked %s", con.getEndpoint().toString());
            }
            synchronized (con) {
                for (ONCRPCRequest rq : con.getSendQueue()) {
                    rq.getListener().requestFailed(rq, new IOException("server not reachable"));
                }
                con.getSendQueue().clear();
            }
        }
        
    }
    
    private void readConnection(SelectionKey key) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        try {
            
            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {
                    
                    while (true) {
                        final ByteBuffer respFragHdr = con.getResponseFragHdr();
                        if (respFragHdr.hasRemaining()) {
                            
                            // do the read operation
                            final int numBytesRead = RPCNIOSocketServer.readData(key, channel, respFragHdr);
                            if (numBytesRead == -1) {
                                // connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (respFragHdr.hasRemaining()) {
                                // not enough data...
                                break;
                            } else {
                                // receive fragment
                                respFragHdr.position(0);
                                final int fragHdrInt = respFragHdr.getInt();
                                final int fragmentSize = ONCRPCRecordFragmentHeader
                                        .getFragmentLength(fragHdrInt);
                                final boolean isLastFragment = ONCRPCRecordFragmentHeader
                                        .isLastFragment(fragHdrInt);
                                assert (fragmentSize > 0) : "fragment has wrong size: " + fragmentSize;
                                ReusableBuffer fragment = BufferPool.allocate(fragmentSize);
                                con.addResponseFragment(fragment);
                                con.setLastResponseFragReceived(isLastFragment);
                            }
                        } else {
                            // read payload
                            final ReusableBuffer buf = con.getCurrentResponseFragment();
                            final int numBytesRead = RPCNIOSocketServer.readData(key, channel, buf
                                    .getBuffer());
                            if (numBytesRead == -1) {
                                // connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (buf.hasRemaining()) {
                                // not enough data to read...
                                break;
                            } else {
                                if (con.isLastResponseFragReceived()) {
                                    // request is complete
                                    assembleResponse(key, con);
                                } else {
                                    // next fragment
                                }
                                respFragHdr.position(0);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key, new IOException("server closed connection", ex));
        }
    }
    
    private void assembleResponse(SelectionKey key, ServerConnection con) {
        // parse the ONCRPCHeader to get XID
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "assemble response");

        ONCRPCResponseHeader hdr = null;
        ReusableBuffer firstFragment = null;
        try {
            firstFragment = con.getResponseFragments().get(0);
            firstFragment.position(0);
            hdr = new ONCRPCResponseHeader();
            hdr.unmarshal(new XDRUnmarshaller(firstFragment));
            
        } catch (Exception ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "received invalid response from %s", con.getChannel().socket().getRemoteSocketAddress()
                            .toString());
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key, new IOException("invalid response header sent"));
            return;
        }
        final int xid = hdr.getXID();
        ONCRPCRequest rec = con.getRequest(xid);
        if (rec == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                "received response for unknown request with XID %d", xid);
            con.clearResponseFragments();
            return;
        }
        if (ENABLE_STATISTICS) {
            rec.endT = System.nanoTime();
            con.bytesRX += firstFragment.capacity();
        }
        rec.setResponseFragments(con.getResponseFragments());
        con.clearResponseFragments();

        final int accept_stat = hdr.getAcceptStat();

        // check for result and exception stuff
        if (accept_stat == ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS) {
            rec.getListener().responseAvailable(rec);
        } else {

            ONCRPCException exception = null;

            if (accept_stat <= ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR) {
                //ONC RPC error message
                exception = new ONCRPCError(accept_stat);
            } else {
                //exception
                try {
                    if (accept_stat >= DIRInterface.getVersion() && (accept_stat < DIRInterface.getVersion()+100)) {
                        exception = DIRInterface.createException(accept_stat);
                    } else if (accept_stat >= MRCInterface.getVersion() && (accept_stat < MRCInterface.getVersion()+100)) {
                        exception = MRCInterface.createException(accept_stat);
                    } else if (accept_stat >= OSDInterface.getVersion() && (accept_stat < OSDInterface.getVersion()+100)) {
                        exception = OSDInterface.createException(accept_stat);
                    } else {
                        throw new Exception();
                    }
                } catch (Exception ex) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this,"received invalid remote exception id %d",accept_stat);
                        exception = new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR, 0, "received invalid remote exception with id "+accept_stat);
                }
                assert(exception != null);
                try {
                    exception.unmarshal(new XDRUnmarshaller(firstFragment));
                } catch (Throwable ex) {
                    rec.getListener().requestFailed(rec, new IOException("invalid exception data received: "+ex));
                    return;
                }
            }
            rec.getListener().remoteExceptionThrown(rec, exception);
        }
    }
    
    private void writeConnection(SelectionKey key) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        try {
            
            if (!channel.isShutdownInProgress()) {
                if (channel.doHandshake(key)) {
                    
                    while (true) {
                        ONCRPCRequest send = con.getSendRequest();
                        if (send == null) {
                            synchronized (con) {
                                send = con.getSendQueue().poll();
                                if (send == null) {
                                    // no more responses, stop writing...
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    break;
                                }
                                con.setSendRequest(send);
                            }
                            // send request as single fragment
                            // create fragment header
                            final ByteBuffer fragHdrBuffer = con.getRequestFragHdr();
                            final int fragmentSize = send.getRequestSize();
                            final int fragHdrInt = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize,
                                true);
                            fragHdrBuffer.position(0);
                            fragHdrBuffer.putInt(fragHdrInt);
                            fragHdrBuffer.position(0);
                            if (ENABLE_STATISTICS) {
                                send.startT = System.nanoTime();
                                con.bytesTX += 4+fragmentSize;
                            }
                        }
                        
                        final ByteBuffer fragHdrBuffer = con.getRequestFragHdr();
                        if (fragHdrBuffer.hasRemaining()) {
                            // send header fragment
                            final int numBytesWritten = RPCNIOSocketServer.writeData(key, channel,
                                fragHdrBuffer);
                            if (numBytesWritten == -1) {
                                // connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (fragHdrBuffer.hasRemaining()) {
                                // not enough data...
                                break;
                            }
                        } else {
                            // send payload
                            final ReusableBuffer buf = send.getCurrentRequestBuffer();
                            final int numBytesWritten = RPCNIOSocketServer.writeData(key, channel, buf
                                    .getBuffer());
                            if (numBytesWritten == -1) {
                                // connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (buf.hasRemaining()) {
                                // not enough data...
                                break;
                            } else {
                                if (!send.isLastRequestBuffer()) {
                                    send.nextRequestBuffer();
                                    continue;
                                } else {
                                    con.addRequest(send.getXID(), send);
                                    con.setSendRequest(null);
                                    if (Logging.isDebug()) {
                                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                            "sent request %d to %s", send.getXID(), con.getEndpoint()
                                                    .toString());
                                    }
                                }
                                // otherwise the request is complete
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            // simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, OutputUtils
                        .stackTraceToString(ex));
            }
            closeConnection(key, new IOException("server closed connection", ex));
        }
    }
    
    private void connectConnection(SelectionKey key) {
        final ServerConnection con = (ServerConnection) key.attachment();
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
                        .getChannel().socket().getLocalSocketAddress().toString(), con.getEndpoint()
                        .toString());
            }
        } catch (IOException ex) {
            con.connectFailed();
            String endpoint;
            try {
                endpoint = con.getEndpoint().toString();
            } catch (Exception ex2) {
                endpoint = "unknown";
            }
            closeConnection(key, new IOException("server '"+endpoint+"' not reachable", ex));
        }
        
    }
    
    private void closeConnection(SelectionKey key, IOException exception) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();
        
        List<ONCRPCRequest> cancelRq = new LinkedList();
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
        for (ONCRPCRequest rq : cancelRq) {
            rq.getListener().requestFailed(rq, exception);
        }
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "closing connection to %s", con
                    .getEndpoint().toString());
        }
    }
    
    private void checkForTimers() {
        // poor man's timer
        long now = System.currentTimeMillis();
        if (now >= lastCheck + TIMEOUT_GRANULARITY) {
            // check for timed out requests
            synchronized (connections) {
                Iterator<ServerConnection> conIter = connections.values().iterator();
                while (conIter.hasNext()) {
                    final ServerConnection con = conIter.next();
                    
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
                        List<ONCRPCRequest> cancelRq = new LinkedList();
                        synchronized (con) {
                            Iterator<ONCRPCRequest> iter = con.getRequests().values().iterator();
                            while (iter.hasNext()) {
                                final ONCRPCRequest rq = iter.next();
                                if (rq.getTimeQueued() + requestTimeout < now) {
                                    cancelRq.add(rq);
                                    iter.remove();
                                }
                            }
                            iter = con.getSendQueue().iterator();
                            while (iter.hasNext()) {
                                final ONCRPCRequest rq = iter.next();
                                if (rq.getTimeQueued() + requestTimeout < now) {
                                    cancelRq.add(rq);
                                    iter.remove();
                                } else {
                                    // requests are ordered :-)
                                    break;
                                }
                            }
                        }
                        for (ONCRPCRequest rq : cancelRq) {
                            rq.getListener().requestFailed(rq, new IOException("request timed out"));
                        }
                        
                    }
                }
                
                lastCheck = now;
            }
        }
    }
    
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
        ServerConnection con = null;
         synchronized (connections) {
             con = connections.get(server);
         }
        if (con == null)
            return null;
        else
            return new long[]{con.bytesRX,con.bytesTX};
    }
}
