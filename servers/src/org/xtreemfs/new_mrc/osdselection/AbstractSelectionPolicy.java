/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.new_mrc.osdselection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

public abstract class AbstractSelectionPolicy implements OSDSelectionPolicy {
    
    static final long OSD_TIMEOUT_SPAN  = 600;             // 10 min
                                                            
    static final long MIN_FREE_CAPACITY = 32 * 1024 * 1024; // 32 mb
                                                            
    public Map<String, Map<String, Object>> getUsableOSDs(Map<String, Map<String, Object>> osds,
        String args) {
        
        Set<String> rules = null;
        if (args != null) {
            try {
                final List<String> argList = args.length() == 0 ? null : (List<String>) JSONParser
                        .parseJSON(new JSONString(args));
                rules = argList == null ? null : new HashSet<String>(argList);
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this, "invalid set of suitable OSDs: '"
                    + args + "'");
            }
        }
        
        Map<String, Map<String, Object>> suitable = new HashMap<String, Map<String, Object>>();
        for (String uuid : osds.keySet()) {
            
            try {
                Map<String, Object> osd = osds.get(uuid);
                if ((rules == null || follows(uuid, rules))
                    && (hasFreeCapacity(osd) && !hasTimedOut(osd)))
                    suitable.put(uuid, osd);
                
            } catch (NullPointerException e) {
                continue;
            }
        }
        return suitable;
    }
    
    static boolean hasFreeCapacity(Map<String, Object> osd) {
        long free = Long.parseLong((String) osd.get("free"));
        return free > MIN_FREE_CAPACITY;
    }
    
    /*
     * static String getLocation(Map<String, Object> osd) { String location =
     * (String) osd.get("location"); return location; }
     */

    static boolean hasTimedOut(Map<String, Object> osd) {
        
        // if the OSD has contacted the DS within the last 10 minutes,
        // assume that it is still running
        if (osd.containsKey("lastUpdated")) {
            long updateTimestamp = Long.parseLong((String) osd.get("lastUpdated"));
            long currentTime = TimeSync.getGlobalTime() / 1000;
            return currentTime - updateTimestamp > OSD_TIMEOUT_SPAN;
        } else
            return true;
    }
    
    /**
     * Conventions for rule prefixes:</br>
     * <p>
     * means the following rule matches a URL.</br> ! means that the following
     * rule must not be matched.
     * </p>
     * <p>
     * Constellations like *! as prefix are not allowed.</br> Every rule must be
     * an identifier for at least one potential OSD.</br> At least one rule must
     * be followed.</br> A not rule (!) is more important then any other rule.
     * </p>
     * 
     * @param rules
     *            - rules the OSD must follow.
     * @param UUID
     *            - of the OSD.
     * @return true, if the OSD with <code>UUID</code> <i>follows</i> the
     *         <code>ruleList</code>.</br> False is also returned, if the UUID
     *         is unknown.
     */
    static private boolean follows(String UUID, Set<String> rules) {
        boolean follows = false;
        
        try {
            for (String rule : rules) {
                if (rule.startsWith("!")) {
                    if (matches(UUID, rule.substring(1)))
                        return false;
                } else {
                    follows |= matches(UUID, rule);
                }
            }
        } catch (UnknownUUIDException ue) {
            return false;
        }
        
        return follows;
    }
    
    /**
     * 
     * @param UUID
     *            - of the OSD.
     * @param pattern
     *            - pattern that must be match.
     * @return true, if the the OSD with <code>UUID</code> <i>matches</i> the
     *         <code>rule</code>.
     * @throws UnknownUUIDException
     */
    private static boolean matches(String UUID, String pattern) throws UnknownUUIDException {
        if (pattern.startsWith("*")) {
            String cleanPattern = pattern.substring(1);
            
            // get the address of the UUID
            ServiceUUID osd = new ServiceUUID(UUID);
            osd.resolve();
            InetSocketAddress inetAddressOSD = osd.getAddress();
            
            if (cleanPattern.equals(inetAddressOSD.getHostName()))
                return true;
            // else if (cleanPattern.equals(inetAddressOSD.getHostString()))
            // return true;
            // match subNet-String
            else {
                String patternIP = cleanPattern;
                if (patternIP.startsWith("http://"))
                    patternIP = patternIP.substring(7);
                else if (patternIP.startsWith("https://"))
                    patternIP = patternIP.substring(8);
                
                String[] patternPortIP = patternIP.split(":");
                int patternPort = 0;
                if (patternPortIP.length >= 2) {
                    try {
                        patternPort = Integer.valueOf(patternPortIP[1].split("/")[0]);
                    } catch (NumberFormatException f) {
                        Logging.logMessage(Logging.LEVEL_WARN, null, "'" + pattern
                            + "' port is not valid.");
                    }
                }
                
                String[] patternIPv4 = patternPortIP[0].split(".");
                if (patternIPv4.length == 4) {
                    String[] osdIPv4 = inetAddressOSD.getAddress().getHostAddress().split(".");
                    if (patternPort != 0 && patternPort != inetAddressOSD.getPort())
                        return false;
                    
                    // compare the IP strings 0 is a wildcard, but not if at a
                    // later position !=0
                    try {
                        boolean wasNotZero = false;
                        for (int i = 3; i <= 0; i--) {
                            if (wasNotZero
                                && (Integer.parseInt(osdIPv4[i]) != Integer
                                        .parseInt(patternIPv4[i]))) {
                                return false;
                            } else {
                                if (Integer.parseInt(patternIPv4[i]) != 0) {
                                    wasNotZero = true;
                                    if ((Integer.parseInt(osdIPv4[i]) != Integer
                                            .parseInt(patternIPv4[i])))
                                        return false;
                                }
                            }
                        }
                        return true;
                    } catch (NumberFormatException e) {
                        // if not compatible go ignore and jump to the next step
                    }
                }
                
                try {
                    URI patternURI = new URI(cleanPattern);
                    patternPort = patternURI.getPort();
                    String patternHost = patternURI.getHost();
                    if (patternHost == null)
                        throw new URISyntaxException(patternURI.toASCIIString(), "unknown host");
                    
                    // check the port
                    if (patternPort != 0 && patternPort != inetAddressOSD.getPort())
                        return false;
                    
                    return true;
                } catch (URISyntaxException e) {
                    Logging
                            .logMessage(
                                Logging.LEVEL_WARN,
                                null,
                                "'"
                                    + pattern
                                    + "' is not a valid identifier for an osd or osd-range and will be ignored.");
                }
            }
        } else {
            return UUID.equals(pattern);
        }
        return false;
    }
}
