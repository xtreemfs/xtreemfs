/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
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

/**
 * This class will act as a state machine used for reading.
 */
public class ECReadWorker implements ECWorker<ReadEvent> {
    public static enum ReadEventType {
        // STRIPE_COMPLETE, STRIPE_FAILED,
        FINISHED, FAILED
    }

    public final static class ReadEvent {
        final ReadEventType type;
        final long          stripeNo;
        final int           osdNo;

        public ReadEvent(ReadEventType type, long stripeNo, int osdNo) {
            this.type = type;
            this.stripeNo = stripeNo;
            this.osdNo = osdNo;
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
    final List<ServiceUUID>                 remoteOSDs;
    final OSDRequestDispatcher              master;
    final FileCredentials                   fileCredentials;
    final ReusableBuffer                    data;

    // Note: circular reference
    final StageRequest                      request;

    // Calculated
    final int                               localOsdNo;
    final int                               dataWidth;         // k
    final int                               parityWidth;       // m
    final int                               stripeWidth;       // n
    final int                               chunkSize;
    final int                               numStripes;

    // State Stuff
    // FIXME (jdillmann): Decide how the worker should be run and if sync is needed
    // int numStripesComplete;
    AtomicInteger                           numStripesComplete;

    boolean                                 finished;
    boolean                                 failed;
    ErrorResponse                           error;
    ObjectInformation                       result;

    final AtomicBoolean                     finishedSignaled;
    final AtomicInteger                     activeHandlers;

    public ECReadWorker(OSDRequestDispatcher master, OSDServiceClient osdClient, FileCredentials fileCredentials,
            String fileId, StripingPolicyImpl sp, Interval reqInterval, List<Interval> commitIntervals,
            List<ServiceUUID> remoteOSDs, ReusableBuffer data, StageRequest request,
            ECWorkerEventProcessor<ReadEvent> processor) {
        this.processor = processor;

        this.fileId = fileId;
        this.sp = sp;
        this.reqInterval = reqInterval;
        this.commitIntervals = commitIntervals;
        this.data = data;
        this.osdClient = osdClient;
        this.remoteOSDs = remoteOSDs;
        this.master = master;
        this.fileCredentials = fileCredentials;
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

        numStripes = ECHelper.safeLongToInt(lastStripeNo - firstStripeNo + 1);
        stripeStates = new StripeState[numStripes];

        finishedSignaled = new AtomicBoolean(false);
        activeHandlers = new AtomicInteger(0);
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
        }
    }

    @Override
    public void start() {
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

            LocalRPCResponseHandler<xtreemfs_ec_readResponse, Long> handler = new LocalRPCResponseHandler<xtreemfs_ec_readResponse, Long>(
                    numResponses, new LocalRPCResponseListener<xtreemfs_ec_readResponse, Long>() {
                        @Override
                        public void responseAvailable(ResponseResult<xtreemfs_ec_readResponse, Long> result,
                                int numResponses, int numErrors, int numQuickFail) {
                            handleDataResponse(result, numResponses, numErrors, numQuickFail);
                        }
                    });


            StripeState stripeState = new StripeState(stripeInterval);
            stripeStates[stripeIdx] = stripeState;

            for (long objNo = firstObjWithData; objNo <= lastObjWithData; objNo++) {

                // ReusableBuffer reqData = null;
                // relative offset to the current object
                int objOffset = 0;
                // length of the data to read from the current object
                int objLength;

                if (objNo == firstObjNo) {
                    objOffset = ECHelper.safeLongToInt(opStart - sp.getObjectStartOffset(firstObjNo));
                    objLength = chunkSize - objOffset;
                } else {
                    objOffset = 0;
                    objLength = chunkSize;
                }

                // Don't merge with cases above as firstObjNo and lastObjNo can be the same.
                if (objNo == lastObjNo) {
                    objLength = ECHelper.safeLongToInt(opEnd - sp.getObjectStartOffset(lastObjNo) - objOffset);
                }

                int osdNo = sp.getOSDforObject(objNo);
                stripeState.responseStates[osdNo] = ResponseState.REQUESTED;

                if (osdNo == localOsdNo) {
                    // make local request
                    handler.addLocal(objNo);
                    ECReadOperation readOp = (ECReadOperation) master.getOperation(ECReadOperation.PROC_ID);
                    readOp.startLocalRequest(fileId, sp, objNo, objOffset, objLength, commitIntervalMsgs, handler);
                } else {
                    try {
                        ServiceUUID server = getRemote(osdNo);
                        RPCResponse<xtreemfs_ec_readResponse> rpcResponse = osdClient.xtreemfs_ec_read(
                                server.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                fileCredentials, fileId, objNo, objOffset, objLength, commitIntervalMsgs);
                        handler.addRemote(rpcResponse, objNo);

                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_WARN, this, ex);
                        failed(stripeNo);
                        return;
                    }
                }

            }

