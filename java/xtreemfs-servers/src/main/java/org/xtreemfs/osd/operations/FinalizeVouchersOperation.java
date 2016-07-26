/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.operations;

import java.util.HashSet;
import java.util.Set;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.FinalizeVoucherCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDFinalizeVouchersResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_finalize_vouchersRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 * Operation to covers the finalize request and enqueues the proper operation on the StorageThread.
 */
public class FinalizeVouchersOperation extends OSDOperation {

    final String      sharedSecret;
    final ServiceUUID localUUID;

    public FinalizeVouchersOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_FINALIZE_VOUCHERS;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        xtreemfs_finalize_vouchersRequest args = (xtreemfs_finalize_vouchersRequest) rq.getRequestArgs();

        Set<Long> expireTimeSet = new HashSet<Long>(args.getExpireTimeMsList());
        expireTimeSet.add(rq.getCapability().getExpireMs());
        master.getStorageStage().finalizeVouchers(rq.getFileId(), rq.getCapability().getClientIdentity(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(), expireTimeSet, rq,
                new FinalizeVoucherCallback() {

                    @Override
                    public void finalizeVoucherComplete(OSDFinalizeVouchersResponse result, ErrorResponse error) {
                        sendResult(rq, result, error);
                    }
                });
    }

    public void sendResult(final OSDRequest rq, OSDFinalizeVouchersResponse result, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            rq.sendSuccess(result, null);
        }

    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");

    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_finalize_vouchersRequest rpcrq = (xtreemfs_finalize_vouchersRequest) rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileCredentials().getXcap().getFileId());
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

}
