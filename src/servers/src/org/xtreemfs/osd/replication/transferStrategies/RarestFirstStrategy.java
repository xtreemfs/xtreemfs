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

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.RarestFirstObjectSelection;

/**
 * 
 * <br>
 * 27.08.2009
 */
public class RarestFirstStrategy extends RandomStrategy {
    /**
     * identifies the random strategy in replication flags
     */
    public static final int              REPLICATION_FLAG = Constants.REPL_FLAG_STRATEGY_RAREST_FIRST;

    protected RarestFirstObjectSelection objectSelection;

    /**
     * @param rqDetails
     */
    public RarestFirstStrategy(String fileId, XLocations xLoc, ServiceAvailability osdAvailability) {
        super(fileId, xLoc, osdAvailability);
        this.objectSelection = new RarestFirstObjectSelection();
    }

    @Override
    protected long selectObject(ObjectSet preferredObjects, ObjectSet requiredObjects)
            throws TransferStrategyException {
        long objectNo;
        // first fetch a preferred object
        if (!preferredObjects.isEmpty()) {
            // preferred objects will not be saved in the queue
            objectNo = super.objectSelection.selectNextObject(preferredObjects);
        } else { // fetch any object
            objectNo = objectSelection.selectNextObject(requiredObjects, super.objectsOnOSDs);
        }
        return objectNo;
    }

    @Override
    public boolean addObject(long objectNo, boolean preferred) {
        boolean returnValue = super.addObject(objectNo, preferred);
        if (!preferred) // if not preferred object => update queue
            objectSelection.addObject(objectNo, super.objectsOnOSDs);
        return returnValue;
    }

    @Override
    public void setOSDsObjectSet(ObjectSet set, ServiceUUID osd) {
//        objectSelection.newObjectSetArrived(requiredObjects, objectsOnOSDs.get(osd).set, set);
        objectSelection.invalidateQueue();
        super.setOSDsObjectSet(set, osd);
    }
    
    @Override
    protected boolean removeObjectFromList(long objectNo) {
        objectSelection.removeObject(objectNo);
        return super.removeObjectFromList(objectNo);
    }
}
