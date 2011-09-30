/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

/**
 * Base class for policies that use datacenter maps.
 * 
 * @author bjko, stender
 */
public abstract class FQDNPolicyBase implements OSDSelectionPolicy {
    
    protected int getMatch(String host, String name) {
        
        final int minLen = Math.min(host.length(), name.length());
        int hostI = host.length() - 1;
        int nameI = name.length() - 1;
        int match = 0;
        for (int i = minLen - 1; i > 0; i--) {
            if (host.charAt(hostI--) != name.charAt(nameI--)) {
                break;
            }
            match++;
        }
        
        return match;
    }
    
}
