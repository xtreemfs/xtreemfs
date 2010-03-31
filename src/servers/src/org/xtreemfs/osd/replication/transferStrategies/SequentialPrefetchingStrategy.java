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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.SequentialObjectSelection;

/**
 * A sequential strategy which is using simple prefetching. It assumes that objects are read sequentially. <br>
 * 23.06.2009
 */
public class SequentialPrefetchingStrategy extends RandomStrategy {
    /**
     * identifies the sequential strategy in replication flags
     */
    public static final int             REPLICATION_FLAG          = Constants.REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING;

    public static int                   DEFAULT_PREFETCHING_COUNT = 10;

    protected SequentialObjectSelection objectSelection;

    private StripingPolicyImpl          stripingPolicy;

    private int                         osdIndex                  = -1;

    /**
     * Saves already prefetched objects. Otherwise objects will be fetched again and again, if they are
     * replicated faster than objects will be requested (sequential reading).
     */
    protected ObjectSet                 alreadyPrefetchedObjects;

    /**
     * @param fileId
     * @param loc
     * @param osdAvailability
     */
    public SequentialPrefetchingStrategy(String fileId, XLocations loc, ServiceAvailability osdAvailability) {
        super(fileId, loc, osdAvailability);
        this.objectSelection = new SequentialObjectSelection();

        this.stripingPolicy = xLoc.getLocalReplica().getStripingPolicy();
        this.alreadyPrefetchedObjects = new ObjectSet();
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        NextRequest next = super.selectNextHook();

        if (next != null) {
            if (this.osdIndex < 0) { // will be executed only first time this method is called
                // save local OSD
                ServiceUUID localOSD = xLoc.getLocalReplica().getOSDForObject(next.objectNo);
                this.osdIndex = xLoc.getLocalReplica().getOSDs().indexOf(localOSD);
            }

            // adjust prefetching count
            // the more preferred objects are requested the more popular the file seems to be => prefetch more
            // objects
            int prefetchingCount = DEFAULT_PREFETCHING_COUNT + this.preferredObjects.size();

            // TODO: at the moment also already local saved objects will be fetched
            // add objects which should be prefetched to lists
            try {
                if (this.preferredObjects.contains(next.objectNo)) {
                    Iterator<Long> objectsIt = stripingPolicy.getObjectsOfOSD(osdIndex, next.objectNo,
                            super.lastObjectNo);
                    objectsIt.next(); // skip current object
                    for (int i = 0; i < prefetchingCount && objectsIt.hasNext(); i++) {
                        Long object = objectsIt.next();
                        if (!this.preferredObjects.contains(object) && !alreadyPrefetchedObjects.contains(object)) {
                            // no preferred object
                            addObject(object, false);
                            alreadyPrefetchedObjects.add(object);

                            if (Logging.isDebug())
                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                        "%s:%d - prefetch object", fileID, object);
                        }
                    }
                }
            } catch (NoSuchElementException e) {
                // just end prefetching
            }
        }
        return next;
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
}
