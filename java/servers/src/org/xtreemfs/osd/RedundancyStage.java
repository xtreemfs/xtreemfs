/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.*;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.operations.EventRWRStatus;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.rwre.RedirectToMasterException;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.osd.rwre.RetryException;
import org.xtreemfs.osd.stages.FleaseMasterEpochStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.RedundantFileState.LocalState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jan Fajerski
 */
public abstract class RedundancyStage extends Stage implements FleaseMessageSenderInterface{

    public static enum Operation {
        READ,
        WRITE,
        TRUNCATE,
        INTERNAL_UPDATE,
        INTERNAL_TRUNCATE
    };

    public static final int STAGEOP_REPLICATED_WRITE          = 1;
    public static final int STAGEOP_CLOSE                     = 2;
    public static final int STAGEOP_PROCESS_FLEASE_MSG        = 3;
    public static final int STAGEOP_PREPAREOP                 = 5;
    public static final int STAGEOP_TRUNCATE                  = 6;
    public static final int STAGEOP_GETSTATUS                 = 7;
    public static final int STAGEOP_EC_WRITE                  = 8;

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

    protected final Map<ASCIIString, String>    cellToFileId;
    protected final RPCNIOSocketClient          client;
    protected final AtomicInteger               externalRequestsInQueue;
    private final RPCNIOSocketClient            fleaseClient;
    private final OSDServiceClient              fleaseOsdClient;
    private final FleaseStage                   fleaseStage;
    protected final ASCIIString                 localID;
    protected final Category                    logCategory;
    private final OSDRequestDispatcher          master;
    private final FleaseMasterEpochStage        masterEpochStage;
    protected final OSDServiceClient            osdClient;
    private static final int                    MAX_EXTERNAL_REQUESTS_IN_Q = 250;
    private static final int                    MAX_PENDING_PER_FILE       = 10;

    public RedundancyStage(String name, OSDRequestDispatcher master, SSLOptions sslOpts, int queueCapacity, Category logCategory) throws IOException {
        super(name, queueCapacity);
        this.logCategory = logCategory;

        externalRequestsInQueue = new AtomicInteger(0);
        cellToFileId = new HashMap<ASCIIString, String>();
        this.master = master;
        localID = new ASCIIString(master.getConfig().getUUID().toString());
        masterEpochStage = new FleaseMasterEpochStage(master.getStorageStage().getStorageLayout(),
                queueCapacity);

        FleaseConfig fleaseConfig = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(), master.getConfig()
                .getFleaseDmaxMS(), master.getConfig().getFleaseMsgToMS(), null, localID.toString(), master.getConfig()
                .getFleaseRetries());

