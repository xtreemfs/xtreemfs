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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

import org.xtreemfs.common.LRUCache;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Base class for policies that use datacenter maps.
 * 
 * @author bjko, stender
 */
public abstract class DCMapPolicyBase implements OSDSelectionPolicy {
    
    public static final String                CONFIG_FILE_PATH = "/etc/xos/xtreemfs/datacentermap";
    
    protected int[][]                         distMap;
    
    protected Inet4AddressMatcher[][]         matchers;
    
    protected LRUCache<Inet4Address, Integer> matchingDCcache;
    
    public DCMapPolicyBase() {
        try {
            File f = new File(CONFIG_FILE_PATH);
            Properties p = new Properties();
            p.load(new FileInputStream(f));
            readConfig(p);
            int maxCacheSize = Integer.valueOf(p.getProperty("max_cache_size", "1000"));
            matchingDCcache = new LRUCache<Inet4Address, Integer>(maxCacheSize);
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "cannot load %s, DCMap replica selection policy will not work", CONFIG_FILE_PATH);
        }
        
    }
    
    public DCMapPolicyBase(Properties p) {
        readConfig(p);
        int maxCacheSize = Integer.valueOf(p.getProperty("max_cache_size", "1000"));
        matchingDCcache = new LRUCache<Inet4Address, Integer>(maxCacheSize);
    }
    
    public abstract ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList currentXLoc,
        int numOSDs);
    
    public abstract void setAttribute(String key, String value);
    
    private void readConfig(Properties p) {
        
        String tmp = p.getProperty("datacenters");
        if (tmp == null) {
            throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName()
                + ": a list of datacenters must be specified in the configuration");
        }
        
        final String[] dcs = tmp.split(",");
        if (dcs.length == 0) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "no datacenters specified");
            return;
        }
        
        for (int i = 0; i < dcs.length; i++) {
            if (!dcs[i].matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName()
                    + ": datacenter name '" + dcs[i] + "' is invalid");
            }
        }
        
        distMap = new int[dcs.length][dcs.length];
        matchers = new Inet4AddressMatcher[dcs.length][];
        for (int i = 0; i < dcs.length; i++) {
            for (int j = i + 1; j < dcs.length; j++) {
                String distStr = p.getProperty("distance." + dcs[i] + "-" + dcs[j]);
                if (distStr == null) {
                    distStr = p.getProperty("distance." + dcs[j] + "-" + dcs[i]);
                }
                if (distStr == null) {
                    throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName()
                        + ": distance between datacenter '" + dcs[i] + "' and '" + dcs[j]
                        + "' is not specified");
                }
                try {
                    distMap[i][j] = distMap[j][i] = Integer.valueOf(distStr);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName()
                        + ": distance '" + distStr + "' between datacenter '" + dcs[i] + "' and '" + dcs[j]
                        + "' is not a valid integer");
                }
            }
            String dcMatch = p.getProperty(dcs[i] + ".addresses");
            if (dcMatch != null) {
                String entries[] = dcMatch.split(",");
                matchers[i] = new Inet4AddressMatcher[entries.length];
                for (int e = 0; e < entries.length; e++) {
                    final String entry = entries[e];
                    if (entry.contains("/")) {
                        // network match
                        try {
                            String ipAddr = entry.substring(0, entry.indexOf("/"));
                            String prefix = entry.substring(entry.indexOf("/") + 1);
                            Inet4Address ia = (Inet4Address) InetAddress.getByName(ipAddr);
                            matchers[i][e] = new Inet4AddressMatcher(ia, Integer.valueOf(prefix));
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Cannot initialize "
                                + this.getClass().getSimpleName() + ": netmask in '" + entry
                                + "' for datacenter '" + dcs[i] + "' is not a valid integer");
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("Cannot initialize "
                                + this.getClass().getSimpleName() + ": address '" + entry
                                + "' for datacenter '" + dcs[i] + "' is not a valid IPv4 address");
                        }
                    } else {
                        // IP match
                        try {
                            Inet4Address ia = (Inet4Address) InetAddress.getByName(entry);
                            matchers[i][e] = new Inet4AddressMatcher(ia);
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("Cannot initialize "
                                + this.getClass().getSimpleName() + ": address '" + entry
                                + "' for datacenter '" + dcs[i] + "' is not a valid IPv4 address");
                        }
                    }
                }
                
            } else {
                // allow empty datacenters
                Logging.logMessage(Logging.LEVEL_WARN, this, "datacenter '" + dcs[i] + "' has no entries!");
            }
        }
        
    }
    
    protected int getDistance(Inet4Address addr1, Inet4Address addr2) {
        final int dc1 = getMatchingDC(addr1);
        final int dc2 = getMatchingDC(addr2);
        
        if ((dc1 != -1) && (dc2 != -1)) {
            return distMap[dc1][dc2];
        } else {
            return Integer.MAX_VALUE;
        }
    }
    
    protected int getMatchingDC(Inet4Address addr) {
        Integer cached = matchingDCcache.get(addr);
        if (cached == null) {
            for (int i = 0; i < matchers.length; i++) {
                for (int j = 0; j < matchers[i].length; j++) {
                    if (matchers[i][j].matches(addr)) {
                        matchingDCcache.put(addr, i);
                        return i;
                    }
                }
            }
            matchingDCcache.put(addr, -1);
            return -1;
        } else {
            return cached;
        }
    }
    
}
