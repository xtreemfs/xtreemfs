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
package org.xtreemfs.osd.replication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;

/**
 * A simple strategy, which selects objects and replicas randomly. <br>
 * 08.12.2008
 */
public class RandomStrategy extends TransferStrategy {
    private Random random;

    /**
     * @param rqDetails
     */
    public RandomStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
        this.random = new Random();
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        long objectNo = -1;

        // first fetch a preferred object
        if (!this.preferredObjects.isEmpty()) {
            objectNo = this.preferredObjects.get(random.nextInt(this.preferredObjects.size()));
        } else { // fetch any object
            if (!this.requiredObjects.isEmpty()) {
                objectNo = this.requiredObjects.get(random.nextInt(this.requiredObjects.size()));
            }
        }

        // select OSD
        if (objectNo != -1)
            return selectNextOSDHook(objectNo);
        else
            // nothing to fetch
            return null;
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
                    + ". It seems to be a hole.", TransferStrategyException.ErrorCode.NO_OSD_FOUND);

        while (testedOSDs.size() != 0) {
            // use random OSD
            ServiceUUID osd = testedOSDs.get(random.nextInt(testedOSDs.size()));
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
}