        client = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, name);
        osdClient = new OSDServiceClient(client, null);
        fleaseClient = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, name + "(flease)");
        fleaseOsdClient = new OSDServiceClient(fleaseClient, null);

        fleaseStage = new FleaseStage(fleaseConfig, master.getConfig().getObjDir() + "/", this, false,
                new FleaseViewChangeListenerInterface() {

                    @Override
                    public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
                        eventViewIdChanged(cellId, viewId);
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
        }, masterEpochStage);

        fleaseStage.setLifeCycleListener(master);
    }

    protected abstract RedundantFileState getState(String fileId);

    protected abstract RedundantFileState removeState(String fileId);

    @Override
    public void start() {
        masterEpochStage.start();
        client.start();
        fleaseClient.start();
        fleaseStage.start();
        super.start();
    }

    @Override
    public void shutdown() {
        client.shutdown();
        fleaseClient.shutdown();
        fleaseStage.shutdown();
        masterEpochStage.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        masterEpochStage.waitForStartup();
        client.waitForStartup();
        fleaseClient.waitForStartup();
        fleaseStage.waitForStartup();
        super.waitForStartup();
    }

    @Override
    public void waitForShutdown() throws Exception {
        client.waitForShutdown();
        fleaseClient.waitForShutdown();
        fleaseStage.waitForShutdown();
        masterEpochStage.waitForShutdown();
        super.waitForShutdown();
    }

    void eventLeaseStateChanged(ASCIIString cellId, Flease lease, FleaseException error) {
        this.enqueueOperation(STAGEOP_LEASE_STATE_CHANGED, new Object[]{cellId, lease, error}, null, null);
    }
    void eventViewIdChanged(ASCIIString cellId, int viewId) {
        master.getPreprocStage().updateXLocSetFromFlease(cellId, viewId);
    }

    public void doWaitingForLease(final RedundantFileState file) {
        // If the file is invalidated a XLocSetChange is in progress and we can assume, that no primary exists.
        if (file.isInvalidated()) {
            doInvalidated(file);
        } else if (file.getPolicy().requiresLease()) {
            if (file.isCellOpen()) {
                if (file.isLocalIsPrimary()) {
                    doPrimary(file);
                } else {
                    doBackup(file);
                }
            } else {
                file.setCellOpen(true);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                            "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(),
                            file.getState(), LocalState.WAITING_FOR_LEASE);
                }
                try {
                    file.setState(LocalState.WAITING_FOR_LEASE);
                    List<InetSocketAddress> osdAddresses = new ArrayList();
                    for (ServiceUUID osd : file.getPolicy().getRemoteOSDUUIDs()) {
                        osdAddresses.add(osd.getAddress());
                    }

                    // it is save to use the version from the XLoc, because outdated or invalidated requests
                    // will be filtered in the preprocStage if the Operation requires a valid view
                    int viewId = file.getLocations().getVersion();

                    fleaseStage.openCell(file.getPolicy().getCellId(), osdAddresses, true, viewId);
                    // wait for lease...
                } catch (UnknownUUIDException ex) {
                    failed(file,
                            ErrorUtils.getErrorResponse(RPC.ErrorType.ERRNO, RPC.POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex),
                            "doWaitingForLease");
                }
            }

        } else {
            // become primary immediately
            doPrimary(file);
        }
    }

    public void doOpen(final RedundantFileState file) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "(R:%s) replica state changed for %s from %s to %s", localID,
                    file.getFileId(), file.getState(), LocalState.OPEN);
        }
        file.setState(LocalState.OPEN);
        if (file.hasPendingRequests()) {
            doWaitingForLease(file);
        }
    }

    private void doPrimary(final RedundantFileState file) {
        assert (file.isLocalIsPrimary());
        try {
            if (file.getPolicy().onPrimary((int) file.getMasterEpoch()) && !file.isPrimaryReset()) {
                file.setPrimaryReset(true);
                doReset(file, ReplicaUpdatePolicy.UNLIMITED_RESET);
            } else {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                            "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(),
                            file.getState(), LocalState.PRIMARY);
                }
                file.setPrimaryReset(false);
                file.setState(LocalState.PRIMARY);
                while (file.hasPendingRequests()) {
                    enqueuePrioritized(file.removePendingRequest());
                }
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex),
                    "doPrimary");
        }
    }

    private void doBackup(final RedundantFileState file) {
        assert (!file.isLocalIsPrimary());

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                    "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(), file.getState(),
                    LocalState.BACKUP);
        }
        file.setPrimaryReset(false);
        file.setState(LocalState.BACKUP);
        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
    }

    private void doInvalidated(final RedundantFileState file) {
        assert (file.isInvalidated());


        if (file.isInvalidatedReset()) {
            // The AuthState has been set and the file is up to date.
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                        "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(),
                        file.getState(), LocalState.INVALIDATED);
            }

            file.setInvalidatedReset(false);
            file.setState(LocalState.INVALIDATED);
        }
        file.setPrimaryReset(false);
        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
    }

    public void doReset(final RedundantFileState file, long updateObjVer) {

        if (file.getState() == LocalState.RESET) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this, "file %s is already in RESET",
                        file.getFileId());
            return;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                    "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(), file.getState(),
                    LocalState.RESET);
        }
        file.setState(LocalState.RESET);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                    "(R:%s) replica RESET started: %s (update objVer=%d)", localID, file.getFileId(), updateObjVer);
        }

        OSDOperation op = master.getInternalEvent(EventRWRStatus.class);
        op.startInternalEvent(new Object[]{file.getFileId(), file.getStripingPolicy()});

    }

    public String getPrimary(final String fileId) {
        String primary = null;

        final RedundantFileState fState = getState(fileId);

        if ((fState != null) && (fState.getLease() != null) && (!fState.getLease().isEmptyLease())) {
            if (fState.getLease().isValid()) {
                primary = "" + fState.getLease().getLeaseHolder();
            }
        }
        return primary;
    }

    public void processLeaseStateChanged(StageRequest method) {
        try {
            final ASCIIString cellId = (ASCIIString) method.getArgs()[0];
            final Flease lease = (Flease) method.getArgs()[1];
            final FleaseException error = (FleaseException) method.getArgs()[2];

            if (error == null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
                            "(R:%s) lease change event: %s, %s", localID, cellId, lease);
                }
            } else {
                Logging.logMessage(Logging.LEVEL_WARN, logCategory, this, "(R:%s) lease error in cell %s: %s",
                        localID, cellId, error);
            }

            final String fileId = cellToFileId.get(cellId);
            if (fileId != null) {
                RedundantFileState state = getState(fileId);
                assert (state != null);

                // Ignore any leaseStateChange if the replica is invalidated
                if (state.isInvalidated()) {
                    return;
                }

                if (error == null) {
                    boolean localIsPrimary = (lease.getLeaseHolder() != null)
                            && (lease.getLeaseHolder().equals(localID));
                    LocalState oldState = state.getState();
                    state.setLocalIsPrimary(localIsPrimary);
                    state.setLease(lease);

                    // Error handling for timeouts on the primary.
                    if (oldState == LocalState.PRIMARY
                            && lease.getLeaseHolder() == null
                            && lease.getLeaseTimeout_ms() == 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, logCategory, this,
                                "(R:%s) was primary, lease error in cell %s, restarting replication: %s", localID,
                                cellId, lease, error);
                        failed(state,
                                ErrorUtils.getInternalServerError(new IOException(fileId
                                        + ": lease timed out, renew failed")), "processLeaseStateChanged");
                    } else {
                        if ((state.getState() == LocalState.BACKUP)
                                || (state.getState() == LocalState.PRIMARY)
                                || (state.getState() == LocalState.WAITING_FOR_LEASE)) {
                            if (localIsPrimary) {
                                // notify onPrimary
                                if (oldState != LocalState.PRIMARY) {
                                    state.setMasterEpoch(lease.getMasterEpochNumber());
                                    doPrimary(state);
                                }
                            } else {
                                if (oldState != LocalState.BACKUP) {
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

    public void receiveFleaseMessage(ReusableBuffer message, InetSocketAddress sender) {
        try {
            FleaseMessage msg = new FleaseMessage(message);
            BufferPool.free(message);
            msg.setSender(sender);
            fleaseStage.receiveMessage(msg);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void prepareOperation(FileCredentials credentials, XLocations xloc, long objNo, long objVersion,
                                 Operation op, FileOperationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_PREPAREOP, new Object[]{credentials, xloc, objNo, objVersion, op},
                request, null, callback);
    }

    public void processFleaseMessage(StageRequest method) {
        try {
            final ReusableBuffer data = (ReusableBuffer) method.getArgs()[0];
            final InetSocketAddress sender = (InetSocketAddress) method.getArgs()[1];

            FleaseMessage msg = new FleaseMessage(data);
            BufferPool.free(data);
            msg.setSender(sender);
            fleaseStage.receiveMessage(msg);

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void processMaxObjAvail(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Long maxObjVersion = (Long) method.getArgs()[1];

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this, "(R:%s) max obj avail for file: "
                        + fileId + " max=" + maxObjVersion, localID);

            RedundantFileState state = getState(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, logCategory, this,
                        "received maxObjAvail event for unknown file: %s", fileId);
                return;
            }

            if (state.getState() == RedundantFileState.LocalState.INITIALIZING) {
                state.getPolicy().setLocalObjectVersion(maxObjVersion);
                doOpen(state);
            } else {
                Logging.logMessage(Logging.LEVEL_ERROR, logCategory, this,
                        "LocalState is %s instead of INITIALIZING, maxObjectVersion=%d", state.getState().name(),
                        maxObjVersion);
                return;
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public abstract void processPrepareOp(StageRequest method);

    public void processPrepareOp(RedundantFileState state, StageRequest method) {
        final FileOperationCallback callback = (FileOperationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final String fileId = credentials.getXcap().getFileId();
            final Long objVersion = (Long) method.getArgs()[3];
            final Operation op = (Operation) method.getArgs()[4];

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
                            Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this,
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
                        if (state.getState() == LocalState.OPEN) {
                            // immediately change to backup mode...no need to check the lease
                            doWaitingForLease(state);
                        }
                        return;
                    }
                }

                // does this internal update have an acceptable version
                if (!state.getPolicy().acceptRemoteUpdate(objVersion)) {
                    Logging.logMessage(Logging.LEVEL_WARN, logCategory, this,
                            "received outdated object version %d for file %s", objVersion, fileId);
                    callback.failed(ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                            "outdated object version for update rejected"));
                    return;
                }

                // or does it need to be reset first
                boolean needsReset = state.getPolicy().onRemoteUpdate(objVersion, state.getState());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, logCategory, this, "%s needs reset: %s", fileId,
                            needsReset);
                }
                if (needsReset) {
                    state.addPendingRequest(method);
                    doReset(state, objVersion);
                } else {
                    callback.success(0);
                }
            } else {
                // not an internal update
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
                /* reset, open, initialising and waiting_for_lease make prepareOp add the operation to pendingRequest
                 * all other states get a new version
                 */

                try {
                    long newVersion = state.getPolicy().onClientOperation(op, objVersion, state.getState(),
                            state.getLease());
                    callback.success(newVersion);
                } catch (RedirectToMasterException ex) {
                    callback.redirect(ex.getMasterUUID());
                } catch (RetryException ex) {
                    final ErrorResponse err = ErrorUtils.getInternalServerError(ex);
                    failed(state, err, "processPrepareOp");
                    if (state.getState() == LocalState.BACKUP || state.getState() == LocalState.PRIMARY) {
                        // Request is not in queue, we must notify
                        // callback.
                        callback.failed(err);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    public void closeFleaseCell(ASCIIString cellId, boolean returnedLease) {
        fleaseStage.closeCell(cellId, returnedLease);
    }

    public void setFleaseViewId(ASCIIString cellId, int viewId, FleaseListener listener) {
        fleaseStage.setViewId(cellId, viewId, listener);
    }

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

    private void enqueuePrioritized(StageRequest rq) {
        while (!q.offer(rq)) {
            StageRequest otherRq = q.poll();
            otherRq.sendInternalServerError(new IllegalStateException(
                    "internal queue overflow, cannot enqueue operation for processing."));
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Dropping request from rwre queue due to overload");
        }
    }

    public void failed(RedundantFileState file, ErrorResponse ex, String methodName) {
        Logging.logMessage(Logging.LEVEL_WARN, logCategory, this,
                "(R:%s) replica for file %s failed (in method: %s): %s", localID, file.getFileId(), methodName,
                ErrorUtils.formatError(ex));
        file.setPrimaryReset(false);
        file.setState(LocalState.OPEN);
        file.setCellOpen(false);
        closeFleaseCell(file.getPolicy().getCellId(), false);
        file.clearPendingRequests(ex);
    }

    @Override
    protected void processMethod(StageRequest method) {
        switch (method.getStageMethod()) {
            case STAGEOP_PROCESS_FLEASE_MSG: processFleaseMessage(method); break;
            case STAGEOP_INTERNAL_MAXOBJ_AVAIL: processMaxObjAvail(method); break;
            case STAGEOP_LEASE_STATE_CHANGED: processLeaseStateChanged(method); break;
            case STAGEOP_PREPAREOP: {
                externalRequestsInQueue.decrementAndGet();
                processPrepareOp(method);
                break;
            }
            default : throw new IllegalArgumentException("no such stageop");
        }
    }
}
