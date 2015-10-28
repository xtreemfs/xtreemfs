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
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.osd.stages.FleaseMasterEpochStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.RedundantFileState.LocalState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jan Fajerski
 * TODO add access to fstage and doOpen and doWaitingForLease
 */
public abstract class RedundancyStage extends Stage implements FleaseMessageSenderInterface{

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
    public static final int STAGEOP_FETCHINVALIDATED          = 23;

    public final RPCNIOSocketClient client;
    public final OSDServiceClient osdClient;
    private final HashMap<String, RedundantFileState> files;
    private final RPCNIOSocketClient               fleaseClient;
    private final OSDServiceClient                 fleaseOsdClient;
    private final FleaseStage fstage;
    public final ASCIIString                      localID;
    private final OSDRequestDispatcher             master;
    private final FleaseMasterEpochStage masterEpochStage;
    public final Map<ASCIIString, String>         cellToFileId;

    public RedundancyStage(String name, OSDRequestDispatcher master, SSLOptions sslOpts, int queueCapacity) throws IOException {
        super(name, queueCapacity);

        files = new HashMap<String, RedundantFileState>();
        cellToFileId = new HashMap<ASCIIString, String>();
        this.master = master;
        localID = new ASCIIString(master.getConfig().getUUID().toString());
        masterEpochStage = new FleaseMasterEpochStage(master.getStorageStage().getStorageLayout(),
                queueCapacity);

        FleaseConfig fcfg = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(), master.getConfig()
                .getFleaseDmaxMS(), master.getConfig().getFleaseMsgToMS(), null, localID.toString(), master.getConfig()
                .getFleaseRetries());

        client = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, name);
        osdClient = new OSDServiceClient(client, null);
        fleaseClient = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, name + "(flease)");
        fleaseOsdClient = new OSDServiceClient(fleaseClient, null);
        fstage = new FleaseStage(fcfg, master.getConfig().getObjDir() + "/", this, false,
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
        fstage.setLifeCycleListener(master);
    }

    @Override
    public void start() {
        masterEpochStage.start();
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
        masterEpochStage.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        masterEpochStage.waitForStartup();
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
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
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

                    fstage.openCell(file.getPolicy().getCellId(), osdAddresses, true, viewId);
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
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
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
        //try {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(), file.getState(),
                    LocalState.BACKUP);
        }
        file.setPrimaryReset(false);
        file.setState(LocalState.BACKUP);
        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
        /*} catch (IOException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }*/
    }

    private void doInvalidated(final RedundantFileState file) {
        assert (file.isInvalidated());


        if (file.isInvalidatedReset()) {
            // The AuthState has been set and the file is up to date.
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
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
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "file %s is already in RESET",
                        file.getFileId());
            return;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "(R:%s) replica state changed for %s from %s to %s", localID, file.getFileId(), file.getState(),
                    LocalState.RESET);
        }
        file.setState(LocalState.RESET);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "(R:%s) replica RESET started: %s (update objVer=%d)", localID, file.getFileId(), updateObjVer);
        }

        OSDOperation op = master.getInternalEvent(EventRWRStatus.class);
        op.startInternalEvent(new Object[]{file.getFileId(), file.getStripingPolicy()});

    }

    public String getPrimary(final String fileId) {
        String primary = null;

        final RedundantFileState fState = files.get(fileId);

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
                RedundantFileState state = files.get(fileId);
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
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
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

    public void processFleaseMessage(StageRequest method) {
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

    public void processMaxObjAvail(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Long maxObjVersion = (Long) method.getArgs()[1];
            final RPC.RPCHeader.ErrorResponse error = (RPC.RPCHeader.ErrorResponse) method.getArgs()[2];

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "(R:%s) max obj avail for file: "
                        + fileId + " max=" + maxObjVersion, localID);

            RedundantFileState state = files.get(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "received maxObjAvail event for unknown file: %s", fileId);
                return;
            }

            if (state.getState() == RedundantFileState.LocalState.INITIALIZING) {
                state.getPolicy().setLocalObjectVersion(maxObjVersion);
                doOpen(state);
            } else {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "LocalState is %s instead of INITIALIZING, maxObjectVersion=%d", state.getState().name(),
                        maxObjVersion);
                return;
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void closeFleaseCell(ASCIIString cellId, boolean returnedLease) {
        fstage.closeCell(cellId, returnedLease);
    }

    public void setFleaseViewId(ASCIIString cellId, int viewId, FleaseListener listener) {
        fstage.setViewId(cellId, viewId, listener);
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
        Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                "(R:%s) replica for file %s failed (in method: %s): %s", localID, file.getFileId(), methodName,
                ErrorUtils.formatError(ex));
        file.setPrimaryReset(false);
        file.setState(LocalState.OPEN);
        file.setCellOpen(false);
        closeFleaseCell(file.getPolicy().getCellId(), false);
        file.clearPendingRequests(ex);
    }

}
