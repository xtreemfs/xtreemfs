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
