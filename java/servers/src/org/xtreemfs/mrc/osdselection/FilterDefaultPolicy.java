/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Filters all those OSDs that haven't been assigned to the current XLoc list
 * yet, have recently sent a heartbeat signal and have sufficient space.
 * 
 * @author stender
 */
public class FilterDefaultPolicy implements OSDSelectionPolicy {
    
    public static final short   POLICY_ID           = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT
                                                            .getNumber();
    
    private static final String OFFLINE_TIME_SECS   = "offline_time_secs";
    
    private static final String FREE_CAPACITY_BYTES = "free_capacity_bytes";
    
    // default: 2GB
    private long                minFreeCapacity     = 2 * 1024 * 1024 * 1024;
    
    // default: 5 min
    private long                maxOfflineTime      = 300;
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs, InetAddress clientIP, VivaldiCoordinates clientCoords,
        XLocList currentXLoc, int numOSDs) {
        
        if (allOSDs == null)
            return null;
        
        // first, remove all OSDs from the set that have already been assigned
        // to the current XLoc list
        ServiceSet.Builder osds = PolicyHelper.removeUsedOSDs(allOSDs, currentXLoc);
        
        return getOSDs(osds);
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        
        if (allOSDs == null)
            return null;
        
        ServiceSet.Builder filteredOSDs = ServiceSet.newBuilder();
        for (Service osd : allOSDs.getServicesList())
            if (!hasTimedOut(osd) && hasFreeCapacity(osd) && isAvailable(osd))
                filteredOSDs.addServices(osd);
        
        return filteredOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        if (OFFLINE_TIME_SECS.equals(key))
            maxOfflineTime = Long.parseLong(value);
        else if (FREE_CAPACITY_BYTES.equals(key))
            minFreeCapacity = Long.parseLong(value);
    }
    
    private boolean hasTimedOut(Service osd) {
        long lastUpdate = Long.parseLong(KeyValuePairs.getValue(osd.getData().getDataList(),
            "seconds_since_last_update"));
        return lastUpdate > maxOfflineTime;
    }
    
    private boolean hasFreeCapacity(Service osd) {
        String freeStr = KeyValuePairs.getValue(osd.getData().getDataList(), "free");
        if (freeStr == null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "invalid OSD registry (free is null!): %s", osd.toString());
            }
            return false;
        }
        long free = Long.parseLong(freeStr);
        return free > minFreeCapacity;
    }
    
    private boolean isAvailable(Service osd) {
        String osdStatus = KeyValuePairs.getValue(osd.getData().getDataList(), HeartbeatThread.STATUS_ATTR);
        if (osdStatus == null)
            return true;
        if (Integer.valueOf(osdStatus) == ServiceStatus.SERVICE_STATUS_AVAIL.getNumber()) {
            return true;
        } else {
            return false;
        }
    }
    
}
