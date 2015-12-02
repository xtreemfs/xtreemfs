/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.operations;

import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.RedundantFileState;
import org.xtreemfs.osd.rwre.RWReplicationStage.GetReplicatedFileStateCallback;
import org.xtreemfs.osd.rwre.ReplicatedFileStateSimple;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersion;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_reset_statusRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_reset_statusResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 * This operation returns information on the RWR reset status. It returns if there is an ongoing RESET and if the OSDs
 * data is complete in regard to the submitted AuthState.
 */
public class InternalRWRResetStatusOperation extends OSDOperation {

    public InternalRWRResetStatusOperation(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_RESET_STATUS;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        master.getRWReplicationStage().getReplicatedFileState(rq.getFileId(), new GetReplicatedFileStateCallback() {
            @Override
            public void getReplicatedFileStateComplete(ReplicatedFileStateSimple state) {
                if (state.getState() == RedundantFileState.LocalState.RESET) {
                    respond(rq, true, false);
                } else {
                    postGetStatus(rq);
                }
            }

            @Override
            public void failed(ErrorResponse error) {
                rq.sendError(error);
            }
        }, rq);
    }

    private void postGetStatus(final OSDRequest rq) {
        master.getStorageStage().internalGetReplicaState(rq.getFileId(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(), 0, new InternalGetReplicaStateCallback() {

                    @Override
                    public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error) {
                        if (error == null) {
                            xtreemfs_rwr_reset_statusRequest rpcrq = (xtreemfs_rwr_reset_statusRequest) rq
                                    .getRequestArgs();
                            boolean complete = checkReplicaStateComplete(rpcrq.getState(), localState);
                            respond(rq, false, complete);
                        } else {
                            respond(rq, false, false);
                        }
                    }
                });
    }

    static boolean checkReplicaStateComplete(AuthoritativeReplicaState authState, ReplicaStatus localState) {
        // Return incomplete if the local replica did miss a truncate
        if (localState.getTruncateEpoch() < authState.getTruncateEpoch()) {
            return false;
        }

        // Check if the local replica has every object from the auth state stored.
        Map<Long, ObjectVersionMapping> missingObjects = new HashMap<Long, ObjectVersionMapping>();
        for (ObjectVersionMapping authObject : authState.getObjectVersionsList()) {
            missingObjects.put(authObject.getObjectNumber(), authObject);
        }
        for (ObjectVersion localObject : localState.getObjectVersionsList()) {
            ObjectVersionMapping object = missingObjects.get(localObject.getObjectNumber());
            if ((object != null) && (localObject.getObjectVersion() >= object.getObjectVersion())) {
                missingObjects.remove(localObject.getObjectNumber());
            }
        }
        return missingObjects.isEmpty();
    }

    private void respond(final OSDRequest rq, boolean running, boolean complete) {
        xtreemfs_rwr_reset_statusResponse response = xtreemfs_rwr_reset_statusResponse.newBuilder().setRunning(running)
                .setComplete(complete).build();

        rq.sendSuccess(response, null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_rwr_reset_statusRequest rpcrq = (xtreemfs_rwr_reset_statusRequest) rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileId());
            rq.setCapability(
                    new Capability(rpcrq.getFileCredentials().getXcap(), master.getConfig().getCapabilitySecret()));
            rq.setLocationList(
                    new XLocations(rpcrq.getFileCredentials().getXlocs(), master.getConfig().getUUID()));

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
        return true;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
