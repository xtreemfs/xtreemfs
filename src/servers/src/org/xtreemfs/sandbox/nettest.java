/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.sandbox;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.NettestInterface.send_bufferRequest;
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

        if (args.length != 4) {
            System.out.println("usage: nettest <block size in kB> <num packets> <hostname> <port>\n");
            System.exit(1);
        }

        try {

            Logging.start(Logging.LEVEL_WARN, Logging.Category.all);
            TimeSync.initializeLocal(60000, 50);

            final int blockSize = Integer.parseInt(args[0]) * 1024;
            final int numPackets = Integer.parseInt(args[1]);
            final String hostname = args[2];
            final int port = Integer.parseInt(args[3]);

            InetSocketAddress srv = new InetSocketAddress(hostname, port);

            RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(null, 15000, 60000);

            rpcClient.start();
            rpcClient.waitForStartup();

            NettestClient ntC = new NettestClient(rpcClient, srv);

            System.out.println("starting " + numPackets + " pings...");
            final long tStart = System.currentTimeMillis();

            for (int i = 0; i < numPackets; i++) {
                ReusableBuffer rb = BufferPool.allocate(blockSize);
                rb.limit(rb.capacity());

                RPCResponse r = ntC.xtreemfs_nettest_send_buffer(null, rb);
                r.get();
                r.freeBuffers();
                System.out.print(".");
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
