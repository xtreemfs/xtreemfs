/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.EventRWRStatus;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;
import org.xtreemfs.osd.stages.PreprocStage.InvalidateXLocSetCallback;
import org.xtreemfs.osd.stages.Stage;
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
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * 
 * @author bjko
 */
public class RWReplicationStage extends Stage implements FleaseMessageSenderInterface {

    public static final int STAGEOP_REPLICATED_WRITE          = 1;
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
    public static final int STAGEOP_INVALIDATED_RESET         = 23;
    public static final int STAGEOP_GET_REPLICATED_FILE_STATE  = 24;

    public  static enum Operation {
        READ,
        WRITE,
        TRUNCATE,
        INTERNAL_UPDATE,
        INTERNAL_TRUNCATE
    };

    private final RPCNIOSocketClient               client;

    private final OSDServiceClient                 osdClient;

    private final Map<String, ReplicatedFileState> files;

    private final Map<ASCIIString, String>         cellToFileId;

    private final OSDRequestDispatcher             master;

    private final FleaseStage                      fstage;

    private final RPCNIOSocketClient               fleaseClient;

    private final OSDServiceClient                 fleaseOsdClient;

    private final ASCIIString                      localID;

    private int                                    numObjsInFlight;

    private static final int                       MAX_OBJS_IN_FLIGHT         = 10;

    private static final int                       MAX_PENDING_PER_FILE       = 10;

    private static final int                       MAX_EXTERNAL_REQUESTS_IN_Q = 250;

    private final Queue<ReplicatedFileState>       filesInReset;

    private final FleaseMasterEpochThread          masterEpochThread;

    private final AtomicInteger                    externalRequestsInQueue;

