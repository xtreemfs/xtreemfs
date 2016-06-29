/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.transferStrategies;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.RarestFirstObjectSelection;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;

/**
 * 
 * <br>
 * 27.08.2009
 */
public class RarestFirstStrategy extends RandomStrategy {
    /**
     * identifies the random strategy in replication flags
     */
    public static final REPL_FLAG              REPLICATION_FLAG = REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST;

    protected RarestFirstObjectSelection objectSelection;

    /**
     * @param rqDetails
     */
    public RarestFirstStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
        this.objectSelection = new RarestFirstObjectSelection();
    }

    @Override
    protected long selectObject(ObjectSet preferredObjects, ObjectSet requiredObjects)
            throws TransferStrategyException {
        long objectNo;
        // first fetch a preferred object
        if (!preferredObjects.isEmpty()) {
            // preferred objects will not be saved in the queue
            objectNo = super.objectSelection.selectNextObject(preferredObjects);
        } else { // fetch any object
            objectNo = objectSelection.selectNextObject(requiredObjects, super.objectsOnOSDs);
        }
        return objectNo;
    }

    @Override
    public boolean addObject(long objectNo, boolean preferred) {
        boolean returnValue = super.addObject(objectNo, preferred);
        if (!preferred) // if not preferred object => update queue
            objectSelection.addObject(objectNo, super.objectsOnOSDs);
        return returnValue;
    }

    @Override
    public void setOSDsObjectSet(ObjectSet set, ServiceUUID osd) {
//        objectSelection.newObjectSetArrived(requiredObjects, objectsOnOSDs.get(osd).set, set);
        objectSelection.invalidateQueue();
        super.setOSDsObjectSet(set, osd);
    }
    
    @Override
    protected boolean removeObjectFromList(long objectNo) {
        objectSelection.removeObject(objectNo);
        return super.removeObjectFromList(objectNo);
    }
}
