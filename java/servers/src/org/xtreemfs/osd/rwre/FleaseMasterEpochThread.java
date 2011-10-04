/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.SimpleStageQueue;
import org.xtreemfs.common.stage.Stage;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.flease.MasterEpochHandlerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.storage.StorageLayout;

/**
 *
 * @author bjko
 */
public class FleaseMasterEpochThread extends Stage<OSDRequest> implements MasterEpochHandlerInterface {
    
    private static final int MAX_QUEUE_LENGTH   = 1000;
    
    private static final int STAGEOP_GET_MEPOCH = 1;
    private static final int STAGEOP_SET_MEPOCH = 2;

    private final StorageLayout layout;

    public FleaseMasterEpochThread(StorageLayout layout) {
        super("FlMEpoThr", new SimpleStageQueue<OSDRequest>(MAX_QUEUE_LENGTH));
        
        this.layout = layout;
    }


    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#generateStageRequest(int, java.lang.Object[], java.lang.Object, org.xtreemfs.common.stage.Callback)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <S extends StageRequest<OSDRequest>> S generateStageRequest(int stageMethodId, Object[] args,
            OSDRequest request, Callback callback) {

        return (S) new StageRequest<OSDRequest>(stageMethodId, args, request, callback) { };
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#processMethod(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    protected <S extends StageRequest<OSDRequest>> boolean processMethod(S stageRequest) {
        
        final Callback callback = stageRequest.getCallback();
        final FleaseMessage message = (FleaseMessage) stageRequest.getArgs()[0];
        final String fileId = message.getCellId().toString().replace("file/", "");
        
        try {
            switch (stageRequest.getStageMethod()) {
                case STAGEOP_GET_MEPOCH: {
                    try {
                        message.setMasterEpochNumber(layout.getMasterEpoch(fileId));
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "fetched master epoch for %s: %d", fileId, 
                                    message.getMasterEpochNumber());
                        }
                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_ERROR, this, ex);
                        message.setMasterEpochNumber(-1l);
                    }
                    callback.success(null, stageRequest);
                    break;
                }
                case STAGEOP_SET_MEPOCH: {
                    try {
                        layout.setMasterEpoch(fileId, (int)message.getMasterEpochNumber());
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "set master epoch for %s: %d", fileId, 
                                    message.getMasterEpochNumber());
                        }
                        callback.success(null, stageRequest);
                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_ERROR, this, ex);
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("no such operation: " + stageRequest.getStageMethod());
                }
            }
        } catch (ErrorResponseException e) {
            callback.failed(e);
        }
        
        return true;
    }

    @Override
    public void sendMasterEpoch(FleaseMessage fm, final Continuation cntntn) {
        enter(STAGEOP_GET_MEPOCH, new Object[]{fm}, null, new Callback() {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                cntntn.processingFinished();
                return true;
            }
            
            @Override
            public void failed(Throwable error) { /* ignored */ }
        });
    }

    @Override
    public void storeMasterEpoch(FleaseMessage fm, final Continuation cntntn) {
        enter(STAGEOP_SET_MEPOCH, new Object[]{fm}, null, new Callback() {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                cntntn.processingFinished();
                return true;
            }
            
            @Override
            public void failed(Throwable error) { /* ignored */ }
        });
    }
}