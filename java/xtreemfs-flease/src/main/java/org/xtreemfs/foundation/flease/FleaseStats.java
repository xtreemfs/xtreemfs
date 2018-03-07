/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.xtreemfs.foundation.flease.comm.tcp.TCPFleaseCommunicator;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseStats extends Thread {
    
    public static final int INTERVAL_IN_MS = 1000;

    private volatile boolean quit;

    private final FleaseStage st;

    private final PrintWriter out;

    public FleaseStats(FleaseStage st, String logfile) throws IOException {
        super("FStats");
        this.st = st;
        this.quit = false;
        try {
            out = new PrintWriter(logfile);
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this,"cannot write to %s, due to %s",logfile,ex.toString());
            ex.printStackTrace();
            throw ex;
        }
    }
    
    public void shutdown() {
        quit = true;
        this.interrupt();
    }

    public void run() {
        long t = 0;
        Logging.logMessage(Logging.LEVEL_INFO, Category.flease, this,"collecting statistics");
        do {
            try {
                try {
                    sleep(INTERVAL_IN_MS);
                } catch (InterruptedException ex) {
                    Logging.logMessage(Logging.LEVEL_INFO, Category.flease, this, "interrupted");
                    break;
                }
                t += INTERVAL_IN_MS;
                int inRq = st.getInRequests();
                int inMsgs = st.getInMessages();
                int outMsgs = st.getOutMessages();
                int inTimers = st.getInTimers();

                int tcpOut = TCPFleaseCommunicator.instance.getNumOut();
                int tcpIn = TCPFleaseCommunicator.instance.getNumIn();
                List<Integer> durRq = st.getRequestDurations();
                List<Integer> durMsgs = st.getMessageDurations();
                List<Integer> durTimers = st.getTimersDurations();

                printValues(inRq, durRq,t,"R");
                printValues(inMsgs, durMsgs,t,"M");
                printValues2(inMsgs, outMsgs,t,"m");
                printValues2(tcpIn, tcpOut,t,"x");
                printValues(inTimers, durTimers,t,"T");
            } catch (Throwable thr) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this,thr.toString());
            }

        } while (!quit);
        out.close();
        Logging.logMessage(Logging.LEVEL_INFO, Category.flease, this,"done");
    }

    void printValues(int numRq, List<Integer> durations, long t, String type) {
        int p95_out = getP95(durations);

        double outPerInterval = ((double)INTERVAL_IN_MS)/( ((double)p95_out) / 1e6);
        double rho = ((double)numRq)/outPerInterval;
        if (p95_out == 0) {
            outPerInterval = 0;
            rho = 0;
        }

        out.format("%10d  %s   %10.2f  %10.2f  %10.2f\n", t,type,(double)numRq,outPerInterval,rho);
    }

    void printValues2(int numRq, int numOut, long t, String type) {

        double rho = ((double)numRq)/((double)numOut);
        if (numOut == 0) {
            rho = 0;
        }

        out.format("%10d  %s   %10.2f  %10.2f  %10.2f\n", t,type,(double)numRq,(double)numOut,rho);
    }

    int getP95(List<Integer> items) {

        if (items.size() == 0)
            return 0;
        if (items.size() == 1)
            return items.get(0);

        Collections.sort(items);

        int rank = ((int)Math.round(0.95*items.size()))-1;
        return items.get(rank);

    }
}
