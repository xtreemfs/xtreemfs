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
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECWriteWorker.WriteEventType;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.LocalRPCResponseListener;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.ResponseResult;
import org.xtreemfs.osd.operations.ECWriteIntervalOperation;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_diffResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_intervalResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * This class will act as a state machine used for writing.
 */
public class ECWriteWorker implements ECWorker<WriteEventType> {
    public static enum WriteEventType {
        FINISHED, FAILED
    }

    final ECWorkerEventProcessor<WriteEventType> processor;

    // Given as argument
    final String               fileId;
    final long                 opId;
    final StripingPolicyImpl   sp;
    final Interval             reqInterval;
    final List<Interval>       commitIntervals;
    final List<IntervalMsg>    commitIntervalMsgs;
    final int                  qw;
    final StripeState[]        stripeStates;
    final OSDServiceClient     osdClient;
    // final List<ServiceUUID> remoteOSDs;
    final OSDRequestDispatcher master;
    final FileCredentials      fileCredentials;
    final XLocations           xloc;
    final ReusableBuffer       data;
    final long                                   timeoutMS;

    // Note: circular reference
    final StageRequest         request;

    // Calculated
    final int                  localOsdNo;
    final int                  dataWidth;         // k
    final int                  parityWidth;       // m
    final int                  stripeWidth;       // n
    final int                  chunkSize;
    final int                  numStripes;

    // State Stuff
    // FIXME (jdillmann): Decide how the worker should be run and if sync is needed
    // int numStripesComplete;
    AtomicInteger              numStripesComplete;
    boolean                    finished;
    boolean                    failed;
    ErrorResponse              error;
    
    final Timer                                  timer;
    final TimerTask                              timeoutTimer;

    public ECWriteWorker(OSDRequestDispatcher master, FileCredentials fileCredentials, XLocations xloc, String fileId,
            long opId, StripingPolicyImpl sp, int qw, Interval reqInterval, List<Interval> commitIntervals,
            ReusableBuffer data, StageRequest request, long timeoutMS, ECMasterStage ecMaster) {
        this.processor = ecMaster;
        this.timer = ecMaster.timer;
        this.osdClient = ecMaster.osdClient;

        this.fileId = fileId;
        this.opId = opId;
        this.sp = sp;
        this.reqInterval = reqInterval;
        this.commitIntervals = commitIntervals;
        this.qw = qw;
        this.data = data;
        this.xloc = xloc;
        this.master = master;
        this.fileCredentials = fileCredentials;
        this.request = request;
        // this.remoteOSDs = remoteOSDs;
        

        commitIntervalMsgs = new ArrayList<IntervalMsg>(commitIntervals.size());
        for (Interval interval : commitIntervals) {
            commitIntervalMsgs.add(ProtoInterval.toProto(interval));
        }


        dataWidth = sp.getWidth();
        parityWidth = sp.getParityWidth();
        stripeWidth = sp.getWidth() + sp.getParityWidth();
        chunkSize = sp.getPolicy().getStripeSize() * 1024;
        localOsdNo = sp.getRelativeOSDPosition();

        // numStripesComplete = 0;
        numStripesComplete = new AtomicInteger(0);
        finished = false;
        failed = false;
        error = null;

        // absolute start and end to the whole file range
        long opStart = reqInterval.getOpStart();
        long opEnd = reqInterval.getOpEnd();

        long firstObjNo = sp.getObjectNoForOffset(opStart);
        long lastObjNo = sp.getObjectNoForOffset(opEnd - 1); // interval.end is exclusive
                
        long firstStripeNo = sp.getRow(firstObjNo);
        long lastStripeNo = sp.getRow(lastObjNo);

        numStripes = ECHelper.safeLongToInt(lastStripeNo - firstStripeNo + 1);
        stripeStates = new StripeState[numStripes];

        this.timeoutMS = timeoutMS;
        timeoutTimer = new TimerTask() {
            @Override
            public void run() {
                // ECWriteWorker.this.processor.signal(ECWriteWorker.this, WriteEventType.FAILED);
                ECWriteWorker.this.timeout();
            }
        };
    }

