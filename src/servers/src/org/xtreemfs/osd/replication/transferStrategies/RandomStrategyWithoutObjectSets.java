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

import java.util.List;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.ObjectSetOSDSelection;
import org.xtreemfs.osd.replication.selection.RandomOSDSelection;
import org.xtreemfs.osd.replication.selection.RandomObjectSelection;

/**
 * A simple strategy, which selects objects and replicas randomly. <br>
 * 08.12.2008
 */
public class RandomStrategyWithoutObjectSets extends MasqueradingTransferStrategy {
    /**
     * identifies the random strategy in replication flags
     */
    public static final int         REPLICATION_FLAG = Constants.REPL_FLAG_STRATEGY_RANDOM;

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
