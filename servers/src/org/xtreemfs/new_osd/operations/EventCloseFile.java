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
 * AUTHORS: Björn Kolbeck (ZIB)
 */
package org.xtreemfs.new_osd.operations;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.new_osd.stages.DeletionStage.DeleteObjectsCallback;
import org.xtreemfs.new_osd.stages.StorageStage.CachesFlushedCallback;

/**
 *
 * @author bjko
 */
public class EventCloseFile extends OSDOperation {

    public EventCloseFile(OSDRequestDispatcher master) {
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
        final Boolean isDeleteOnClose = (Boolean) args[1];

        master.getStorageStage().flushCaches(fileId, new CachesFlushedCallback() {

            @Override
            public void cachesFlushed(Exception error) {
                if (isDeleteOnClose) {
                    deleteObjects(fileId);
                } //otherwise finished
            }
        });

    }

    public void deleteObjects(String fileId) {
        master.getDeletionStage().deleteObjects(fileId, null, new DeleteObjectsCallback() {

            @Override
            public void deleteComplete(Exception error) {
                if (error != null) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: " + error);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, error);
                }

            }
        });
    }
}
