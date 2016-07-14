/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
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

    public final static class ReadEvent {
        final ReadEventType type;
        final StripeState   stripeState;

        public ReadEvent(ReadEventType type, StripeState stripeState) {
            this.type = type;
            this.stripeState = stripeState;
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
    // FIXME (jdillmann): Decide how the worker should be run and if sync is needed
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
                ECReadWorker.this.failed(stripeNo);
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

        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstStripeNo = sp.getRow(firstObjNo);
        long lastStripeNo = sp.getRow(lastObjNo);

        for (int stripeIdx = 0; stripeIdx < numStripes; stripeIdx++) {
            long stripeNo = firstStripeNo + stripeIdx;

            long firstStripeObjNo = stripeNo * dataWidth;
            long lastStripeObjNo = (stripeNo + 1) * dataWidth - 1;

            long firstObjWithData = Math.max(firstStripeObjNo, firstObjNo);
            long lastObjWithData = Math.min(lastStripeObjNo, lastObjNo);

            long stripeDataStart = Math.max(sp.getObjectStartOffset(firstObjWithData), opStart);
            long stripeDataEnd = Math.min(sp.getObjectEndOffset(lastObjWithData) + 1, opEnd);

            ProtoInterval stripeInterval = new ProtoInterval(stripeDataStart, stripeDataEnd, reqInterval.getVersion(),
                    reqInterval.getId(), opStart, opEnd);

            // boolean localIsParity = localOsdNo > dataWidth;
            int numResponses = ECHelper.safeLongToInt(lastObjWithData - firstObjWithData + 1);

            LocalRPCResponseHandler<xtreemfs_ec_readResponse, ChunkState> handler = new LocalRPCResponseHandler<xtreemfs_ec_readResponse, ChunkState>(
                    numResponses, new LocalRPCResponseListener<xtreemfs_ec_readResponse, ChunkState>() {
                        @Override
                        public void responseAvailable(ResponseResult<xtreemfs_ec_readResponse, ChunkState> result,
                                int numResponses, int numErrors, int numQuickFail) {
                            handleDataResponse(result, numResponses, numErrors, numQuickFail);
                        }
                    });


            StripeState stripeState = new StripeState(stripeNo, stripeInterval);
            stripeStates[stripeIdx] = stripeState;

            for (long objNo = firstObjWithData; objNo <= lastObjWithData; objNo++) {
                int osdNo = sp.getOSDforObject(objNo);

                // ReusableBuffer reqData = null;
                // relative offset to the current object
                int objOffset = getObjOffset(objNo);
                // length of the data to read from the current object
                int objLength = getObjLength(objNo);

                boolean partial = (objLength < chunkSize);
                ChunkState chunkState = new ChunkState(osdNo, objNo, stripeNo, partial, ResponseState.REQUESTED);
                stripeState.chunkStates[osdNo] = chunkState;

                if (osdNo == localOsdNo) {
                    // make local request
                    handler.addLocal(chunkState);
                    ECReadOperation readOp = (ECReadOperation) master.getOperation(ECReadOperation.PROC_ID);
                    readOp.startLocalRequest(fileId, sp, objNo, objOffset, objLength, commitIntervalMsgs, false,
                            handler);
                } else {
                    try {
                        ServiceUUID server = getRemote(osdNo);
                        RPCResponse<xtreemfs_ec_readResponse> rpcResponse = osdClient.xtreemfs_ec_read(
                                server.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                fileCredentials, fileId, objNo, objOffset, objLength, commitIntervalMsgs, false);
                        handler.addRemote(rpcResponse, chunkState);

                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_WARN, this, ex);
                        failed(stripeState);
                        return;
                    }
                }

            }

            for (int r = 0; r < parityWidth; r++) {
                int osdNo = dataWidth + r;
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
            if (stripeState.isInReconstruction() && chunkState.partial) {
                // Ignore
                Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                        "Ignored response from data device %d for object %d because a full object was requested due to reconstruction",
                        chunkState.osdNo, chunkState.objNo);
                BufferPool.free(result.getData());
                deregisterActiveHandler();
                return;
            }

            if (objFailed || needsReconstruction) {
                boolean stripeFailed = stripeState.markFailed(chunkState, needsReconstruction);
                if (stripeFailed) {
                    failed(stripeState);
                } else {
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
        processor.signal(this, new ReadEvent(ReadEventType.START_RECONSTRUCTION, stripeState));
    }

    private void processStartReconstruction(ReadEvent event) {
        if (finishedSignaled.get()) {
            return;
        }

        StripeState stripeState = event.stripeState;
        StripeReconstructor reconstructor = new StripeReconstructor(master, fileCredentials, xloc, fileId,
                stripeState.stripeNo, sp, reqInterval, commitIntervalMsgs, osdClient, stripeReconstructorCallback);
        stripeState.markForReconstruction(reconstructor);
        reconstructor.start();
        
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
        stripeState.reconstructor.decode();

        // absolute start and end to the whole file range
        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstStripeObjWithData = sp.getObjectNoForOffset(stripeState.interval.getStart());
        long lastStripeObjWithData = sp.getObjectNoForOffset(stripeState.interval.getEnd() - 1);

        synchronized (stripeState) {
            for (long objNo = firstStripeObjWithData; objNo <= lastStripeObjWithData; objNo++) {
                int osdNo = sp.getOSDforObject(objNo);
                ChunkState chunkState = stripeState.chunkStates[osdNo];

                if (chunkState.state == ResponseState.SUCCESS) {
                    continue;
                }

                // relative offset to the current object
                int objOffset = getObjOffset(objNo);
                // length of the data to read from the current object
                int objLength = getObjLength(objNo);

                ReusableBuffer reconBuffer = reconstructor.getObject(osdNo);
                reconBuffer.range(objOffset, objLength);
                stripeState.reconstructedAvailable(chunkState, reconBuffer);
            }

            assert (stripeState.isComplete()) : "Stripe has to be complete after reconstruction.";
        }

        stripeComplete(stripeState);
    }

    private void stripeComplete(StripeState stripeState) {
        // processor.signal(this, new ReadEvent(ReadEventType.STRIPE_COMPLETE, stripeNo, osdPos));

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

                    // TODO (jdillmann): viewBuf not necessarily needed.
                    ReusableBuffer viewBuf = buf.createViewBuffer();
                    if (buf.capacity() > objLength) { // remaining
                        viewBuf.range(objOffset, objLength);
                    }

                    // TODO: This is probably very slow
                    data.put(buf);

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

    private void failed(long stripeNo) {
        failed(getStripeStateForStripe(stripeNo));
    }

    private void failed(StripeState stripeState) {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            processor.signal(this, new ReadEvent(ReadEventType.FAILED, stripeState));
        }
    }

    private void processFailed(ReadEvent event) {
        waitForActiveHandlers();

        BufferPool.free(data);
        for (StripeState stripeState : stripeStates) {
            // Clear response buffers
            for (ChunkState chunkState : stripeState.chunkStates) {
                if (chunkState != null) {
                    BufferPool.free(chunkState.buffer);
                }
            }
        }

        if (error == null && event.stripeState != null) {
            error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    String.format("Request failed. Could not read from stripe %d ([%d:%d]).",
                            event.stripeState.stripeNo,
                            event.stripeState.interval.getStart(),
                            event.stripeState.interval.getEnd()));
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
         * Sets the OSDs response to failed and returns true if the request can no longer be fullfilled <br>
         * Note: Needs monitor on chunkState
         * 
         * @param osdNum
         * @return
         */
        synchronized boolean markFailed(ChunkState chunkState, boolean needsReconstruction) {
            if (finished) {
                return false;
            }

            numFailures++;
            chunkState.state = needsReconstruction ? ResponseState.NEEDS_RECONSTRUCTION : ResponseState.FAILED;

            if (stripeWidth - numFailures < dataWidth) {
                // we can't fullfill the write quorum anymore
                finished = true;
                return true;
            }

            if (reconstruction && !chunkState.partial) {
                boolean reconstructionFailed = reconstructor.markFailed(chunkState.osdNo);
                if (reconstructionFailed) {
                    finished = true;
                    return true;
                }
            }

            return false;
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

            if (chunkState.osdNo >= firstOSDWithData && chunkState.osdNo <= lastOSDWithData) {
                numDataAvailable++;
            }

            chunkState.state = ResponseState.SUCCESS;
            chunkState.buffer = buf;
            chunkState.objData = response;

            if (numDataAvailable == numDataRequired) {
                finished = true;
                return true;
            }

            if (reconstruction && !chunkState.partial) {
                ReusableBuffer viewBuffer = ECHelper.zeroPad(chunkState.buffer, chunkSize);
                reconstructor.addResult(chunkState.osdNo, viewBuffer);

                // even if enough chunks for reconstruction are available the StripeState is not finished yet.
                return false;
            }

            return false;
        }

        synchronized boolean reconstructedAvailable(ChunkState chunkState, ReusableBuffer buf) {
            if (finished) {
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
    }

}
