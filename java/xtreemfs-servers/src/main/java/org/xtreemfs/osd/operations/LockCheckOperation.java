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
import org.xtreemfs.osd.stages.PreprocStage.LockOperationCompleteCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.lockRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * <br>15.06.2009
 */
public class LockCheckOperation extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public LockCheckOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_LOCK_CHECK;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final lockRequest args = (lockRequest) rq
                .getRequestArgs();

//        System.out.println("rq: " + args);

        master.getPreprocStage().checkLock(args.getLockRequest().getClientUuid(), args.getLockRequest().getClientPid(), args.getFileCredentials().getXcap().getFileId(),
                args.getLockRequest().getOffset(), args.getLockRequest().getLength(), args.getLockRequest().getExclusive(), rq, new LockOperationCompleteCallback() {

            @Override
            public void parseComplete(Lock result, ErrorResponse error) {
                postAcquireLock(rq,args,result,error);
            }
        });
    }

    public void postAcquireLock(final OSDRequest rq, lockRequest args,
            Lock lock, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            rq.sendSuccess(lock,null);
        }
    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            lockRequest rpcrq = (lockRequest)rq.getRequestArgs();
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

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
