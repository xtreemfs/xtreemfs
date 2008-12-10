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
    but WITHOUT ANY WARRANTY; without even tnhe implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.foundation.speedy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.foundation.pinky.channels.SSLChannelIO;

/**
 * Main (and single) thread for Speedy a client for the Pinky async IO server.
 * This variant is based on PipelinedSpeedy and can handle connections to
 * multiple servers.
 *
 * @author bjko
 */
public class MultiSpeedy extends LifeCycleThread {

    /**
     * Selector for server socket
     */
    Selector selector;

    /**
     * If set to true thei main loop will exit upon next invocation
     */
    boolean quit;

    /**
     * List of all active connections.
     */
    private Map<InetSocketAddress, ConnectionState> connections;

    /**
     * Maximum size of the request queue.
     */
    public static final int MAX_CLIENT_QUEUE = 50000;

    /**
     * registered listeners for responses
     */
    private final Map<InetSocketAddress, SpeedyResponseListener> listeners;

    /**
     * connections that still need to be registered with the selector.
     */
    private final LinkedBlockingQueue<ConnectionState> newCons;

    /**
     * Maxmimum tries to reconnect to the server
     */
    public static final int MAX_RECONNECT = 4;


    /**
     * milliseconds between two timeout checks
     */
    public static final int TIMEOUT_GRANULARITY = 250;

    /**
     * timestamp of last timeout check
     */
    private long lastCheck;

    /**
     * a single listener for all connections if null listeners is used.
     */
    private SpeedyResponseListener singleListener;

    /**
     * options for ssl connection
     */
    private SSLOptions sslOptions;
    
    /**
     * delete idle connections after five minutes.
     */
    public static long CONNECTION_REMOVE_TIMEOUT = 1000*60*5;

    //private ParserThread pThread;
    
    /**
     * Creates a new instance of the Pinky server without SSL
     *
     * @throws java.io.IOException
     *             passes IO Exception when it cannot setup the server socket
     */
    public MultiSpeedy() throws IOException {
    	this(null);
    }

    /**
     * Creates a new instance of the Pinky server
     *
     * @param sslOptions
     *            options for ssl connection, null for no SSL
     * @throws java.io.IOException
     *             passes IO Exception when it cannot setup the server socket
     */
    public MultiSpeedy(SSLOptions sslOptions) throws IOException {

        super("Speedy thr");

        connections = Collections.synchronizedMap(new HashMap());

        Logging.logMessage(Logging.LEVEL_INFO,this,"speedy operational");

        this.newCons = new LinkedBlockingQueue();

        // create a selector
        selector = Selector.open();

        listeners = new HashMap();

        singleListener = null;

        this.sslOptions = sslOptions;

        //pThread = new ParserThread(this);
        //pThread.start();
    }

    /**
     * registers a listener for client requests. Overwrites any prevoiusly
     * installed listener.
     *
     * @param rl
     *            the listener or null to unregister
     */
    public void registerListener(SpeedyResponseListener rl, InetSocketAddress server) {
        if (rl != null)
            this.listeners.put(server, rl);
        else
            this.listeners.remove(server);
    }

    /**
     * registers a listener for all client requests. SpeedyRequest has methods
     * to get server IP. Overwrites any prevoiusly installed listener.
     *
     * @param rl
     *            the listener or null to unregister
     */
    public void registerSingleListener(SpeedyResponseListener rl) {
        this.singleListener = rl;
    }

    /**
     * This method checks only if the server is available according
     * to speedy's settings (i.e. timeout after connection failure).
     * This does not mean that the server can be contacted, it just
     * means that speedy will ttry to connect.
     */
    public boolean serverIsAvailable(InetSocketAddress server) {
        ConnectionState con = connections.get(server);
        if (con != null) {
            return con.serverIsAvailable();
        } else {
            //new servers are always connectable
            return true;
        }
    }

    /**
     * Releases the resources associated with a server. Does not close the
     * connection itself.
     */
    public void releaseConnection(InetSocketAddress server) {
        ConnectionState con = connections.get(server);
        if (con != null) {
            if (con.channel == null) {
                connections.remove(server).freeBuffers();
                listeners.remove(server);
            }
        }
    }

