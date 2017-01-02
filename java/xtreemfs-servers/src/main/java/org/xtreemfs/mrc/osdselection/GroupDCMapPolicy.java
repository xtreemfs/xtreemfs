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
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Determines a subgroup of OSDs that can be assigned to new replica, by means
 * of the datacenter map. The selection will be restricted to OSDs that are
 * located in the same datacenter. If multiple such possible subgroups exist,
 * the one closest to the client will be returned.
 * 
 * @author bjko, stender
 */
public class GroupDCMapPolicy extends DCMapPolicyBase {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_GROUP_DCMAP
                                                .getNumber();
    
    public GroupDCMapPolicy() {
    }
    
    public GroupDCMapPolicy(Properties p) {
        super(p);
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path) {
        
        allOSDs = getOSDs(allOSDs);
        
        // find the closest group to the client that is large enough
        int currentDC = 0;
        int currentDCSize = 0;
        int currentIndex = 0;
        
        int bestClientDist = Integer.MAX_VALUE;
        int bestIndex = -1;
        
        for (int i = 0; i < allOSDs.getServicesCount(); i++) {
            
            try {
                Service s = allOSDs.getServices(i);
                Inet4Address addr = (Inet4Address) new ServiceUUID(s.getUuid()).getAddress().getAddress();
                final int dc = getMatchingDC(addr);
                
                if (dc == currentDC) {
                    
                    currentDCSize++;
                    if (currentDCSize == numOSDs) {
                        int cd = getDistance(addr, (Inet4Address) clientIP);
                        if (cd < bestClientDist) {
                            bestClientDist = cd;
                            bestIndex = currentIndex;
                        }
                    }
                }

                else {
                    currentDCSize = 1;
                    currentDC = dc;
                    currentIndex = i;
                    
                    if (currentDCSize == numOSDs) {
                        int cd = getDistance(addr, (Inet4Address) clientIP);
                        if (cd < bestClientDist) {
                            bestClientDist = cd;
                            bestIndex = currentIndex;
                        }
                    }
                }
                
            } catch (UnknownUUIDException exc) {
                Logging.logError(Logging.LEVEL_ERROR, this, exc);
                break;
            }
            
        }
        
        ServiceSet.Builder result = ServiceSet.newBuilder();
        
        if (bestIndex != -1)
            for (int i = 0; i < numOSDs; i++)
                result.addServices(allOSDs.getServices(bestIndex + i));
        
        return result;
        
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        
        // sort the list by their data centers
        if (allOSDs != null) {
            allOSDs = PolicyHelper.sortServiceSet(allOSDs, new Comparator<Service>() {
                public int compare(Service o1, Service o2) {
                    
                    try {
                        Inet4Address addr1 = (Inet4Address) new ServiceUUID(o1.getUuid()).getAddress()
                                .getAddress();
                        Inet4Address addr2 = (Inet4Address) new ServiceUUID(o2.getUuid()).getAddress()
                                .getAddress();
                        
                        return getMatchingDC(addr1) - getMatchingDC(addr2);
                        
                    } catch (UnknownUUIDException exc) {
                        Logging.logError(Logging.LEVEL_ERROR, this, exc);
                        return 0;
                    }
                }
            });
        }
        
        return allOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }
    
}
