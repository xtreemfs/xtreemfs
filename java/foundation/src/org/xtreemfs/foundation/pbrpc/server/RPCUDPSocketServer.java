/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.utils.PBRPCDatagramPacket;
import org.xtreemfs.foundation.pbrpc.utils.RecordMarker;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;

import com.google.protobuf.Message;

/**
 *
 * @author bjko
 */
public class RPCUDPSocketServer extends LifeCycleThread implements RPCServerInterface {

    public final int                              port;

    private final DatagramChannel                 channel;

    private final Selector                        selector;

    private volatile boolean                      quit;

    private final LinkedBlockingQueue<UDPMessage> q;

    private final RPCServerRequestListener        receiver;

    public static final int                       MAX_UDP_SIZE = 2048;

    private final AtomicInteger                   callIdCounter;

    public RPCUDPSocketServer(int port, RPCServerRequestListener receiver) throws IOException {
        super("UDPComStage");
        this.port = port;
        q = new LinkedBlockingQueue<UDPMessage>();
        this.receiver = receiver;
        callIdCounter = new AtomicInteger(1);

        selector = Selector.open();

        channel = DatagramChannel.open();
        channel.socket().setReuseAddress(true);
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);

        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "UDP socket on port %d ready", port);
    }

    @Override
    public void sendResponse(RPCServerRequest request, RPCServerResponse response) {
        UDPMessage msg = (UDPMessage)request.getConnection();
        UDPMessage responseMsg = new UDPMessage(response.getBuffers()[0], msg.getAddress(), this);
        request.freeBuffers();
        send(responseMsg);
    }

    public void sendRequest(RPCHeader header, Message message, InetSocketAddress receiver) throws IOException {
        PBRPCDatagramPacket dpack = new PBRPCDatagramPacket(header, message);
        header = header.toBuilder().setCallId(callIdCounter.getAndIncrement()).build();
        UDPMessage msg = new UDPMessage(dpack.assembleDatagramPacket(), receiver, this);
        //msg.getBuffer().flip();
        send(msg);
    }

    private void send(UDPMessage rq) {
        q.add(rq);

        if (q.size() == 1) {
            // System.out.println("wakeup!");
            selector.wakeup();
        }
    }

    @Override
    public void shutdown() {
        quit = true;
        interrupt();
    }

    @Override
    public void run() {

        try {
            notifyStarted();

            boolean isRdOnly = true;

            while (!quit) {

                if (q.size() == 0) {
                    if (!isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
                        // System.out.println("read only");
                        isRdOnly = true;
                    }
                } else {
                    if (isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        // System.out.println("read write");
                        isRdOnly = false;
                    }
                }

                int numKeys = selector.select();

                if (q.size() == 0) {
                    if (!isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
                        // System.out.println("read only");
                        isRdOnly = true;
                    }
                } else {
                    if (isRdOnly) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        // System.out.println("read write");
                        isRdOnly = false;
                    }
                }

                if (numKeys == 0)
                    continue;

                if (q.size() > 10000) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this, "QS!!!!! %d", q.size());
                    Logging.logMessage(Logging.LEVEL_WARN, Category.net, this, "is readOnly: " + isRdOnly);
                }

                // fetch events
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                // process all events
                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    // remove key from the list
                    iter.remove();

                    if (key.isReadable()) {
                        InetSocketAddress sender = null;
                        // do {
                        ReusableBuffer data = BufferPool.allocate(MAX_UDP_SIZE);
                        sender = (InetSocketAddress) channel.receive(data.getBuffer());
                        if (sender == null || !data.hasRemaining()) {
                            BufferPool.free(data);
                            Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                "read key for empty read/empty packet");
                        } else {

                            if (Logging.isDebug())
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "read data from %s", sender.toString());

                            try {
                                data.flip();
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "data: %s",
                                            data.toString());
                                RecordMarker rm = new RecordMarker(data.getBuffer());
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "rm: %d/%d data: %d",
                                            rm.getRpcHeaderLength(), rm.getMessageLength(), data.limit());
                                ReusableBufferInputStream rbis = new ReusableBufferInputStream(data);

                                final int origLimit = data.limit();
                                assert (origLimit == RecordMarker.HDR_SIZE + rm.getRpcHeaderLength()
                                        + rm.getMessageLength());
                                data.limit(RecordMarker.HDR_SIZE + rm.getRpcHeaderLength());

                                RPCHeader header = RPCHeader.newBuilder().mergeFrom(rbis).build();

                                data.range(RecordMarker.HDR_SIZE + rm.getRpcHeaderLength(), rm.getMessageLength());

                                UDPMessage msg = new UDPMessage(null, sender, this);
                                RPCServerRequest rq = new RPCServerRequest(msg, header, data);
                                receiver.receiveRecord(rq);
                            } catch (Throwable ex) {
                                ex.printStackTrace();
                                Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                "received invalid UPD message: "+ex);
                                BufferPool.free(data);
                            }
                        }
                        // } while (sender != null);
                    } else if (key.isWritable()) {
                        UDPMessage r = q.poll();
                        while (r != null) {
                            if (Logging.isDebug())
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                                    "sent packet to %s", r.getAddress().toString());
                            int sent = channel.send(r.getBuffer().getBuffer(), r.getAddress());
                            BufferPool.free(r.getBuffer());
                            if (sent == 0) {
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "cannot send anymore");
                                q.put(r);
                                break;
                            }
                            r = q.poll();
                        }
                    } else {
                        throw new RuntimeException("strange key state: " + key);
                    }
                }

            }

            selector.close();
            channel.close();

        } catch (CancelledKeyException ex) {
            // ignore
        } catch (ClosedByInterruptException ex) {
            // ignore
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        } catch (Throwable th) {
            notifyCrashed(th);
            return;
        }

        notifyStopped();
    }



}
