/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.selection.SequentialObjectSelection;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;

/**
 * A sequential strategy which is using simple prefetching. It assumes that objects are read sequentially. <br>
 * 23.06.2009
 */
public class SequentialPrefetchingStrategy extends RandomStrategy {
    /**
     * identifies the sequential strategy in replication flags
     */
    public static final REPL_FLAG             REPLICATION_FLAG          = REPL_FLAG.REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING;

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
