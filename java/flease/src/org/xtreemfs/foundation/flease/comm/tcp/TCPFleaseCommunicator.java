/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease.comm.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.MasterEpochHandlerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class TCPFleaseCommunicator implements FleaseMessageSenderInterface {
    
    private final FleaseStage                     stage;

    private final int                             port;

    private TCPClient                             comm;

    private volatile boolean                      quit;

    private final AtomicBoolean                   sendMode;

    private final LinkedBlockingQueue<FleaseMessage> q;

    private static final int                      MAX_UDP_SIZE  = 16*1024;

    private long numTx,numRx;

    public static TCPFleaseCommunicator           instance;

    private AtomicInteger                         numIn, numOut;

    public TCPFleaseCommunicator(FleaseConfig config, String lockfileDir,
            boolean ignoreLockForTesting,
            final FleaseViewChangeListenerInterface viewListener, FleaseStatusListener fsl, MasterEpochHandlerInterface meHandler) throws Exception {
        stage = new FleaseStage(config, lockfileDir, this, ignoreLockForTesting, viewListener,fsl,meHandler);
        port = config.getEndpoint().getPort();
        q = new LinkedBlockingQueue<FleaseMessage>();
        sendMode = new AtomicBoolean(false);
        numTx = 0;
        numRx = 0;
        numIn = new AtomicInteger();
        numOut = new AtomicInteger();
        instance  = this; //only for testing!
        comm = new TCPClient(port, null, new NIOServer() {

            public void onAccept(NIOConnection connection) {
                ReusableBuffer hdr = BufferPool.allocate(4);
                hdr.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
                ReusableBuffer bdy = BufferPool.allocate(2048);
                bdy.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
                Connection c = new Connection(hdr, bdy);
                connection.setContext(c);
                connection.read(hdr);
            }

            public void onConnect(NIOConnection connection) {
                onAccept(connection);
            }

            public void onRead(NIOConnection connection, ReusableBuffer buffer) {
                Connection c = (Connection) connection.getContext();
                try {
                    if (c.readingHdr) {
                        if (buffer.hasRemaining()) {
                            connection.read(buffer);
                        } else {
                            buffer.flip();
                            c.readingHdr = false;
                            int size = buffer.getInt();
                            if ((size <= 0) || (size > 2048)) {
                                Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this,
                                        "warning: invalid fragment size: %d", size);
                                connection.close();
                                return;
                            }
                            buffer.clear();
                            c.data.limit(size);
                            connection.read(c.data);
                        }
                    } else {
                        if (buffer.hasRemaining()) {
                            connection.read(buffer);
                        } else {
                            buffer.flip();
                            FleaseMessage m = new FleaseMessage(buffer);
                            m.setSender(connection.getEndpoint());
                            buffer.clear();
                            c.readingHdr = true;
                            connection.read(c.header);
                            stage.receiveMessage(m);
                        }
                    }
                } catch (Exception ex) {
                    Logging.logError(Logging.LEVEL_ERROR, this,ex);
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this, buffer.toString());
                    connection.close();
                }

            }

            public void onClose(NIOConnection connection) {
                Connection c = (Connection) connection.getContext();
                if (c.readingHdr)
                    BufferPool.free(c.data);
                else
                    BufferPool.free(c.header);
               
            }

            public void onWriteFailed(IOException exception, Object context) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this, "write failed: " + context);
            }

            public void onConnectFailed(InetSocketAddress endpoint, IOException exception, Object context) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this, "could not connect to: " + endpoint);
            }
        });

        /*Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {

            long maxQsize = 0;
            long sum = 0;
            long numSamples = 0;
            int cnt = 0;

            @Override
            public void run() {
                long size = comm.getSendQueueSize();
                sum = sum+size;
                numSamples++;
                if (size > maxQsize)
                    maxQsize = size;
                cnt++;
                if (cnt == 10) {
                    System.out.println("avg: "+(sum/numSamples)+" max: "+maxQsize);
                    cnt = 0;
                }
            }
        }, 10, 1000);*/
    }

    public FleaseStage getStage() {
        return stage;
    }


    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
        FleaseMessage m = message.clone();
        m.setSender(recipient);
        send(m);
        
        if (FleaseStage.COLLECT_STATISTICS)
            numOut.incrementAndGet();
    }

    /**
     * sends a UDPRequest.
     *
     * @attention Overwrites the first byte of rq.data with the message type.
     */
    public void send(FleaseMessage rq) {
        if (FleaseStage.COLLECT_STATISTICS)
            numIn.incrementAndGet();

        final int size = rq.getSize();
        ReusableBuffer data = BufferPool.allocate(size+4);
        data.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        data.putInt(size);
        rq.serialize(data);
        data.getBuffer().order(ByteOrder.BIG_ENDIAN);
        data.flip();
        if (data.remaining() != size+4)
            throw new IllegalStateException("data is wrong: "+data);
        comm.write(rq.getSender(), data, null);
    }

    public void start() throws Exception {
        comm.start();
        comm.waitForStartup();
        stage.start();
        stage.waitForStartup();
    }

    public void shutdown() throws Exception {
        stage.shutdown();
        stage.waitForShutdown();
        comm.shutdown();
        comm.waitForShutdown();
    }

    public void setLifeCycleListener(LifeCycleListener l) {
        comm.setLifeCycleListener(l);
        stage.setLifeCycleListener(l);
    }

    public int getNumIn() {
        return numIn.getAndSet(0);
    }

    public int getNumOut() {
        return numOut.getAndSet(0);
    }

    private static class Connection {
        public ReusableBuffer header;
        public ReusableBuffer data;
        public boolean        readingHdr;

        public Connection(ReusableBuffer header, ReusableBuffer data) {
            this.header = header;
            this.data = data;
            readingHdr = true;
        }

    }

}
