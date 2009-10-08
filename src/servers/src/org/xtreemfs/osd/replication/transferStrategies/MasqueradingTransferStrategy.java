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
package org.xtreemfs.osd.replication.transferStrategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.osd.replication.ObjectSet;

/**
 * 
 * <br>
 * 08.09.2009
 */
public abstract class MasqueradingTransferStrategy extends TransferStrategy {
    private final int                      defaultNumberOfRequests    = 20;              // every 20th
    // request

    private static final ObjectSet         dummyObjectSet             = new ObjectSet(0);

    /**
     * Contains a list of possible OSDs for each object. It's used to notice which OSDs were already
     * requested. <br>
     * key: objectNo
     */
    protected Map<Long, List<ServiceUUID>> availableOSDsForObject;

    protected int                          timeSinceLastRandomRequest = 0;

    protected long                         lastObjectNo;

    protected boolean                      fetchObjectSets;

    /**
     * 
     * @param fileID
     * @param xLoc
     * @param osdAvailability
     * @param fetchObjectSets
     *            turn on fetching object sets
     */
    public MasqueradingTransferStrategy(String fileID, XLocations xLoc, ServiceAvailability osdAvailability,
            boolean fetchObjectSets) {
        super(fileID, xLoc, osdAvailability);
        this.availableOSDsForObject = new HashMap<Long, List<ServiceUUID>>();
        this.fetchObjectSets = fetchObjectSets;

        StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
        this.lastObjectNo = sp.getObjectNoForOffset(xLoc.getXLocSet().getRead_only_file_size() - 1);
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

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        long objectNo;

        objectNo = selectObject(this.preferredObjects, this.requiredObjects);

        try {
            // select OSD
            return selectNextOSDHook(objectNo);
        } catch (TransferStrategyException e) {
            // special error case => take another object

            ObjectSet copyOfPreferredObjects = null;
            ObjectSet copyOfRequiredObjects = null;
            try {
                copyOfPreferredObjects = this.preferredObjects.clone();
                copyOfRequiredObjects = this.requiredObjects.clone();
            } catch (CloneNotSupportedException e2) {
                // could not happen
            }
            assert (copyOfPreferredObjects != null && copyOfRequiredObjects != null);

            while (true) {
                objectNo = selectObject(copyOfPreferredObjects, copyOfRequiredObjects);

                try {
                    // select OSD
                    return selectNextOSDHook(objectNo);
                } catch (TransferStrategyException e1) {
                    // remove for next time
                    if (copyOfPreferredObjects.contains(objectNo))
                        copyOfPreferredObjects.remove(objectNo);
                    else if (copyOfRequiredObjects.contains(objectNo))
                        copyOfRequiredObjects.remove(objectNo);

                    // if all objects are tried throw exception
                    if (copyOfPreferredObjects.isEmpty() && copyOfRequiredObjects.isEmpty())
                        throw e;
                }
            }
        }
    }

    /**
     * selects an object
     * 
     * @param preferredObjects
     * @param requiredObjects
     * @return
     * @throws TransferStrategyException
     */
    protected abstract long selectObject(ObjectSet preferredObjects, ObjectSet requiredObjects)
            throws TransferStrategyException;

    @Override
    protected NextRequest selectNextOSDHook(long objectNo) throws TransferStrategyException {
        NextRequest next = new NextRequest();
        next.objectNo = objectNo;
        
        List<ServiceUUID> availableOSDsForObject = this.getAvailableOSDsForObject(objectNo);

        if (availableOSDsForObject.size() == 0)
            throw new TransferStrategyException("No OSD could be found for object " + objectNo
                    + ". Maybe it is a hole.", TransferStrategyException.ErrorCode.NO_OSD_FOUND);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "%s:%d - available OSDs for file: %s", fileID, objectNo, availableOSDsForObject
                            .toString());

        if (fetchObjectSets)
            // time to get a new object set from an OSD?
            next.attachObjectSet = isTimeForNewObjectSet();

        // select the OSD
        ServiceUUID osd = selectOSD(availableOSDsForObject, objectNo, next.attachObjectSet);

        if (fetchObjectSets)
            // if no object set is available of this OSD => get one
            if (!objectsOnOSDs.containsKey(osd))
                next.attachObjectSet = true;

        // check if OSD is available at all
        if (osdAvailability.isServiceAvailable(osd)) {
            // remove current OSD from list (is requested)
            availableOSDsForObject.remove(osd);

            // OSD is available
            next.osd = osd;
        } else { // OSD is not available
            // special error case => "search" for another OSD
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "OSD %s is not available", osd.toString());

            // to check, if all OSDs have been tested
            List<ServiceUUID> testedOSDs = new ArrayList<ServiceUUID>(availableOSDsForObject);

            // test all other OSDs until one is found or all were tested
            while (testedOSDs.size() != 0) {
                osd = selectOSD(testedOSDs, objectNo, next.attachObjectSet);

                if (osdAvailability.isServiceAvailable(osd)) {
                    // OSD found
                    next.osd = osd;
                    break;
                } else {
                    testedOSDs.remove(osd);

                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "OSD %s is not available", osd.toString());
                }
            }
        }

        // if no OSD could be found
        if (next.osd == null) {
            throw new TransferStrategyException("At the moment no OSD is reachable for object " + objectNo,
                    TransferStrategyException.ErrorCode.NO_OSD_REACHABLE);
        }

        if (fetchObjectSets)
            // adapt counter
            if (next.attachObjectSet) {
                timeSinceLastRandomRequest = 0;
                if (!objectsOnOSDs.containsKey(next.osd))
                    // set dummy object set, if no object set exists yet; otherwise object set will be
                    // requested multiple times until the first will be received
                    super.setOSDsObjectSet(dummyObjectSet, next.osd);
            } else
                timeSinceLastRandomRequest++;

        return next;
    }

    /**
     * selects an OSD
     * 
     * @param availableOSDsForObject
     * @param objectNo
     * @param timeForNewObjectSet
     * @return
     * @throws TransferStrategyException
     */
    protected abstract ServiceUUID selectOSD(List<ServiceUUID> availableOSDsForObject, long objectNo,
            boolean timeForNewObjectSet) throws TransferStrategyException;

    /**
     * returns a list of available OSDs for the given object
     * 
     * @param objectNo
     * @return
     */
    protected List<ServiceUUID> getAvailableOSDsForObject(long objectNo) {
        // assert (requiredObjects.contains(objectNo) || preferredObjects.contains(objectNo));

        List<ServiceUUID> list = availableOSDsForObject.get(objectNo);
        if (list == null) {
            // add existing OSDs containing the object
            list = xLoc.getOSDsForObject(objectNo, xLoc.getLocalReplica());
            availableOSDsForObject.put(objectNo, list);
        }
        return list;
    }

    /**
     * decides if this request should fetch an object set
     * 
     * @return
     */
    protected boolean isTimeForNewObjectSet() {
        // assert (getObjectsCount() != 0);

        double ratio = (this.lastObjectNo == 0) ? 1 : super.getObjectsCount() / this.lastObjectNo;

        // value between 10 and 100
//        if (timeSinceLastRandomRequest > defaultNumberOfRequests / ratio)
        if (timeSinceLastRandomRequest > defaultNumberOfRequests)
            return true;
        else
            return false;
    }
}
