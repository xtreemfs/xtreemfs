/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.discovery;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.utils.PBRPCDatagramPacket;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.DirService;

/**
 *
 * @author bjko
 */
public class DiscoveryMsgThread extends LifeCycleThread {

    private final DirService me;
    private boolean quit;

    public DiscoveryMsgThread(String address, int port, String protocol) {
        super("DiscovMsgThr");
        me = DirService.newBuilder().setAddress(address).setPort(port).setProtocol(protocol).setInterfaceVersion(10001).build();
        quit = false;
    }

    @Override
    public void run() {

        notifyStarted();

        final ReusableBuffer data = BufferPool.allocate(2048);
        
        try {

            final DatagramChannel channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(me.getPort()));
            channel.configureBlocking(true);

            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.lifecycle, me, "DiscoveryMessageThread started");

            do {

                data.position(0);
                data.limit(data.capacity());

                ReusableBuffer dataOut = null;
                try {
                    InetSocketAddress sender = (InetSocketAddress) channel.receive(data.getBuffer());
                    data.flip();

                    PBRPCDatagramPacket packetIn = new PBRPCDatagramPacket(data, emptyRequest.getDefaultInstance());
                    
                    RPCHeader resp = RPCHeader.newBuilder().setMessageType(MessageType.RPC_RESPONSE_SUCCESS).setCallId(packetIn.getHeader().getCallId()).build();
                    PBRPCDatagramPacket packetOut = new PBRPCDatagramPacket(resp, me);
                    dataOut = packetOut.assembleDatagramPacket();

                    channel.send(dataOut.getBuffer(), sender);

                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.net, this, "responded to UDP dir discover message from %s", sender);
                        
                } catch (Exception ex) {
                    //bad packet
                    continue;
                } finally {
                    if (dataOut != null)
                        BufferPool.free(dataOut);
                }


            } while (!quit);

            channel.close();
            
        } catch (Throwable ex) {
            if (!quit)
                notifyCrashed(ex);
        } finally {
            BufferPool.free(data);
        }
        notifyStopped();
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.lifecycle, me, "DiscoveryMessageThread shutdown complete");

    }

    @Override
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }
}
