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
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECReconstructionStage;
import org.xtreemfs.osd.ec.InternalOperationCallback;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.stages.StorageStage.ECCommitVectorCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/** FIXME (jdillmann): DOC */
public class ECCommitVectorOperation extends OSDOperation {
    final static public int PROC_ID = OSDServiceConstants.PROC_ID_XTREEMFS_EC_COMMIT_VECTOR;

    // FIXME (jdillmann): Is it required to check the cap?
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public ECCommitVectorOperation(OSDRequestDispatcher master) {
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
        final xtreemfs_ec_commit_vectorRequest args;
        args = (xtreemfs_ec_commit_vectorRequest) rq.getRequestArgs();

        final String fileId = args.getFileId();
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        final ECReconstructionStage reconstructor = master.getEcReconstructionStage();
        if (reconstructor.isInReconstruction(fileId)) {
            rq.sendError(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                    "File is in reconstruction"));
            return;
        }

        // Create the IntervalVector from the message
        final List<Interval> commitIntervals = new ArrayList<Interval>(args.getIntervalsCount());
        for (IntervalMsg msg : args.getIntervalsList()) {
            commitIntervals.add(new ProtoInterval(msg));
        }

        master.getStorageStage().ecCommitVector(fileId, sp, commitIntervals, rq, new ECCommitVectorCallback() {
            @Override
            public void ecCommitVectorComplete(List<Interval> missingIntervals, boolean needsReconstruct,
                    ErrorResponse error) {
                if (error != null) {
                    rq.sendError(error);
                } else if (needsReconstruct) {
                    reconstructor.startReconstruction(fileId, args.getFileCredentials(), rq.getCapability(),
                            rq.getLocationList(), commitIntervals, missingIntervals);
                    rq.sendSuccess(buildResponse(true), null);
                } else {
                    rq.sendSuccess(buildResponse(false), null);
                }
            }
        });
    }

    public void startLocalRequest(final String fileId, final FileCredentials fileCredentials, final XLocations xloc,
            final StripingPolicyImpl sp, final List<Interval> commitIntervals,
            final InternalOperationCallback<xtreemfs_ec_commit_vectorResponse> callback) {

        final ECReconstructionStage reconstructor = master.getEcReconstructionStage();
        if (reconstructor.isInReconstruction(fileId)) {
            callback.localRequestFailed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                    POSIXErrno.POSIX_ERROR_EAGAIN, "File is in reconstruction"));
            return;
        }

        master.getStorageStage().ecCommitVector(fileId, sp, commitIntervals, null, new ECCommitVectorCallback() {
            @Override
            public void ecCommitVectorComplete(List<Interval> missingIntervals, boolean needsReconstruct,
                    ErrorResponse error) {
                if (error != null) {
                    callback.localRequestFailed(error);
                } else if (needsReconstruct) {
                    Capability cap = new Capability(fileCredentials.getXcap(), sharedSecret);
                    reconstructor.startReconstruction(fileId, fileCredentials, cap, xloc, commitIntervals,
                            missingIntervals);
                    callback.localResultAvailable(buildResponse(true), null);
                } else {
                    callback.localResultAvailable(buildResponse(false), null);
                }
            }
        });
    }

    xtreemfs_ec_commit_vectorResponse buildResponse(boolean needsReconstruction) {
        xtreemfs_ec_commit_vectorResponse response = xtreemfs_ec_commit_vectorResponse.newBuilder()
                .setNeedsReconstruction(needsReconstruction)
                .build();
        return response;
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_commit_vectorRequest rpcrq = (xtreemfs_ec_commit_vectorRequest) rq.getRequestArgs();
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
