/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.dir.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
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

            final DatagramSocket dsock = new DatagramSocket(DIRInterface.DEFAULT_ONCRPC_PORT);

            final byte[] data = new byte[2048];

            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.lifecycle, me, "DiscoveryMessageThread started");

            do {

                DatagramPacket p = new DatagramPacket(data, data.length);

                dsock.receive(p);

                try {
                    ReusableBuffer b = ReusableBuffer.wrap(p.getData(), 0, p.getLength());
                    b.position(Integer.SIZE / 8);
                    ONCRPCRequestHeader hdr = new ONCRPCRequestHeader();
                    hdr.deserialize(b);
                    xtreemfs_discover_dirRequest rq = new xtreemfs_discover_dirRequest();
                    rq.deserialize(b);


                    xtreemfs_discover_dirResponse resp = new xtreemfs_discover_dirResponse(me);
                    ONCRPCResponseHeader respHdr = new ONCRPCResponseHeader(hdr.getXID(),
                            ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED, 
                            ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);
                
                    ONCRPCBufferWriter wr = new ONCRPCBufferWriter(2048);
                    wr.putInt(ONCRPCRecordFragmentHeader.getFragmentHeader(respHdr.calculateSize()+resp.calculateSize(), true));
                    respHdr.serialize(wr);
                    resp.serialize(wr);
                    wr.flip();
                    byte[] rdata = wr.getBuffers().get(0).array();
                    wr.freeBuffers();
                    
                    DatagramPacket rp = new DatagramPacket(rdata, 0, rdata.length, p.getSocketAddress());
                    dsock.send(rp);

                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.net, this, "responded to UDP dir discover message from %s", p.getSocketAddress());
                        
                } catch (Exception ex) {
                    //bad packet
                    continue;
                }


            } while (!quit);

            dsock.close();

        } catch (Exception ex) {
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
