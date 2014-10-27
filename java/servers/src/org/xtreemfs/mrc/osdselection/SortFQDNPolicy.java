/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.Comparator;

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
 * The distance is determined by means of the server's and client's FQDNs.
 * 
 * @author bjko, stender
 */
public class SortFQDNPolicy extends FQDNPolicyBase {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_FQDN
                                                .getNumber();
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs, final InetAddress clientIP,
        VivaldiCoordinates clientCoords, XLocList currentXLoc, int numOSDs) {
        
        if (allOSDs == null)
            return null;
        
        allOSDs = PolicyHelper.sortServiceSet(allOSDs, new Comparator<Service>() {
            public int compare(Service o1, Service o2) {
                try {
                    return getMatch(new ServiceUUID(o2.getUuid()).getAddress().getHostName(), clientIP
                            .getCanonicalHostName())
                        - getMatch(new ServiceUUID(o1.getUuid()).getAddress().getHostName(), clientIP
                                .getCanonicalHostName());
                } catch (UnknownUUIDException e) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, "cannot compare FQDNs");
                    Logging.logMessage(Logging.LEVEL_WARN, this, OutputUtils.stackTraceToString(e));
                    return 0;
                }
            }
        });
        
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
