/*
 * Copyright (c) 2016 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.IntervalVersionTree.Interval;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FleasePrefixHandler;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECFileState.FileState;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StorageStage.GetECVersionsCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public class ECMasterStage extends Stage {
    private static final int               MAX_EXTERNAL_REQUESTS_IN_Q = 250;
    private static final int               MAX_PENDING_PER_FILE       = 10;

    private static final String            FLEASE_PREFIX              = "/ec/";

    private final ServiceUUID              localUUID;
    private final ASCIIString              localID;

    private final OSDRequestDispatcher     master;

    private final FleaseStage              fstage;

    private final RPCNIOSocketClient       client;

    private final OSDServiceClient         osdClient;

    private final AtomicInteger            externalRequestsInQueue;

    private final Map<String, ECFileState> fileStates;

    public ECMasterStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength,
            FleaseStage fstage, FleasePrefixHandler fleaseHandler) throws IOException {
        super("ECMasterStage", maxRequestsQueueLength);
        this.master = master;

        // FIXME (jdillmann): Do i need my own RPC client? What should be the timeouts?
        client = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, "ECMasterStage");
        osdClient = new OSDServiceClient(client, null);
        externalRequestsInQueue = new AtomicInteger(0);

        fileStates = new HashMap<String, ECFileState>();

        // TODO (jdillmann): make local id a parameter?
        localUUID = master.getConfig().getUUID();
        localID = new ASCIIString(localUUID.toString());

        this.fstage = fstage;
        master.getFleaseHandler().registerPrefix(FLEASE_PREFIX, new FleaseViewChangeListenerInterface() {

            @Override
            public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal) {
                // eventViewIdChanged(cellId, viewId, onProposal);
                // FIXME (jdillmann): Implement views on EC
            }
        }, new FleaseStatusListener() {

            @Override
            public void statusChanged(ASCIIString cellId, Flease lease) {
                eventLeaseStateChanged(cellId, lease, null);
            }

            @Override
            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                // change state
                // flush pending requests
                eventLeaseStateChanged(cellId, null, error);
            }
        });

    }

    @Override
    public void start() {
        client.start();
        super.start();
    }

    @Override
    public void shutdown() {
        client.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        client.waitForStartup();
        super.waitForStartup();
    }

    @Override
    public void waitForShutdown() throws Exception {
        client.waitForShutdown();
        super.waitForShutdown();
    }

    void enqueueOperation(final STAGE_OP stageOp, final Object[] args, final OSDRequest request,
            final ReusableBuffer createdViewBuffer, final Object callback) {
        enqueueOperation(stageOp.ordinal(), args, request, createdViewBuffer, callback);
    }

    void enqueueExternalOperation(final STAGE_OP stageOp, final Object[] args, final OSDRequest request,
            final ReusableBuffer createdViewBuffer, final Object callback) {
        if (externalRequestsInQueue.get() >= MAX_EXTERNAL_REQUESTS_IN_Q) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "EC stage is overloaded, request %d for %s dropped",
                    request.getRequestId(), request.getFileId());
            request.sendInternalServerError(
                    new IllegalStateException("EC replication stage is overloaded, request dropped"));

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
            this.enqueueOperation(stageOp, args, request, createdViewBuffer, callback);
        }
    }

    void enqueuePrioritized(StageRequest rq) {
        while (!q.offer(rq)) {
            StageRequest otherRq = q.poll();
            otherRq.sendInternalServerError(
                    new IllegalStateException("internal queue overflow, cannot enqueue operation for processing."));
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "Dropping request from rwre queue due to overload");
        }
    }

    private static enum STAGE_OP {
        PREPARE, READ, WRITE, TRUNCATE, LEASE_STATE_CHANGED;

        private static STAGE_OP[] values_ = values();

        public static STAGE_OP valueOf(int n) {
            return values_[n];
        }
    };

    @Override
    protected void processMethod(StageRequest method) {
        switch (STAGE_OP.valueOf(method.getStageMethod())) {
        // External requests
        case PREPARE:
            externalRequestsInQueue.decrementAndGet();
            processPrepare(method);
            break;
        case READ:
            externalRequestsInQueue.decrementAndGet();
            break;
        case WRITE:
            externalRequestsInQueue.decrementAndGet();
            break;
        case TRUNCATE:
            externalRequestsInQueue.decrementAndGet();
            break;
        // Internal requests
        case LEASE_STATE_CHANGED:
            processLeaseChanged(method);
            break;

        // default : throw new IllegalArgumentException("No such stageop");
        }
    }

    /**
     * If the file is already open, return the cached fileState. <br>
     * If it is closed create a FileState in state INITALIZING and open a flease cell.
     */
    ECFileState getFileState(FileCredentials credentials, XLocations locations, boolean invalidated) {

        final String fileId = credentials.getXcap().getFileId();
        ECFileState file = fileStates.get(fileId);

        if (file == null) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "Opening FileState for: %s", fileId);

            ASCIIString cellId = new ASCIIString(FLEASE_PREFIX + fileId);

            file = new ECFileState(fileId, cellId, localUUID, locations, credentials);
            fileStates.put(fileId, file);
        }

        if (file.getState() == FileState.INITIALIZING && !file.isCellOpen()) {
            doWaitingForLease(file);
        }

        return file;
    }

    /**
     * This will reset the FileState to INITIALIZING and aborts pending requests.<br>
     * If a flease cell is open, it will be closed
     */
    void failed(final ECFileState file, final ErrorResponse ex) {
        // FIXME (jdillmann): Ensure everything is reset to the INIT state
        Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this,
                "(R:%s) replica for file %s failed (in state: %s): %s", localID, file.getFileId(), file.getState(),
                ErrorUtils.formatError(ex));
        file.resetDefaults();
        fstage.closeCell(file.getCellId(), false);
        abortPendingRequests(file, ex);
    }

    /**
     * Abort all pending requests associated with this FileState.
     */
    void abortPendingRequests(final ECFileState file, ErrorResponse error) {
        if (error == null) {
            error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "Request had been aborted.");
        }

        while (file.hasPendingRequests()) {
            StageRequest request = file.removePendingRequest();
            Object callback = request.getCallback();
            if (callback != null && callback instanceof FallibleCallback) {
                ((FallibleCallback) callback).failed(error);
            }
        }
    }

    /**
     * Go to state WAITING_FOR_LEASE and open the flease cell. <br>
     * When flease is ready {@link #eventLeaseStateChanged} will be called.
     */
    void doWaitingForLease(final ECFileState file) {
        assert (!file.isCellOpen());
        // FIXME (jdillmann): Maybe check if cell is already open
        // can't happen right now, since doWaitingForLease is only called from getState

        try {
            XLocations locations = file.getLocations();
            List<ServiceUUID> remoteOSDs = file.getRemoteOSDs();
            List<InetSocketAddress> acceptors = new ArrayList<InetSocketAddress>(remoteOSDs.size());
            for (ServiceUUID service : remoteOSDs) {
                acceptors.add(service.getAddress());
            }

            fstage.openCell(file.getCellId(), acceptors, true, locations.getVersion());

            file.setState(FileState.WAITING_FOR_LEASE);
            file.setCellOpen(true);

        } catch (UnknownUUIDException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }

    }

    void doPrimary(final ECFileState file) {
        file.setState(FileState.PRIMARY);
    }

    /**
     * Go to state BACKUP and re-enqueue pending requests prioritized.
     */
    void doBackup(final ECFileState file) {
        file.setState(FileState.BACKUP);

        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
    }

    /**
     * Go to state VERSION_RESET and start to fetch local and remote versions.<br>
     * When enough versions are fetched {@link #eventVersionsFetched} will be called.
     */
    void doVersionReset(final ECFileState file) {
        file.setState(FileState.VERSION_RESET);
        fetchVersions(file);
    }


    /**
     * This will be called every time the flease cell changed.
     */
    void eventLeaseStateChanged(ASCIIString cellId, Flease lease, FleaseException error) {
        this.enqueueOperation(STAGE_OP.LEASE_STATE_CHANGED, new Object[] { cellId, lease, error }, null, null, null);
    }

    void processLeaseChanged(StageRequest method) {
        final ASCIIString cellId = (ASCIIString) method.getArgs()[0];
        final Flease lease = (Flease) method.getArgs()[1];
        final FleaseException error = (FleaseException) method.getArgs()[2];

        final String fileId = FleasePrefixHandler.stripPrefix(cellId).toString();
        final ECFileState file = fileStates.get(fileId);

        if (file == null) {
            // Lease cells can be opened from any OSD in the Xloc, thus the file has not to be opened on this OSD.
            // Receiving an leaseChangeEvent is no error.
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "Received LeaseChange event for non opened file (%s)", fileId);
            return;
        }

        if (error != null) {
            failed(file, ErrorUtils.getInternalServerError(error, "Lease Error"));
            return;
        }

        // TODO(jdillmann): Invalidation stuff?
        FileState state = file.getState();
        boolean localIsPrimary = (lease.getLeaseHolder() != null) && (lease.getLeaseHolder().equals(localID));
        file.setLocalIsPrimary(localIsPrimary);
        file.setLease(lease);

        // Handle timeouts on the primary by throwing an error
        if (state == FileState.PRIMARY && lease.getLeaseHolder() == null && lease.getLeaseTimeout_ms() == 0) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.ec, this,
                    "(R:%s) was primary, lease error in cell %s, restarting replication: %s", localID, cellId, lease);
            failed(file,
                    ErrorUtils.getInternalServerError(new IOException(fileId + ": lease timed out, renew failed")));
            return;
        }

        // The VERSION_RESET is only triggered if the OSD became primary. If in the meantime it was deselected the
        // operation should be aborted
        if (state == FileState.VERSION_RESET && !localIsPrimary) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.ec, this,
                    "(R:%s) is in VERSION_RESET but lost its PRIMARY state in cell %s: %s", localID, cellId, lease);
            failed(file,
                    ErrorUtils.getInternalServerError(new IOException("Primary was deselected during VERSION_RESET")));
            return;
        }

        // FIXME (jdillmann): Check again which states will result in an error

        // Only make a transition if the file is in a valid state.
        if (state == FileState.PRIMARY || state == FileState.BACKUP || state == FileState.WAITING_FOR_LEASE) {
            if (localIsPrimary && state != FileState.PRIMARY) {
                // The local OSD became primary and has been BACKUP or WAITING before
                file.setMasterEpoch(lease.getMasterEpochNumber());
                doVersionReset(file);

            } else if (!localIsPrimary && state != FileState.BACKUP) {
                // The local OSD became backup and has been PRIMARY or WAITING before
                file.setMasterEpoch(FleaseMessage.IGNORE_MASTER_EPOCH);
                doBackup(file);
            }
        }
    }

    public void prepare(FileCredentials credentials, XLocations xloc, Interval interval, PrepareCallback callback,
            OSDRequest request) {
        this.enqueueExternalOperation(STAGE_OP.PREPARE, new Object[] { credentials, xloc, interval }, request, null,
                callback);
    }

    public static interface PrepareCallback extends FallibleCallback {
        public void success(final List<Interval> curVersions, final List<Interval> nextVersions);
        public void redirect(String redirectTo);
    }

    void processPrepare(StageRequest method) {
        final PrepareCallback callback = (PrepareCallback) method.getCallback();
        final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
        final XLocations loc = (XLocations) method.getArgs()[1];
        final Interval interval = (Interval) method.getArgs()[2];

        final String fileId = credentials.getXcap().getFileId();
        StripingPolicyImpl sp = loc.getLocalReplica().getStripingPolicy();
        assert (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE);

        final ECFileState file = getFileState(credentials, loc, false);
        // FIXME (jdillmann): Deadlock, falls bei doWaitingForLease Fehler auftreten.
        // Dann bleibt der Zustand INIT, aber es wird nicht nochmal doWaiting aufgerufen
        // Fehler auch bei RWR vorhanden, daher erstmal ignorieren

        // if (!isInternal)
        file.setCredentials(credentials);

        switch (file.getState()) {

        case INITIALIZING:
        case WAITING_FOR_LEASE:
        case VERSION_RESET:
            if (file.sizeOfPendingRequests() > MAX_PENDING_PER_FILE) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                            "Rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                            file.sizeOfPendingRequests(), MAX_PENDING_PER_FILE, fileId);
                }
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
            } else {
                file.addPendingRequest(method);
            }
            return;

        case BACKUP:
            assert(!file.isLocalIsPrimary());
            // if (!isInternal)
            
            Flease lease = file.getLease();
            if (lease.isEmptyLease()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                        "Unknown lease state for %s: %s", file.getCellId(), lease);
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_EAGAIN,
                        "Unknown lease state for cell " + file.getCellId()
                                + ", can't redirect to master. Please retry.");
                // FIXME (jdillmann): abort all requests?
                callback.failed(error);
                return;
            }

            callback.redirect(lease.getLeaseHolder().toString());
            return;
            
        case PRIMARY:
            assert (file.isLocalIsPrimary());
            // FIXME (jdillmann): Return real version vectors
            callback.success(null, null);
            return;


        // case INVALIDATED:
        // break;
        // case OPENING:
        // break;

        // default:
        // break;

        }
    }

    /**
     * This will request the local and remote VersionTrees.
     * On success eventVersionResetComplete is called
     * @param file
     */
    // TODO (jdillmann): Think about moving this to a separate class or OSD Event Method
    void fetchVersions(final ECFileState file) {
        StripingPolicyImpl sp = file.getLocations().getLocalReplica().getStripingPolicy();
        master.getStorageStage().getECVersions(file.getFileId(), sp, Interval.COMPLETE, null,
                new GetECVersionsCallback() {
                    @Override
                    public void getECVersionsComplete(List<Interval> curVersions, List<Interval> nextVersions,
                            ErrorResponse error) {
                        if (error == null) {
                            fetchRemoteVersions(file, curVersions, nextVersions);
                        } else {
                            failed(file, error);
                        }
                    }
                });
    }

    /**
     * This will be called after the local VersionTree has been fetched.
     * Attention: This will be executed in the context of the StorageStage.
     * @param file
     * @param localCurVer
     * @param nextCurVer
     */
    // TODO (jdillmann): Think about moving this to a separate class or OSD Event Method
    void fetchRemoteVersions(final ECFileState file, final List<Interval> localCurVer,
            final List<Interval> nextCurVer) {
        
        
        
    }
}
