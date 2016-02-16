/*
 * Copyright (c) 2008-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectDissemination;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;

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

    public static final int STAGEOP_START_NEW_REPLICATION_FOR_FILE = 4;

    private OSDRequestDispatcher master;

    private ObjectDissemination disseminationLayer;
    
    public ReplicationStage(OSDRequestDispatcher master, int maxRequestsQueueLength) {
        super("OSD ReplSt", maxRequestsQueueLength);

        // FIXME: test stuff
//        Monitoring.enable();

        this.master = master;
        this.disseminationLayer = new ObjectDissemination(master);
    }

    @Override
    public void shutdown() {
        disseminationLayer.shutdown();
        super.shutdown();
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
        public void fetchComplete(ObjectInformation objectInfo, ErrorResponse error);
    }

    /**
     * Checks the response from a requested replica.
     * Only for internal use. 
     * @param usedOSD
     * @param objectList
     * @param error
     */
    public void internalObjectFetched(String fileId, long objectNo, ServiceUUID usedOSD, InternalObjectData data,
            ObjectList objectList, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_OBJECT_FETCHED, new Object[] { fileId, objectNo, usedOSD,
                data, objectList, error }, null, null);
    }

    /**
     * Stops replication for file.
     * Only for internal use. 
     */
    public void cancelReplicationForFile(String fileId) {
        this.enqueueOperation(STAGEOP_CANCEL_REPLICATION_FOR_FILE, new Object[] { fileId }, null,
                null);
    }

    /**
     * Triggers replication for file.
     * Only for internal use. 
     */
    public void triggerReplicationForFile(String fileId) {
        this.enqueueOperation(STAGEOP_START_NEW_REPLICATION_FOR_FILE, new Object[] { fileId }, null,
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
            case STAGEOP_START_NEW_REPLICATION_FOR_FILE: {
                processInternalStartFile(rq);
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

        // if replica exist and stripe size of all replicas is the same
        if (xLoc.getNumReplicas() > 1 && !xLoc.getLocalReplica().isComplete())
            disseminationLayer.fetchObject(fileId, objectNo, xLoc, cap, cow, rq);
        else
            // object does not exist locally and no replica exists => hole
            callback.fetchComplete(new ObjectInformation(ObjectInformation.ObjectStatus.PADDING_OBJECT, null,
                    xLoc.getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo)), null);
    }

    private void processInternalObjectFetched(StageRequest rq) {
        String fileId = (String) rq.getArgs()[0];
        long objectNo = (Long) rq.getArgs()[1];
        final ServiceUUID usedOSD = (ServiceUUID) rq.getArgs()[2];
        InternalObjectData data = (InternalObjectData) rq.getArgs()[3];
        ObjectList objectList = (ObjectList) rq.getArgs()[4];
        final ErrorResponse error = (ErrorResponse) rq.getArgs()[5];

        if (error != null) {
            if (error.getErrorType() == ErrorType.INVALID_VIEW) {
                // it could happen the request is rejected, because the XLoc is outdated caused by removing
                // the replica of this OSD send client error
                disseminationLayer.objectNotFetchedBecauseViewError(fileId, usedOSD, objectNo, error);
            } else {
                disseminationLayer.objectNotFetchedBecauseError(fileId, usedOSD, objectNo, error);
            }
            if (data != null && data.getData() != null)
                BufferPool.free(data.getData());
        } else {
            // decode object list, if attached
            if (objectList != null) {
                try {
                    ObjectSet objectSet = new ObjectSet(objectList.getStripeWidth(), objectList
                            .getFirst(), objectList.getSet().toByteArray());
                    disseminationLayer.objectSetFetched(fileId, usedOSD, objectSet, objectList.getSet()
                            .size());
                } catch (IOException e) {
                    Logging.logError(Logging.LEVEL_ERROR, this, e);
                } catch (ClassNotFoundException e) {
                    Logging.logError(Logging.LEVEL_ERROR, this, e);
                }
            }

            if (data != null && data.getData() != null && data.getData().limit() != 0)
                disseminationLayer.objectFetched(fileId, objectNo, usedOSD, data);
            else {
                // data could not be fetched
                disseminationLayer.objectNotFetched(fileId, usedOSD, objectNo, data);
                
                if (data != null)
                    BufferPool.free(data.getData());
            }
        }
    }

    private void processInternalCancelFile(StageRequest rq) {
        String fileId = (String) rq.getArgs()[0];
        disseminationLayer.cancelFile(fileId);
    }

    /**
     * @param rq
     */
    private void processInternalStartFile(StageRequest rq) {
        String fileId = (String) rq.getArgs()[0];
        disseminationLayer.startNewReplication(fileId);
    }
}
