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

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Sorts the list of OSDs in ascending order of their distance to the client.
 * The distance needs to be defined in the datacenter map.
 * 
 * @author bjko, stender
 */
public class SortDCMapPolicy extends DCMapPolicyBase {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_DCMAP
                                                .intValue();
    
    public SortDCMapPolicy() {
    }
    
    public SortDCMapPolicy(Properties p) {
        super(p);
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList currentXLoc, int numOSDs) {
        
        final Inet4Address cAddr = (Inet4Address) clientIP;
        
        Collections.sort(allOSDs, new Comparator<Service>() {
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
        
        return allOSDs;
        
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs) {
        return allOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }
    
}
