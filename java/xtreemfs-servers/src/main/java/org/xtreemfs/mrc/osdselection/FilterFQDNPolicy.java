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
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Filters all OSDs that have a matching domain. Domains are specified via an
 * extended attribute value, as defined in DOMAINS. Attribute values may contain
 * '*'s to indicate that subdomains also match. By default, the list of domains
 * contains '*' to indicate that any domain matches.
 * 
 * @author stender
 */
public class FilterFQDNPolicy implements OSDSelectionPolicy {
    
    public static final short   POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_FQDN
                                                  .getNumber();
    
    private static final String DOMAINS   = "domains";
    
    private final List<String>        domains   = new LinkedList<String>();
    
    {
        // default: all domains match
        domains.add("*");
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs, InetAddress clientIP, VivaldiCoordinates clientCoords,
        XLocList currentXLoc, int numOSDs) {
        return getOSDs(allOSDs);
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        
        if (allOSDs == null)
            return null;
        
        ServiceSet.Builder filteredOSDs = ServiceSet.newBuilder();
        for (Service osd : allOSDs.getServicesList())
            if (isInDomains(osd))
                filteredOSDs.addServices(osd);
        
        return filteredOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        if (key.equals(DOMAINS)) {
            domains.clear();
            if (value == null) {
                value = "";
            }
            StringTokenizer st = new StringTokenizer(value, " ,;\t\n");
            if (st != null) {
                while (st.hasMoreTokens()) {
                    domains.add(st.nextToken());
                }
            }
        }
    }
    
    private boolean isInDomains(Service osd) {
        
        try {
            
            final ServiceUUID uuid = new ServiceUUID(osd.getUuid());
            final String osdHostName = uuid.getAddress().getHostName();
            
            for (String domain : domains) {
                
                if (domain.endsWith("*") && osdHostName.startsWith(domain.substring(0, domain.length() - 1)))
                    return true;
                
                if (domain.startsWith("*") && osdHostName.endsWith(domain.substring(1, domain.length())))
                    return true;
                
                if (domain.equals(osdHostName))
                    return true;
            }
            
            return false;
            
        } catch (UnknownUUIDException exc) {
            
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, "invalid OSD UUID");
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            
            return false;
        }
    }
    
}
