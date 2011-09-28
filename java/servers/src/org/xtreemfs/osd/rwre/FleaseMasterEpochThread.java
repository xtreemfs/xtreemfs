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

    @Override
    public void processMethod(StageRequest<OSDRequest> method) {
        
        final Callback callback = method.getCallback();
        final FleaseMessage message = (FleaseMessage) method.getArgs()[0];
        final String fileId = message.getCellId().toString().replace("file/", "");
        
        switch (method.getStageMethod()) {
            case STAGEOP_GET_MEPOCH: {
                try {
                    message.setMasterEpochNumber(layout.getMasterEpoch(fileId));
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "fetched master epoch for %s: %d", fileId, message.getMasterEpochNumber());
                    }
                } catch (IOException ex) {
                    Logging.logError(Logging.LEVEL_ERROR, this, ex);
                    message.setMasterEpochNumber(-1l);
                }
                callback.success(null);
                break;
            }
            case STAGEOP_SET_MEPOCH: {
                try {
                    layout.setMasterEpoch(fileId, (int)message.getMasterEpochNumber());
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "set master epoch for %s: %d", fileId, message.getMasterEpochNumber());
                    }
                    callback.success(null);
                } catch (IOException ex) {
                    Logging.logError(Logging.LEVEL_ERROR, this, ex);
                }
                break;
            }
            default: {
                throw new IllegalStateException("no such operation: " + method.getStageMethod());
            }
        }
    }

    @Override
    public void sendMasterEpoch(FleaseMessage fm, final Continuation cntntn) {
        enter(STAGEOP_GET_MEPOCH, new Object[]{fm}, null, new Callback() {
            
            @Override
            public boolean success(Object result) {
                
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
            public boolean success(Object result) {
                
                cntntn.processingFinished();
                return true;
            }
            
            @Override
            public void failed(Throwable error) { /* ignored */ }
        });
    }
}