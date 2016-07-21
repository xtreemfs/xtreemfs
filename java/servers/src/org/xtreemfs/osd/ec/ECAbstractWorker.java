/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECWorker.ECWorkerEvent;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public abstract class ECAbstractWorker<EVENT extends ECWorkerEvent> implements ECWorker<EVENT> {

    // Given as argument
    final String                      fileId;
    final StripingPolicyImpl          sp;
    final Interval                    reqInterval;
    final List<Interval>              commitIntervals;
    final List<IntervalMsg>           commitIntervalMsgs;
    final OSDServiceClient            osdClient;
    final OSDRequestDispatcher        master;
    final FileCredentials             fileCredentials;
    final XLocations                  xloc;

    // Note: circular reference
    final StageRequest                request;

    // Calculated
    final int                         localOsdNo;
    final int                         dataWidth;                  // k
    final int                         parityWidth;                // m
    final int                         stripeWidth;                // n
    final int                         chunkSize;
    final int                         numStripes;

    final long                        firstObjNo;
    final long                        lastObjNo;
    final int                         firstObjOffset;
    final int                         firstObjLength;
    final int                         lastObjOffset;
    final int                         lastObjLength;

    // State related
    boolean                           finished;
    boolean                           failed;
    ErrorResponse                     error;

    final AtomicBoolean               finishedSignaled;
    final AtomicInteger               activeHandlers;

    public ECAbstractWorker(OSDRequestDispatcher master, OSDServiceClient osdClient, FileCredentials fileCredentials,
            XLocations xloc, String fileId, StripingPolicyImpl sp, Interval reqInterval,
            List<Interval> commitIntervals, StageRequest request) {
        this.osdClient = osdClient;

        this.fileId = fileId;
        this.sp = sp;
        this.reqInterval = reqInterval;
        this.commitIntervals = commitIntervals;
        this.xloc = xloc;
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

        // absolute start and end to the whole file range
        final long opStart = reqInterval.getOpStart();
        final long opEnd = reqInterval.getOpEnd();

        firstObjNo = sp.getObjectNoForOffset(opStart);
        lastObjNo = sp.getObjectNoForOffset(opEnd - 1); // interval.end is exclusive

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


        long firstStripeNo = sp.getRow(firstObjNo);
        long lastStripeNo = sp.getRow(lastObjNo);
        numStripes = ECHelper.safeLongToInt(lastStripeNo - firstStripeNo + 1);

        finishedSignaled = new AtomicBoolean(false);
        activeHandlers = new AtomicInteger(0);
    }

    ServiceUUID getRemote(int osdNo) {
        return xloc.getLocalReplica().getOSDs().get(osdNo);
        // return remoteOSDs.get(osdNo < localOsdNo ? osdNo : osdNo - 1);
    }

    int getStripeIdx(long stripeNo) {
        return ECHelper.safeLongToInt(stripeNo - sp.getRow(sp.getObjectNoForOffset(reqInterval.getOpStart())));
    }

    int getObjOffset(long objNo) {
        if (objNo == firstObjNo) {
            return firstObjOffset;
        } else if (objNo == lastObjNo) {
            return lastObjOffset;
        } else {
            return 0;
        }
    }

    int getObjLength(long objNo) {
        if (objNo == firstObjNo) {
            return firstObjLength;
        } else if (objNo == lastObjNo) {
            return lastObjLength;
        } else {
            return chunkSize;
        }
    }

    void registerActiveHandler() {
        activeHandlers.incrementAndGet();
    }

    void deregisterActiveHandler() {
        if (activeHandlers.decrementAndGet() == 0) {
            synchronized (activeHandlers) {
                activeHandlers.notifyAll();
            }
        }
    }

    void waitForActiveHandlers() {
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
    public Interval getRequestInterval() {
        return reqInterval;
    }

    @Override
    public StageRequest getRequest() {
        return request;
    }
}
