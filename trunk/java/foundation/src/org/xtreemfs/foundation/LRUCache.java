/*
 * Copyright (c) 2008-2010 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  This class implements a LRU cache
 *
 *  @author jmalo
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {
    private static final long serialVersionUID = -4673214355284364245L;
    private int maximumSize;
    
    /** Creates a new instance of LRUCache */
    public LRUCache(int size) {
        super(size, (float)0.75, true);
        
        maximumSize = size;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maximumSize;
    }
}
