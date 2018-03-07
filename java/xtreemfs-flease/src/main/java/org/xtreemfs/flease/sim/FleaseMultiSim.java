///*
// * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
// *
// * Licensed under the BSD License, see LICENSE file for details.
// *
// */
//
//package org.xtreemfs.foundation.flease.sim;
//
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Semaphore;
//import java.util.concurrent.TimeUnit;
//import org.xtreemfs.common.TimeSync;
//import org.xtreemfs.common.buffer.ASCIIString;
//import org.xtreemfs.common.logging.Logging;
//import org.xtreemfs.common.logging.Logging.Category;
//import org.xtreemfs.foundation.LifeCycleListener;
//import FleaseConfig;
//import FleaseMessageSenderInterface;
//import FleaseStage;
//import FleaseViewChangeListenerInterface;
//import FleaseMessage;
//import FleaseListener;
//
///**
// * Simulator for testing
// * @author bjko
// */
//public class FleaseMultiSim {
//
//    //private void
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//        try {
//
//            final boolean DEBUG_COMM_MSGS = false;
//
//            final int dmax = 500;
//            final int leaseTimeout = 10000;
//
//            final int numHosts = 10;
//            final int numConcurrentProposers = 10;
//            final FleaseStage[] stages = new FleaseStage[numHosts];
//
//            Logging.start(Logging.LEVEL_DEBUG, Category.all);
//            TimeSync.initializeLocal(10000, 50);
//
//            final Communicator com = new Communicator(10, 10, 2000, 5, true, 0.05, 0.05, DEBUG_COMM_MSGS);
//            com.start();
//
//            List<InetSocketAddress> allPorts = new ArrayList(numHosts);
//            List<InetSocketAddress>[] acceptors = new List[numHosts];
//
//            for (int i = 0; i < numHosts; i++) {
//                final int portNo = 1024+i;
//                FleaseConfig cfg = new FleaseConfig(leaseTimeout, dmax, 500, new InetSocketAddress(portNo), "localhost:"+(1024+i),5);
//                stages[i] = new FleaseStage(cfg, "/tmp/xtreemfs-test", new FleaseMessageSenderInterface() {
//
//                    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
//                        assert(message != null);
//                        if (DEBUG_COMM_MSGS)
//                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"received message for delivery to port %d: %s",recipient.getPort(),message.toString());
//                        message.setSender(new InetSocketAddress("localhost", portNo));
//                        com.send(recipient.getPort(), message);
//                    }
//                }, true, new FleaseViewChangeListenerInterface() {
//
//                    public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
//                           //ignore
//                    }
//                },null);
//                stages[i].setLifeCycleListener(new LifeCycleListener() {
//
//                    public void startupPerformed() {
//                    }
//
//                    public void shutdownPerformed() {
//                    }
//
//                    public void crashPerformed(Throwable cause) {
//                        cause.printStackTrace();
//                        System.exit(100);
//                    }
//                });
//                stages[i].start();
//                allPorts.add(new InetSocketAddress("localhost", 1024+i));
//                com.openPort(1024+i, stages[i]);
//            }
//
//            for (int i = 0; i < numHosts; i++) {
//                final int portNo = 1024+i;
//                acceptors[i] = new ArrayList(numHosts-1);
//                for (InetSocketAddress ia : allPorts) {
//                    if (ia.getPort() != portNo)
//                        acceptors[i].add(ia);
//                }
//                stages[i].openCell(new ASCIIString("testcell"), acceptors[i],true);
//            }
//
//            //do something
//
//            do {
//                final List<LInfo> results = new ArrayList(numHosts);
//                final Semaphore s = new Semaphore(0);
//
//
//                final List<Integer> hostNumbers = new ArrayList(numHosts);
//                for (int i = 0; i < numHosts; i++)
//                    hostNumbers.add(i);
//
//                while (hostNumbers.size()+numConcurrentProposers > numHosts) {
//                    final int tmp = (int)(Math.random()*hostNumbers.size());
//                    final int host = hostNumbers.remove((int)tmp);
//                    stages[host].getLease(new ASCIIString("testcell"), new FleaseListener() {
//
//                        public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
//                            final ASCIIString id = stages[host].getIdentity();
//                            synchronized (results) {
//                                LInfo i = new LInfo();
//                                i.host = id;
//                                i.owner = leaseHolder;
//                                i.to = leaseTimeout_ms;
//                                results.add(i);
//                            }
//                            s.release();
//                            System.out.println("host: "+id+". lease result: "+leaseHolder+"/"+leaseTimeout_ms);
//                        }
//
//                        public void proposalFailed(ASCIIString cellId, Throwable cause) {
//                            final ASCIIString id = stages[host].getIdentity();
//                            System.out.println("host: "+id+". lease failed: "+cause);
//                            s.release();
//                        }
//                    });
//                }
//                System.out.println("all started");
//
//                if (!s.tryAcquire(numConcurrentProposers, 30000, TimeUnit.MILLISECONDS)) {
//                    throw new RuntimeException("timed out waiting for semaphore");
//                }
//
//                System.out.println("all results available");
//                final long now = TimeSync.getGlobalTime();
//
//                //first: remove all leases which are timed out
//                Iterator<LInfo> iter = results.iterator();
//                while (iter.hasNext()) {
//                    LInfo i = iter.next();
//                    if (i.to+dmax < now) {
//                        iter.remove();
//                    }
//                }
//
//                //now check all the values
//                ASCIIString owner = null;
//                LInfo other = null;
//                boolean violated = false;
//                for (LInfo i : results) {
//                    if (owner == null) {
//                        owner = i.owner;
//                        other = i;
//                    } else {
//                        if (!owner.equals(i.owner)) {
//                            com.shutdown();
//                            Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();
//                            System.out.println("INVARIANT VIOLATED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//                            System.out.println("\n\n");
//                            System.out.println(i.host+" has : "+i.owner+"/"+i.to);
//                            System.out.println(other.host+" has : "+other.owner+"/"+other.to);
//
//                            for (int j = 0; j < numHosts; j++) {
//                                System.out.println(stages[j]._dump_acceptor_state(new ASCIIString("testcell")));
//                            }
//
//                            System.out.println("");
//                            for (Thread t : traces.keySet()) {
//                                System.out.println("Thread: "+t.getName());
//                                final StackTraceElement[] e = traces.get(t);
//                                for (StackTraceElement elem : e) {
//                                    System.out.println(elem.toString());
//                                }
//                            }
//                            System.exit(2);
//                        }
//                    }
//                }
//
//
//                //make sure that cells also time out some times (requires a wait > 2*lease_timeout)
//                final int waitTime = (int)(Math.random()*(leaseTimeout*2+2000+dmax));
//                System.out.println("waiting for "+waitTime+"ms");
//                Thread.sleep(waitTime);
//            } while (true);
//            /*
//            for (int i = 0; i < numHosts; i++) {
//                stages[i].shutdown();
//            }
//            com.shutdown();
//            */
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();
//            System.out.println("");
//            for (Thread t : traces.keySet()) {
//                System.out.println("Thread: "+t.getName());
//                final StackTraceElement[] e = traces.get(t);
//                for (StackTraceElement elem : e) {
//                    System.out.println(elem.toString());
//                }
//            }
//            System.exit(1);
//        }
//
//    }
//
//    public static final class LInfo {
//        ASCIIString host;
//        ASCIIString owner;
//        long to;
//
//    }
//
//}
