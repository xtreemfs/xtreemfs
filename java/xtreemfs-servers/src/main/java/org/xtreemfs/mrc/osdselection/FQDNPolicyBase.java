/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for policies that use datacenter maps.
 * 
 * @author bjko, stender
 */
public abstract class FQDNPolicyBase implements OSDSelectionPolicy {
    
    /**
     * Counts the number of consecutive matching components in the given domain
     * names, starting at the last component.
     * 
     * @param dn1
     *            the first domain name
     * @param dn2
     *            the second domain name
     * @return the number of consecutive matching components, starting at the
     *         last component
     */
    protected int getMatch(String dn1, String dn2) {
        
        String[] array1 = tokenizeAndReverseDN(dn1);
        String[] array2 = tokenizeAndReverseDN(dn2);
        
        int len = Math.min(array1.length, array2.length);
        
        int match = 0;
        for (int i = 0; i < len; i++) {
            if (array1[i].equals(array2[i]))
                match++;
            else
                break;
        }
        
        return match;
    }
    
    private static String[] tokenizeAndReverseDN(String dn) {
        
        List<Integer> dots = new ArrayList<Integer>();
        
        // determine the positions of all dots
        for (int i = 0; i < dn.length(); i++) {
            if (dn.charAt(i) == '.')
                dots.add(i);
        }
        
        String[] array = new String[dots.size() + 1];
        for (int i = 0; i < array.length; i++) {
            int currDot = i >= dots.size() ? dn.length() : dots.get(i);
            int prevDot = i == 0 ? -1 : dots.get(i - 1);
            array[array.length - i - 1] = dn.substring(prevDot + 1, currDot);
        }
        
        return array;
    }
    
}
