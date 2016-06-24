/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ProtoInterval;
import org.xtreemfs.osd.stages.StorageStage.ECGetVectorsCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_interval_vectorsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_interval_vectorsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/** FIXME (jdillmann): DOC */
public class ECGetIntervalVectors extends OSDOperation {

    // FIXME (jdillmann): Is it required to check the cap?
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public ECGetIntervalVectors(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_EC_GET_INTERVAL_VECTORS;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_ec_get_interval_vectorsRequest args;
        args = (xtreemfs_ec_get_interval_vectorsRequest) rq.getRequestArgs();

        // FIXME (jdillmann): Use EC Stage with caching instead reading it directly?
        master.getStorageStage().ecGetVectors(rq.getFileId(), rq, new ECGetVectorsCallback() {
            @Override
            public void ecGetVectorsComplete(IntervalVector curVector, IntervalVector nextVector, ErrorResponse error) {
                if (error == null) {
                    sendResult(rq, curVector, nextVector);
                } else {
                    rq.sendError(error);
                }
            }
        });
    }

    void sendResult(final OSDRequest rq, IntervalVector curVector, IntervalVector nextVector) {
        xtreemfs_ec_get_interval_vectorsResponse.Builder respBuilder = xtreemfs_ec_get_interval_vectorsResponse
                .newBuilder();

        for (Interval interval : curVector.serialize()) {
            respBuilder.addCurIntervals(ProtoInterval.toProto(interval));
        }

        for (Interval interval : nextVector.serialize()) {
            respBuilder.addNextIntervals(ProtoInterval.toProto(interval));
        }

        rq.sendSuccess(respBuilder.build(), null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_get_interval_vectorsRequest rpcrq = (xtreemfs_ec_get_interval_vectorsRequest) rq
                    .getRequestArgs();
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
