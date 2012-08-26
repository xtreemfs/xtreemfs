/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

/**
 * Checks regularly for remote MRCs and their replication endpoints.
 * 
 * @author stenjan
 */
public class MRCStatusManager extends LifeCycleThread {
    
    /**
     * Interval in ms to wait between two checks.
     */
    private int                            checkIntervalMillis = 1000 * 5;
    
    private ServiceSet.Builder             knownMRCs;
    
    private Map<InetSocketAddress, String> mrcAddrMap;
    
    private Object                         syncLock            = new Object();
    
    /**
     * Thread shuts down if true.
     */
    private boolean                        quit                = false;
    
    /**
     * Reference to the MRCRequestDispatcher.
     */
    private MRCRequestDispatcher           master;
    
    public MRCStatusManager(MRCRequestDispatcher master) throws IOException {
        
        super("MRCStatusManager");
        
        this.master = master;
        
        knownMRCs = ServiceSet.newBuilder();
        mrcAddrMap = new HashMap<InetSocketAddress, String>();
        
        int interval = master.getConfig().getOsdCheckInterval();
        checkIntervalMillis = 1000 * interval;
    }
    
    /**
     * Shuts down the thread.
     */
    public synchronized void shutdown() {
        quit = true;
        this.interrupt();
        this.notifyAll();
    }
    
    /**
     * Main loop.
     */
    public void run() {
        
        // initially fetch the list of MRCs from the Directory Service
        try {
            
            knownMRCs = master
                    .getDirClient()
                    .xtreemfs_service_get_by_type(null, RPCAuthentication.authNone, RPCAuthentication.userService,
                            ServiceType.SERVICE_TYPE_MRC).toBuilder();
            
        } catch (Throwable exc) {
            this.notifyCrashed(exc);
        }
        
        notifyStarted();
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                    "MRC status manager operational, using DIR %s", master.getConfig().getDirectoryService().toString());
        
        while (!quit) {
            
            synchronized (this) {
                try {
                    this.wait(knownMRCs == null || knownMRCs.getServicesCount() == 0 ? checkIntervalMillis / 2
                            : checkIntervalMillis);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "sending request for MRC list to DIR...");
            
            try {
                // request list of registered MRCs from Directory
                // Service
                
                knownMRCs = master
                        .getDirClient()
                        .xtreemfs_service_get_by_type(null, RPCAuthentication.authNone, RPCAuthentication.userService,
                                ServiceType.SERVICE_TYPE_MRC).toBuilder();
                
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "... received MRC list from DIR");
                
                evaluateResponse(knownMRCs);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Exception exc) {
                if (!quit)
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, OutputUtils.stackTraceToString(exc));
            }
            
            synchronized (syncLock) {
                syncLock.notifyAll();
            }
            
        }
        
        notifyStopped();
    }
    
    public synchronized void evaluateResponse(ServiceSet.Builder knownMRCs) {
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "response...");
        
        assert (knownMRCs != null);
        assert (knownMRCs.getServicesCount() != 0);
        
        if (Logging.isDebug()) {
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "registered MRCs");
            for (Service mrc : knownMRCs.getServicesList()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "%s", mrc.getUuid());
            }
        }
        
        // update the list
        this.knownMRCs = knownMRCs;
        mrcAddrMap.clear();
        for (Service mrc : knownMRCs.getServicesList()) {
            String endpoint = KeyValuePairs.getValue(mrc.getData().getDataList(), "babudbReplAddr");
            if (endpoint != null) {
                int index = endpoint.indexOf(':');
                if (index != -1)
                    mrcAddrMap.put(
                            new InetSocketAddress(endpoint.substring(0, index), Integer.parseInt(endpoint
                                    .substring(index + 1))), mrc.getUuid());
            }
        }
    }
    
    public void waitForNextSync(boolean immediately) throws InterruptedException {
        
        synchronized (this) {
            if (immediately)
                notify();
        }
        
        synchronized (syncLock) {
            syncLock.wait();
        }
    }
    
    public synchronized String getUUIDForReplHost(InetSocketAddress host) {
        return mrcAddrMap.get(host);
    }
    
}
