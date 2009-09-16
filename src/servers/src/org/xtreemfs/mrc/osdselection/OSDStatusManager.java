/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.osdselection;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.XLocList;

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
    private ServiceSet                         knownOSDs;
    
    /**
     * A map containing all known OSDs sorted by their UUIDs.
     */
    private Map<String, Service>               knownOSDMap;
    
    /**
     * Thread shuts down if true.
     */
    private boolean                            quit                = false;
    
    /**
     * Reference to the MRCRequestDispatcher.
     */
    private MRCRequestDispatcher               master;
    
    public OSDStatusManager(MRCRequestDispatcher master) throws IOException {
        
        super("OSDStatusManager");
        
        this.master = master;
        
        volumeMap = new HashMap<String, VolumeOSDFilter>();
        knownOSDs = new ServiceSet();
        knownOSDMap = new HashMap<String, Service>();
        
        int interval = master.getConfig().getOsdCheckInterval();
        checkIntervalMillis = 1000 * interval;
    }
    
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
    
    public synchronized void volumeDeleted(String volumeId) {
        volumeMap.remove(volumeId);
        
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
        
        // initially fetch the list of OSDs from the Directory Service
        RPCResponse<ServiceSet> r = null;
        try {
            r = master.getDirClient().xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_OSD);
            knownOSDs = r.get();
        } catch (Exception exc) {
            this.notifyCrashed(exc);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
        
        notifyStarted();
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                "OSD status manager operational, using DIR %s", master.getConfig().getDirectoryService()
                        .toString());
        
        while (!quit) {
            
            synchronized (this) {
                try {
                    this.wait(checkIntervalMillis);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                "sending request for OSD list to DIR...");
            r = null;
            
            try {
                // request list of registered OSDs from Directory
                // Service
                r = master.getDirClient().xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_OSD);
                knownOSDs = r.get();
                
                Logging
                        .logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "... received OSD list from DIR");
                
                evaluateResponse(knownOSDs);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, OutputUtils
                        .stackTraceToString(exc));
            } finally {
                if (r != null)
                    r.freeBuffers();
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
    public synchronized ServiceSet getUsableOSDs(String volumeId, InetAddress clientIP, XLocList currentXLoc,
        int numOSDs) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // return a set of OSDs
        ServiceSet result = vol.filterByOSDSelectionPolicy(knownOSDs, clientIP, currentXLoc, numOSDs);
        
        if (result.size() == 0) {
            String osds = "";
            for (Service s : knownOSDs)
                osds += s.getUuid() + ", " + s.getData() + " ";
            Logging.logMessage(Logging.LEVEL_WARN, this, "all OSDs: %s", osds);
        }
        
        return result;
    }
    
    public synchronized ServiceSet getUsableOSDs(String volumeId) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // return a set of OSDs
        return vol.filterByOSDSelectionPolicy(knownOSDs);
    }
    
    public synchronized ReplicaSet getSortedReplicaList(String volumeId, InetAddress clientIP,
        XLocList currentXLoc) {
        
        VolumeOSDFilter vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // return a sorted set of replicas
        return vol.sortByReplicaSelectionPolicy(clientIP, currentXLoc);
        
    }
    
    public synchronized void evaluateResponse(ServiceSet knownOSDs) {
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "response...");
        
        assert (knownOSDs != null);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "registered OSDs");
        if (knownOSDs.size() == 0)
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "there are currently no OSDs available");
        if (Logging.isDebug())
            for (Service osd : knownOSDs) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "%s", osd.getUuid());
            }
        
        // update the list of known OSDs
        this.knownOSDs = knownOSDs;
        knownOSDMap.clear();
        for (Service osd : knownOSDs)
            knownOSDMap.put(osd.getUuid(), osd);
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
        
        ServiceSet usableOSDs = getUsableOSDs(volumeId);
        if (usableOSDs == null)
            return 0;
        
        for (Service entry : usableOSDs) {
            String freeStr = entry.getData().get("free");
            if (freeStr != null)
                free += Long.valueOf(freeStr);
        }
        return free;
    }
    
}
