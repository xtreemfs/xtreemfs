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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_statusRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalRWRStatusOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRStatusOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_STATUS;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_statusRequest args = (xtreemfs_rwr_statusRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR status request for file %s",args.getFileId());
        }

        getState(rq,args);

    }
    
    public void getState(final OSDRequest rq, final xtreemfs_rwr_statusRequest args) {
        master.getStorageStage().internalGetReplicaState(args.getFileId(),
               rq.getLocationList().getLocalReplica().getStripingPolicy(), args.getMaxLocalObjVersion(), new InternalGetReplicaStateCallback() {

            @Override
            public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error) {
                sendResult(rq, localState, error);
            }
        });
    }

    public void sendResult(final OSDRequest rq, ReplicaStatus response, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //only locally
           rq.sendSuccess(response,null);
        }
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_rwr_statusRequest rpcrq = (xtreemfs_rwr_statusRequest)rq.getRequestArgs();
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