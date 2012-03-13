/*
 * Copyright (c) 2012 by Matthias Noack,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.xtreemfs.dir.VivaldiClientMap.VivaldiClientValue;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

public class VivaldiClientMap extends ConcurrentHashMap<InetSocketAddress, VivaldiClientValue> {

    private static final long serialVersionUID = 1L;
    private final int MAX_SIZE; 
    private final long TIME_OUT_IN_MS;

    public class VivaldiClientValue {
        private VivaldiCoordinates coordinates;
        private long timeStamp;
        
        public VivaldiClientValue(VivaldiCoordinates coords, long time) {
            this.coordinates = coords;
            this.timeStamp = time;
        }
    
        public VivaldiClientValue(VivaldiCoordinates coords) {
            this.coordinates = coords;
            this.timeStamp = System.currentTimeMillis();
        }
        
        public VivaldiCoordinates getCoordinates() {
            return coordinates;
        }
        
        public long getTimeStamp() {
            return timeStamp;
        }
    }
    
    public VivaldiClientMap(int maxSize, long timeOut) {
        super();
        MAX_SIZE = maxSize;
        TIME_OUT_IN_MS = timeOut;
    }

    public void put(InetSocketAddress addr, VivaldiCoordinates coords) {
        // TODO (mno): InetSocketAddress changes as a side effect when 
        // some getter or toString a lookup or reverse-lookup is triggered
        if(MAX_SIZE > 0)
        {
            // first put ...
            this.put(addr, new VivaldiClientValue(coords));
            
            // ...then check if size was exceeded, this avoids re-inserts for puts with existing keys
            if (this.size() > MAX_SIZE) {
                filterTimeOuts();
                if (this.size() > MAX_SIZE) {
                    removeOldestEntry();
                }
            }
        }
    }
    
    private void removeOldestEntry() {
        long minTime = Long.MAX_VALUE;
        InetSocketAddress minKey = null;
        boolean first = true;
                
        for (Map.Entry<InetSocketAddress, VivaldiClientValue> e : this.entrySet()) {
            final long putTime = e.getValue().getTimeStamp();
            if (first || (minTime < putTime)) {
                minTime = putTime;
                minKey = e.getKey();
                first = false;
            }
        }
        
        if(minKey != null)
            this.remove(minKey);
    }
    
    public void filterTimeOuts() {
        for (Iterator<Map.Entry<InetSocketAddress, VivaldiClientValue>> it = this.entrySet().iterator(); it.hasNext(); ) {
            final long timeSinceLastUpdate = System.currentTimeMillis() - it.next().getValue().getTimeStamp();
            if (timeSinceLastUpdate > TIME_OUT_IN_MS) {
                it.remove();
            }
        }
    }
    
}
