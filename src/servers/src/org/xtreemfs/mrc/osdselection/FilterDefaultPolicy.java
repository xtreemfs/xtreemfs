/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

import java.net.InetAddress;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Filters all those OSDs that haven't been assigned to the current XLoc list
 * yet, have recently sent a heartbeat signal and have sufficient space.
 * 
 * @author stender
 */
public class FilterDefaultPolicy implements OSDSelectionPolicy {
    
    public static final short   POLICY_ID           = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT
                                                            .intValue();
    
    private static final String OFFLINE_TIME_SECS   = "offline_time_secs";
    
    private static final String FREE_CAPACITY_BYTES = "free_capacity_bytes";
    
    // default: 2GB
    private long                minFreeCapacity     = 2 * 1024 * 1024 * 1024;
    
    // default: 5 min
    private long                maxOfflineTime      = 300;
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList currentXLoc, int numOSDs) {
        
        // first, remove all OSDs from the set that have already been assigned
        // to the current XLoc list
        PolicyHelper.removeUsedOSDs(allOSDs, currentXLoc);
        
        return getOSDs(allOSDs);
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs) {
        
        ServiceSet filteredOSDs = new ServiceSet();
        for (Service osd : allOSDs)
            if (!hasTimedOut(osd) && hasFreeCapacity(osd))
                filteredOSDs.add(osd);
        
        return filteredOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        if (OFFLINE_TIME_SECS.equals(key))
            maxOfflineTime = Long.parseLong(value) * 1000;
        else if (FREE_CAPACITY_BYTES.equals(key))
            minFreeCapacity = Long.parseLong(value);
    }
    
    private boolean hasTimedOut(Service osd) {
        long updateTimestamp = osd.getLast_updated_s();
        long currentTime = TimeSync.getGlobalTime() / 1000;
        return currentTime - updateTimestamp > maxOfflineTime;
    }
    
    private boolean hasFreeCapacity(Service osd) {
        String freeStr = osd.getData().get("free");
        long free = Long.parseLong(freeStr);
        return free > minFreeCapacity;
    }
    
}
