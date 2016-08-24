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
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECReconstructionStage;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.stages.StorageStage.ECWriteDiffCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_diffRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/** FIXME (jdillmann): DOC */
public class ECWriteDiffOperation extends OSDOperation {
    final static public int PROC_ID = OSDServiceConstants.PROC_ID_XTREEMFS_EC_WRITE_DIFF;

    // FIXME (jdillmann): Is it required to check the cap?
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public ECWriteDiffOperation(OSDRequestDispatcher master) {
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

        final xtreemfs_ec_write_diffRequest args = (xtreemfs_ec_write_diffRequest) rq.getRequestArgs();

        final String fileId = args.getFileId();
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        final long opId = args.getOpId();
        final long objNo = args.getObjectNumber();
        final int offset = args.getOffset();

        final long stripeNo = sp.getRow(objNo);
        final int osdNo = sp.getRelativeOSDPosition();
        assert (osdNo >= 0);

        ReusableBuffer data = rq.getRPCRequest().getData();
        assert (data != null);

        final ECReconstructionStage reconstructor = master.getEcReconstructionStage();
        if (reconstructor.isInReconstruction(fileId)) {
            rq.sendError(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EAGAIN,
                    "File is in reconstruction"));
            return;
        }

        Interval diffInterval = new ProtoInterval(args.getDiffInterval());
        Interval stripeInterval = new ProtoInterval(args.getStripeInterval());

        // Create the IntervalVector from the message
        final List<Interval> commitIntervals = new ArrayList<Interval>(args.getCommitIntervalsCount());
        for (IntervalMsg msg : args.getCommitIntervalsList()) {
            commitIntervals.add(new ProtoInterval(msg));
        }

        if (commitIntervals.isEmpty()) {
            Interval interval = ObjectInterval.empty(diffInterval.getOpStart(), diffInterval.getOpEnd());
            commitIntervals.add(interval);
        }
        
        ReusableBuffer viewBuffer = data.createViewBuffer();
        master.getStorageStage().ecWriteDiff(fileId, sp, objNo, offset, diffInterval, stripeInterval, commitIntervals,
                viewBuffer, rq, new ECWriteDiffCallback() {

                    @Override
                    public void ecWriteDiffComplete(boolean stripeComplete, boolean needsReconstruct,
                            ErrorResponse error) {
                        if (error != null) {
                            master.getECMasterStage().sendDiffResponse(fileId, args.getFileCredentials(),
                                    rq.getLocationList(), opId, stripeNo, osdNo, false, error);
                        } else if (needsReconstruct) {
                            // FIXME (jdillmann): Trigger reconstruction if not complete.
                            master.getECMasterStage().sendDiffResponse(fileId, args.getFileCredentials(),
                                    rq.getLocationList(), opId, stripeNo, osdNo, true, null);
                        } else if (stripeComplete) {
                            master.getECMasterStage().sendDiffResponse(fileId, args.getFileCredentials(),
                                    rq.getLocationList(), opId, stripeNo, osdNo, false, null);
                        }

                    }
                });

        // immediately respond to the data device
        rq.sendSuccess(null, null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_write_diffRequest rpcrq = (xtreemfs_ec_write_diffRequest) rq.getRequestArgs();
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
