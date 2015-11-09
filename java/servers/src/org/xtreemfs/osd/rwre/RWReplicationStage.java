/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.*;
import org.xtreemfs.osd.stages.PreprocStage.InvalidateXLocSetCallback;
import org.xtreemfs.osd.stages.StorageStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.LeaseState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersion;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.XLocSetVersionState;

/**
 * 
 * @author bjko
 */
public class RWReplicationStage extends RedundancyStage implements FleaseMessageSenderInterface {



    private final HashMap<String, ReplicatedFileState>  files;
    private int                                         numObjsInFlight;
    private final Queue<ReplicatedFileState>            filesInReset;
    private final OSDRequestDispatcher                  master;
    private static final int                            MAX_OBJS_IN_FLIGHT         = 10;

    public RWReplicationStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength)
            throws IOException {
        super("RWReplSt", master, sslOpts, maxRequestsQueueLength);
        this.master = master;
        files = new HashMap<String, ReplicatedFileState>();
        numObjsInFlight = 0;
        filesInReset = new LinkedList();

    }

    public void eventReplicaStateAvailable(String fileId, ReplicaStatus localState, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_STATEAVAIL, new Object[] { fileId, localState, error }, null, null);
    }

    public void eventForceReset(FileCredentials credentials, XLocations xloc) {
        this.enqueueOperation(STAGEOP_FORCE_RESET, new Object[] { credentials, xloc }, null, null);
    }

    public void eventDeleteObjectsComplete(String fileId, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_DELETE_COMPLETE, new Object[] { fileId, error }, null, null);
    }

    void eventObjectFetched(String fileId, ObjectVersionMapping object, InternalObjectData data, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_OBJFETCHED, new Object[] { fileId, object, data, error }, null, null);
    }

    void eventSetAuthState(String fileId, AuthoritativeReplicaState authState, ReplicaStatus localState,
            ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_AUTHSTATE, new Object[] { fileId, authState, localState, error }, null,
                null);
    }

    void eventMaxObjAvail(String fileId, long maxObjVer, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_MAXOBJ_AVAIL, new Object[] { fileId, maxObjVer, error }, null, null);
    }

    public void eventBackupReplicaReset(String fileId, AuthoritativeReplicaState authState, ReplicaStatus localState,
            FileCredentials credentials, XLocations xloc) {
        this.enqueueOperation(STAGEOP_INTERNAL_BACKUP_AUTHSTATE, new Object[]{fileId, authState, localState,
                credentials, xloc}, null, null);
    }

    private void executeSetAuthState(final ReplicaStatus localState, final AuthoritativeReplicaState authState,
            ReplicatedFileState state, final String fileId) {
        // Create a list of missing objects.
        Map<Long, Long> objectsToBeDeleted = new HashMap();
        for (ObjectVersion localObject : localState.getObjectVersionsList()) {
            // Never delete any object which is newer than auth state!
            if (localObject.getObjectVersion() <= authState.getMaxObjVersion()) {
                objectsToBeDeleted.put(localObject.getObjectNumber(), localObject.getObjectVersion());
            }
        }
        // Delete everything that is older or not part of the authoritative state.
        for (ObjectVersionMapping authObject : authState.getObjectVersionsList()) {
            Long version = objectsToBeDeleted.get(authObject.getObjectNumber());
            if ((version != null) && (version == authObject.getObjectVersion())) {
                objectsToBeDeleted.remove(authObject.getObjectNumber());
            }
        }
        Map<Long, ObjectVersionMapping> missingObjects = new HashMap();
        for (ObjectVersionMapping authObject : authState.getObjectVersionsList()) {
            missingObjects.put(authObject.getObjectNumber(), authObject);
        }
        for (ObjectVersion localObject : localState.getObjectVersionsList()) {
            ObjectVersionMapping object = missingObjects.get(localObject.getObjectNumber());
            if ((object != null) && (localObject.getObjectVersion() >= object.getObjectVersion())) {
                missingObjects.remove(localObject.getObjectNumber());
            }
        }
        if (!missingObjects.isEmpty() || !objectsToBeDeleted.isEmpty()
                || (localState.getTruncateEpoch() < authState.getTruncateEpoch())) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "(R:%s) replica RESET required updates for: %s", localID, state.getFileId());
            }
            state.setObjectsToFetch(new LinkedList(missingObjects.values()));
            filesInReset.add(state);
            // Start by deleting the old objects.
            master.getStorageStage().deleteObjects(fileId, state.getStripingPolicy(), authState.getTruncateEpoch(),
                    objectsToBeDeleted, new DeleteObjectsCallback() {

                        @Override
                        public void deleteObjectsComplete(ErrorResponse error) {
                            eventDeleteObjectsComplete(fileId, error);
                        }
                    });
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "(R:%s) replica RESET finished (replica is up-to-date): %s", localID, state.getFileId());
            }
            doOpen(state);
        }
    }

    private void processBackupAuthoritativeState(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final AuthoritativeReplicaState authState = (AuthoritativeReplicaState) method.getArgs()[1];
            final ReplicaStatus localState = (ReplicaStatus) method.getArgs()[2];
            final FileCredentials credentials = (FileCredentials) method.getArgs()[3];
            final XLocations loc = (XLocations) method.getArgs()[4];

            ReplicatedFileState state = getState(credentials, loc, true, false);

            // Cancel the Request if the file has been invalidated.
            if (state.isInvalidated()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.replication, this,
                        "(R:%s) auth state ignored, file is invalidated %s", localID, fileId);
                return;
            }

            switch (state.getState()) {
            case INITIALIZING:
            case OPEN:
            case WAITING_FOR_LEASE: {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "(R:%s) enqueued backup reset for file %s", localID, fileId);
                state.addPendingRequest(method);
                break;
            }
            case BACKUP: {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "(R:%s) backup reset triggered by AUTHSTATE request for file %s", localID, fileId);
                state.setState(ReplicatedFileState.LocalState.RESET);
                executeSetAuthState(localState, authState, state, fileId);
                break;
            }
            case RESET:
            default: {
                // Ignore.
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                        "(R:%s) auth state ignored, already in reset for file %s", localID, fileId);
            }
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processSetAuthoritativeState(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final AuthoritativeReplicaState authState = (AuthoritativeReplicaState) method.getArgs()[1];
            final ReplicaStatus localState = (ReplicaStatus) method.getArgs()[2];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[3];

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                        "(R:%s) set AUTH for unknown file: %s", localID, fileId);
                return;
            }

            if (error != null) {
                failed(state, error, "processSetAuthoritativeState");
            } else {
                executeSetAuthState(localState, authState, state, fileId);
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processDeleteObjectsComplete(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[1];

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "(R:%s) deleted all objects requested by RESET for %s with %s", localID, state.getFileId(),
                            ErrorUtils.formatError(error));
                }
                if (error != null) {
                    failed(state, error, "processDeleteObjectsComplete");
                } else {
                    fetchObjects();
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void fetchObjects() {

        while (numObjsInFlight < MAX_OBJS_IN_FLIGHT) {

            ReplicatedFileState file = filesInReset.poll();
            if (file == null)
                break;

            if (!file.getObjectsToFetch().isEmpty()) {
                ObjectVersionMapping o = file.getObjectsToFetch().remove(0);
                file.setNumObjectsPending(file.getNumObjectsPending() + 1);
                numObjsInFlight++;
                fetchObject(file.getFileId(), o);
            }

            if (!file.getObjectsToFetch().isEmpty()) {
                filesInReset.add(file);
            }
        }
    }

    private void fetchObject(final String fileId, final ObjectVersionMapping record) {
        ReplicatedFileState state = files.get(fileId);
        if (state == null) {
            return;
        }

        try {
            final ServiceUUID osd = new ServiceUUID(record.getOsdUuidsList().get(0));
            // fetch that object
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "(R:%s) file %s, fetch object %d (version %d) from %s", localID, fileId,
                        record.getObjectNumber(), record.getObjectVersion(), osd);

            RPCResponse r = osdClient.xtreemfs_rwr_fetch(osd.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, state.getCredentials(), fileId, record.getObjectNumber(),
                    record.getObjectVersion());
            r.registerListener(new RPCResponseAvailableListener() {

                @Override
                public void responseAvailable(RPCResponse r) {
                    try {
                        ObjectData metadata = (ObjectData) r.get();
                        InternalObjectData data = new InternalObjectData(metadata, r.getData());
                        eventObjectFetched(fileId, record, data, null);
                    } catch (PBRPCException ex) {
                        // Transform exception into correct ErrorResponse.
                        // TODO(mberlin): Generalize this functionality by returning "Throwable" instead of
                        //                "ErrorResponse" to the event* functions.
                        //                The "ErrorResponse" shall be created in the last 'step' at the
                        //                invocation of failed().
                        eventObjectFetched(fileId,
                                           record,
                                           null,
                                           ErrorUtils.getErrorResponse(ex.getErrorType(), ex.getPOSIXErrno(), ex.toString(), ex));
                    } catch (Exception ex) {
                        eventObjectFetched(fileId,
                                           record,
                                           null,
                                           ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_NONE, ex.toString(), ex));
                    } finally {
                        r.freeBuffers();
                    }
                }
            });
        } catch (IOException ex) {
            eventObjectFetched(fileId, record, null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }

    }

    private void processObjectFetched(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ObjectVersionMapping record = (ObjectVersionMapping) method.getArgs()[1];
            final InternalObjectData data = (InternalObjectData) method.getArgs()[2];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[3];

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {

                if (error != null) {
                    numObjsInFlight--;
                    fetchObjects();

                    failed(state, error, "processObjectFetched");
                } else if (data.getData() == null) {
                    // data is null if object was deleted meanwhile.
                    numObjsInFlight--;
                    fetchObjects();

                    ErrorResponse generatedError = ErrorResponse
                            .newBuilder()
                            .setErrorType(RPC.ErrorType.INTERNAL_SERVER_ERROR)
                            .setErrorMessage("Fetching a missing object failed because no data was returned. The object was probably deleted meanwhile.")
                            .build();
                    failed(state, generatedError, "processObjectFetched");
                } else {
                    final int bytes = data.getData().remaining();
                    master.getStorageStage().writeObjectWithoutGMax(fileId, record.getObjectNumber(),
                            state.getStripingPolicy(), 0, data.getData(), CowPolicy.PolicyNoCow, null, false,
                            record.getObjectVersion(), null, new WriteObjectCallback() {

                                @Override
                                public void writeComplete(OSDWriteResponse result, ErrorResponse error) {
                                    if (error != null) {
                                        Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                                                "cannot write object locally: %s", ErrorUtils.formatError(error));
                                    }
                                }
                            });
                    master.getPreprocStage().pingFile(fileId);
                    master.objectReplicated();
                    master.replicatedDataReceived(bytes);

                    numObjsInFlight--;
                    final int numPendingFile = state.getNumObjectsPending() - 1;
                    state.setNumObjectsPending(numPendingFile);
                    state.getPolicy().objectFetched(record.getObjectVersion());
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "(R:%s) fetched object for replica, file %s, remaining %d", localID, fileId,
                                numPendingFile);
                    fetchObjects();
                    if (numPendingFile == 0) {
                        // reset complete!
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "(R:%s) RESET complete for file %s", localID, fileId);
                        doOpen(state);
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processReplicaStateAvailExecReset(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ReplicaStatus localReplicaState = (ReplicaStatus) method.getArgs()[1];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[2];

            final ReplicatedFileState state = files.get(fileId);
            if (state != null) {
                if (error != null) {
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                            "local state for %s failed: %s", state.getFileId(), error);
                    failed(state, error, "processReplicaStateAvailExecReset");
                } else {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "(R:%s) local state for %s available.", localID, state.getFileId());
                    }
                    state.getPolicy().executeReset(state.getCredentials(), localReplicaState,
                            new ReplicaUpdatePolicy.ExecuteResetCallback() {

                                @Override
                                public void finished(AuthoritativeReplicaState authState) {
                                    eventSetAuthState(state.getFileId(), authState, localReplicaState, null);
                                }

                                @Override
                                public void failed(ErrorResponse error) {
                                    eventSetAuthState(state.getFileId(), null, null, error);
                                }
                            });
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processForceReset(StageRequest method) {
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];

            ReplicatedFileState state = getState(credentials, loc, true, false);
            if (!state.isForceReset()) {
                state.setForceReset(true);
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void replicatedWrite(FileCredentials credentials, XLocations xloc, long objNo, long objVersion,
            InternalObjectData data, ReusableBuffer createdViewBuffer, FileOperationCallback callback,
            OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_REPLICATED_WRITE, new Object[]{credentials, xloc, objNo, objVersion,
                data}, request, createdViewBuffer, callback);
    }

    public void replicateTruncate(FileCredentials credentials, XLocations xloc, long newFileSize,
            long newObjectVersion, FileOperationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_TRUNCATE,
                new Object[]{credentials, xloc, newFileSize, newObjectVersion}, request, null, callback);
    }

    public void fileClosed(String fileId) {
        this.enqueueOperation(STAGEOP_CLOSE, new Object[]{fileId}, null, null);
    }

    public void getStatus(StatusCallback callback) {
        this.enqueueOperation(STAGEOP_GETSTATUS, new Object[]{}, null, callback);
    }

    public static interface StatusCallback {
        public void statusComplete(Map<String, Map<String, String>> status);
    }

    @Override
    protected void processMethod(StageRequest method) {
        switch (method.getStageMethod()) {
            case STAGEOP_REPLICATED_WRITE: {
                externalRequestsInQueue.decrementAndGet();
                processReplicatedWrite(method);
                break;
            }
            case STAGEOP_TRUNCATE: {
                externalRequestsInQueue.decrementAndGet();
                processReplicatedTruncate(method);
                break;
            }
            case STAGEOP_CLOSE: processFileClosed(method); break;
            case STAGEOP_INTERNAL_AUTHSTATE: processSetAuthoritativeState(method); break;
            case STAGEOP_INTERNAL_OBJFETCHED: processObjectFetched(method); break;
            case STAGEOP_INTERNAL_STATEAVAIL: processReplicaStateAvailExecReset(method); break;
            case STAGEOP_INTERNAL_DELETE_COMPLETE: processDeleteObjectsComplete(method); break;
            case STAGEOP_INTERNAL_BACKUP_AUTHSTATE: processBackupAuthoritativeState(method); break;
            case STAGEOP_FORCE_RESET: processForceReset(method); break;
            case STAGEOP_GETSTATUS: processGetStatus(method); break;
            case STAGEOP_SETVIEW: processSetFleaseView(method); break;
            case STAGEOP_INVALIDATEVIEW: processInvalidateReplica(method); break;
            case STAGEOP_FETCHINVALIDATED: processFetchInvalidated(method); break;
            default : super.processMethod(method);
        }
    }

    private void processFileClosed(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            closeFileState(fileId, false);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void closeFileState(String fileId, boolean returnLease) {
        ReplicatedFileState state = files.remove(fileId);
        if (state != null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "closing file %s", fileId);
            }
            state.getPolicy().closeFile();
            if (state.getPolicy().requiresLease())
                closeFleaseCell(state.getPolicy().getCellId(), returnLease);
            cellToFileId.remove(state.getPolicy().getCellId());
        }
    }

    protected RedundantFileState getState(String fileId) {
        return files.get(fileId);
    }

    private ReplicatedFileState getState(FileCredentials credentials, XLocations loc, boolean forceReset,
            boolean invalidated) throws IOException {

        final String fileId = credentials.getXcap().getFileId();

        ReplicatedFileState state = files.get(fileId);
        if (state == null) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "open file: " + fileId);
            // "open" file
            state = new ReplicatedFileState(fileId, loc, master.getConfig().getUUID(), osdClient);
            files.put(fileId, state);
            state.setCredentials(credentials);
            state.setForceReset(forceReset);
            state.setInvalidated(invalidated);
            cellToFileId.put(state.getPolicy().getCellId(), fileId);
            assert (state.getState() == ReplicatedFileState.LocalState.INITIALIZING);

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

    private void processReplicatedWrite(StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long objNo = (Long) method.getArgs()[2];
            final Long objVersion = (Long) method.getArgs()[3];
            final InternalObjectData objData = (InternalObjectData) method.getArgs()[4];

            final String fileId = credentials.getXcap().getFileId();

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                BufferPool.free(objData.getData());
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_EIO, "file is not open!"));
                return;
            }
            state.setCredentials(credentials);

            state.getPolicy().executeWrite(credentials, objNo, objVersion, objData,
                    new ReplicaUpdatePolicy.ClientOperationCallback() {

                        @Override
                        public void finished() {
                            callback.success(objVersion);
                        }

                        @Override
                        public void failed(ErrorResponse error) {
                            callback.failed(error);
                        }
                    });

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    private void processReplicatedTruncate(StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long newFileSize = (Long) method.getArgs()[2];
            final Long newObjVersion = (Long) method.getArgs()[3];

            final String fileId = credentials.getXcap().getFileId();

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_EIO, "file is not open!"));
                return;
            }
            state.setCredentials(credentials);

            state.getPolicy().executeTruncate(credentials, newFileSize, newObjVersion,
                    new ReplicaUpdatePolicy.ClientOperationCallback() {

                        @Override
                        public void finished() {
                            callback.success(newObjVersion);
                        }

                        @Override
                        public void failed(ErrorResponse error) {
                            callback.failed(error);
                        }
                    });

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    public void processPrepareOp(StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];

            ReplicatedFileState state = getState(credentials, loc, false, false);
            processPrepareOp(state, method);

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    private void processGetStatus(StageRequest method) {
        final StatusCallback callback = (StatusCallback) method.getCallback();
        try {
            Map<String, Map<String, String>> status = new HashMap();

            for (String fileId : this.files.keySet()) {
                Map<String, String> fStatus = new HashMap();
                final ReplicatedFileState fState = files.get(fileId);
                final ASCIIString cellId = fState.getPolicy().getCellId();
                fStatus.put("policy", fState.getPolicy().getClass().getSimpleName());
                fStatus.put("peers (OSDs)", fState.getPolicy().getRemoteOSDUUIDs().toString());
                fStatus.put("pending requests", String.valueOf(fState.sizeOfPendingRequests()));
                fStatus.put("cellId", cellId.toString());
                String primary = "unknown";
                if ((fState.getLease() != null) && (!fState.getLease().isEmptyLease())) {
                    if (fState.getLease().isValid()) {
                        if (fState.isLocalIsPrimary()) {
                            primary = "primary";
                        } else {
                            primary = "backup ( primary is " + fState.getLease().getLeaseHolder() + ")";
                        }
                    } else {
                        primary = "outdated lease: " + fState.getLease().getLeaseHolder();
                    }
                }
                fStatus.put("role", primary);
                status.put(fileId, fStatus);
            }
            callback.statusComplete(status);
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.statusComplete(null);
        }
    }

    /**
     * Set the viewId associated with the fileId/cellId. This will close open cells.
     */
    public void setFleaseView(String fileId, ASCIIString cellId, XLocSetVersionState versionState) {
        enqueueOperation(STAGEOP_SETVIEW, new Object[] { fileId, cellId, versionState }, null, null);
    }

    private void processSetFleaseView(StageRequest method) {
        final Object[] args = method.getArgs();
        final String fileId = (String) args[0];
        final ASCIIString cellId = (ASCIIString) args[1];
        final XLocSetVersionState versionState = (XLocSetVersionState) args[2];

        int viewId;
        if (versionState.getInvalidated()) {
            viewId = FleaseMessage.VIEW_ID_INVALIDATED;
        } else {
            viewId = versionState.getVersion();
        }

        // Close ReplicatedFileState opened in a previous view to ensure no outdated UUIDList can exist.
        ReplicatedFileState state = files.get(fileId);
        if (state != null && state.getLocations().getVersion() < versionState.getVersion()) {
            closeFileState(fileId, true);
        }

        setFleaseViewId(cellId, viewId, new FleaseListener() {

            @Override
            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms,
                                       long masterEpochNumber) {
                // Ignore because #setFleaseView is never used with a callback.
            }

            @Override
            public void proposalFailed(ASCIIString cellId, Throwable cause) {
                // Ignore because proposalFailed will never be called in #setViewId
            }
        });
    }

    /**
     * Invalidate the replica and the corresponding flease view.<br>
     * If this Replica is the primary it will ensure the lease is given back.
     * 
     * @param fileId
     *            to close.
     * @param fileCreds
     *            used to call {@link #getState(FileCredentials, XLocations, boolean, boolean)}.
     * @param xLoc
     *            used to call {@link #getState(FileCredentials, XLocations, boolean, boolean)}.
     * @param callback
     *            to execute after the view has been invalidated.
     */
    public void invalidateReplica(String fileId, FileCredentials fileCreds, XLocations xLoc,
            InvalidateXLocSetCallback callback) {
        enqueueOperation(STAGEOP_INVALIDATEVIEW, new Object[] { fileId, fileCreds, xLoc }, null, callback);
    }

    private void processInvalidateReplica(StageRequest method) {
        final Object[] args = method.getArgs();
        final String fileId = (String) args[0];
        final FileCredentials fileCreds = (FileCredentials) args[1];
        final XLocations xLoc = (XLocations) args[2];
        final InvalidateXLocSetCallback callback = (InvalidateXLocSetCallback) method.getCallback();


        // Set the fileState to invalidated (or open the file invalidated).
        ReplicatedFileState state;
        try {
            state = getState(fileCreds, xLoc, true, true);
            state.setInvalidated(true);
            assert (state.isInvalidated());

        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            callback.invalidateComplete(LeaseState.NONE, ErrorUtils.getInternalServerError(ex));
            return;
        }

        // Check the replicas lease state.
        final LeaseState leaseState;
        if (state.isCellOpen()) {
            if (state.isLocalIsPrimary()) {
                leaseState = LeaseState.PRIMARY;
            } else {
                leaseState = LeaseState.BACKUP;
            }
        } else {
            leaseState = LeaseState.IDLE;
        }

        // Close the flease cell and return the lease if possible.
        closeFleaseCell(state.getPolicy().getCellId(), true);
        cellToFileId.remove(state.getPolicy().getCellId());

        // Clear pending requests. This ensures the LocalState won't change from now on.
        if (state.hasPendingRequests()) {
            ErrorResponse er = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    "File got invalidated!");
            state.clearPendingRequests(er);
        }

        // Transfer the view to flease.
        setFleaseViewId(state.getPolicy().getCellId(), FleaseMessage.VIEW_ID_INVALIDATED, new FleaseListener() {

            @Override
            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms,
                                       long masterEpochNumber) {
                callback.invalidateComplete(leaseState, null);
            }

            @Override
            public void proposalFailed(ASCIIString cellId, Throwable cause) {
                callback.invalidateComplete(leaseState, ErrorUtils.getInternalServerError(cause));
            }
        });

    }

    public void fetchInvalidated(String fileId, AuthoritativeReplicaState authState, ReplicaStatus localState,
            FileCredentials credentials, XLocations xloc, FileOperationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_FETCHINVALIDATED,
                new Object[] { fileId, authState, localState, credentials, xloc }, request, null, callback);
    }

    private void processFetchInvalidated(StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();
        try {
            final String fileId = (String) method.getArgs()[0];
            final AuthoritativeReplicaState authState = (AuthoritativeReplicaState) method.getArgs()[1];
            final ReplicaStatus localState = (ReplicaStatus) method.getArgs()[2];
            final FileCredentials credentials = (FileCredentials) method.getArgs()[3];
            final XLocations loc = (XLocations) method.getArgs()[4];

            // Set the fileState to invalidated (or open the file invalidated).
            ReplicatedFileState state = getState(credentials, loc, true, true);
            state.setInvalidated(true);

            assert (state.isInvalidated());

            // There should exist no pending requests, because they were cleaned when the replica got invalidated and
            // subsequent requests are denied.
            if (state.hasPendingRequests()) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "(R:%s) pending requests were queued while the replica for %s has been invalidated.", localID,
                        fileId);
            }

            switch (state.getState()) {
                case RESET:
                    // Wait until this reset is done. Since fileState.isInvalidated() is true this will result in an OPEN
                    // state.
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "(R:%s) enqueued fetch invalidated reset for file %s", localID, fileId);
                    state.addPendingRequest(method);
                    break;

                case INITIALIZING:
                    // Wait until the initializing is done. Since fileState.isInvalidated() this will result in an OPEN
                    // state.
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "(R:%s) enqueued fetch invalidated reset for file %s", localID, fileId);
                    state.addPendingRequest(method);
                    break;

                case OPEN:
                case WAITING_FOR_LEASE:
                case BACKUP:
                case PRIMARY:
                    // At this point it is ensured, that no other Request is queued. Therefore the AuthState can be set
                    // regardless of the state.
                    state.setInvalidatedReset(true);
                    state.addPendingRequest(method);
                    executeSetAuthState(localState, authState, state, fileId);
                    break;

                case INVALIDATED:
                    // The AuthState has been set and the Replica is up to date.

                    // Close the file by clearing the state.
                    closeFileState(fileId, true);

                    // Finish the request.
                    callback.success(0);
                    break;
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

}
