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
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Filters all OSDs that have a matching UUID. UUIDs are specified via an
 * extended attribute value, as defined in UUIDs. Attribute values may contain
 * '*'s to indicate that parts of the UUID also match. By default, the list of
 * UUIDs contains '*' to indicate that any UUID matches.
 * 
 * @author stender
 */
public class FilterUUIDPolicy implements OSDSelectionPolicy {
    
    public static final short   POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_UUID
                                                  .intValue();
    
    private static final String UUIDS     = "uuids";
    
    private List<String>        uuids     = new LinkedList<String>();
    
    {
        // default: all domains match
        uuids.add("*");
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, VivaldiCoordinates clientCoords,
        XLocList currentXLoc, int numOSDs) {
        return getOSDs(allOSDs);
    }
    
    @Override
    public ServiceSet getOSDs(ServiceSet allOSDs) {
        
        ServiceSet filteredOSDs = new ServiceSet();
        for (Service osd : allOSDs)
            if (isInUUIDs(osd))
                filteredOSDs.add(osd);
        
        return filteredOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        if (key.equals(UUIDS)) {
            uuids.clear();
            StringTokenizer st = new StringTokenizer(value, " ,;\t\n");
            while (st.hasMoreTokens())
                uuids.add(st.nextToken());
        }
    }
    
    private boolean isInUUIDs(Service osd) {
        
        final String osdUUID = new ServiceUUID(osd.getUuid()).toString();
        
        for (String uuid : uuids) {
            
            if (uuid.endsWith("*") && osdUUID.startsWith(uuid.substring(0, uuid.length() - 1)))
                return true;
            
            if (uuid.startsWith("*") && osdUUID.endsWith(uuid.substring(1, uuid.length())))
                return true;
            
            if (uuid.equals(osdUUID))
                return true;
        }
        
        return false;
        
    }
    
}
