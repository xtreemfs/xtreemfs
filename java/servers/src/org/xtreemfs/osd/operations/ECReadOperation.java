/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.operations;

import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.InternalOperationCallback;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.stages.StorageStage.ECReadDataCallback;
import org.xtreemfs.osd.stages.StorageStage.ECReadParityCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_readRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_readResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/** FIXME (jdillmann): DOC */
public class ECReadOperation extends OSDOperation {
    final static public int PROC_ID = OSDServiceConstants.PROC_ID_XTREEMFS_EC_READ;

    // FIXME (jdillmann): Is it required to check the cap?
    final String            sharedSecret;
    final ServiceUUID       localUUID;

    public ECReadOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return PROC_ID;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_ec_readRequest args = (xtreemfs_ec_readRequest) rq.getRequestArgs();

        final String fileId = args.getFileId();
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        final long objNo = args.getObjectNumber();
        final int offset = args.getOffset();
        final int length = args.getLength();

        final boolean ignoreAbort = args.getIgnoreAbort();


        // Create the IntervalVector from the message
        final List<Interval> intervals = new ArrayList<Interval>(args.getIntervalsCount());
        for (IntervalMsg msg : args.getIntervalsList()) {
            intervals.add(new ProtoInterval(msg));
        }

        if (intervals.isEmpty()) {
            long opStart = sp.getObjectStartOffset(objNo) + offset;
            long opEnd = opStart + length;

            Interval interval = ObjectInterval.empty(opStart, opEnd);
            intervals.add(interval);
        }

        int osdNo = sp.getRelativeOSDPosition();
        if (osdNo >= sp.getWidth()) {

            // The objNo is the stripeNo in case of parity devices
            long stripeNo = objNo;

            master.getStorageStage().ecReadParity(fileId, sp, stripeNo, offset, length, intervals, ignoreAbort, rq,
                    new ECReadParityCallback() {

                        @Override
                        public void ecReadParityComplete(ObjectInformation result, boolean needsReconstruct,
                                ErrorResponse error) {
                            if (error != null) {
                                rq.sendError(error);
                            } else if (needsReconstruct) {
                                // FIXME (jdillmann): Trigger reconstruction if not complete.
                                // FIXME (jdillmann): Add response field = needReconstruction or error message type
                                BufferPool.free(result.getData());
                                rq.sendSuccess(buildResponse(false, null), null);
                            } else {
                                master.objectSent();
                                if (result.getData() != null)
                                    master.dataSent(result.getData().capacity());

                                // FIXME (jdillmann): Could it make sense to set isLastObj?
                                InternalObjectData intObjData = result.getObjectData(false, offset, length);
                                rq.sendSuccess(buildResponse(false, intObjData), result.getData());
                            }
                        }
                    });
        } else {
            master.getStorageStage().ecReadData(fileId, sp, objNo, offset, length, intervals, ignoreAbort, rq,
                    new ECReadDataCallback() {

                        @Override
                        public void ecReadDataComplete(ObjectInformation result, boolean needsReconstruct,
                                ErrorResponse error) {
                            if (error != null) {
                                rq.sendError(error);
                            } else if (needsReconstruct) {
                                // FIXME (jdillmann): Trigger reconstruction if not complete.
                                // FIXME (jdillmann): Add response field = needReconstruction or error message type
                                BufferPool.free(result.getData());
                                rq.sendSuccess(buildResponse(false, null), null);
                            } else {
                                master.objectSent();
                                if (result.getData() != null)
                                    master.dataSent(result.getData().capacity());

                                // FIXME (jdillmann): Could it make sense to set isLastObj?
                                InternalObjectData intObjData = result.getObjectData(false, offset, length);
                                rq.sendSuccess(buildResponse(false, intObjData), result.getData());
                            }
                        }
                    });
        }
    }

    public void startLocalRequest(final String fileId, final StripingPolicyImpl sp, final long objNo, final int offset,
            final int length, final List<IntervalMsg> intervalMsgs, boolean ignoreAbort,
            final InternalOperationCallback<xtreemfs_ec_readResponse> callback) {

        // Create the IntervalVector from the message
        final List<Interval> intervals = new ArrayList<Interval>(intervalMsgs.size());
        for (IntervalMsg msg : intervalMsgs) {
            intervals.add(new ProtoInterval(msg));
        }

        if (intervals.isEmpty()) {
            long opStart = sp.getObjectStartOffset(objNo) + offset;
            long opEnd = opStart + length;

            Interval interval = ObjectInterval.empty(opStart, opEnd);
            intervals.add(interval);
        }

        int osdNo = sp.getRelativeOSDPosition();
        if (osdNo >= sp.getWidth()) {

            // The objNo is the stripeNo in case of parity devices
            long stripeNo = objNo;

            master.getStorageStage().ecReadParity(fileId, sp, stripeNo, offset, length, intervals, ignoreAbort, null,
                    new ECReadParityCallback() {

                        @Override
                        public void ecReadParityComplete(ObjectInformation result, boolean needsReconstruct,
                                ErrorResponse error) {
                            if (error != null) {
                                callback.localRequestFailed(error);
                            } else if (needsReconstruct) {
                                // FIXME (jdillmann): Trigger reconstruction if not complete.
                                // FIXME (jdillmann): Add response field = needReconstruction or error message type
                                if (result != null) {
                                    BufferPool.free(result.getData());
                                }
                                callback.localResultAvailable(buildResponse(true, null), null);
                            } else {
                                // FIXME (jdillmann): Could it make sense to set isLastObj?
                                InternalObjectData intObjData = result.getObjectData(false, offset, length);

                                callback.localResultAvailable(buildResponse(false, intObjData), result.getData());
                            }

                        }
                    });
        } else {
            master.getStorageStage().ecReadData(fileId, sp, objNo, offset, length, intervals, ignoreAbort, null,
                    new ECReadDataCallback() {
                        @Override
                        public void ecReadDataComplete(ObjectInformation result, boolean needsReconstruct,
                                ErrorResponse error) {
                            if (error != null) {
                                callback.localRequestFailed(error);
                            } else if (needsReconstruct) {
                                // FIXME (jdillmann): Trigger reconstruction if not complete.
                                // FIXME (jdillmann): Add response field = needReconstruction or error message type
                                if (result != null) {
                                    BufferPool.free(result.getData());
                                }
                                callback.localResultAvailable(buildResponse(true, null), null);
                            } else {
                                // FIXME (jdillmann): Could it make sense to set isLastObj?
                                InternalObjectData intObjData = result.getObjectData(false, offset, length);

                                callback.localResultAvailable(buildResponse(false, intObjData), result.getData());
                            }
                        }
                    });
        }
    }

    xtreemfs_ec_readResponse buildResponse(boolean needsReconstruction, InternalObjectData intObjData) {
        xtreemfs_ec_readResponse.Builder responseB = xtreemfs_ec_readResponse.newBuilder();
        responseB.setNeedsReconstruction(needsReconstruction);
        
        if (intObjData != null) {
            responseB.setObjectData(intObjData.getMetadata());
        }
        return responseB.build();
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_readRequest rpcrq = (xtreemfs_ec_readRequest) rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileId());
            rq.setCapability(new Capability(rpcrq.getFileCredentials().getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpcrq.getFileCredentials().getXlocs(), localUUID));

            return null;
        } catch (InvalidXLocationsException ex) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
        } catch (Throwable ex) {
            return ErrorUtils.getInternalServerError(ex);
        }
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }

    @Override
    public boolean bypassViewValidation() {
        // FIXME (jdillmann): What about views?
        return false;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