    @Override
    public void processEvent(WriteEventType event) {
        switch (event) {
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
            
            StripeState stripeState = new StripeState(stripeInterval);
            stripeStates[stripeIdx] = stripeState;

            for (long objNo = firstStripeObjNo; objNo <= lastStripeObjNo; objNo++) {

                boolean hasData = false;
                ReusableBuffer reqData = null;
                // relative offset to the current object
                int objOffset = 0;

                if (objNo >= firstObjWithData && objNo <= lastObjWithData) {
                    hasData = true;
                    // length of the data to write off the current object
                    int objLength;
                    // relative offset to the data buffer
                    int bufOffset;

                    if (objNo == firstObjNo) {
                        objOffset = ECHelper.safeLongToInt(opStart - sp.getObjectStartOffset(firstObjNo));
                        objLength = chunkSize - objOffset;
                        bufOffset = 0;
                    } else {
                        objOffset = 0;
                        objLength = chunkSize;
                        bufOffset = ECHelper.safeLongToInt(sp.getObjectStartOffset(objNo) - opStart);
                    }

                    // Don't merge with cases above as firstObjNo and lastObjNo can be the same.
                    if (objNo == lastObjNo) {
                        objLength = ECHelper.safeLongToInt(opEnd - sp.getObjectStartOffset(lastObjNo) - objOffset);
                    }

                    reqData = data.createViewBuffer();
                    reqData.range(bufOffset, objLength);
                }

                int osdNo = sp.getOSDforObject(objNo);
                stripeState.responseStates[osdNo] = hasData ? ResponseState.REQ_DATA : ResponseState.REQ_INTERVAL;

                if (osdNo == localOsdNo) {
                    // make local request
                    handler.addLocal(objNo, reqData);
                    ECWriteIntervalOperation writeOp = (ECWriteIntervalOperation) master.getOperation(ECWriteIntervalOperation.PROC_ID);
                    writeOp.startLocalRequest(fileId, sp, opId, hasData, objNo, objOffset, stripeInterval.getMsg(),
                            commitIntervalMsgs, reqData, fileCredentials, xloc, handler);

                } else {
                    try {
                        ServiceUUID server = getRemote(osdNo);
                        // FIXME (jdillmann): Does write free the buffer?
                        // FIXME (jdillmann): which op id should be used?
                        RPCResponse<xtreemfs_ec_write_intervalResponse> rpcResponse = osdClient.xtreemfs_ec_write_interval(
                                server.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                fileCredentials, fileId, opId, hasData, objNo, objOffset, stripeInterval.getMsg(),
                                commitIntervalMsgs, reqData);
                        handler.addRemote(rpcResponse, objNo);

                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_WARN, this, ex);
                        failed();
                        return;
                    }
                }

            }

            for (int r = 0; r < parityWidth; r++) {
                int osdNo = dataWidth + r;
                // stripeState.responseStates[osdNo] = ResponseState.;
            }
        }
    }
    

    private void handleDataResponse(ResponseResult<xtreemfs_ec_write_intervalResponse, Long> result, int numResponses,
            int numErrors, int numQuickFail) {
        long objNo = result.getMappedObject();

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = !objFailed && result.getResult().getNeedsReconstruction();

        long stripeNo = sp.getRow(objNo);
        StripeState stripeStatus = getStripeStateForStripe(stripeNo);
        Interval stripeInterval = stripeStatus.interval;

        int osdPos = sp.getOSDforObject(objNo);

        if (objFailed || needsReconstruction) {
            int curNacks = stripeStatus.nacks.incrementAndGet();
            stripeStatus.responseStates[osdPos] = needsReconstruction ? ResponseState.NEEDS_RECONSTRUCTION
                    : ResponseState.FAILED;
            // Only needed if the OSD would have been written
            // if (relOsdPos == sp.getOSDforObject(objNo)) {
            // // Need read/write cycle
            // long chunkStart = sp.getObjectStartOffset(objNo);
            // long chunkEnd = sp.getObjectStartOffset(objNo) + 1;
            //
            // long stripeStart = stripeNo * dataWidth * chunkSize;
            // long stripeEnd = stripeStart + chunkSize;
            //
            // // Get the right vector from the
            // IntervalVector commitVector = new ListIntervalVector(commitIntervals);
            // List<Interval> prevIntervals = commitVector.getSlice(stripeStart, stripeEnd);
            //
            // // FIXME (jdillmann): Give a solution!
            //
            // // // THIS IS NOT TRUE! as we can't diff this would break (or we would need another operation)
            // // especially
            // // // for full stripes.
            // // // easier: if (stripeInterval.getEnd() - stripeInterval.getStart() == chunkSize) {
            // // if (stripeInterval.getStart() == stripeStart && stripeInterval.getEnd() == stripeEnd) {
            // // // The full stripe is written, no need to get data from the other devices
            // // long dataOffset = stripeInterval.getOpStart() - stripeInterval.getStart();
            // //
            // //
            // // } else {
            // // // get data from the other devices.
            // // // TODO (jdillmann): could be optimized by only getting those not available with this request.
            // // }
            // }

            if (stripeWidth - curNacks < qw) {
                // we can't fullfill the write quorum anymore
                failed();
            }

        } else {
            int curAcks = stripeStatus.acks.incrementAndGet();
            stripeStatus.responseStates[osdPos] = ResponseState.SUCCESS;

            if (curAcks >= qw) {
                // SUCCESS
                int curNumStripesComplete = numStripesComplete.incrementAndGet();
                if (curNumStripesComplete == numStripes) {
                    finished();
                }
            }
        }
       

    }

    void handleParityResponse(xtreemfs_ec_write_diffResponse response) {
        boolean objFailed = response.hasError();
        boolean needsReconstruction = response.getNeedsReconstruction();

        long stripeNo = response.getStripeNumber();
        StripeState stripeStatus = getStripeStateForStripe(stripeNo);
        Interval stripeInterval = stripeStatus.interval;

        int osdPos = response.getOsdNumber();

        if (objFailed || needsReconstruction) {
            int curNacks = stripeStatus.nacks.incrementAndGet();
            stripeStatus.responseStates[osdPos] = needsReconstruction ? ResponseState.NEEDS_RECONSTRUCTION
                    : ResponseState.FAILED;


            if (stripeWidth - curNacks < qw) {
                // we can't fullfill the write quorum anymore
                failed();
            }

        } else {
            int curAcks = stripeStatus.acks.incrementAndGet();
            stripeStatus.responseStates[osdPos] = ResponseState.SUCCESS;

            if (curAcks >= qw) {
                // SUCCESS
                int curNumStripesComplete = numStripesComplete.incrementAndGet();
                if (curNumStripesComplete == numStripes) {
                    finished();
                }
            }
        }

    }

    public void timeout() {
        timeoutTimer.cancel();
        // FIXME (jdillmann): check for concurrency!!!
        processor.signal(this, WriteEventType.FAILED);
    }

    private void finished() {
        timeoutTimer.cancel();
        // finishedSignaled is not really needed as long as only one process is allowed to enter the when cur == num
        // if (finishedSignaled.compareAndSet(false, true)) {
        // }
        processor.signal(this, WriteEventType.FINISHED);
    }

    private void processFinished(WriteEventType event) {
        BufferPool.free(data);
        finished = true;
    }

    private void failed() {
        timeoutTimer.cancel();
        processor.signal(this, WriteEventType.FAILED);
    }

    private void processFailed(WriteEventType event) {
        BufferPool.free(data);
        finished = true;
        failed = true;

        error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                "Request failed. Could not write enough chunks.");
        // String.format("Request failed. Could not write to stripe %d ([%d:%d]).", event.stripeNo,
        // errorStripeState.interval.getStart(), errorStripeState.interval.getEnd()));
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
    public Object getResult() {
        return null;
    }

    @Override
    public TYPE getType() {
        return TYPE.WRITE;
    }

    @Override
    public Interval getRequestInterval() {
        return reqInterval;
    }

    @Override
    public StageRequest getRequest() {
        return request;
    }

    enum ResponseState {
        NONE, REQ_INTERVAL, REQ_DATA, SUCCESS, FAILED, NEEDS_RECONSTRUCTION,
    }

    private class StripeState {
        final Interval        interval;
        final ResponseState[] responseStates;

        AtomicInteger         acks  = new AtomicInteger(0);
        AtomicInteger         nacks = new AtomicInteger(0);

        public StripeState(Interval interval) {
            this.interval = interval;

            responseStates = new ResponseState[stripeWidth];
            for (int i = 0; i < stripeWidth; i++) {
                responseStates[i] = ResponseState.NONE;
            }
        }
    }

}
