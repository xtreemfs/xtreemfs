/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.RPCResponseListener;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;

/**
 *
 * @author bjko
 */
public class MRCPerformance {

    private final int numFiles;
    
    private final InetSocketAddress mrcAddress;
    
    private final String volName;
    
    private final MRCClient client;

    public final int MAXINFLIGHT = 500;
    
    public MRCPerformance(InetSocketAddress mrcAddress, String volName, int numFiles) throws IOException {
        this.mrcAddress = mrcAddress;
        this.volName = volName + (volName.endsWith("/") ? "" : "/");
        this.numFiles = numFiles;
        this.client = new MRCClient(); 
    }
    
    public void shutdown() {
        client.shutdown();
        client.waitForShutdown();
    }
    
    public void createFiles() throws Exception {

        final String authStr = NullAuthProvider.createAuthString("user", "users");

        final AtomicInteger numResponses = new AtomicInteger(0);

        final AtomicInteger inFlight = new AtomicInteger(0);

        RPCResponseListener rl = new RPCResponseListener() {

            @Override
            public void responseAvailable(RPCResponse response) {
                try {
                    if (response.getStatusCode() != 200)
                        System.out.println("error: "+response.getBody().getString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                response.freeBuffers();

                if (inFlight.getAndDecrement() == 10) {
                    synchronized (inFlight) {
                        inFlight.notify();
                    }
                }

                if (numResponses.incrementAndGet() == numFiles) {
                    synchronized (numResponses) {
                        numResponses.notifyAll();
                    }
                }
            }
        };

        long tStart = System.currentTimeMillis();

        for (int i = 0; i < numFiles; i++) {
            final String fname = String.format("%020d",i);
            RPCResponse r = client.async_createFile(mrcAddress, volName+fname, authStr);
            r.setResponseListener(rl);
            if (inFlight.incrementAndGet() > MAXINFLIGHT) {
                synchronized (inFlight) {
                    inFlight.wait();
                }
            }
        }

        long tEnd;
        if (numResponses.get() != numFiles) {
            synchronized (numResponses) {
                numResponses.wait();
            }
        }
        tEnd = System.currentTimeMillis();

        System.out.println("creating "+numFiles+" files took "+(tEnd-tStart)+" ms");

    }


    public void deleteFiles() throws Exception {

        final String authStr = NullAuthProvider.createAuthString("user", "users");

        final AtomicInteger numResponses = new AtomicInteger(0);

        final AtomicInteger inFlight = new AtomicInteger(0);

        RPCResponseListener rl = new RPCResponseListener() {

            @Override
            public void responseAvailable(RPCResponse response) {
                try {
                    if (response.getStatusCode() != 200)
                        System.out.println("error: "+response.getBody().getString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                response.freeBuffers();

                if (inFlight.getAndDecrement() == 10) {
                    synchronized (inFlight) {
                        inFlight.notify();
                    }
                }

                if (numResponses.incrementAndGet() == numFiles) {
                    synchronized (numResponses) {
                        numResponses.notifyAll();
                    }
                }
            }
        };

        long tStart = System.currentTimeMillis();

        for (int i = 0; i < numFiles; i++) {
            final String fname = String.format("%020d",i);
            RPCResponse r = client.async_delete(mrcAddress, volName+fname, authStr);
            r.setResponseListener(rl);
            if (inFlight.incrementAndGet() > MAXINFLIGHT) {
                synchronized (inFlight) {
                    inFlight.wait();
                }
            }
        }

        long tEnd;
        if (numResponses.get() != numFiles) {
            synchronized (numResponses) {
                numResponses.wait();
            }
        }
        tEnd = System.currentTimeMillis();

        System.out.println("deleteing "+numFiles+" files took "+(tEnd-tStart)+" ms");

    }

    public void listFiles() throws Exception {

        final String authStr = NullAuthProvider.createAuthString("user", "users");

        long tStart = System.currentTimeMillis();

        List<String> files = client.readDir(mrcAddress, volName, authStr);
        
        long tEnd = System.currentTimeMillis();

        System.out.println("listing "+numFiles+" files took "+(tEnd-tStart)+" ms");

    }

    public static void main(String[] args) {
        try {
            int numfiles = Integer.valueOf(args[0]);
            Logging.start(Logging.LEVEL_INFO);
            TimeSync.initialize(null, 10000, 50, "");
            System.out.println("benchmarking "+numfiles+"files");
            MRCPerformance p = new MRCPerformance(new InetSocketAddress("localhost",32636), "test", numfiles);
            p.createFiles();

            System.out.println("Please press <Enter> after flushing caches...");
            System.in.read();

            p.listFiles();
            p.deleteFiles();
            p.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }


}
