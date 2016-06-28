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
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECInternalOperationCallback;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.stages.StorageStage.ECWriteDataCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_dataRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_dataResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/** FIXME (jdillmann): DOC */
public class ECWriteOperation extends OSDOperation {
    final static public int PROC_ID = OSDServiceConstants.PROC_ID_XTREEMFS_EC_WRITE_DATA;

    // FIXME (jdillmann): Is it required to check the cap?
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public ECWriteOperation(OSDRequestDispatcher master) {
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
        final xtreemfs_ec_write_dataRequest args = (xtreemfs_ec_write_dataRequest) rq.getRequestArgs();

        final String fileId = args.getFileId();
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        final long opId = args.getOpId();
        final long objNo = args.getObjectNumber();
        ReusableBuffer viewBuffer = rq.getRPCRequest().getData().createViewBuffer();

        Interval stripeInterval = new ProtoInterval(args.getStripeInterval());

        // Create the IntervalVector from the message
        final List<Interval> commitIntervals = new ArrayList<Interval>(args.getCommitIntervalsCount());
        for (IntervalMsg msg : args.getCommitIntervalsList()) {
            commitIntervals.add(new ProtoInterval(msg));
        }

        master.getStorageStage().ecWritedata(fileId, sp, args.getObjectNumber(), args.getOffset(), stripeInterval,
                commitIntervals, viewBuffer, rq, new ECWriteDataCallback() {

                    @Override
                    public void ecWriteDataComplete(ReusableBuffer diff, boolean needsReconstruct,
                            ErrorResponse error) {
                        if (error != null) {
                            rq.sendError(error);
                            BufferPool.free(diff);
                        } else if (needsReconstruct) {
                            // FIXME (jdillmann): Trigger reconstruction if not complete.
                            // FIXME (jdillmann): Add response field = needReconstruction or error message type
                            rq.sendSuccess(buildResponse(opId, objNo), null);
                            BufferPool.free(diff);
                        } else {
                            rq.sendSuccess(buildResponse(opId, objNo), null);

                            // send to coding devices, then clear the buffer on the responseAvailable CB
                            // FIXME (jdillmann): clear diff buffer?
                            // BufferPool.free(diff); // even though i know it is only wrapped ?
                        }
                    }
                });

    }


    @Override
    public void startInternalEvent(Object[] args) {
        final String fileId = (String) args[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) args[1];
        // final IntervalVector commitVector = (IntervalVector) args[2];
        final ECInternalOperationCallback<xtreemfs_ec_write_dataResponse> callback = (ECInternalOperationCallback<xtreemfs_ec_write_dataResponse>) args[3];

    }

    xtreemfs_ec_write_dataResponse buildResponse(long opId, long objNo) {
        xtreemfs_ec_write_dataResponse.Builder responseB = xtreemfs_ec_write_dataResponse.newBuilder();
        responseB.setOpId(opId).setObjectNumber(objNo);
        // if (error != null) {
        // responseB.setError(error);
        // }
        return responseB.build();
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_write_dataRequest rpcrq = (xtreemfs_ec_write_dataRequest) rq.getRequestArgs();
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
}
