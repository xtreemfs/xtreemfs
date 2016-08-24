/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECHelper;
import org.xtreemfs.osd.ec.ECReconstructionStage;
import org.xtreemfs.osd.ec.InternalOperationCallback;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.stages.StorageStage.ECWriteIntervalCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_intervalRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_intervalResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/** FIXME (jdillmann): DOC */
public class ECWriteIntervalOperation extends OSDOperation {
    final static public int PROC_ID = OSDServiceConstants.PROC_ID_XTREEMFS_EC_WRITE_INTERVAL;

    // FIXME (jdillmann): Is it required to check the cap?
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public ECWriteIntervalOperation(OSDRequestDispatcher master) {
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
        final xtreemfs_ec_write_intervalRequest args = (xtreemfs_ec_write_intervalRequest) rq.getRequestArgs();

        final String fileId = args.getFileId();
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        final long opId = args.getOpId();
        final long objNo = args.getObjectNumber();
        final int offset = args.getOffset();
        ReusableBuffer data = rq.getRPCRequest().getData();
        ReusableBuffer viewBuffer = (data != null) ? data.createViewBuffer() : null;

        final Interval stripeInterval = new ProtoInterval(args.getStripeInterval());
        
        final ECReconstructionStage reconstructor = master.getEcReconstructionStage();
        if (reconstructor.isInReconstruction(fileId)) {
            rq.sendError(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                    "File is in reconstruction"));
            return;
        }

        // Create the IntervalVector from the message
        final List<Interval> commitIntervals = new ArrayList<Interval>(args.getCommitIntervalsCount());
        for (IntervalMsg msg : args.getCommitIntervalsList()) {
            commitIntervals.add(new ProtoInterval(msg));
        }

        if (commitIntervals.isEmpty()) {
            Interval interval = ObjectInterval.empty(stripeInterval.getOpStart(), stripeInterval.getOpEnd());
            commitIntervals.add(interval);
        }

        master.getStorageStage().ecWriteInterval(fileId, sp, objNo, offset, stripeInterval,
                commitIntervals, viewBuffer, rq, new ECWriteIntervalCallback() {

                    @Override
                    public void ecWriteIntervalComplete(ReusableBuffer diff, boolean needsReconstruct,
                            ErrorResponse error) {
                        if (error != null) {
                            rq.sendError(error);
                            BufferPool.free(diff);
                        } else if (needsReconstruct) {
                            rq.sendSuccess(buildResponse(false), null);
                            BufferPool.free(diff);
                        } else {
                            if (diff != null) {
                                sendDiff(fileId, args.getFileCredentials(), rq.getLocationList(), sp, opId, objNo,
                                        offset, diff, stripeInterval, args.getCommitIntervalsList());
                            }
                            rq.sendSuccess(buildResponse(false), null);
                        }
                    }
                });

    }

    public void startLocalRequest(final String fileId, final StripingPolicyImpl sp, final long opId,
            final boolean hasData, final long objNo, final int offset, final IntervalMsg stripeIntervalMsg,
            final List<IntervalMsg> commitIntervalMsgs, final ReusableBuffer data,
            final FileCredentials fileCredentials, final XLocations xloc,
            final InternalOperationCallback<xtreemfs_ec_write_intervalResponse> callback) {

        final ReusableBuffer viewBuffer = (data != null) ? data.createViewBuffer() : null;

        final ECReconstructionStage reconstructor = master.getEcReconstructionStage();
        if (reconstructor.isInReconstruction(fileId)) {
            callback.localRequestFailed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                    POSIXErrno.POSIX_ERROR_EAGAIN, "File is in reconstruction"));
            return;
        }

        // Create Interval Objects from the message
        final Interval stripeInterval = new ProtoInterval(stripeIntervalMsg);
        final List<Interval> commitIntervals = new ArrayList<Interval>(commitIntervalMsgs.size());
        for (IntervalMsg msg : commitIntervalMsgs) {
            commitIntervals.add(new ProtoInterval(msg));
        }

        if (commitIntervals.isEmpty()) {
            Interval interval = ObjectInterval.empty(stripeInterval.getOpStart(), stripeInterval.getOpEnd());
            commitIntervals.add(interval);
        }

        master.getStorageStage().ecWriteInterval(fileId, sp, objNo, offset, stripeInterval, commitIntervals, viewBuffer,
                null,
                new ECWriteIntervalCallback() {

                    @Override
                    public void ecWriteIntervalComplete(ReusableBuffer diff, boolean needsReconstruct,
                            ErrorResponse error) {
                        if (error != null) {
                            callback.localRequestFailed(error);
                            BufferPool.free(diff);
                        } else if (needsReconstruct) {
                            // FIXME (jdillmann): Trigger reconstruction if not complete.
                            // FIXME (jdillmann): Add response field = needReconstruction or error message type
                            callback.localResultAvailable(buildResponse(true), null);
                            BufferPool.free(diff);
                        } else {
                            if (diff != null) {
                                sendDiff(fileId, fileCredentials, xloc, sp, opId, objNo, offset, diff, stripeInterval,
                                        commitIntervalMsgs);
                            }
                            callback.localResultAvailable(buildResponse(false), null);
                        }
                    }
                });
    }

    void sendDiff(final String fileId, final FileCredentials fileCredentials, final XLocations xloc,
            final StripingPolicyImpl sp, final long opId, final long objNo, final int offset, ReusableBuffer diff,
            final Interval stripeInterval, final List<IntervalMsg> commitIntervalMsgs) {

        long diffStart = sp.getObjectStartOffset(objNo) + offset;
        long diffEnd = diffStart + diff.capacity();
        ProtoInterval diffInterval = new ProtoInterval(diffStart, diffEnd,
                stripeInterval.getVersion(), stripeInterval.getId(), stripeInterval.getOpStart(), stripeInterval.getOpEnd());
        
        // Find the parity devices to update
        Replica r = xloc.getLocalReplica();
        List<ServiceUUID> osds = r.getOSDs();
        List<ServiceUUID> parityOSDs = new ArrayList<ServiceUUID>(sp.getParityWidth());
        for (int i = 0; i < sp.getParityWidth(); i++) {
            parityOSDs.add(osds.get(sp.getWidth() + i));
        }
        
        // FIXME (jdillmann): Which OSD client should i use?
        OSDServiceClient osdClient = master.getOSDClient();
        // master.getECMasterStage().getOSDClient();



        try {
            for (ServiceUUID parityOSD : parityOSDs) {
                @SuppressWarnings("unchecked")
                RPCResponse<emptyResponse> response = osdClient.xtreemfs_ec_write_diff(parityOSD.getAddress(),
                        RPCAuthentication.authNone, RPCAuthentication.userService, fileCredentials, fileId, opId, objNo,
                        offset, diffInterval.getMsg(), ProtoInterval.toProto(stripeInterval), commitIntervalMsgs,
                        diff.createViewBuffer());
                response.registerListener(ECHelper.emptyResponseListener);
            }
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_WARN, this, ex);
        } finally {
            BufferPool.free(diff);
        }
    }

    xtreemfs_ec_write_intervalResponse buildResponse(boolean needsReconstruction) {
        xtreemfs_ec_write_intervalResponse.Builder responseB = xtreemfs_ec_write_intervalResponse.newBuilder();
        responseB.setNeedsReconstruction(needsReconstruction);
        // if (error != null) {
        // responseB.setError(error);
        // }
        return responseB.build();
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_write_intervalRequest rpcrq = (xtreemfs_ec_write_intervalRequest) rq.getRequestArgs();
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
