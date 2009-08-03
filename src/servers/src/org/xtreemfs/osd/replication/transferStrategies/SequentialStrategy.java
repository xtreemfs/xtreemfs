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
package org.xtreemfs.osd.replication.transferStrategies;

import java.util.List;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.replication.selection.SequentialOSDSelection;
import org.xtreemfs.osd.replication.selection.SequentialObjectSelection;

/**
 * A simple transfer strategy, which fetches the next object and iterates sequentially through the replicas
 * (like Round-Robin). It iterates not strictly through the replicas, because it uses different
 * position-pointers for every stripe. This is necessary, otherwise it could happen some replicas will be
 * never used, which could cause in an infinite loop.<br>
 * NOTE: This strategy assumes that all replicas uses the same striping policy (more precisely the same stripe
 * width). <br>
 * 13.10.2008
 */
public class SequentialStrategy extends TransferStrategy {
    /**
     * identifies the sequential strategy in replication flags
     */
    public static final int             REPLICATION_FLAG = Constants.REPL_FLAG_STRATEGY_SEQUENTIAL;

    protected SequentialObjectSelection objectSelection;
    protected SequentialOSDSelection    osdSelection;

    /**
     * @param rqDetails
     */
    public SequentialStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
        objectSelection = new SequentialObjectSelection();
        osdSelection = new SequentialOSDSelection(xLoc.getLocalReplica().getStripingPolicy().getWidth());
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        long objectNo;
        // first try to fetch a preferred object
        if (!this.preferredObjects.isEmpty()) {
            objectNo = objectSelection.selectNextObject(this.preferredObjects);
        } else { // fetch any object
            objectNo = objectSelection.selectNextObject(this.requiredObjects);
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
                        objectNo = objectSelection.selectNextObjectOfSameSet(this.preferredObjects);
                    } else { // fetch any object
                        objectNo = objectSelection.selectNextObjectOfSameSet(this.requiredObjects);
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

        ServiceUUID osd = osdSelection.selectNextOSD(osds, objectNo);
        for (testedOSDs = 0; testedOSDs < osds.size(); testedOSDs++) {
            // if OSD is available => end "search"
            if (osdAvailability.isServiceAvailable(osd)) {
                next.osd = osd;
                // no object list
                next.attachObjectSet = false;
                break;
            } else {
                // try next OSD
                osd = osds.get((osds.indexOf(osd) + 1) % osds.size());
            }
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
}
