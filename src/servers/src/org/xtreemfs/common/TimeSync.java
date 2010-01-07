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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import java.net.Socket;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
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

    private enum extSyncSource {
        XTREEMFS_DIR,
        GPSD,
        LOCAL_CLOCK
    };
    
    /**
     * A dir client used to synchronize clocks
     */
    private DIRClient        dir;
    
    /**
     * interval in ms to wait between to synchronizations.
     */
    private final int        timeSyncInterval;
    
    /**
     * interval between updates of the local system clock.
     */
    private final int        localTimeRenew;

    private final extSyncSource     syncSource;

    private final InetSocketAddress gpsdAddr;
    
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
    private volatile long    lastSync;

    private volatile int     syncRTT;

    private volatile boolean syncSuccess;
    
    private static TimeSync  theInstance;

    private final Pattern    gpsdDatePattern;

    private Socket            gpsdSocket;
    
    /**
     * Creates a new instance of TimeSync
     * 
     * @dir a directory server to use for synchronizing clocks, can be null for
     *      test setups only
     */
    private TimeSync(extSyncSource source, DIRClient dir, InetSocketAddress gpsd, int timeSyncInterval, int localTimeRenew) {
        super("TSync Thr");
        setDaemon(true);
        this.localTimeRenew = localTimeRenew;
        this.timeSyncInterval = timeSyncInterval;
        this.dir = dir;
        this.syncSuccess = false;
        this.syncSource = source;
        this.gpsdAddr = gpsd;

        gpsdDatePattern = Pattern.compile("GPSD,D=(....)-(..)-(..)T(..):(..):(..)\\.(.+)Z");

        if (source == extSyncSource.GPSD) {
            try {
                gpsdSocket = new Socket();
                gpsdSocket.setSoTimeout(2000);
                gpsdSocket.setTcpNoDelay(true);
                gpsdSocket.connect(gpsdAddr,2000);
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this,"cannot connect to GPSd: "+ex);
                gpsdSocket = null;
            }
        }
    }


    
    /**
     * main loop
     */
    @Override
    public void run() {
        TimeSync.theInstance = this;
        notifyStarted();
        String tsStatus = " using the local clock (precision is " + this.localTimeRenew + "ms)";
        if (this.dir != null) {
            tsStatus = " and remote sync every " + this.timeSyncInterval + "ms";
        }
        Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "TimeSync is running %s", tsStatus);
        while (!quit) {
            localSysTime = System.currentTimeMillis();
            if (localSysTime - lastSync > timeSyncInterval) {
                resync();
            }
            if (!quit) {
                try {
                    TimeSync.sleep(localTimeRenew);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            
        }
        
        notifyStopped();
        syncSuccess = false;
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
    public static TimeSync initialize(DIRClient dir, int timeSyncInterval, int localTimeRenew)
            throws Exception {
        
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, null, "time sync already running",
                new Object[0]);
            return theInstance;
        }
        
        TimeSync s = new TimeSync(extSyncSource.XTREEMFS_DIR, dir, null, timeSyncInterval, localTimeRenew);
        s.start();
        s.waitForStartup();
        return s;
    }
    
    public static TimeSync initializeLocal(int timeSyncInterval, int localTimeRenew) {
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, null, "time sync already running",
                new Object[0]);
            return theInstance;
        }
        
        TimeSync s = new TimeSync(extSyncSource.LOCAL_CLOCK, null, null, timeSyncInterval, localTimeRenew);
        s.start();
        return s;
    }

    public static TimeSync initializeGPSD(InetSocketAddress gpsd, int timeSyncInterval, int localTimeRenew) {
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, null, "time sync already running",
                new Object[0]);
            return theInstance;
        }

        TimeSync s = new TimeSync(extSyncSource.GPSD, null, gpsd, timeSyncInterval, localTimeRenew);
        s.start();
        return s;
    }
    
    public void enableRemoteSynchronization(DIRClient client) {
        if (this.dir != null) {
            throw new RuntimeException("remote time synchronization is already enabled");
        }
        this.dir = client;
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
                "TimeSync remote synchronization enabled every %d ms", this.timeSyncInterval);
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
        if (gpsdSocket != null) {
            try {
                gpsdSocket.close();
            } catch (IOException ex) {
            }
        }
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

    public static int getSyncRTT() {
        return getInstance().syncRTT;
    }

    public static boolean lastSyncWasSuccessful() {
        return getInstance().syncSuccess;
    }

    
    /**
     *
     * @return the timestamp (local time) when the drift
     * was calculated
     */
    public static long getLastSyncTimestamp() {
        return getInstance().lastSync;
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
        switch (syncSource) {
            case LOCAL_CLOCK : return;
            case XTREEMFS_DIR : {
                RPCResponse<Long> r = null;
                try {
                    long tStart = localSysTime;

                    long oldDrift = currentDrift;
                    r = dir.xtreemfs_global_time_get(null);
                    Long globalTime = r.get();

                    long tEnd = System.currentTimeMillis();
                    // add half a roundtrip to estimate the delay
                    syncRTT = (int)(tEnd - tStart);
                    globalTime += syncRTT / 2;
                    syncSuccess = true;


                    currentDrift = globalTime - tEnd;
                    lastSync = tEnd;

                    if (Math.abs(oldDrift - currentDrift) > 5000 && oldDrift != 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                            "STRANGE DRIFT CHANGE from %d to %d", oldDrift, currentDrift);
                    }

                } catch (Exception ex) {
                    syncSuccess = false;
                    ex.printStackTrace();
                    lastSync = System.currentTimeMillis();
                } finally{
                    if(r!=null){
                        r.freeBuffers();
                    }
                }
                break;
            }
            case GPSD : {
                try {
                    
                    BufferedReader br = new BufferedReader(new InputStreamReader(gpsdSocket.getInputStream()));
                    OutputStream os = gpsdSocket.getOutputStream();
                    long tStart = System.currentTimeMillis();
                    
                    os.write(new byte[]{'d','\n'});
                    os.flush();

                    long oldDrift = currentDrift;
                    String response = br.readLine();
                    long tEnd = System.currentTimeMillis();


                    Matcher m = gpsdDatePattern.matcher(response);
                    Calendar c = Calendar.getInstance();
                    if (m.matches()) {
                        c.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
                        c.set(Calendar.MONTH, Integer.parseInt(m.group(2))-1);
                        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(3)));
                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(4)));
                        c.set(Calendar.MINUTE, Integer.parseInt(m.group(5)));
                        c.set(Calendar.SECOND, Integer.parseInt(m.group(6)));
                        //c.set(Calendar.MILLISECOND, Integer.parseInt(m.group(7))*10);
                    } else {
                        Logging.logMessage(Logging.LEVEL_WARN, this,"cannot parse GPSd response: %s",response);
                        syncSuccess = false;
                        lastSync = tEnd;
                        return;
                    }

                    long globalTime = c.getTimeInMillis();
                    Date d = new Date(globalTime);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"global GPSd time: %d (%d:%d:%d)",c.getTimeInMillis(),d.getHours(),
                            d.getMinutes(),d.getSeconds());
                    
                    // add half a roundtrip to estimate the delay
                    syncRTT = (int)(tEnd - tStart);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"sync RTT: %d ms",syncRTT);
                    globalTime += syncRTT / 2;
                    syncSuccess = true;


                    currentDrift = globalTime - tEnd;
                    lastSync = tEnd;

                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "resync success, drift: %d ms", Math.abs(oldDrift-currentDrift));

                    if (Math.abs(oldDrift - currentDrift) > 5000 && oldDrift != 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                            "STRANGE DRIFT CHANGE from %d to %d", oldDrift, currentDrift);
                    }
                } catch (Exception ex) {
                    syncSuccess = false;
                    ex.printStackTrace();
                    lastSync = System.currentTimeMillis();
                }
            }
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
            
            /*RPCNIOSocketClient c = new RPCNIOSocketClient(null, 5000, 60000);
            c.start();
            c.waitForStartup();
            DIRClient dir = new DIRClient(c, new InetSocketAddress("xtreem.zib.de", 32638));*/
            TimeSync ts = new TimeSync(extSyncSource.GPSD,null, new InetSocketAddress("localhost", 2947), 10000, 50);
            ts.start();
            ts.waitForStartup();
            
            for (;;) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "local time  = %d", ts
                        .getLocalSystemTime());
                Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "global time = %d + %d", ts
                        .getGlobalTime(), ts.getDrift());
                Thread.sleep(1000);
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
