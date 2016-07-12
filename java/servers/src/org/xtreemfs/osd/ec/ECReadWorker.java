/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.logging.Logging;
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
import org.xtreemfs.osd.operations.ECReadOperation;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_readResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import com.backblaze.erasure.ReedSolomon;

/**
 * This class will act as a state machine used for reading.
 */
public class ECReadWorker implements ECWorker<ReadEvent> {
    public static enum ReadEventType {
        // STRIPE_COMPLETE, STRIPE_FAILED,
        FINISHED, FAILED, START_RECONSTRUCTION
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
    final String                            fileId;
    // final long opId;
    final StripingPolicyImpl                sp;
    final Interval                          reqInterval;

    final List<Interval>                    commitIntervals;
    final List<IntervalMsg>                 commitIntervalMsgs;
    final StripeState[]                     stripeStates;
    final OSDServiceClient                  osdClient;
    // final List<ServiceUUID> remoteOSDs;
    final OSDRequestDispatcher              master;
    final FileCredentials                   fileCredentials;
    final XLocations                        xloc;
    final ReusableBuffer                    data;

    final Timer                             timer;
    final TimerTask                         timeoutTimer;
    final long                              timeoutMS;

    // Note: circular reference
    final StageRequest                      request;

    // Calculated
    final int                               localOsdNo;
    final int                               dataWidth;         // k
    final int                               parityWidth;       // m
    final int                               stripeWidth;       // n
    final int                               chunkSize;
    final int                               numStripes;

    final int                               firstObjOffset;
    final int                               firstObjLength;
    final int                               lastObjOffset;
    final int                               lastObjLength;

    // State Stuff
    // FIXME (jdillmann): Decide how the worker should be run and if sync is needed
    // int numStripesComplete;
    AtomicInteger                           numStripesComplete;

    boolean                                 finished;
    boolean                                 failed;
    volatile ErrorResponse                  error;
    ObjectInformation                       result;

    final AtomicBoolean                     finishedSignaled;
    final AtomicInteger                     activeHandlers;

