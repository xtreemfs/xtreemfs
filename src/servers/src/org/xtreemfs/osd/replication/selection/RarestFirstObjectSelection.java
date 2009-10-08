/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.osd.replication.selection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.ObjectSetInfo;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;

/**
 * 
 * <br>
 * 29.06.2009
 */
public class RarestFirstObjectSelection {
    /**
     * max. occurrence of objects == #replicas, so map should not be too big
     */
    protected TreeMap<Integer, ObjectSet> rarestObjects;
    protected long                        objectsCount;

    /**
     * 
     */
    public RarestFirstObjectSelection() {
        this.rarestObjects = new TreeMap<Integer, ObjectSet>();
        this.objectsCount = 0;
    }

    public long selectNextObject(ObjectSet objects, Map<ServiceUUID, ObjectSetInfo> objectsOnOSDsMap)
            throws TransferStrategyException {
        // at least one object must be wanted
        assert (!objects.isEmpty());

        // build new queue, if it is empty or invalidated
        if (objectsCount <= 0)
            buildQueue(objects, objectsOnOSDsMap);

        // check if at least one object is contained by queue
        assert (objectsCount > 0);

        Long objectNo = null;
        for (ObjectSet objectWithOccurrence : rarestObjects.values()) {
            // get an object of the highest priority (less occurrence)
            if (objectWithOccurrence.isEmpty())
                continue; // try next priority-group
            objectNo = objectWithOccurrence.getRandom();
            break;
        }
        return objectNo.longValue();
    }

    /**
     * Rebuilds the queue of rarest objects.
     * 
     * @param objects
     * @param objectsOnOSDsMap
     */
    public void buildQueue(ObjectSet objects, Map<ServiceUUID, ObjectSetInfo> objectsOnOSDsMap) {
        // iterate through all wanted objects
        Iterator<Long> objectsIt = objects.iterator();
        while (objectsIt.hasNext()) {
            Long objectNo = objectsIt.next();

            addObject(objectNo, objectsOnOSDsMap);
        }
    }

    /**
     * Adds the object to the queue of rarest objects (and counts the occurrence).
     * 
     * @param objectNo
     * @param objectsOnOSDsMap
     */
    public void addObject(long objectNo, Map<ServiceUUID, ObjectSetInfo> objectsOnOSDsMap) {
        Collection<ObjectSetInfo> objectsOnOSDs = objectsOnOSDsMap.values();

        // check how often this object is contained
        int counter = 0;
        for (ObjectSetInfo setInfo : objectsOnOSDs)
            if (setInfo.set.contains(objectNo))
                counter++;

        // if object occurres zero times, at least one map is missing (of a complete replica) => object gets the lowest
        // priority
        if (counter == 0)
            counter = Integer.MAX_VALUE;

        addObject(objectNo, counter);
    }
    
    /**
     * Adds the object to the queue of rarest objects.
     */
    private void addObject(Long objectNo, Integer occurrence) {
        // get correct queue
        ObjectSet objectWithOccurrence = rarestObjects.get(occurrence);
        if (objectWithOccurrence == null) {
            // new set, if does not exist yet
            objectWithOccurrence = new ObjectSet();
            rarestObjects.put(occurrence, objectWithOccurrence);
        }
        // put into queue
        objectWithOccurrence.add(objectNo);
        objectsCount++;
    }

    /**
     * Must be called each time the map of objects on OSDs has changed.
     */
    public void invalidateQueue() {
        for (ObjectSet set : rarestObjects.values())
            set.clear();
        objectsCount = 0;
    }

    /**
     * 
     * @param objects
     * @param oldObjectSet
     * @param newObjectSet
     */
    /*
     * alternative to invalidation; not used so far
     */
    public void newObjectSetArrived(ObjectSet objects, ObjectSet oldObjectSet, ObjectSet newObjectSet) {
        ObjectSet objectSet;
        if (oldObjectSet == null)
            objectSet = new ObjectSet();
        else
            objectSet = new ObjectSet(oldObjectSet);

        // object set should only contain new objects (difference to old object set)
        objectSet.complement(newObjectSet.size()-1);
        objectSet.intersection(newObjectSet);
        
        // object set should only contain new wanted objects
        objectSet.intersection(objects);

        // iterate through all new wanted objects
        Iterator<Long> objectsIt = objectSet.iterator();
        while (objectsIt.hasNext()) {
            Long objectNo = objectsIt.next();

            // find objectNo in an objectSet
            for (Entry<Integer, ObjectSet> e : rarestObjects.entrySet())
                if (e.getValue().contains(objectNo)) {
                    // increase occurrence of objectNo
                    e.getValue().remove(objectNo);
                    objectsCount--;
                    addObject(objectNo, e.getKey() + 1);
                    break;
                }
        }
    }

    public void removeObject(long objectNo) {
        for (ObjectSet objectWithOccurrence : rarestObjects.values())
            if (objectWithOccurrence.remove(objectNo)) {
                objectsCount--;
                break;
            }
    }
}
