/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
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
package org.xtreemfs.osd.replication.transferStrategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.replication.selection.RandomOSDSelection;
import org.xtreemfs.osd.replication.selection.RandomObjectSelection;

/**
 * A simple strategy, which selects objects and replicas randomly. <br>
 * 08.12.2008
 */
public class RandomStrategy extends TransferStrategy {
    /**
     * identifies the random strategy in replication flags
     */
    public static final int              REPLICATION_FLAG = Constants.REPL_FLAG_STRATEGY_RANDOM;

    protected RandomObjectSelection      objectSelection;
    protected RandomOSDSelection         osdSelection;

    /**
     * Contains a list of possible OSDs for each object. It's used to notice which OSDs were already
     * requested. <br>
     * key: objectNo
     */
    private Map<Long, List<ServiceUUID>> availableOSDsForObject;

    /**
     * @param rqDetails
     */
    public RandomStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
        this.availableOSDsForObject = new HashMap<Long, List<ServiceUUID>>();
        this.objectSelection = new RandomObjectSelection();
        this.osdSelection = new RandomOSDSelection();
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        long objectNo;

        // first fetch a preferred object
        if (!super.preferredObjects.isEmpty()) {
            objectNo = objectSelection.selectNextObject(this.preferredObjects);
        } else { // fetch any object
            objectNo = objectSelection.selectNextObject(this.requiredObjects);
        }

        try {
            // select OSD
            return selectNextOSDHook(objectNo);
        } catch (TransferStrategyException e) {
            // special error case => take another object

            // just a simple break-condition, but the good one would be too expensive
            for (int objectToTest = this.getObjectsCount(), preferredObjectToTest = 0;; objectToTest--) {
                try {
                    // first try to fetch a preferred object
                    if (!super.preferredObjects.isEmpty()
                            && preferredObjectToTest < this.preferredObjects.size()) {
                        objectNo = objectSelection.selectNextObject(this.preferredObjects);
                        preferredObjectToTest++;
                    } else { // fetch any object
                        objectNo = objectSelection.selectNextObject(this.requiredObjects);
                    }

                    // select OSD
                    return selectNextOSDHook(objectNo);
                } catch (TransferStrategyException e1) {
                    if (objectToTest == 1) // if all objects are tried throw exception
                        throw e;
                }
            }
        }
    }

    @Override
    protected NextRequest selectNextOSDHook(long objectNo) throws TransferStrategyException {
        NextRequest next = new NextRequest();
        next.objectNo = objectNo;

        // to check, if all OSDs have been tested
        List<ServiceUUID> testedOSDs = new ArrayList<ServiceUUID>(this.getAvailableOSDsForObject(objectNo));
        // FIXME: only for debugging
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                "available OSDs for file %s: %s.", fileID, testedOSDs.toString());

        if (testedOSDs.size() == 0)
            throw new TransferStrategyException("No OSD could be found for object " + objectNo
                    + ". Maybe it is a hole.", TransferStrategyException.ErrorCode.NO_OSD_FOUND);

        while (testedOSDs.size() != 0) {
            // use random OSD
            ServiceUUID osd = osdSelection.selectNextOSD(testedOSDs);
            // if OSD is available => end "search"
            if (osdAvailability.isServiceAvailable(osd)) {
                next.osd = osd;

                // no object list
                next.requestObjectList = false;
                break;
            }
            // OSD is not available => remove it from list
            testedOSDs.remove(osd);
            // FIXME: only for debugging
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "OSD %s is not available",
                    osd.toString());
        }
        // if no OSD could be found
        if (next.osd == null) {
            throw new TransferStrategyException("At the moment no OSD is reachable for object " + objectNo,
                    TransferStrategyException.ErrorCode.NO_OSD_REACHABLE);
        }
        return next;
    }

    /**
     * returns a list of available OSDs for the given object
     * 
     * @param objectNo
     * @return
     */
    protected List<ServiceUUID> getAvailableOSDsForObject(long objectNo) {
        assert (requiredObjects.contains(objectNo) || preferredObjects.contains(objectNo));

        List<ServiceUUID> list = availableOSDsForObject.get(objectNo);
        if (list == null) {
            // add existing OSDs containing the object
            list = xLoc.getOSDsForObject(objectNo, xLoc.getLocalReplica());
            availableOSDsForObject.put(objectNo, list);
        }
        return list;
    }

    @Override
    public NextRequest getNext() {
        NextRequest next = super.getNext();
        if (next != null) {
            // remove used OSD for this object, because the OSD will not be used a second time
            List<ServiceUUID> osds = availableOSDsForObject.get(next.objectNo);
            if (osds != null)
                osds.remove(next.osd);
        }
        return next;
    }

    @Override
    public boolean removeObject(long objectNo) {
        boolean contained = (null != availableOSDsForObject.remove(objectNo));
        contained = contained || super.removeObjectFromList(objectNo);
        return contained;
    }
}
