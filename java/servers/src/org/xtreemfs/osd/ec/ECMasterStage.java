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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FleasePrefixHandler;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECFileState.FileState;
import org.xtreemfs.osd.ec.ECMasterStage.ResponseResultManager.ResponseResult;
import org.xtreemfs.osd.ec.ECMasterStage.ResponseResultManager.ResponseResultListener;
import org.xtreemfs.osd.operations.ECCommitVector;
import org.xtreemfs.osd.operations.ECGetIntervalVectors;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_interval_vectorsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import com.google.protobuf.Message;

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
        PREPARE, READ, WRITE, TRUNCATE, LEASE_STATE_CHANGED, CLOSE, 
        VECTORS_AVAILABLE, COMMIT_VECTOR_COMPLETE;

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
            processRead(method);
            break;
        case WRITE:
            externalRequestsInQueue.decrementAndGet();
            break;
        case TRUNCATE:
            externalRequestsInQueue.decrementAndGet();
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
                file.setMasterEpoch(lease.getMasterEpochNumber());
                doVersionReset(file);

            } else if (!localIsPrimary && state != FileState.BACKUP) {
                // The local OSD became backup and has been PRIMARY or WAITING before
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


    public void prepare(FileCredentials credentials, XLocations xloc, PrepareCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGE_OP.PREPARE, new Object[] { credentials, xloc, }, request, null,
                callback);
    }

    public static interface PrepareCallback extends FallibleCallback {
        public void success();

        public void redirect(String redirectTo);
    }

    void processPrepare(StageRequest method) {
        final PrepareCallback callback = (PrepareCallback) method.getCallback();
        final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
        final XLocations loc = (XLocations) method.getArgs()[1];

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
            assert (!file.isLocalIsPrimary());
            // if (!isInternal)

            Flease lease = file.getLease();
            if (lease.isEmptyLease()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this, "Unknown lease state for %s: %s",
                        file.getCellId(), lease);
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                        POSIXErrno.POSIX_ERROR_EAGAIN, "Unknown lease state for cell " + file.getCellId()
                                + ", can't redirect to master. Please retry.");
                // FIXME (jdillmann): abort all requests?
                callback.failed(error);
                return;
            }

            callback.redirect(lease.getLeaseHolder().toString());
            return;

        case PRIMARY:
            assert (file.isLocalIsPrimary());
            callback.success();
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
     * 
     * @param file
     */
    // TODO (jdillmann): Think about moving this to a separate class or OSD Event Method
    void fetchVectors(final ECFileState file) {
        assert (file.state == FileState.VERSION_RESET);
        final String fileId = file.getFileId();

        List<ServiceUUID> remoteUUIDs = file.getRemoteOSDs();
        int numRemotes = remoteUUIDs.size();

        ResponseResultListener<xtreemfs_ec_get_interval_vectorsResponse, Integer> listener;
        listener = new ResponseResultListener<xtreemfs_ec_get_interval_vectorsResponse, Integer>() {

            @Override
            public void success(ResponseResult<xtreemfs_ec_get_interval_vectorsResponse, Integer>[] results) {
                eventVectorsAvailable(fileId, results, null);
            }

            @Override
            public void failed(ResponseResult<xtreemfs_ec_get_interval_vectorsResponse, Integer>[] results) {
                // TODO (jdillmann): Add numberOfFailures as a parameter?
                String errorMsg = String.format("(EC: %s) VectorReset failed due to too many unreachable remote OSDs.",
                        localUUID);
                ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                        errorMsg);
                eventVectorsAvailable(fileId, null, error);
            }
        };

        // Try to get a result from every node
        int numReqAck = numRemotes + 1;

        final ResponseResultManager<xtreemfs_ec_get_interval_vectorsResponse, Integer> manager;
        manager = new ResponseResultManager<xtreemfs_ec_get_interval_vectorsResponse, Integer>(numRemotes, numReqAck,
                true, listener);

        try {
            for (int i = 0; i < numRemotes; i++) {
                ServiceUUID uuid = remoteUUIDs.get(i);

                RPCResponse<xtreemfs_ec_get_interval_vectorsResponse> response;
                response = osdClient.xtreemfs_ec_get_interval_vectors(uuid.getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, file.getCredentials(), fileId);
                manager.add(response, i);
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getInternalServerError(ex));
        }

        // Add the local with some invalid id
        manager.addLocal(-1);

        // Register the listeners and wait for results
        manager.registerListeners();

        // Wait for the local result
        OSDOperation getVectorOp = master.getOperation(ECGetIntervalVectors.PROC_ID);
        getVectorOp.startInternalEvent(
                new Object[] { fileId, new ECInternalOperationCallback<xtreemfs_ec_get_interval_vectorsResponse>() {

                    @Override
                    public void success(xtreemfs_ec_get_interval_vectorsResponse result) {
                        manager.localResultAvailable(result);
                    }

                    @Override
                    public void error(ErrorResponse error) {
                        Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                                "Retrieving the local vector failed for file (%s)", fileId);
                        manager.localResultFailed();
                    }
                } });

    }


    void eventVectorsAvailable(String fileId,
            ResponseResult<xtreemfs_ec_get_interval_vectorsResponse, Integer>[] results, ErrorResponse error) {
        this.enqueueOperation(STAGE_OP.VECTORS_AVAILABLE, new Object[] { fileId, results, error }, null, null, null);
    }

    void processVectorsAvailable(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        @SuppressWarnings("unchecked")
        final ResponseResult<xtreemfs_ec_get_interval_vectorsResponse, Integer>[] results = 
                (ResponseResult<xtreemfs_ec_get_interval_vectorsResponse, Integer>[]) method.getArgs()[1];
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
            xtreemfs_ec_get_interval_vectorsResponse response = results[r].getResult();
            
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
            // FIXME (jdillmann): Do something
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

        ResponseResultListener<xtreemfs_ec_commit_vectorResponse, Integer> listener;
        listener = new ResponseResultListener<xtreemfs_ec_commit_vectorResponse, Integer>() {

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

        final ResponseResultManager<xtreemfs_ec_commit_vectorResponse, Integer> manager;
        manager = new ResponseResultManager<xtreemfs_ec_commit_vectorResponse, Integer>(numRemotes, file.getPolicy().k,
                true, listener);

        try {
            for (int i = 0; i < numRemotes; i++) {
                ServiceUUID uuid = remoteUUIDs.get(i);

                RPCResponse<xtreemfs_ec_commit_vectorResponse> response;
                response = osdClient.xtreemfs_ec_commit_vector(uuid.getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, request);
                manager.add(response, i);
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getInternalServerError(ex));
        }

        // Add the local with some invalid id
        manager.addLocal(-1);

        // Register the listeners and wait for results
        manager.registerListeners();

        // Wait for the local result
        OSDOperation getVectorOp = master.getOperation(ECCommitVector.PROC_ID);
        getVectorOp.startInternalEvent(new Object[] { fileId, file.getPolicy().sp, resultIntervals,
                new ECInternalOperationCallback<xtreemfs_ec_commit_vectorResponse>() {

                    @Override
                    public void success(xtreemfs_ec_commit_vectorResponse result) {
                        manager.localResultAvailable(result);
                    }

                    @Override
                    public void error(ErrorResponse error) {
                        Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                                "Committing the local vector failed for file (%s)", fileId);
                        manager.localResultFailed();
                    }
                } });

    }

    void eventCommitVectorComplete(String fileId,
            ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[] results, ErrorResponse error) {
        this.enqueueOperation(STAGE_OP.COMMIT_VECTOR_COMPLETE, new Object[] { fileId, results, error }, null, null,
                null);
    }

    void processCommitVectorComplete(StageRequest method) {
        final String fileId = (String) method.getArgs()[0];
        @SuppressWarnings("unchecked")
        final ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[] results = 
                (ResponseResult<xtreemfs_ec_commit_vectorResponse, Integer>[]) method.getArgs()[1];
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
            if (result.hasFinished() && !result.hasFailed() && result.getResult().getComplete()) {
                numComplete++;
            }
        }

        if (numComplete < file.getPolicy().k) {
            doPrimary(file);
        } else {
            String errorMsg = String.format(
                    "(EC: %s) CommitVector failed because less then k nodes could complete the commit.", localUUID);
            ErrorResponse error2 = ErrorUtils.getErrorResponse(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    errorMsg);
            failed(file, error2);
        }
    }

    public void read(FileCredentials credentials, XLocations xloc, long start, long end, ReusableBuffer data,
            ReadCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGE_OP.READ, new Object[] { credentials, xloc, start, end, data }, request,
                data, callback);
    }
    
    public static interface ReadCallback extends FallibleCallback {
        public void success(ObjectInformation result);
    }

    void processRead(StageRequest method) {
        final ReadCallback callback = (ReadCallback) method.getCallback();
        final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
        final XLocations loc = (XLocations) method.getArgs()[1];
        final long start = (Long) method.getArgs()[2];
        final long end = (Long) method.getArgs()[3];
        final ReusableBuffer data = (ReusableBuffer) method.getArgs()[4];

        final String fileId = credentials.getXcap().getFileId();
        StripingPolicyImpl sp = loc.getLocalReplica().getStripingPolicy();
        assert (sp.getPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE);

        final ECFileState file = fileStates.get(fileId);
        if (file == null) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    "file is not open!"));
            return;
        }
        file.setCredentials(credentials);

        ObjectInformation result = new ObjectInformation(ObjectStatus.EXISTS, data, 0);
        callback.success(result);

    }


    static class ResponseResultManager<M extends Message, O> implements RPCResponseAvailableListener<M> {
        final AtomicInteger                count;
        final RPCResponse<M>[]             responses;
        final ResponseResult<M, O>[]       results;

        final int                          numAcksRequired;
        final ResponseResultListener<M, O> listener;

        final AtomicInteger                numQuickFail;
        final AtomicInteger                numResponses;
        final AtomicInteger                numErrors;


        @SuppressWarnings("unchecked")
        public ResponseResultManager(int capacity, int numAcksRequired, boolean hasLocal,
                ResponseResultListener<M, O> listener) {
            count = new AtomicInteger(0);
            numQuickFail = new AtomicInteger(0);
            numResponses = new AtomicInteger(0);
            numErrors = new AtomicInteger(0);

            int resultCapacity = hasLocal ? capacity + 1 : capacity;
            results = new ResponseResult[resultCapacity];
            responses = new RPCResponse[capacity];

            this.numAcksRequired = numAcksRequired;
            this.listener = listener;
        }

        public void add(RPCResponse<M> response, O object) {
            add(response, object, false);
        }

        public void add(RPCResponse<M> response, O object, boolean quickFail) {
            int i = count.getAndIncrement();
            if (i >= responses.length) {
                throw new IndexOutOfBoundsException();
            }

            if (quickFail) {
                numQuickFail.incrementAndGet();
            }

            responses[i] = response;
            results[i] = new ResponseResult<M, O>(object, quickFail);
        }

        public void addLocal(O object) {
            assert (results.length == responses.length + 1);
            results[results.length - 1] = new ResponseResult<M, O>(object, false);
        }

        public void registerListeners() {
            for (RPCResponse<M> response : responses) {
                response.registerListener(this);
            }
        }

        @SuppressWarnings("rawtypes")
        static int indexOf(RPCResponse[] responses, RPCResponse response) {
            for (int i = 0; i < responses.length; ++i) {
                if (responses[i].equals(response)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void responseAvailable(RPCResponse<M> r) {
            int i = indexOf(responses, r);
            if (i < 0) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, "received unknown response");
                r.freeBuffers();
                return;
            }

            ResponseResult<M, O> responseResult = results[i];

            int curNumResponses, curNumErrors, curNumQuickFail;

            // Decrement the number of outstanding requests that may fail quick.
            if (responseResult.mayQuickFail()) {
                curNumQuickFail = numQuickFail.decrementAndGet();
            } else {
                curNumQuickFail = numQuickFail.get();
            }

            try {
                // Try to get and add the result.
                M result = r.get();
                responseResult.setResult(result);
                curNumResponses = numResponses.incrementAndGet();
                curNumErrors = numErrors.get();
            } catch (PBRPCException ex) {
                // FIXME (jdillmann): Which errors should mark OSDs as quickfail?
                responseResult.setFailed();
                curNumErrors = numErrors.incrementAndGet();
                curNumResponses = numResponses.get();

            } catch (Exception ex) {
                // Try to
                responseResult.setFailed();
                curNumErrors = numErrors.incrementAndGet();
                curNumResponses = numResponses.get();

            } finally {
                r.freeBuffers();
            }

            // TODO(jdillmann): Think about waiting for quickFail timeouts if numAcksReq is not fullfilled otherwise.
            if (curNumResponses + curNumErrors + curNumQuickFail == results.length) {
                if (curNumResponses >= numAcksRequired) {
                    listener.success(results);
                } else {
                    listener.failed(results);
                }
            }
        }

        public void localResultAvailable(M result) {
            ResponseResult<M, O> responseResult = results[results.length - 1];

            int curNumResponses, curNumErrors, curNumQuickFail;

            responseResult.setResult(result);
            curNumResponses = numResponses.incrementAndGet();
            curNumErrors = numErrors.get();
            curNumQuickFail = numQuickFail.get();

            // TODO(jdillmann): Think about waiting for quickFail timeouts if numAcksReq is not fullfilled otherwise.
            if (curNumResponses + curNumErrors + curNumQuickFail == results.length) {
                if (curNumResponses >= numAcksRequired) {
                    listener.success(results);
                } else {
                    listener.failed(results);
                }
            }
        }

        public void localResultFailed() {
            ResponseResult<M, O> responseResult = results[results.length - 1];

            int curNumResponses, curNumErrors, curNumQuickFail;

            responseResult.setFailed();
            curNumErrors = numErrors.incrementAndGet();
            curNumResponses = numResponses.get();
            curNumQuickFail = numQuickFail.get();

            // TODO(jdillmann): Think about waiting for quickFail timeouts if numAcksReq is not fullfilled otherwise.
            if (curNumResponses + curNumErrors + curNumQuickFail == results.length) {
                if (curNumResponses >= numAcksRequired) {
                    listener.success(results);
                } else {
                    listener.failed(results);
                }
            }
        }

        public static class ResponseResult<M extends Message, O> {
            private final O       object;
            private final boolean quickFail;
            private boolean       failed;
            private M             result;
            private boolean       local;

            ResponseResult(O object, boolean quickFail) {
                this.failed = false;
                this.result = null;
                this.object = object;
                this.quickFail = quickFail;
            }

            synchronized void setFailed() {
                this.failed = true;
                this.result = null;
            }

            synchronized public boolean hasFailed() {
                return failed;
            }

            synchronized public boolean hasFinished() {
                return (result != null || failed);
            }

            synchronized void setResult(M result) {
                this.failed = false;
                this.result = result;
            }

            synchronized public M getResult() {
                return result;
            }

            public O getMappedObject() {
                return object;
            }

            public boolean mayQuickFail() {
                return quickFail;
            }
        }

        public static interface ResponseResultListener<M extends Message, O> {
            void success(ResponseResult<M, O>[] results);

            void failed(ResponseResult<M, O>[] results);
        }

    }

}