    /**
     * Called to send a response.
     *
     * @attention rq must have a connection attached!
     * @param rq
     *            the request to be sent
     * @throws java.lang.IllegalStateException
     *             if the send queue is full
     * @throws java.io.IOException
     *             passes all exceptions from the used IO primitives
     */
    public void sendRequest(SpeedyRequest rq, InetSocketAddress server)
            throws IOException, IllegalStateException {

        if (rq.listener == null) {
            if (singleListener != null) {
                rq.listener = singleListener;
            } else {
                rq.listener = this.listeners.get(server);
                if (rq.listener == null)
                    throw new RuntimeException("not listener set for "+server);
            }

        }


        ConnectionState con = null;
        synchronized (connections) {
            con = connections.get(server);
            if (con == null) {
                // create a new connection
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"received new request, open new connection to "
                                    + server);
                ChannelIO channel;
                try {
                    if(sslOptions == null){ // no SSL
                            channel = new ChannelIO(SocketChannel.open());
                    } else {
                        channel = new SSLChannelIO(SocketChannel.open(), sslOptions, true);
                    }
                } catch (IOException ex) {
                    System.out.println("\n\nSPEEDY STATUS:");
                    System.out.println(this.getStatus());
                    System.out.println();
                    throw ex;
                }
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(true);
                channel.socket().setReceiveBufferSize(256*1024);
                channel.connect(server);

                con = new ConnectionState(channel);
                con.endpoint = server;
                newCons.add(con);
                connections.put(server, con);

                rq.registerConnection(con);
                rq.status = SpeedyRequest.RequestStatus.PENDING;
                con.sendQ.add(rq);

                Logging.logMessage(Logging.LEVEL_DEBUG,this,"connecting...");
                selector.wakeup();
            } else {
                // recycle old connection

                if (con.conRetries >= this.MAX_RECONNECT) {
                    if (con.canReconnect()) {
                        con.conRetries = 0;
                        con.channel = null;
                        Logging.logMessage(Logging.LEVEL_DEBUG,this,"retry count reset "
                                        + con.endpoint);
                    } else {
                        throw new IOException("Cannot contact server");
                    }
                }
                if (con.channel == null) {
                    Logging.logMessage(Logging.LEVEL_DEBUG,this,"need a reconnect to "
                                        + server);
                    reconnect(con);
                }
                con.lastUsed = TimeSync.getLocalSystemTime();
                rq.registerConnection(con);
                rq.status = SpeedyRequest.RequestStatus.PENDING;
                con.sendQ.add(rq);

                SelectionKey key = con.channel.keyFor(selector);
                if (key != null) {
                    if (key.isValid()) {
                        synchronized (key) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                    } else
                        Logging.logMessage(Logging.LEVEL_WARN,this,"invalid key for "
                                + server);
                } else {
                    Logging.logMessage(Logging.LEVEL_WARN,this,"no key for " + server);
                }

                Logging.logMessage(Logging.LEVEL_DEBUG,this,"received new request, use existing connection to "
                                    + server);
                selector.wakeup();
            }
        }


    }

    public void resetRetryCount(InetSocketAddress server) {
        ConnectionState con = connections.get(server);
        if (con != null) {
            con.conRetries = 0;
        }
    }

    private void checkForTimers() {
        //poor man's timer
        long now = System.currentTimeMillis(); //TimeSync.getLocalSystemTime();
        if (now >= lastCheck + TIMEOUT_GRANULARITY) {
            //check for timed out requests
            synchronized (connections) {
                Iterator<ConnectionState> conIter = connections.values().iterator();
                while (conIter.hasNext()) {
                    ConnectionState con = conIter.next();
                    
                    if (con.lastUsed < (TimeSync.getLocalSystemTime()-CONNECTION_REMOVE_TIMEOUT)) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"removing idle connection from speedy: "+con.endpoint);
                        try {
                            conIter.remove();
                            cancelRequests(con);
                            con.channel.close();
                        } catch(Exception ex) {
                        } finally {
                            con.freeBuffers();
                        }
                    }
                    
                    for (int i = 1; i < 3; i++) {
                        Iterator<SpeedyRequest> iter = (i == 1) ? con.receiveQ.iterator() : con.sendQ.iterator();
                        while (iter.hasNext()) {
                            SpeedyRequest rq = iter.next();
                            if ((rq.status != SpeedyRequest.RequestStatus.FAILED) && (rq.status != SpeedyRequest.RequestStatus.FINISHED)
                                && (rq.timeout > 0)) {
                                rq.waited += TIMEOUT_GRANULARITY;
                                if (rq.waited > rq.timeout) {
                                    try {
                                        Logging.logMessage(Logging.LEVEL_ERROR,this,"request timed out after "+rq.waited+"ms (to was "+rq.timeout+"). KeySet is "+con.channel.keyFor(selector).interestOps()+
                                                "receive wait queue length is "+con.receiveQ.size());
                                    } catch (Exception e) {
                                    }
                                    rq.status = SpeedyRequest.RequestStatus.FAILED;
                                    assert (!rq.listenerNotified);
                                    rq.listener.receiveRequest(rq);
                                    rq.listenerNotified = true;
                                    rq.freeBuffer();
                                    iter.remove();
                                    //if the connection is still open, close it!
                                    try {
                                        rq.con.channel.close();
                                        con.freeBuffers();
                                        conIter.remove();
                                        cancelRequests(rq.con);
                                    } catch (Exception ex2) {
                                        Logging.logMessage(Logging.LEVEL_DEBUG,this,ex2);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            lastCheck = now;
        }
    }

    /**
     * reconnect after an unsuccessfull connect or after connection was closed
     *
     * @param con
     *            The connection to reconnect.
     */
    private void reconnect(ConnectionState con) {
        try {
            // cancel old key
            if (con.channel != null) {
                SelectionKey key = con.channel.keyFor(selector);
                key.cancel();
            }

            Logging.logMessage(Logging.LEVEL_DEBUG,this,"reconnect, open new connection to "
                                + con.endpoint);

            ChannelIO channel;
            if(sslOptions == null){ // no SSL
            	channel = new ChannelIO(SocketChannel.open());
            } else {
                channel = new SSLChannelIO(SocketChannel.open(), sslOptions, true);
            }
            channel.configureBlocking(false);
            channel.socket().setTcpNoDelay(true);
            channel.socket().setReceiveBufferSize(256*1024);
            //try to resolve the address again!
            con.endpoint = new InetSocketAddress(con.endpoint.getHostName(), con.endpoint.getPort());
            channel.connect(con.endpoint);
            this.newCons.add(con);
            con.channel = channel;
            selector.wakeup();
        } catch (SocketException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
        }
    }

    /**
     * Shuts the server down gracefully. All connections are closed.
     */
    public void shutdown() {
        this.quit = true;
        selector.wakeup();
    }

    public void closeConnection(ConnectionState con) {
        try {
            con.channel.close();
            synchronized (connections) {
                connections.remove(con.endpoint).freeBuffers();
            }
            con.channel.keyFor(selector).cancel();
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG,this,ex);
        }
        cancelRequests(con);
      	Logging.logMessage(Logging.LEVEL_DEBUG, this, "connection to " + con.channel.socket().getRemoteSocketAddress() + " closed");
    }

    /**
     * Pinky's main loop
     */
    public void run() {

        try {

            notifyStarted();
            lastCheck = System.currentTimeMillis();

            // repeat until someone shuts the thread down
            while (!quit) {
                // try to select events...
                int numKeys = 0;
                try {
                    numKeys = selector.select(TIMEOUT_GRANULARITY);
                    
                } catch (IOException ex) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                    continue;
                }

                // register new connections w/ selector
                if (!this.newCons.isEmpty()) {
                    ConnectionState cs = this.newCons.poll();
                    try {
                        cs.channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, cs);
                    } catch (ClosedChannelException ex) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                    }
                }

                if (numKeys == 0) {
                    checkForTimers();
                    continue;
                }
                 
                if (quit)
                    break;

                // fetch events
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                // process all events
                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    // remove key from the list
                    iter.remove();

                    ConnectionState con = (ConnectionState) key.attachment();

                    // ACCEPT A CONNECTION
                    if (key.isConnectable()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "is connectable...");

                        ChannelIO client = null;

                        // FIXME: Better exception handling!
                        try {

                            client = con.channel;

                            if (client.isConnectionPending()) {
                                client.finishConnect();
                            }

                            if (!con.sendQ.isEmpty()) {
                                synchronized (key) {
                                    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                                }
                            }

                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "connected to server " + con.endpoint);


                            con.successfulConnect();
                        } catch (Exception ex) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Exception while connecting " + ex);
                            if (con.conRetries < this.MAX_RECONNECT) {
                                Logging.logMessage(Logging.LEVEL_WARN, this, "cannot contact server...retrying");
                                this.reconnect(con);
                            } else {
                                Logging.logMessage(Logging.LEVEL_WARN, this, "cannot contact server(" + con.endpoint + ")! giving up!");
                                cancelRequests(con);
                                con.connectFailed();
                            }
                            con.conRetries++;
                            continue;
                        }

                    }

                    try {
                        // INPUT READY
                        if (key.isReadable()) {
                            // make sure there is an attachment
                            if (con != null) {
                                if (!con.channel.isShutdownInProgress()) {
                                    if (con.channel.doHandshake(key)) {
                                        //System.out.println("read...");
                                        // Q has space
                                        int numread; // num bytes read from Socket

                                        //ReusableBuffer rb = BufferPool.allocate(ConnectionState.BUFFSIZE);

                                        try {
                                            numread = con.channel.read(con.data.getBuffer());
                                        } catch (IOException ex) {
                                            //BufferPool.free(rb);
                                            // read returns -1 when connection was closed
                                            numread = -1;
                                        }

                                        if (numread == -1) {
                                            // connection was closed...
                                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "server closed connection!");

                                            // cancel the key, i.e. deregister from Selector
                                            key.cancel();
                                            synchronized (connections) {
                                                connections.remove(con.endpoint).freeBuffers();
                                            }

                                            try {
                                                con.channel.close();
                                            } catch (IOException ex) {
                                                // no one cares!
                                                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
                                            }

                                            if (!con.sendQ.isEmpty()) {
                                                if (con.canReconnect()) {
                                                    con.conRetries = 0;
                                                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "retry count reset " + con.endpoint);
                                                }
                                                if (con.conRetries < this.MAX_RECONNECT) {
                                                    this.reconnect(con);
                                                } else {
                                                    Logging.logMessage(Logging.LEVEL_WARN, this, "cannot reconnect to server " + con.endpoint);
                                                    // kill requests
                                                    cancelRequests(con);
                                                    con.connectFailed();
                                                }
                                                con.conRetries++;
                                            } else {

                                                // inform client that requests have failed (we
                                                // do not know if we can resend them
                                                // because the server might have received and
                                                // processed them!
                                                cancelRequests(con);

                                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "server closed connection, but it was not needed anymore.");
                                                con.conRetries = 0;
                                                con.channel = null;
                                            }
                                            continue;

                                        }

                                        // so there is new data available
                                        if (numread > 0) {
                                            //pThread.enqueueRequest(con,rb);
                                            try {

                                                // important to find lingering connections
                                                con.active.set(true);

                                                // prepare buffer for reading
                                                con.data.flip();

                                                // the parser may return multiple requests
                                                // because this is async io

                                                while (con.data.hasRemaining()) {
                                                    // as long as there is data we call the
                                                    // parser
                                                    con.processBuffer();

                                                    while (true) {

                                                        if (con.receiveQ.isEmpty()) {
                                                            break;
                                                        }

                                                        SpeedyRequest sr = con.receiveQ.peek();
                                                        if ((sr.status == SpeedyRequest.RequestStatus.FINISHED) || (sr.status == SpeedyRequest.RequestStatus.FAILED)) {
                                                            con.receiveQ.poll();
                                                            sr.received = System.currentTimeMillis();
                                                            
                                                            // here we should notify
                                                            // consumers...
                                                            assert (!sr.listenerNotified);
                                                            sr.listener.receiveRequest(sr);
                                                            sr.listenerNotified = true;

                                                        } else {
                                                            break;
                                                        }
                                                    }

                                                }
                                                // make buffer ready for reading again
                                                con.data.compact();

                                            } catch (SpeedyException ex) {
                                                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                                                if (ex.isAbort()) {
                                                    try {
                                                        con.channel.close();
                                                        synchronized (connections) {
                                                            connections.remove(con.endpoint).freeBuffers();
                                                        }
                                                        key.cancel();
                                                    } catch (Exception ex2) {
                                                    }

                                                }
                                                cancelRequests(con);
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CAN WRITE OUPUT
                        if (key.isWritable()) {
                            // we have a request to send out
                            con.active.set(true);

                            if (!con.channel.isShutdownInProgress()) {
                                if (con.channel.doHandshake(key)) {
                                    boolean rqDone = false;

                                    do {
                                        rqDone = false;
                                        SpeedyRequest toSend = con.sendQ.peek();

                                        if (toSend == null) {
                                            break;
                                        }
                                        // ITERATE as long as a requests finish (buffer is
                                        // not full)
                                        // if we wait for the next select it takes ages and
                                        // the buffer is underfull
                                        if (toSend.status == SpeedyRequest.RequestStatus.PENDING) {
                                            toSend.status = SpeedyRequest.RequestStatus.SENDING;
                                            toSend.sendStart = System.currentTimeMillis();
                                        }

                                        if (toSend.requestHeaders != null) {
                                            con.channel.write(toSend.requestHeaders.getBuffer());
                                            if (!toSend.requestHeaders.hasRemaining() && con.channel.isFlushed()) {
                                                BufferPool.free(toSend.requestHeaders);
                                                toSend.requestHeaders = null;
                                                // if there is no body we can skip the next
                                                // round!
                                                if (toSend.requestBody == null) {
                                                    rqDone = true;
                                                } else {
                                                    toSend.requestBody.position(0);
                                                }
                                            } else {
                                                break;
                                            }
                                        }
                                        if (toSend.requestHeaders == null) {
                                            // headers sent, send body
                                            if (toSend.requestBody == null) {
                                                rqDone = true;
                                            } else {
                                                if (toSend.requestBody.hasRemaining()) {
                                                    con.channel.write(toSend.requestBody.getBuffer());

                                                }
                                                if (!toSend.requestBody.hasRemaining()) {
                                                    BufferPool.free(toSend.requestBody);
                                                    toSend.requestBody = null;
                                                    rqDone = true;
                                                } else {
                                                    break;
                                                }
                                            }
                                        }

                                        if (rqDone) {
                                            
                                            toSend.status = SpeedyRequest.RequestStatus.WAITING;
                                            con.sendQ.poll();
                                            con.receiveQ.add(toSend);

                                            if (con.sendQ.isEmpty()) {
                                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Q empty");
                                                synchronized (key) {
                                                    key.interestOps(SelectionKey.OP_READ);
                                                }
                                            // selector.wakeup();
                                            }

                                        }
                                    } while (rqDone);
                                }
                            }
                            con.active.set(true);
                            continue;
                        }
                    } catch (IOException e) {
                        if (con.channel != null) {
                            con.channel.close();
                        }
                        Logging.logMessage(Logging.LEVEL_ERROR, this, e);
                        con.sendQ.poll();
                    }

                }
            }

            // gracefully shutdown...
            try {
                synchronized (connections) {
                    for (InetSocketAddress endpt : connections.keySet()) {
                        ConnectionState cs = connections.get(endpt);
                        try {
                            if (cs.channel != null) {
                                // TODO: non-blocking shutdown would be better
                                while (!cs.channel.shutdown(cs.channel.keyFor(selector))) {}
                            }
                        } catch (IOException ex) {
                           	Logging.logMessage(Logging.LEVEL_ERROR, this, "Exception when shutdown connection: "+cs.channel.socket().getRemoteSocketAddress()+" "+ex.toString());
                        }finally{
                        	cs.channel.close();
//   							closeConnection(cs);
                            cancelRequests(cs);
                        }
                    }
                }
                selector.close();
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
            }
            //pThread.shutdown();
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "shutdown complete");
            notifyStopped();

        } catch (Throwable th) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, th);
            notifyCrashed(th instanceof Exception ? (Exception) th
                    : new Exception(th));
        }
    }

    void cancelRequests(final ConnectionState con) {
        // kill requests
        for (SpeedyRequest rq : con.sendQ) {
            rq.status = SpeedyRequest.RequestStatus.FAILED;
            assert(!rq.listenerNotified);
            rq.listener.receiveRequest(rq);
            rq.listenerNotified = true;
            rq.freeBuffer();
        }
        for (SpeedyRequest rq : con.receiveQ) {
            if ((rq.status == SpeedyRequest.RequestStatus.PENDING)
                    || (rq.status == SpeedyRequest.RequestStatus.SENDING)
                    || (rq.status == SpeedyRequest.RequestStatus.WAITING)) {
                rq.status = SpeedyRequest.RequestStatus.FAILED;
                assert(!rq.listenerNotified);
                rq.listener.receiveRequest(rq);
                rq.listenerNotified = true;
            }
            rq.freeBuffer();
        }
        con.sendQ.clear();
        con.receiveQ.clear();

        // free the send buffer
        //BufferPool.free(con.data);
    }

    /** Get current queue loads.
     *  @returns an array containing the sum of all pending requests and the number of connections
     */
    public int[] getQLength() {
        synchronized (connections) {
            int totalL = 0;
            for (ConnectionState cs : this.connections.values()) {
                totalL += cs.sendQ.size();
            }
            return new int[]{totalL,this.connections.size()};
        }
    }

    public String getStatus() {
        int[] qs = getQLength();
        String str = "queue length: "+qs[0]+"\n";
        str += "connections: "+qs[1];
        return str;
    }

}
