/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
package org.xtreemfs.osd.operations;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Writes an object to disk without sending GMax-messages. 
 *
 * 01.04.2009
 */
public class EventWriteObject extends OSDOperation {

    public EventWriteObject(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startRequest(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");

    }

    @Override
    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean requiresCapability() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startInternalEvent(Object[] args) {
        final String fileId = (String) args[0];
        final long objectNo = (Long) args[1];
        final ReusableBuffer data = (ReusableBuffer) args[2];
        final XLocations xloc = (XLocations) args[3];
        final CowPolicy cow = (CowPolicy) args[4];

        master.getStorageStage().writeObjectWithoutGMax(fileId, objectNo, xloc.getLocalReplica().getStripingPolicy(), 0,
                data, cow, xloc, null, new WriteObjectCallback() {
                    @Override
                    public void writeComplete(OSDWriteResponse result, Exception error) {
                        if (error != null) {
                            Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: "
                                    + error);
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, error);
                        }
                    }
                });
    }
}