            for (int r = 0; r < parityWidth; r++) {
                int osdNo = dataWidth + r;
            }
        }
    }


    private void handleDataResponse(ResponseResult<xtreemfs_ec_readResponse, Long> result, int numResponses,
            int numErrors, int numQuickFail) {

        if (finishedSignaled.get()) {
            BufferPool.free(result.getData());
            return;
        }
        activeHandlers.incrementAndGet();

        long objNo = result.getMappedObject();

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = result.getResult().getNeedsReconstruction();

        long stripeNo = sp.getRow(objNo);
        StripeState stripeState = getStripeStateForStripe(stripeNo);
        Interval stripeInterval = stripeState.interval;

        int osdPos = sp.getOSDforObject(objNo);


        if (objFailed || needsReconstruction) {
            boolean stripeFailed = stripeState.markFailed(osdPos);
            if (stripeFailed) {
                failed(stripeNo);
            }

        } else {
            boolean stripeComplete = stripeState.setResultBuffer(osdPos, result.getData(),
                    result.getResult().getObjectData());

            if (stripeComplete) {
                stripeComplete(stripeNo);
            }
        }


        if (activeHandlers.decrementAndGet() == 0) {
            synchronized (activeHandlers) {
                activeHandlers.notifyAll();
            }
        }
    }

    private void stripeComplete(long stripeNo) {
        // processor.signal(this, new ReadEvent(ReadEventType.STRIPE_COMPLETE, stripeNo, osdPos));

        int curStripesComplete = numStripesComplete.incrementAndGet();
        if (curStripesComplete == numStripes) {
            finished();
        }
    }

    private void finished() {
        // finishedSignaled is not really needed as long as only one process is allowed to enter the when cur == num
        if (finishedSignaled.compareAndSet(false, true)) {
            processor.signal(this, new ReadEvent(ReadEventType.FINISHED, 0, 0));
        }
    }

    private void processFinished(ReadEvent event) {
        synchronized (activeHandlers) {
            while (activeHandlers.get() > 0) {
                try {
                    activeHandlers.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

        finished = true;
        
        
        data.position(0);
        for (StripeState stripeState : stripeStates) {
            // Reconstruct if necessary
            if (stripeState.chunksAvailable.get() < stripeState.chunksRequired) {
                // FIXME (jdillmann): do
            }

            for (int osdNo = stripeState.firstOSDWithData; osdNo <= stripeState.lastOSDWithData; osdNo++) {
                ResponseState responseState = stripeState.responseStates[osdNo];
                assert (responseState == ResponseState.SUCCESS || responseState == ResponseState.FAILED);

                ObjectData objData = stripeState.responses[osdNo];
                ReusableBuffer buf = stripeState.buffers[osdNo];

                // Copy the buffer to the result
                if (buf != null) {
                    // TODO: This is probably very slow
                    data.put(buf);
                }

                if (objData != null) {
                    for (int i = 0; i < objData.getZeroPadding(); i++) {
                        // TODO: This is probably very slow
                        data.put((byte) 0);
                    }
                }

            }

            // Clear response buffers
            for (ReusableBuffer buf : stripeState.buffers) {
                BufferPool.free(buf);
            }
        }

        assert (!data.hasRemaining()) : "Error while reading and copying read responses";
        
        // TODO (jdillmann): Padding responses at the end could be merged and reflected int the objInfo
        data.position(0);
        result = new ObjectInformation(ObjectStatus.EXISTS, data, chunkSize);
    }

    private void failed(long stripeNo) {
        // finishedSignaled is not really needed as long as only one process is allowed to enter the when cur == num
        if (finishedSignaled.compareAndSet(false, true)) {
            processor.signal(this, new ReadEvent(ReadEventType.FAILED, stripeNo, 0));
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

            if (stripeState != null) {
                // Clear response buffers
                for (ReusableBuffer buf : stripeState.buffers) {
                    BufferPool.free(buf);
                }
            }
        }

        finished = true;
        failed = true;
        
        StripeState errorStripeState = getStripeStateForStripe(event.stripeNo);
        error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                String.format("Request failed. Could not read from stripe %d ([%d:%d]).",
                        event.stripeNo, errorStripeState.interval.getStart(), errorStripeState.interval.getEnd()));

    }

    ServiceUUID getRemote(int osdNo) {
        return remoteOSDs.get(osdNo < localOsdNo ? osdNo : osdNo - 1);
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
        NONE, REQUESTED,
        FAILED, NEEDS_RECONSTRUCTION,
        SUCCESS, RECONSTRUCTED;
    }

    private class StripeState {
        final Interval         interval;
        final ResponseState[]  responseStates;
        final ReusableBuffer[] buffers;
        final ObjectData[]     responses;
        final int              firstOSDWithData;
        final int              lastOSDWithData;
        final AtomicInteger    acks;
        final AtomicInteger    nacks;
        final AtomicInteger    chunksAvailable;
        final int              chunksRequired;
        volatile boolean       finished;

        public StripeState(Interval interval) {
            this.interval = interval;

            responseStates = new ResponseState[stripeWidth];
            for (int i = 0; i < stripeWidth; i++) {
                responseStates[i] = ResponseState.NONE;
            }

            buffers = new ReusableBuffer[stripeWidth];
            responses = new ObjectData[stripeWidth];

            acks = new AtomicInteger(0);
            nacks = new AtomicInteger(0);
            chunksAvailable = new AtomicInteger(0);

            firstOSDWithData = sp.getOSDforOffset(interval.getStart());
            lastOSDWithData = sp.getOSDforOffset(interval.getEnd() - 1);
            chunksRequired = lastOSDWithData - firstOSDWithData + 1;

            finished = false;
        }

        /**
         * Sets the OSDs response to failed and returns true if the request can no longer be fullfilled
         * 
         * @param osdNum
         * @return
         */
        boolean markFailed(int osdPos) {
            int curNacks = nacks.incrementAndGet();
            if (!finished) {
                responseStates[osdPos] = ResponseState.FAILED;

                if (stripeWidth - curNacks < dataWidth) {
                    finished = true;
                    return true;
                }
            }
            return false;
        }

        /**
         * Sets the OSDs result to success and returns true if all chunks are available.
         * 
         * @param osdPos
         * @param buf
         * @return
         */
        boolean setResultBuffer(int osdPos, ReusableBuffer buf, ObjectData response) {
            int curAcks = acks.incrementAndGet();
            int curComplete = chunksAvailable.incrementAndGet();

            if (!finished) {
                responseStates[osdPos] = ResponseState.SUCCESS;
                buffers[osdPos] = buf;
                responses[osdPos] = response;

                if (curComplete == chunksRequired) {
                    finished = true;
                    return true;
                }
            } else {
                BufferPool.free(buf);
            }

            return false;
        }
    }


}
