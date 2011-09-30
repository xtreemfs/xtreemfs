/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication.selection;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.ObjectSetInfo;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;

/**
 *
 * <br>26.08.2009
 */
public class ObjectSetOSDSelection {
    private Random random = new Random();

    public ServiceUUID selectNextOSD(List<ServiceUUID> osds, Map<ServiceUUID, ObjectSetInfo> objectsOnOSDs,
            long objectNo) throws TransferStrategyException {
        // at least one osd must be available
        assert (osds.size() > 0);

        Collections.shuffle(osds, random);

        Iterator<ServiceUUID> it = osds.iterator();
        ServiceUUID osd = null;
        while (it.hasNext()) {
            osd = it.next();
            ObjectSetInfo objectSetInfo = objectsOnOSDs.get(osd);

            // exit if OSD contains object or OSD is part of a complete replica
            if (objectSetInfo != null && (objectSetInfo.complete || objectSetInfo.set.contains(objectNo)))
                // OSD found
                break;
        }
        return osd;
    }
}
