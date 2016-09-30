/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECWriteWorker.WriteEvent;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.LocalRPCResponseListener;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.ResponseResult;
import org.xtreemfs.osd.ec.StripeReconstructor.StripeReconstructorCallback;
import org.xtreemfs.osd.operations.ECWriteIntervalOperation;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_diffResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_intervalResponse;

/**
 * This class will act as a state machine used for writing.
 */
public class ECWriteWorker extends ECAbstractWorker<WriteEvent> {
    public static enum WriteEventType {
        FINISHED, FAILED, START_RECONSTRUCTION, RECONSTRUCTION_FINISHED
    }

    public final static class WriteEvent implements ECWorker.ECWorkerEvent {
        final WriteEventType type;
        final StripeState    stripeState;
        final boolean        needsStripeInterval;
        List<Interval>       stripeInterval;

        public WriteEvent(WriteEventType type, StripeState stripeState) {
            this.type = type;
            this.stripeState = stripeState;
            needsStripeInterval = false;
        }

        public WriteEvent(WriteEventType type, StripeState stripeState, boolean needsStripeInterval) {
            this.type = type;
            this.stripeState = stripeState;
            this.needsStripeInterval = needsStripeInterval;
        }


        @Override
        public boolean needsStripeInterval() {
            return needsStripeInterval;
        }

        @Override
        public long getStripeNo() {
            return (stripeState != null) ? stripeState.stripeNo : -1;
        }

        @Override
        public void setStripeInterval(List<Interval> stripeInterval) {
            this.stripeInterval = stripeInterval;
        }

        @Override
        public String toString() {
            return String.format("WriteEvent [type=%s, stripeNo=%d]", type, stripeState.stripeNo);
        }
    }

    final ECWorkerEventProcessor<WriteEvent> processor;

    // Given as argument
    final long                               opId;
    final int                                qw;
    final ReusableBuffer                     data;

    final Timer                              timer;
    final TimerTask                          timeoutTimer;
    final long                               timeoutMS;

    final StripeReconstructorCallback        stripeReconstructorCallback;

    // State Stuff
    // FIXME (jdillmann): Decide how the worker should be run and if sync is needed
    final StripeState[]                      stripeStates;
    AtomicInteger                            numStripesComplete;


