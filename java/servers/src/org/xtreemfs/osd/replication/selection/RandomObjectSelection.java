/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.selection;

import java.util.Random;

import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;

/**
 * Selects objects randomly. <br>
 * 29.06.2009
 */
public class RandomObjectSelection {
    private Random random = new Random();

    public long selectNextObject(ObjectSet objects) throws TransferStrategyException {
        // at least one object must be wanted
        assert (!objects.isEmpty());

        return objects.getRandom();
    }
}
