/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

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
    
    private boolean initialized;
    
    public DCMapPolicyBase() {
        try {
            File f = new File(CONFIG_FILE_PATH);
            Properties p = new Properties();
            InputStream fileInputStream = new FileInputStream(f);
            p.load(fileInputStream);
            fileInputStream.close();
            readConfig(p);
            int maxCacheSize = Integer.valueOf(p.getProperty("max_cache_size", "1000").trim());
            matchingDCcache = new LRUCache<Inet4Address, Integer>(maxCacheSize);
            initialized = true;
            
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "cannot load %s, DCMap replica selection policy will not work", CONFIG_FILE_PATH);
            Logging.logError(Logging.LEVEL_WARN, this, ex);
            initialized = false;
        }
        
    }
    
    public DCMapPolicyBase(Properties p) {
        readConfig(p);
        int maxCacheSize = Integer.valueOf(p.getProperty("max_cache_size", "1000"));
        matchingDCcache = new LRUCache<Inet4Address, Integer>(maxCacheSize);
        initialized = true;
    }
        
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
            
            // evaluate the address ranges for the data centers
            String dcMatch = p.getProperty("addresses." + dcs[i]);
            if (dcMatch == null)
                dcMatch = p.getProperty(dcs[i] + ".addresses");
            if (dcMatch == null) {
                matchers[i] = new Inet4AddressMatcher[0];
                // allow empty datacenters
                Logging.logMessage(Logging.LEVEL_WARN, this, "datacenter '" + dcs[i] + "' has no entries!");
            }
            
            else {
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
                
            }
        }
        
    }
    
    protected int getDistance(Inet4Address addr1, Inet4Address addr2) {
        
        if(!initialized)
            return 0;
        
        final int dc1 = getMatchingDC(addr1);
        final int dc2 = getMatchingDC(addr2);
        
        if ((dc1 != -1) && (dc2 != -1)) {
            return distMap[dc1][dc2];
        } else {
            return Integer.MAX_VALUE;
        }
    }
    
    protected int getMatchingDC(Inet4Address addr) {
        
        if(!initialized)
            return -1;
        
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
