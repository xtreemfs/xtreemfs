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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.foundation.pinky;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.foundation.pinky.channels.SSLChannelIO;

/**
 * Main (and single) thread for the Pinky async IO server.
 *
 * @author bjko
 */
public class PipelinedPinky extends LifeCycleThread implements PinkyInterface {

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
    private boolean quit;

    /**
     * List of all active connections.
     */
    private final Queue<ConnectionState> connections;

    /**
     * Number of active connections. Counting in a ConcurrentQ is too expensive.
     */
    private final AtomicInteger numCon;

    /**
     * Cleaner thread that removes lingering connections after timeout.
     */
    private ConnectionRemover crThr;

    /**
     * A listener that receives all requests.
     */
    private PinkyRequestListener receiver;

    /**
     * maximu size of a client pipeline
     */
    public static int MAX_CLIENT_QUEUE = 20000;

    /**
     * if the Q was full we need at least
     * CLIENT_Q_THR spaces before we start
     * reading from the client again.
     * This is to prevent it from oscillating
     */
    public static int CLIENT_Q_THR = 5000;

    /**
     * options for ssl connection
     */
    private SSLOptions sslOptions;

    public static final int CONNECTION_REM_INTERVAL = 1000 * 60;

    /**
     * Creates a new instance of the Pinky server without a secure channel
     *
     * @param bindPort
     *            port to bind the server socket to
     * @param bindAddr
     *            device address to bind the server socket to (null = any)
     * @param rl
     *                    the listener to use or null if it will be specified later
     * @throws java.io.IOException
     *             passes IO Exception when it cannot setup the server socket
     */
    public PipelinedPinky(int bindPort, InetAddress bindAddr, PinkyRequestListener rl) throws IOException {
        this(bindPort, bindAddr, rl, null);
    }

    /**
     * Creates a new instance of the Pinky server
     *
     * @param bindPort
     *            port to bind the server socket to
     * @param rl
     *                    the listener to use or null if it will be specified later
     * @param sslOptions
     *            options for ssl connection, null for no SSL
     * @throws java.io.IOException
     *             passes IO Exception when it cannot setup the server socket
     */
    public PipelinedPinky(int bindPort, InetAddress bindAddr, PinkyRequestListener rl, SSLOptions sslOptions) throws IOException {

        super("Pinky thr." + bindPort);
        this.numCon = new AtomicInteger(0);

        connections = new ConcurrentLinkedQueue<ConnectionState>();

        // open server socket
        socket = ServerSocketChannel.open();
        socket.configureBlocking(false);
        socket.socket().setReceiveBufferSize(256 * 1024);
        socket.socket().setReuseAddress(true);
        socket.socket().bind(bindAddr == null ? new InetSocketAddress(bindPort) : new InetSocketAddress(bindAddr, bindPort));

        // create a selector and register socket
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_ACCEPT);

        // server is ready to accept connections now

        receiver = rl;

