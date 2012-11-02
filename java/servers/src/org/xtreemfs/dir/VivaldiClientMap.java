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

public class VivaldiClientMap extends ConcurrentHashMap<String, VivaldiClientValue> {

    private static final long serialVersionUID = 1L;
    private final int MAX_SIZE; 
    private final long TIME_OUT_IN_MS;

    public class VivaldiClientValue {
        private InetSocketAddress address;
        private VivaldiCoordinates coordinates;
        private long timeStamp;
        
        public VivaldiClientValue(InetSocketAddress addr, VivaldiCoordinates coords, long time) {
            this.address = addr;
            this.coordinates = coords;
            this.timeStamp = time;
        }
    
        public VivaldiClientValue(InetSocketAddress addr, VivaldiCoordinates coords) {
            this.address = addr;
            this.coordinates = coords;
            this.timeStamp = System.currentTimeMillis();
        }
        
        public InetSocketAddress getAddress() {
            return address;
        }
        
        public VivaldiCoordinates getCoordinates() {
            return coordinates;
        }
        
        public long getTimeStamp() {
            return timeStamp;
        }
    }
    
    public VivaldiClientMap(int maxSize, long timeOutInMS) {
        super(maxSize + 1); // +1 because put inserts first, and then removes an old element if necessary
        MAX_SIZE = maxSize;
        TIME_OUT_IN_MS = timeOutInMS;
    }

    public void put(InetSocketAddress addr, VivaldiCoordinates coords) {
        // TODO (mno): InetSocketAddress changes as a side effect when 
        // some getter or toString trigger a lookup or reverse-lookup respectively
        if(MAX_SIZE > 0)
        {
            // first put ...
            this.put(addr.getHostName(), new VivaldiClientValue(addr, coords));
            
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
        String minKey = null;
        boolean first = true;
                
        for (Map.Entry<String, VivaldiClientValue> e : this.entrySet()) {
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
        for (Iterator<Map.Entry<String, VivaldiClientValue>> it = this.entrySet().iterator(); it.hasNext(); ) {
            final long timeSinceLastUpdate = System.currentTimeMillis() - it.next().getValue().getTimeStamp();
            if (timeSinceLastUpdate > TIME_OUT_IN_MS) {
                it.remove();
            }
        }
    }
    
}
