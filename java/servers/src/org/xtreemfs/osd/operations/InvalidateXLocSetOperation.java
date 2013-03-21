/*
 * Copyright (c) 2012-2013 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

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
import org.xtreemfs.osd.stages.PreprocStage.InvalidateXLocSetCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_xloc_set_invalidateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public class InvalidateXLocSetOperation extends OSDOperation {
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public InvalidateXLocSetOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();

    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_XLOC_SET_INVALIDATE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        master.getPreprocStage().invalidateXLocSet(rq, new InvalidateXLocSetCallback() {
            
            @Override
            public void invalidateComplete(String fileId, int version, boolean isPrimary, ErrorResponse error) {
                // TODO Auto-generated method stub
            }
        });

        // master.getXLocSetStage().invalidateXLocSet(rq, new InvalidateOperationCallback() {
        //
        // @Override
        // public void invalidateComplete(boolean isPrimary) {
        //
        // xtreemfs_xloc_set_invalidateResponse msg = xtreemfs_xloc_set_invalidateResponse.newBuilder()
        // .setFileId(rq.getFileId()).setIsPrimary(isPrimary).build();
        //
        // rq.sendSuccess(msg, null);
        // }
        // });
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_xloc_set_invalidateRequest rpcrq = (xtreemfs_xloc_set_invalidateRequest) rq.getRequestArgs();
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
    public boolean requiresValidView() {
        return false;
    }
}
