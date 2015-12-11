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
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.*;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.*;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
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

    protected void processPrepareOp(StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();

        try {
            final FileCredentials credentials = (GlobalTypes.FileCredentials) method.getArgs()[0];
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

    /**
     * TODO(janf):
     * check why queued operations carry seemingly redundant information. i.e. often the OSDRequest is attached to the
     * stage request. it has information like the XLocations. nevertheless xlocations are frequently attached to the
     * StageRequest as well. does this serve a purpose or is it simply laziness?
     */
    public void ecWrite(final long newObjectVersion, ReusableBuffer createdViewBuffer, ReusableBuffer existing_data,
                        FileCredentials credentials, final long objectNumber, OSDRequest rq, FileOperationCallback callback) {
        this.enqueueOperation(STAGEOP_EC_WRITE, new Object[]{existing_data, createdViewBuffer, newObjectVersion, credentials, objectNumber},
                              rq, null, callback);
    }

    private void processECWrite(StageRequest method) {
        try {
            ReusableBuffer existingData = (ReusableBuffer) method.getArgs()[0];
            ReusableBuffer newData = (ReusableBuffer) method.getArgs()[1];
            final long newObjectVersion = (Long) method.getArgs()[2];
            FileCredentials credentials = (FileCredentials) method.getArgs()[3];
            final long objectNumber = (Long) method.getArgs()[4];
            String fileId = method.getRequest().getFileId();
            final FileOperationCallback callback = (FileOperationCallback) method.getCallback();

            if (existingData == null) {
                // object content is new
                // get state for file and distribute data to coding devices
            } else {
                // object is updated
                // xor and send diff to coding devices
                while (newData.remaining() > 0) {
                    newData.put(newData.position() - 1, (byte)(existingData.get() ^ newData.get()));
                }
            }

            // now distribute newData to coding devices
            // call callback and give control back to WriteOperation
            // from there call back into ECStage and use replicaupdate policy to distribute xor and verions
            StripedFileState state = files.get(fileId);
            if (state == null) {
                BufferPool.free(newData);
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_EIO, "file is not open!"));
                return;
            }
            state.setCredentials(credentials);

            state.getPolicy().executeDiffDistribute(credentials, objectNumber, newObjectVersion, newData,
                    new ReplicaUpdatePolicy.ClientOperationCallback() {

                        @Override
                        public void finished() {
                            callback.success(newObjectVersion);
                        }

                        @Override
                        public void failed(ErrorResponse error) {
                            callback.failed(error);
                        }
                    });

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void addDiff(final long objectNumber, final int offset, final long newVersion,
                        OSDRequest rq, FileOperationCallback callback) {
        this.enqueueOperation(STAGEOP_EC_DIFF, new Object[] {objectNumber, offset, newVersion}, rq, null, callback);
    }

    private void processAddDiff(StageRequest method) {
        // get state, store diff in state and send ack

        ((FileOperationCallback) method.getCallback()).success(0);
    }

    protected RedundantFileState getState(String fileId) {
        return files.get(fileId);
    }

    protected RedundantFileState removeState(String fileId) {
        return files.remove(fileId);
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

            master.getStorageStage().internalGetMaxObjectNo(fileId, loc.getLocalReplica().getStripingPolicy(),
                    new InternalGetMaxObjectNoCallback() {

                        @Override
                        public void maxObjectNoCompleted(long maxObjNo, long fileSize, long truncateEpoch,
                                                         ErrorResponse error) {
                            eventMaxObjAvail(fileId, maxObjNo, error);
                        }
                    });
        }
        return state;
    }
}