    public ECWriteWorker(OSDRequestDispatcher master, FileCredentials fileCredentials, XLocations xloc, String fileId,
            long opId, StripingPolicyImpl sp, int qw, Interval reqInterval, List<Interval> commitIntervals,
            ReusableBuffer data, StageRequest request, long timeoutMS, ECMasterStage ecMaster) {
        super(master, ecMaster.osdClient, fileCredentials, xloc, fileId, sp, reqInterval, commitIntervals, request);
        this.processor = ecMaster;
        this.timer = ecMaster.timer;

        this.opId = opId;
        this.qw = qw;
        this.data = data;

        stripeStates = new StripeState[numStripes];
        numStripesComplete = new AtomicInteger(0);

        this.timeoutMS = timeoutMS;
        timeoutTimer = new TimerTask() {
            @Override
            public void run() {
                // ECWriteWorker.this.processor.signal(ECWriteWorker.this, WriteEventType.FAILED);
                ECWriteWorker.this.timeout();
            }
        };

        stripeReconstructorCallback = new StripeReconstructorCallback() {

            @Override
            public void success(long stripeNo) {
                ECWriteWorker.this.reconstructionFinished(stripeNo);
            }

            @Override
            public void failed(long stripeNo) {
                ECWriteWorker.this.failed(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                        "Request failed. StripeReconstruction could not be completed."));
            }

            @Override
            public void markForReconstruction(int osdNo) {
                ECWriteWorker.this.markForReconstruction(osdNo);
            }
        };
    }

    @Override
    public void processEvent(WriteEvent event) {
        switch (event.type) {
        case FINISHED:
            processFinished(event);
            break;
        case FAILED:
            processFailed(event);
            break;
        case START_RECONSTRUCTION:
            processStartReconstruction(event);
            break;
        case RECONSTRUCTION_FINISHED:
            processReconstructionFinished(event);
            break;
        }
    }

    @Override
    public void start() {
        timer.schedule(timeoutTimer, timeoutMS);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "ECWriteWorker: Start %s", this);
        }

        for (Stripe stripe : Stripe.getIterable(reqInterval, sp)) {
            // We will always write the version to the full stripe
            int numResponses = dataWidth;

            LocalRPCResponseHandler<xtreemfs_ec_write_intervalResponse, Long> handler = new LocalRPCResponseHandler<xtreemfs_ec_write_intervalResponse, Long>(
                    numResponses, new LocalRPCResponseListener<xtreemfs_ec_write_intervalResponse, Long>() {
                        @Override
                        public void responseAvailable(ResponseResult<xtreemfs_ec_write_intervalResponse, Long> result,
                                int numResponses, int numErrors, int numQuickFail) {
                            handleDataResponse(result, numResponses, numErrors, numQuickFail);
                        }
                    });
            
            StripeState stripeState = new StripeState(stripe.getStripeNo(), stripe.getStripeInterval());
            stripeStates[stripe.getRelIdx()] = stripeState;

            for (long objNo = stripe.getFirstObj(); objNo <= stripe.getLastObj(); objNo++) {

                boolean hasData = false;
                ReusableBuffer reqData = null;
                // relative offset to the current object
                int objOffset = 0;

                if (objNo >= stripe.getFirstObjWithData() && objNo <= stripe.getLastObjWithData()) {
                    hasData = true;
                    objOffset = getObjOffset(objNo);
                    // length of the data to write off the current object
                    int objLength = getObjLength(objNo);
                    // relative offset to the data buffer
                    int bufOffset = (objNo == firstObjNo) ? 0
                            : ECHelper.safeLongToInt(sp.getObjectStartOffset(objNo) - reqInterval.getOpStart());

                    reqData = data.createViewBuffer();
                    reqData.range(bufOffset, objLength);
                }

                int osdNo = sp.getOSDforObject(objNo);
                stripeState.responseStates[osdNo] = hasData ? ResponseState.REQ_DATA : ResponseState.REQ_INTERVAL;

                if (osdNo == localOsdNo) {
                    // make local request
                    handler.addLocal(objNo, reqData);
                    ECWriteIntervalOperation writeOp = (ECWriteIntervalOperation) master
                            .getOperation(ECWriteIntervalOperation.PROC_ID);
                    writeOp.startLocalRequest(fileId, sp, opId, hasData, objNo, objOffset,
                            ProtoInterval.toProto(stripe.getStripeInterval()), commitIntervalMsgs, reqData,
                            fileCredentials, xloc, handler);

                } else {
                    try {
                        ServiceUUID server = getRemote(osdNo);
                        // FIXME (jdillmann): Does write free the buffer?
                        // FIXME (jdillmann): which op id should be used?
                        RPCResponse<xtreemfs_ec_write_intervalResponse> rpcResponse = osdClient
                                .xtreemfs_ec_write_interval(server.getAddress(), RPCAuthentication.authNone,
                                        RPCAuthentication.userService, fileCredentials, fileId, opId, hasData, objNo,
                                        objOffset, ProtoInterval.toProto(stripe.getStripeInterval()),
                                        commitIntervalMsgs, reqData);
                        handler.addRemote(rpcResponse, objNo);

                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_WARN, this, ex);
                        failed(ErrorUtils.getInternalServerError(ex));
                        return;
                    }
                }

            }
        }
    }
    

    private void handleDataResponse(ResponseResult<xtreemfs_ec_write_intervalResponse, Long> result, int numResponses,
            int numErrors, int numQuickFail) {
        if (finishedSignaled.get()) {
            return;
        }
        registerActiveHandler();

        long objNo = result.getMappedObject();

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = !objFailed && result.getResult().getNeedsReconstruction();

        long stripeNo = sp.getRow(objNo);
        StripeState stripeState = getStripeStateForStripe(stripeNo);

        int osdNo = sp.getOSDforObject(objNo);

        if (objFailed || needsReconstruction) {

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                        "ECWriteWorker: OSD=%d [data] failed [fileId=%s, interval=%s, needsReconstruction=%s, error=%s]",
                        osdNo, fileId, reqInterval, needsReconstruction, result.getError());
            }

            if (needsReconstruction) {
                markForReconstruction(osdNo);
            }

            if (stripeState.markFailed(osdNo, needsReconstruction)) {
                failed(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                        "Request failed. Could not write enough chunks because too many OSDs did fail. Policy requires "
                                + this.qw + "OSDs to be written, only " + stripeState.acks + " succeded."));
            } else {
                // If this device would have been written, the data has to be reconstructed.
                if (osdNo >= stripeState.firstOSDWithData && osdNo <= stripeState.lastOSDWithData) {
                    startReconstruction(stripeState);
                }
            }

        } else {
            if (stripeState.markSuccess(osdNo)) {
                stripeComplete(stripeState);
            }
        }
       
        deregisterActiveHandler();
    }

    void handleParityResponse(xtreemfs_ec_write_diffResponse response) {
        if (finishedSignaled.get()) {
            return;
        }
        registerActiveHandler();

        boolean objFailed = response.hasError();
        boolean needsReconstruction = !objFailed && response.getNeedsReconstruction();

        long stripeNo = response.getStripeNumber();
        StripeState stripeState = getStripeStateForStripe(stripeNo);

        int osdNo = response.getOsdNumber();

        if (objFailed || needsReconstruction) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                        "ECWriteWorker: OSD=%d [parity] failed [fileId=%s, interval=%s, needsReconstruction=%s, error=%s]",
                        osdNo, fileId, reqInterval, needsReconstruction,
                        response.hasError() ? response.getError() : "");
            }

            if (needsReconstruction) {
                markForReconstruction(osdNo);
            }

            if (stripeState.markFailed(osdNo, needsReconstruction)) {
                failed(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                        "Request failed. Could not write enough chunks because too many OSDs did fail. Policy requires "
                                + this.qw + "OSDs to be written, only " + stripeState.acks + " succeded."));
            }
        } else {
            if (stripeState.markSuccess(osdNo)) {
                stripeComplete(stripeState);
            }
        }

        deregisterActiveHandler();
    }

    private void timeout() {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    "Request failed due to a timeout.");
            processor.signal(this, new WriteEvent(WriteEventType.FAILED, null));
        }
    }

    private void stripeComplete(StripeState stripeState) {
        // Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "ECWriteWorker: Completed Stripe %s",
        // stripeState);

        // TODO (jdillmann): Decide when to ping the file. Pinging too often (each Chunk?) could reduce performance.
        master.getPreprocStage().pingFile(fileId);

        int curStripesComplete = numStripesComplete.incrementAndGet();
        if (curStripesComplete == numStripes) {
            finished();
        }
    }

    private void finished() {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            processor.signal(this, new WriteEvent(WriteEventType.FINISHED, null));
        }
    }

    private void processFinished(WriteEvent event) {
        if (finished) {
            return;
        }
        waitForActiveHandlers();

        finished = true;
        freeBuffers();
    }

    @Override
    public void abort() {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "ECWriteWorker: Aborted from ECMasterStage.");
            processFailed(new WriteEvent(WriteEventType.FAILED, null));
        }
    }

    @Override
    public void abort(WriteEvent event) {
        timeoutTimer.cancel();
        finishedSignaled.set(true);

        // Mark the Worker as failed and clean everything up
        error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                "ECWriteWorker: Aborted from ECMasterStage.");
        processFailed(event);

        // We still have to process the event to clear possible buffers
        processEvent(event);
    }

    private void failed(ErrorResponse error) {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            this.error = error;
            processor.signal(this, new WriteEvent(WriteEventType.FAILED, null));
        }
    }

    private void processFailed(WriteEvent event) {
        if (finished) {
            return;
        }
        waitForActiveHandlers();
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "ECReadWorker: Failure [OSD=%s, fileId=%s, Interval=%s]\n%s", localOsdNo, fileId, reqInterval,
                    Arrays.toString(stripeStates));
        }

        finished = true;
        failed = true;

        for (StripeState stripeState : stripeStates) {
            stripeState.abort();
        }

        freeBuffers();
    }

    private void freeBuffers() {
        BufferPool.free(data);

        for (StripeState stripeState : stripeStates) {
            stripeState.freeBuffers();
        }
    }

    private void startReconstruction(StripeState stripeState) {
        processor.signal(this, new WriteEvent(WriteEventType.START_RECONSTRUCTION, stripeState, true));
    }

    private void processStartReconstruction(WriteEvent event) {
        if (finishedSignaled.get()) {
            return;
        }
        assert (event.stripeInterval != null);

        StripeState stripeState = event.stripeState;
        synchronized (stripeState) {
            if (stripeState.reconstructor == null) {
                // This is the first chunk that failed => start the reconstruction
                StripeReconstructor reconstructor = new StripeReconstructor(master, fileCredentials, xloc, fileId,
                        stripeState.stripeNo, sp, event.stripeInterval, osdClient, stripeReconstructorCallback);
                stripeState.markForReconstruction(reconstructor);
                reconstructor.start();

            } else if (stripeState.reconstructor.isComplete()) {
                // The reconstruction did already finish => send diff for failed data nodes
                sendDiffsForFailedChunks(stripeState);
            }
        }
    }

    private void reconstructionFinished(long stripeNo) {
        StripeState stripeState = getStripeStateForStripe(stripeNo);
        processor.signal(this, new WriteEvent(WriteEventType.RECONSTRUCTION_FINISHED, stripeState));
    }

    private void processReconstructionFinished(WriteEvent event) {
        if (finishedSignaled.get()) {
            return;
        }

        StripeState stripeState = event.stripeState;
        sendDiffsForFailedChunks(stripeState);
    }

    private void sendDiffsForFailedChunks(StripeState stripeState) {
        long stripeNo = stripeState.stripeNo;
        Interval stripeInterval = stripeState.interval;
        StripeReconstructor reconstructor = stripeState.reconstructor;

        try {
            reconstructor.decode(false);
        } catch (XtreemFSException ex) {
            failed(ErrorUtils.getInternalServerError(ex));
            return;
        }

        // long firstStripeObjNo = stripeNo * dataWidth;
        // long lastStripeObjNo = (stripeNo + 1) * dataWidth - 1;

        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstStripeObjWithData = sp.getObjectNoForOffset(stripeInterval.getStart());
        long lastStripeObjWithData = sp.getObjectNoForOffset(stripeInterval.getEnd() - 1);

        for (long objNo = firstStripeObjWithData; objNo <= lastStripeObjWithData; objNo++) {

            int osdNo = sp.getOSDforObject(objNo);

            if (!stripeState.hasFailed(osdNo)) {
                continue;
            }

            // relative offset to the current object
            int objOffset = getObjOffset(objNo);
            // length of the data to write off the current object
            int objLength = getObjLength(objNo);
            // relative offset to the data buffer
            int bufOffset = (objNo == firstObjNo) ? 0
                    : ECHelper.safeLongToInt(sp.getObjectStartOffset(objNo) - opStart);

            ReusableBuffer reqData = data.createViewBuffer();
            reqData.range(bufOffset, objLength);

            ReusableBuffer prevData = reconstructor.getObject(osdNo);
            prevData.range(objOffset, objLength);

            ReusableBuffer diff = BufferPool.allocate(objLength);

            ECHelper.xor(diff, reqData, prevData);
            diff.flip();
            BufferPool.free(reqData);
            BufferPool.free(prevData);

            long diffStart = sp.getObjectStartOffset(objNo) + objOffset;
            long diffEnd = diffStart + objLength;
            Interval diffInterval = new ProtoInterval(diffStart, diffEnd, stripeInterval.getVersion(),
                    stripeInterval.getId(), stripeInterval.getOpStart(), stripeInterval.getOpEnd());

            try {
                for (int parityOsdNo = dataWidth; parityOsdNo < stripeWidth; parityOsdNo++) {

                    if (stripeState.hasFailed(parityOsdNo)) {
                        // Parity OSD did already fail, don't send diff again.
                        continue;
                    }

                    synchronized (stripeState) {
                        stripeState.responseStates[parityOsdNo] = ResponseState.REQ_DIFF;
                        stripeState.responseStates[osdNo] = ResponseState.RECONSTRUCTED;
                    }

                    ServiceUUID parityOSD = getRemote(parityOsdNo);
                    try {
                        @SuppressWarnings("unchecked")
                        RPCResponse<emptyResponse> response = osdClient.xtreemfs_ec_write_diff(parityOSD.getAddress(),
                                RPCAuthentication.authNone, RPCAuthentication.userService, fileCredentials, fileId,
                                opId, objNo, objOffset, ProtoInterval.toProto(diffInterval),
                                ProtoInterval.toProto(stripeInterval), commitIntervalMsgs, diff.createViewBuffer());
                        response.registerListener(ECHelper.emptyResponseListener);
                    } catch (IOException ex) {
                        // TODO (jdillmann): Maybe mark parityOSD as failed
                        Logging.logError(Logging.LEVEL_WARN, this, ex);
                    }

                }
            } finally {
                BufferPool.free(diff);
            }
        }
    }

    StripeState getStripeStateForObj(long objNo) {
        return getStripeStateForStripe(sp.getRow(objNo));
    }

    StripeState getStripeStateForStripe(long stripeNo) {
        return stripeStates[getStripeIdx(stripeNo)];
    }

    @Override
    public Object getResult() {
        return null;
    }

    @Override
    public TYPE getType() {
        return TYPE.WRITE;
    }

    enum ResponseState {
        NONE, 
        REQ_INTERVAL, REQ_DATA, REQ_DIFF,
        FAILED, NEEDS_RECONSTRUCTION,
        SUCCESS, RECONSTRUCTED
    }

    private class StripeState {
        final long            stripeNo;
        final Interval        interval;
        final ResponseState[] responseStates;

        final int             firstOSDWithData;
        final int             lastOSDWithData;


        int                   acks           = 0;
        int                   nacks          = 0;
        boolean               finished       = false;
        StripeReconstructor   reconstructor  = null;

        public StripeState(long stripeNo, Interval interval) {
            this.stripeNo = stripeNo;
            this.interval = interval;

            firstOSDWithData = sp.getOSDforOffset(interval.getStart());
            lastOSDWithData = sp.getOSDforOffset(interval.getEnd() - 1);

            responseStates = new ResponseState[stripeWidth];
            for (int i = 0; i < stripeWidth; i++) {
                responseStates[i] = ResponseState.NONE;
            }

        }

        synchronized boolean markFailed(int osdPos, boolean needsReconstruction) {
            if (finished) {
                return false;
            }
            nacks++;
            responseStates[osdPos] = needsReconstruction ? ResponseState.NEEDS_RECONSTRUCTION : ResponseState.FAILED;

            if (stripeWidth - nacks < qw) {
                // we can't fullfill the write quorum anymore
                finished = true;
                return true;
            }

            return false;
        }

        synchronized boolean markSuccess(int osdPos) {
            if (finished) {
                return false;
            }

            acks++;

            responseStates[osdPos] = ResponseState.SUCCESS;
            if (acks >= qw) {
                return true;
            }

            return false;
        }


        synchronized void markForReconstruction(StripeReconstructor reconstructor) {
            this.reconstructor = reconstructor;

            for (int osdNo = 0; osdNo < dataWidth; osdNo++) {
                if (hasFailed(osdNo)) {
                    reconstructor.markFailed(osdNo);
                }
            }
        }

        synchronized boolean hasFailed(int osdNo) {
            return (responseStates[osdNo] == ResponseState.FAILED
                    || responseStates[osdNo] == ResponseState.NEEDS_RECONSTRUCTION);
        }

        synchronized void abort() {
            finished = true;

            // Abort any reconstruction
            if (reconstructor != null) {
                reconstructor.abort();
            }

            // Free the buffers
            freeBuffers();
        }

        synchronized void freeBuffers() {
            if (reconstructor != null) {
                reconstructor.abort();
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                    "StripeState [stripeNo=%s, interval=%s, acks=%s, nacks=%s, finished=%s, reconstruction=%s]",
                    stripeNo, interval, acks, nacks, finished, (reconstructor != null));
        }


    }

}
