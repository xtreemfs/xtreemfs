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

import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;

/**
 * A sequential strategy which is using simple prefetching. It assumes that objects are read sequentially. <br>
 * 23.06.2009
 */
public class SequentialPrefetchingStrategy extends SequentialStrategy {
    /**
     * identifies the sequential strategy in replication flags
     */
    // TODO: after protocol change: use correct flag
    public static final int    REPLICATION_FLAG          = Constants.REPL_FLAG_STRATEGY_SIMPLE;

    public static int          DEFAULT_PREFETCHING_COUNT = 5;

    /**
     * specifies how many objects will be prefetched
     */
    private int                prefetchingCount;

    private StripingPolicyImpl stripingPolicy;

    private int                osdIndex                  = -1;

    private long               lastObject;

    /**
     * @param fileId
     * @param loc
     * @param osdAvailability
     */
    public SequentialPrefetchingStrategy(String fileId, XLocations loc, ServiceAvailability osdAvailability,
            long lastObjectNo) {
        super(fileId, loc, osdAvailability);
        this.stripingPolicy = xLoc.getLocalReplica().getStripingPolicy();
        this.lastObject = lastObjectNo;
        this.prefetchingCount = DEFAULT_PREFETCHING_COUNT;
    }

    @Override
    protected NextRequest selectNextHook() throws TransferStrategyException {
        NextRequest next = super.selectNextHook();

        if (next != null) {
            if (this.osdIndex < 0) { // happens only first time this method is called
                // save local OSD
                ServiceUUID localOSD = xLoc.getLocalReplica().getOSDForObject(next.objectNo);
                this.osdIndex = xLoc.getLocalReplica().getOSDs().indexOf(localOSD);
            }

            // adjust prefetching count
            // the more preferred objects are requested the more popular the file seems to be => prefetch more
            // objects
            prefetchingCount = DEFAULT_PREFETCHING_COUNT + this.preferredObjects.size();

            // TODO: at the moment also already local saved objects will be fetched
            // add objects which should be prefetched to lists
            if (this.preferredObjects.contains(next.objectNo)) {
                Iterator<Long> objectsIt = stripingPolicy
                        .getObjectsOfOSD(osdIndex, next.objectNo, lastObject);
                objectsIt.next(); // skip first object
                for (int i = 0; i < prefetchingCount && objectsIt.hasNext(); i++) {
                    long object = objectsIt.next();
                    if (!this.preferredObjects.contains(object)) // no preferred object
                        addObject(object, false);
                }
            }
        }
        return next;
    }
}
