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
package org.xtreemfs.osd.replication.selection;

import java.util.HashMap;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;

/**
 * Selects the OSDs sequentially (like Round-Robin). It iterates not strictly through the replicas, because it
 * uses different position-pointers for every stripe. This is necessary, otherwise it could happen some
 * replicas will be never used, which could cause in an infinite loop.<br>
 * 29.06.2009
 */
public class RoundRobinOSDSelection {
    /**
     * contains the position (replica) in xLoc list of the next OSD which should be used for this stripe<br>
     * key: stripe<br>
     * value: index of replica
     */
    private HashMap<Integer, Integer> nextOSDforObject;

    private final int                 maxStripeWidth;

    private int                       lastKnownNumberOfReplicas;
    
    /**
     * Creates a new instance. Sets an initial value for the number of replicas.
     * @param maxStripeWidth
     */
    public RoundRobinOSDSelection(int maxStripeWidth) {
        this.maxStripeWidth = maxStripeWidth;
        this.nextOSDforObject = new HashMap<Integer, Integer>(maxStripeWidth);
        this.lastKnownNumberOfReplicas = 0;

        // set first replica as start position for all stripes
        for (int i = 0; i < maxStripeWidth; i++)
            this.nextOSDforObject.put(i, 0);
    }

    /**
     * Selects an OSD.
     * @param osds
     * @param objectNo
     * @return
     * @throws TransferStrategyException
     */
    public ServiceUUID selectNextOSD(List<ServiceUUID> osds, long objectNo) throws TransferStrategyException {
        // at least one osd must be available
        assert (osds.size() > 0);

        // update number of replicas
        if (lastKnownNumberOfReplicas != osds.size())
            lastKnownNumberOfReplicas = osds.size();

        int positionOfNextOSD = getPositionOfNextOSD(objectNo);
        increasePositionOfOSD(objectNo);

        // each OSD in list represents a replica
        return osds.get(positionOfNextOSD);
    }

    /**
     * returns the position of the next using OSD (from replica) for the stripe of the given object
     * 
     * @param objectNo
     * @return
     */
    protected int getPositionOfNextOSD(long objectNo) {
        return nextOSDforObject.get((int) (objectNo % maxStripeWidth));
    }

    /**
     * increases the position of the next using OSD for the stripe of the given object
     * 
     * @param objectNo
     */
    protected void increasePositionOfOSD(long objectNo) {
        int oldPosition = nextOSDforObject.get((int) (objectNo % maxStripeWidth));
        // do not count local replica
        nextOSDforObject.put((int) (objectNo % maxStripeWidth), ++oldPosition % lastKnownNumberOfReplicas);
    }
}
