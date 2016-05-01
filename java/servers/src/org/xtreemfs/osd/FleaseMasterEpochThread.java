/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.IOException;

import org.xtreemfs.foundation.flease.MasterEpochHandlerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.storage.StorageLayout;

/**
 *
 * @author bjko
 */
public class FleaseMasterEpochThread extends Stage implements MasterEpochHandlerInterface {
    private static final int STAGEOP_GET_MEPOCH = 1;
    private static final int STAGEOP_SET_MEPOCH = 2;

    private final StorageLayout layout;

    public FleaseMasterEpochThread(StorageLayout layout, int maxRequestsQueueLength) {
        super("FlMEpoThr", maxRequestsQueueLength);
        this.layout = layout;
    }

    @Override
    protected void processMethod(StageRequest method) {
        final Continuation callback = (Continuation) method.getCallback();
        final FleaseMessage message = (FleaseMessage) method.getArgs()[0];
        final String fileId = FleasePrefixHandler.stripPrefix(message.getCellId()).toString();

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
                callback.processingFinished();
                break;
            }
            case STAGEOP_SET_MEPOCH: {
                try {
                    layout.setMasterEpoch(fileId, (int)message.getMasterEpochNumber());
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "set master epoch for %s: %d", fileId, message.getMasterEpochNumber());
                    }
                    callback.processingFinished();
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
    public void sendMasterEpoch(FleaseMessage fm, Continuation cntntn) {
        this.enqueueOperation(STAGEOP_GET_MEPOCH, new Object[]{fm}, null, cntntn);
    }

    @Override
    public void storeMasterEpoch(FleaseMessage fm, Continuation cntntn) {
        this.enqueueOperation(STAGEOP_SET_MEPOCH, new Object[]{fm}, null, cntntn);
    }

}
