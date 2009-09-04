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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.include.common.logging.Logging;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.mrc.metadata.XLocList;

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
                                                .intValue();
    
    public GroupDCMapPolicy() {
    }
    
    public GroupDCMapPolicy(Properties p) {
        super(p);
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList currentXLoc, int numOSDs) {
        
        allOSDs = getOSDs(allOSDs);
        
        // find the closest group to the client that is large enough
        int currentDC = 0;
        int currentDCSize = 0;
        int currentIndex = 0;
        
        int bestClientDist = Integer.MAX_VALUE;
        int bestIndex = -1;
        
        for (int i = 0; i < allOSDs.size(); i++) {
            
            try {
                Service s = allOSDs.get(i);
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
        
        ServiceSet result = new ServiceSet();
        
        if (bestIndex != -1)
            for (int i = 0; i < numOSDs; i++)
                result.add(allOSDs.get(bestIndex + i));
        
        return result;
        
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs) {
        
        // sort the list by their data centers
        Collections.sort(allOSDs, new Comparator<Service>() {
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
        
        return allOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }
    
}