        this.sslOptions = sslOptions;
    }

    public void start() {

        // start helper threads
        crThr = new ConnectionRemover(connections, this.numCon, selector, CONNECTION_REM_INTERVAL);
        crThr.start();

        super.start();
    }

    /**
     * DOES NOT REALLY WORK AT THE MOMENT! DO NOT USE!
     *
     * @param conn
     */
    public void releaseConnection(ConnectionState conn) {
        connections.remove(conn);
        SelectionKey conKey = conn.channel.keyFor(selector);
        conKey.cancel();
        selector.wakeup();
    }

    public int getNumConnections() {
        return this.numCon.get();
    }

    public int getTotalQLength() {
        int total = 0;
        for (ConnectionState cs : connections) {
            total += cs.pipeline.size();
        }
        return total;
    }

    /**
     * DOES NOT REALLY WORK AT THE MOMENT! DO NOT USE!
     */
    public void returnConnection(ConnectionState conn) throws IOException {
        try {
            conn.active.set(true);
            if (!connections.contains(conn)) {
                conn.channel.configureBlocking(false);
                conn.channel.register(selector, SelectionKey.OP_READ, conn);
            }
        } catch (ClosedChannelException ex) {
            throw new IOException("Cannot return connection because channel is closed!");
        } catch (IOException ex) {
            throw ex;
        }
        connections.add(conn);
        selector.wakeup();
    }

    /**
     * Called to send a response.
     *
     * @attention rq must have a connection attached!
     * @param rq
     *            the request to be sent
     * @throws java.io.IOException
     *             passes all exceptions from the used IO primitives
     */
    public void sendResponse(PinkyRequest rq) {
        assert (rq != null) : "Request must not be null";
        assert (rq.client != null) : "Request is not associated with a client connection!";
        assert (rq.client.channel != null) : "Client connection has no channel!";
        assert (rq.responseSet) : "no response set for request, cannot send!";

        SelectionKey key = rq.client.channel.keyFor(this.selector);

        if (key == null) {
            //throw new RuntimeException("SelectionKey for client is null?!?!");
            //the client disconnected while process
            rq.ready = true;

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "sendResponse for disconnected client");

            return;
        }
        try {

            synchronized (this) {
                rq.ready = true;
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
            // make sure we can send it away!
            selector.wakeup();
        } catch (CancelledKeyException e) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "sendResponse for disconnected client");
            return;
        }
    }

    /**
     * registers a listener for client requests. Overwrites any prevoiusly
     * installed listener.
     *
     * @param rl
     *            the listener or null to unregister
     */
    public void registerListener(PinkyRequestListener rl) {
        this.receiver = rl;
    }

    /**
     * Shuts the server down gracefully. All connections are closed.
     */
    public void shutdown() {
        try {
            this.quit = true;
            crThr.quitThread();
            crThr.join();
            selector.wakeup();
        } catch (InterruptedException exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        }
    }

    public void restartReading() {
        for (ConnectionState cs : connections) {
            try {
                SelectionKey key = cs.channel.keyFor(selector);
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            } catch (CancelledKeyException ex) {
                // don't care
                Logging.logMessage(Logging.LEVEL_WARN, this, ex);
            }
        }
        selector.wakeup();

    }

    /**
     * Pinky's main loop
     */
    public void run() {

        try {

            // to ease debugging
            Logging.logMessage(Logging.LEVEL_INFO, this, ((sslOptions != null) ? "SSL enabled " : "") + " pinky operational");
            notifyStarted();

            // repeat until someone shuts the thread down
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
                        // ACCEPT A CONNECTION
                        if (key.isAcceptable()) {

                            SocketChannel client = null;
                            ConnectionState con = null;
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
                                con = new ConnectionState(channelIO);

                                // and configure it to be non blocking
                                // IMPORTANT!
                                client.configureBlocking(false);
                                client.register(selector, SelectionKey.OP_READ, con);
                                client.socket().setTcpNoDelay(true);

                                numCon.incrementAndGet();
                                
                                // this is used to hold the state and buffer for each
                                // connection
                                connections.add(con);

                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connect from client at " + client.socket().getRemoteSocketAddress());

                            } catch (ClosedChannelException ex) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "cannot establish connection: " + ex);
                                if (channelIO != null) {
                                    channelIO.close();
                                }
                                continue;
                            } catch (IOException ex) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "cannot establish connection: " + ex);
                                if (channelIO != null) {
                                    channelIO.close();
                                }
                                continue;
                            } 
                        }

                        try {
                            // INPUT READY
                            if (key.isReadable()) {
                                ConnectionState con = (ConnectionState) key.attachment();

                                // make sure there is an attachment
                                if (con != null) {
                                    if (!con.channel.isShutdownInProgress()) {
                                        if (con.channel.doHandshake(key)) {
                                            if (con.pipeline.size() > MAX_CLIENT_QUEUE) {
                                                // client Q is full
                                                if (!con.channel.socket().isOutputShutdown()) {
                                                    Logging.logMessage(Logging.LEVEL_ERROR, this, "Q full, stop reading " + con.channel.socket().getRemoteSocketAddress());

                                                    //we stop reading and thus delay the client
                                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                                                //continue, because it is probably writable and we
                                                //want to write it out...
                                                }
                                            } else {
                                                // Q has space
                                                int numread; // num bytes read from Socket

                                                try {
                                                    numread = con.channel.read(con.data.getBuffer());
                                                } catch (IOException ex) {
                                                    // read returns -1 when connection was closed
                                                    numread = -1;
                                                }


                                                if (numread == -1) {
                                                    // connection was closed...
                                                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "client deconnected " + con.channel.socket().getRemoteSocketAddress());

                                                    // cancel the key, i.e. deregister from Selector
                                                    key.cancel();
                                                    connections.remove(con);

                                                    numCon.decrementAndGet();

                                                    try {
                                                        con.channel.close();
                                                    } catch (IOException ex) {
                                                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "exception while closing channel: " + ex);
                                                    // no one cares!
                                                    }
                                                    con.freeBuffers();
                                                    // continue because key is cancelled!
                                                    continue;
                                                }

                                                // so there is new data available
                                                if (numread > 0) {

                                                    // important to find lingering connections
                                                    con.active.set(true);
                                                    // channel.read(con.data);

                                                    // prepare buffer for reading
                                                    con.data.flip();

                                                    // the parser may return multiple requests
                                                    // because this is async io
                                                    List<PinkyRequest> requests = new LinkedList<PinkyRequest>();

                                                    while (con.data.hasRemaining()) {
                                                        // as long as there is data we call the
                                                        // parser
                                                        PinkyRequest rq = con.processBuffer();

                                                        // null means there is no request
                                                        // complete
                                                        if (rq != null) {
                                                            //rq._receiveTime = System.nanoTime();
                                                            requests.add(rq);
                                                        }

                                                    }
                                                    // make buffer ready for reading again
                                                    con.data.compact();

                                                    if (receiver != null) {
                                                        // hand over requests for processing
                                                        for (PinkyRequest rq : requests) {
                                                            if (rq.statusCode == HTTPUtils.SC_OKAY) {
                                                                receiver.receiveRequest(rq);
                                                            } else {
                                                                this.sendResponse(rq);
                                                            }
                                                        }
                                                    }

                                                }
                                            }

                                        }
                                    }
                                }
                            }

                            // CAN WRITE OUPUT
                            if (key.isWritable()) {
                                ConnectionState con = (ConnectionState) key.attachment();

                                con.active.set(true);
                                if (!con.channel.isShutdownInProgress()) {
                                    if (con.channel.doHandshake(key)) {
                                        boolean rqDone;
                                        int numSent = 0;
                                        do {
                                            rqDone = false;

                                            if (con.toSend == null) {
                                                synchronized (this) {
                                                    if (con.pipeline.size() > 0) {
                                                        if (con.pipeline.get(0).ready) {
                                                            con.toSend = con.pipeline.get(0);
                                                            con.toSend.responseHeaders.position(0);
                                                            con.remainingBytes = ((con.toSend.responseBody != null) ? con.toSend.responseBody.capacity() : 0) +
                                                                    con.toSend.responseHeaders.capacity();
                                                            if (con.toSend.responseBody == null) {
                                                                con.sendData = new ByteBuffer[]{con.toSend.responseHeaders.getBuffer()};
                                                            } else {
                                                                con.toSend.responseBody.position(0);
                                                                con.sendData = new ByteBuffer[]{con.toSend.responseHeaders.getBuffer(),
                                                                    con.toSend.responseBody.getBuffer()
                                                                };
                                                            }
                                                        } else {
                                                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                                            break;
                                                        }
                                                    } else {
                                                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                                        break;
                                                    }
                                                }
                                            }

                                            //System.out.println("[ I | PipelinedPinky ] sent in loop : "+(numSent++));


                                                con.remainingBytes -= con.channel.write(con.sendData);
                                                assert con.remainingBytes >= 0;
                                                if (con.remainingBytes == 0 && con.channel.isFlushed()) {
                                                    //System.out.println("NEXT REQUEST!");
                                                    rqDone = true;
                                                } else {
                                                    //System.out.println("WAIT FOR NEXT BUFFER!");
                                                    break;
                                                }

                                            /*if (con.toSend.responseHeaders != null) {
                                            con.channel.write(con.toSend.responseHeaders.getBuffer());
                                            if (!con.toSend.responseHeaders.hasRemaining()) {
                                            BufferPool.free(con.toSend.responseHeaders);
                                            con.toSend.responseHeaders = null;
                                            // if there is no body we can skip the
                                            // next round!
                                            if (con.toSend.responseBody != null) {
                                            con.toSend.responseBody.position(0);
                                            }
                                            } else {
                                            // skip loop because buffer is full
                                            break;
                                            }
                                            }
                                            if (con.toSend.responseHeaders == null) {
                                            // headers sent, send body
                                            if (con.toSend.responseBody == null) {
                                            rqDone = true;
                                            } else {
                                            if (con.toSend.responseBody.hasRemaining()) {
                                            con.channel.write(con.toSend.responseBody.getBuffer());
                                            }
                                            if (!con.toSend.responseBody.hasRemaining()) {
                                            rqDone = true;
                                            } else {
                                            // skip loop because buffer is full
                                            break;
                                            }
                                            }
                                            }*/

                                            if (rqDone) {
                                                // close or fetch next request...
                                                if (con.toSend.closeConnection) {
                                                    if (con.channel.shutdown(key)) {
                                                        closeConnection(key, con);
                                                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "connection to " + con.channel.socket().getRemoteSocketAddress() + " closed");
                                                        break;
                                                    }
                                                } else {
                                                    //this is the requeue for "streaming"
                                                    if (con.toSend.streaming) {
                                                        PinkyRequest rq = con.toSend;
                                                        BufferPool.free(rq.responseBody);
                                                        BufferPool.free(rq.responseHeaders);
                                                        con.sendData = null;
                                                        con.toSend = null;
                                                        //notify the request listener (requeue)
                                                        receiver.receiveRequest(rq);
                                                        // do not continue with the rest...
                                                        break;
                                                    }

                                                    /*if (con.toSend._receiveTime > 0) {
                                                        long dur = System.nanoTime()-con.toSend._receiveTime;
                                                        System.out.print("[ "+con.toSend.requestMethod+","+con.toSend.requestURI+" "+((double)dur)/1e6+"ms] ");
                                                    }*/

                                                    con.toSend.freeBuffer();
                                                    con.toSend = null;
                                                    // we can do this safely because
                                                    // add/remove occur only in this thread
                                                    con.pipeline.remove(0);

                                                    //if we're not interested in READ the client Q
                                                    //was full and we have to check, if there
                                                    //is space now. We have to do it here because
                                                    //break further down could destroy everything
                                                    if ((key.interestOps() & SelectionKey.OP_READ) == 0) {
                                                        if (con.pipeline.size() < (MAX_CLIENT_QUEUE - CLIENT_Q_THR)) {
                                                            //read from client again
                                                            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                                            Logging.logMessage(Logging.LEVEL_ERROR, this, "Q for " + con.channel.socket().getRemoteSocketAddress() + " is ready again");
                                                        }
                                                    }

                                                    if (con.pipeline.size() > 0) {
                                                        if (con.pipeline.get(0).ready) {
                                                            // fetch next Q item
                                                            //con.toSend = con.pipeline.get(0);
                                                            //System.out.println("next request fetched!");
                                                            continue;
                                                        } else {
                                                            break;
                                                        }
                                                    } else {
                                                        // Q empty, no more writables!

                                                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                                        break;
                                                    }

                                                }
                                            }
                                        } while (rqDone);
                                    }
                                } else {
                                    if (con.channel.shutdown(key)) {
                                        closeConnection(key, con);
                                    }
                                }
                                continue;
                            }
                        } catch (IOException ex) {
                            try {
                                ConnectionState con = (ConnectionState) key.attachment();
                                Logging.logMessage(Logging.LEVEL_INFO, this, "connection to " + con.channel.socket().getRemoteSocketAddress() + " broke:" + ex);
                                ex.printStackTrace();
                                //throw away everything and close connection!
                                closeConnection(key, con);

                            } catch (IOException ex2) {
                                Logging.logMessage(Logging.LEVEL_ERROR, this, "cannot close connection due to " + ex2);
                            //cannot do anything here -> ignore!
                            }
                        }
                    } catch (CancelledKeyException ex) {
                        Logging.logMessage(Logging.LEVEL_WARN, this,"key has been canceled: "+ex);
                    }

                }
            }

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "initiating gracefull shutdown...");
            // gracefully shutdown...
            try {
                for (ConnectionState cs : connections) {
                    try {
                        cs.freeBuffers();
                        // TODO: non-blocking shutdown would be better
                        while (!(cs.channel.shutdown(cs.channel.keyFor(selector)))) {
                        }
                    } catch (IOException ex) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "Exception when shutdown connection: " + cs.channel.socket().getRemoteSocketAddress() + " " + ex.toString());
                    } finally {
                        try {
                            closeConnection(cs.channel.keyFor(selector), cs);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                selector.close();
                socket.close();
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                return;
            }
            Logging.logMessage(Logging.LEVEL_INFO, this, "shutdown complete");
            notifyStopped();
        } catch (OutOfMemoryError ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
            Logging.logMessage(Logging.LEVEL_ERROR, this, BufferPool.getStatus());
            notifyCrashed(new Exception(ex));
        } catch (Throwable th) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, th);
            notifyCrashed(th instanceof Exception ? (Exception) th
                    : new Exception(th));
        }
    }

    void closeConnection(SelectionKey key, ConnectionState con) throws IOException {
        numCon.decrementAndGet();
        con.channel.close();
        con.active.set(false);
        con.requestHeaders = null;
        key.cancel();
        con.toSend = null;
        con.freeBuffers();
        connections.remove(con);
        selector.wakeup();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "connection to " + con.channel.socket().getRemoteSocketAddress() + " closed");
    }

    public SSLOptions getSSLOptions() {
        return this.sslOptions;
    }
}
