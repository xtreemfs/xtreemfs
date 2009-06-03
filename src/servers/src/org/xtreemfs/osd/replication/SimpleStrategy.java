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

import java.util.List;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;

/**
 * A simple transfer strategy, which fetches the next object and iterates sequentially through the replicas
 * (like Round-Robin). <br>
 * NOTE: This Strategy does not remember which OSDs it has used before for an object, so it could happen that
 * it always takes the same OSD for an object. This can result in a infinite loop for this object. <br>
 * 13.10.2008
 */
@Deprecated
public class SimpleStrategy extends TransferStrategy {
    private int indexOfLastUsedOSD = -1;

    /**
     * @param rqDetails
     */
    public SimpleStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        long objectNo = -1;

        // first fetch a preferred object
        if (!this.preferredObjects.isEmpty()) {
            objectNo = this.preferredObjects.get(0);
        } else { // fetch any object
            if (!this.requiredObjects.isEmpty()) {
                objectNo = this.requiredObjects.get(0);
            }
        }

        // TODO: handle case, if no OSD could be found for object (=> hole), because nobody will ever notice,
        // that this object is a hole

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
        int testedOSDs;

        // use the next replica relative to the last used replica
        List<ServiceUUID> osds = this.xLoc.getOSDsForObject(objectNo, xLoc.getLocalReplica());
        if (osds.size() == 0)
            throw new TransferStrategyException("No OSD could be found for object " + objectNo
                    + ". It seems to be a hole.", TransferStrategyException.ErrorCode.NO_OSD_FOUND);
        for (testedOSDs = 0; testedOSDs < osds.size(); testedOSDs++) {
            indexOfLastUsedOSD = ++indexOfLastUsedOSD % osds.size();
            ServiceUUID osd = osds.get(indexOfLastUsedOSD);

            // if OSD is available => end "search"
            if (osdAvailability.isServiceAvailable(osd)) {
                next.osd = osd;
                // no object list
                next.requestObjectList = false;
                break;
            }
        }
        // if no OSD could be found
        if (next.osd == null || isHole(objectNo)) {
            throw new TransferStrategyException("At the moment no OSD is reachable for object " + objectNo,
                    TransferStrategyException.ErrorCode.NO_OSD_REACHABLE);
        }
        return next;
    }
}
