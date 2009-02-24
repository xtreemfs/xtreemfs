/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.foundation.oncrpc.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.foundation.pinky.channels.SSLChannelIO;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseListener.Errors;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

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

    private int transactionId;

    private final int requestHdrSize;

    private final int responseHdrSize;

    public RPCNIOSocketClient(SSLOptions sslOptions, int requestTimeout, int connectionTimeout) throws IOException {
        super("RPC Client");
        this.requestTimeout = requestTimeout;
        this.connectionTimeout = connectionTimeout;
        connections = new HashMap();
        selector = Selector.open();
        this.sslOptions = sslOptions;
        quit = false;
        transactionId = 1;

        final ONCRPCRequestHeader tmp = new ONCRPCRequestHeader();
        this.requestHdrSize = tmp.calculateSize();

        final ONCRPCResponseHeader tmp2 = new ONCRPCResponseHeader();
        this.responseHdrSize = tmp.calculateSize();

    }

    public void sendRequest(InetSocketAddress server, RPCRequest request) {

        //get connection
        ServerConnection con = null;
        synchronized (connections) {
            con = connections.get(server);
            if (con == null) {
                con = new ServerConnection();
                connections.put(server, con);
            }
        }
        synchronized (con) {
            boolean isEmpty = con.getSendQueue().isEmpty();
            con.getSendQueue().add(request);
            if (!con.isConnected()) {
                establishConnection(server, con);
            } else {
                if (isEmpty) {
                    final SelectionKey key = con.getChannel().keyFor(selector);
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
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
                //who cares
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_WARN, this, "Exception while selecting: " + ex);
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

                    if (key.isConnectable()) {
                        connectConnection(key);
                    }
                    if (key.isReadable()) {
                        readConnection(key);
                    }
                    if (key.isWritable()) {
                        writeConnection(key);
                    }
                }
            }
            checkForTimers();
        }
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
                channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, con);
            } catch (IOException ex) {
                con.connectFailed();
                for (RPCRequest rq : con.getSendQueue()) {
                    rq.getListener().requestFailed(rq, Errors.SERVER_NOT_REACHABLE);
                }
                con.getSendQueue().clear();
            }
        } else {
            synchronized (con) {
                for (RPCRequest rq : con.getSendQueue()) {
                    rq.getListener().requestFailed(rq, Errors.SERVER_NOT_REACHABLE);
                }
                con.getSendQueue().clear();
            }
        }

    }

    private void readConnection(SelectionKey key) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {
            while (true) {
                if (con.getResponsePayload() == null) {
                    //read headers first
                    ReusableBuffer headerBuffer = con.getResponseHeaders();
                    if (headerBuffer == null) {
                        headerBuffer = BufferPool.allocate(responseHdrSize);
                        con.setResponseHeaders(BufferPool.allocate(responseHdrSize));
                    }

                    //do the read operation
                    final int numBytesRead = RPCNIOSocketServer.readData(key, channel, headerBuffer.getBuffer());
                    if (numBytesRead == -1) {
                        //connection closed
                        closeConnection(key, Errors.CONNECTION_CLOSED);
                        return;
                    }
                    if (headerBuffer.hasRemaining()) {
                        //not enough data...
                        break;
                    } else {
                        //header complete
                        //parse header
                        headerBuffer.flip();
                        final ONCRPCResponseHeader hdr = new ONCRPCResponseHeader();
                        hdr.deserialize(headerBuffer);
                        BufferPool.free(headerBuffer);
                        con.setResponseHeaders(null);
                        con.setRpcResponseHeader(hdr);

                        //allocate buffer for payload
                        final int payloadSize = 0;
                        assert (con.getResponsePayload() == null);
                        con.setResponsePayload(BufferPool.allocate(payloadSize));
                    //FIXME: what to do with the header?? con.setRpcHeader(hdr);
                    }
                } else {
                    //read payload
                    final ReusableBuffer payload = con.getResponsePayload();
                    final int numBytesRead = RPCNIOSocketServer.readData(key, channel, payload.getBuffer());
                    if (numBytesRead == -1) {
                        //connection closed
                        closeConnection(key, Errors.CONNECTION_CLOSED);
                        return;
                    }
                    if (payload.hasRemaining()) {
                        //not enough data to read...
                        break;
                    } else {
                        //request is complete!
                        //FIXME: create response and notify listener(con);
                        RPCRequest r = con.getRequest(con.getRpcResponseHeader().getXID());
                        assert(r != null);
                        r.setResponsePayload(payload);
                        r.setResponseHeaders(con.getRpcResponseHeader());
                        con.setResponsePayload(null);
                        
                        r.getListener().responseAvailable(r);
                    }
                }
            }
        } catch (IOException ex) {
            //simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            closeConnection(key, Errors.CONNECTION_CLOSED);
        }
    }

    private void writeConnection(SelectionKey key) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {
            while (true) {
                final ReusableBuffer headerBuffer = con.getRequestHeaders();
                final ReusableBuffer payload = con.getRequestPayload();

                if ((headerBuffer == null) && (payload == null)) {
                    //fetch next request, if available
                    RPCRequest rq = null;
                    synchronized (con) {
                        rq = con.getSendQueue().poll();
                        if (rq == null) {
                            //no more responses, stop writing...
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                            break;
                        }
                    }
                    //assign transaction id and insert into map
                    final int transId = transactionId++;
                    rq.setRpcTransId(transId);
                    con.addRequest(transId, rq);

                    //wrap response and send it...
                    //FIXME: write headers
                    con.setRequestPayload(rq.getResponsePayload());
                }
                if (headerBuffer != null) {
                    //send header fragment
                    final int numBytesWritten = RPCNIOSocketServer.writeData(key, channel, headerBuffer.getBuffer());
                    if (numBytesWritten == -1) {
                        //connection closed
                        closeConnection(key, Errors.CONNECTION_CLOSED);
                        return;
                    }
                    if (headerBuffer.hasRemaining()) {
                        //not enough data...
                        break;
                    } else {
                        //set header buffer to null
                        BufferPool.free(headerBuffer);
                        con.setRequestHeaders(null);
                    }
                } else {
                    //send payload
                    final int numBytesWritten = RPCNIOSocketServer.writeData(key, channel, payload.getBuffer());
                    if (numBytesWritten == -1) {
                        //connection closed
                        closeConnection(key, Errors.CONNECTION_CLOSED);
                        return;
                    }
                    if (payload.hasRemaining()) {
                        //not enough data...
                        break;
                    } else {
                        BufferPool.free(payload);
                        con.setRequestPayload(null);
                    }
                }
            }
        } catch (IOException ex) {
            //simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            closeConnection(key, Errors.CONNECTION_CLOSED);
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
        } catch (IOException ex) {
            con.connectFailed();
            closeConnection(key, Errors.SERVER_NOT_REACHABLE);
        }

    }

    private void closeConnection(SelectionKey key, Errors reason) {
        final ServerConnection con = (ServerConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        List<RPCRequest> cancelRq = new LinkedList();
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
        }

        //notify listeners
        for (RPCRequest rq : cancelRq) {
            rq.getListener().requestFailed(rq, reason);
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "closing connection to " + channel.socket().getRemoteSocketAddress());
        }
    }

    private void checkForTimers() {
        //poor man's timer
        long now = TimeSync.getLocalSystemTime();
        if (now >= lastCheck + TIMEOUT_GRANULARITY) {
            //check for timed out requests
            synchronized (connections) {
                Iterator<ServerConnection> conIter = connections.values().iterator();
                while (conIter.hasNext()) {
                    final ServerConnection con = conIter.next();

                    if (con.getLastUsed() < (TimeSync.getLocalSystemTime() - connectionTimeout)) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "removing idle connection");
                        }
                        try {
                            conIter.remove();
                            closeConnection(con.getChannel().keyFor(selector), Errors.CONNECTION_UNUSED);
                        } catch (Exception ex) {
                        }
                    } else {
                        //check for request timeout
                        List<RPCRequest> cancelRq = new LinkedList();
                        synchronized (con) {
                            Iterator<RPCRequest> iter = con.getRequests().values().iterator();
                            while (iter.hasNext()) {
                                final RPCRequest rq = iter.next();
                                if (rq.getTimeQueued() + requestTimeout < TimeSync.getLocalSystemTime()) {
                                    cancelRq.add(rq);
                                    iter.remove();
                                }
                            }
                            iter = con.getSendQueue().iterator();
                            while (iter.hasNext()) {
                                final RPCRequest rq = iter.next();
                                if (rq.getTimeQueued() + requestTimeout < TimeSync.getLocalSystemTime()) {
                                    cancelRq.add(rq);
                                    iter.remove();
                                } else {
                                    //requests are ordered :-)
                                    break;
                                }
                            }
                        }
                        for (RPCRequest rq : cancelRq) {
                            rq.getListener().requestFailed(rq, Errors.TIMEOUT);
                        }

                    }
                }

                lastCheck = now;
            }
        }
    }


    }
