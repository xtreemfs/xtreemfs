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
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_auth_stateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalRWRAuthStateOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRAuthStateOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_AUTH_STATE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_auth_stateRequest args = (xtreemfs_rwr_auth_stateRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR auth state request for file %s",args.getFileId());
        }

        getLocalReplicaState(rq, args);

    }

    public void getLocalReplicaState(final OSDRequest rq, final xtreemfs_rwr_auth_stateRequest args) {
        master.getStorageStage().internalGetReplicaState(rq.getFileId(), rq.getLocationList().getLocalReplica().getStripingPolicy(), 0, new InternalGetReplicaStateCallback() {

            @Override
            public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error) {
                if (error == null) {
                    master.getRWReplicationStage().eventBackupReplicaReset(rq.getFileId(), args.getState(), localState, args.getFileCredentials(), rq.getLocationList());
                    rq.sendSuccess(emptyResponse.getDefaultInstance(), null);
                } else {
                    sendResult(rq, null, error);
                }
            }
        });
    }

    public void sendResult(final OSDRequest rq, InternalObjectData response, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //only locally
           rq.sendSuccess(response.getMetadata(),response.getData());
        }
    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_rwr_auth_stateRequest rpcrq = (xtreemfs_rwr_auth_stateRequest)rq.getRequestArgs();
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