    public RWReplicationStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength)
            throws IOException {
        super("RWReplSt", maxRequestsQueueLength);
        this.master = master;
        client = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, "RWReplicationStage");
        fleaseClient = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, "RWReplicationStage (flease)");
        osdClient = new OSDServiceClient(client, null);
        fleaseOsdClient = new OSDServiceClient(fleaseClient, null);
        files = new HashMap<String, ReplicatedFileState>();
        cellToFileId = new HashMap<ASCIIString, String>();
        numObjsInFlight = 0;
        filesInReset = new LinkedList<ReplicatedFileState>();
        externalRequestsInQueue = new AtomicInteger(0);

        localID = new ASCIIString(master.getConfig().getUUID().toString());

        masterEpochThread = new FleaseMasterEpochThread(master.getStorageStage().getStorageLayout(),
                maxRequestsQueueLength);

        FleaseConfig fcfg = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(), master.getConfig()
                .getFleaseDmaxMS(), master.getConfig().getFleaseMsgToMS(), null, localID.toString(), master.getConfig()
                .getFleaseRetries());

        fstage = new FleaseStage(fcfg, master.getConfig().getObjDir() + "/", this, false,
                new FleaseViewChangeListenerInterface() {

                    @Override
                    public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal) {
                        eventViewIdChanged(cellId, viewId, onProposal);
                    }
                }, new FleaseStatusListener() {

                    @Override
                    public void statusChanged(ASCIIString cellId, Flease lease) {
                        // FIXME: change state
                        eventLeaseStateChanged(cellId, lease, null);
                    }

                    @Override
                    public void leaseFailed(ASCIIString cellID, FleaseException error) {
                        // change state
                        // flush pending requests
                        eventLeaseStateChanged(cellID, null, error);
                    }
                }, masterEpochThread);
        fstage.setLifeCycleListener(master);
    }

    @Override
    public void start() {
        masterEpochThread.start();
        client.start();
        fleaseClient.start();
        fstage.start();
        super.start();
    }

    @Override
    public void shutdown() {
        client.shutdown();
        fleaseClient.shutdown();
        fstage.shutdown();
        masterEpochThread.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        masterEpochThread.waitForStartup();
        client.waitForStartup();
        fleaseClient.waitForStartup();
        fstage.waitForStartup();
        super.waitForStartup();
    }

    @Override
    public void waitForShutdown() throws Exception {
        client.waitForShutdown();
        fleaseClient.waitForShutdown();
        fstage.waitForShutdown();
        masterEpochThread.waitForShutdown();
        super.waitForShutdown();
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

    void eventLeaseStateChanged(ASCIIString cellId, Flease lease, FleaseException error) {
        this.enqueueOperation(STAGEOP_LEASE_STATE_CHANGED, new Object[] { cellId, lease, error }, null, null);
    }

    void eventMaxObjAvail(String fileId, long maxObjVer, long fileSize, long truncateEpoch, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_MAXOBJ_AVAIL, new Object[] { fileId, maxObjVer, error }, null, null);
    }

    public void eventBackupReplicaReset(String fileId, AuthoritativeReplicaState authState, ReplicaStatus localState,
            FileCredentials credentials, XLocations xloc) {
        this.enqueueOperation(STAGEOP_INTERNAL_BACKUP_AUTHSTATE, new Object[] { fileId, authState, localState,
                credentials, xloc }, null, null);
    }

    void eventViewIdChanged(ASCIIString cellId, int viewId, boolean onProposal) {
        if (onProposal) {
            // Newer views encountered on lease proposals are ignored, because they could revalidate a removed Replica,
            // that had been primary trying to renew its lease.
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "New view (%d) encountered on lease proposal for %s is ignored.", viewId, cellId);
        } else {
            master.getPreprocStage().updateXLocSetFromFlease(cellId, viewId);
        }
    }

    private void executeSetAuthState(final ReplicaStatus localState, final AuthoritativeReplicaState authState,
            ReplicatedFileState state, final String fileId) {
        // Calculate what we need to do locally based on the local state.
        boolean resetRequired = localState.getTruncateEpoch() < authState.getTruncateEpoch();
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
            master.getStorageStage().deleteObjects(fileId, state.getsPolicy(), authState.getTruncateEpoch(),
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
            doResetComplete(state);
        }
    }

    private void processLeaseStateChanged(StageRequest method) {
        try {
            final ASCIIString cellId = (ASCIIString) method.getArgs()[0];
            final Flease lease = (Flease) method.getArgs()[1];
            final FleaseException error = (FleaseException) method.getArgs()[2];

            if (error == null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "(R:%s) lease change event: %s, %s", localID, cellId, lease);
                }
            } else {
                // Logging.logMessage(Logging.LEVEL_WARN, Category.replication,
                // this,"(R:%s) lease error in cell %s: %s cell debug: %s",localID, cellId, error,
                // error.getFleaseCellDebugString());
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this, "(R:%s) lease error in cell %s: %s",
                        localID, cellId, error);
            }

            final String fileId = cellToFileId.get(cellId);
            if (fileId != null) {
                ReplicatedFileState state = files.get(fileId);
                assert (state != null);

                // Ignore any leaseStateChange if the replica is invalidated.
                if (state.isInvalidated()) {
                    doInvalidated(state);
                    return;
                }

                boolean leaseOk = false;
                if (error == null) {
                    boolean localIsPrimary = (lease.getLeaseHolder() != null)
                            && (lease.getLeaseHolder().equals(localID));
                    ReplicaState oldState = state.getState();
                    state.setLocalIsPrimary(localIsPrimary);
                    state.setLease(lease);

                    // Error handling for timeouts on the primary.
                    if (oldState == ReplicaState.PRIMARY
                            && lease.getLeaseHolder() == null
                            && lease.getLeaseTimeout_ms() == 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                                "(R:%s) was primary, lease error in cell %s, restarting replication: %s", localID,
                                cellId, lease, error);
                        failed(state,
                                ErrorUtils.getInternalServerError(new IOException(fileId
                                        + ": lease timed out, renew failed")), "processLeaseStateChanged");
                    } else {
                        if ((oldState == ReplicaState.BACKUP) 
                                || (oldState == ReplicaState.PRIMARY)
                                || (oldState == ReplicaState.WAITING_FOR_LEASE)) {
                            if (localIsPrimary) {
                                // notify onPrimary
                                if (oldState != ReplicaState.PRIMARY) {
                                    state.setMasterEpoch(lease.getMasterEpochNumber());
                                    doPrimary(state);
                                }
                            } else {
                                if (oldState != ReplicaState.BACKUP) {
                                    state.setMasterEpoch(FleaseMessage.IGNORE_MASTER_EPOCH);
                                    doBackup(state);
                                }
                            }
                        }
                    }
                } else {
                    failed(state, ErrorUtils.getInternalServerError(error), "processLeaseStateChanged (error != null)");
                }
            }

        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,
                    "Exception was thrown and caught while processing the change of the lease state."
                            + " This is an error in the code. Please report it! Caught exception: ");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
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
                state.setState(ReplicaState.RESET);
                executeSetAuthState(localState, authState, state, fileId);
                break;
            }
            case RESET: {
                // Ignore.
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                        "(R:%s) auth state ignored, already in reset for file %s", localID, fileId);
                break;
            }
            default: {
                Logging.logMessage(Logging.LEVEL_INFO, Category.replication, this,
                        "(R:%s) auth state ignored, because file %s is in state %s", localID, fileId, state.getState());
                break;
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
            } else {
                Logging.logMessage(Logging.LEVEL_WARN, this, "file state not found after deleting objects");
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void fetchObjects() {

        while (numObjsInFlight < MAX_OBJS_IN_FLIGHT) {

            ReplicatedFileState fileInReset = filesInReset.poll();
            if (fileInReset == null)
                break;

            // Don't fetch objects for closed files.
            // This should not happen while a RESET is in progress, since #processObjectFetched calls ping.
            // If a file is deleted, the filestate will be cleared, too. But in that case it is reasonable to
            // abort the RESET.
            ReplicatedFileState file = files.get(fileInReset.getFileId());
            if (file == null || file != fileInReset) {
                continue;
            }

            // Remove an object from the queue and process it
            if (!file.getObjectsToFetch().isEmpty()) {
                ObjectVersionMapping o = file.getObjectsToFetch().remove(0);
                file.incrementNumObjectsPending();
                numObjsInFlight++;
                fetchObject(file, o);
            }

            // If there are still missing objects, return the file to the reset queue
            if (!file.getObjectsToFetch().isEmpty()) {
                filesInReset.add(file);
            }
                
            // If every missing object is fetches and no object is pending processing, the reset is complete
            if (file.getObjectsToFetch().isEmpty() && file.getNumObjectsPending() == 0) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "(R:%s) RESET complete for file %s",
                        localID, file.getFileId());
                doResetComplete(file);
            }
        }
    }

    private void fetchObject(final ReplicatedFileState state, final ObjectVersionMapping record) {
        final String fileId = state.getFileId();
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
                        eventObjectFetched(
                                fileId,
                                           record,
                                           null,
                                           ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_NONE, ex.toString(), ex));
                    } finally {
                        r.freeBuffers();
                    }
                }
            });
        } catch (IOException ex) {
            eventObjectFetched(fileId, record, null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }

    }

    private void processObjectFetched(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ObjectVersionMapping record = (ObjectVersionMapping) method.getArgs()[1];
            final InternalObjectData data = (InternalObjectData) method.getArgs()[2];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[3];

            numObjsInFlight--;

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {
                if (error != null) {
                    fetchObjects();

                    failed(state, error, "processObjectFetched");
                } else if (data.getData() == null) {
                    // data is null if object was deleted meanwhile.
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
                            state.getsPolicy(), 0, data.getData(), CowPolicy.PolicyNoCow, null, false,
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

                    state.decrementNumObjectsPending();
                    state.getPolicy().objectFetched(record.getObjectVersion());
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "(R:%s) fetched object for replica, file %s, remaining %d", localID, fileId,
                                state.getNumObjectsPending());
                    fetchObjects();
                    // If no more objects in flight are pending and the queue is empty, the reset is complete.
                    if (state.getNumObjectsPending() == 0 && state.getObjectsToFetch().size() == 0) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "(R:%s) RESET complete for file %s", localID, fileId);
                        doResetComplete(state);
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void doReset(final ReplicatedFileState file, long updateObjVer) {

        if (file.getState() == ReplicaState.RESET) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "file %s is already in RESET",
                        file.getFileId());
            return;
        }
        file.setState(ReplicaState.RESET);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "(R:%s) replica RESET started: %s (update objVer=%d)", localID, file.getFileId(), updateObjVer);
        }

        OSDOperation op = master.getInternalEvent(EventRWRStatus.class);
        op.startInternalEvent(new Object[] { file.getFileId(), file.getsPolicy() });

    }

    private void doResetComplete(final ReplicatedFileState file) {
        file.setState(ReplicaState.RESET_COMPLETE);

        if (file.isInvalidated()) {
            doInvalidated(file);
        } else {
            doOpen(file);
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

    private void doWaitingForLease(final ReplicatedFileState file) {
        assert (!file.isInvalidated());
            
        if (file.getPolicy().requiresLease()) {
            if (file.isCellOpen()) {
                if (file.isLocalIsPrimary()) {
                    doPrimary(file);
                } else {
                    doBackup(file);
                }
            } else {
                file.setCellOpen(true);
                try {
                    file.setState(ReplicaState.WAITING_FOR_LEASE);
                    List<InetSocketAddress> osdAddresses = new ArrayList<InetSocketAddress>();
                    for (ServiceUUID osd : file.getPolicy().getRemoteOSDUUIDs()) {
                        osdAddresses.add(osd.getAddress());
                    }

                    // it is save to use the version from the XLoc, because outdated or invalidated requests
                    // will be filtered in the preprocStage if the Operation requires a valid view
                    int viewId = file.getLocations().getVersion();

                    fstage.openCell(file.getPolicy().getCellId(), osdAddresses, true, viewId);
                    // wait for lease...
                } catch (UnknownUUIDException ex) {
                    failed(file,
                            ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex),
                            "doWaitingForLease");
                }
            }

        } else {
            // become primary immediately
            doPrimary(file);
        }
    }

    private void doOpen(final ReplicatedFileState file) {
        assert (!file.isInvalidated());
        file.setState(ReplicaState.OPEN);

        if (file.hasPendingRequests()) {
            doWaitingForLease(file);
        }
    }

    private void doPrimary(final ReplicatedFileState file) {
        assert (!file.isInvalidated());
        assert (file.isLocalIsPrimary());
        try {
            if (file.getPolicy().onPrimary((int) file.getMasterEpoch()) && !file.isPrimaryReset()) {
                file.setPrimaryReset(true);
                doReset(file, ReplicaUpdatePolicy.UNLIMITED_RESET);
            } else {
                file.setPrimaryReset(false);
                file.setState(ReplicaState.PRIMARY);
                while (file.hasPendingRequests()) {
                    enqueuePrioritized(file.removePendingRequest());
                }
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex),
                    "doPrimary");
        }
    }

    private void doBackup(final ReplicatedFileState file) {
        assert (!file.isInvalidated());
        assert (!file.isLocalIsPrimary());
        //try {
        file.setPrimaryReset(false);
        file.setState(ReplicaState.BACKUP);
        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
        /*} catch (IOException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }*/
    }

    private void doInvalidated(final ReplicatedFileState file) {
        file.setState(ReplicaState.INVALIDATED);

        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
    }

    private void failed(ReplicatedFileState file, ErrorResponse ex, String methodName) {
        Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                "(R:%s) replica for file %s failed (in method: %s): %s", localID, file.getFileId(), methodName,
                ErrorUtils.formatError(ex));
        file.setPrimaryReset(false);
        file.setState(ReplicaState.OPEN);
        file.setInvalidatedReset(false);
        file.setCellOpen(false);
        fstage.closeCell(file.getPolicy().getCellId(), false);
        file.clearPendingRequests(ex);
    }

    private void enqueuePrioritized(StageRequest rq) {
        while (!q.offer(rq)) {
            StageRequest otherRq = q.poll();
            otherRq.sendInternalServerError(new IllegalStateException(
                    "internal queue overflow, cannot enqueue operation for processing."));
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Dropping request from rwre queue due to overload");
        }
    }

    public static interface RWReplicationFailableCallback {
        public void failed(ErrorResponse ex);
    }
    
    public static interface RWReplicationCallback extends RWReplicationFailableCallback {
        public void success(long newObjectVersion);
        public void redirect(String redirectTo);
    }

    /*public void openFile(FileCredentials credentials, XLocations locations, boolean forceReset,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_OPEN, new Object[]{credentials,locations,forceReset}, request, callback);
    }*/

    protected void enqueueExternalOperation(int stageOp, Object[] arguments, OSDRequest request,
            ReusableBuffer createdViewBuffer, Object callback) {
        if (externalRequestsInQueue.get() >= MAX_EXTERNAL_REQUESTS_IN_Q) {
            Logging.logMessage(Logging.LEVEL_WARN, this,
                    "RW replication stage is overloaded, request %d for %s dropped", request.getRequestId(),
                    request.getFileId());
            request.sendInternalServerError(new IllegalStateException(
                    "RW replication stage is overloaded, request dropped"));

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
            Operation op, RWReplicationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_PREPAREOP, new Object[] { credentials, xloc, objNo, objVersion, op },
                request, null, callback);
    }

    public void replicatedWrite(FileCredentials credentials, XLocations xloc, long objNo, long objVersion,
            InternalObjectData data, ReusableBuffer createdViewBuffer, RWReplicationCallback callback,
            OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_REPLICATED_WRITE, new Object[] { credentials, xloc, objNo, objVersion,
                data }, request, createdViewBuffer, callback);
    }

    public void replicateTruncate(FileCredentials credentials, XLocations xloc, long newFileSize,
            long newObjectVersion, RWReplicationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_TRUNCATE,
                new Object[] { credentials, xloc, newFileSize, newObjectVersion }, request, null, callback);
    }

    public void fileClosed(String fileId) {
        this.enqueueOperation(STAGEOP_CLOSE, new Object[] { fileId }, null, null);
    }

    public void receiveFleaseMessage(ReusableBuffer message, InetSocketAddress sender) {
        // this.enqueueOperation(STAGEOP_PROCESS_FLEASE_MSG, new Object[]{message,sender}, null, null);
        try {
            FleaseMessage msg = new FleaseMessage(message);
            BufferPool.free(message);
            msg.setSender(sender);
            fstage.receiveMessage(msg);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void getStatus(StatusCallback callback) {
        this.enqueueOperation(STAGEOP_GETSTATUS, new Object[] {}, null, callback);
    }

    public static interface StatusCallback extends RWReplicationFailableCallback {
        public void statusComplete(Map<String, Map<String, String>> status);
    }

    @Override
    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
        ReusableBuffer data = BufferPool.allocate(message.getSize());
        message.serialize(data);
        data.flip();
        try {
            RPCResponse r = fleaseOsdClient.xtreemfs_rwr_flease_msg(recipient, RPCAuthentication.authNone,
                    RPCAuthentication.userService, master.getHostName(), master.getConfig().getPort(), data);
            r.registerListener(new RPCResponseAvailableListener() {

                @Override
                public void responseAvailable(RPCResponse r) {
                    r.freeBuffers();
                }
            });
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
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
        case STAGEOP_PROCESS_FLEASE_MSG: processFleaseMessage(method); break;
        case STAGEOP_PREPAREOP: {
            externalRequestsInQueue.decrementAndGet();
            processPrepareOp(method);
            break;
        }
        case STAGEOP_INTERNAL_AUTHSTATE: processSetAuthoritativeState(method); break;
        case STAGEOP_LEASE_STATE_CHANGED: processLeaseStateChanged(method); break;
        case STAGEOP_INTERNAL_OBJFETCHED: processObjectFetched(method); break;
        case STAGEOP_INTERNAL_STATEAVAIL: processReplicaStateAvailExecReset(method); break;
        case STAGEOP_INTERNAL_DELETE_COMPLETE: processDeleteObjectsComplete(method); break;
        case STAGEOP_INTERNAL_MAXOBJ_AVAIL: processMaxObjAvail(method); break;
        case STAGEOP_INTERNAL_BACKUP_AUTHSTATE: processBackupAuthoritativeState(method); break;
        case STAGEOP_FORCE_RESET: processForceReset(method); break;
        case STAGEOP_GETSTATUS: processGetStatus(method); break;
        case STAGEOP_SETVIEW: processSetView(method); break;
        case STAGEOP_INVALIDATEVIEW: processInvalidateReplica(method); break;
        case STAGEOP_INVALIDATED_RESET: processInvalidatedReplicaReset(method); break;
        // TODO: externalRequestsInQueue.decrementAndGet(); ????
        case STAGEOP_GET_REPLICATED_FILE_STATE: processGetInvalidatedResetStatus(method); break; 

        default : throw new IllegalArgumentException("no such stageop");
        }
    }

    private void processFleaseMessage(StageRequest method) {
        try {
            final ReusableBuffer data = (ReusableBuffer) method.getArgs()[0];
            final InetSocketAddress sender = (InetSocketAddress) method.getArgs()[1];

            FleaseMessage msg = new FleaseMessage(data);
            BufferPool.free(data);
            msg.setSender(sender);
            fstage.receiveMessage(msg);

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processFileClosed(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        // Files are closed due to a timer in the openFileTable or if they are unlinked.
        // Since the openFileTable is pinged on most operations and an unlinked file is no longer available the fileState
        // can be closed.
        // TODO (jdillmann): Correct errno would be probably EBADF (9)
        ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                "file has been closed");
        closeFileState(fileId, false, error);
    }

    private void closeFileState(String fileId, boolean returnLease, ErrorResponse error) {
        ReplicatedFileState state = files.remove(fileId);
        if (state != null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "closing file %s", fileId);
            }

            if (error == null) {
                error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                        "file has been closed");
            }

            state.clearPendingRequests(error);
            state.getPolicy().closeFile();
            if (state.getPolicy().requiresLease())
                fstage.closeCell(state.getPolicy().getCellId(), returnLease);
            cellToFileId.remove(state.getPolicy().getCellId());
        }
    }

    private ReplicatedFileState getState(FileCredentials credentials, XLocations loc, boolean forceReset,
            boolean invalidated) throws IOException {

        final String fileId = credentials.getXcap().getFileId();

        ReplicatedFileState state = files.get(fileId);
        if (state == null) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "open file: " + fileId);
            // "open" file
            state = new ReplicatedFileState(fileId, loc, master.getConfig().getUUID(), fstage, osdClient);
            files.put(fileId, state);
            state.setCredentials(credentials);
            state.setForceReset(forceReset);
            state.setInvalidated(invalidated);
            cellToFileId.put(state.getPolicy().getCellId(), fileId);
            assert (state.getState() == ReplicaState.INITIALIZING);

            master.getStorageStage().internalGetMaxObjectNo(fileId, loc.getLocalReplica().getStripingPolicy(),
                    new InternalGetMaxObjectNoCallback() {

                        @Override
                        public void maxObjectNoCompleted(long maxObjNo, long fileSize, long truncateEpoch,
                                ErrorResponse error) {
                            eventMaxObjAvail(fileId, maxObjNo, fileSize, truncateEpoch, error);
                        }
                    });
        }
        return state;
    }

    private void processMaxObjAvail(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Long maxObjVersion = (Long) method.getArgs()[1];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[2];

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "(R:%s) max obj avail for file: "
                        + fileId + " max=" + maxObjVersion, localID);

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "received maxObjAvail event for unknow file: %s", fileId);
                return;
            }

            if (state.getState() == ReplicaState.INITIALIZING) {
                state.getPolicy().setLocalObjectVersion(maxObjVersion);
                if (state.isInvalidated()) {
                    doInvalidated(state);
                } else {
                    doOpen(state);
                }
            } else {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "ReplicaState is %s instead of INITIALIZING, maxObjectVersion=%d", state.getState().name(),
                        maxObjVersion);
                return;
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processReplicatedWrite(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
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
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
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

    private void processPrepareOp(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
            final Long objVersion = (Long) method.getArgs()[3];
            final Operation op = (Operation) method.getArgs()[4];

            final String fileId = credentials.getXcap().getFileId();

            ReplicatedFileState state = getState(credentials, loc, false, false);

            // Abort the request if the file has been invalidated.
            if (state.isInvalidated()) {
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_NONE, "file has been invalidated"));
                return;
            }

            if ((op == Operation.INTERNAL_UPDATE) || (op == Operation.INTERNAL_TRUNCATE)) {
                switch (state.getState()) {
                case WAITING_FOR_LEASE:
                case INITIALIZING:
                case RESET:
                case OPEN: {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "enqeue update for %s (state is %s)", fileId, state.getState());
                    }
                    if (state.sizeOfPendingRequests() > MAX_PENDING_PER_FILE) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                    "rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                                    state.sizeOfPendingRequests(), MAX_PENDING_PER_FILE, fileId);
                        }
                        callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                                POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
                        return;
                    } else {
                        state.addPendingRequest(method);
                    }

                    if (state.getState() == ReplicaState.OPEN) {
                        // immediately change to backup mode...no need to check the lease
                        doWaitingForLease(state);
                    }
                    return;
                }
                }

                if (!state.getPolicy().acceptRemoteUpdate(objVersion)) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                            "received outdated object version %d for file %s", objVersion, fileId);
                    callback.failed(ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                            "outdated object version for update rejected"));
                    return;
                }
                boolean needsReset = state.getPolicy().onRemoteUpdate(objVersion, state.getState());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "%s needs reset: %s", fileId,
                            needsReset);
                }
                if (needsReset) {
                    state.addPendingRequest(method);
                    doReset(state, objVersion);
                } else {
                    callback.success(0);
                }
            } else {
                state.setCredentials(credentials);

                switch (state.getState()) {
                case WAITING_FOR_LEASE:
                case INITIALIZING:
                case RESET: {
                    if (state.sizeOfPendingRequests() > MAX_PENDING_PER_FILE) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                    "rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                                    state.sizeOfPendingRequests(), MAX_PENDING_PER_FILE, fileId);
                        }
                        callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                                POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
                    } else {
                        state.addPendingRequest(method);
                    }
                    return;
                }

                case OPEN: {
                    if (state.sizeOfPendingRequests() > MAX_PENDING_PER_FILE) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                    "rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                                    state.sizeOfPendingRequests(), MAX_PENDING_PER_FILE, fileId);
                        }
                        callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                                POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
                        return;
                    } else {
                        state.addPendingRequest(method);
                    }
                    doWaitingForLease(state);
                    return;
                }
                }

                try {
                    long newVersion = state.getPolicy().onClientOperation(op, objVersion, state.getState(),
                            state.getLease());
                    callback.success(newVersion);
                } catch (RedirectToMasterException ex) {
                    callback.redirect(ex.getMasterUUID());
                } catch (RetryException ex) {
                    final ErrorResponse err = ErrorUtils.getInternalServerError(ex);
                    failed(state, err, "processPrepareOp");
                    if (state.getState() == ReplicaState.BACKUP || state.getState() == ReplicaState.PRIMARY) {
                        // Request is not in queue, we must notify callback.
                        callback.failed(err);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    private void processGetStatus(StageRequest method) {
        final StatusCallback callback = (StatusCallback) method.getCallback();
        try {
            Map<String, Map<String, String>> status = new HashMap();

            Map<ASCIIString, FleaseMessage> fleaseState = fstage.getLocalState();

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
     * 
     * @param fileId
     * @param cellId
     * @param versionState
     */
    public void setView(String fileId, ASCIIString cellId, XLocSetVersionState versionState) {
        enqueueOperation(STAGEOP_SETVIEW, new Object[] { fileId, cellId, versionState }, null, null);
    }

    private void processSetView(StageRequest method) {
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
        // This will also cancel any pending (invalidated) resets, but the reset will be restarted by the replication
        // policy.
        ReplicatedFileState state = files.get(fileId);
        if (state != null) {
            // Abort if the view to be set is older than the current one.
            if (state.getLocations().getVersion() > versionState.getVersion()) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "(R:%s) requested to set an older view than stored in the ReplicatedFileState for %s.",
                        localID, fileId);
                return;
            }

            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.INVALID_VIEW, POSIXErrno.POSIX_ERROR_NONE,
                    "file has been closed due to a new view");
            closeFileState(fileId, true, error);
        }

        fstage.setViewId(cellId, viewId, new FleaseListener() {

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
     * If this Replica is the primary it will try to give the lease back.
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
        final ReplicatedFileState state;
        try {
            state = getState(fileCreds, xLoc, true, true);
            state.setInvalidated(true);

        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            callback.invalidateComplete(LeaseState.NONE, ErrorUtils.getInternalServerError(ex));
            return;
        }

        // Clear pending requests. This ensures the ReplicaState won't change from now on.
        if (state.hasPendingRequests()) {
            ErrorResponse er = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    "File got invalidated!");
            state.clearPendingRequests(er);
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
        
        switch (state.getState()) {
        case INVALIDATED:
        // If file.isInvalidated is true, the following states will make a transition to INVALIDATED.
        case INITIALIZING:
        case WAITING_FOR_LEASE:
        case RESET:
        case RESET_COMPLETE:
            callback.invalidateComplete(leaseState, null);
            break;

        case OPEN:
        case BACKUP:
        case PRIMARY:
            state.setState(ReplicaState.INVALIDATED);

            // Transfer the view to flease.
            fstage.setViewId(state.getPolicy().getCellId(), FleaseMessage.VIEW_ID_INVALIDATED, new FleaseListener() {

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

            // Asynchronous close the cell
            fstage.closeCell(state.getPolicy().getCellId(), true);
            state.setCellOpen(false);
            break;
        }

    }

    public void invalidatedReplicaReset(String fileId, AuthoritativeReplicaState authState, ReplicaStatus localState,
            FileCredentials credentials, XLocations xloc, OSDRequest request) {
        this.enqueueOperation(STAGEOP_INVALIDATED_RESET,
                new Object[] { fileId, authState, localState, credentials, xloc }, request, null, null);
    }

    private void processInvalidatedReplicaReset(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final AuthoritativeReplicaState authState = (AuthoritativeReplicaState) method.getArgs()[1];
            final ReplicaStatus localState = (ReplicaStatus) method.getArgs()[2];
            final FileCredentials credentials = (FileCredentials) method.getArgs()[3];
            final XLocations loc = (XLocations) method.getArgs()[4];

            // Set the fileState to invalidated (or open the file invalidated).
            ReplicatedFileState state = getState(credentials, loc, true, true);
            state.setInvalidated(true);

            // There should exist no pending requests, because they were cleaned when the replica got invalidated and
            // subsequent requests are denied.
            if (state.hasPendingRequests()) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "(R:%s) pending requests were queued while the replica for %s has been invalidated.", localID,
                        fileId);
            }

            switch (state.getState()) {
            case RESET:
                throw new XtreemFSException(
                        String.format("(R:%s) auth state ignored, already in reset for file %s", localID, fileId));

            case INITIALIZING:
                // Wait until the initializing is done. Since fileState.isInvalidated() this will result in an
                // INVALIDATED state.
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "(R:%s) enqueued fetch invalidated reset for file %s", localID, fileId);
                state.addPendingRequest(method);
                break;

            case INVALIDATED:
                // At this point it is ensured, that no other Request is queued. Therefore the AuthState can be set
                // regardless of the state.
                // TODO(jdillmann): It would be possible to store and compare maxObjNo and truncateEpoch to identify a AuthState.
                if (!state.isInvalidatedReset()) {
                    // Execute the RESET
                    state.setInvalidatedReset(true);
                    state.setForceReset(false);
                    state.setPrimaryReset(false);
                    state.addPendingRequest(method);
                    executeSetAuthState(localState, authState, state, fileId);
                } else {
                    // If we end up in INVALIDATED with the invalidatedReset flag set, the reset is finished.
                    state.setInvalidatedReset(false);
                }
                break;

            case OPEN:
            case BACKUP:
            case PRIMARY:
            case WAITING_FOR_LEASE:
            case RESET_COMPLETE:
                throw new XtreemFSException(
                        String.format("(R:%s) Replica for %s ended up in bad state %s while being invalidated.", 
                                      localID, fileId, state.getState()));
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_WARN, this, ex);
        }
    }

     
    /**
     * Returns the {@link ReplicatedFileStateSimple} to the callback or null if the file is not opened in this instance.
     * 
     * @param fileId
     * @param callback
     * @param request
     */
    public void getReplicatedFileState(String fileId, GetReplicatedFileStateCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_GET_REPLICATED_FILE_STATE, new Object[] { fileId }, request, null, callback);
    }

    public interface GetReplicatedFileStateCallback extends RWReplicationFailableCallback {
        public void getReplicatedFileStateComplete(ReplicatedFileStateSimple state);
    }

    private void processGetInvalidatedResetStatus(StageRequest method) {
        final GetReplicatedFileStateCallback callback = (GetReplicatedFileStateCallback) method.getCallback();
        final String fileId = (String) method.getArgs()[0];
        
        ReplicatedFileState state = files.get(fileId);
        if (state != null) {
            callback.getReplicatedFileStateComplete(new ReplicatedFileStateSimple(state));
        } else {
            callback.getReplicatedFileStateComplete(null);
        }
    }
}