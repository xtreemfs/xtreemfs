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
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.foundation.pinky.channels.SSLChannelIO;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.interfaces.Exceptions.Exceptions;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.ErrNo;

/**
 *
 * @author bjko
 */
public class RPCNIOSocketClient extends LifeCycleThread {

    /**
     * Maxmimum tries to reconnect to the server
     */
    public static final int MAX_RECONNECT = 4;

    /**
     * milliseconds between two timeout checks
     */
    public static final int TIMEOUT_GRANULARITY = 250;

    private final Map<InetSocketAddress, ServerConnection> connections;

    private final int requestTimeout;

    private final int connectionTimeout;

    private long lastCheck;

    private final Selector selector;

    private volatile boolean quit;

    private final SSLOptions sslOptions;

    private final AtomicInteger transactionId;

    private final ConcurrentLinkedQueue<ServerConnection> toBeEstablished;

    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout) throws IOException {
        super("RPC Client");
        if (requestTimeout >= connectionTimeout - TIMEOUT_GRANULARITY * 2) {
            throw new IllegalArgumentException("request timeout must be smaller than connection timeout less " + TIMEOUT_GRANULARITY * 2 + "ms");
        }
        this.requestTimeout = requestTimeout;
        this.connectionTimeout = connectionTimeout;
        connections = new HashMap();
        selector = Selector.open();
        this.sslOptions = sslOptions;
        quit = false;
        transactionId = new AtomicInteger((int)(Math.random()*1e6+1.0));
        toBeEstablished = new ConcurrentLinkedQueue<ServerConnection>();
    }

    public void sendRequest(RPCResponseListener listener, InetSocketAddress server, int programId,
            int versionId, int procedureId, Serializable message) {
        sendRequest(listener, server, programId, versionId, procedureId, message, null);
    }

    public void sendRequest(RPCResponseListener listener, InetSocketAddress server, int programId,
            int versionId, int procedureId, Serializable message, Object attachment) {
        sendRequest(listener, server, programId, versionId, procedureId, message, attachment, null);
    }

    public void sendRequest(RPCResponseListener listener, InetSocketAddress server, int programId,
            int versionId, int procedureId, Serializable message, Object attachment, UserCredentials credentials) {
        ONCRPCRequest rec = new ONCRPCRequest(listener, this.transactionId.getAndIncrement(), programId, versionId, procedureId, message, attachment,credentials);
        sendRequest(server, rec);
    }

    private void sendRequest(InetSocketAddress server, ONCRPCRequest request) {
        if (Logging.tracingEnabled()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "send request " + request + " no " + transactionId.get());
        }
        //get connection
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

        Logging.logMessage(Logging.LEVEL_INFO, this, "ONCRPC Client running");

        while (!quit) {

            int numKeys = 0;

            try {
                numKeys = selector.select(TIMEOUT_GRANULARITY);
            } catch (CancelledKeyException ex) {
                //who cares
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_WARN, this, "Exception while selecting: " + ex);
                continue;
            }

            if (!toBeEstablished.isEmpty()) {
                while (true) {
                    ServerConnection con = toBeEstablished.poll();
                    if (con == null) {
                        break;
                    }
                    try {
                        con.getChannel().register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, con);
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
            }
        }

        Logging.logMessage(Logging.LEVEL_INFO, this, "ONCRPC Client stopped");

        notifyStopped();
    }

    private void establishConnection(InetSocketAddress server, ServerConnection con) {

        if (con.canReconnect()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connect to " + server);
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
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "established");
            } catch (IOException ex) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "cannot contact server " + con.getEndpoint());
                }
                con.connectFailed();
                for (ONCRPCRequest rq : con.getSendQueue()) {
                    rq.getListener().requestFailed(rq, new IOException("server not reachable", ex));
                }
                con.getSendQueue().clear();
            }
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "reconnect to server still blocked " + con.getEndpoint());
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

                            //do the read operation
                            final int numBytesRead = RPCNIOSocketServer.readData(key, channel, respFragHdr);
                            if (numBytesRead == -1) {
                                //connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (respFragHdr.hasRemaining()) {
                                //not enough data...
                                break;
                            } else {
                                //receive fragment
                                respFragHdr.position(0);
                                final int fragHdrInt = respFragHdr.getInt();
                                final int fragmentSize = ONCRPCRecordFragmentHeader.getFragmentLength(fragHdrInt);
                                final boolean isLastFragment = ONCRPCRecordFragmentHeader.isLastFragment(fragHdrInt);
                                ReusableBuffer fragment = BufferPool.allocate(fragmentSize);
                                con.addResponseFragment(fragment);
                                con.setLastResponseFragReceived(isLastFragment);
                            }
                        } else {
                            //read payload
                            final ReusableBuffer buf = con.getCurrentResponseFragment();
                            final int numBytesRead = RPCNIOSocketServer.readData(key, channel, buf.getBuffer());
                            if (numBytesRead == -1) {
                                //connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (buf.hasRemaining()) {
                                //not enough data to read...
                                break;
                            } else {
                                if (con.isLastResponseFragReceived()) {
                                    //request is complete
                                    assembleResponse(key, con);
                                } else {
                                    //next fragment
                                }
                                respFragHdr.position(0);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            //simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            closeConnection(key, new IOException("server closed connection", ex));
        }
    }

    private void assembleResponse(SelectionKey key, ServerConnection con) {
        //parse the ONCRPCHeader to get XID

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "assemble response");

        ONCRPCResponseHeader hdr = null;
        ReusableBuffer firstFragment = null;
        try {
            firstFragment = con.getResponseFragments().get(0);
            firstFragment.position(0);
            hdr = new ONCRPCResponseHeader();
            hdr.deserialize(firstFragment);

        } catch (Exception ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "received invalid response from " + con.getChannel().socket().getRemoteSocketAddress());
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            closeConnection(key, new IOException("invalid response header sent"));
            return;
        }
        final int xid = hdr.getXID();
        ONCRPCRequest rec = con.getRequest(xid);
        if (rec == null) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "received response for unknown request with XID " + xid);
            con.clearResponseFragments();
            return;
        }
        rec.setResponseFragments(con.getResponseFragments());
        con.clearResponseFragments();

        //check for result and exception stuff
        if (hdr.getAcceptStat() == ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS) {
            rec.getListener().responseAvailable(rec);
        } else {
            //check if there is an exception name and throw it
            ONCRPCException exception = null;
            String exName = null;
            if (firstFragment.hasRemaining()) {
                try {
                    final int exNameLen = firstFragment.getInt();
                    final byte[] exBytes = new byte[exNameLen];
                    firstFragment.get(exBytes);
                    exName = new String(exBytes);
                    if (exNameLen % 4 > 0) {
                        for (int i = 0; i < (4 - exNameLen % 4); i++) {
                            firstFragment.get();
                        }
                    }
                    exception = Exceptions.createException(exName);
                    Serializable exAsSer = (Serializable) exception;
                    exAsSer.deserialize(firstFragment);
                } catch (IOException ex) {
                    exName = "IOException";
                    rec.getListener().requestFailed(rec, new IOException("invalid exception data received"));
                    return;
                }
            }
            if (exName == null) {
                //throw exception deduced from accept stat type
                exception = new ProtocolException(hdr.getAcceptStat(), ErrNo.EINVAL, "");
            }
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "reveived remote exception: " + exName + "/" + exception);
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
                                    //no more responses, stop writing...
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    break;
                                }
                                con.setSendRequest(send);
                            }
                            //send request as single fragment
                            //create fragment header
                            final ByteBuffer fragHdrBuffer = con.getRequestFragHdr();
                            final int fragmentSize = send.getRequestSize();
                            final int fragHdrInt = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize, true);
                            fragHdrBuffer.position(0);
                            fragHdrBuffer.putInt(fragHdrInt);
                            fragHdrBuffer.position(0);
                        }

                        final ByteBuffer fragHdrBuffer = con.getRequestFragHdr();
                        if (fragHdrBuffer.hasRemaining()) {
                            //send header fragment
                            final int numBytesWritten = RPCNIOSocketServer.writeData(key, channel, fragHdrBuffer);
                            if (numBytesWritten == -1) {
                                //connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (fragHdrBuffer.hasRemaining()) {
                                //not enough data...
                                break;
                            }
                        } else {
                            //send payload
                            final ReusableBuffer buf = send.getCurrentRequestBuffer();
                            final int numBytesWritten = RPCNIOSocketServer.writeData(key, channel, buf.getBuffer());
                            if (numBytesWritten == -1) {
                                //connection closed
                                closeConnection(key, new IOException("server closed connection"));
                                return;
                            }
                            if (buf.hasRemaining()) {
                                //not enough data...
                                break;
                            } else {
                                if (!send.isLastRequestBuffer()) {
                                    send.nextRequestBuffer();
                                    continue;
                                } else {
                                    con.addRequest(send.getXID(), send);
                                    con.setSendRequest(null);
                                    if (Logging.tracingEnabled()) {
                                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "sent request to " + con.getEndpoint());
                                    }
                                }
                            //otherwise the request is complete
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            //simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
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
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connected from " + con.getChannel().socket().getLocalSocketAddress() + " to " + con.getEndpoint());
            }
        } catch (IOException ex) {
            con.connectFailed();
            closeConnection(key, new IOException("server not reachable", ex));
        }

    }

    private void closeConnection(SelectionKey key, IOException exception) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        List<ONCRPCRequest> cancelRq = new LinkedList();
        synchronized (con) {
            //remove the connection from the selector and close socket
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

        //notify listeners
        for (ONCRPCRequest rq : cancelRq) {
            rq.getListener().requestFailed(rq, exception);
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "closing connection to " + con.getEndpoint());
        }
    }

    private void checkForTimers() {
        //poor man's timer
        long now = System.currentTimeMillis();
        if (now >= lastCheck + TIMEOUT_GRANULARITY) {
            //check for timed out requests
            synchronized (connections) {
                Iterator<ServerConnection> conIter = connections.values().iterator();
                while (conIter.hasNext()) {
                    final ServerConnection con = conIter.next();

                    if (con.getLastUsed() < (now - connectionTimeout)) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "removing idle connection");
                        }
                        try {
                            conIter.remove();
                            closeConnection(con.getChannel().keyFor(selector), null);
                        } catch (Exception ex) {
                        }
                    } else {
                        //check for request timeout
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
                                    //requests are ordered :-)
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
}
