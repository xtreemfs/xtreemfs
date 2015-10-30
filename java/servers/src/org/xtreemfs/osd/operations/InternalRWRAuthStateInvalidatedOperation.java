/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
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
import org.xtreemfs.mrc.stages.XLocSetCoordinator;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.FileOperationCallback;
import org.xtreemfs.osd.stages.PreprocStage.InvalidateXLocSetCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.LeaseState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_auth_stateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 * Sets the authoritative state on an invalidated replica and fetch missing data from other OSDs. <br>
 * In contrast to {@link InternalRWRAuthStateOperation} this operation does not require a valid view and works on
 * invalidated replicas. Effectively the replica will be invalidated when executing this operation. <br>
 * This operation is intended to be called from the MRCs {@link XLocSetCoordinator}.
 */
public class InternalRWRAuthStateInvalidatedOperation extends OSDOperation {

    final String      sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRAuthStateInvalidatedOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_AUTH_STATE_INVALIDATED;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_auth_stateRequest args = (xtreemfs_rwr_auth_stateRequest) rq.getRequestArgs();

        master.getPreprocStage().invalidateXLocSet(rq, args.getFileCredentials(), false,
                new InvalidateXLocSetCallback() {

                    @Override
                    public void invalidateComplete(LeaseState leaseState, ErrorResponse error) {
                        if (error != null) {
                            rq.sendError(error);
                        } else {
                            postInvalidation(rq);
                        }
                    }
                });
    }

    private void postInvalidation(final OSDRequest rq) {
        final String fileId = rq.getFileId();

        master.getStorageStage().internalGetReplicaState(fileId,
                rq.getLocationList().getLocalReplica().getStripingPolicy(), 0, new InternalGetReplicaStateCallback() {

            @Override
            public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error) {

                if (error != null) {
                    rq.sendError(error);
                } else {
                    startFetch(rq, localState);
                }
            }
        });
    }

    private void startFetch(final OSDRequest rq, final ReplicaStatus localState) {
        final String fileId = rq.getFileId();
        final XLocations xloc = rq.getLocationList();
        final xtreemfs_rwr_auth_stateRequest args = (xtreemfs_rwr_auth_stateRequest) rq.getRequestArgs();

        master.getRWReplicationStage().fetchInvalidated(fileId, args.getState(), localState, args.getFileCredentials(),
                xloc, new FileOperationCallback() {

                    @Override
                    public void success(long newObjectVersion) {
                        rq.sendSuccess(emptyResponse.getDefaultInstance(), null);
                    }

                    @Override
                    public void failed(ErrorResponse ex) {
                        rq.sendError(ex);
                    }

                    @Override
                    public void redirect(String redirectTo) {
                        // Not used in fetchInvalidated
                        rq.sendError(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR,
                                POSIXErrno.POSIX_ERROR_NONE,
                                "FetchInvalidated called the redirect method. This should never happen"));
                    }
                }, rq);
    }


    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_rwr_auth_stateRequest rpcrq = (xtreemfs_rwr_auth_stateRequest) rq.getRequestArgs();
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
        return true;
    }

}
