/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.selection;

import java.util.List;
import java.util.Random;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;

/**
 * Selects OSDs randomly. <br>
 * 29.06.2009
 */
public class RandomOSDSelection {
    private Random random = new Random();

    public ServiceUUID selectNextOSD(List<ServiceUUID> osds) throws TransferStrategyException {
        // at least one osd must be available
        assert (osds.size() > 0);
        return osds.get(random.nextInt(osds.size()));
    }
}
