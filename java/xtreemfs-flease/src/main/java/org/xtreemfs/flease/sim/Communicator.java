/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.flease.sim;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.flease.FleaseStage;
import org.xtreemfs.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * A tool to simulate package loss, temporary host disconnects and message delay.
 * @author bjko
 */
public class Communicator extends LifeCycleThread {

    /**
     * the connected UDPSimSockets
     */
    protected final Map<Integer,FleaseStage> ports;

    /**
     * sockets which are currently blocked (i.e. simulated network outage)
     */
    protected final Map<Integer,Integer> blockedPorts;

    /**
     * the packet loss in percent (0..100)
     */
    private int pkgLossPct;

    /**
     * if true the thread will quit operation
     */
    private boolean quit;

    /**
     * debug output is generated if set to true
     */
    private boolean debug;

    /**
     * queue with packets to be delivered to sockets
     */
    private final LinkedBlockingQueue<Packet> sendQ;

    /**
     * singleton
     */
    private static volatile Communicator theInstance;


    /**
     * thread for delayed delivery and blocking ports
     */
    private DelayedDelivery dd;
    private int minDelay;
    private int maxDelay;

    /**
     * percent of packets to be delayed
     */
    private int pctDelay;

    /**
     * if set to true, ports are blocked unsymmetric (i.e. receive but cannot send)
     */
    private boolean halfLink;

    /**
     * Creates a new instance of UDPSim
     * @param pkgLossPct packet loss in percent
     * @param minDelay minimum delay of a delayed packet
     * @param maxDelay maximum delay of a delayed packet
     * @param pctDelay percentage of packets to be delayed
     * @param halfLink if set to true, ports are blocked unsymmetric (i.e. receive but cannot send)
     * @param pHostUnavail probability (0..1) that a host becomes unavailable
     * @param pHostRecovery probability that a host is recovered. This value is multiplied by the
     * number of rounds that the host is already unavailable.
     * @param debug if set to true extensive debug output is generated
     */
    public Communicator(int pkgLossPct, int minDelay, int maxDelay, int pctDelay,
                  boolean halfLink, double pHostUnavail, double pHostRecovery,
            boolean debug) {

        super("UDP-Sim");

        this.ports = new ConcurrentHashMap();
        this.blockedPorts = new ConcurrentHashMap();
        this.pkgLossPct = pkgLossPct;
        this.quit = false;
        this.debug = debug;
        this.sendQ = new LinkedBlockingQueue();
        this.dd = new DelayedDelivery(sendQ,blockedPorts,ports,
                                        pHostUnavail,pHostRecovery,
                                         debug);
        dd.start();
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.pctDelay = pctDelay;

        this.halfLink = halfLink;


        theInstance = this;

    }

    /**
     * opens a port and delivers messages into the queue
     * @param port port number to open
     * @param q the queue to receive messages
     * @return true if the port was opened succesfully, false if the port is already in use
     */
    public boolean openPort(int port, FleaseStage stage) {
        if (ports.get(port) != null)
            return false;

        ports.put(port,stage);
        return true;
    }

    /**
     * Opens a free port
     * @param q the queue to receive messages
     * @return treu if successful, false if there is no free port available
     */
    public int openPort(FleaseStage stage) {
        int tries = 0;
        while (tries < 5) {
            int rport = (int)(Math.random()*65000.0)+1;
            if (ports.get(rport) == null) {
                ports.put(rport,stage);
                return rport;
            }
            tries++;
        }
        return -1;
    }

    /**
     * closes the port
     * @param port port number to close
     */
    public void closePort(int port) {
        ports.remove(port);
    }

    /**
     * sends a datagram packet from
     * @param port sending port number
     * @param dp packet to send
     */
    public synchronized void send(int port, FleaseMessage msg) {
        Packet p = new Packet(msg,port);
        sendQ.add(p);
    }

    /**
     * main loop
     */
    public void run() {
        try {
            InetAddress ia = InetAddress.getLocalHost();

            notifyStarted();
            while (!quit) {
                try {
                    Packet p = sendQ.take();

                    FleaseStage rec = ports.get(p.recipientPort);

                    if (rec == null)
                        continue;

                    if (blockedPorts.containsKey(p.msg.getSender().getPort())) {
                        if (debug)
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,
                                    "msg dropped, port blocked " + p.msg.getSender().getPort());
                        continue;
                    }

                    if (blockedPorts.containsKey(p.recipientPort)) {
                        if (debug)
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,
                                    "msg dropped, port blocked " + p.recipientPort);
                        continue;
                    }

                    if (!dropPacket() || p.requeued) {

                        int delay = delayPacket();
                        if ((delay > 0) && !p.requeued) {
                            if (debug)
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this, "msg delayed " + delay
                                        + "ms " + p.recipientPort + " -> " + p.msg.getSender().getPort());
                            dd.add(p,delay);
                        } else {

                            //p.msg.setSender(new InetSocketAddress(ia, p.recipientPort));
                            try {
                                rec.receiveMessage(p.msg);
                            } catch (IllegalStateException e) {
                                //just drop it
                            }
                        }
                    } else {
                        if (debug)
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,
                                    "msg lost " + p.recipientPort + " -> " + p.msg.getSender().getPort());
                    }



                } catch (InterruptedException ex) {
                    Logging.logError(Logging.LEVEL_ERROR,this,ex);
                }
            }

        } catch (UnknownHostException ex) {
            Logging.logError(Logging.LEVEL_ERROR,this,ex);
        }

        notifyStopped();
    }

    /**
     * decides if a message should be dropped.
     * @return true, if the packet is to be dropped
     */
    private boolean dropPacket() {
        int rv = (int)(Math.random()*100.0);
        return (rv < this.pkgLossPct);
    }



    /**
     * singleton
     * @return null, if not initialized, the instance otherwise
     */
    public static Communicator getInstance() {
        return theInstance;
    }

    /**
     * kills the thread and the delayed delivery thread
     */
    public void shutdown() {

        try {
            this.quit = true;
            this.interrupt();
            dd.shutdown();

            dd.waitForShutdown();
            waitForShutdown();
        } catch (Exception exc) {
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
        }
    }

    /**
     * decides if and how long a packet is delayed
     * @return delay in ms
     */
    private int delayPacket() {
        if (pctDelay == 0)
            return 0;
        int rv = (int)(Math.random()*100.0);
        if (rv < this.pctDelay) {
            return (int)(Math.random()*((double)maxDelay-minDelay))+minDelay;
        } else {
            return 0;
        }
    }

    /**
     * Packet information
     */
    protected static class Packet {
        /**
         * the datagram that is being sent
         */
        public FleaseMessage msg;
        /**
         * originating prot number
         */
        public int recipientPort;
        /**
         * set to true if it was requeued after delay (i.e. must not be dropped, delayed...)
         */
        public boolean requeued;
        /**
         * creates a new packet
         * @param dp datagram packet
         * @param port originating port
         */
        public Packet(FleaseMessage msg, int port) {
            this.msg = msg;
            this.recipientPort = port;
            this.requeued = false;
        }
    }

}
