/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.util.Map;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.LRUCache;

/**
 *  This class implements a cache for Locations
 *
 *  @author jmalo
 */
public class LocationsCache {
    
    private Map<String, XLocations> cache;
    private final int maximumSize;
    
    /**
     *  Creates a new instance of LocationsCache
     *  @param size Maximum number of entries to store
     */
    public LocationsCache(int size) {
        maximumSize = size;
        cache = new LRUCache(maximumSize);
    }

    /**
     *  It gets the existing version number in cache of the locations of a file
     *  @param fileId File to look for inside the cache
     *  @return The version number of the stored locations or 0 if the locations are not cached.
     */
    public long getVersion(String fileId) {
        XLocations loc = cache.get(fileId);
        return (loc != null)?loc.getVersion():0;
    }

    /**
     *  It updates the existing entry of a file with a new locations
     *  @param fileId File refered by the locations
     *  @param updatedLoc New locations for the file
     */
    public void update(String fileId, XLocations updatedLoc) {
        cache.put(fileId, updatedLoc);
    }

    /*
     *  It gets the existing cached Locations of a file
     *  @param fileId File referred to the requested Locations 
     *  @return The existing cached Locations or null if there isn't any Locations related to fileId
     */
    public XLocations getLocations(String fileId) {
        return cache.get(fileId);
    }
    
    public void removeLocations(String fileId) {
        cache.remove(fileId);
    }
    
    
    
    
}
