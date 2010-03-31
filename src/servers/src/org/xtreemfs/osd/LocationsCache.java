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
 * AUTHORS: Björn Kolbeck (ZIB)
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
