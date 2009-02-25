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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.foundation.pinky.channels.SSLChannelIO;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;

/**
 *
 * @author bjko
 */
public class RPCNIOSocketServer extends LifeCycleThread {

    /**
     * Maximum number of record fragments supported.
     */
    public static final int MAX_FRAGMENTS = 1;

    /**
     * Maximum fragment size to accept. If the size is larger, the
     * connection is closed.
     */
    public static final int MAX_FRAGMENT_SIZE = 1024*1024*50;

    /**
     * the server socket
     */
    private final ServerSocketChannel socket;

    /**
     * Selector for server socket
     */
    private final Selector selector;

    /**
     * If set to true thei main loop will exit upon next invocation
     */
    private volatile boolean quit;

    /**
     * The receiver that gets all incomming requests.
     */
    private final RPCServerRequestListener receiver;

    /**
     * sslOptions if SSL is enabled, null otherwise
     */
    private final SSLOptions sslOptions;

    /**
     * Connection count
     */
    private final AtomicInteger numConnections;

    /**
     * Port on which the server listens for incomming connections.
     */
    private final int bindPort;

    public RPCNIOSocketServer(int bindPort, InetAddress bindAddr, RPCServerRequestListener rl, SSLOptions sslOptions) throws IOException {
        super("ONCRPCSrv@" + bindPort);

        // open server socket
        socket = ServerSocketChannel.open();
        socket.configureBlocking(false);
        socket.socket().setReceiveBufferSize(256 * 1024);
        socket.socket().setReuseAddress(true);
        socket.socket().bind(bindAddr == null ? new InetSocketAddress(bindPort) : new InetSocketAddress(bindAddr, bindPort));
        this.bindPort = bindPort;

        // create a selector and register socket
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_ACCEPT);

        // server is ready to accept connections now

        this.receiver = rl;

        this.sslOptions = sslOptions;

