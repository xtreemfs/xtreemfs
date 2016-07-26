/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.transferStrategies;

import java.util.List;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.RandomOSDSelection;
import org.xtreemfs.osd.replication.selection.RandomObjectSelection;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;

/**
 * A simple strategy, which selects objects and replicas randomly. <br>
 * 08.12.2008
 */
public class RandomStrategyWithoutObjectSets extends MasqueradingTransferStrategy {
    /**
     * identifies the random strategy in replication flags
     */
    public static final REPL_FLAG         REPLICATION_FLAG = REPL_FLAG.REPL_FLAG_STRATEGY_RANDOM;

    protected RandomObjectSelection objectSelection;
    protected RandomOSDSelection    randomOSDselection;
//    protected ObjectSetOSDSelection objectSetOSDselection;

    /**
     * @param rqDetails
     */
    public RandomStrategyWithoutObjectSets(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability, false);
        this.objectSelection = new RandomObjectSelection();
        this.randomOSDselection = new RandomOSDSelection();
//        this.objectSetOSDselection = new ObjectSetOSDSelection();
    }

    /**
     * @return
     * @throws TransferStrategyException
     */
    @Override
    protected long selectObject(ObjectSet preferredObjects, ObjectSet requiredObjects)
            throws TransferStrategyException {
        long objectNo;
        // first fetch a preferred object
        if (!preferredObjects.isEmpty()) {
            objectNo = objectSelection.selectNextObject(preferredObjects);
        } else { // fetch any object
            objectNo = objectSelection.selectNextObject(requiredObjects);
        }
        return objectNo;
    }

    @Override
    protected ServiceUUID selectOSD(List<ServiceUUID> availableOSDsForObject, long objectNo,
            boolean timeForNewObjectSet) throws TransferStrategyException {
        ServiceUUID osd;
//        if (timeForNewObjectSet) {
            osd = randomOSDselection.selectNextOSD(availableOSDsForObject);
//        } else {
//            osd = objectSetOSDselection.selectNextOSD(availableOSDsForObject, objectsOnOSDs, objectNo);
//        }
        return osd;
    }
}
