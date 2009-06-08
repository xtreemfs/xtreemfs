/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.dir.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import org.xtreemfs.common.buffer.ReusableBuffer;
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
public class DiscoveryUtils {

    public static DirService discoverDir(int maxWaitSeconds)  {
        try {

            final DatagramSocket dsock = new DatagramSocket();
            dsock.setBroadcast(true);
            dsock.setSoTimeout(1000);

            final byte[] data = new byte[2048];

            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();

            List<InetAddress> broadcasts = new LinkedList();

            broadcasts.add(InetAddress.getLocalHost());

            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                for (InterfaceAddress ia : nif.getInterfaceAddresses()) {
                    InetAddress bc = ia.getBroadcast();
                    if (bc != null) {
                        broadcasts.add(bc);
                    }
                }
            }



            for (int i = 0; i < maxWaitSeconds; i++) {


                    xtreemfs_discover_dirRequest rq = new xtreemfs_discover_dirRequest();
                    ONCRPCRequestHeader rqHdr = new ONCRPCRequestHeader(1, 1, DIRInterface.getVersion(), rq.TAG);

                    ONCRPCBufferWriter wr = new ONCRPCBufferWriter(2048);
                    wr.putInt(ONCRPCRecordFragmentHeader.getFragmentHeader(rqHdr.calculateSize()+rq.calculateSize(), true));
                    rqHdr.serialize(wr);
                    rq.serialize(wr);
                    wr.flip();
                    byte[] rdata = wr.getBuffers().get(0).array();
                    wr.freeBuffers();

                    for (InetAddress bc : broadcasts) {
                        DatagramPacket rp = new DatagramPacket(rdata, rdata.length, bc, DIRInterface.DEFAULT_ONCRPC_PORT);
                        dsock.send(rp);
                    }

                    DatagramPacket p = new DatagramPacket(data, data.length);

                    try {
                        dsock.receive(p);
                    } catch (SocketTimeoutException ex) {
                        continue;
                    }

                    ReusableBuffer b = ReusableBuffer.wrap(data, 0, p.getLength());
                    b.position(Integer.SIZE / 8);
                    ONCRPCResponseHeader respHdr = new ONCRPCResponseHeader();
                    respHdr.deserialize(b);
                    xtreemfs_discover_dirResponse resp = new xtreemfs_discover_dirResponse();
                    resp.deserialize(b);

                    return resp.getDir_service();

            }

            dsock.close();

            return null;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


}
