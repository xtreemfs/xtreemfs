/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.stages.StorageStage.ECCommitVectorCallback;
import org.xtreemfs.osd.stages.StorageStage.ECGetVectorsCallback;
import org.xtreemfs.osd.stages.StorageStage.ECReadDataCallback;
import org.xtreemfs.osd.stages.StorageStage.ECReadParityCallback;
import org.xtreemfs.osd.stages.StorageStage.ECWriteDiffCallback;
import org.xtreemfs.osd.stages.StorageStage.ECWriteIntervalCallback;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.osd.storage.StorageLayout;

import com.backblaze.erasure.ReedSolomon;

/**
 * This class contains the methods regarding EC data and IntervalVector handling. <br>
 * For sake of clarity the methods are separated to this class. <br>
 * Since the IntervalVectors are tightly coupled to the data integrity they have to be handled in the same stage to
 * reduce the chance of failures and inconsistencies.<br>
 * Unfortunately this means, that the possibly expensive IntervalVector calculations and the expensive encoding
 * operations are also run in the StorageStage. If profiling shows their impact those methods should be moved to a
 * separate stage.
 */
public class ECStorage {
    public final static String         FILEID_NEXT_SUFFIX  = ".next";
    public final static String         FILEID_CODE_SUFFIX  = ".code";
    public final static String         FILEID_DELTA_SUFFIX = ".delta";

    private final MetadataCache        cache;
    private final StorageLayout        layout;
    private final OSDRequestDispatcher master;
    private final boolean              checksumsEnabled;

