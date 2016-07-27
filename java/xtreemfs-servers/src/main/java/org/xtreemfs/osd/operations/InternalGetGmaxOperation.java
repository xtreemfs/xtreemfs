/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
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
import org.xtreemfs.osd.stages.StorageStage.InternalGetGmaxCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_gmaxRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalGetGmaxOperation extends OSDOperation {
    
    final String      sharedSecret;
    
    final ServiceUUID localUUID;
    
    public InternalGetGmaxOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }
    
    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_GET_GMAX;
    }
    
    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_internal_get_gmaxRequest args = (xtreemfs_internal_get_gmaxRequest) rq
                .getRequestArgs();
        master.getStorageStage().internalGetGmax(
            args.getFileId(),
            rq.getLocationList().getLocalReplica().getStripingPolicy(),
            rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP ? rq.getCapability()
                    .getSnapTimestamp() : 0, rq, new InternalGetGmaxCallback() {
                
                @Override
                public void gmaxComplete(InternalGmax result, ErrorResponse error) {
                    if (error != null) {
                        rq.sendError(error);
                    } else
                        sendResponse(rq, result);
                }
            });
    }
    
    public void sendResponse(OSDRequest rq, InternalGmax result) {
        rq.sendSuccess(result,null);
    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_internal_get_gmaxRequest rpcrq = (xtreemfs_internal_get_gmaxRequest)rq.getRequestArgs();
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
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}