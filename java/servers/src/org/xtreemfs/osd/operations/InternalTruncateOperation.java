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
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalTruncateOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalTruncateOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_TRUNCATE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final truncateRequest args = (truncateRequest)rq.getRequestArgs();

        if (args.getNewFileSize() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "new_file_size for truncate must be >= 0");
            return;
        }

        master.getStorageStage().truncate(args.getFileId(), args.getNewFileSize(),
            rq.getLocationList().getLocalReplica().getStripingPolicy(),
            rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                args.getObjectVersion() == 0 ? null : args.getObjectVersion(), false, rq,
            new TruncateCallback() {

            @Override
            public void truncateComplete(OSDWriteResponse result, ErrorResponse error) {
                step2(rq, result, error);
            }
        });
    }

    public void step2(final OSDRequest rq, OSDWriteResponse result, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //only locally
            sendResponse(rq, result);
        }
    }

    
    public void sendResponse(OSDRequest rq, OSDWriteResponse result) {
        rq.sendSuccess(result,null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            truncateRequest rpcrq = (truncateRequest)rq.getRequestArgs();
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