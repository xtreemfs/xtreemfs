/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.selection;

import java.util.Iterator;

import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;

/**
 * Selects objects in a sequentially way. <br>
 * 29.06.2009
 */
public class SequentialObjectSelection {
    private int            lastSizeOfObjectList;

    private Iterator<Long> iterator = null;

    /**
     * Selects always the first object in set, because it assumes that the first entry will be before next
     * call.
     * 
     * @param objects
     * @return
     * @throws TransferStrategyException
     */
    public long selectNextObject(ObjectSet objects) throws TransferStrategyException {
        // at least one object must be wanted
        assert (!objects.isEmpty());

        // reset iterator
        if (iterator != null)
            iterator = null;

        return objects.getFirst();
    }

    /**
     * Selects sequentially the next object in set. Useful if it is ensured the set does not change during calls.
     * 
     * @param objects
     * @return
     * @throws TransferStrategyException
     */
    public long selectNextObjectOfSameSet(ObjectSet objects) throws TransferStrategyException {
        // initialize on first call
        if (iterator == null)
            iterator = objects.iterator();

        if (iterator.hasNext())
            return iterator.next();
        else
            return -1;
    }
}
