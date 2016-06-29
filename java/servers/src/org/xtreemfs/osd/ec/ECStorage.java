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

import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
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
import org.xtreemfs.osd.stages.StorageStage.ECWriteDataCallback;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.osd.storage.StorageLayout;

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
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);

            List<Interval> toCommit = new LinkedList<Interval>();
            List<Interval> toAbort = new LinkedList<Interval>();

            boolean failed = calculateIntervalsToCommitAbort(commitIntervals, fi.getECCurVector().serialize(),
                    fi.getECNextVector().serialize(), toCommit, toAbort);

            if (failed) {
                callback.ecCommitVectorComplete(true, null);
                return;
            }


            for (Interval interval : toCommit) {
                commitECData(fileId, fi, interval);
            }

            // FIXME (jdillmann): Also delete the EC data
            AVLTreeIntervalVector emptyNextVector = new AVLTreeIntervalVector();
            layout.setECIntervalVector(fileId, emptyNextVector.serialize(), true, false);
            fi.setECNextVector(emptyNextVector);

            // FIXME (jdillmann): truncate cur?

            callback.ecCommitVectorComplete(false, null);

        } catch (IOException ex) {
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecCommitVectorComplete(false, error);
            return;
        }

    }

    public void processWriteData(final StageRequest rq) {
        final ECWriteDataCallback callback = (ECWriteDataCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        final long objectNo = (Long) rq.getArgs()[2];
        final int offset = (Integer) rq.getArgs()[3];
        final Interval reqInterval = (Interval) rq.getArgs()[4];
        final List<Interval> commitIntervals = (List<Interval>) rq.getArgs()[5];
        final ReusableBuffer data = (ReusableBuffer) rq.getArgs()[6];

        boolean consistent = true;
        try {
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            IntervalVector curVector = fi.getECCurVector();
            IntervalVector nextVector = fi.getECNextVector();

            long opStart = reqInterval.getOpStart();
            long opEnd = reqInterval.getOpEnd();

            // FIXME(jdillmann): A single write may never cross stripe boundaries

            // Get this nodes interval vectors and check if the vector this operation is based on can be fully committed
            long commitStart = !commitIntervals.isEmpty() ? commitIntervals.get(0).getOpStart() : 0;
            long commitEnd = !commitIntervals.isEmpty() ? commitIntervals.get(commitIntervals.size() - 1).getOpEnd() : 0;
            // FIXME (jdillmann): Check if this is correct
            List<Interval> curVecIntervals = curVector.getOverlapping(commitStart, commitEnd);
            List<Interval> nextVecIntervals = nextVector.getOverlapping(commitStart, commitEnd);

            LinkedList<Interval> toCommitAcc = new LinkedList<Interval>();
            LinkedList<Interval> toAbortAcc = new LinkedList<Interval>();
            boolean failed = calculateIntervalsToCommitAbort(commitIntervals, curVecIntervals, nextVecIntervals,
                    toCommitAcc, toAbortAcc);

            // Signal to go to reconstruct if the vector can not be fully committed.
            if (failed) {
                callback.ecWriteDataComplete(null, true, null);
                return;
            }

            // Commit or abort intervals from the next vector.
            for (Interval interval : toCommitAcc) {
                commitECData(fileId, fi, interval);
            }
            for (Interval interval : toAbortAcc) {
                abortECData(fileId, fi, interval);
            }

            // Check operation boundaries.
            // FIXME (jdillmann): Check for exclusive ends
            long dataStart = sp.getObjectStartOffset(objectNo) + offset;
            long dataEnd = dataStart + data.capacity();
            // The data range has to be within the request interval and may not cross chunk boundaries
            assert (dataStart >= reqInterval.getStart() && dataEnd <= reqInterval.getEnd());
            assert (dataEnd - dataStart <= sp.getStripeSizeForObject(objectNo));

            // Write the data
            String fileIdNext = fileId + ".next";
            long version = 1;
            // FIXME (jdillmann): Decide if/when sync should be used
            boolean sync = false;
            layout.writeObject(fileIdNext, fi, data.createViewBuffer(), objectNo, offset, version, sync, false);
            consistent = false;

            // Store the vector
            layout.setECIntervalVector(fileId, Arrays.asList(reqInterval), true, true);
            fi.getECNextVector().insert(reqInterval);
            consistent = true;

            // Generate diff between the current data and the newly written data.
            byte[] diff = new byte[data.capacity()];
            ObjectInformation objInfo = layout.readObject(fileId, fi, objectNo, offset, data.capacity(), version);
            if (objInfo.getStatus() == ObjectStatus.PADDING_OBJECT
                    || objInfo.getStatus() == ObjectStatus.DOES_NOT_EXIST 
                    || objInfo.getData() == null) {
                // There is no current data: the new data is the diff.
                data.position(0);
                data.get(diff);
            } else {
                ReusableBuffer curData = objInfo.getData();
                assert (curData.capacity() == data.capacity());

                // byte-wise diff between cur and next data
                // TODO (jdillmann): Benchmark if getting the buffers as byte[] would be faster
                data.position(0);
                curData.position(0);
                for (int i = 0; i < data.capacity(); i++) {
                    diff[i] = (byte) (data.get() ^ curData.get());
                }
                
                BufferPool.free(curData);
            }

            // Return the diff buffer to the caller
            ReusableBuffer diffBuffer = ReusableBuffer.wrap(diff);
            callback.ecWriteDataComplete(diffBuffer, false, null);

        } catch (IOException ex) {
            if (!consistent) {
                // FIXME (jdillmann): Inform in detail about critical error
                Logging.logError(Logging.LEVEL_CRIT, this, ex);
            }

            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString(), ex);
            callback.ecWriteDataComplete(null, false, error);
        } finally {
            BufferPool.free(data);
        }
    }

    /**
     * Checks for each interval in commitIntervals if it is present in the curVecIntervals or the nextVecIntervals.<br>
     * If it is present in curVecIntervals, overlapping intervals from nextVecIntervals will be added to toAbortAcc.<br>
     * If it is present in nextVecIntervals, it is added to the toCommitAcc accumulator.<br>
     * If it isn't present in neither, false is returned immediately.<br>
     * If it's version is lower then an overlapping one from curVecIntervals, an exception is thrown.
     * 
     * @param commitIntervals
     *            List of intervals to commit.
     * @param curVecIntervals
     *            List of intervals from the currently stored data. Maybe sliced to the commitIntervals range.
     * @param nextVecIntervals
     *            List of intervals from the next buffer. Maybe sliced to the commitIntervals range.
     * @param toCommitAcc
     *            Accumulator used to return the intervals to be committed from the next buffer.
     * @param toAbortAcc
     *            Accumulator used to return the intervals to be aborted from the next buffer.
     * @return false if an interval from commitIntervals can not be found. true otherwise.
     * @throws IOException
     *             if an interval from commitIntervals contains a lower version version, then an overlapping interval
     *             from curVecIntervals.
     */
    static boolean calculateIntervalsToCommitAbort(List<Interval> commitIntervals, List<Interval> curVecIntervals,
            List<Interval> nextVecIntervals, List<Interval> toCommitAcc, List<Interval> toAbortAcc) throws IOException {
        
        long commitStart = !commitIntervals.isEmpty() ? commitIntervals.get(0).getOpStart() : 0;
        long commitEnd = !commitIntervals.isEmpty() ? commitIntervals.get(commitIntervals.size() - 1).getOpEnd() : 0;

        // If there is nothing to commit, abort every interval in next and return
        if (commitEnd + commitStart == 0) {
            toAbortAcc.addAll(nextVecIntervals);
            return false;
        }

        Interval emptyInterval = ObjectInterval.empty(commitStart, commitEnd);

        Iterator<Interval> curIt = curVecIntervals.iterator();
        Iterator<Interval> nextIt = nextVecIntervals.iterator();

        Interval curInterval = curIt.hasNext() ? curIt.next() : emptyInterval;
        Interval nextInterval = nextIt.hasNext() ? nextIt.next() : emptyInterval;
        
        // Check for every commit interval if it is available.
        for (Interval commitInterval : commitIntervals) {
            // Advance to the next interval that could be a possible match
            // or set an empty interval as a placeholder
            while (curInterval.getEnd() <= commitInterval.getStart() && curIt.hasNext()) {
                curInterval = curIt.next();
            }
            if (curInterval.getEnd() <= commitInterval.getStart()) {
                curInterval = emptyInterval;
            }

            // Advance to the next interval that could be a possible match
            // or set an empty interval as a placeholder
            while (nextInterval.getEnd() <= commitInterval.getStart() && nextIt.hasNext()) {
                nextInterval = nextIt.next();
            }
            if (nextInterval.getEnd() <= commitInterval.getStart()) {
                nextInterval = emptyInterval;
            }

            // Check if the interval exists in the current vector.
            // It could be, that the interval in the current vector is larger then the current one, because it has
            // not been split yet.
            // req:  |--1--|-2-|     or   |-2-|--1--|  or |-1-|-2-|-1-|
            // cur:  |----1----|          |----1----|     |-----1-----|
            // next: |     |-2-|          |-2-|     |     |   |-2-|   |
            // It is obvious, that intervals from next must have matching start/end positions also.

            if (commitInterval.equalsVersionId(curInterval)) {
                // If the version and the id match, they have to overlap
                assert (commitInterval.overlaps(curInterval));

                // Since the commitInterval is already in the curVector, overlapping intervals from next have to be
                // aborted.
                while (commitInterval.overlaps(nextInterval)) {
                    if (!nextInterval.isEmpty()) {
                        // ABORT/INVALIDATE
                        toAbortAcc.add(nextInterval);
                    }
                    
                    // Advance the nextInterval iterator or set an empty interval as a placeholder and stop the loop
                    if (nextIt.hasNext()) {
                        nextInterval = nextIt.next();
                    } else {
                        nextInterval = emptyInterval;
                        break;
                    }
                }

                
            } else {
                if (commitInterval.getVersion() < curInterval.getVersion() && commitInterval.overlaps(curInterval)) {
                    // FAILED (should never happen)
                    // TODO (jdillmann): Log with better message.
                    throw new XtreemFSException("request interval is older then the current interval");
                }

                if (commitInterval.equals(nextInterval)) {
                    // COMMIT nextInterval
                    toCommitAcc.add(nextInterval);
                } else {
                    // FAILED (go into recovery)
                    return true;
                }
            }
        }

        return false;
    }

    void commitECData(String fileId, FileMetadata fi, Interval interval) throws IOException {
        StripingPolicyImpl sp = fi.getStripingPolicy();
        assert (interval.isOpComplete());
        long intervalStartOffset = interval.getStart();
        long intervalEndOffset = interval.getEnd() - 1; // end is exclusive

        long startObjNo = sp.getObjectNoForOffset(intervalStartOffset);
        long endObjNo = sp.getObjectNoForOffset(intervalEndOffset);

        boolean consistent = true;
        try {
            Iterator<Long> objNoIt = sp.getObjectsOfOSD(sp.getRelativeOSDPosition(), startObjNo, endObjNo);
            while (objNoIt.hasNext()) {
                Long objNo = objNoIt.next();

                long startOff = Math.max(sp.getObjectStartOffset(objNo), intervalStartOffset);
                long endOff = Math.min(sp.getObjectEndOffset(objNo), intervalEndOffset);

                String fileIdNext = fileId + ".next";
                int offset = (int) (startOff - sp.getObjectStartOffset(objNo));
                int length = (int) (endOff - startOff) + 1; // The byte from the end offset has to be included.
                int objVer = 1;
                // FIXME (jdillmann): Decide if/when sync should be used
                boolean sync = false;


                // TODO (jdillmann): Allow to copy data directly between files (bypass Buffer).
                ObjectInformation obj = layout.readObject(fileIdNext, fi, objNo, offset, length, objVer);

                ReusableBuffer buf; // will be freed by writeObject
                if (obj.getStatus() == ObjectStatus.EXISTS) {
                    buf = obj.getData();
                } else {
                    byte[] zeros = new byte[length];
                    buf = ReusableBuffer.wrap(zeros);
                }

                // FIXME (jdillmann): Handle padding objects etc.
                layout.writeObject(fileId, fi, buf, objNo, offset, objVer, sync, false);
                consistent = false;

            }

            // Finally append the interval to the cur vector and "remove" it from the next vector
            fi.getECCurVector().insert(interval);
            layout.setECIntervalVector(fileId, Arrays.asList(interval), false, true);

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

    void abortECData(String fileId, FileMetadata fi, Interval interval) throws IOException {
        StripingPolicyImpl sp = fi.getStripingPolicy();
        assert (interval.isOpComplete());
        long intervalStartOffset = interval.getStart();
        long intervalEndOffset = interval.getEnd() - 1; // end is exclusive
        long startObjNo = sp.getObjectNoForOffset(intervalStartOffset);
        long endObjNo = sp.getObjectNoForOffset(intervalEndOffset);
        int objVer = 1;

        boolean consistent = true;
        try {
            Iterator<Long> objNoIt = sp.getObjectsOfOSD(sp.getRelativeOSDPosition(), startObjNo, endObjNo);
            while (objNoIt.hasNext()) {
                Long objNo = objNoIt.next();

                // Delete the completely aborted objects
                if (intervalStartOffset <= sp.getObjectStartOffset(objNo) && intervalEndOffset >= sp.getObjectEndOffset(objNo)) {
                    layout.deleteObject(fileId, fi, objNo, objVer);
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
