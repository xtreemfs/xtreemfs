/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
