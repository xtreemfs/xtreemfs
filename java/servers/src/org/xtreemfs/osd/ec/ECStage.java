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

    public static final int STAGEOP_EC_WRITE                  = 1;
    public static final int STAGEOP_CLOSE                     = 2;
    public static final int STAGEOP_PROCESS_FLEASE_MSG        = 3;
    public static final int STAGEOP_PREPAREOP                 = 5;
    public static final int STAGEOP_TRUNCATE                  = 6;
    public static final int STAGEOP_GETSTATUS                 = 7;

    public static final int STAGEOP_INTERNAL_AUTHSTATE        = 10;
    public static final int STAGEOP_INTERNAL_OBJFETCHED       = 11;

    public static final int STAGEOP_LEASE_STATE_CHANGED       = 13;
    public static final int STAGEOP_INTERNAL_STATEAVAIL       = 14;
    public static final int STAGEOP_INTERNAL_DELETE_COMPLETE  = 15;
    public static final int STAGEOP_FORCE_RESET               = 16;
    public static final int STAGEOP_INTERNAL_MAXOBJ_AVAIL     = 17;
    public static final int STAGEOP_INTERNAL_BACKUP_AUTHSTATE = 18;

    public static final int STAGEOP_SETVIEW                   = 21;
    public static final int STAGEOP_INVALIDATEVIEW            = 22;
    public static final int STAGEOP_FETCHINVALIDATED          = 23;

    public static enum Operation {
        READ,
        WRITE,
        TRUNCATE,
        INTERNAL_UPDATE,
        INTERNAL_TRUNCATE
    };

    private final Map<String, StripedFileState>    files;

    private final Map<ASCIIString, String>         cellToFileId;

    private final OSDRequestDispatcher             master;

    private int                                    numObjsInFlight;

    private static final int                       MAX_OBJS_IN_FLIGHT         = 10;

    private static final int                       MAX_PENDING_PER_FILE       = 10;

    private static final int                       MAX_EXTERNAL_REQUESTS_IN_Q = 250;


    private final AtomicInteger                    externalRequestsInQueue;

    public ECStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength)
            throws IOException {
        super("ECSt", master, sslOpts, maxRequestsQueueLength, Category.ec);
        this.master = master;
        files = new HashMap<String, StripedFileState>();
        cellToFileId = new HashMap<ASCIIString, String>();
        numObjsInFlight = 0;
        externalRequestsInQueue = new AtomicInteger(0);

    }

    protected void enqueueExternalOperation(int stageOp, Object[] arguments, OSDRequest request,
            ReusableBuffer createdViewBuffer, Object callback) {
        if (externalRequestsInQueue.get() >= MAX_EXTERNAL_REQUESTS_IN_Q) {
            Logging.logMessage(Logging.LEVEL_WARN, this,
                    "EC stage is overloaded, request %d for %s dropped", request.getRequestId(),
                    request.getFileId());
            request.sendInternalServerError(new IllegalStateException(
                    "EC stage is overloaded, request dropped"));

            // Make sure that the data buffer is returned to the pool if
            // necessary, as some operations create view buffers on the
            // data. Otherwise, a 'finalized but not freed before' warning
            // may occur.
            if (createdViewBuffer != null) {
                assert (createdViewBuffer.getRefCount() >= 2);
                BufferPool.free(createdViewBuffer);
            }

        } else {
            externalRequestsInQueue.incrementAndGet();
            this.enqueueOperation(stageOp, arguments, request, createdViewBuffer, callback);
        }
    }

    public void prepareOperation(FileCredentials credentials, XLocations xloc, long objNo, long objVersion,
            Operation op, FileOperationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_PREPAREOP, new Object[] { credentials, xloc, objNo, objVersion, op },
                request, null, callback);
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

    public void getStatus(StatusCallback callback) {
        this.enqueueOperation(STAGEOP_GETSTATUS, new Object[] {}, null, callback);
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
