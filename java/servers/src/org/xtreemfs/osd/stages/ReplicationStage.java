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
import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.AugmentedInternalRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.olp.OverloadProtectedStage;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
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
public class ReplicationStage extends OverloadProtectedStage<AugmentedRequest> {
    
    private final static int  NUM_RQ_TYPES                              = 2;
    private final static int  NUM_INTERNAL_RQ_TYPES                     = 3;
    private final static int  STAGE_ID                                  = 0;
    
    /**
     * fetching an object from another replica
     */
    public static final int   STAGEOP_FETCH_OBJECT                      = -1;
    public static final int   STAGEOP_INTERNAL_OBJECT_FETCHED           = 0;
    public static final int   STAGEOP_CANCEL_REPLICATION_FOR_FILE       = 1;
    public static final int   STAGEOP_START_NEW_REPLICATION_FOR_FILE    = 2;

    private final ObjectDissemination              disseminationLayer;
        
    public ReplicationStage(OSDRequestDispatcher master) {
        super("OSD ReplSt", STAGE_ID, NUM_RQ_TYPES, NUM_INTERNAL_RQ_TYPES);

        this.disseminationLayer = new ObjectDissemination(master);
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        
        disseminationLayer.shutdown();
        super.shutdown();
    }

    /**
     * fetching an object from another replica
     */
    public void fetchObject(String fileId, long objectNo, XLocations xLoc, Capability cap, CowPolicy cow,
            OSDRequest request, AbstractRPCRequestCallback callback) {
        
        enter(STAGEOP_FETCH_OBJECT, new Object[] { fileId, objectNo, xLoc, cap, cow }, request, callback, 
                getInitialPredecessors());
    }

    /**
     * Checks the response from a requested replica.
     * Only for internal use. 
     * 
     * @param usedOSD
     * @param objectList
     * @param error
     */
    public void internalObjectFetched(String fileId, long objectNo, ServiceUUID usedOSD, InternalObjectData data,
            ObjectList objectList, ErrorResponse error) {
        
        enter(STAGEOP_INTERNAL_OBJECT_FETCHED, new Object[] { fileId, objectNo, usedOSD, data, objectList, error }, 
                new AugmentedInternalRequest(STAGEOP_INTERNAL_OBJECT_FETCHED), null);
    }

    /**
     * Stops replication for file.
     * Only for internal use. 
     */
    public void cancelReplicationForFile(String fileId) {
        
        enter(STAGEOP_CANCEL_REPLICATION_FOR_FILE, new Object[] { fileId }, 
                new AugmentedInternalRequest(STAGEOP_CANCEL_REPLICATION_FOR_FILE), null);
    }

    /**
     * Triggers replication for file.
     * Only for internal use. 
     */
    public void triggerReplicationForFile(String fileId) {
        
        enter(STAGEOP_START_NEW_REPLICATION_FOR_FILE, new Object[] { fileId }, 
                new AugmentedInternalRequest(STAGEOP_START_NEW_REPLICATION_FOR_FILE), null);
    }


    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#_processMethod(org.xtreemfs.common.olp.OLPStageRequest)
     */
    @Override
    protected boolean _processMethod(OLPStageRequest<AugmentedRequest> stageRequest) {
        
        final Callback callback = stageRequest.getCallback();
        final int requestedMethod = stageRequest.getStageMethod();
        
        try {
            switch (requestedMethod) {
            case STAGEOP_FETCH_OBJECT: {
                processFetchObject(stageRequest);
                break;
            }
            case STAGEOP_INTERNAL_OBJECT_FETCHED: {
                processInternalObjectFetched(stageRequest);
                break;
            }
            case STAGEOP_CANCEL_REPLICATION_FOR_FILE: {
                processInternalCancelFile(stageRequest);
                break;
            }
            case STAGEOP_START_NEW_REPLICATION_FOR_FILE: {
                processInternalStartFile(stageRequest);
                break;
            }
            default:
                Logging.logMessage(Logging.LEVEL_ERROR, this, "unknown stageop called: %d", requestedMethod);
                break;
            }
        } catch (ErrorResponseException exc) {
            
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            stageRequest.voidMeasurments();
            callback.failed(exc);
        }
        
        return true;
    }

    private void processFetchObject(OLPStageRequest<AugmentedRequest> stageRequest) throws ErrorResponseException {
        
        final AbstractRPCRequestCallback callback = (AbstractRPCRequestCallback) stageRequest.getCallback();
        final String fileId = (String) stageRequest.getArgs()[0];
        final long objectNo = (Long) stageRequest.getArgs()[1];
        final XLocations xLoc = (XLocations) stageRequest.getArgs()[2];
        final Capability cap = (Capability) stageRequest.getArgs()[3];
        final CowPolicy cow = (CowPolicy) stageRequest.getArgs()[4];

        // if replica exist and stripe size of all replicas is the same
        if (xLoc.getNumReplicas() > 1 && !xLoc.getLocalReplica().isComplete()) {
            disseminationLayer.fetchObject(fileId, objectNo, xLoc, cap, cow, stageRequest);
        } else {
            
            // object does not exist locally and no replica exists => hole
            callback.success(new ObjectInformation(ObjectInformation.ObjectStatus.PADDING_OBJECT, null,
                    xLoc.getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo)),stageRequest);
        }
    }

    private void processInternalObjectFetched(StageRequest<AugmentedRequest> stageRequest) {
        
        final String fileId = (String) stageRequest.getArgs()[0];
        final long objectNo = (Long) stageRequest.getArgs()[1];
        final ServiceUUID usedOSD = (ServiceUUID) stageRequest.getArgs()[2];
        final InternalObjectData data = (InternalObjectData) stageRequest.getArgs()[3];
        final ObjectList objectList = (ObjectList) stageRequest.getArgs()[4];
        final ErrorResponse error = (ErrorResponse) stageRequest.getArgs()[5];

        if (error != null) {
            
            if (error.getPosixErrno() == POSIXErrno.POSIX_ERROR_EAGAIN) {
                // it could happen the request is rejected, because the XLoc is outdated caused by removing
                // the replica of this OSD
                // send client error
                disseminationLayer.sendError(fileId, error);
            } else {
                disseminationLayer.objectNotFetched(fileId, usedOSD, objectNo, data);
                if (data.getData() != null)
                    BufferPool.free(data.getData());
            }
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

    private void processInternalCancelFile(StageRequest<AugmentedRequest> stageRequest) {
        
        final String fileId = (String) stageRequest.getArgs()[0];
        disseminationLayer.cancelFile(fileId);
    }
    
    private void processInternalStartFile(StageRequest<AugmentedRequest> stageRequest) {
        
        final String fileId = (String) stageRequest.getArgs()[0];
        disseminationLayer.startNewReplication(fileId);
    }
}