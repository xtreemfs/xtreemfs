/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

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
                                                  .getNumber();
    
    private static final String UUIDS     = "uuids";
    
    private List<String>        uuids     = new LinkedList<String>();
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs, InetAddress clientIP,
        VivaldiCoordinates clientCoords, XLocList currentXLoc, int numOSDs) {
        return getOSDs(allOSDs);
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        
        if (allOSDs == null)
            return null;
        
        ServiceSet.Builder filteredOSDs = ServiceSet.newBuilder();
        for (Service osd : allOSDs.getServicesList())
            if (isInUUIDs(osd))
                filteredOSDs.addServices(osd);
        
        return filteredOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        
        if (key.equals(UUIDS)) {
            
            uuids.clear();
            
            if (value != null) {
                StringTokenizer st = new StringTokenizer(value, " ,;\t\n");
                while (st.hasMoreTokens())
                    uuids.add(st.nextToken());
            }
        }
    }
    
    private boolean isInUUIDs(Service osd) {
        
        final String osdUUID = new ServiceUUID(osd.getUuid()).toString();
        
        if (uuids.size() == 0)
            return true;
        
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