    public ECReadWorker(OSDRequestDispatcher master, FileCredentials fileCredentials, XLocations xloc,
            String fileId, StripingPolicyImpl sp, Interval reqInterval, List<Interval> commitIntervals,
            ReusableBuffer data, StageRequest request, long timeoutMS, ECMasterStage ecMaster) {
        this.processor = ecMaster;
        this.timer = ecMaster.timer;
        this.osdClient = ecMaster.osdClient;

        this.fileId = fileId;
        this.sp = sp;
        this.reqInterval = reqInterval;
        this.commitIntervals = commitIntervals;
        this.data = data;
        this.master = master;
        this.fileCredentials = fileCredentials;
        this.xloc = xloc;
        this.request = request;

        commitIntervalMsgs = new ArrayList<IntervalMsg>(commitIntervals.size());
        for (Interval interval : commitIntervals) {
            commitIntervalMsgs.add(ProtoInterval.toProto(interval));
        }

        dataWidth = sp.getWidth();
        parityWidth = sp.getParityWidth();
        stripeWidth = sp.getWidth() + sp.getParityWidth();
        chunkSize = sp.getPolicy().getStripeSize() * 1024;
        localOsdNo = sp.getRelativeOSDPosition();

        numStripesComplete = new AtomicInteger(0);

        finished = false;
        failed = false;
        error = null;
        result = null;

        // absolute start and end to the whole file range
        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstObjNo = sp.getObjectNoForOffset(opStart);
        long lastObjNo = sp.getObjectNoForOffset(opEnd - 1); // interval.end is exclusive

        long firstStripeNo = sp.getRow(firstObjNo);
        long lastStripeNo = sp.getRow(lastObjNo);

        int firstObjOffset = ECHelper.safeLongToInt(opStart - sp.getObjectStartOffset(firstObjNo));
        int firstObjLength = chunkSize - firstObjOffset;
        int lastObjOffset = 0;
        int lastObjLength = chunkSize - ECHelper.safeLongToInt(sp.getObjectEndOffset(lastObjNo) - (opEnd - 1));
        if (firstObjNo == lastObjNo) {
            lastObjOffset = firstObjOffset;
            firstObjLength = chunkSize - (chunkSize - firstObjLength) - (chunkSize - lastObjLength);
            lastObjLength = firstObjLength;
        }

        this.firstObjOffset = firstObjOffset;
        this.firstObjLength = firstObjLength;
        this.lastObjOffset = lastObjOffset;
        this.lastObjLength = lastObjLength;



        numStripes = ECHelper.safeLongToInt(lastStripeNo - firstStripeNo + 1);
        stripeStates = new StripeState[numStripes];

        finishedSignaled = new AtomicBoolean(false);
        activeHandlers = new AtomicInteger(0);

        this.timeoutMS = timeoutMS;
        timeoutTimer = new TimerTask() {
            @Override
            public void run() {
                // ECWriteWorker.this.processor.signal(ECWriteWorker.this, WriteEventType.FAILED);
                ECReadWorker.this.timeout();
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
        }
    }

    @Override
    public void start() {
        timer.schedule(timeoutTimer, timeoutMS);

        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstObjNo = sp.getObjectNoForOffset(opStart);
        long lastObjNo = sp.getObjectNoForOffset(opEnd - 1); // interval.end is exclusive

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

                // ReusableBuffer reqData = null;
                // relative offset to the current object
                int objOffset;
                // length of the data to read from the current object
                int objLength;
                if (objNo == firstObjNo) {
                    objOffset = firstObjOffset;
                    objLength = firstObjLength;
                } else if (objNo == lastObjNo) {
                    objOffset = lastObjOffset;
                    objLength = lastObjLength;
                } else {
                    objOffset = 0;
                    objLength = chunkSize;
                }

                int osdNo = sp.getOSDforObject(objNo);

                boolean partial = (objLength < chunkSize);
                ChunkState chunkState = new ChunkState(osdNo, objNo, stripeNo, partial, ResponseState.REQUESTED);
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
        activeHandlers.incrementAndGet();

        ChunkState chunkState = result.getMappedObject();

        long stripeNo = chunkState.stripeNo;
        StripeState stripeState = getStripeStateForStripe(stripeNo);

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = !objFailed && result.getResult().getNeedsReconstruction();


        synchronized (stripeState) {
            if (stripeState.isInReconstruction() && chunkState.partial) {
                // Ignore
                Logging.logMessage(Logging.LEVEL_INFO, this,
                        "Ignored response from data device %d for object %d because a full object was requested due to reconstruction",
                        chunkState.osdNo, chunkState.objNo);
                BufferPool.free(result.getData());
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


        if (activeHandlers.decrementAndGet() == 0) {
            synchronized (activeHandlers) {
                activeHandlers.notifyAll();
            }
        }
    }



    private void waitForActiveHandlers() {
        synchronized (activeHandlers) {
            while (activeHandlers.get() > 0) {
                try {
                    activeHandlers.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
    }

    private void startReconstruction(StripeState stripeState) {
        processor.signal(this, new ReadEvent(ReadEventType.START_RECONSTRUCTION, stripeState));
    }

    private void processStartReconstruction(ReadEvent event) {
        StripeState stripeState = event.stripeState;
        synchronized (stripeState) {
            stripeState.markForReconstruction();

            // FIXME (jdillmann): Only k chunks have to be fetched.
            // If we would have a better (non timeout) error detector we could just read one full chunk from a partial
            // data device, or a data or coding device not read yet. But since we have to wait for timeouts, the serial
            // execution could possibly take very long. Thus we immediately fetch the whole stripe, which results is
            // higher (maybe unnecessary) network bandwidth.

            long stripeNo = stripeState.stripeNo;
            long firstStripeObj = stripeNo * dataWidth;
            int numResponses = parityWidth;

            for (int osdNo = 0; osdNo < dataWidth; osdNo++) {
                ChunkState chunkState = stripeState.chunkStates[osdNo];
                if (chunkState.state == ResponseState.SUCCESS && chunkState.partial) {
                    // Remove partial results from the result
                    stripeState.numDataAvailable--;
                    BufferPool.free(chunkState.buffer);
                }
                
                if (chunkState.state == ResponseState.NONE || chunkState.partial) {
                    numResponses++;
                }
            }

            LocalRPCResponseHandler<xtreemfs_ec_readResponse, ChunkState> handler = new LocalRPCResponseHandler<xtreemfs_ec_readResponse, ChunkState>(
                    numResponses, new LocalRPCResponseListener<xtreemfs_ec_readResponse, ChunkState>() {
                        @Override
                        public void responseAvailable(ResponseResult<xtreemfs_ec_readResponse, ChunkState> result,
                                int numResponses, int numErrors, int numQuickFail) {
                            handleReconstructionResponse(result, numResponses, numErrors, numQuickFail);
                        }
                    });


            int objOffset = 0;
            int objLength = chunkSize;

            for (int osdNo = 0; osdNo < stripeWidth; osdNo++) {
                // FIXME (jdillmann): Maybe send stripe no
                long objNo = osdNo < dataWidth ? firstStripeObj + osdNo : firstStripeObj;

                ChunkState chunkState = stripeState.chunkStates[osdNo];
                if (chunkState == null || chunkState.state == ResponseState.NONE || chunkState.partial) {

                    chunkState = new ChunkState(osdNo, objNo, stripeNo, false, ResponseState.REQ_RECOVERY);
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

                            boolean stripeFailed = stripeState.markFailed(chunkState, false);
                            if (stripeFailed) {
                                failed(stripeState);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleReconstructionResponse(ResponseResult<xtreemfs_ec_readResponse, ChunkState> result,
            int numResponses, int numErrors, int numQuickFail) {
        if (finishedSignaled.get()) {
            BufferPool.free(result.getData());
            return;
        }
        activeHandlers.incrementAndGet();

        ChunkState chunkState = result.getMappedObject();

        long stripeNo = chunkState.stripeNo;
        StripeState stripeState = getStripeStateForStripe(stripeNo);

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = !objFailed && result.getResult().getNeedsReconstruction();


        if (objFailed || needsReconstruction) {
            boolean stripeFailed = stripeState.markFailed(chunkState, needsReconstruction);
            if (stripeFailed) {
                failed(stripeState);
            }
        } else {
            boolean stripeComplete = stripeState.dataAvailable(chunkState, result.getData(),
                    result.getResult().getObjectData());

            if (stripeComplete) {
                stripeComplete(stripeState);
            }
        }


        if (activeHandlers.decrementAndGet() == 0) {
            synchronized (activeHandlers) {
                activeHandlers.notifyAll();
            }
        }
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

        long firstObjNo = sp.getObjectNoForOffset(opStart);
        long lastObjNo = sp.getObjectNoForOffset(opEnd - 1); // interval.end is exclusive

        data.position(0);
        for (StripeState stripeState : stripeStates) {
            // Reconstruct if necessary
            if (stripeState.isInReconstruction()) {
                ReedSolomon codec = ReedSolomon.create(dataWidth, parityWidth);
                boolean[] present = new boolean[stripeWidth];
                ByteBuffer[] shards = new ByteBuffer[stripeWidth];
                                
                for (int i = 0; i < stripeWidth; i++) {
                    ChunkState chunkState = stripeState.chunkStates[i];
                    
                    if (chunkState.state == ResponseState.SUCCESS) {
                        present[i] = true;
                        shards[i] = chunkState.buffer.getBuffer().slice();
                    } else {
                        present[i] = false;
                        chunkState.buffer = BufferPool.allocate(chunkSize);
                        chunkState.state = ResponseState.RECONSTRUCTED;
                        shards[i] = chunkState.buffer.getBuffer().slice();
                    }
                }

                // FIXME (jdillmann): optimize / use buffer
                codec.decodeMissing(shards, present, 0, chunkSize, false);


                // boolean[] present = new boolean[stripeWidth];
                // byte[][] shards = new byte[stripeWidth][chunkSize];
                // for (int i = 0; i < stripeWidth; i++) {
                // ChunkState chunkState = stripeState.chunkStates[i];
                // if (chunkState != null && chunkState.state == ResponseState.SUCCESS) {
                // present[i] = true;
                // ReusableBuffer viewBuf = chunkState.buffer.createViewBuffer();
                // viewBuf.get(shards[i]);
                // BufferPool.free(viewBuf);
                // } else {
                // present[i] = false;
                // }
                // }
                //
                // // FIXME (jdillmann): optimize / use buffer
                // codec.decodeMissing(shards, present, 0, chunkSize);
                // for (int i = 0; i < dataWidth; i++) {
                // ChunkState chunkState = stripeState.chunkStates[i];
                // if (chunkState != null && chunkState.state == ResponseState.FAILED
                // || chunkState.state == ResponseState.NEEDS_RECONSTRUCTION) {
                // ReusableBuffer recoveredBuf = ReusableBuffer.wrap(shards[i]);
                // chunkState.state = ResponseState.RECONSTRUCTED;
                // chunkState.buffer = recoveredBuf;
                // }
                // }
            }

            long firstObjWithData = sp.getObjectNoForOffset(stripeState.interval.getStart());

            for (int osdNo = stripeState.firstOSDWithData; osdNo <= stripeState.lastOSDWithData; osdNo++) {
                ChunkState chunkState = stripeState.chunkStates[osdNo];
                assert (chunkState.state == ResponseState.SUCCESS || chunkState.state == ResponseState.RECONSTRUCTED);

                long objNo = firstObjWithData + osdNo;

                ObjectData objData = chunkState.objData;
                ReusableBuffer buf = chunkState.buffer;

                // Copy the buffer to the result
                if (buf != null) {
                    // relative offset to the current object
                    int objOffset;
                    // length of the data to read from the current object
                    int objLength;
                    if (objNo == firstObjNo) {
                        objOffset = firstObjOffset;
                        objLength = firstObjLength;
                    } else if (objNo == lastObjNo) {
                        objOffset = lastObjOffset;
                        objLength = lastObjLength;
                    } else {
                        objOffset = 0;
                        objLength = chunkSize;
                    }

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

    private void failed(StripeState stripeState) {
        timeoutTimer.cancel();
        if (finishedSignaled.compareAndSet(false, true)) {
            processor.signal(this, new ReadEvent(ReadEventType.FAILED, stripeState));
        }
    }

    private void processFailed(ReadEvent event) {
        synchronized (activeHandlers) {
            while (activeHandlers.get() > 0) {
                try {
                    activeHandlers.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

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

    ServiceUUID getRemote(int osdNo) {
        return xloc.getLocalReplica().getOSDs().get(osdNo);
        // return remoteOSDs.get(osdNo < localOsdNo ? osdNo : osdNo - 1);
    }

    StripeState getStripeStateForObj(long objNo) {
        return getStripeStateForStripe(sp.getRow(objNo));
    }

    StripeState getStripeStateForStripe(long stripeNo) {
        return stripeStates[getStripeIdx(stripeNo)];
    }

    int getStripeIdx(long stripeNo) {
        return ECHelper.safeLongToInt(stripeNo - sp.getRow(sp.getObjectNoForOffset(reqInterval.getOpStart())));
    }

    @Override
    public TYPE getType() {
        return TYPE.READ;
    }

    @Override
    public Interval getRequestInterval() {
        return reqInterval;
    }

    @Override
    public StageRequest getRequest() {
        return request;
    }

    @Override
    public boolean hasFinished() {
        return finished;
    }

    @Override
    public boolean hasFailed() {
        return failed;
    }

    @Override
    public boolean hasSucceeded() {
        return !failed && finished;
    }

    @Override
    public ErrorResponse getError() {
        return error;
    }

    @Override
    public ObjectInformation getResult() {
        return result;
    }

    enum ResponseState {
        NONE, REQUESTED, REQ_RECOVERY,
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
        }
    }

    private class StripeState {
        final long         stripeNo;
        final Interval     interval;
        final ChunkState[] chunkStates;

        final int          firstOSDWithData;
        final int          lastOSDWithData;

        int                numFailures          = 0;

        final int          numCompleteRequired;
        int                numCompleteAvailable = 0;

        final int          numDataRequired;
        int                numDataAvailable     = 0;

        boolean            finished             = false;
        boolean            reconstruction       = false;

        public StripeState(long stripeNo, Interval interval) {
            this.stripeNo = stripeNo;
            this.interval = interval;
            chunkStates = new ChunkState[stripeWidth];

            firstOSDWithData = sp.getOSDforOffset(interval.getStart());
            lastOSDWithData = sp.getOSDforOffset(interval.getEnd() - 1);
            numDataRequired = lastOSDWithData - firstOSDWithData + 1;

            numCompleteRequired = dataWidth;
        }

        synchronized void markForReconstruction() {
            reconstruction = true;
        }

        synchronized boolean isInReconstruction() {
            return reconstruction;
        }

        synchronized boolean hasFinished() {
            return finished;
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

            int responseSize = 0;
            if (buf != null) {
                responseSize += buf.capacity();
            }
            if (response != null) {
                responseSize += response.getZeroPadding();
            }
            
                        
            boolean complete = !chunkState.partial && (responseSize == chunkSize);
            if (reconstruction && !complete) {
                // ignore partial results, if a recovery is requested
                BufferPool.free(buf);
                return false;
            }


            if (chunkState.osdNo >= firstOSDWithData && chunkState.osdNo <= lastOSDWithData) {
                numDataAvailable++;
            }

            if (complete) {
                numCompleteAvailable++;
            }


            chunkState.state = ResponseState.SUCCESS;
            chunkState.buffer = buf;
            chunkState.objData = response;


            if (reconstruction && numCompleteAvailable == numCompleteRequired) {
                finished = true;
                return true;

            } else if (numDataAvailable == numDataRequired) {
                finished = true;
                return true;
            }

            return false;
        }
    }

}
