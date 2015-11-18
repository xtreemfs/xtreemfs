/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease.sim;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.MasterEpochHandlerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * Simulator for testing
 * @author bjko
 */
public class FleaseSim {

    //private void

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {

            final boolean DEBUG_COMM_MSGS = false;

            final int dmax = 500;
            final int leaseTimeout = 5000;

            final int numHosts = 10;
            final FleaseStage[] stages = new FleaseStage[numHosts];

            final boolean useME = true;

            Logging.start(Logging.LEVEL_DEBUG, Category.all);
            TimeSync.initializeLocal(50);

            final Communicator com = new Communicator(10, 100, 30000, 5, true, 0.2, 0.05, DEBUG_COMM_MSGS);
            com.start();

            List<InetSocketAddress> allPorts = new ArrayList(numHosts);
            List<InetSocketAddress>[] acceptors = new List[numHosts];
            MasterEpochHandlerInterface[] meHandlers = new MasterEpochHandlerInterface[numHosts];
            final AtomicReference<Flease>[] leaseStates = new AtomicReference[numHosts];

            for (int i = 0; i < numHosts; i++) {
                final int portNo = 1024+i;
                final int myI = i;
                FleaseConfig cfg = new FleaseConfig(leaseTimeout, dmax, 500, new InetSocketAddress(portNo), "localhost:"+(1024+i),5, true, 0, true);
                leaseStates[i] = new AtomicReference<Flease>(Flease.EMPTY_LEASE);

                if (useME) {
                    meHandlers[i] = new MasterEpochHandlerInterface() {
                        long me = 0;

                        public long getMasterEpoch() {
                            return me;
                        }

                        @Override
                        public void sendMasterEpoch(FleaseMessage response, Continuation callback) {
                            response.setMasterEpochNumber(me);
                            callback.processingFinished();
                        }

                        @Override
                        public void storeMasterEpoch(FleaseMessage request, Continuation callback) {
                            me = request.getMasterEpochNumber();
                            callback.processingFinished();
                        }
                    };
                }


                stages[i] = new FleaseStage(cfg, "/tmp/xtreemfs-test", new FleaseMessageSenderInterface() {

                    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
                        assert(message != null);
                        if (DEBUG_COMM_MSGS)
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"received message for delivery to port %d: %s",recipient.getPort(),message.toString());
                        message.setSender(new InetSocketAddress("localhost", portNo));
                        com.send(recipient.getPort(), message);
                    }
                }, true, new FleaseViewChangeListenerInterface() {

                    public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal) {
                    }
                },new FleaseStatusListener() {

                    public void statusChanged(ASCIIString cellId, Flease lease) {
                        
                        synchronized (leaseStates) {
                            leaseStates[myI].set(lease);
                        }
                        System.out.println("state change for "+portNo+": "+lease.getLeaseHolder()+"/"+lease.getLeaseTimeout_ms());
                    }

                    public void leaseFailed(ASCIIString cellId, FleaseException error) {
                        System.out.println("lease failed: "+error);
                        synchronized (leaseStates) {
                            leaseStates[myI].set(Flease.EMPTY_LEASE);
                        }
                        //restart my cell
                    }
                },meHandlers[i]);
                stages[i].setLifeCycleListener(new LifeCycleListener() {

                    public void startupPerformed() {
                    }

                    public void shutdownPerformed() {
                    }

                    public void crashPerformed(Throwable cause) {
                        cause.printStackTrace();
                        System.exit(100);
                    }
                });
                stages[i].start();
                allPorts.add(new InetSocketAddress("localhost", 1024+i));
                com.openPort(1024+i, stages[i]);
            }

            for (int i = 0; i < numHosts; i++) {
                final int portNo = 1024+i;
                acceptors[i] = new ArrayList(numHosts-1);
                for (InetSocketAddress ia : allPorts) {
                    if (ia.getPort() != portNo)
                        acceptors[i].add(ia);
                }
                stages[i].openCell(new ASCIIString("testcell"), acceptors[i],false);
            }

            //do something

            do {
                final AtomicBoolean sync = new AtomicBoolean();
                final AtomicReference<ASCIIString> ref = new AtomicReference();

                Thread.sleep(100);
                System.out.print("checking local states: ");
                ASCIIString leaseHolder = null;
                int    leaseInstanceId = 0;



                synchronized (leaseStates) {
                    for (int i = 0; i < numHosts; i++) {
                        if (!leaseStates[i].get().isEmptyLease()) {
                            if (leaseHolder == null) {
                                leaseHolder = leaseStates[i].get().getLeaseHolder();
                            } else {
                                if (!leaseHolder.equals(leaseStates[i].get().getLeaseHolder())) {
                                    com.shutdown();
                                    System.out.println("INVARIANT VIOLATED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                    System.out.println("\n\n");
                                    System.out.println("got lease for: "+leaseHolder);
                                    System.out.println("but host "+i+" learned "+leaseStates[i].get().getLeaseHolder());

                                    for (int j = 0; j < numHosts; j++) {
                                        System.out.println(stages[j]._dump_acceptor_state(new ASCIIString("testcell")));
                                        System.out.println("signalled result: "+leaseStates[j].get());
                                        System.out.println("          valid: "+leaseStates[j].get().isValid());
                                    }
                                    System.exit(2);
                                } else {
                                    System.out.print("+");
                                }
                            }
                        } else {
                            System.out.print("o");
                        }
                    }
                }

                System.out.println("");
                
                final int host = (int)(Math.random()*numHosts);
                stages[host].closeCell(new ASCIIString("testcell"), false);
                

                //make sure that cells also time out some times (requires a wait > 2*lease_timeout)
                final int waitTime = (int)(Math.random()*(leaseTimeout*2+1000));
                System.out.println("waiting for "+waitTime+"ms");
                Thread.sleep(waitTime);
                stages[host].openCell(new ASCIIString("testcell"), acceptors[host],false);
            } while (true);

            /*
            for (int i = 0; i < numHosts; i++) {
                stages[i].shutdown();
            }
            com.shutdown();
             */
            

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

}
