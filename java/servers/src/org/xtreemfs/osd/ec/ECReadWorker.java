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
import org.xtreemfs.osd.ec.ECReadWorker.ReadEvent;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.LocalRPCResponseListener;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.ResponseResult;
import org.xtreemfs.osd.ec.StripeReconstructor.StripeReconstructorCallback;
import org.xtreemfs.osd.operations.ECReadOperation;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_readResponse;

/**
 * This class will act as a state machine used for reading.
 */
public class ECReadWorker extends ECAbstractWorker<ReadEvent> {
    public static enum ReadEventType {
        // STRIPE_COMPLETE, STRIPE_FAILED,
        FINISHED, FAILED, 
        START_RECONSTRUCTION, RECONSTRUCTION_FINISHED
    }

    public final static class ReadEvent implements ECWorker.ECWorkerEvent {
        final ReadEventType type;
        final StripeState   stripeState;
        final boolean       needsStripeInterval;
        List<Interval>      stripeInterval;

        public ReadEvent(ReadEventType type, StripeState stripeState) {
            this.type = type;
            this.stripeState = stripeState;
            needsStripeInterval = false;
        }

        public ReadEvent(ReadEventType type, StripeState stripeState, boolean needsStripeInterval) {
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
            return String.format("ReadEvent [type=%s, stripeNo=%d]", type, stripeState.stripeNo);
        }
    }

    final ECWorkerEventProcessor<ReadEvent> processor;

    // Given as argument
    final ReusableBuffer                    data;

    final Timer                             timer;
    final TimerTask                         timeoutTimer;
    final long                              timeoutMS;

    final StripeReconstructorCallback       stripeReconstructorCallback;

    // State Stuff
    final StripeState[]                     stripeStates;
    AtomicInteger                           numStripesComplete;
    ObjectInformation                       result;


