/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.transferStrategies;

import java.util.Iterator;
import java.util.List;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.RandomOSDSelection;
import org.xtreemfs.osd.replication.selection.SequentialObjectSelection;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;

/**
 * A simple transfer strategy, which fetches the objects in ascending order. The OSD will be a member of a
 * randomly selected completed replica. <br>
 * 13.10.2008
 */
public class SequentialStrategy extends MasqueradingTransferStrategy {
    /**
     * identifies the sequential strategy in replication flags
     */
    public static final REPL_FLAG             REPLICATION_FLAG = REPL_FLAG.REPL_FLAG_STRATEGY_SEQUENTIAL;

    protected SequentialObjectSelection objectSelection;
    protected RandomOSDSelection        osdSelection;

    /**
     * @param rqDetails
     */
    public SequentialStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability, false);
        this.objectSelection = new SequentialObjectSelection();
        this.osdSelection = new RandomOSDSelection();
    }

    @Override
    protected long selectObject(ObjectSet preferredObjects, ObjectSet requiredObjects)
            throws TransferStrategyException {
        long objectNo;
        // first try to fetch a preferred object
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
        return osdSelection.selectNextOSD(availableOSDsForObject);
    }

    @Override
    protected List<ServiceUUID> getAvailableOSDsForObject(long objectNo) {
        assert (requiredObjects.contains(objectNo) || preferredObjects.contains(objectNo));

        List<ServiceUUID> list = availableOSDsForObject.get(objectNo);
        if (list == null) {
            // add existing OSDs containing the object
            list = xLoc.getOSDsForObject(objectNo, xLoc.getLocalReplica());

            // remove all not complete replicas
            Iterator<ServiceUUID> iterator = list.iterator();
            while (iterator.hasNext()) {
                ServiceUUID osd = iterator.next();
                if (!xLoc.getReplica(osd).isComplete())
                    iterator.remove();
            }

            availableOSDsForObject.put(objectNo, list);
        }
        return list;
    }
}