    public ECStorage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout, boolean checksumsEnabled) {
        this.master = master;
        this.cache = cache;
        this.layout = layout;
        this.checksumsEnabled = checksumsEnabled;
    }

    public void processGetVectors(final StageRequest rq) {
        final ECGetVectorsCallback callback = (ECGetVectorsCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];

        FileMetadata fi = cache.getFileInfo(fileId);
        if (fi == null) {
            try {
                IntervalVector curVector = new AVLTreeIntervalVector();
                layout.getECIntervalVector(fileId, false, curVector);

                IntervalVector nextVector = new AVLTreeIntervalVector();
                layout.getECIntervalVector(fileId, true, nextVector);

                callback.ecGetVectorsComplete(curVector, nextVector, null);

            } catch (Exception ex) {
                callback.ecGetVectorsComplete(null, null,
                        ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
            }
        } else {
            callback.ecGetVectorsComplete(fi.getECCurVector(), fi.getECNextVector(), null);
        }
    }

    public void processCommitVector(final StageRequest rq) {
        final ECCommitVectorCallback callback = (ECCommitVectorCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        final List<Interval> commitIntervals = (List<Interval>) rq.getArgs()[2];

        try {
            boolean isParity = sp.getRelativeOSDPosition() >= sp.getWidth();
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);

            List<Interval> toCommit = new LinkedList<Interval>();
            List<Interval> toAbort = new LinkedList<Interval>();
            List<Interval> missing = new LinkedList<Interval>();

            boolean needsRecontruction = false;
            if (!commitIntervals.isEmpty()) {
                boolean failed = ECPolicy.calculateIntervalsToCommitAbort(commitIntervals, null,
                        fi.getECCurVector().serialize(), fi.getECNextVector().serialize(), toCommit, toAbort, missing);

                if (failed) {
                    needsRecontruction = true;
                }

                for (Interval interval : toCommit) {
                    if (isParity) {
                        commitECDelta(fileId, fi, interval);
                    } else {
                        commitECData(fileId, fi, interval);
                    }
                }
            }

            // Abort everything in the next vector
            // FIXME (jdillmann): Also delete the EC data?
            AVLTreeIntervalVector emptyNextVector = new AVLTreeIntervalVector();
            layout.setECIntervalVector(fileId, emptyNextVector.serialize(), true, false);
            fi.setECNextVector(emptyNextVector);

            // FIXME (jdillmann): truncate cur?
            callback.ecCommitVectorComplete(missing, needsRecontruction, null);

        } catch (IOException ex) {
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecCommitVectorComplete(null, false, error);
            return;
        }
    }



    public void processWriteInterval(final StageRequest rq) {
        final ECWriteIntervalCallback callback = (ECWriteIntervalCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        final long objectNo = (Long) rq.getArgs()[2];
        final int offset = (Integer) rq.getArgs()[3];
        final Interval reqInterval = (Interval) rq.getArgs()[4];
        final List<Interval> commitIntervals = (List<Interval>) rq.getArgs()[5];
        final ReusableBuffer data = (ReusableBuffer) rq.getArgs()[6];

        assert (!commitIntervals.isEmpty());

        long objVer = 1;
        // FIXME (jdillmann): Decide if/when sync should be used
        boolean sync = false;

        boolean consistent = true;
        try {
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            IntervalVector curVector = fi.getECCurVector();
            IntervalVector nextVector = fi.getECNextVector();

            long opStart = reqInterval.getOpStart();
            long opEnd = reqInterval.getOpEnd();

            // FIXME(jdillmann): A single write may never cross stripe boundaries

            // Get this nodes interval vectors and check if the vector this operation is based on can be fully committed
            long commitStart = commitIntervals.get(0).getOpStart();
            long commitEnd = commitIntervals.get(commitIntervals.size() - 1).getOpEnd();
            // FIXME (jdillmann): Check if this is correct
            List<Interval> curVecIntervals = curVector.getOverlapping(commitStart, commitEnd);
            List<Interval> nextVecIntervals = nextVector.getOverlapping(commitStart, commitEnd);

            LinkedList<Interval> toCommitAcc = new LinkedList<Interval>();
            LinkedList<Interval> toAbortAcc = new LinkedList<Interval>();

            boolean failed = ECPolicy.calculateIntervalsToCommitAbort(commitIntervals, reqInterval, curVecIntervals,
                    nextVecIntervals, toCommitAcc, toAbortAcc);

            // Signal to go to reconstruct if the vector can not be fully committed.
            if (failed) {
                callback.ecWriteIntervalComplete(null, true, null);
                return;
            }

            // Commit or abort intervals from the next vector.
            for (Interval interval : toCommitAcc) {
                commitECData(fileId, fi, interval);
            }
            for (Interval interval : toAbortAcc) {
                abortECData(fileId, fi, interval);
            }

            if (data != null) {
                // Check operation boundaries.
                // FIXME (jdillmann): Check for exclusive ends
                long dataStart = sp.getObjectStartOffset(objectNo) + offset;
                long dataEnd = dataStart + data.capacity();
                // The data range has to be within the request interval and may not cross chunk boundaries
                assert (dataStart >= reqInterval.getStart() && dataEnd <= reqInterval.getEnd());
                assert (dataEnd - dataStart <= sp.getStripeSizeForObject(objectNo));

                // Write the data
                String fileIdNext = fileId + FILEID_NEXT_SUFFIX;
                layout.writeObject(fileIdNext, fi, data.createViewBuffer(), objectNo, offset, objVer, sync, false);
                consistent = false;

                // Store the vector
                layout.setECIntervalVector(fileId, Arrays.asList(reqInterval), true, true);
                fi.getECNextVector().insert(reqInterval);
                consistent = true;

                // Generate diff between the current data and the newly written data.
                ReusableBuffer diffBuffer = BufferPool.allocate(data.capacity());
                ObjectInformation objInfo = layout.readObject(fileId, fi, objectNo, offset, data.capacity(), objVer);
                if (objInfo.getData() != null) {
                    ReusableBuffer curData = objInfo.getData();
                    ECHelper.xor(diffBuffer, curData, data);
                    BufferPool.free(curData);
                }
                
                diffBuffer.put(data);
                assert (!data.hasRemaining() && !diffBuffer.hasRemaining());
                data.flip();
                diffBuffer.flip();

                // Return the diff buffer to the caller
                callback.ecWriteIntervalComplete(diffBuffer, false, null);

            } else {
                // Store the vector
                layout.setECIntervalVector(fileId, Arrays.asList(reqInterval), true, true);
                fi.getECNextVector().insert(reqInterval);

                callback.ecWriteIntervalComplete(null, false, null);
            }

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }

            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecWriteIntervalComplete(null, false, error);
        } finally {
            BufferPool.free(data);
        }
    }

    public void processWriteDiff(final StageRequest rq) {
        final ECWriteDiffCallback callback = (ECWriteDiffCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        final long objectNo = (Long) rq.getArgs()[2];
        final int offset = (Integer) rq.getArgs()[3];
        final Interval diffInterval = (Interval) rq.getArgs()[4];
        final Interval stripeInterval = (Interval) rq.getArgs()[5];
        final List<Interval> commitIntervals = (List<Interval>) rq.getArgs()[6];
        final ReusableBuffer data = (ReusableBuffer) rq.getArgs()[7];

        // final long opId

        assert (!commitIntervals.isEmpty());
        
        long objVer = 1;
        // FIXME (jdillmann): Decide if/when sync should be used
        boolean sync = false;

        boolean consistent = true;
        try {
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            IntervalVector curVector = fi.getECCurVector();
            IntervalVector nextVector = fi.getECNextVector();

            long opStart = diffInterval.getOpStart();
            long opEnd = diffInterval.getOpEnd();

            // FIXME(jdillmann): A single write may never cross stripe boundaries

            // Get this nodes interval vectors and check if the vector this operation is based on can be fully committed
            long commitStart = commitIntervals.get(0).getOpStart();
            long commitEnd = commitIntervals.get(commitIntervals.size() - 1).getOpEnd();
            // FIXME (jdillmann): Check if this is correct
            List<Interval> curVecIntervals = curVector.getOverlapping(commitStart, commitEnd);
            List<Interval> nextVecIntervals = nextVector.getOverlapping(commitStart, commitEnd);

            LinkedList<Interval> toCommitAcc = new LinkedList<Interval>();
            LinkedList<Interval> toAbortAcc = new LinkedList<Interval>();

            boolean failed = ECPolicy.calculateIntervalsToCommitAbort(commitIntervals, diffInterval, curVecIntervals,
                    nextVecIntervals, toCommitAcc, toAbortAcc);

            // Signal to go to reconstruct if the vector can not be fully committed.
            if (failed) {
                callback.ecWriteDiffComplete(false, true, null);
                return;
            }

            // Commit or abort intervals from the next vector.
            for (Interval interval : toCommitAcc) {
                commitECDelta(fileId, fi, interval);
            }
            for (Interval interval : toAbortAcc) {
                abortECData(fileId, fi, interval);
            }

            // Check operation boundaries.
            // FIXME (jdillmann): Check for exclusive ends
            long dataStart = sp.getObjectStartOffset(objectNo) + offset;
            long dataEnd = dataStart + data.capacity();
            // The data range has to be within the request interval and may not cross chunk boundaries
            assert (dataStart >= diffInterval.getStart() && dataEnd <= diffInterval.getEnd());
            assert (dataEnd - dataStart <= sp.getStripeSizeForObject(objectNo));


            // Encode the data
            // FIXME (jdillmann): Decide which is faster and which to use

            // ReedSolomon codec = ReedSolomon.create(sp.getWidth(), sp.getParityWidth());
            // byte[] dataArray = data.array();
            // byte[] deltaArray = new byte[data.capacity()];
            // int chunkOSDNoOff = sp.getOSDforObject(objectNo);
            // int parityOSDNoOff = sp.getRelativeOSDPosition() - sp.getWidth();
            // codec.encodeDiffParity(dataArray, deltaArray, chunkOSDNoOff, parityOSDNoOff, offset, data.capacity());
            // ReusableBuffer deltaBuf = ReusableBuffer.wrap(deltaArray);

            ReedSolomon codec = ReedSolomon.create(sp.getWidth(), sp.getParityWidth());
            ReusableBuffer deltaBuf = BufferPool.allocate(data.capacity());
            int chunkOSDNoOff = sp.getOSDforObject(objectNo);
            int parityOSDNoOff = sp.getRelativeOSDPosition() - sp.getWidth();
            assert (parityOSDNoOff >= 0) : "Write DIFF operation on data device";

            codec.encodeDiffParity(data.getBuffer(), chunkOSDNoOff, deltaBuf.getBuffer(), parityOSDNoOff, 0,
                    data.capacity());
            deltaBuf.position(0);

            // Write the data
            String fileIdDelta = fileId + FILEID_DELTA_SUFFIX;
            // codeBuf.flip();
            layout.writeObject(fileIdDelta, fi, deltaBuf, objectNo, offset, objVer, sync, false);
            consistent = false;

            // Store the vector
            layout.setECIntervalVector(fileId, Arrays.asList(diffInterval), true, true);
            fi.getECNextVector().insert(diffInterval);
            consistent = true;
            
            // The stripe is complete if the stripeInterval is completely stored within the next vector.
            boolean stripeComplete = fi.getECNextVector().contains(stripeInterval);

            // Return the diff buffer to the caller
            callback.ecWriteDiffComplete(stripeComplete, false, null);

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }

            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecWriteDiffComplete(false, false, error);
        } finally {
            BufferPool.free(data);
        }
    }

    public void processReadData(final StageRequest rq) {
        final ECReadDataCallback callback = (ECReadDataCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        final long objNo = (Long) rq.getArgs()[2];
        final int offset = (Integer) rq.getArgs()[3];
        final int length = (Integer) rq.getArgs()[4];
        final List<Interval> intervals = (List<Interval>) rq.getArgs()[5];
        final boolean ignoreAbort = (Boolean) rq.getArgs()[6];

        assert (!intervals.isEmpty());
        assert (offset + length <= sp.getStripeSizeForObject(objNo));

        final String fileIdNext = fileId + FILEID_NEXT_SUFFIX;
        long objOffset = sp.getObjectStartOffset(objNo);

        long objVer = 1;

        // ReusableBuffer data = BufferPool.allocate(length);
        
        try {
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            IntervalVector curVector = fi.getECCurVector();
            IntervalVector nextVector = fi.getECNextVector();

            long commitStart = intervals.get(0).getStart();
            long commitEnd = intervals.get(intervals.size() - 1).getEnd();

            List<Interval> curOverlapping = curVector.getOverlapping(commitStart, commitEnd);
            List<Interval> nextOverlapping = nextVector.getOverlapping(commitStart, commitEnd);

            LinkedList<Interval> toCommitAcc = new LinkedList<Interval>();
            LinkedList<Interval> toAbortAcc = new LinkedList<Interval>();
            boolean failed = ECPolicy.calculateIntervalsToCommitAbort(intervals, null, curOverlapping, nextOverlapping,
                    toCommitAcc, toAbortAcc);

            // Signal to go to reconstruct if the vector can not be fully committed.
            // Note: this is actually to rigorous, since we would only have to care about the overlapping intervals with
            // the current object read range. But to keep it simple and uniform we require the whole commit interval to
            // be present. This (faulty) behavior is also present in the commit vector routines.
            if (failed) {
                callback.ecReadDataComplete(null, true, null);
                return;
            }

            // Commit or abort intervals from the next vector.
            for (Interval interval : toCommitAcc) {
                commitECData(fileId, fi, interval);
            }

            if (!ignoreAbort) {
                for (Interval interval : toAbortAcc) {
                    abortECData(fileId, fi, interval);
                }
            }

            ObjectInformation result = layout.readObject(fileId, fi, objNo, offset, length, objVer);
            // TODO (jdillmann): Add Metadata?
            // result.setChecksumInvalidOnOSD(checksumInvalidOnOSD);
            // result.setLastLocalObjectNo(lastLocalObjectNo);
            // result.setGlobalLastObjectNo(globalLastObjectNo);
            callback.ecReadDataComplete(result, false, null);


            // FIXME (jdillmann): Won't return the correct error if the read is outdated.

            //
            // Interval emptyInterval = ObjectInterval.empty(commitStart, commitEnd);
            //
            // Iterator<Interval> curIt = curOverlapping.iterator();
            // Iterator<Interval> nextIt = nextOverlapping.iterator();
            // Interval curInterval = curIt.hasNext() ? curIt.next() : emptyInterval;
            // Interval nextInterval = nextIt.hasNext() ? nextIt.next() : emptyInterval;
            //
            //
            // for (Interval interval : intervals) {
            // int iOffset = ECHelper.safeLongToInt(interval.getStart() - objOffset);
            // int iLength = ECHelper.safeLongToInt(interval.getEnd() - interval.getStart());
            // int version = 1;
            //
            // // Advance to the next interval that could be a possible match
            // // or set an empty interval as a placeholder
            // while (curInterval.getEnd() <= interval.getStart() && curIt.hasNext()) {
            // curInterval = curIt.next();
            // }
            // if (curInterval.getEnd() <= interval.getStart()) {
            // curInterval = emptyInterval;
            // }
            //
            // // Advance to the next interval that could be a possible match
            // // or set an empty interval as a placeholder
            // while (nextInterval.getEnd() <= interval.getStart() && nextIt.hasNext()) {
            // nextInterval = nextIt.next();
            // }
            // if (nextInterval.getEnd() <= interval.getStart()) {
            // nextInterval = emptyInterval;
            // }
            //
            // // equals: start, end, version, id
            // // Note: not opStart/opEnd
            // if (interval.equals(curInterval)) {
            // // FINE: do something
            // readToBuf(fileId, fi, objNo, iOffset, iLength, version, data);
            //
            // } else {
            // if (interval.overlaps(curInterval)
            // && interval.getVersion() < curInterval.getVersion()) {
            // // ERROR => non fatal, read request has to be retried or just abort
            // String errorMsg = String
            // .format("Could not read fileId '%s' due to a concurrent write operation.", fileId);
            // ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO,
            // POSIXErrno.POSIX_ERROR_EAGAIN, errorMsg);
            // callback.ecReadDataComplete(null, false, error);
            // return;
            // }
            //
            // if (interval.equals(nextInterval)) {
            // // FINE
            // readToBuf(fileIdNext, fi, objNo, iOffset, iLength, version, data);
            // } else {
            // // NOT FOUND! => reconstruction required
            // callback.ecReadDataComplete(null, true, null);
            // return;
            // }
            // }
            // }
            //
            // data.position(0);
            // ObjectInformation result = new ObjectInformation(ObjectStatus.EXISTS, data, 0);
            // // TODO (jdillmann): Add Metadata?
            // // result.setChecksumInvalidOnOSD(checksumInvalidOnOSD);
            // // result.setLastLocalObjectNo(lastLocalObjectNo);
            // // result.setGlobalLastObjectNo(globalLastObjectNo);
            // callback.ecReadDataComplete(result, false, null);

        } catch (IOException ex) {
            // BufferPool.free(data);
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecReadDataComplete(null, false, error);
        }
        
    }

    public void processReadParity(final StageRequest rq) {
        final ECReadParityCallback callback = (ECReadParityCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        final long stripeNo = (Long) rq.getArgs()[2];
        final int offset = (Integer) rq.getArgs()[3];
        final int length = (Integer) rq.getArgs()[4];
        final List<Interval> intervals = (List<Interval>) rq.getArgs()[5];
        final boolean ignoreAbort = (Boolean) rq.getArgs()[6];

        assert (!intervals.isEmpty());
        assert (offset + length <= sp.getStripeSizeForObject(0));

        final String fileIdCode = fileId + FILEID_CODE_SUFFIX;

        long objVer = 1;


        // ReusableBuffer data = BufferPool.allocate(length);

        try {


            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            IntervalVector curVector = fi.getECCurVector();
            IntervalVector nextVector = fi.getECNextVector();

            long commitStart = intervals.get(0).getStart();
            long commitEnd = intervals.get(intervals.size() - 1).getEnd();

            List<Interval> curOverlapping = curVector.getOverlapping(commitStart, commitEnd);
            List<Interval> nextOverlapping = nextVector.getOverlapping(commitStart, commitEnd);

            LinkedList<Interval> toCommitAcc = new LinkedList<Interval>();
            LinkedList<Interval> toAbortAcc = new LinkedList<Interval>();
            boolean failed = ECPolicy.calculateIntervalsToCommitAbort(intervals, null, curOverlapping, nextOverlapping,
                    toCommitAcc, toAbortAcc);

            // Signal to go to reconstruct if the vector can not be fully committed.
            // Note: this is actually to rigorous, since we would only have to care about the overlapping intervals with
            // the current object read range. But to keep it simple and uniform we require the whole commit interval to
            // be present. This (faulty) behavior is also present in the commit vector routines.
            if (failed) {
                callback.ecReadParityComplete(null, true, null);
                return;
            }

            // Commit or abort intervals from the next vector.
            for (Interval interval : toCommitAcc) {
                commitECDelta(fileId, fi, interval);
            }
            if (!ignoreAbort) {
                for (Interval interval : toAbortAcc) {
                    abortECDelta(fileId, fi, interval);
                }
            }

            ObjectInformation result = layout.readObject(fileIdCode, fi, stripeNo, offset, length, objVer);
            // TODO (jdillmann): Add Metadata?
            // result.setChecksumInvalidOnOSD(checksumInvalidOnOSD);
            // result.setLastLocalObjectNo(lastLocalObjectNo);
            // result.setGlobalLastObjectNo(globalLastObjectNo);
            callback.ecReadParityComplete(result, false, null);


            // FIXME (jdillmann): Won't return the correct error if the read is outdated.
        } catch (IOException ex) {
            // BufferPool.free(data);
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecReadParityComplete(null, false, error);
        }

    }

    // void readToBuf(String fileId, FileMetadata fi, long objNo, int offset, int length, int version, ReusableBuffer
    // data)
    // throws IOException {
    //
    // // TODO (jdillmann): Add read to existing buffer method to StorageLayout
    // ObjectInformation objInfo = layout.readObject(fileId, fi, objNo, offset, length, version);
    //
    // data.position(offset);
    // assert (data.remaining() >= length);
    //
    // if (objInfo.getStatus() == ObjectStatus.EXISTS) {
    // ReusableBuffer readBuf = objInfo.getData();
    // readBuf.position(0);
    // data.put(readBuf);
    // BufferPool.free(readBuf);
    // }
    //
    // // Fill with zeros
    // int remaining = (offset + length) - data.position();
    // for (int i = 0; i < remaining; i++) {
    // data.put((byte) 0);
    // }
    // }


    void commitECData(String fileId, FileMetadata fi, Interval interval) throws IOException {
        StripingPolicyImpl sp = fi.getStripingPolicy();
        assert (interval.isOpComplete());
        long intervalStartOffset = interval.getStart();
        long intervalEndOffset = interval.getEnd() - 1; // end is exclusive

        long startObjNo = sp.getObjectNoForOffset(intervalStartOffset);
        long endObjNo = sp.getObjectNoForOffset(intervalEndOffset);

        long objVer = 1;
        // FIXME (jdillmann): Decide if/when sync should be used
        boolean sync = false;

        boolean consistent = true;
        try {
            Iterator<Long> objNoIt = sp.getObjectsOfOSD(sp.getRelativeOSDPosition(), startObjNo, endObjNo);
            while (objNoIt.hasNext()) {
                Long objNo = objNoIt.next();

                long startOff = Math.max(sp.getObjectStartOffset(objNo), intervalStartOffset);
                long endOff = Math.min(sp.getObjectEndOffset(objNo), intervalEndOffset);

                String fileIdNext = fileId + FILEID_NEXT_SUFFIX;
                int offset = (int) (startOff - sp.getObjectStartOffset(objNo));
                int length = (int) (endOff - startOff) + 1; // The byte from the end offset has to be included.



                // TODO (jdillmann): Allow to copy data directly between files (bypass Buffer).
                ObjectInformation obj = layout.readObject(fileIdNext, fi, objNo, offset, length, objVer);

                ReusableBuffer buf = null; // will be freed by writeObject
                if (obj.getStatus() == ObjectStatus.EXISTS) {
                    buf = obj.getData();

                    int resultLength = buf.capacity();
                    if (resultLength < length) {
                        if (!buf.enlarge(length)) {
                            ReusableBuffer tmp = BufferPool.allocate(length);
                            tmp.put(buf);

                            BufferPool.free(buf);
                            buf = tmp;
                        }
                    }

                    buf.position(resultLength);
                } else {
                    buf = BufferPool.allocate(length);
                }

                while (buf.hasRemaining()) {
                    buf.put((byte) 0);
                }

                buf.flip();
                layout.writeObject(fileId, fi, buf, objNo, offset, objVer, sync, false);
                consistent = false;

                // Delete completely committed intervals.
                if (intervalStartOffset <= sp.getObjectStartOffset(objNo)
                        && intervalEndOffset >= sp.getObjectEndOffset(objNo)) {
                    layout.deleteObject(fileIdNext, fi, objNo, objVer);
                }
            }

            // Finally append the interval to the cur vector and "remove" it from the next vector
            layout.setECIntervalVector(fileId, Arrays.asList(interval), false, true);
            fi.getECCurVector().insert(interval);

            // Remove the interval by overwriting it with an empty interval
            Interval empty = ObjectInterval.empty(interval);
            layout.setECIntervalVector(fileId, Arrays.asList(empty), true, true);
            fi.getECNextVector().insert(empty);
            consistent = true;

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }
            throw ex;
        }

        // FIXME (jdillmann): truncate?
    }

    void commitECDelta(String fileId, FileMetadata fi, Interval interval) throws IOException {
        StripingPolicyImpl sp = fi.getStripingPolicy();
        assert (interval.isOpComplete());
        long intervalStartOffset = interval.getStart();
        long intervalEndOffset = interval.getEnd() - 1; // end is exclusive

        long startObjNo = sp.getObjectNoForOffset(intervalStartOffset);
        long endObjNo = sp.getObjectNoForOffset(intervalEndOffset);

        long firstStripNo = sp.getRow(startObjNo);
        long lastStripeNo = sp.getRow(endObjNo);

        String fileIdDelta = fileId + FILEID_DELTA_SUFFIX;
        String fileIdCode = fileId + FILEID_CODE_SUFFIX;

        long objVer = 1;
        // FIXME (jdillmann): Decide if/when sync should be used
        boolean sync = false;

        // Read from the complete code object (could be optimized)
        int codeOff = 0;
        int codeLength = sp.getStripeSizeForObject(startObjNo);

        ReusableBuffer codeBuf = null;
        boolean consistent = true;
        try {
            for (long stripeNo = firstStripNo; stripeNo <= lastStripeNo; stripeNo++) {
                long firstObjInStripe = stripeNo * sp.getWidth();
                long lastObjInStripe = (stripeNo + 1) * sp.getWidth() - 1;

                long stripeStartObjNo = Math.max(firstObjInStripe, startObjNo);
                long stripeEndObjNo = Math.min(lastObjInStripe, endObjNo);

                // Read the code for the current stripe
                codeBuf = readCode(fileId, fi, stripeNo, codeOff, codeLength, objVer);

                // For each delta object belonging to this stripe/operation calculate an XOR diff
                for (long objNo = stripeStartObjNo; objNo <= stripeEndObjNo; objNo++) {
                    long startOff = Math.max(sp.getObjectStartOffset(objNo), intervalStartOffset);
                    long endOff = Math.min(sp.getObjectEndOffset(objNo), intervalEndOffset);

                    int offset = (int) (startOff - sp.getObjectStartOffset(objNo));
                    int length = (int) (endOff - startOff) + 1; // The byte from the end offset has to be included.

                    // Read the current delta object and XOR it with the current code data
                    ObjectInformation deltaObj = layout.readObject(fileIdDelta, fi, objNo, offset, length, objVer);

                    ReusableBuffer deltaBuf = null;
                    int deltaLength = 0;
                    if (deltaObj.getStatus() == ObjectStatus.EXISTS) {
                        deltaBuf = deltaObj.getData();
                        deltaLength = deltaBuf.capacity();
                    }

                    if (deltaLength > 0) {
                        codeBuf.position(offset);
                        ECHelper.xor(codeBuf, deltaBuf);
                        codeBuf.position(0);
                    }
                    BufferPool.free(deltaBuf);
                }

                // Write the updated code back
                codeBuf.position(0);
                layout.writeObject(fileIdCode, fi, codeBuf, stripeNo, codeOff, objVer, sync, false);
                consistent = false;
                codeBuf = null;
            }


            // Finally append the interval to the cur vector and "remove" it from the next vector
            layout.setECIntervalVector(fileId, Arrays.asList(interval), false, true);
            fi.getECCurVector().insert(interval);

            // Remove the interval by overwriting it with an empty interval
            Interval empty = ObjectInterval.empty(interval);
            layout.setECIntervalVector(fileId, Arrays.asList(empty), true, true);
            fi.getECNextVector().insert(empty);
            consistent = true;

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }
            throw ex;
        } finally {
            if (codeBuf != null) {
                BufferPool.free(codeBuf);
            }
        }

        // Delete completely committed deltas.
        for (long objNo = startObjNo; objNo <= endObjNo; objNo++) {
            if (intervalStartOffset <= sp.getObjectStartOffset(objNo)
                    && intervalEndOffset >= sp.getObjectEndOffset(objNo)) {
                layout.deleteObject(fileIdDelta, fi, objNo, objVer);
            }
        }

        // FIXME (jdillmann): truncate?
    }


    /**
     * Reads the code object from offset and appends 0 until length bytes are stored.
     * 
     * @param fileId
     * @param fi
     * @param stripeNo
     * @param offset
     * @param length
     * @param version
     * @return
     * @throws IOException
     */
    private ReusableBuffer readCode(String fileId, FileMetadata fi, long stripeNo, int offset, int length, long version)
            throws IOException {
        String fileIdCode = fileId + FILEID_CODE_SUFFIX;
        ObjectInformation codeObj = layout.readObject(fileIdCode, fi, stripeNo, offset, length, version);

        // Read the current code data and append 0 until the buffer is full (codeLength)
        ReusableBuffer codeBuf = ECHelper.zeroPad(codeObj.getData(), length);
        BufferPool.free(codeObj.getData());

        return codeBuf;
    }

    void abortECData(String fileId, FileMetadata fi, Interval interval) throws IOException {
        StripingPolicyImpl sp = fi.getStripingPolicy();
        long intervalStartOffset = interval.getStart();
        long intervalEndOffset = interval.getEnd() - 1; // end is exclusive
        long startObjNo = sp.getObjectNoForOffset(intervalStartOffset);
        long endObjNo = sp.getObjectNoForOffset(intervalEndOffset);
        long objVer = 1;

        String fileIdNext = fileId + FILEID_NEXT_SUFFIX;

        boolean consistent = true;
        try {
            Iterator<Long> objNoIt = sp.getObjectsOfOSD(sp.getRelativeOSDPosition(), startObjNo, endObjNo);
            while (objNoIt.hasNext()) {
                Long objNo = objNoIt.next();

                // Delete the completely aborted objects
                if (intervalStartOffset <= sp.getObjectStartOffset(objNo) && intervalEndOffset >= sp.getObjectEndOffset(objNo)) {
                    layout.deleteObject(fileIdNext, fi, objNo, objVer);
                    consistent = false;
                }
                // FIXME (jdillmann): Delete also, if the remaining partials are not set
                // => test on slice or iv first and iv last
                // TODO (jdillmann): Maybe overwrite partials with zeros.
            }

            // Remove the interval by overwriting it with an empty interval
            Interval empty = ObjectInterval.empty(interval);
            layout.setECIntervalVector(fileId, Arrays.asList(interval), true, true);
            fi.getECNextVector().insert(empty);
            // FIXME (jdillmann): Truncate the next vector if the interval start = 0 and end >= lastEnd
            // layout.setECIntervalVector(fileId, Collections.<Interval> emptyList(), true, false);
            consistent = true;

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }
            throw ex;
        }
    }

    void abortECDelta(String fileId, FileMetadata fi, Interval interval) throws IOException {
        StripingPolicyImpl sp = fi.getStripingPolicy();
        long intervalStartOffset = interval.getStart();
        long intervalEndOffset = interval.getEnd() - 1; // end is exclusive
        long startObjNo = sp.getObjectNoForOffset(intervalStartOffset);
        long endObjNo = sp.getObjectNoForOffset(intervalEndOffset);
        long objVer = 1;

        String fileIdDelta = fileId + FILEID_DELTA_SUFFIX;

        boolean consistent = true;
        try {

            for (long objNo = startObjNo; objNo <= endObjNo; objNo++) {
                // Delete the completely aborted objects
                if (intervalStartOffset <= sp.getObjectStartOffset(objNo)
                        && intervalEndOffset >= sp.getObjectEndOffset(objNo)) {
                    layout.deleteObject(fileIdDelta, fi, objNo, objVer);
                    consistent = false;
                }
                // FIXME (jdillmann): Delete also, if the remaining partials are not set
                // => test on slice or iv first and iv last
                // TODO (jdillmann): Maybe overwrite partials with zeros.
            }

            // Remove the interval by overwriting it with an empty interval
            Interval empty = ObjectInterval.empty(interval);
            layout.setECIntervalVector(fileId, Arrays.asList(interval), true, true);
            fi.getECNextVector().insert(empty);
            // FIXME (jdillmann): Truncate the next vector if the interval start = 0 and end >= lastEnd
            // layout.setECIntervalVector(fileId, Collections.<Interval> emptyList(), true, false);
            consistent = true;

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }
            throw ex;
        }
    }

}
