/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.client.MRCClient;

/**
 * 
 * @author bjko
 */
public class MRCPerformance {
    
    private final int                numFiles;
    
    private final InetSocketAddress  mrcAddress;
    
    private final String             volName;
    
    private final MRCClient          client;
    
    private final RPCNIOSocketClient rpcClient;
    
    public final int                 MAXINFLIGHT = 500;
    
    private final UserCredentials    userCred;
    
    public MRCPerformance(InetSocketAddress mrcAddress, String volName, int numFiles) throws IOException {
        this.mrcAddress = mrcAddress;
        this.volName = volName + (volName.endsWith("/") ? "" : "/");
        this.numFiles = numFiles;
        this.rpcClient = new RPCNIOSocketClient(null, 10000, 300000);
        this.client = new MRCClient(rpcClient, mrcAddress);
        
        
        List<String>gids = new LinkedList<String>();
        gids.add("myGroup");

        userCred = MRCClient.getCredentials("me", gids);
    }
    
    public void shutdown() throws Exception {
        rpcClient.shutdown();
        rpcClient.waitForShutdown();
    }
    
    public void createFiles() throws Exception {
        
        final AtomicInteger numResponses = new AtomicInteger(0);
        
        final AtomicInteger inFlight = new AtomicInteger(0);
        
        RPCResponseAvailableListener rl = new RPCResponseAvailableListener() {
            
            @Override
            public void responseAvailable(RPCResponse response) {
                
                try {
                    response.get();
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
                    
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        
        long tStart = System.currentTimeMillis();
        
        for (int i = 0; i < numFiles; i++) {
            final String fname = String.format("%020d", i);
            RPCResponse<Object> r = client.create(null, userCred, volName + fname, 509);
            r.registerListener(rl);
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
        
        System.out.println("creating " + numFiles + " files took " + (tEnd - tStart) + " ms");
        
    }
    
    public void deleteFiles() throws Exception {
        
        final AtomicInteger numResponses = new AtomicInteger(0);
        
        final AtomicInteger inFlight = new AtomicInteger(0);
        
        RPCResponseAvailableListener rl = new RPCResponseAvailableListener() {
            
            @Override
            public void responseAvailable(RPCResponse response) {
                try {
                    response.get();
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
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        
        long tStart = System.currentTimeMillis();
        
        for (int i = 0; i < numFiles; i++) {
            final String fname = String.format("%020d", i);
            RPCResponse r = client.unlink(null, userCred, volName + fname);
            r.registerListener(rl);
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
        
        System.out.println("deleteing " + numFiles + " files took " + (tEnd - tStart) + " ms");
        
    }
    
    public void listFiles() throws Exception {
        
        long tStart = System.currentTimeMillis();
        
        RPCResponse<DirectoryEntrySet> files = client.readdir(null,userCred, volName);
        
        long tEnd = System.currentTimeMillis();
        
        System.out.println("listing " + numFiles + " files took " + (tEnd - tStart) + " ms");
        
    }
    
    public static void main(String[] args) {
        try {
            int numfiles = Integer.valueOf(args[0]);
            Logging.start(Logging.LEVEL_INFO);
            TimeSync.initialize(null, 10000, 50);
            System.out.println("benchmarking " + numfiles + "files");
            MRCPerformance p = new MRCPerformance(new InetSocketAddress("localhost", 32636), "test", numfiles);
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
