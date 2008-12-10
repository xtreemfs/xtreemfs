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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;

/**
 *
 * @author bjko
 */
public class UDPCommunicator extends LifeCycleThread {

    public final int         port;

    private DatagramChannel  channel;

    private Selector         selector;

    private volatile boolean quit;

    private final AtomicBoolean sendMode;

    private final LinkedBlockingQueue<UDPRequest> q;

    private final UDPReceiverInterface receiver;

    public static final int MAX_UDP_SIZE = 1024;

    public UDPCommunicator(int port, UDPReceiverInterface receiver) {
        super("UDPComStage");
        this.port = port;
        q = new LinkedBlockingQueue<UDPRequest>();
        sendMode = new AtomicBoolean(false);
        this.receiver = receiver;
    }

    /**
     * sends a UDPRequest.
     * @attention Overwrites the first byte of rq.data with the message type.
     */
    public void send(ReusableBuffer data, InetSocketAddress receiver) {
        UDPRequest rq = new UDPRequest();
        rq.address = receiver;
        rq.data = data;
        data.position(0);
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

        try {

            selector = Selector.open();

            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            Logging.logMessage(Logging.LEVEL_INFO,this,"UDP socket on port "+port+" ready");

            notifyStarted();

            boolean isRdOnly = true;

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
                    System.out.println("QS!!!!! "+q.size());
                    System.out.println("is readOnly: "+isRdOnly);
                }

                // fetch events
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                // process all events
                while(iter.hasNext()) {

                    SelectionKey key = iter.next();

                    // remove key from the list
                    iter.remove();

                    if (key.isReadable()) {
                        InetSocketAddress sender = null;
                        //do {
                            ReusableBuffer data = BufferPool.allocate(MAX_UDP_SIZE);
                            sender = (InetSocketAddress) channel.receive(data.getBuffer());
                            if (sender == null) {
                                BufferPool.free(data);
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_WARN,this,"read key for empty read");
                            } else {

                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG,this,"read data from "+sender);

                                receiver.receiveUDP(data,sender);
                            }
                        //} while (sender != null);
                    } else if (key.isWritable()) {
                        UDPRequest r = q.poll();
                        while (r != null) {
                            if (Logging.isDebug())
                                Logging.logMessage(Logging.LEVEL_WARN,this,"sent packet to "+r.address);
                            int sent = channel.send(r.data.getBuffer(),r.address);
                            BufferPool.free(r.data);
                            if (sent == 0) {
                                //System.out.println("cannot send anymore!");
                                q.put(r);
                                break;
                            }
                            r = q.poll();
                        }
                    } else {
                        throw new RuntimeException("strange key state: "+key);
                    }
                }

            }

            selector.close();
            channel.close();

        } catch(ClosedByInterruptException ex) {
            // ignore
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
        } catch (Throwable th) {
            notifyCrashed(th instanceof Exception ? (Exception) th
                : new Exception(th));
            return;
        }

        notifyStopped();
    }

    private static final class UDPRequest {
        public InetSocketAddress address;
        public ReusableBuffer data;
    }


}
