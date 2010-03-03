/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseListener;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.writeRequest;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.utils.ONCRPCException;

/**
 *
 * @author bjko
 */
public class RPCPingPong {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        if (args.length < 2) {
            System.out.println("usage: RPCPingPong ping|pong block size [server IP]");
        }
        try {
            final int bSize = Integer.valueOf(args[1]);

            Logging.start(Logging.LEVEL_WARN, Logging.Category.all);
            TimeSync.initializeLocal(60000, 50);

            if (args[0].equalsIgnoreCase("PONG")) {
                doPong(bSize);
            } else {
                doPing(bSize,args[2]);
            }

            TimeSync.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void doPing(final int bSize, String hostname) throws Exception {

        InetSocketAddress srv = new InetSocketAddress(hostname, 32640);

        RPCNIOSocketClient client = new RPCNIOSocketClient(null, 15000, 60000);

        client.start();
        client.waitForStartup();

        System.out.println("starting PING...");

        final int count = 1000;


        long tStart = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {

            ReusableBuffer rb = BufferPool.allocate(bSize);
            rb.limit(rb.capacity());
            org.xtreemfs.interfaces.OSDInterface.writeRequest data = new writeRequest(new FileCredentials(), "file", 1, 1, 1, 1, new ObjectData(0, true, 0, rb));

            final AtomicBoolean lock = new AtomicBoolean(false);

            client.sendRequest(new RPCResponseListener() {

                @Override
                public void responseAvailable(org.xtreemfs.foundation.oncrpc.client.ONCRPCRequest request) {
                    request.freeBuffers();
                    synchronized (lock) {
                        lock.set(true);
                        lock.notifyAll();
                    }
                }

                @Override
                public void remoteExceptionThrown(org.xtreemfs.foundation.oncrpc.client.ONCRPCRequest rquest, ONCRPCException exception) {
                    System.out.println("failed: "+exception);
                    synchronized (lock) {
                        lock.set(true);
                        lock.notifyAll();
                    }
                }

                @Override
                public void requestFailed(org.xtreemfs.foundation.oncrpc.client.ONCRPCRequest request, IOException reason) {
                    System.out.println("failed: "+reason);
                    System.exit(1);
                }
            }, srv, 0, 0, 0, data);

            synchronized (lock) {
                if (lock.get() == false)
                    lock.wait();
            }

        }
        long tEnd = System.currentTimeMillis();
        System.out.println("duration: "+(tEnd-tStart)+" ms");
        double avgPerCall = ((double)(tEnd-tStart))/((double)count);
        System.out.println("avg ms/call: "+avgPerCall);
        double kbS = (((double)bSize)*((double)count)/1024.0)/(((double)(tEnd-tStart))/1000.0);
        System.out.println(kbS+" kB/s");

        client.shutdown();
        client.waitForShutdown();
        System.out.println("DONE");
    }

    private static void doPong(final int bSize) throws Exception {

        RPCNIOSocketServer srv = new RPCNIOSocketServer(32640, null, new RPCServerRequestListener() {

            @Override
            public void receiveRecord(ONCRPCRequest rq) {
                ReusableBuffer rb = BufferPool.allocate(bSize);
                rb.limit(rb.capacity());
                org.xtreemfs.interfaces.OSDInterface.writeRequest data = new writeRequest(new FileCredentials(), "file", 1, 1, 1, 1, new ObjectData(0, true, 0, rb));
                rq.sendResponse(data);
            }
        }, null);
        srv.start();
        srv.waitForStartup();


        System.out.println("PONG ready. anykey to quit");
        System.in.read();

        srv.shutdown();
        srv.waitForShutdown();
        System.out.println("DONE");
    }

}