        this.numConnections = new AtomicInteger(0);

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
     * @param request the request
     */
    public void sendResponse(ONCRPCRecord request) {
        assert(request.getResponseBuffers() != null);
        final ClientConnection connection = request.getConnection();
        if (!connection.isConnectionClosed()) {
            synchronized (connection) {
                boolean isEmpty = connection.getPendingResponses().isEmpty();
                connection.addPendingResponse(request);
                if (isEmpty) {
                    System.out.println("write enabled");
                    SelectionKey key = connection.getChannel().keyFor(selector);
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
            }
            System.out.println("wake-up");
            selector.wakeup();
        } else {
            //ignore and free bufers
            request.freeBuffers();
        }
    }

    public void run() {

        notifyStarted();

        Logging.logMessage(Logging.LEVEL_INFO, this,"ONCRPC Srv "+bindPort+" ready");

        try {
            while (!quit) {
                // try to select events...
                try {
                    if (selector.select() == 0) {
                        continue;
                    }
                } catch (CancelledKeyException ex) {
                    //who cares
                } catch (IOException ex) {
                    Logging.logMessage(Logging.LEVEL_WARN, this, "Exception while selecting: " + ex);
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
                        //nobody cares...
                        continue;
                    }
                }
            }

            //close socket
            selector.close();
            socket.close();

            Logging.logMessage(Logging.LEVEL_INFO, this,"ONCRPC Server "+bindPort+" shutdown complete");

            notifyStopped();
        } catch (Exception thr) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,"ONRPC Server "+bindPort+" CRASHED!");
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,thr);
            }
            notifyCrashed(thr);
        }

    }

    /**
     * read data from a readable connection
     * @param key a readable key
     */
    private void readConnection(SelectionKey key) {
        final ClientConnection con = (ClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {
            while (true) {
                final ByteBuffer fragmentHeader = con.getReceiveFragHdr();
                if (fragmentHeader.hasRemaining()) {
                    //read fragment header
                    final int numBytesRead = readData(key, channel, fragmentHeader);
                    if (numBytesRead == -1) {
                        //connection closed
                        closeConnection(key);
                        return;
                    }
                    if (fragmentHeader.hasRemaining()) {
                        //not enough data...
                        break;
                    } else {
                        //fragment header is complete...
                        fragmentHeader.position(0);
                        final int fragmentHeaderInt = fragmentHeader.getInt();
                        final int fragmentSize = ONCRPCRecordFragmentHeader.getFragmentLength(fragmentHeaderInt);
                        final boolean lastFragment = ONCRPCRecordFragmentHeader.isLastFragment(fragmentHeaderInt);

                        if ((fragmentSize <= 0) || (fragmentSize >= MAX_FRAGMENT_SIZE)) {
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,"invalid fragment size ("+fragmentSize+") received, closing connection");
                            }
                            closeConnection(key);
                            break;
                        }
                        System.out.println("fragHdr "+fragmentHeaderInt);
                        System.out.println("fragment "+fragmentSize);
                        final ReusableBuffer fragment = BufferPool.allocate(fragmentSize);

                        ONCRPCRecord rq = con.getReceive();
                        if (rq == null) {
                            rq = new ONCRPCRecord(this,con);
                            con.setReceive(rq);
                        }
                        rq.addNewRequestFragment(fragment);
                        System.out.println("last fragment: "+lastFragment);
                        rq.setAllFragmentsReceived(lastFragment);
                    }
                } else {
                    final ONCRPCRecord rq = con.getReceive();
                    final ReusableBuffer fragment = rq.getLastRequestFragment();

                    System.out.println("reading fragment: "+fragment.remaining());

                    final int numBytesRead = readData(key, channel, fragment.getBuffer());
                    if (numBytesRead == -1) {
                        //connection closed
                        closeConnection(key);
                        return;
                    }
                    if (fragment.hasRemaining()) {
                        //not enough data...
                        break;
                    } else {
                        //reset fragment header position to read next fragment
                        fragmentHeader.position(0);
                        
                        if (rq.isAllFragmentsReceived()) {
                            con.setReceive(null);
                            //request is complete... send to receiver
                            if (Logging.tracingEnabled()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,rq.toString());
                            }
                            con.getOpenRequests().incrementAndGet();
                            receiveRequest(key,rq);
                        }
                    }
                }
            }
        } catch (ClosedByInterruptException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connection to "+con.getChannel().socket().getRemoteSocketAddress()+" closed by remote peer");
            }
            closeConnection(key);
        } catch (IOException ex) {
            //simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            closeConnection(key);
        }
    }


    /**
     * write data to a writeable connection
     * @param key the writable key
     */
    private void writeConnection(SelectionKey key) {
        final ClientConnection con = (ClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        try {
            while (true) {

                final ByteBuffer fragmentHeader = con.getSendFragHdr();

                ONCRPCRecord rq = con.getSend();
                if (rq == null) {
                    synchronized (con) {
                        rq = con.getPendingResponses().poll();
                        if (rq == null) {
                            //no more responses, stop writing...
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                            break;
                        }
                        con.setSend(rq);
                    }
                    //create fragment header
                    fragmentHeader.position(0);
                    final int fragmentSize = rq.getResponseSize();
                    final boolean isLastFragment = true;
                    final int fragmentHeaderInt = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize, isLastFragment);
                    fragmentHeader.putInt(fragmentHeaderInt);
                    fragmentHeader.position(0);
                }

                

                if (fragmentHeader.hasRemaining()) {
                    final int numBytesWritten = writeData(key, channel, fragmentHeader);
                    if (numBytesWritten == -1) {
                        //connection closed
                        closeConnection(key);
                        return;
                    }
                    if (fragmentHeader.hasRemaining()) {
                        //not enough data...
                        break;
                    }
                    //finished sending... send fragment data now...
                } else {
                    //send fragment data
                    final ReusableBuffer currentBuf = rq.getCurrentResponseBuffer();
                    final int numBytesWritten = writeData(key, channel, currentBuf.getBuffer());
                    if (numBytesWritten == -1) {
                        //connection closed
                        closeConnection(key);
                        return;
                    }
                    if (currentBuf.hasRemaining()) {
                        //not enough data...
                        break;
                    }
                    con.setSendingFragmentHeader(true);
                    //finished sending fragment
                    if (rq.isLastResoponseBuffer()) {
                        //clean up :-) request finished
                        rq.freeBuffers();
                        con.setSend(null);
                        continue;
                    } else {
                        rq.nextResponseBuffer();
                    }

                }
            }
        } catch (ClosedByInterruptException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connection to "+con.getChannel().socket().getRemoteSocketAddress()+" closed by remote peer");
            }
            closeConnection(key);
        } catch (IOException ex) {
            //simply close the connection
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            closeConnection(key);
        }
    }


    /**
     * Reads data from the socket, ensures that SSL connection is ready
     * @param key the SelectionKey
     * @param channel the channel to read from
     * @param buf the buffer to read to
     * @return number of bytes read, -1 on EOF
     * @throws java.io.IOException
     */
    public static int readData(SelectionKey key, ChannelIO channel, ByteBuffer buf) throws IOException {
        if (!channel.isShutdownInProgress()) {
            if (channel.doHandshake(key)) {
                return channel.read(buf);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static int writeData(SelectionKey key, ChannelIO channel, ByteBuffer buf) throws IOException {
        if (!channel.isShutdownInProgress()) {
            if (channel.doHandshake(key)) {
                return channel.write(buf);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * close a connection
     * @param key matching key
     */
    private void closeConnection(SelectionKey key) {
        final ClientConnection con = (ClientConnection) key.attachment();
        final ChannelIO channel = con.getChannel();

        //remove the connection from the selector and close socket
        try {
            con.setConnectionClosed(true);
            key.cancel();
            channel.close();
        } catch (Exception ex) {
        } finally {
            //adjust connection count and make sure buffers are freed
            numConnections.decrementAndGet();
            con.freeBuffers();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "closing connection to " + channel.socket().getRemoteSocketAddress());
        }
    }

    /**
     * accept a new incomming connection
     * @param key the acceptable key
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
                channelIO = new SSLChannelIO(client, sslOptions, false);
            }
            con = new ClientConnection(channelIO);

            // and configure it to be non blocking
            // IMPORTANT!
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, con);
            client.socket().setTcpNoDelay(true);

            numConnections.incrementAndGet();

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connect from client at " + client.socket().getRemoteSocketAddress());
            }

        } catch (ClosedChannelException ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "cannot establish connection: " + ex);
            if (channelIO != null) {
                try {
                    channelIO.close();
                } catch (IOException ex2) {
                }
            }
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "cannot establish connection: " + ex);
            if (channelIO != null) {
                try {
                    channelIO.close();
                } catch (IOException ex2) {
                }
            }
        }
    }

    private void receiveRequest(SelectionKey key, ONCRPCRecord record) {
        try {
            ONCRPCRequest rq = new ONCRPCRequest(record);
            receiver.receiveRecord(rq);
        } catch (BufferUnderflowException ex) {
            //close connection if the header cannot be parsed
            closeConnection(key);
        }
    }
}
