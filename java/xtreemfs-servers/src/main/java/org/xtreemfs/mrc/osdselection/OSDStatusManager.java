/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Checks regularly for suitable OSDs for each volume.
 * 
 * @author bjko
 */
public class OSDStatusManager extends LifeCycleThread implements VolumeChangeListener {
    
    /**
     * Interval in ms to wait between two checks.
     */
    private int                                checkIntervalMillis = 1000 * 5;
    
    /**
     * A list of volumes registered with the thread.
     */
    private final Map<String, VolumeOSDFilter> volumeMap;
    
    /**
     * The latest set of all known OSDs fetched from the Directory Service.
     */
    private ServiceSet.Builder                 knownOSDs;
    
    /**
     * A map containing all known OSDs sorted by their UUIDs.
     */
    private final Map<String, Service>               knownOSDMap;
    
    /**
     * Thread shuts down if true.
     */
    private boolean                            quit                = false;
    
    /**
     * Reference to the MRCRequestDispatcher.
     */
    private final MRCRequestDispatcher               master;
    
    public OSDStatusManager(MRCRequestDispatcher master) throws IOException {
        
        super("OSDStatusManager");
        
        this.master = master;
        
        volumeMap = new HashMap<String, VolumeOSDFilter>();
        knownOSDs = ServiceSet.newBuilder();
        knownOSDMap = new HashMap<String, Service>();
        
        int interval = master.getConfig().getOsdCheckInterval();
        checkIntervalMillis = 1000 * interval;
    }
    
    @Override
    public synchronized void volumeChanged(VolumeInfo volume) {
        
        final String volId = volume.getId();
        VolumeOSDFilter vol = volumeMap.get(volId);
        
        if (vol == null) {
            vol = new VolumeOSDFilter(master, knownOSDMap);
            volumeMap.put(volId, vol);
        }
        
        try {
            vol.init(volume);
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
        }
        
        this.notifyAll();
    }
    
    @Override
    public synchronized void volumeDeleted(String volumeId) {
        volumeMap.remove(volumeId);
    }
    
    @Override
    public synchronized void attributeSet(String volumeId, String key, String value) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        
        if (vol == null) {
            Logging.logError(Logging.LEVEL_ERROR, this, new Exception(
                "no volume OSD filter found for volume " + volumeId));
            return;
        }
        
        vol.setAttribute(key, value);
        
    }
    
    /**
     * Shuts down the thread.
     */
    @Override
    public synchronized void shutdown() {
        quit = true;
        this.interrupt();
        this.notifyAll();
    }
    
    /**
     * Main loop.
     */
    @Override
    public void run() {
        
        // initially fetch the list of OSDs from the Directory Service
        try {
            knownOSDs = master.getDirClient().xtreemfs_service_get_by_type(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, ServiceType.SERVICE_TYPE_OSD).toBuilder();
        } catch (Throwable exc) {
            this.notifyCrashed(exc);
        }
        
        notifyStarted();
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                "OSD status manager operational, using DIR %s", master.getConfig().getDirectoryService()
                        .toString());
        
        while (!quit) {
            
            synchronized (this) {
                try {
                    this
                            .wait(knownOSDs == null || knownOSDs.getServicesCount() == 0 ? checkIntervalMillis / 2
                                : checkIntervalMillis);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                "sending request for OSD list to DIR...");
            
            try {
                // request list of registered OSDs from Directory
                // Service
                knownOSDs = master.getDirClient().xtreemfs_service_get_by_type(null, RPCAuthentication.authNone,
                            RPCAuthentication.userService, ServiceType.SERVICE_TYPE_OSD).toBuilder();
                
                Logging
                        .logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "... received OSD list from DIR");
                
                evaluateResponse(knownOSDs);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Exception exc) {
                if (!quit)
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, OutputUtils
                            .stackTraceToString(exc));
            }
            
        }
        
        notifyStopped();
    }
    
    /**
     * Returns the list of usable OSDs for the given volume id.
     * 
     * @param volumeId
     *            the volume id
     * @param clientIP
     *            the client's IP address
     * @param currentXLoc
     *            the file's current XLoc list
     * @param numOSDs
     *            the number of requested OSDs
     * @return a list of feasible OSDs
     */
    public synchronized ServiceSet.Builder getUsableOSDs(String volumeId, InetAddress clientIP,
        VivaldiCoordinates clientCoords, XLocList currentXLoc, int numOSDs) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // return a set of OSDs
        ServiceSet.Builder result = vol.filterByOSDSelectionPolicy(knownOSDs, clientIP, clientCoords,
            currentXLoc, numOSDs);
        
        return result;
    }
    
    public synchronized ServiceSet.Builder getUsableOSDs(String volumeId) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // return a set of OSDs
        return vol.filterByOSDSelectionPolicy(knownOSDs);
    }
    
    public synchronized Replicas getSortedReplicaList(String volumeId, InetAddress clientIP,
        VivaldiCoordinates clientCoords, List<Replica> repls, XLocList xLocList) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // return a sorted set of replicas
        return vol.sortByReplicaSelectionPolicy(clientIP, clientCoords, repls, xLocList);
        
    }
    
    public synchronized void evaluateResponse(ServiceSet.Builder knownOSDs) {
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "response...");
        
        assert (knownOSDs != null);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "registered OSDs");
        if (knownOSDs.getServicesCount() == 0)
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "there are currently no OSDs available");
        if (Logging.isDebug())
            for (Service osd : knownOSDs.getServicesList()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "%s", osd.getUuid());
            }
        
        // update the list of known OSDs
        this.knownOSDs = knownOSDs;
        knownOSDMap.clear();
        for (Service osd : knownOSDs.getServicesList())
            knownOSDMap.put(osd.getUuid(), osd);
    }
    
    public synchronized Service getOSDService(String uuid) {
        return knownOSDMap.get(uuid);
    }
    
    /**
     * Returns the approximate amount of free space in the given volume.
     * 
     * @param volumeId
     *            the ID of the volume
     * 
     * @return the approximate number of free bytes in the volume
     * 
     */
    public long getFreeSpace(String volumeId) {
        
        long free = 0;
        
        ServiceSet.Builder usableOSDs = getUsableOSDs(volumeId);
        if (usableOSDs == null)
            return 0;
        
        for (Service entry : usableOSDs.getServicesList()) {
            String freeStr = KeyValuePairs.getValue(entry.getData().getDataList(), "free");
            if (freeStr != null)
                free += Long.valueOf(freeStr);
        }
        return free;
    }

    /**
     * Returns the approximate amount of usable space in the given volume.
     *
     * @param volumeId
     *            the ID of the volume
     *
     * @return the approximate number of usable bytes in the volume by non-privileged users
     *
     */
    public long getUsableSpace(String volumeId) {

        long usable = 0;

        ServiceSet.Builder usableOSDs = getUsableOSDs(volumeId);
        if (usableOSDs == null)
            return 0;

        for (Service entry : usableOSDs.getServicesList()) {
            String usableStr = KeyValuePairs.getValue(entry.getData().getDataList(), "usable");
            if (usableStr != null)
                usable += Long.valueOf(usableStr);
        }
        return usable;
    }
    
    public long getTotalSpace(String volumeId) {
        
        long total = 0;
        
        ServiceSet.Builder usableOSDs = getUsableOSDs(volumeId);
        if (usableOSDs == null)
            return 0;
        
        for (Service entry : usableOSDs.getServicesList()) {
            String totalStr = KeyValuePairs.getValue(entry.getData().getDataList(), "total");
            if (totalStr != null)
                total += Long.valueOf(totalStr);
        }
        return total;
    }
    
}
