/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.discovery;

import com.google.protobuf.ByteString;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import org.xtreemfs.foundation.buffer.BufferPool;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.utils.PBRPCDatagramPacket;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferOutputStream;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.DirService;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;

/**
 *
 * @author bjko
 */
public class DiscoveryUtils {

    public static final String AUTODISCOVER_HOSTNAME = ".autodiscover";

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

                    RPCHeader resp = RPCHeader.newBuilder().setMessageType(MessageType.RPC_REQUEST).setCallId(1).build();
                    PBRPCDatagramPacket dpack = new PBRPCDatagramPacket(resp, emptyRequest.getDefaultInstance());
                    ReusableBuffer buf = dpack.assembleDatagramPacket();

                    byte[] rdata = buf.getData();
                    BufferPool.free(buf);

                    for (InetAddress bc : broadcasts) {
                        DatagramPacket rp = new DatagramPacket(rdata, rdata.length, bc, PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());
                        dsock.send(rp);
                    }


                    DatagramPacket p = new DatagramPacket(data, data.length);

                    try {
                        dsock.receive(p);
                    } catch (SocketTimeoutException ex) {
                        continue;
                    }

                    ReusableBuffer b = ReusableBuffer.wrap(data, 0, p.getLength());
                    dpack = new PBRPCDatagramPacket(b, DirService.getDefaultInstance());
                    DirService service = (DirService)dpack.getMessage();

                    return service;

            }

            dsock.close();

            return null;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


}
