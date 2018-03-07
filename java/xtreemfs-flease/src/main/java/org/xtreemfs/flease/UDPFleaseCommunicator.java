/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.flease;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class UDPFleaseCommunicator extends LifeCycleThread implements FleaseMessageSenderInterface {
    
    private final FleaseStage                     stage;

    private final int                             port;

    private DatagramChannel                       channel;

    private Selector                              selector;

    private volatile boolean                      quit;

    private final AtomicBoolean                   sendMode;

    private final LinkedBlockingQueue<FleaseMessage> q;

    private static final int                      MAX_UDP_SIZE  = 16*1024;

    private long numTx,numRx;

    public UDPFleaseCommunicator(FleaseConfig config, String lockfileDir,
            boolean ignoreLockForTesting,
            final FleaseViewChangeListenerInterface viewListener) throws Exception {
        super("FlUDPCom");
        stage = new FleaseStage(config, lockfileDir, this, ignoreLockForTesting, viewListener, null, null);
        port = config.getEndpoint().getPort();
        q = new LinkedBlockingQueue<FleaseMessage>();
        sendMode = new AtomicBoolean(false);
        numTx = 0;
        numRx = 0;
    }

    public FleaseStage getStage() {
        return stage;
    }


    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
        FleaseMessage m = message.clone();
        m.setSender(recipient);
        send(m);
    }

    /**
     * sends a UDPRequest.
     *
     * @attention Overwrites the first byte of rq.data with the message type.
     */
    public void send(FleaseMessage rq) {
        q.add(rq);

        if (q.size() == 1) {
            //System.out.println("wakeup!");
            selector.wakeup();
        }
    }

    public void shutdown() {
        quit = true;
        interrupt();
    }

    @Override
    public void run() {

        long numRxCycles = 0;
        long durAllRx = 0;
        long maxPkgCycle = 0;
        long avgPkgCycle = 0;

        try {


            selector = Selector.open();

            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(port));
            channel.socket().setReceiveBufferSize(1024*1024*1024);
            channel.socket().setSendBufferSize(1024*1024*1024);
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,"sendbuffer size: "+channel.socket().getSendBufferSize());
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,"recv       size: "+channel.socket().getReceiveBufferSize());
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "UDP socket on port %d ready",
                    port);

            stage.start();
            stage.waitForStartup();


            notifyStarted();

           


            boolean isRdOnly = true;

            List<FleaseMessage> sendList = new ArrayList(5000);
            ReusableBuffer data = BufferPool.allocate(MAX_UDP_SIZE);

            while (!quit) {

                if (q.size() == 0) {
                    if (!isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
                        //System.out.println("read only");
                        isRdOnly = true;
                    }
                } else {
                    if (isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        //System.out.println("read write");
                        isRdOnly = false;
                    }
                }

                int numKeys = selector.select();

                if (q.size() == 0) {
                    if (!isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
                        //System.out.println("read only");
                        isRdOnly = true;
                    }
                } else {
                    if (isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        //System.out.println("read write");
                        isRdOnly = false;
                    }
                }

                if (numKeys == 0)
                    continue;

                if (q.size() > 10000) {
                    System.out.println("QS!!!!! " + q.size());
                    System.out.println("is readOnly: " + isRdOnly);
                }

                // fetch events
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                // process all events
                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    // remove key from the list
                    iter.remove();

                    if (key.isWritable()) {
                        q.drainTo(sendList,50);
                        //System.out.println("sent: "+queue.size());
                        while (!sendList.isEmpty()) {
                            FleaseMessage r = sendList.remove(sendList.size()-1);
                            if (r == null)
                                break;
                            if (Logging.isDebug())
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "sent packet to %s", r.getSender().toString());
                            data.clear();
                            r.serialize(data);
                            data.flip();
                            int sent = channel.send(data.getBuffer(), r.getSender());
                            if (sent == 0) {
                                System.out.println("cannot send anymore!");
                                q.addAll(sendList);
                                sendList.clear();
                                break;
                            }
                            numTx++;
                        }
                    }
                    if (key.isReadable()) {
                        InetSocketAddress sender = null;

                        int numRxInCycle = 0;
                        do {
                            data.clear();
                            sender = (InetSocketAddress) channel.receive(data.getBuffer());
                            if (sender == null) {
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                        "read key for empty read");
                                break;
                            } else {
                                numRxInCycle++;
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                        "read data from %s", sender.toString());

                                try {
                                    //unpack flease message
                                    data.flip();
                                    FleaseMessage m = new FleaseMessage(data);
                                    m.setSender(sender);
                                    numRx++;
                                    stage.receiveMessage(m);
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                    "received invalid UPD message: "+ex);
                                }
                            }
                        } while (sender != null);
                        numRxCycles++;
                        avgPkgCycle += numRxInCycle;
                        if (numRxInCycle > maxPkgCycle)
                            maxPkgCycle = numRxInCycle;
                    }
                }

            }

            stage.shutdown();

            selector.close();
            channel.close();

        } catch (ClosedByInterruptException ex) {
            // ignore
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        } catch (Throwable th) {
            notifyCrashed(th);
            return;
        }


        Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this,"num packets tranferred: %d tx    %d rx",numTx,numRx);
        Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this,"numRxCycles %d, maxPkgPerCycle %d, avg/Cycle %d",numRxCycles,maxPkgCycle,avgPkgCycle/numRxCycles);

        notifyStopped();
    }
}
