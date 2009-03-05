/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Jesús Malo (BSC)
 */

package org.xtreemfs.common;

import java.net.InetSocketAddress;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;

/**
 * A class that offers a local time w/ adjustable granularity and a global time
 * based on the time reported by the DIR. Global time is adjusted periodically.
 * This class should be used to minimize the number of calls to
 * System.currentTimeMillis which is a costly system call on Linux. Moreover it
 * offers a system-global time.
 * 
 * @author bjko
 */
public final class TimeSync extends LifeCycleThread {
    
    /**
     * A dir client used to synchronize clocks
     */
    private final DIRClient  dir;
    
    /**
     * interval in ms to wait between to synchronizations.
     */
    private final int        timeSyncInterval;
    
    /**
     * interval between updates of the local system clock.
     */
    private final int        localTimeRenew;
    
    /**
     * local sys time as of last update
     */
    private volatile long    localSysTime;
    
    /**
     * drift between local clock and global time as of last resync() operation.
     */
    private volatile long    currentDrift;
    
    /**
     * set to true to stop thread
     */
    private volatile boolean quit;
    
    /**
     * timestamp of last resync operation
     */
    private long             lastSync;
    
    private static TimeSync  theInstance;
    
    /**
     * Creates a new instance of TimeSync
     * 
     * @dir a directory server to use for synchronizing clocks, can be null for
     *      test setups only
     */
    private TimeSync(DIRClient dir, int timeSyncInterval, int localTimeRenew) {
        super("TimeSync Thread");
        setDaemon(true);
        this.localTimeRenew = localTimeRenew;
        this.timeSyncInterval = timeSyncInterval;
        this.dir = dir;
    }
    
    /**
     * main loop
     */
    @Override
    public void run() {
        TimeSync.theInstance = this;
        notifyStarted();
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"running");
        while (!quit) {
            localSysTime = System.currentTimeMillis();
            if (localSysTime - lastSync > timeSyncInterval) {
                resync();
            }
            if (! quit) {
                try {
                    TimeSync.sleep(localTimeRenew);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            
        }
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"shutdown complete");
        notifyStopped();
        theInstance = null;
    }
    
    /**
     * Initializes the time synchronizer. Note that only the first invocation of
     * this method has an effect, any further invocations will be ignored.
     * 
     * @param dir
     * @param timeSyncInterval
     * @param localTimeRenew
     * @param dirAuthStr
     */
    public static TimeSync initialize(DIRClient dir, int timeSyncInterval, int localTimeRenew) {
        
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_ERROR, null,"time sync already running");
            return theInstance;
        }
        
        TimeSync s = new TimeSync(dir, timeSyncInterval, localTimeRenew);
        s.start();
        return s;
    }
    
    public static void close() {
        if (theInstance == null)
            return;
        theInstance.shutdown();
    }
    
    /**
     * stop the thread
     */
    public void shutdown() {
        quit = true;
        this.interrupt();
    }
    
    /**
     * returns the current value of the local system time variable. Has a
     * resolution of localTimeRenew ms.
     */
    public static long getLocalSystemTime() {
        return getInstance().localSysTime;
    }
    
    /**
     * returns the current value of the local system time adjusted to global
     * time. Has a resolution of localTimeRenew ms.
     */
    public static long getGlobalTime() {
        return getInstance().localSysTime + getInstance().currentDrift;
    }
    
    public static long getLocalRenewInterval() {
        return getInstance().localTimeRenew;
    }
    
    public static int getTimeSyncInterval() {
        return getInstance().timeSyncInterval;
    }
    
    /**
     * returns the current clock drift.
     */
    public long getDrift() {
        return this.currentDrift;
    }
    
    /**
     * resynchronizes with the global time obtained from the DIR
     */
    private void resync() {
        if (dir == null)
            return;
        try {
            long tStart = localSysTime;
            
            long oldDrift = currentDrift;
            RPCResponse<Long> r = dir.global_time_get(null);
            Long globalTime = r.get();
            r.freeBuffers();
            long tEnd = System.currentTimeMillis();
            // add half a roundtrip to estimate the delay
            globalTime += (tEnd - tStart) / 2;
            
            currentDrift = globalTime - tEnd;
            lastSync = tEnd;
            
            if (Math.abs(oldDrift - currentDrift) > 5000) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "STRANGE DRIFT CHANGE from " + oldDrift
                    + " to " + currentDrift);
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            lastSync = System.currentTimeMillis();
        }
    }
    
    public static TimeSync getInstance() {
        if (theInstance == null)
            throw new RuntimeException("TimeSync not initialized!");
        return theInstance;
    }
    
    /**
     * Simple demonstration routine
     */
    public static void main(String[] args) {
        try {
            // simple test
            Logging.start(Logging.LEVEL_DEBUG);
            
            RPCNIOSocketClient c = new RPCNIOSocketClient(null, 5000, 60000);
            c.start();
            c.waitForStartup();
            DIRClient dir = new DIRClient(c, new InetSocketAddress("xtreem.zib.de", 32638));
            TimeSync ts = new TimeSync(dir, 1000, 50);
            ts.start();
            
            for (;;) {
                Logging.logMessage(Logging.LEVEL_INFO, null, "local time  = " + ts.getLocalSystemTime());
                Logging.logMessage(Logging.LEVEL_INFO, null, "global time = " + ts.getGlobalTime() + " +"
                    + ts.getDrift());
                Thread.sleep(1000);
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
