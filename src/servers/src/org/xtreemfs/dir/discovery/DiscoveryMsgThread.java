/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.dir.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_discover_dirRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_discover_dirResponse;
import org.xtreemfs.interfaces.DirService;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

/**
 *
 * @author bjko
 */
public class DiscoveryMsgThread extends LifeCycleThread {

    private final DirService me;
    private boolean quit;

    public DiscoveryMsgThread(String address, int port, String protocol) {
        super("DiscovMsgThr");
        me = new DirService(address, port, protocol, DIRInterface.getVersion());
        quit = false;
    }

    public void run() {

        notifyStarted();

        try {

            final DatagramChannel channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(me.getPort()));
            channel.configureBlocking(true);

            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.lifecycle, me, "DiscoveryMessageThread started");

            final ReusableBuffer data = BufferPool.allocate(2048);

            do {

                data.position(0);
                data.limit(data.capacity());

                InetSocketAddress sender = (InetSocketAddress) channel.receive(data.getBuffer());

                try {
                    data.position(Integer.SIZE / 8);
                    ONCRPCRequestHeader hdr = new ONCRPCRequestHeader(null);
                    hdr.unmarshal(new XDRUnmarshaller(data));
                    xtreemfs_discover_dirRequest rq = new xtreemfs_discover_dirRequest();
                    rq.unmarshal(new XDRUnmarshaller(data));


                    xtreemfs_discover_dirResponse resp = new xtreemfs_discover_dirResponse(me);
                    ONCRPCResponseHeader respHdr = new ONCRPCResponseHeader(hdr.getXID(),
                            ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED, 
                            ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);
                
                    ONCRPCBufferWriter wr = new ONCRPCBufferWriter(2048);
                    wr.writeInt32(null,ONCRPCRecordFragmentHeader.getFragmentHeader(respHdr.getXDRSize()+resp.getXDRSize(), true));
                    respHdr.marshal(wr);
                    resp.marshal(wr);
                    wr.flip();

                    channel.send(wr.getBuffers().get(0).getBuffer(), sender);

                    wr.freeBuffers();

                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.net, this, "responded to UDP dir discover message from %s", sender);
                        
                } catch (Exception ex) {
                    //bad packet
                    continue;
                }


            } while (!quit);

            channel.close();
        } catch (Exception ex) {
            if (!quit)
                notifyCrashed(ex);
        }
        notifyStopped();
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.lifecycle, me, "DiscoveryMessageThread shutdown complete");

    }

    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }
}
