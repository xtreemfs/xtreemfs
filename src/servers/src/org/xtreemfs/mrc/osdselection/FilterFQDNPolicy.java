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
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.mrc.metadata.XLocList;

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
                                                  .intValue();
    
    private static final String DOMAINS   = "domains";
    
    private List<String>        domains   = new LinkedList<String>();
    
    {
        // default: all domains match
        domains.add("*");
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
            if (isInDomains(osd))
                filteredOSDs.add(osd);
        
        return filteredOSDs;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        if (key.equals(DOMAINS)) {
            domains.clear();
            StringTokenizer st = new StringTokenizer(value, " ,;\t\n");
            while (st.hasMoreTokens())
                domains.add(st.nextToken());
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
