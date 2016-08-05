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
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
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
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FleasePrefixHandler;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECFileState.FileState;
import org.xtreemfs.osd.ec.ECWorker.ECWorkerEvent;
import org.xtreemfs.osd.ec.ECWorker.ECWorkerEventProcessor;
import org.xtreemfs.osd.ec.ECWorker.TYPE;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.LocalRPCResponseQuorumListener;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.ResponseResult;
import org.xtreemfs.osd.operations.ECCommitVector;
import org.xtreemfs.osd.operations.ECGetIntervalVectors;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.readRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_vectorsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_diffResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public class ECMasterStage extends Stage implements ECWorkerEventProcessor {
    // FIXME (jdilmann): What would be sane queue limits?
    private static final int               MAX_EXTERNAL_REQUESTS_IN_Q = 250;
    private static final int               MAX_PENDING_PER_FILE       = 10;

    private static final String            FLEASE_PREFIX              = "/ec/";

    private static final long              WRITE_WORKER_TIMEOUT_MS    = 30 * 1000;
    private static final long              READ_WORKER_TIMEOUT_MS     = 30 * 1000;

    private final ServiceUUID              localUUID;
    private final ASCIIString              localID;

    private final OSDRequestDispatcher     master;

    private final FleaseStage              fstage;

    private final RPCNIOSocketClient       client;

    private final AtomicInteger            externalRequestsInQueue;

    private final Map<String, ECFileState> fileStates;

    private long                           opIdCount;

    final OSDServiceClient                 osdClient;

    final Timer                            timer;

    public ECMasterStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength,
            FleaseStage fstage, FleasePrefixHandler fleaseHandler) throws IOException {
        super("ECMasterStage", maxRequestsQueueLength);
        this.master = master;

        // FIXME (jdillmann): Do i need my own RPC client? What should be the timeouts?
        client = new RPCNIOSocketClient(sslOpts, 15 * 1000, 5 * 60 * 1000, "ECMasterStage");
        osdClient = new OSDServiceClient(client, null);
        externalRequestsInQueue = new AtomicInteger(0);

        // Start a timer daemon
        timer = new Timer(true);

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

        // Initialize this masters operation_id
        opIdCount = 0;
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
            Logging.logMessage(Logging.LEVEL_WARN, this, "EC master stage is overloaded, request %d for %s dropped",
                    request.getRequestId(), request.getFileId());
            request.sendInternalServerError(
                    new IllegalStateException("EC master stage is overloaded, request dropped"));

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
        READ, WRITE, TRUNCATE, GET_FILE_SIZE,
        SEND_DIFF_RESPONSE, RECV_DIFF_RESPONSE,
        TRIGGER_RECONSTRUCTION,
        LEASE_STATE_CHANGED, 
        CLOSE, VECTORS_AVAILABLE, COMMIT_VECTOR_COMPLETE,
        WRITE_WORKER_SIGNAL, READ_WORKER_SIGNAL,;

        private static STAGE_OP[] values_ = values();

        public static STAGE_OP valueOf(int n) {
            return values_[n];
        }
    };

    @Override
    protected void processMethod(StageRequest method) {
        switch (STAGE_OP.valueOf(method.getStageMethod())) {
        // External requests
        case READ:
            externalRequestsInQueue.decrementAndGet();
            processRead(method);
            break;
        case READ_WORKER_SIGNAL:
            processReadWorkerSignal(method);
            break;
        case WRITE:
            // FIXME (jdillmann): The write is not finished after the process write method returned
            externalRequestsInQueue.decrementAndGet();
            processWrite(method);
            break;
        case WRITE_WORKER_SIGNAL:
            processWriteWorkerSignal(method);
            break;
        case TRUNCATE:
            externalRequestsInQueue.decrementAndGet();
            break;
        case GET_FILE_SIZE:
            externalRequestsInQueue.decrementAndGet();
            processGetFileSize(method);
            break;
        // Internal requests
        case LEASE_STATE_CHANGED:
            processLeaseStateChanged(method);
            break;
        case VECTORS_AVAILABLE:
            processVectorsAvailable(method);
            break;
        case COMMIT_VECTOR_COMPLETE:
            processCommitVectorComplete(method);
            break;
        case CLOSE:
            processCloseFile(method);
            break;
        case SEND_DIFF_RESPONSE:
            processSendDiffResponse(method);
            break;
        case RECV_DIFF_RESPONSE:
            processRecvDiffResponse(method);
            break;
        case TRIGGER_RECONSTRUCTION:
            processTriggerReconstruction(method);
            break;

        // default : throw new IllegalArgumentException("No such stageop");
        }
    }

    @Override
    public void signal(ECWorker worker, ECWorkerEvent event) {
        STAGE_OP op;

        switch (worker.getType()) {
        case WRITE:
            op = STAGE_OP.WRITE_WORKER_SIGNAL;
            break;

        case READ:
            op = STAGE_OP.READ_WORKER_SIGNAL;
            break;

        default:
            Logging.logMessage(Logging.LEVEL_INFO, this, "Unknown worker type %s ignores", worker.getType());
            return;

        }

        // Re-Enque
        Object callback = worker.getRequest().getCallback();
        OSDRequest request = worker.getRequest().getRequest();
        this.enqueueOperation(op, new Object[] { worker, event }, request, null, callback);
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
        fstage.closeCell(file.getCellId(), false);
        abortPendingRequests(file, ex);
        // Finally reset the file state
        file.resetDefaults();
    }

    /**
     * Abort all pending requests associated with this FileState.
     */
    void abortPendingRequests(final ECFileState file, ErrorResponse error) {
        if (error == null) {
            error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "Request had been aborted.");
        }

        for (ECWorker worker : file.getActiveRequests()) {
            Object callback = worker.getRequest().getCallback();
            if (callback != null && callback instanceof FallibleCallback) {
                ((FallibleCallback) callback).failed(error);
            }
        }
        file.clearActiveRequests();

        while (file.hasPendingRequests()) {
            StageRequest request = file.removePendingRequest();
            Object callback = request.getCallback();
            if (callback != null && callback instanceof FallibleCallback) {
                ((FallibleCallback) callback).failed(error);
            }
        }
    }

    void addPendingRequest(final ECFileState file, StageRequest request) {
        if (file.sizeOfPendingRequests() > MAX_PENDING_PER_FILE) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                        "Rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                        file.sizeOfPendingRequests(), MAX_PENDING_PER_FILE, file.getFileId());
            }
            Object callback = request.getCallback();
            if (callback != null && callback instanceof FallibleCallback) {
                ((FallibleCallback) callback).failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
            }
        } else {
            file.addPendingRequest(request);
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

        while (file.hasPendingRequests()) {
            enqueuePrioritized(file.removePendingRequest());
        }
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
        fetchVectors(file);
    }

    /**
     * This will be called every time the flease cell changed.
     */
    void eventLeaseStateChanged(ASCIIString cellId, Flease lease, FleaseException error) {
        this.enqueueOperation(STAGE_OP.LEASE_STATE_CHANGED, new Object[] { cellId, lease, error }, null, null, null);
    }

    void processLeaseStateChanged(StageRequest method) {
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
                if (file.getMasterEpoch() != lease.getMasterEpochNumber()) {
                    long masterEpoch = lease.getMasterEpochNumber();
                    // use the lower 32 bit of the master epoch for the new opId
                    opIdCount = (masterEpoch << 32);

                    file.setMasterEpoch(lease.getMasterEpochNumber());
                }
                doVersionReset(file);

            } else if (!localIsPrimary && state != FileState.BACKUP) {
                // The local OSD became backup and has been PRIMARY or WAITING before
                opIdCount = 0;
                file.setMasterEpoch(FleaseMessage.IGNORE_MASTER_EPOCH);
                doBackup(file);
            }
        }
    }

    public void closeFile(String fileId) {
        this.enqueueOperation(STAGE_OP.CLOSE, new Object[] { fileId }, null, null, null);
    }

    private void processCloseFile(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        // Files are closed due to a timer in the openFileTable or if they are unlinked.
        // Since the openFileTable is pinged on most operations and an unlinked file is no longer available the
        // fileState can be closed.
        // TODO: Correct errno would be probably EBADF (9)
        ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                "file has been closed");

        ECFileState state = fileStates.remove(fileId);
        if (state != null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "closing file %s", fileId);
            }

            abortPendingRequests(state, error);
            fstage.closeCell(state.getCellId(), false);
        }
    }

    public static interface RedirectCallback extends FallibleCallback {
        public void redirect(String redirectTo);
    }

    boolean prepare(StageRequest method, FileCredentials credentials, XLocations loc) {
        // final String fileId = credentials.getXcap().getFileId();
        StripingPolicyImpl sp = loc.getLocalReplica().getStripingPolicy();
        assert (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE);

        final ECFileState file = getFileState(credentials, loc, false);
        file.setCredentials(credentials);

        switch (file.getState()) {

        case INITIALIZING:
        case WAITING_FOR_LEASE:
        case VERSION_RESET:
            addPendingRequest(file, method);
            return false;

        case BACKUP:
            assert (!file.isLocalIsPrimary());
            return true;

        case PRIMARY:
            assert (file.isLocalIsPrimary());
            return true;

        case INVALIDATED:
        default:
            // TODO (jdillmann): Handle INVALIDATED errors
            return false;
        }
    }

    boolean preparePrimary(StageRequest method, RedirectCallback callback, FileCredentials credentials, XLocations loc,
            ReusableBuffer viewData) {
        // final String fileId = credentials.getXcap().getFileId();
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
            addPendingRequest(file, method);
            return false;

        case BACKUP:
            assert (!file.isLocalIsPrimary());
            // if (!isInternal)

            BufferPool.free(viewData);

            Flease lease = file.getLease();
            if (lease.isEmptyLease()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, "Unknown lease state for %s: %s",
                        file.getCellId(), lease);
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_EAGAIN, "Unknown lease state for cell " + file.getCellId()
                                + ", can't redirect to master. Please retry.");
                abortPendingRequests(file, error);
                callback.failed(error);
                return false;
            }

            callback.redirect(lease.getLeaseHolder().toString());
            return false;

        case PRIMARY:
            assert (file.isLocalIsPrimary());
            return true;

        case INVALIDATED:
        default:
            BufferPool.free(viewData);
            // TODO (jdillmann): Handle INVALIDATED errors
            return false;
        }

    }
    

    

    /**
     * This will request the local and remote VersionTrees.
     * 
     * @param file
     */
    // TODO (jdillmann): Think about moving this to a separate class or OSD Event Method
    void fetchVectors(final ECFileState file) {
        assert (file.state == FileState.VERSION_RESET);
        final String fileId = file.getFileId();

        List<ServiceUUID> remoteUUIDs = file.getRemoteOSDs();
        int numRemotes = remoteUUIDs.size();

        LocalRPCResponseQuorumListener<xtreemfs_ec_get_vectorsResponse, Integer> listener;
        listener = new LocalRPCResponseQuorumListener<xtreemfs_ec_get_vectorsResponse, Integer>() {

            @Override
            public void success(ResponseResult<xtreemfs_ec_get_vectorsResponse, Integer>[] results) {
                eventVectorsAvailable(fileId, results, null);
            }

            @Override
            public void failed(ResponseResult<xtreemfs_ec_get_vectorsResponse, Integer>[] results) {
                // TODO (jdillmann): Add numberOfFailures as a parameter?
                String errorMsg = String.format("(EC: %s) VectorReset failed due to too many unreachable remote OSDs.",
                        localUUID);
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                        errorMsg);
                eventVectorsAvailable(fileId, null, error);
            }
        };

        // Try to get a result from every node
        int numResponses = numRemotes + 1;
        // FIXME (jdillmann): How to determine how long to wait without an error detector?
        int numReqAck = file.getPolicy().getReadQuorum();

        final LocalRPCResponseHandler<xtreemfs_ec_get_vectorsResponse, Integer> handler;
        handler = new LocalRPCResponseHandler<xtreemfs_ec_get_vectorsResponse, Integer>(numResponses,
                numReqAck, listener);

        try {
            for (int i = 0; i < numRemotes; i++) {
                ServiceUUID uuid = remoteUUIDs.get(i);

                RPCResponse<xtreemfs_ec_get_vectorsResponse> response;
                response = osdClient.xtreemfs_ec_get_vectors(uuid.getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, file.getCredentials(), fileId);
                handler.addRemote(response, i);
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getInternalServerError(ex));
        }

        // Add the local with some invalid id
        handler.addLocal(-1);

        // Wait for the local result
        ECGetIntervalVectors getVectorOp = (ECGetIntervalVectors) master.getOperation(ECGetIntervalVectors.PROC_ID);
        getVectorOp.startLocalRequest(fileId, handler);
    }

    void eventVectorsAvailable(String fileId,
            ResponseResult<xtreemfs_ec_get_vectorsResponse, Integer>[] results, ErrorResponse error) {
        this.enqueueOperation(STAGE_OP.VECTORS_AVAILABLE, new Object[] { fileId, results, error }, null, null, null);
    }

    void processVectorsAvailable(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        @SuppressWarnings("unchecked")
        final ResponseResult<xtreemfs_ec_get_vectorsResponse, Integer>[] results = (ResponseResult<xtreemfs_ec_get_vectorsResponse, Integer>[]) method
                .getArgs()[1];
        final ErrorResponse error = (ErrorResponse) method.getArgs()[2];

        final ECFileState file = fileStates.get(fileId);

        if (file == null) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "Received LocalIntervalVectorAvailable event for non opened file (%s)", fileId);
            return;
        }

        if (error != null) {
            failed(file, error);
            return;
        }

        assert (file.state == FileState.VERSION_RESET);

        List<Interval>[] curVectors = new List[results.length + 1];
        List<Interval>[] nextVectors = new List[results.length + 1];

        int responseCount = 0;

        // Transform protobuf messages to IntervalVectors
        for (int r = 0; r < results.length; r++) {
            xtreemfs_ec_get_vectorsResponse response = results[r].getResult();

            if (response != null) {
                responseCount++;

                curVectors[r] = new ArrayList<Interval>(response.getCurIntervalsCount());
                for (int i = 0; i < response.getCurIntervalsCount(); i++) {
                    curVectors[r].add(new ProtoInterval(response.getCurIntervals(i)));
                }

                nextVectors[r] = new ArrayList<Interval>(response.getNextIntervalsCount());
                for (int i = 0; i < response.getNextIntervalsCount(); i++) {
                    nextVectors[r].add(new ProtoInterval(response.getNextIntervals(i)));
                }
            } else {
                // FIXME (jdillmann): Mark the OSD as failed and allow for fastfail until a timeout is reached
                curVectors[r] = null;
                curVectors[r] = null;
            }
        }

        // Recover the latest interval vectors from the available vectors.
        final AVLTreeIntervalVector resultVector = new AVLTreeIntervalVector();
        boolean needsCommit;
        try {
            needsCommit = file.getPolicy().recoverVector(responseCount, curVectors, nextVectors, resultVector);
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_WARN, this, e);
            failed(file, null);
            return;
        }

        file.setCurVector(resultVector);
        if (needsCommit) {
            // Well well... and we have to do another round!
            commitVector(file, resultVector);

        } else {
            // FIXME (jdillmann): Maybe send async commit
            doPrimary(file);
        }
    }

    void commitVector(final ECFileState file, final IntervalVector resultVector) {
        // TODO (jdillmann): Or only an if
        assert (file.state == FileState.VERSION_RESET);

        final String fileId = file.getFileId();

        List<ServiceUUID> remoteUUIDs = file.getRemoteOSDs();
        int numRemotes = remoteUUIDs.size();

        List<Interval> resultIntervals = resultVector.serialize();

        xtreemfs_ec_commit_vectorRequest.Builder reqBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder();
        reqBuilder.setFileId(fileId).setFileCredentials(file.getCredentials());
        for (Interval interval : resultIntervals) {
            reqBuilder.addIntervals(ProtoInterval.toProto(interval));
        }
        xtreemfs_ec_commit_vectorRequest request = reqBuilder.build();

        LocalRPCResponseQuorumListener<xtreemfs_ec_commit_vectorResponse, Integer> listener;
        listener = new LocalRPCResponseQuorumListener<xtreemfs_ec_commit_vectorResponse, Integer>() {

            @Override
            public void success(ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[] results) {
                eventCommitVectorComplete(fileId, results, null);
            }

            @Override
            public void failed(ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[] results) {
                // TODO (jdillmann): Add numberOfFailures as a parameter?
                String errorMsg = String.format("(EC: %s) VectorReset failed due to too many unreachable remote OSDs.",
                        localUUID);
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                        errorMsg);
                eventCommitVectorComplete(fileId, null, error);
            }
        };

        int numResponses = numRemotes + 1;
        int numAcksReq = file.getPolicy().getDataWidth();
        final LocalRPCResponseHandler<xtreemfs_ec_commit_vectorResponse, Integer> handler;
        handler = new LocalRPCResponseHandler<xtreemfs_ec_commit_vectorResponse, Integer>(numResponses, numAcksReq,
                listener);

        try {
            for (int i = 0; i < numRemotes; i++) {
                ServiceUUID uuid = remoteUUIDs.get(i);

                RPCResponse<xtreemfs_ec_commit_vectorResponse> response;
                response = osdClient.xtreemfs_ec_commit_vector(uuid.getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, request);
                handler.addRemote(response, i);
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getInternalServerError(ex));
        }

        // Add the local with some invalid id
        handler.addLocal(-1);

        // Wait for the local result
        ECCommitVector getVectorOp = (ECCommitVector) master.getOperation(ECCommitVector.PROC_ID);
        getVectorOp.startLocalRequest(fileId, file.getCredentials(), file.getLocations(),
                file.getPolicy().getStripingPolicy(), resultIntervals, handler);

    }

    void eventCommitVectorComplete(String fileId, ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[] results,
            ErrorResponse error) {
        this.enqueueOperation(STAGE_OP.COMMIT_VECTOR_COMPLETE, new Object[] { fileId, results, error }, null, null,
                null);
    }

    void processCommitVectorComplete(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        @SuppressWarnings("unchecked")
        final ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[] results = (ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[]) method
                .getArgs()[1];
        final ErrorResponse error = (ErrorResponse) method.getArgs()[2];

        final ECFileState file = fileStates.get(fileId);

        if (file == null) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "Received eventCommitVectorComplete event for non opened file (%s)", fileId);
            return;
        }

        if (file.state != FileState.VERSION_RESET) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "Ignore eventCommitVectorComplete event for file (%s) in state %s", fileId, file.state);
            return;
        }

        if (error != null) {
            failed(file, error);
            return;
        }

        int numComplete = 0;
        for (ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer> result : results) {
            if (result.hasFinished() && !result.hasFailed() && !result.getResult().getNeedsReconstruction()) {
                numComplete++;
            }
        }

        if (numComplete >= file.getPolicy().getDataWidth()) {
            doPrimary(file);
        } else {
            String errorMsg = String.format(
                    "(EC: %s) CommitVector failed because less then k nodes could complete the commit.", localUUID);
            ErrorResponse error2 = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    errorMsg);
            failed(file, error2);
        }
    }

    public void read(OSDRequest request, ReadCallback callback) {
        this.enqueueExternalOperation(STAGE_OP.READ, new Object[] {}, request, null, callback);
    }

    public static interface ReadCallback extends RedirectCallback {
        public void success(ObjectInformation result);
    }

    void processRead(StageRequest method) {
        final ReadCallback callback = (ReadCallback) method.getCallback();

        final OSDRequest rq = method.getRequest();
        final readRequest args = (readRequest) rq.getRequestArgs();

        final String fileId = rq.getFileId();
        final XLocations loc = rq.getLocationList();
        final FileCredentials credentials = args.getFileCredentials();
        final StripingPolicyImpl sp = loc.getLocalReplica().getStripingPolicy();

        assert (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE);

        if (!preparePrimary(method, callback, credentials, loc, null)) {
            return;
        }

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    "file is not open!"));
            return;
        }
        assert (file.isLocalIsPrimary());

        long firstObjNo = args.getObjectNumber();
        int reqOffset = args.getOffset();
        int reqSize = args.getLength();

        long start = sp.getObjectStartOffset(firstObjNo) + reqOffset;
        long end = start + reqSize;
        
        long stripeStart = sp.getObjectStartOffset(sp.getRow(firstObjNo) * sp.getWidth());
        long stripeEnd = sp.getObjectStartOffset((sp.getRow(firstObjNo) + 1) * sp.getWidth());

        Interval reqInterval = ObjectInterval.empty(start, end);
        ReusableBuffer data = BufferPool.allocate(reqSize);

        // Prevent reads concurrent to writes?
        if (file.overlapsActiveWriteRequest(reqInterval)) {
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                    "The file is currently written at an overlapping range by another process.");
            callback.failed(error);
            return;
        }

        List<Interval> commitIntervals = file.getCurVector().getOverlapping(start, end);
        ECReadWorker worker = new ECReadWorker(master, credentials, loc, fileId, sp, reqInterval,
                commitIntervals, data, method, READ_WORKER_TIMEOUT_MS, this);
        file.addActiveRequest(worker);
        worker.start();
    }

    void processReadWorkerSignal(StageRequest method) {
        final ReadCallback callback = (ReadCallback) method.getCallback();

        final OSDRequest rq = method.getRequest();
        final String fileId = rq.getFileId();

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    "file is not open!"));
            return;
        }

        final ECReadWorker worker = (ECReadWorker) method.getArgs()[0];
        final ECReadWorker.ReadEvent event = (ECReadWorker.ReadEvent) method.getArgs()[1];

        // Try to get the full stripe interval
        if (event.needsStripeInterval()) {
            long stripeNo = event.getStripeNo();
            assert (stripeNo >= 0);

            StripingPolicyImpl sp = file.getPolicy().getStripingPolicy();
            long start = sp.getObjectStartOffset(stripeNo * sp.getWidth());
            long end = sp.getObjectStartOffset((stripeNo + 1) * sp.getWidth());

            Interval reqInterval = worker.getRequestInterval();
            Interval stripeRangeInterval = new ObjectInterval(start, end, reqInterval.getVersion(),
                    reqInterval.getId());

            if (file.overlapsActiveWriteRequest(stripeRangeInterval)) {
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                        "stripe " + stripeNo + " can not be restored, because it is currently written at an overlapping range by another process.");

                worker.abort(error);
                callback.failed(error);
                return;
            }

            List<Interval> stripeInterval = file.getCurVector().getOverlapping(start, end);
            event.setStripeInterval(stripeInterval);
        }

        // Process the worker event
        worker.processEvent(event);

        // Respond to the request if the worker finished
        if (worker.hasFinished()) {
            file.removeActiveRequest(worker);

            if (worker.hasFailed()) {
                callback.failed(worker.getError());
            } else {
                callback.success(worker.getResult());
            }
        }
    }


    public void write(OSDRequest request, ReusableBuffer data, WriteCallback callback) {
        this.enqueueExternalOperation(STAGE_OP.WRITE, new Object[] { data }, request, data, callback);
    }

    public static interface WriteCallback extends RedirectCallback {
        public void success(OSDWriteResponse response);
    }

    void processWrite(StageRequest method) {
        final WriteCallback callback = (WriteCallback) method.getCallback();

        final OSDRequest rq = method.getRequest();
        final writeRequest args = (writeRequest) rq.getRequestArgs();

        final String fileId = rq.getFileId();
        final XLocations loc = rq.getLocationList();
        final FileCredentials credentials = args.getFileCredentials();
        final StripingPolicyImpl sp = loc.getLocalReplica().getStripingPolicy();

        // FIXME (jdillmann): Who is responsible for freeing?
        final ReusableBuffer data = (ReusableBuffer) method.getArgs()[0];

        assert (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE);

        if (!preparePrimary(method, callback, credentials, loc, data)) {
            return;
        }

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    "file is not open!"));
            return;
        }
        assert (file.isLocalIsPrimary());

        // send requests
        file.setCredentials(credentials);

        long firstObjNo = args.getObjectNumber();
        // relative offset to the firstObjNo
        int reqOffset = args.getOffset();
        int reqSize = data.capacity();
        // args.getObjectVersion() // currently ignored

        // absolute start and end to the whole file range
        long opStart = sp.getObjectStartOffset(firstObjNo) + reqOffset;
        long opEnd = opStart + reqSize;

        AVLTreeIntervalVector curVector = file.getCurVector();

        // FIXME (jdillmann): get max counter from range or from the whole vector?
        long maxVersion = curVector.getMaxVersion();
        // increment the opIdCounter and assign it to the current request
        long opId = ++opIdCount;
        long version = maxVersion + 1;

        Interval reqInterval = new ObjectInterval(opStart, opEnd, version, opId);

        // Prevent concurrent writes
        if (file.overlapsActiveWriteRequest(reqInterval)) {
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                    "The file is currently written at an overlapping range by another process.");
            callback.failed(error);
            return;
        }

        List<Interval> commitIntervals = curVector.getOverlapping(opStart, opEnd);
        final ECWriteWorker worker = new ECWriteWorker(master, file.getCredentials(), file.getLocations(), fileId, opId,
                sp, file.getPolicy().getWriteQuorum(), reqInterval, commitIntervals, data, method,
                WRITE_WORKER_TIMEOUT_MS, this);

        file.addActiveRequest(worker);
        worker.start();
    }

    void processWriteWorkerSignal(StageRequest method) {
        final WriteCallback callback = (WriteCallback) method.getCallback();

        final OSDRequest rq = method.getRequest();
        final String fileId = rq.getFileId();

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    "file is not open!"));
            return;
        }

        final ECWriteWorker worker = (ECWriteWorker) method.getArgs()[0];
        final ECWriteWorker.WriteEvent event = (ECWriteWorker.WriteEvent) method.getArgs()[1];

        // Try to get the full stripe interval
        if (event.needsStripeInterval()) {
            long stripeNo = event.getStripeNo();
            assert(stripeNo >= 0);
            
            StripingPolicyImpl sp = file.getPolicy().getStripingPolicy();
            long start = sp.getObjectStartOffset(stripeNo * sp.getWidth());
            long end = sp.getObjectStartOffset((stripeNo + 1) * sp.getWidth());
            
            Interval reqInterval = worker.getRequestInterval();
            Interval stripeRangeInterval = new ObjectInterval(start, end, reqInterval.getVersion(),
                    reqInterval.getId());

            
            if (file.overlapsActiveWriteRequest(stripeRangeInterval)) {
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                        "stripe " + stripeNo + " can not be restored, because it is currently written at an overlapping range by another process.");
                
                worker.abort(error);
                callback.failed(error);
                return;
            }
            
            List<Interval> stripeInterval = file.getCurVector().getOverlapping(start, end);
            event.setStripeInterval(stripeInterval);
        }
        
        // Process the worker event
        worker.processEvent(event);

        // Respond to the request if the worker finished
        if (worker.hasFinished()) {
            file.removeActiveRequest(worker);

            if (worker.hasFailed()) {
                callback.failed(worker.getError());
            } else {
                file.getCurVector().insert(worker.getRequestInterval());

                // FIXME (jdillmann): Get truncate epoch
                long fileSize = file.getCurVector().getEnd();
                int truncateEpoch = file.getCredentials().getXcap().getTruncateEpoch();
                OSDWriteResponse response = OSDWriteResponse.newBuilder().setSizeInBytes(fileSize)
                        .setTruncateEpoch(truncateEpoch).build();

                callback.success(response);
            }
        }
    }

    public void sendDiffResponse(String fileId, FileCredentials fileCredentials, XLocations xloc, long opId,
            long stripeNo, int osdNo, boolean needsReconstruction, ErrorResponse error) {
        this.enqueueOperation(STAGE_OP.SEND_DIFF_RESPONSE,
                new Object[] { fileId, fileCredentials, xloc, opId, stripeNo, osdNo, needsReconstruction, error }, null,
                null, null);
    }

    void processSendDiffResponse(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        final FileCredentials fileCredentials = (FileCredentials) method.getArgs()[1];
        final XLocations xloc = (XLocations) method.getArgs()[2];
        final long opId = (Long) method.getArgs()[3];
        final long stripeNo = (Long) method.getArgs()[4];
        final int osdNo = (Integer) method.getArgs()[5];
        final boolean needsReconstruction = (Boolean) method.getArgs()[6];
        final ErrorResponse error = (ErrorResponse) method.getArgs()[7];

        // Try to find the master
        if (!prepare(method, fileCredentials, xloc)) {
            return;
        }

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "file is not open!");
            return;
        }

        xtreemfs_ec_write_diffResponse.Builder responseB = xtreemfs_ec_write_diffResponse.newBuilder()
                .setFileCredentials(fileCredentials).setFileId(fileId).setOpId(opId).setStripeNumber(stripeNo)
                .setOsdNumber(osdNo).setNeedsReconstruction(needsReconstruction);
        if (error != null) {
            responseB.setError(error);
        }

        if (file.isLocalIsPrimary()) {
            // handle local
            handleParityResponse(file, responseB.build());

        } else {
            Flease lease = file.getLease();
            if (lease.isEmptyLease()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this,
                        "Unknown lease state for %s: %s, can't send response to master.", file.getCellId(), lease);
                return;
            }

            ServiceUUID masterServer = new ServiceUUID(lease.getLeaseHolder());
            try {
                @SuppressWarnings("unchecked")
                RPCResponse<emptyResponse> response = osdClient.xtreemfs_ec_write_diff_response(
                        masterServer.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        responseB.build());
                response.registerListener(ECHelper.emptyResponseListener);
            } catch (IOException ex) {
                Logging.logUserError(Logging.LEVEL_WARN, Category.ec, this, ex);
            }
        }
    }

    public void recvDiffResponse(OSDRequest request) {
        this.enqueueOperation(STAGE_OP.RECV_DIFF_RESPONSE, new Object[] {}, request, null, null);
    }

    void processRecvDiffResponse(StageRequest method) {
        final OSDRequest rq = method.getRequest();
        final xtreemfs_ec_write_diffResponse args = (xtreemfs_ec_write_diffResponse) rq.getRequestArgs();

        final String fileId = rq.getFileId();
        final XLocations loc = rq.getLocationList();
        final FileCredentials credentials = args.getFileCredentials();

        if (!prepare(method, credentials, loc)) {
            return;
        }

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, "Received diff response for non open file %s",
                    fileId);
            return;
        }

        if (!file.isLocalIsPrimary()) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, "Backup received diff response for file %s",
                    fileId);
            return;
        }

        handleParityResponse(file, args);
    }

    private void handleParityResponse(ECFileState file, xtreemfs_ec_write_diffResponse args) {
       ECWriteWorker worker = null;
       for (ECWorker curWorker : file.getActiveRequests()) {
            if (curWorker.getType() == TYPE.WRITE && curWorker.getRequestInterval().getId() == args.getOpId()) {
               worker = (ECWriteWorker) curWorker;
               break;
           }
       }

       if (worker == null) {
           Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this,
                    "Received diff response for unkown operation %d of file %s", args.getOpId(), file.getFileId());
           return;
       }

       worker.handleParityResponse(args);
    }


    public void triggerReconstruction(String fileId, FileCredentials fileCredentials, XLocations xloc, int osdNumber) {
        this.enqueueOperation(STAGE_OP.TRIGGER_RECONSTRUCTION, new Object[] { fileId, fileCredentials, xloc, osdNumber },
                null, null, null);
    }

    void processTriggerReconstruction(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        final FileCredentials fileCredentials = (FileCredentials) method.getArgs()[1];
        final XLocations xloc = (XLocations) method.getArgs()[2];
        final int osdNumber = (Integer) method.getArgs()[3];


        // Try to find the master
        if (!prepare(method, fileCredentials, xloc)) {
            return;
        }

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "file is not open!");
            return;
        }

        if (file.isLocalIsPrimary()) {
            // The primary has to send a commit request to the OSD

            final StripingPolicyImpl sp = file.getPolicy().getStripingPolicy();
            int localOsdPos = sp.getRelativeOSDPosition();
            
            List<Interval> commitIntervals = file.getCurVector().serialize();
            
            if (localOsdPos == osdNumber) {
                // Send local commit
                ECCommitVector getVectorOp = (ECCommitVector) master.getOperation(ECCommitVector.PROC_ID);
                getVectorOp.startLocalRequest(fileId, file.getCredentials(), file.getLocations(),
                        sp, commitIntervals, 
                        new InternalOperationCallback<xtreemfs_ec_commit_vectorResponse>() {
                            
                            @Override
                            public void localResultAvailable(xtreemfs_ec_commit_vectorResponse result, ReusableBuffer data) {
                                // Ignore
                            }
                            
                            @Override
                            public void localRequestFailed(ErrorResponse error) {
                                Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, ErrorUtils.formatError(error));
                            }
                        });

                
            } else {
                // Send remote commit
                List<ServiceUUID> remoteUUIDs = file.getRemoteOSDs();
                ServiceUUID remote = remoteUUIDs.get((localOsdPos <= osdNumber) ? osdNumber - 1 : osdNumber);
    
                xtreemfs_ec_commit_vectorRequest.Builder reqBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder();
                reqBuilder.setFileId(fileId).setFileCredentials(file.getCredentials());
    
                for (Interval interval : file.getCurVector().serialize()) {
                    reqBuilder.addIntervals(ProtoInterval.toProto(interval));
                }
    
                RPCResponse<xtreemfs_ec_commit_vectorResponse> response = null;
                try {
                    response = osdClient.xtreemfs_ec_commit_vector(remote.getAddress(), RPCAuthentication.authNone,
                            RPCAuthentication.userService, reqBuilder.build());
                    xtreemfs_ec_commit_vectorResponse responseMsg = response.get();

                } catch (Exception ex) {
                    Logging.logUserError(Logging.LEVEL_WARN, Category.ec, this, ex);

                } finally {
                    if (response != null) {
                        response.freeBuffers();
                    }
                }
            }


        } else {
            // The backup has to relay the trigger to the master, which will then send a commit to the OSD

            Flease lease = file.getLease();
            if (lease.isEmptyLease()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this,
                        "Unknown lease state for %s: %s, can't send response to master.", file.getCellId(), lease);
                return;
            }

            ServiceUUID masterServer = new ServiceUUID(lease.getLeaseHolder());
            try {
                @SuppressWarnings("unchecked")
                RPCResponse<emptyResponse> response = osdClient.xtreemfs_ec_trigger_reconstruction(
                        masterServer.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        fileCredentials, fileId, osdNumber);
                response.registerListener(ECHelper.emptyResponseListener);
            } catch (IOException ex) {
                Logging.logUserError(Logging.LEVEL_WARN, Category.ec, this, ex);
            }
        }
    }


    public static interface GetFileSizeCallback extends RedirectCallback {
        public void success(long fileSize);
    }

    public void getFileSize(String fileId, FileCredentials credentials, XLocations loc, GetFileSizeCallback callback,
            OSDRequest rq) {
        this.enqueueExternalOperation(STAGE_OP.GET_FILE_SIZE, new Object[] { fileId, credentials, loc }, rq, null,
                callback);
    }

    void processGetFileSize(StageRequest method) {
        final GetFileSizeCallback callback = (GetFileSizeCallback) method.getCallback();
        final String fileId = (String) method.getArgs()[0];
        final FileCredentials credentials = (FileCredentials) method.getArgs()[1];
        final XLocations loc = (XLocations) method.getArgs()[2];
        final StripingPolicyImpl sp = loc.getLocalReplica().getStripingPolicy();

        assert (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE);

        if (!preparePrimary(method, callback, credentials, loc, null)) {
            return;
        }

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    "file is not open!"));
            return;
        }
        assert (file.isLocalIsPrimary());

        callback.success(file.getCurVector().getEnd());
    }
}