    public ECReadWorker(OSDRequestDispatcher master, FileCredentials fileCredentials, XLocations xloc,
            String fileId, StripingPolicyImpl sp, Interval reqInterval, List<Interval> commitIntervals,
            ReusableBuffer data, StageRequest request, long timeoutMS, ECMasterStage ecMaster) {
        super(master, ecMaster.osdClient, fileCredentials, xloc, fileId, sp, reqInterval, commitIntervals, request);

        this.processor = ecMaster;
        this.timer = ecMaster.timer;

        this.data = data;

        stripeStates = new StripeState[numStripes];
        numStripesComplete = new AtomicInteger(0);
        result = null;

        this.timeoutMS = timeoutMS;
        timeoutTimer = new TimerTask() {
            @Override
            public void run() {
                // ECWriteWorker.this.processor.signal(ECWriteWorker.this, WriteEventType.FAILED);
                ECReadWorker.this.timeout();
            }
        };

        stripeReconstructorCallback = new StripeReconstructorCallback() {
            @Override
            public void success(long stripeNo) {
                ECReadWorker.this.reconstructionFinished(stripeNo);
            }

            @Override
            public void failed(long stripeNo) {
                ECReadWorker.this.failed(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                        "Request failed. StripeReconstruction could not be completed."));
            }

            @Override
            public void markForReconstruction(int osdNo) {
                ECReadWorker.this.markForReconstruction(osdNo);
            }
        };
    }

    @Override
    public void processEvent(ReadEvent event) {
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
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "ECReadWorker: Start %s", this);
        }

        for (Stripe stripe : Stripe.getIterable(reqInterval, sp)) {
            // boolean localIsParity = localOsdNo > dataWidth;
            int numResponses = ECHelper.safeLongToInt(stripe.getLastObjWithData() - stripe.getFirstObjWithData() + 1);

            LocalRPCResponseHandler<xtreemfs_ec_readResponse, ChunkState> handler = new LocalRPCResponseHandler<xtreemfs_ec_readResponse, ChunkState>(
                    numResponses, new LocalRPCResponseListener<xtreemfs_ec_readResponse, ChunkState>() {
                        @Override
                        public void responseAvailable(ResponseResult<xtreemfs_ec_readResponse, ChunkState> result,
                                int numResponses, int numErrors, int numQuickFail) {
                            handleDataResponse(result, numResponses, numErrors, numQuickFail);
                        }
                    });


            StripeState stripeState = new StripeState(stripe.getStripeNo(), stripe.getStripeInterval());
            stripeStates[stripe.getRelIdx()] = stripeState;

            for (long objNo = stripe.getFirstObjWithData(); objNo <= stripe.getLastObjWithData(); objNo++) {
                int osdNo = sp.getOSDforObject(objNo);

                // ReusableBuffer reqData = null;
                // relative offset to the current object
                int objOffset = getObjOffset(objNo);
                // length of the data to read from the current object
                int objLength = getObjLength(objNo);

                boolean partial = (objLength < chunkSize);
                ChunkState chunkState = new ChunkState(osdNo, objNo, stripe.getStripeNo(), partial,
                        ResponseState.REQUESTED);
                stripeState.chunkStates[osdNo] = chunkState;

                if (osdNo == localOsdNo) {
                    // make local request
                    handler.addLocal(chunkState);
                    ECReadOperation readOp = (ECReadOperation) master.getOperation(ECReadOperation.PROC_ID);
                    readOp.startLocalRequest(fileId, sp, objNo, objOffset, objLength, commitIntervalMsgs, handler);
                } else {
                    try {
                        ServiceUUID server = getRemote(osdNo);
                        RPCResponse<xtreemfs_ec_readResponse> rpcResponse = osdClient.xtreemfs_ec_read(
                                server.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                fileCredentials, fileId, objNo, objOffset, objLength, commitIntervalMsgs);
                        handler.addRemote(rpcResponse, chunkState);

                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_WARN, this, ex);
                        failed(ErrorUtils.getInternalServerError(ex));
                        return;
                    }
                }

            }

        }
    }


    private void handleDataResponse(ResponseResult<xtreemfs_ec_readResponse, ChunkState> result, int numResponses,
            int numErrors, int numQuickFail) {
        if (finishedSignaled.get()) {
            BufferPool.free(result.getData());
            return;
        }
        registerActiveHandler();

        ChunkState chunkState = result.getMappedObject();

        long stripeNo = chunkState.stripeNo;
        StripeState stripeState = getStripeStateForStripe(stripeNo);

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = !objFailed && result.getResult().getNeedsReconstruction();


        synchronized (stripeState) {
            if (objFailed || needsReconstruction) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                            "ECReadWorker: OSD=%d [data] failed [needsReconstruction=%s, error=%s]", chunkState.osdNo,
                            needsReconstruction, result.getError());
                }

                if (needsReconstruction) {
                    markForReconstruction(chunkState.osdNo);
                }

                stripeState.markFailed(chunkState, needsReconstruction);

                if (!stripeState.isInReconstruction()) {
                    startReconstruction(stripeState);
                }

            } else {
                boolean stripeComplete = stripeState.dataAvailable(chunkState, result.getData(),
                        result.getResult().getObjectData());

                if (stripeComplete) {
                    stripeComplete(stripeState);
                }
            }
        }

        deregisterActiveHandler();
    }

    private void startReconstruction(StripeState stripeState) {
        processor.signal(this, new ReadEvent(ReadEventType.START_RECONSTRUCTION, stripeState, true));
    }

    private void processStartReconstruction(ReadEvent event) {
        if (finishedSignaled.get()) {
            return;
        }
        assert (event.stripeInterval != null);


        StripeState stripeState = event.stripeState;
        synchronized (stripeState) {
            if (stripeState.isInReconstruction() || stripeState.hasFinished()) {
                // Skip re-starting the reconstruction
                return;
            }

            StripeReconstructor reconstructor = new StripeReconstructor(master, fileCredentials, xloc, fileId,
                    stripeState.stripeNo, sp, event.stripeInterval, osdClient, stripeReconstructorCallback);
            stripeState.markForReconstruction(reconstructor);
            reconstructor.start();
        }
        
        // FIXME (jdillmann): Only k chunks have to be fetched.
        // If we would have a better (non timeout) error detector we could just read one full chunk from a partial
        // data device, or a data or coding device not read yet. But since we have to wait for timeouts, the serial
        // execution could possibly take very long. Thus we immediately fetch the whole stripe, which results is
        // higher (maybe unnecessary) network bandwidth.
    }

    private void reconstructionFinished(long stripeNo) {
        StripeState stripeState = getStripeStateForStripe(stripeNo);
        processor.signal(this, new ReadEvent(ReadEventType.RECONSTRUCTION_FINISHED, stripeState));
    }

    private void processReconstructionFinished(ReadEvent event) {
        if (finishedSignaled.get()) {
            return;
        }

        StripeState stripeState = event.stripeState;
        StripeReconstructor reconstructor = stripeState.reconstructor;
        assert (reconstructor.isComplete());
        reconstructor.decode(false);

        // absolute start and end to the whole file range
        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstStripeObjWithData = sp.getObjectNoForOffset(stripeState.interval.getStart());
        long lastStripeObjWithData = sp.getObjectNoForOffset(stripeState.interval.getEnd() - 1);

        synchronized (stripeState) {
            if (stripeState.hasFinished()) {
                return;
            }

            for (long objNo = firstStripeObjWithData; objNo <= lastStripeObjWithData; objNo++) {
                int osdNo = sp.getOSDforObject(objNo);
                ChunkState chunkState = stripeState.chunkStates[osdNo];

                // relative offset to the current object
                int objOffset = getObjOffset(objNo);
                // length of the data to read from the current object
                int objLength = getObjLength(objNo);

                ReusableBuffer reconBuffer = reconstructor.getObject(osdNo);
                reconBuffer.range(objOffset, objLength);
                stripeState.reconstructedAvailable(chunkState, reconBuffer);
            }

            assert (stripeState.isComplete()) : "Stripe has to be complete after reconstruction.";
            stripeComplete(stripeState);
        }

    }

    private void stripeComplete(StripeState stripeState) {
        // Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "ECReadWorker: Completed Stripe %s", stripeState);

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
            processor.signal(this, new ReadEvent(ReadEventType.FINISHED, null));
        }
    }

    private void processFinished(ReadEvent event) {
        if (finished) {
            return;
        }
        waitForActiveHandlers();

        // absolute start and end to the whole file range
        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        data.position(0);
        for (StripeState stripeState : stripeStates) {

            long firstStripeObjWithData = sp.getObjectNoForOffset(stripeState.interval.getStart());
            long lastStripeObjWithData = sp.getObjectNoForOffset(stripeState.interval.getEnd() - 1);

            for (long objNo = firstStripeObjWithData; objNo <= lastStripeObjWithData; objNo++) {
                int osdNo = sp.getOSDforObject(objNo);

                ChunkState chunkState = stripeState.chunkStates[osdNo];
                assert (chunkState.state == ResponseState.SUCCESS
                        || chunkState.state == ResponseState.RECONSTRUCTED) : "chunkState was: " + chunkState.state;

                ObjectData objData = chunkState.objData;
                ReusableBuffer buf = chunkState.buffer;

                // Copy the buffer to the result
                if (buf != null) {
                    // relative offset to the current object
                    int objOffset = getObjOffset(objNo);
                    // length of the data to read from the current object
                    int objLength = getObjLength(objNo);

                    ReusableBuffer viewBuf = buf.createViewBuffer();
                    if (buf.capacity() > objLength) { // remaining
                        viewBuf.range(objOffset, objLength);
                    }

                    data.put(viewBuf);

                    BufferPool.free(viewBuf);
                }

                if (objData != null) {
                    for (int i = 0; i < objData.getZeroPadding(); i++) {
                        // TODO: This is probably very slow
                        data.put((byte) 0);
                    }
                }

            }

            // Clear response buffers
            for (ChunkState chunkState : stripeState.chunkStates) {
                if (chunkState != null) {
                    BufferPool.free(chunkState.buffer);
                }
            }
        }

        assert (!data.hasRemaining()) : "Error while reading and copying read responses";
        
        // TODO (jdillmann): Padding responses at the end could be merged and reflected int the objInfo
        data.position(0);
        result = new ObjectInformation(ObjectStatus.EXISTS, data, chunkSize);

        finished = true;
    }

    private void timeout() {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    "Request failed due to a timeout.");
            processor.signal(this, new ReadEvent(ReadEventType.FAILED, null));
        }
    }

    @Override
    public void abort() {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "ECReadWorker: Aborted from ECMasterStage.");
            processFailed(new ReadEvent(ReadEventType.FAILED, null));
        }
    }

    @Override
    public void abort(ReadEvent event) {
        timeoutTimer.cancel();
        finishedSignaled.set(true);

        // Mark the Worker as failed and clean everything up
        error = ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                "ECReadWorker: Aborted from ECMasterStage.");
        processFailed(event);

        // We still have to process the event to clear possible buffers
        processEvent(event);
    }

    private void failed(ErrorResponse error) {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            this.error = error;
            processor.signal(this, new ReadEvent(ReadEventType.FAILED, null));
        }
    }

    private void processFailed(ReadEvent event) {
        if (finished) {
            return;
        }
        timeoutTimer.cancel();
        waitForActiveHandlers();
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "ECReadWorker: Failure [OSD=%s, fileId=%s, Interval=%s]\n%s", localOsdNo, fileId, reqInterval,
                    Arrays.toString(stripeStates));
        }

        BufferPool.free(data);
        for (StripeState stripeState : stripeStates) {
            stripeState.abort();
        }

        finished = true;
        failed = true;
    }

    StripeState getStripeStateForObj(long objNo) {
        return getStripeStateForStripe(sp.getRow(objNo));
    }

    StripeState getStripeStateForStripe(long stripeNo) {
        return stripeStates[getStripeIdx(stripeNo)];
    }

    @Override
    public TYPE getType() {
        return TYPE.READ;
    }

    @Override
    public ObjectInformation getResult() {
        return result;
    }

    enum ResponseState {
        NONE, REQUESTED, REQ_RECONSTRUCTION,
        FAILED, NEEDS_RECONSTRUCTION,
        SUCCESS, RECONSTRUCTED;
    }

    final private static class ChunkState {
        final int      osdNo;
        final long     objNo;
        final long     stripeNo;
        boolean        partial;
        ResponseState  state;
        ReusableBuffer buffer;
        ObjectData     objData;

        public ChunkState(int osdNo, long objNo, long stripeNo, boolean partial, ResponseState state) {
            this.osdNo = osdNo;
            this.objNo = objNo;
            this.stripeNo = stripeNo;
            this.partial = partial;
            this.state = state;
        }

        public boolean hasFailed() {
            return (state == ResponseState.FAILED || state == ResponseState.NEEDS_RECONSTRUCTION);
        }

        @Override
        public String toString() {
            return String.format(
                    "ChunkState [osdNo=%s, objNo=%s, stripeNo=%s, partial=%s, state=%s]", 
                    osdNo, objNo, stripeNo, partial, state);
        }
    }

    private class StripeState {
        final long          stripeNo;
        final Interval      interval;
        final ChunkState[]  chunkStates;

        final int           firstOSDWithData;
        final int           lastOSDWithData;

        int                 numFailures          = 0;

        final int           numDataRequired;
        int                 numDataAvailable     = 0;

        boolean             finished             = false;
        boolean             reconstruction       = false;

        StripeReconstructor reconstructor        = null;

        public StripeState(long stripeNo, Interval interval) {
            this.stripeNo = stripeNo;
            this.interval = interval;
            chunkStates = new ChunkState[stripeWidth];

            firstOSDWithData = sp.getOSDforOffset(interval.getStart());
            lastOSDWithData = sp.getOSDforOffset(interval.getEnd() - 1);
            numDataRequired = lastOSDWithData - firstOSDWithData + 1;
        }

        synchronized void markForReconstruction(StripeReconstructor reconstructor) {
            assert (!reconstruction) : "Reconstruction may only started once.";
            reconstruction = true;
            
            this.reconstructor = reconstructor;
            
            for (int osdNo = 0; osdNo < stripeWidth; osdNo++) {
                ChunkState chunkState = chunkStates[osdNo];
                if (chunkState != null) {
                    switch(chunkState.state) {
                    case FAILED:
                    case NEEDS_RECONSTRUCTION:
                        reconstructor.markFailed(osdNo);
                        break;

                    case REQUESTED:
                        if (!chunkState.partial) {
                            reconstructor.markExternalRequest(osdNo);
                        }
                        break;

                    case SUCCESS:
                        if (!chunkState.partial) {
                            ReusableBuffer viewBuffer = ECHelper.zeroPad(chunkState.buffer, chunkSize);
                            reconstructor.addResult(osdNo, viewBuffer);
                        }
                        break;

                    case NONE:
                        chunkState.state = ResponseState.REQ_RECONSTRUCTION;
                        break;

                    case REQ_RECONSTRUCTION:
                    case RECONSTRUCTED:
                        Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                                "Invalid ChunkState %s while starting reconstruction.", chunkState.state);
                        break;
                    }
                } else {
                    long firstStripeObjNo = stripeNo * dataWidth;
                    long objNo = osdNo < dataWidth ? firstStripeObjNo + osdNo : stripeNo;
                    chunkStates[osdNo] = new ChunkState(osdNo, objNo, stripeNo, false,
                            ResponseState.REQ_RECONSTRUCTION);
                }
            }
        }

        synchronized boolean isInReconstruction() {
            return reconstruction;
        }

        synchronized boolean hasFinished() {
            return finished;
        }

        synchronized boolean isComplete() {
            return (numDataAvailable >= numDataRequired);
        }


        /**
         * Sets the OSDs response to failed
         */
        synchronized void markFailed(ChunkState chunkState, boolean needsReconstruction) {
            if (finished) {
                return;
            }

            if (reconstruction) {
                if (chunkState.partial) {
                    // Ignore
                    Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                            "Ignored response from data device %d for object %d because a full object was requested due to reconstruction",
                            chunkState.osdNo, chunkState.objNo);
                    return;
                }

                boolean reconstructionFailed = reconstructor.markFailed(chunkState.osdNo);
                if (reconstructionFailed) {
                    finished = true;
                }
                return;
            }

            numFailures++;
            chunkState.state = needsReconstruction ? ResponseState.NEEDS_RECONSTRUCTION : ResponseState.FAILED;
        }


        /**
         * Sets the OSDs result to success and returns true if all chunks are available. <br>
         * Note: Needs monitor on chunkState
         * 
         * @param osdPos
         * @param buf
         * @return
         */
        synchronized boolean dataAvailable(ChunkState chunkState, ReusableBuffer buf, ObjectData response) {
            if (finished) {
                BufferPool.free(buf);
                return false;
            }

            if (reconstruction) {
                if (chunkState.partial) {
                    // Ignore
                    Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                            "Ignored response from data device %d for object %d because a full object was requested due to reconstruction",
                            chunkState.osdNo, chunkState.objNo);
                    BufferPool.free(buf);
                    return false;
                }

                ReusableBuffer viewBuffer = ECHelper.zeroPad(buf, chunkSize);
                reconstructor.addResult(chunkState.osdNo, viewBuffer);
            }

            chunkState.state = ResponseState.SUCCESS;
            chunkState.buffer = buf;
            chunkState.objData = response;

            if (chunkState.osdNo >= firstOSDWithData && chunkState.osdNo <= lastOSDWithData) {
                numDataAvailable++;
            }

            // the stripe has to be finished by the reconstructor
            if (!reconstruction && numDataAvailable == numDataRequired) {
                finished = true;
                return true;
            }

            return false;
        }

        synchronized boolean reconstructedAvailable(ChunkState chunkState, ReusableBuffer buf) {
            if (finished) {
                BufferPool.free(buf);
                return false;
            }

            if (chunkState.state == ResponseState.SUCCESS) {
                BufferPool.free(buf);
                return false;
            }

            if (chunkState.osdNo >= firstOSDWithData && chunkState.osdNo <= lastOSDWithData) {
                numDataAvailable++;
            }

            chunkState.state = ResponseState.RECONSTRUCTED;
            BufferPool.free(chunkState.buffer);
            chunkState.buffer = buf;
            chunkState.objData = null;

            if (numDataAvailable == numDataRequired) {
                finished = true;
                return true;
            }

            return false;
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
            // Clear response buffers
            for (ChunkState chunkState : chunkStates) {
                if (chunkState != null) {
                    BufferPool.free(chunkState.buffer);
                    chunkState.buffer = null;
                }
            }

            // Clear the reconstruction buffers
            if (reconstructor != null) {
                reconstructor.freeBuffers();
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                    "StripeState [stripeNo=%s, interval=%s, numFailures=%s, numDataRequired=%s, numDataAvailable=%s, finished=%s, reconstruction=%s]",
                    stripeNo, interval, numFailures, numDataRequired, numDataAvailable, finished, reconstruction);
        }
    }

}
