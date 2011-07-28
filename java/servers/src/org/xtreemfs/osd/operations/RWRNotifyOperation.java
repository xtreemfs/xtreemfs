/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.PreprocStage.DeleteOnCloseCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class RWRNotifyOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public RWRNotifyOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_NOTIFY;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final FileCredentials args = (FileCredentials)rq.getRequestArgs();

        

        if (!rq.getLocationList().containsOSD(localUUID)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR notify for file %s: REMOVE REPLICA",args.getXcap().getFileId());
            }
            master.getPreprocStage().checkDeleteOnClose(args.getXcap().getFileId(), new DeleteOnCloseCallback() {

                @Override
                public void deleteOnCloseResult(boolean isDeleteOnClose, ErrorResponse error) {
                    sendResult(rq, error);
                }
            });
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR notify for file %s: FORCE RESET",args.getXcap().getFileId());
            }
            master.getRWReplicationStage().eventForceReset(args, rq.getLocationList());
            sendResult(rq, null);
        }
    }
    


    public void sendResult(final OSDRequest rq, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //only locally
            sendResponse(rq);
        }
    }

    
    public void sendResponse(OSDRequest rq) {
        rq.sendSuccess(null,null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            FileCredentials rpcrq = (FileCredentials)rq.getRequestArgs();
            rq.setFileId(rpcrq.getXcap().getFileId());
            rq.setCapability(new Capability(rpcrq.getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpcrq.getXlocs(), localUUID));

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