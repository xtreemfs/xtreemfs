/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.sandbox;

import java.net.InetSocketAddress;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.SnapConfig;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.utils.NettestClient;

/**
 *
 * @author bjko
 */
public class nettest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        if (args.length < 4) {
            System.out.println("usage: nettest <block size in kB> <num packets> <hostname> <port> [osd UUID]\n");
            System.exit(1);
        }

        try {

            Logging.start(Logging.LEVEL_WARN, Logging.Category.all);
            TimeSync.initializeLocal(60000, 50);

            final int blockSize = Integer.parseInt(args[0]) * 1024;
            final int numPackets = Integer.parseInt(args[1]);
            final String hostname = args[2];
            final int port = Integer.parseInt(args[3]);
            final boolean dummyWrites = args.length == 5;
            final String uuid = (dummyWrites) ? args[4]: "";

            InetSocketAddress srv = new InetSocketAddress(hostname, port);

            RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(null, 15000, 60000, Client.getExceptionParsers());

            rpcClient.start();
            rpcClient.waitForStartup();

            NettestClient ntC = new NettestClient(rpcClient, srv);
            OSDClient osdC = new OSDClient(rpcClient);

            System.out.println("starting " + numPackets + " pings...");
            final long tStart = System.currentTimeMillis();

            if (dummyWrites) {

                XLocSet xloc = new XLocSet();
                ReplicaSet rset = new ReplicaSet();
                StringSet uuids = new StringSet();
                uuids.add(uuid);
                Replica repl = new Replica(uuids, 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, blockSize, 1));
                rset.add(repl);
                xloc.setReplicas(rset);
                FileCredentials fcred = new FileCredentials(new XCap(0, "yagga", 0, 0, "ABCDEF:123", false, "", 1, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0),
                        xloc);

                for (int i = 0; i < numPackets; i++) {

                    ReusableBuffer rb = BufferPool.allocate(blockSize);
                    rb.limit(rb.capacity());

                    ObjectData odata = new ObjectData(0, false, 0, rb);

                    RPCResponse r = osdC.write(srv,"ABCDEF:123", fcred, (long)i, 0, 0, 0, odata);
                    r.get();
                    r.freeBuffers();
                    System.out.print(".");
                }
            } else {
                for (int i = 0; i < numPackets; i++) {

                    ReusableBuffer rb = BufferPool.allocate(blockSize);
                    rb.limit(rb.capacity());

                    RPCResponse r = ntC.xtreemfs_nettest_send_buffer(null, rb);
                    r.get();
                    r.freeBuffers();
                    System.out.print(".");
                }
            }
            System.out.println("");

            long tEnd = System.currentTimeMillis();
            System.out.println("total data: "+OutputUtils.formatBytes(numPackets*blockSize));
            System.out.println("duration: " + (tEnd - tStart) + " ms");
            double avgPerCall = ((double) (tEnd - tStart)) / ((double) numPackets);
            System.out.println("avg ms/call: " + avgPerCall);
            double bytesSec = (((double) blockSize) * ((double) numPackets)) / (((double) (tEnd - tStart)) / 1000.0);
            System.out.println(OutputUtils.formatBytes((long)bytesSec) + "/s");

            rpcClient.shutdown();
            rpcClient.waitForShutdown();

            TimeSync.close();
            System.out.println("DONE");


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
