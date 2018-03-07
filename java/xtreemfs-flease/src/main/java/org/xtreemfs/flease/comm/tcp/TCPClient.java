/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.flease.comm.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class TCPClient {

    private static final long MAX_WAITTIME_MS = 1000 * 60 * 10;

    final Map<InetSocketAddress, ClientConnection> connections;

    final ReadWriteLock conLock;

    final TCPCommunicator server;

    final NIOServer implementation;

    final Timer     closeTimer;

    public TCPClient(int port, InetAddress bindAddr, final NIOServer implementation) throws IOException {
        conLock = new ReentrantReadWriteLock();
        connections = new HashMap();
        this.implementation = implementation;
        NIOServer impl = new NIOServer() {

            public void onAccept(NIOConnection connection) {
                final InetSocketAddress endpt = connection.getEndpoint();
                try {
                    conLock.writeLock().lock();
                    ClientConnection cc = connections.get(endpt);
                    if (cc == null) {
                        cc = new ClientConnection();
                        cc.setConnection(connection);
                        cc.connectSucces();
                        connections.put(endpt,cc);
                    }
                } finally {
                    conLock.writeLock().unlock();
                }
                implementation.onAccept(connection);
            }

            public void onConnect(NIOConnection connection) {
                final InetSocketAddress endpt = connection.getEndpoint();
                try {
                    conLock.writeLock().lock();
                    ClientConnection cc = connections.get(endpt);
                    if (cc != null)
                        cc.connectSucces();
                    else {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this,
                                "connect for unknown connection: " + connection);
                        connection.close();
                    }
                } finally {
                    conLock.writeLock().unlock();
                }
                implementation.onConnect(connection);
            }

            public void onRead(NIOConnection connection, ReusableBuffer buffer) {
                implementation.onRead(connection, buffer);
            }

            public void onClose(NIOConnection connection) {
                final InetSocketAddress endpt = connection.getEndpoint();
                try {
                    conLock.writeLock().lock();
                    connections.remove(endpt);
                } finally {
                    conLock.writeLock().unlock();
                }
                implementation.onClose(connection);
            }

            public void onWriteFailed(IOException exception, Object context) {
                implementation.onWriteFailed(exception, context);
            }

            public void onConnectFailed(InetSocketAddress endpoint, IOException exception, Object context) {
                System.out.println("connect failed for: "+endpoint);
                try {
                    conLock.readLock().lock();
                    ClientConnection cc = connections.get(endpoint);
                    if (cc != null) {
                        synchronized (cc) {
                            cc.connectFailed();
                            cc.setConnection(null);
                        }
                    }
                } finally {
                    conLock.readLock().unlock();
                }
                implementation.onConnectFailed(endpoint,exception,context);
            }
        };

        server = new TCPCommunicator(impl, port, bindAddr);

        closeTimer = new Timer();
        /*closeTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                final long now = TimeSync.getLocalSystemTime();
                try {
                    conLock.writeLock().lock();
                    for (Entry<InetSocketAddress,ClientConnection> e : connections.entrySet()) {
                        if (e.getValue().lastConnectAttempt_ms)

                    }
                } finally {
                    conLock.writeLock().unlock();
                }
            }
        }, CON_TIMEOUT, CON_TIMEOUT);
        */

    }

    public void write(InetSocketAddress remote, ReusableBuffer data, Object context) {
        ClientConnection client = null;
        try {
            conLock.readLock().lock();
            client = connections.get(remote);
        } finally {
            conLock.readLock().unlock();
        }
        if (client == null) {
            try {
                conLock.writeLock().lock();
                client = connections.get(remote);
                if (client == null) {
                    client = new ClientConnection();
                    connections.put(remote, client);
                }
            } finally {
                conLock.writeLock().unlock();
            }
        }
        synchronized (client) {
            try {
                if (!client.isConnected()) {
                    if (client.canReconnect()) {
                        NIOConnection con = server.connect(remote,null);
                        client.setConnection(con);
                    } else {
                        implementation.onWriteFailed(new IOException("cannot connect to server, blocked due to reconnect timeout"), context);
                        return;
                    }
                }
            } catch (IOException ex) {
                implementation.onWriteFailed(ex, context);
                return;
            }
            assert(client.getConnection() != null);
            client.getConnection().write(data, context);
        }
        
       
    }

    public void start() {
        server.start();
    }

    public void waitForStartup() throws Exception {
        server.waitForStartup();
    }

    public void shutdown() {
        server.shutdown();
        closeTimer.cancel();
    }

    public void waitForShutdown() throws Exception {
        server.waitForShutdown();
    }

    public void setLifeCycleListener(LifeCycleListener l) {
        server.setLifeCycleListener(l);
    }

    public int getSendQueueSize() {
        return server.getSendQueueSize();
    }

    private static class ClientConnection {

        NIOConnection connection;

        long lastConnectAttempt_ms;

        long waitTime_ms;

        public ClientConnection() {
            lastConnectAttempt_ms = 0;
            waitTime_ms = 1000;
        }

        public NIOConnection getConnection() {
            return connection;
        }

        public void setConnection(NIOConnection connection) {
            this.connection = connection;
        }

        public boolean canReconnect() {
            return (lastConnectAttempt_ms + waitTime_ms < TimeSync.getLocalSystemTime());
        }

        public boolean isConnected() {
            return (connection != null);
        }

        public void connectSucces() {
            waitTime_ms = 1000;
            lastConnectAttempt_ms = 0;
        }

        public void connectFailed() {
            lastConnectAttempt_ms = TimeSync.getLocalSystemTime();
            waitTime_ms = waitTime_ms * 2;
            if (waitTime_ms > MAX_WAITTIME_MS) {
                waitTime_ms = MAX_WAITTIME_MS;
            }
        }
    }
}
