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

import java.util.List;
import java.util.Random;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;

/**
 * A simple strategy, which selects objects and replicas randomly.
 * 
 * 08.12.2008
 * 
 * @author clorenz
 */
public class RandomStrategy extends TransferStrategy {
    private Random random;

    /**
     * @param rqDetails
     */
    public RandomStrategy(String fileId, XLocations xLoc, long filesize,
            ServiceAvailability osdAvailability) {
        super(fileId, xLoc, filesize, osdAvailability);
        this.random = new Random();
    }

    @Override
    public void selectNext() {
        // prepare
        super.selectNext();

        long objectNo = -1;

        // first fetch a preferred object
        if (!this.preferredObjects.isEmpty()) {
            objectNo = this.preferredObjects.get(getPositiveRandom() % this.preferredObjects.size());
        } else { // fetch any object
            if (!this.requiredObjects.isEmpty()) {
                objectNo = this.requiredObjects.get(getPositiveRandom() % this.requiredObjects.size());
            }
        }

        // select OSD
        if (objectNo != -1)
            next = selectNextOSDhelper(objectNo);
        else
            // nothing to fetch
            next = null;
    }

    @Override
    public void selectNextOSD(long objectNo) {
        // prepare
        super.selectNext();
        // select OSD
        next = selectNextOSDhelper(objectNo);
    }

    private NextRequest selectNextOSDhelper(long objectNo) {
        NextRequest next = new NextRequest();
        next.objectNo = objectNo;
        int testedOSDs;

        List<ServiceUUID> osds = this.availableOSDsForObject.get(objectNo);
        for (testedOSDs = 0; testedOSDs < osds.size(); testedOSDs++) {
            // use random OSD
            ServiceUUID osd = osds.get(getPositiveRandom() % osds.size());
            // if OSD is available => end "search"
            if (osdAvailability.isServiceAvailable(osd)) {
                next.osd = osd;

                // no object list
                next.requestObjectList = false;
                break;
            }
        }
        // if no OSD could be found
        if (osds.size() == 0 || osds.size() == testedOSDs || isHole(objectNo))
            next = null;
        return next;
    }
    
    /**
     * returns a random positive integer
     * 
     * @return
     */
    private int getPositiveRandom() {
        int result = random.nextInt();
        return (result > 0) ? result : 0 - result;
    }
    
//  /**
//  * Removes the OSD from the list that is used for knowing which OSDs could be used for fetching this
//  * object.
//  * 
//  * @param objectID
//  */
// public void removeOSDForObject(long objectID, ServiceUUID osd) {
//     availableOSDsForObject.get(Long.valueOf(objectID)).remove(osd);
// }
//
// /*
//  * FIXME: internal-handling would be better
//  */
// public void removeOSDListForObject(long objectID) {
//     availableOSDsForObject.remove(Long.valueOf(objectID));
// }
//
// /**
//  * @return
//  * @see java.util.ArrayList#isEmpty()
//  */
// public boolean isOSDAvailableForObject(long objectNo) {
//     return !availableOSDsForObject.get(objectNo).isEmpty();
// }
}
