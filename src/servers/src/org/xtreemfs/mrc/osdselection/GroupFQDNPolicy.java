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

import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.include.common.logging.Logging;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Determines a subgroup of OSDs that can be assigned to a new replica, by means
 * of the fully qualified distinguished server names. The selection will be
 * restricted to OSDs that are located in the same domain. If multiple such
 * possible subgroups exist, the one closest to the client will be returned.
 * 
 * @author bjko, stender
 */
public class GroupFQDNPolicy extends FQDNPolicyBase {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_GROUP_FQDN
                                                .intValue();
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList currentXLoc, int numOSDs) {
        
        // second, sort the remaining list by their FQDNs
        allOSDs = getOSDs(allOSDs);
        
        // third, find the closest group to the client that is large enough
        String currentDomain = "";
        int currentDomainSize = 0;
        int currentIndex = 0;
        
        int bestClientDist = Integer.MAX_VALUE;
        int bestIndex = -1;
        
        for (int i = 0; i < allOSDs.size(); i++) {
            
            try {
                Service s = allOSDs.get(i);
                Inet4Address addr = (Inet4Address) new ServiceUUID(s.getUuid()).getAddress().getAddress();
                String domain = NetUtils.getDomain(addr.getHostName());
                
                if (domain.equals(currentDomain)) {
                    
                    currentDomainSize++;
                    if (currentDomainSize == numOSDs) {
                        int cd = getMatch(addr.getCanonicalHostName(), clientIP.getCanonicalHostName());
                        if (cd < bestClientDist) {
                            bestClientDist = cd;
                            bestIndex = currentIndex;
                        }
                    }
                }

                else {
                    currentDomainSize = 1;
                    currentDomain = domain;
                    currentIndex = i;
                }
                
            } catch (UnknownUUIDException exc) {
                Logging.logError(Logging.LEVEL_ERROR, this, exc);
                break;
            }
            
        }
        
        ServiceSet result = new ServiceSet();
        
        if (bestIndex != -1)
            for (int i = 0; i < numOSDs; i++)
                result.add(allOSDs.get(bestIndex + numOSDs));
        
        return result;
        
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs) {
        
        Collections.sort(allOSDs, new Comparator<Service>() {
            public int compare(Service o1, Service o2) {
                
                try {
                    final String host1 = new ServiceUUID(o1.getUuid()).getAddress().getAddress()
                            .getCanonicalHostName();
                    final String host2 = new ServiceUUID(o2.getUuid()).getAddress().getAddress()
                            .getCanonicalHostName();
                    
                    return host1.compareTo(host2);
                    
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
