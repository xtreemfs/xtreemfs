/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.ec;

import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.flease.*;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.*;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Jan Fajerski
 */
public class ECStage extends RedundancyStage implements FleaseMessageSenderInterface {

    private final Map<String, StripedFileState>    files;
    private final OSDRequestDispatcher             master;
    private int                                    numObjsInFlight;
    private static final int                       MAX_OBJS_IN_FLIGHT         = 10;

    public ECStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength)
            throws IOException {
        super("ECSt", master, sslOpts, maxRequestsQueueLength, Category.ec);
        this.master = master;
        files = new HashMap<String, StripedFileState>();
        numObjsInFlight = 0;

    }

    public void processPrepareOp(StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();

        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];

            StripedFileState state = getState(credentials, loc, false, false);
            processPrepareOp(state, method);

            callback.success(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    public static interface StatusCallback {
        public void statusComplete(Map<String, Map<String, String>> status);
    }

    @Override
    protected void processMethod(StageRequest method) {
        switch (method.getStageMethod()) {
            case STAGEOP_EC_WRITE: {
                externalRequestsInQueue.decrementAndGet();
                processECWrite(method);
                break;
            }
            case STAGEOP_TRUNCATE: {
                externalRequestsInQueue.decrementAndGet();
                break;
            }
            case STAGEOP_CLOSE:  break;
            case STAGEOP_INTERNAL_STATEAVAIL:  break;
            case STAGEOP_INTERNAL_DELETE_COMPLETE:  break;
            case STAGEOP_GETSTATUS:  break;
            default : super.processMethod(method);
        }
    }

    private void processECWrite(StageRequest method) {
    }

    protected RedundantFileState getState(String fileId) {
        return files.get(fileId);
    }

    private StripedFileState getState(FileCredentials credentials, XLocations loc, boolean forceReset,
                                         boolean invalidated) throws IOException {

        final String fileId = credentials.getXcap().getFileId();

        StripedFileState state = files.get(fileId);
        if (state == null) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "open file: " + fileId);
            // "open" file
            state = new StripedFileState(fileId, loc, master.getConfig().getUUID(), osdClient);
            files.put(fileId, state);
            state.setCredentials(credentials);
            //state.setInvalidated(invalidated);
            //cellToFileId.put(state.getPolicy().getCellId(), fileId);

            //master.getStorageStage().internalGetMaxObjectNo(fileId, loc.getLocalReplica().getStripingPolicy(),
                    //new InternalGetMaxObjectNoCallback() {

                        //@Override
                        //public void maxObjectNoCompleted(long maxObjNo, long fileSize, long truncateEpoch,
                                                         //ErrorResponse error) {
                            //eventMaxObjAvail(fileId, maxObjNo, fileSize, truncateEpoch, error);
                        //}
                    //});
        }
        return state;
    }
}
