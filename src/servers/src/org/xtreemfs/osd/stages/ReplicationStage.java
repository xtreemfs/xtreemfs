/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
package org.xtreemfs.osd.stages;

import java.io.IOException;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.include.foundation.json.JSONException;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectDissemination;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.ObjectInformation;

/**
 * 
 * 09.09.2008
 * 
 * @author clorenz
 */
public class ReplicationStage extends Stage {
    /**
     * fetching an object from another replica
     */
    public static final int STAGEOP_FETCH_OBJECT = 1;

    public static final int STAGEOP_INTERNAL_OBJECT_FETCHED = 2;

    public static final int STAGEOP_CANCEL_REPLICATION_FOR_FILE = 3;

    private OSDRequestDispatcher master;

    private ObjectDissemination disseminationLayer;

    public ReplicationStage(OSDRequestDispatcher master) {
        super("OSD Replication Stage");

        this.master = master;
        this.disseminationLayer = new ObjectDissemination(master);
    }

    /**
     * fetching an object from another replica
     */
    public void fetchObject(String fileId, long objectNo, XLocations xLoc, Capability cap, CowPolicy cow,
            final OSDRequest request, FetchObjectCallback listener) {
        this.enqueueOperation(STAGEOP_FETCH_OBJECT, new Object[] { fileId, objectNo, xLoc, cap, cow },
                request, listener);
    }

    public static interface FetchObjectCallback {
        public void fetchComplete(ObjectInformation objectInfo, Exception error);
    }

    /**
     * Checks the response from a requested replica.
     * Only for internal use. 
     */
    public void internalObjectFetched(String fileId, long objectNo, ObjectData data) {
        this.enqueueOperation(STAGEOP_INTERNAL_OBJECT_FETCHED, new Object[] { fileId, objectNo, data }, null,
                null);
    }

    /**
     * Stops replication for file.
     * Only for internal use. 
     */
    public void cancelReplicationForFile(String fileId) {
        this.enqueueOperation(STAGEOP_CANCEL_REPLICATION_FOR_FILE, new Object[] { fileId }, null,
                null);
    }

    @Override
    protected void processMethod(StageRequest rq) {
        try {
            switch (rq.getStageMethod()) {
            case STAGEOP_FETCH_OBJECT: {
                processFetchObject(rq);
                break;
            }
            case STAGEOP_INTERNAL_OBJECT_FETCHED: {
                processInternalObjectFetched(rq);
                break;
            }
            case STAGEOP_CANCEL_REPLICATION_FOR_FILE: {
                processInternalCancelFile(rq);
                break;
            }
            default:
                rq.sendInternalServerError(new RuntimeException("unknown stage op request"));
            }
        } catch (Throwable exc) {
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            rq.sendInternalServerError(exc);
            return;
        }
    }

    private void processFetchObject(StageRequest rq) throws IOException, JSONException {
        final FetchObjectCallback callback = (FetchObjectCallback) rq.getCallback();
        String fileId = (String) rq.getArgs()[0];
        long objectNo = (Long) rq.getArgs()[1];
        XLocations xLoc = (XLocations) rq.getArgs()[2];
        Capability cap = (Capability) rq.getArgs()[3];
        CowPolicy cow = (CowPolicy) rq.getArgs()[4];

        // if replica exist
        if (xLoc.getNumReplicas() > 1) {
            disseminationLayer.fetchObject(fileId, objectNo, xLoc, cap, cow, rq);
        } else
            // object does not exist locally and no replica exists => hole
            callback.fetchComplete(new ObjectInformation(ObjectInformation.ObjectStatus.PADDING_OBJECT, null,
                    xLoc.getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo)), null);
    }

    private void processInternalObjectFetched(StageRequest rq) {
        String fileId = (String) rq.getArgs()[0];
        long objectNo = (Long) rq.getArgs()[1];
        ObjectData data = (ObjectData) rq.getArgs()[2];

        if (data != null && data.getData().limit() != 0)
            disseminationLayer.objectFetched(fileId, objectNo, data);
        else {
            // data could not be fetched
            disseminationLayer.objectNotFetched(fileId, objectNo);
            if(data != null)
                BufferPool.free(data.getData());
        }
    }

    private void processInternalCancelFile(StageRequest rq) {
        String fileId = (String) rq.getArgs()[0];
        disseminationLayer.cancelFile(fileId);
    }
}
