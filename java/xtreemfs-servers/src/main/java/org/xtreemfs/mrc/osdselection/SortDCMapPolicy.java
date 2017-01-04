/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.Properties;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Sorts the list of OSDs in ascending order of their distance to the client.
 * The distance needs to be defined in the datacenter map.
 * 
 * @author bjko, stender
 */
public class SortDCMapPolicy extends DCMapPolicyBase {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_DCMAP
                                                .getNumber();
    
    public SortDCMapPolicy() {
    }
    
    public SortDCMapPolicy(Properties p) {
        super(p);
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path) {
        
        final Inet4Address cAddr = (Inet4Address) clientIP;
        
        if (allOSDs != null) {
            allOSDs = PolicyHelper.sortServiceSet(allOSDs, new Comparator<Service>() {
                public int compare(Service o1, Service o2) {
                    try {
                        ServiceUUID uuid1 = new ServiceUUID(o1.getUuid());
                        ServiceUUID uuid2 = new ServiceUUID(o2.getUuid());
                        Inet4Address osdAddr1 = (Inet4Address) uuid1.getAddress().getAddress();
                        Inet4Address osdAddr2 = (Inet4Address) uuid2.getAddress().getAddress();
                        
                        return getDistance(osdAddr1, cAddr) - getDistance(osdAddr2, cAddr);
                        
                    } catch (UnknownUUIDException e) {
                        Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, "cannot compare UUIDs");
                        Logging.logMessage(Logging.LEVEL_WARN, this, OutputUtils.stackTraceToString(e));
                        return 0;
                    }
                }
            });
        }
        
        return allOSDs;
        
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        return allOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }
    
}
