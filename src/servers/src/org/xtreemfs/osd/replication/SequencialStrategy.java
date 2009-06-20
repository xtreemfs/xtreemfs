/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
package org.xtreemfs.osd.replication;

import java.util.HashMap;
import java.util.List;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;

/**
 * A simple transfer strategy, which fetches the next object and iterates sequentially through the replicas
 * (like Round-Robin). It iterates not strictly through the replicas, because it uses different
 * position-pointers for every stripe. This is necessary, otherwise it could happen some replicas will be
 * never used, which could cause in an infinite loop.<br>
 * NOTE: This strategy assumes that all replicas uses the same striping policy (more precisely the same stripe
 * width). <br>
 * 13.10.2008
 */
public class SequencialStrategy extends TransferStrategy {
    /**
     * contains the position (replica) in xLoc list of the next OSD which should be used for this stripe<br>
     * key: stripe
     */
    // TODO: works only if all replicas are using the same striping policy
    private HashMap<Integer, Integer> nextOSDforObject;

    /**
     * @param rqDetails
     */
    public SequencialStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
        int stripeWidth = xLoc.getLocalReplica().getStripingPolicy().getWidth();
        this.nextOSDforObject = new HashMap<Integer, Integer>(stripeWidth);
        // set zero as start position for all stripes
        for (int i = 0; i < stripeWidth; i++)
            this.nextOSDforObject.put(i, 0);
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        long objectNo;
        // first try to fetch a preferred object
        if (!this.preferredObjects.isEmpty()) {
            objectNo = this.preferredObjects.get(0);
        } else { // fetch any object
            objectNo = this.requiredObjects.get(0);
        }

        try {
            // select OSD
            return selectNextOSDHook(objectNo);
        } catch (TransferStrategyException e) {
            // special error case => take another object

            for (int objectToTest = this.getObjectsCount(), preferredObjectToTest = 0, requiredObjectToTest = 0;; objectToTest--) {
                try {
                    // first try to fetch a preferred object
                    if (!this.preferredObjects.isEmpty()
                            && preferredObjectToTest < this.preferredObjects.size()) {
                        objectNo = this.preferredObjects.get(preferredObjectToTest++);
                    } else { // fetch any object
                        objectNo = this.requiredObjects.get(requiredObjectToTest++);
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
        int testedOSDs;

        // use the next replica relative to the last used replica
        List<ServiceUUID> osds = this.xLoc.getOSDsForObject(objectNo, xLoc.getLocalReplica());
        if (osds.size() == 0)
            throw new TransferStrategyException("No OSD could be found for object " + objectNo + ".",
                    TransferStrategyException.ErrorCode.NO_OSD_FOUND);

        int positionOfNextOSD = getPositionOfNextOSD(objectNo);
        increasePositionOfOSD(objectNo);

        for (testedOSDs = 0; testedOSDs < osds.size(); testedOSDs++) {
            ServiceUUID osd = osds.get(positionOfNextOSD);

            // if OSD is available => end "search"
            if (osdAvailability.isServiceAvailable(osd)) {
                next.osd = osd;
                // no object list
                next.requestObjectList = false;
                break;
            }
            
            positionOfNextOSD = (positionOfNextOSD + 1) % osds.size();
        }
        // if no OSD could be found
        if (next.osd == null) {
            throw new TransferStrategyException("At the moment no OSD is reachable for object " + objectNo,
                    TransferStrategyException.ErrorCode.NO_OSD_REACHABLE);
        }
        return next;
    }

    @Override
    public boolean removeObject(long objectNo) {
        return removeObjectFromList(objectNo);
    }

    /**
     * returns the position of the next using OSD for the stripe of the given object
     * 
     * @param objectNo
     * @return
     */
    private int getPositionOfNextOSD(long objectNo) {
        int stripeWidth = xLoc.getLocalReplica().getStripingPolicy().getWidth();
        return nextOSDforObject.get((int) (objectNo % stripeWidth));
    }

    /**
     * increases the position of the next using OSD for the stripe of the given object
     * 
     * @param objectNo
     */
    private void increasePositionOfOSD(long objectNo) {
        int stripeWidth = xLoc.getLocalReplica().getStripingPolicy().getWidth();
        int oldPosition = nextOSDforObject.get((int) (objectNo % stripeWidth));
        nextOSDforObject.put((int) (objectNo % stripeWidth), ++oldPosition % xLoc.getNumReplicas());
    }
}
