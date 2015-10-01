/*
 * Copyright (c) 2008-2010 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

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

    public enum ExtSyncSource {
        XTREEMFS_DIR, GPSD, LOCAL_CLOCK
    };

    /**
     * The maximum round trip time for a clock synchronization message between
     * the <code>TimeSync</code> and the DIR. If the round trip time of a
     * synchronization message exceeds this value, the message will be ignored.
     */
    private static final int MAX_RTT = 1000;
    
    /**
     * A client used to synchronize clocks
     */
    private TimeServerClient       timeServerClient;

    /**
     * interval in ms to wait between to synchronizations.
     */
    private volatile int           timeSyncInterval;

    /**
     * interval between updates of the local system clock.
     * 
     * If it's set to 0, the local renew by the thread is disabled and the time
     * is read from the system on demand. 
     */
    private volatile int           localTimeRenew;

    private volatile ExtSyncSource syncSource;

    private InetSocketAddress      gpsdAddr;

    /**
     * local sys time as of last update
     */
    private volatile long          localSysTime;

    /**
     * drift between local clock and global time as of last resync() operation.
     */
    private volatile long          currentDrift;

    /**
     * set to true to stop thread
     */
    private volatile boolean       quit;

    /**
     * timestamp of last resync operation
     */
    private volatile long          lastSuccessfulSync;
    
    /**
     * Timestamp of the last resync attempt.
     * 
     * @note No need to specify it as volatile since it's only used by run().
     */
    private long                   lastSyncAttempt;

    private volatile int           syncRTT;

    private volatile boolean       syncSuccess;

    private static TimeSync        theInstance;

    private final Pattern          gpsdDatePattern;

    private Socket                 gpsdSocket;
    
    /**
     * Creates a new instance of TimeSync
     * 
     * @dir a directory server to use for synchronizing clocks, can be null for
     *      test setups only
     */
    private TimeSync(ExtSyncSource source, TimeServerClient dir, InetSocketAddress gpsd, int timeSyncInterval, int localTimeRenew) {
        super("TSync Thr");
        setDaemon(true);
        this.syncSuccess = false;
        this.gpsdDatePattern = Pattern.compile("GPSD,D=(....)-(..)-(..)T(..):(..):(..)\\.(.+)Z");
        
        init(source, dir, gpsd, timeSyncInterval, localTimeRenew);
    }

    /**
     * Initializes the TimeSync with new parameters.
     * 
     * @param source
     * @param dir
     * @param gpsd
     * @param timeSyncInterval
     * @param localTimeRenew
     */
    public synchronized void init(ExtSyncSource source, TimeServerClient dir, InetSocketAddress gpsd, int timeSyncInterval, int localTimeRenew) {
        this.localTimeRenew = localTimeRenew;
        this.timeSyncInterval = timeSyncInterval;
        this.timeServerClient = dir;
        this.syncSource = source;
        this.gpsdAddr = gpsd;
        
        if (this.timeServerClient != null && this.timeSyncInterval != 0 && this.localTimeRenew != 0) {
            this.localTimeRenew = 0;
            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                    "Disabled the periodic local time renew (set local_clock_renewal to 0)" +
                    " and using always the current system time as base since the time will be corrected by synchronizing with the DIR service.");
        }

        if (source == ExtSyncSource.GPSD) {
            try {
                if (gpsdSocket != null)
                    gpsdSocket.close();
                
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
        String tsStatus;
        if (localTimeRenew == 0) {
            tsStatus = "using the local clock";
        } else {
            tsStatus = "using the local clock (precision is " + this.localTimeRenew + " ms)";
        }
        if (this.timeServerClient != null && timeSyncInterval != 0) {
            tsStatus += " and remote sync every " + this.timeSyncInterval + " ms";
        }
        Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "TimeSync is running %s", tsStatus);
        while (!quit) {
            // Renew cached local time.
            final long previousLocalSysTime = localSysTime;
            localSysTime = System.currentTimeMillis();
            if (localTimeRenew > 0 && previousLocalSysTime != 0) {
                final long timeBetweenUpdates = Math.abs(localSysTime - previousLocalSysTime);
                if (timeBetweenUpdates > 4 * localTimeRenew) {
                    Logging.logMessage(Logging.LEVEL_WARN, this,
                            "The granularity of the renewed local time could not be guaranteed" +
                            " since it took longer to retrieve the latest local time (%d ms) than configured (local_clock_renewal = %d)." +
                            " Maybe the system is under high I/O load and therefore scheduling threads takes longer than usual?",
                            timeBetweenUpdates,
                            localTimeRenew);
                }
            }
            
            // Remote sync time.
            if (timeSyncInterval != 0 && localSysTime - lastSyncAttempt > timeSyncInterval) {
                resync();
            }
            if (!quit) {
                // 
                try {
                    // If local refresh was disabled, use timeSyncInterval as sleep time.
                    long sleepTimeMs = localTimeRenew != 0 ? localTimeRenew : timeSyncInterval;
                    if (sleepTimeMs == 0) {
                        // If there is no need to run this thread at all, let it sleep for 10 minutes.
                        sleepTimeMs = 600000;
                    }
                    TimeSync.sleep(sleepTimeMs);
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
    public static TimeSync initialize(TimeServerClient dir, int timeSyncInterval, int localTimeRenew) throws Exception {
        
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, null, "time sync already running",
                new Object[0]);
            return theInstance;
        }
        
        TimeSync s = new TimeSync(ExtSyncSource.XTREEMFS_DIR, dir, null, timeSyncInterval, localTimeRenew);
        s.start();
        s.waitForStartup();
        return s;
    }
    
    public static TimeSync initializeLocal(int localTimeRenew) throws Exception {
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, null, "time sync already running",
                new Object[0]);
            return theInstance;
        }
        
        TimeSync s = new TimeSync(ExtSyncSource.LOCAL_CLOCK, null, null, 0, localTimeRenew);
        s.start();
        s.waitForStartup();

        return s;
    }

    public static TimeSync initializeGPSD(InetSocketAddress gpsd, int timeSyncInterval, int localTimeRenew)
            throws Exception {
        if (theInstance != null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, null, "time sync already running",
                new Object[0]);
            return theInstance;
        }

        TimeSync s = new TimeSync(ExtSyncSource.GPSD, null, gpsd, timeSyncInterval, localTimeRenew);
        s.start();
        s.waitForStartup();

        return s;
    }
    
    public void close() {
        shutdown();
        try {
            waitForShutdown();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_ERROR, null, e);
        }
    }
    
    /**
     * stop the thread
     */
    @Override
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
        TimeSync ts = getInstance();
        if (ts.localTimeRenew == 0 || ts.localSysTime == 0) {
            return System.currentTimeMillis();
        } else {
            return ts.localSysTime;
        }
    }
    
    /**
     * returns the current value of the local system time adjusted to global
     * time. Has a resolution of localTimeRenew ms.
     */
    public static long getGlobalTime() {
        TimeSync ts = getInstance();
        if (ts.localTimeRenew == 0 || ts.localSysTime == 0) {
            return System.currentTimeMillis() + ts.currentDrift;
        } else {
            return ts.localSysTime + ts.currentDrift;
        }
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
     * was successfully calculated
     */
    public static long getLastSuccessfulSyncTimestamp() {
        return getInstance().lastSuccessfulSync;
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
    @SuppressWarnings("deprecation")
    private void resync() {
        switch (syncSource) {
            case LOCAL_CLOCK : return;
            case XTREEMFS_DIR : {
                try {
                    long tStart = System.currentTimeMillis();
                    lastSyncAttempt = tStart;
                    long oldDrift = currentDrift;
                    long globalTime = timeServerClient.xtreemfs_global_time_get(null);
                    if (globalTime <= 0) {
                        //error
                        return;
                    }

                    long tEnd = System.currentTimeMillis();
                    // add half a roundtrip to estimate the delay
                    syncRTT = (int)(tEnd - tStart);
                    
                    if (syncRTT > MAX_RTT) {
                        Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                                "Ignored time synchronization message because DIR took too long to respond (%d ms)",
                                syncRTT);
                        syncSuccess = false;
                        return;
                    }
                    
                    globalTime += syncRTT / 2;
                    syncSuccess = true;

                    currentDrift = globalTime - tEnd;
                    lastSuccessfulSync = tEnd;

                    if (Math.abs(oldDrift - currentDrift) > 5000 && oldDrift != 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                            "STRANGE DRIFT CHANGE from %d to %d", oldDrift, currentDrift);
                    }

                } catch (Exception ex) {
                    syncSuccess = false;
                    ex.printStackTrace();
                }
                break;
            }
            case GPSD : {
                try {
                    
                    BufferedReader br = new BufferedReader(new InputStreamReader(gpsdSocket.getInputStream()));
                    OutputStream os = gpsdSocket.getOutputStream();
                    long tStart = System.currentTimeMillis();
                    lastSyncAttempt = tStart;
                    
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
                    lastSuccessfulSync = tEnd;

                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "resync success, drift: %d ms", Math.abs(oldDrift-currentDrift));

                    if (Math.abs(oldDrift - currentDrift) > 5000 && oldDrift != 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                            "STRANGE DRIFT CHANGE from %d to %d", oldDrift, currentDrift);
                    }
                } catch (Exception ex) {
                    syncSuccess = false;
                    ex.printStackTrace();
                }
             }
        }
    }
    
    public static TimeSync getInstance() {
        if (theInstance == null)
            throw new RuntimeException("TimeSync not initialized!");
        return theInstance;
    }
    
    public static boolean isInitialized() {
        return theInstance != null;
    }
}
